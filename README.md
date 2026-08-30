# AI Resume Analyser — Microservices Architecture

> **Status: NOT VERIFIED.** Every service here was written by hand-migrating
> the original monolith's code (`ResumeBackend/`, kept alongside this as the
> working fallback - see "Migration status" below). I had no `mvn`, no cached
> Maven dependencies, and no access to Maven Central in the environment this
> was built in, so **none of this backend code has been compiled, let alone
> run.** Brace-balance and structural checks pass; that is not the same as a
> successful build. Run `mvn clean compile` in every `backend/*` folder
> before trusting any of it. The frontend, by contrast, **has** been built
> and linted successfully (`npm run build`, 0 errors).

## Architecture

```text
                    ┌──────────────────────┐
                    │     React + Vite     │  :5173
                    └──────────┬───────────┘
                               │ VITE_API_URL
                               ▼
                    ┌──────────────────────┐
                    │     API Gateway      │  :8080
                    │  (routing, CORS,     │
                    │   JWT pre-check)     │
                    └──────────┬───────────┘
                               │
          ┌────────────────────┼─────────────────────┬───────────────┐
          ▼                    ▼                     ▼               ▼
 ┌────────────────┐   ┌────────────────┐   ┌────────────────┐  ┌─────────────┐
 │  auth-service   │   │ resume-service │   │analysis-service│  │ job-service │
 │     :8081       │   │     :8082      │   │     :8083      │  │    :8085    │
 └───────┬─────────┘   └────────────────┘   └───────┬────────┘  └──────┬──────┘
         │                                            │                 ▲
         │ (internal)                    ┌────────────┼─────────────┐   │
         ▼                                ▼            ▼             │   │
 ┌─────────────────┐              ┌─────────────┐ ┌──────────────┐  │   │
 │ notification-svc │              │  Gemini AI  │ │report-service│──┘   │
 │      :8086       │              └─────────────┘ │    :8084     │──────┘
 └─────────────────┘                                └──────────────┘

          ┌───────────────────────┐   ┌──────────────────────┐
          │ eureka-server  :8761  │   │  config-server :8888  │
          └───────────────────────┘   └──────────────────────┘

                          ┌───────────────┐
                          │     MySQL     │  :3306
                          └───────────────┘
```

No Redis - nothing in this codebase has a real use case for it yet (see
`docs/microservices-migration-design.md` §20/§28 for the reasoning).

## Migration status

The original monolith is untouched at `ResumeBackend/` and still works on
its own (it already got a same-repo async-analysis fix in an earlier pass -
see its own code comments). This microservices split is **new, parallel,
unverified code** in `backend/*` - per the strangler-fig approach in
`docs/microservices-migration-design.md`, don't delete the monolith until
you've actually built and exercised this new stack.

| Original class | New home | Status |
|---|---|---|
| `UsersTable`, `OtpVerify`, `JwtService(Impl)`, `JwtFilter`, `SecurityConfiguration`, `SuccessHandler`, `FailureHandler`, `SecurityController` | `auth-service` | Migrated |
| `MailServiceImpl` + Thymeleaf templates | `notification-service` | Migrated |
| Tika parsing | `resume-service` | Migrated |
| `AnalysisJob`, `AnalysisWorker`, Gemini call | `analysis-service` | Migrated (adapted to call resume-service/report-service instead of local repos) |
| `PreviousTable`, `PrevTableRepo` | `report-service` | Migrated, table name preserved (`previous_table`) |
| Adzuna integration (`fetchAdzunaJobsWithFallback`) | `job-service` | Migrated verbatim (same 3-tier fallback) |
| `AppController`'s `/logout`, `/delete-account`, `/validate-token` | `auth-service` | Migrated |

**Explicitly not implemented** (see README §"What's scoped out" below):
GitHub OAuth (never actually existed in the monolith despite being mentioned
throughout the migration brief - only Google was configured), report
history/list/delete (`PreviousTable.email` is the primary key - only one
report per user ever existed; see design doc §5), Flyway migrations, and a
per-service automated test suite.

## Service ports

| Service | Port | Purpose |
|---|---|---|
| frontend | 5173 | React/Vite |
| api-gateway | 8080 | Public entry point |
| auth-service | 8081 | JWT, OAuth, OTP, account |
| resume-service | 8082 | Upload + Tika parsing |
| analysis-service | 8083 | Async Gemini analysis |
| report-service | 8084 | Report storage/retrieval |
| job-service | 8085 | Adzuna job search |
| notification-service | 8086 | Email/OTP delivery |
| config-server | 8888 | Centralized config |
| eureka-server | 8761 | Service registry |
| MySQL | 3306 | Shared database (see below) |

## Database

All services currently point at **the same MySQL database**
(`resume_analyser`), each JPA-managing only its own entities - not a full
schema-per-service split yet. This was a deliberate choice, not an
oversight: a real split is higher-risk than I could respondsibly do without
being able to compile or test any of it. See design doc §5/§18 for the
reasoning and what a real split would require. `previous_table`, `users_table`,
and `otp_verify` keep their exact original names/columns, so existing data
from the monolith is preserved and immediately usable.

Set `spring.jpa.hibernate.ddl-auto: update` (already the default in every
service's config) will create the new `analysis_job` table automatically on
first boot - it's new, additive, and safe.

## Local setup

### Prerequisites
Java 17, Maven, Node 20+, MySQL running locally (or via the Docker Compose
file below), and real API keys for Gemini/Adzuna/Brevo/Google OAuth (**not**
the ones that were in the original `application.properties` - rotate those,
see the security note in the design doc).

### Environment variables
```bash
cp .env.example .env
# fill in every value in .env
```
See `.env.example` for the full list.

### Run without Docker (each in its own terminal)
```bash
cd backend/eureka-server && mvn spring-boot:run
cd backend/config-server && mvn spring-boot:run       # after eureka-server is up
cd backend/notification-service && mvn spring-boot:run
cd backend/resume-service && mvn spring-boot:run
cd backend/job-service && mvn spring-boot:run
cd backend/auth-service && mvn spring-boot:run
cd backend/analysis-service && mvn spring-boot:run
cd backend/report-service && mvn spring-boot:run
cd backend/api-gateway && mvn spring-boot:run          # after every service above is up
cd frontend && npm install && npm run dev
```
Start order matters loosely (gateway last, eureka/config first) but every
service's `spring.config.import` is `optional:configserver:...`, so a
missing config-server never blocks startup - it can only override values,
never require them.

### Run with Docker Compose
```bash
docker compose up --build
```
**NOT VERIFIED** - see the Dockerfiles' own comments. No Docker daemon was
available to test this here.

## API contracts (via the gateway)

```text
POST /api/auth/register
POST /api/auth/login
POST /api/auth/verify-email
POST /api/auth/send-reset-otp
POST /api/auth/verify-reset-otp
POST /api/auth/reset-password
POST /api/auth/logout                 (auth required)
DELETE /api/auth/delete-account       (auth required)
POST /api/auth/validate-token         (auth required)
GET  /oauth2/authorization/google     (OAuth redirect)

POST /api/analyses                    (auth required, multipart: roles, jobDescription, file)
     -> 202 { analysisId, status: "QUEUED" }
GET  /api/analyses/{id}/status        (auth required, owner-only)
     -> { analysisId, status, progress, message, errorMessage, reportReady }

GET  /api/reports/latest              (auth required)

POST /api/jobs/search                 (auth required)
```

## Resume analysis flow

```text
Frontend
  │ POST /api/analyses (file, role, jobDescription)
  ▼
API Gateway → analysis-service
  │ creates AnalysisJob (QUEUED), returns immediately
  │
  │ (background thread)
  ├─ calls resume-service to parse the file → extracted text
  ├─ calls Gemini directly (bounded retries, never blocks the original request)
  ├─ on success: calls report-service to persist the result
  └─ updates AnalysisJob.status at every stage

Frontend polls GET /api/analyses/{id}/status every ~1.5s until COMPLETED/FAILED
  │
  ▼ (on COMPLETED)
GET /api/reports/latest → report-service
  │ calls job-service for matching jobs (failure here never fails the report -
  │ falls back to jobs: [] + an explanatory message)
  ▼
Report displayed
```

This is the same async fix built in the previous session
(`AnalysisJob`/`AnalysisWorker`/polling), now split across service
boundaries rather than living in one process.

## Security notes

- JWT is validated **independently by every service that needs identity**
  (auth-service, resume-service, analysis-service, report-service,
  job-service each carry their own small validator), not trusted from a
  gateway-set header - see design doc §6/§9 for why.
- Internal-only endpoints (account deletion cascade, report save, the
  `previousResults` flag update) are guarded by a shared `INTERNAL_API_KEY`
  header and are **not** reachable through the gateway's public route table.
- CORS is configured **only** at the gateway.
- **Rotate every credential that was in the original monolith's
  `application.properties`** - see the top of this README and the design doc.

## What's scoped out (told you rather than faked)

- Flyway migrations - `ddl-auto: update` is used instead, which is fine for
  this migration but isn't how you'd want to manage schema changes long-term.
- Per-service automated test suites - none were added. `mvn clean test` will
  currently just run (or fail to find) whatever default test scaffolding
  Spring Initializr would generate; no real tests were written for any service.
- A real schema-per-service split - see Database section above.
- Report history (list/get-by-id/delete) - blocked on the same schema
  constraint, see design doc §5.
- GitHub OAuth - was never actually implemented in the original monolith
  despite being mentioned throughout the request; only a placeholder config
  key exists.
- Circuit breakers - not added (§28 says not to over-engineer; nothing here
  has hit a failure mode yet that a bounded retry + graceful fallback, both
  of which do exist, wouldn't already handle).

## Further reading

`docs/microservices-migration-design.md` has the full rationale for every
architectural decision above (why shared DB, why gateway doesn't set trust
headers, why job search is decoupled from analysis, sequencing for an
incremental real-world rollout, etc).
