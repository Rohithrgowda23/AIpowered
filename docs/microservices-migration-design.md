# AI Resume Analyser — Microservices Migration Design Doc

Status: **design only, not implemented**. This is a roadmap for splitting the
current Spring Boot monolith into the service layout you asked for, written
so the split can happen incrementally, one service at a time, without a
big-bang rewrite.

The async analysis fix (`AnalysisJob` / `AnalysisWorker` / `POST /analyses` /
`GET /analyses/{id}/status`) is already implemented in the monolith — see the
code. That work is what `analysis-service` below is built around; extracting
it into its own service later is a lift-and-shift of code that already
exists and (once you've run `mvn clean compile` / tested it) already works,
not new logic.

---

## 1. Why incremental, not big-bang

The current app is a single Spring Boot service with one MySQL database, JWT
auth, OAuth (Google/GitHub), Gemini analysis, Adzuna job search, and Brevo
email — all working. Rewriting all of that into 9 services in one pass means
9 services' worth of new bugs land simultaneously, with no working baseline
to fall back to if something breaks in production. The **strangler fig**
pattern avoids that: stand up the new architecture *around* the monolith,
move one capability at a time behind the gateway, and only delete the
monolith's copy of that capability once the extracted service is proven.

Sequencing matters. Recommended order, each stage independently shippable:

1. **API Gateway + Eureka** in front of the existing monolith (zero behavior
   change — just adds a hop).
2. **Config Server**, monolith pulls its config from it instead of local
   `application.yml`.
3. **notification-service** (email/OTP) — lowest risk, no auth coupling.
4. **resume-service** (upload + parsing) — self-contained, file-in/text-out.
5. **analysis-service** — owns `AnalysisJob`/`AnalysisWorker`/Gemini calls
   (already built, see above).
6. **report-service** — owns `PreviousTable` and the report DTOs.
7. **job-service** — owns the Adzuna integration.
8. **auth-service** last — everything else depends on JWT validation, so
   extracting it is the highest-blast-radius step and should happen once
   you have confidence in the pattern from the previous six.

At every stage the monolith keeps running; you only remove a capability from
it after the corresponding service has been running in parallel and verified.

---

## 2. Target architecture

```text
                              ┌──────────────┐
                              │ React / Vite │
                              └──────┬───────┘
                                     │ VITE_API_URL
                                     ▼
                              ┌──────────────┐
                              │ API Gateway  │  :8080
                              │ (Spring      │  - routing
                              │  Cloud       │  - JWT validation
                              │  Gateway)    │  - CORS
                              └──────┬───────┘
                                     │
                    ┌────────────────────────────────┐
                    │      Eureka Service Registry     │  :8761
                    └────────────────────────────────┘
                                     │
      ┌───────────┬─────────────────┼─────────────────┬──────────────┐
      ▼           ▼                 ▼                 ▼              ▼
 auth-service  resume-service  analysis-service  report-service  job-service
   :8081          :8082            :8083             :8084         :8085
      │                                │                 │
      │                                ▼                 │
      │                          ┌──────────┐            │
      │                          │  Gemini  │            │
      │                          └──────────┘            │
      │                                                    
      ▼                                                    
notification-service  :8086                                
      │                                                    
      ▼                                                    
   Brevo                                                    

              config-server :8888  (all services pull config from here)
```

Each service has its own database/schema (see §5) and registers with Eureka.
The gateway is the *only* thing the frontend talks to.

---

## 3. Service responsibilities & why the boundary is there

| Service | Owns | Does NOT own |
|---|---|---|
| **api-gateway** | Routing, JWT validation at the edge, CORS, rate limiting | Business logic |
| **eureka-server** | Service discovery | Nothing else |
| **config-server** | Centralized config, env-var-backed secrets | Nothing else |
| **auth-service** | Register/login/JWT issuance, email verification, OTP, password reset, Google/GitHub OAuth, account deletion | Resume data, reports |
| **resume-service** | Upload, file validation, Tika parsing → plain text | Scoring, AI calls |
| **analysis-service** | `AnalysisJob` state machine, Gemini calls, retry/timeout handling | Persisting the final report long-term, job search |
| **report-service** | `PreviousTable` (or its successor), report DTOs, list/get/delete | AI logic |
| **job-service** | Adzuna integration, term broadening/fallback logic | Resume/report data |
| **notification-service** | Email sending, OTP generation/verification handoff | Auth logic (issuing tokens) |

The `analysis-service` / `report-service` split mirrors what's already true
in the code today: `AnalysisWorker` computes a result and hands it off;
`lastReport()` reads it back and separately calls Adzuna. That existing
seam is exactly where the service boundary should go — it's not a new cut,
just formalizing one that already exists.

---

## 4. API Gateway routing table

```text
/api/auth/**       -> auth-service
/api/resumes/**     -> resume-service
/api/analyses/**    -> analysis-service
/api/reports/**     -> report-service
/api/jobs/**        -> job-service
```

JWT validation happens **once**, at the gateway, using a `GatewayFilter`
that calls a lightweight introspection endpoint on auth-service (or
validates the signature directly if the gateway holds the public key —
cheaper, no extra hop per request, and works fine since the JWT here is
already self-contained per `JwtServiceImpl`). Downstream services trust an
internal header (e.g. `X-User-Email`) set by the gateway after validation,
rather than each re-parsing the JWT. This is a meaningful behavior change
from today (where `JwtFilter` runs inside the monolith on every request) and
should be tested carefully — specifically, downstream services must reject
any request that arrives *without* that internal header, so a compromised
network path can't spoof identity by hitting a service directly instead of
through the gateway.

---

## 5. Database strategy

Per §19 of your brief: separate schemas per service, not necessarily
separate MySQL instances, to start.

```text
auth_db      — UsersTable (existing)
resume_db    — (new) resume metadata if you persist it; today parsing is
                stateless (parse-and-discard), so this may start empty
analysis_db  — AnalysisJob (new, already added in the monolith)
report_db    — PreviousTable + its @ElementCollection tables (existing)
job_db       — likely empty; Adzuna results are not persisted today
```

**Critical constraint already discovered while implementing the async fix:**
`PreviousTable.email` is the `@Id` — there is only ever one report per user,
overwritten on each analysis. If you want real report *history* (the
"Previous Reports" list/search/filter UI from your earlier redesign request),
that requires a schema change here: add a surrogate `id`, make `email` a
regular indexed column, add `createdAt`, and add `GET /reports`,
`DELETE /reports/{id}` alongside the existing `GET /reports/latest`. That's a
backward-compatible additive migration (old single-report behavior keeps
working via a "latest" query) and should happen **before or during** the
report-service extraction, not after — it's much cheaper to do once, in one
place, than to migrate a distributed report-service's schema later.

Use Flyway for every service's schema from day one of that service's
extraction (`V1__init.sql` = current shape, subsequent `V2__...` for changes).
Do **not** run Flyway against the monolith's existing tables destructively —
the first migration for each extracted service should be additive/no-op
against existing data, then verified, before any column changes.

---

## 6. Analysis workflow in the target architecture

This is a relocation of the code already written in the monolith, not new
logic:

```text
Frontend
   │ POST /api/analyses (multipart: file, role, jobDescription)
   ▼
API Gateway  ── validates JWT, forwards with X-User-Email header
   ▼
analysis-service
   │ creates AnalysisJob row (analysis_db), returns { analysisId, status: QUEUED }
   │ immediately, HTTP request ends here
   │
   │ (background thread, resumeAnalysisExecutor)
   ├─ calls resume-service to get parsed text (or does its own Tika parse,
   │  if resume-service is not yet extracted at this stage)
   ├─ calls Gemini directly (as AnalysisWorker does today)
   ├─ on success: calls report-service to persist the result
   └─ updates AnalysisJob.status at each stage

Frontend
   │ GET /api/analyses/{id}/status   (poll every ~1.5s, as implemented)
   ▼
API Gateway -> analysis-service -> AnalysisStatusDto
```

Job search stays decoupled from analysis (as it is today — Adzuna is called
from `lastReport()`, not from the analysis pipeline), so job-service being
down never fails an analysis. `report-service`'s "get report" endpoint calls
`job-service` at read time and degrades to `jobs: []` +
`jobSearchMessage` on failure, exactly like `fetchAdzunaJobsWithFallback`
does today.

---

## 7. Docker Compose (local dev) — service list & ports

```yaml
services:
  mysql:            "3306:3306"
  redis:             "6379:6379"   # only if/when a service actually needs caching or rate-limit counters
  eureka-server:     "8761:8761"
  config-server:     "8888:8888"
  api-gateway:       "8080:8080"
  auth-service:      "8081:8081"
  resume-service:    "8082:8082"
  analysis-service:  "8083:8083"
  report-service:    "8084:8084"
  job-service:       "8085:8085"
  notification-service: "8086:8086"
  frontend:          "5173:5173"   # dev; served via nginx in prod build
```

Each service `depends_on` eureka-server and config-server with a health-check
condition (`condition: service_healthy`), not just startup order, since
Spring Boot apps that start before their config is available fail hard.

Redis is listed but **optional per your own instruction** ("don't make Redis
mandatory for every service if it isn't needed") — the only clear current use
case is rate-limiting at the gateway; nothing else in this app needs a shared
cache today.

---

## 8. Environment variables (`.env.example`)

```text
# Database
DATABASE_URL=jdbc:mysql://mysql:3306/resume_analyser
DATABASE_USERNAME=
DATABASE_PASSWORD=

# Auth
JWT_SECRET=

# AI
GEMINI_API_KEY=

# OAuth
GOOGLE_CLIENT_ID=
GOOGLE_CLIENT_SECRET=
GITHUB_CLIENT_ID=
GITHUB_CLIENT_SECRET=

# Email
BREVO_API_KEY=

# Jobs
ADZUNA_APP_ID=
ADZUNA_APP_KEY=
ADZUNA_COUNTRY=

# Frontend
VITE_API_URL=http://localhost:8080
```

Config Server should read these from the environment (not from a committed
`application.yml`) and expose per-service config over its `/​{service}/{profile}`
endpoint; each service's `bootstrap.yml` points at `config-server:8888` and
nothing else.

---

## 9. Cross-cutting concerns worth flagging now

- **OAuth redirects**: `SuccessHandler`/`FailureHandler` currently issue a
  redirect with the JWT in the URL back to the frontend. Once auth-service is
  behind the gateway, the OAuth provider's registered redirect URI must point
  at the **gateway's** public URL, not directly at auth-service — this is an
  easy thing to get wrong and worth a dedicated test pass when auth-service
  is extracted (stage 8, deliberately last).
- **CORS**: currently configured once, in the monolith's
  `SecurityConfiguration`. In the target state it should live **only** at the
  gateway — downstream services should not need their own CORS config, since
  they're never called directly by the browser.
- **Correlation IDs**: generate a `traceId` at the gateway (or reuse the
  `analysisId` where one exists) and propagate it via a header through every
  downstream call, so a single analysis can be traced across
  analysis-service → resume-service → report-service → job-service in logs.
- **Lazy-loading**: `PreviousTable`'s `@ElementCollection` lists (skills,
  strengths, etc.) currently load within the request via the repository
  method's implicit transaction. Once `report-service` exposes these over
  REST, they need to be mapped to DTOs *inside* a `@Transactional` service
  method before the entity is returned — never return the JPA entity itself
  from a controller, and never flip these to `FetchType.EAGER` as a shortcut
  (per your own instruction).

---

## 10. Testing strategy for the migration

For each service, at extraction time:
- Unit tests for the extracted business logic (should mostly already exist
  or be portable from the monolith's existing tests, if any).
- A contract test against the gateway route for that service.
- An end-to-end smoke test that repeats the exact 15-step verification list
  from your brief (upload → analysisId → status changes → COMPLETED →
  report displayed), run against the **new** topology before removing the
  capability from the monolith.
- Explicit failure-mode tests per service: Gemini 429/503/malformed JSON for
  analysis-service, Adzuna failure for job-service (must not fail the
  report), expired JWT at the gateway.

---

## 11. What's already done vs. what this doc covers

| Piece | Status |
|---|---|
| Async `AnalysisJob` state machine, dedicated executor, `POST /analyses`, `GET /analyses/{id}/status` | **Implemented** in the monolith (this session) — needs `mvn clean compile` + testing on your end, see the caveat given earlier |
| Frontend polling hook + real progress UI | **Implemented**, builds and lints clean |
| Everything in this document (gateway, Eureka, config server, service extraction, Docker Compose, Flyway migrations, per-service tests) | **Design only** — not implemented |

## 12. Suggested next concrete step

Given the sequencing in §1, the lowest-risk next piece of *actual code* would
be standing up `api-gateway` + `eureka-server` as two new, mostly-boilerplate
Spring Cloud projects that route 100% of traffic straight through to the
existing monolith unchanged. That proves the gateway/registry pattern works
end-to-end without touching any business logic, and gives you a safe point to
pause and verify before extracting the first real service. Happy to build
that scaffold next if useful — flagging that, like the changes already made,
I won't be able to compile/verify it here for the same Maven Central reason,
so it would need your local `mvn clean compile` before you trust it either.
