import { toast } from "react-toastify"
import Styles from "./upload.module.css"
import { useContext, useEffect, useRef, useState } from "react"
import { UserContext } from "../context/usercontext";
import { useNavigate } from "react-router-dom";
import Navbar from "../components/Navbar.jsx";
import { useAnalysisStatus } from "../hooks/useAnalysisStatus";

const ALLOWED_TYPES = [
    'application/pdf',
    'application/msword',
    'application/vnd.openxmlformats-officedocument.wordprocessingml.document'
];
const MAX_FILE_BYTES = 2 * 1024 * 1024;
const MAX_JD_LENGTH = 6000;

const STEPS = [
    { key: "QUEUED", label: "Resume uploaded" },
    { key: "PARSING_RESUME", label: "Reading resume" },
    { key: "ANALYZING_WITH_AI", label: "AI analysing your experience" },
    { key: "PROCESSING_RESULT", label: "Processing results" },
    { key: "SAVING_REPORT", label: "Saving your report" },
];

function stepState(stepKey, currentStatus, phase) {
    const order = STEPS.map(s => s.key);
    const currentIndex = order.indexOf(currentStatus);
    const thisIndex = order.indexOf(stepKey);

    if (phase === "completed") return "done";
    if (currentIndex === -1) return thisIndex === 0 ? "active" : "pending";
    if (thisIndex < currentIndex) return "done";
    if (thisIndex === currentIndex) return "active";
    return "pending";
}

function Uploadpage() {

    const { analysisURL } = useContext(UserContext)
    const navigate = useNavigate()

    const [role, setRole] = useState("")
    const [jobDescription, setJobDescription] = useState("")
    const [file, setFile] = useState(null)
    const [fileError, setFileError] = useState("")
    const fileInputRef = useRef(null)

    const {
        phase, status, progress, message, errorMessage, sessionExpired, isSlow, start, reset
    } = useAnalysisStatus({
        analysisURL,
        onComplete: () => navigate("/analysereport"),
    })

    const isAnalysing = phase === "starting" || phase === "polling" || phase === "completed"

    const onFileChange = (event) => {
        const selected = event.target.files[0]
        if (!selected) return;

        if (!ALLOWED_TYPES.includes(selected.type)) {
            setFileError("Upload a resume in pdf/doc format")
            setFile(null)
            event.target.value = ""
            return;
        }
        if (selected.size > MAX_FILE_BYTES) {
            setFileError("Upload a file less than 2MB")
            setFile(null)
            event.target.value = ""
            return;
        }

        setFileError("")
        setFile(selected)
    }

    const displayFileName = (name) => {
        if (name.length <= 24) return name;
        return name.substring(0, 11) + "..." + name.substring(name.length - 9);
    }

    const analysedoc = (event) => {
        event.preventDefault()

        if (role.trim() === "") {
            toast.warn("Role must not be empty")
            return;
        }
        if (!file) {
            toast.warn("Please upload the resume")
            return;
        }

        const formdata = new FormData()
        formdata.append("roles", role.trim())
        formdata.append("jobDescription", jobDescription)
        formdata.append("file", file)

        start(formdata)
    }

    const tryAgain = () => {
        reset()
    }

    const backToUpload = () => {
        reset()
        setFile(null)
        if (fileInputRef.current) fileInputRef.current.value = ""
    }

    useEffect(() => {
        if (sessionExpired) {
            toast.error(errorMessage || "Your session has expired. Please log in again.")
            navigate("/login")
        }
    }, [sessionExpired])

    return (
        <div className={Styles.container}>
            <Navbar
                rightSlot={
                    <button className="btn btn-ghost btn-sm" onClick={() => navigate("/")} disabled={isAnalysing}>
                        <i className="fa-solid fa-house" /> Home
                    </button>
                }
            />

            <div className={Styles.page}>

                {phase === "idle" &&
                    <>
                        <div className={Styles.pageHead}>
                            <span className="badge">Step 1 of 2</span>
                            <h1>Upload your resume</h1>
                            <p>Tell us the role you're targeting and we'll do the rest.</p>
                        </div>

                        <div className={Styles.layout}>
                            <div className={`${Styles.uploadcontainer} card`}>
                                <form onSubmit={analysedoc}>
                                    <label className={Styles.uploadcontainerlabel} htmlFor="roles">Role</label>
                                    <input
                                        type="text" autoComplete="off" placeholder="Ex : Software Engineer "
                                        id="roles" value={role} onChange={(e) => setRole(e.target.value)}
                                    />

                                    <label className={Styles.uploadcontainerlabel} htmlFor="jobDescription">
                                        Job Description <span className={Styles.optionalTag}>(optional)</span>
                                    </label>
                                    <textarea
                                        className={Styles.jdTextarea}
                                        id="jobDescription"
                                        maxLength={MAX_JD_LENGTH}
                                        rows={6}
                                        autoComplete="off"
                                        placeholder="Paste the job posting here to get more targeted job recommendations - skills, tech stack, experience level, and location get pulled out automatically."
                                        value={jobDescription}
                                        onChange={(e) => setJobDescription(e.target.value)}
                                    />
                                    <span className={Styles.jdCount}>{jobDescription.length}/{MAX_JD_LENGTH}</span>

                                    <label htmlFor="resume" className={Styles.fileinp}>
                                        <i className={`fa-solid fa-cloud-arrow-up ${Styles.fileIcon}`} />
                                        <p>Upload your resume here</p>
                                        <h5>Select File</h5>
                                        <span className={Styles.spn}>
                                            {file ? displayFileName(file.name) : "No file uploaded"}
                                        </span>
                                    </label>
                                    <input
                                        ref={fileInputRef}
                                        type="file" onChange={onFileChange} id="resume" hidden
                                        accept=".pdf,.doc,.docx"
                                    />
                                    {fileError && <p className={Styles.fileError}>{fileError}</p>}

                                    <button type="submit" className="btn btn-primary btn-block">
                                        <i className="fa-solid fa-wand-magic-sparkles" /> Analyse
                                    </button>
                                </form>
                            </div>

                            <div className={`${Styles.guidelinescontainer} card`}>
                                <h2>Guidelines</h2>
                                <ul>
                                    <li><i className="fa-solid fa-file-lines" /><span><b>File Format:</b> Upload your resume in PDF or DOC/DOCX format only.</span></li>
                                    <li><i className="fa-solid fa-weight-hanging" /><span><b>File Size:</b> Ensure your file size is less than 2 MB.</span></li>
                                    <li><i className="fa-solid fa-language" /><span><b>Language:</b> Upload your resume only in English</span></li>
                                    <li><i className="fa-solid fa-file-circle-plus" /><span><b>Job Description:</b> Optional, but pasting one in gets you more relevant job recommendations.</span></li>
                                </ul>
                            </div>
                        </div>
                    </>
                }

                {(phase === "starting" || phase === "polling" || phase === "completed") &&
                    <div className={Styles.progressWrap}>
                        <div className={`${Styles.progressCard} card`}>
                            <span className="badge">Analysing</span>
                            <h1>{phase === "completed" ? "Analysis Complete" : "Analysing your resume"}</h1>

                            <div className={Styles.progressBarTrack}>
                                <div className={Styles.progressBarFill} style={{ width: `${progress}%` }} />
                            </div>
                            <p className={Styles.progressPct}>{progress}%</p>

                            <ul className={Styles.stepList}>
                                {STEPS.map((step) => {
                                    const st = stepState(step.key, status, phase)
                                    return (
                                        <li key={step.key} className={Styles[`step_${st}`]}>
                                            <span className={Styles.stepIcon}>
                                                {st === "done" && <i className="fa-solid fa-circle-check" />}
                                                {st === "active" && <i className="fa-solid fa-circle-notch fa-spin" />}
                                                {st === "pending" && <i className="fa-regular fa-circle" />}
                                            </span>
                                            {step.label}
                                        </li>
                                    )
                                })}
                            </ul>

                            <p className={Styles.progressMsg}>{message}</p>

                            {isSlow && phase !== "completed" &&
                                <p className={Styles.slowNotice}>
                                    This is taking longer than usual. You can safely wait - your analysis is still running.
                                </p>
                            }
                        </div>
                    </div>
                }

                {phase === "failed" &&
                    <div className={Styles.progressWrap}>
                        <div className={`${Styles.errorCard} card`}>
                            <i className={`fa-solid fa-triangle-exclamation ${Styles.errorIcon}`} />
                            <h1>Analysis Failed</h1>
                            <p className={Styles.errorReason}>
                                We couldn't complete your resume analysis.
                                <br />
                                <span className={Styles.errorReasonDetail}>{errorMessage}</span>
                            </p>
                            <div className={Styles.errorActions}>
                                <button className="btn btn-primary" onClick={tryAgain}>
                                    <i className="fa-solid fa-rotate-right" /> Try Again
                                </button>
                                <button className="btn btn-secondary" onClick={backToUpload}>
                                    Back to Upload
                                </button>
                            </div>
                        </div>
                    </div>
                }
            </div>
        </div>
    )
}

export default Uploadpage
