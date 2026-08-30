import { useContext, useEffect, useState } from "react"
import Styles from "./analyse.module.css"
import { Heat } from "@alptugidin/react-circular-progress-bar"
import { UserContext } from "../context/usercontext"
import { useNavigate } from "react-router-dom"
import Navbar from "../components/Navbar.jsx"
import LoadingOverlay from "../components/LoadingOverlay.jsx"

function Analyse() {
    const navigate = useNavigate()
    const [score, setscore] = useState(0)
    const [atsscore, setatsscore] = useState(0)
    const [summary, setsummary] = useState("")
    const [experienceLevel, setexperienceLevel] = useState("")
    const [skills, setskills] = useState([])
    const [missingSkills, setmissingSkills] = useState([])
    const [strengths, setstrengths] = useState([])
    const [weaknesses, setweaknesses] = useState([])
    const [interviewTips, setinterviewTips] = useState([])
    const [pros, setpros] = useState([])
    const [cons, setcons] = useState([])
    const [sug, setsug] = useState([])
    const [jobs, setjobs] = useState([])


    const [jobRole, setjobRole] = useState("")
    const [jobDescription, setjobDescription] = useState("")
    const [jdExperienceLevel, setjdExperienceLevel] = useState("")
    const [jdSkills, setjdSkills] = useState([])
    const [jdTechnologies, setjdTechnologies] = useState([])
    const [jdKeywords, setjdKeywords] = useState([])
    const [jdResponsibilities, setjdResponsibilities] = useState([])
    const [jdQualifications, setjdQualifications] = useState([])
    const [jdLocation, setjdLocation] = useState("")
    const [jobSearchMessage, setjobSearchMessage] = useState("")

    const { reportURL } = useContext(UserContext)
    const [status, setstatus] = useState("loading") // "loading" | "done" | "error"

    useEffect(() => {
        document.getElementById("animate").style.display = "flex"

        fetch(`${reportURL}/latest`, {
            headers: { "Authorization": `Bearer ${localStorage.getItem("token")}` }
        })
            .then(res => {
                document.getElementById("animate").style.display = "none"
                if (res.ok) return res.json()
                setstatus("error")
            })
            .then(data => {
                if (data != null) {
                    setscore(data.score ?? 0)
                    setatsscore(data.atsoptimizationscore ?? 0)
                    setsummary(data.summary ?? "")
                    setexperienceLevel(data.experienceLevel ?? "")
                    setskills(Array.isArray(data.skills) ? data.skills : [])
                    setmissingSkills(Array.isArray(data.missingSkills) ? data.missingSkills : [])
                    setstrengths(Array.isArray(data.strengths) ? data.strengths : [])
                    setweaknesses(Array.isArray(data.weaknesses) ? data.weaknesses : [])
                    setinterviewTips(Array.isArray(data.interviewTips) ? data.interviewTips : [])
                    setpros(Array.isArray(data.pros) ? data.pros : [])
                    setcons(Array.isArray(data.cons) ? data.cons : [])
                    setsug(Array.isArray(data.suggestions) ? data.suggestions : [])
                    setjobs(Array.isArray(data.jobs) ? data.jobs : [])

                    setjobRole(data.jobRole ?? "")
                    setjobDescription(data.jobDescription ?? "")
                    setjdExperienceLevel(data.jdExperienceLevel ?? "")
                    setjdSkills(Array.isArray(data.jdSkills) ? data.jdSkills : [])
                    setjdTechnologies(Array.isArray(data.jdTechnologies) ? data.jdTechnologies : [])
                    setjdKeywords(Array.isArray(data.jdKeywords) ? data.jdKeywords : [])
                    setjdResponsibilities(Array.isArray(data.jdResponsibilities) ? data.jdResponsibilities : [])
                    setjdQualifications(Array.isArray(data.jdQualifications) ? data.jdQualifications : [])
                    setjdLocation(data.jdLocation ?? "")
                    setjobSearchMessage(data.jobSearchMessage ?? "")

                    setstatus("done")
                } else {
                    setstatus("error")
                }
            })
            .catch(error => {
                console.log(error)
                document.getElementById("animate").style.display = "none"
                setstatus("error")
            })
    }, [])

    const hasJdInsights = jdSkills.length > 0 || jdTechnologies.length > 0 || jdKeywords.length > 0
        || jdResponsibilities.length > 0 || jdQualifications.length > 0 || jdExperienceLevel || jdLocation

    return (
        <div className={Styles.container}>
            <Navbar
                rightSlot={
                    <button className="btn btn-primary btn-sm" onClick={() => navigate("/uploaddoc")}>
                        <i className="fa-solid fa-rotate" /> New Analysis
                    </button>
                }
            />

            <LoadingOverlay label="Preparing Report" />

            {status === "done" &&
                <div className={Styles.doc}>
                    <div className={Styles.pageHead}>
                        <span className="badge">Your Report</span>
                        <h1>Resume Analysis</h1>
                    </div>

                    <div className={`${Styles.report} card`}>
                        <div className={Styles.sc1}>
                            <Heat
                                progress={score}
                                range={{ from: 0, to: 100 }}
                                sign={{ value: '', position: 'end' }}
                                showValue={true}
                                revertBackground={true}
                                text={'Overall Score'}
                                sx={{
                                    barWidth: 7,
                                    bgColor: ' #2c2c2cb1',
                                    bgStrokeColor: '#ffffff',
                                    valueSize: 13,
                                    textSize: 10,
                                    valueFamily: 'Poppins',
                                    textFamily: 'Poppins',
                                    valueWeight: 'normal',
                                    textWeight: 'normal',
                                    textColor: '#ffffffff',
                                    valueColor: '#ffffffff',
                                    loadingTime: 1000,
                                    strokeLinecap: 'round',
                                    valueAnimation: true,
                                }}
                            />
                        </div>
                        <div className={Styles.sc2}>
                            <Heat
                                progress={atsscore}
                                range={{ from: 0, to: 100 }}
                                sign={{ value: '', position: 'end' }}
                                showValue={true}
                                revertBackground={true}
                                text={'ATS optimization score'}
                                sx={{
                                    barWidth: 7,
                                    bgColor: ' #2c2c2cb1',
                                    bgStrokeColor: '#ffffff',
                                    valueSize: 13,
                                    textSize: 7,
                                    valueFamily: 'Poppins',
                                    textFamily: 'Poppins',
                                    valueWeight: 'normal',
                                    textWeight: 'normal',
                                    textColor: '#ffffffff',
                                    valueColor: '#ffffffff',
                                    loadingTime: 1000,
                                    strokeLinecap: 'round',
                                    valueAnimation: true,
                                }}
                            />
                        </div>
                    </div>

                    {(summary || experienceLevel) &&
                        <div className={`${Styles.summaryCard} card`}>
                            {experienceLevel &&
                                <span className={Styles.expBadge}>{experienceLevel} level</span>
                            }
                            {summary && <p className={Styles.summaryText}>{summary}</p>}
                        </div>
                    }


                    {(jobRole || jobDescription) &&
                        <div className={`${Styles.jdCard} card`}>
                            <h2>Target Role</h2>
                            {jobRole && <p className={Styles.jdRole}>{jobRole}</p>}
                            {jobDescription &&
                                <>
                                    <h3 className={Styles.jdSubHeading}>Job Description</h3>
                                    <p className={Styles.jdText}>{jobDescription}</p>
                                </>
                            }
                        </div>
                    }

                    {hasJdInsights &&
                        <div className={`${Styles.jdCard} card`}>
                            <h2>Job Description Insights</h2>
                            <div className={Styles.jdMetaRow}>
                                {jdExperienceLevel &&
                                    <span className={Styles.expBadge}>{jdExperienceLevel} level required</span>
                                }
                                {jdLocation &&
                                    <span className={Styles.expBadge}>{jdLocation}</span>
                                }
                            </div>

                            {(jdSkills.length > 0 || jdTechnologies.length > 0 || jdKeywords.length > 0) &&
                                <div className={Styles.tagWrap}>
                                    {jdSkills.map((item, index) => <span className={Styles.tagJd} key={`skill-${index}`}>{item}</span>)}
                                    {jdTechnologies.map((item, index) => <span className={Styles.tagJd} key={`tech-${index}`}>{item}</span>)}
                                    {jdKeywords.map((item, index) => <span className={Styles.tagJdKeyword} key={`kw-${index}`}>{item}</span>)}
                                </div>
                            }

                            {jdResponsibilities.length > 0 &&
                                <>
                                    <h3 className={Styles.jdSubHeading}>Key Responsibilities</h3>
                                    <ul className={Styles.jdList}>
                                        {jdResponsibilities.map((item, index) => <li key={index}>{item}</li>)}
                                    </ul>
                                </>
                            }

                            {jdQualifications.length > 0 &&
                                <>
                                    <h3 className={Styles.jdSubHeading}>Preferred Qualifications</h3>
                                    <ul className={Styles.jdList}>
                                        {jdQualifications.map((item, index) => <li key={index}>{item}</li>)}
                                    </ul>
                                </>
                            }
                        </div>
                    }

                    {(skills.length > 0 || missingSkills.length > 0) &&
                        <div className={Styles.skillsRow}>
                            {skills.length > 0 &&
                                <div className={`${Styles.skillsBox} card`}>
                                    <h2>Skills Found</h2>
                                    <div className={Styles.tagWrap}>
                                        {skills.map((item, index) =>
                                            <span className={Styles.tagGood} key={index}>{item}</span>
                                        )}
                                    </div>
                                </div>
                            }
                            {missingSkills.length > 0 &&
                                <div className={`${Styles.skillsBox} card`}>
                                    <h2>Missing Skills</h2>
                                    <div className={Styles.tagWrap}>
                                        {missingSkills.map((item, index) =>
                                            <span className={Styles.tagMissing} key={index}>{item}</span>
                                        )}
                                    </div>
                                </div>
                            }
                        </div>
                    }

                    <div className={Styles.rev}>
                        {strengths.length > 0 &&
                            <div className={`${Styles.pros} card`}>
                                <h2><i className="fa-solid fa-circle-check" /> Strengths</h2>
                                <ul>
                                    {strengths.map((item, index) => <li key={index}>{item}</li>)}
                                </ul>
                            </div>
                        }
                        {weaknesses.length > 0 &&
                            <div className={`${Styles.cons} card`}>
                                <h2><i className="fa-solid fa-circle-exclamation" /> Weaknesses</h2>
                                <ul>
                                    {weaknesses.map((item, index) => <li key={index}>{item}</li>)}
                                </ul>
                            </div>
                        }
                        {pros.length > 0 &&
                            <div className={`${Styles.pros} card`}>
                                <h2><i className="fa-solid fa-thumbs-up" /> Pros</h2>
                                <ul>
                                    {pros.map((item, index) => <li key={index}>{item}</li>)}
                                </ul>
                            </div>
                        }
                        {cons.length > 0 &&
                            <div className={`${Styles.cons} card`}>
                                <h2><i className="fa-solid fa-thumbs-down" /> Cons</h2>
                                <ul>
                                    {cons.map((item, index) => <li key={index}>{item}</li>)}
                                </ul>
                            </div>
                        }
                        {sug.length > 0 &&
                            <div className={`${Styles.sug} card`}>
                                <h2><i className="fa-solid fa-lightbulb" /> Tips to enhance</h2>
                                <ul>
                                    {sug.map((item, index) => <li key={index}>{item}</li>)}
                                </ul>
                            </div>
                        }
                        {interviewTips.length > 0 &&
                            <div className={`${Styles.sug} card`}>
                                <h2><i className="fa-solid fa-comments" /> Interview Preparation</h2>
                                <ul>
                                    {interviewTips.map((item, index) => <li key={index}>{item}</li>)}
                                </ul>
                            </div>
                        }
                        {jobs.length > 0 &&
                            <div className={`${Styles.jobs} card`}>
                                <h2><i className="fa-solid fa-briefcase" /> Suggested Jobs</h2>
                                {jobs.map((item, index) =>
                                    <div className={Styles.jobidiv} key={index}>
                                        <h3 className={Styles.jobtitle}>{item.title}</h3>
                                        <div className={Styles.jobMeta}>
                                            <span className={Styles.com}><i className="fa-solid fa-building" /> {item.company?.display_name?.trim() || "Not specified"}</span>
                                            <span className={Styles.loc}><i className="fa-solid fa-location-dot" /> {item.location?.display_name?.trim() || "Not specified"}</span>
                                            <span className={Styles.cat}><i className="fa-solid fa-tag" /> {item.category?.label?.trim() || "Not specified"}</span>
                                        </div>
                                        <p className={Styles.jobdes}>{item.description}</p>
                                        <a className={`btn btn-secondary btn-sm ${Styles.joblink}`} href={item.redirect_url} target="_blank" rel="noreferrer">Apply now <i className="fa-solid fa-arrow-up-right-from-square" /></a>
                                    </div>
                                )}
                            </div>
                        }

                        {jobs.length === 0 && jobSearchMessage &&
                            <div className={`${Styles.jobs} card`}>
                                <h2><i className="fa-solid fa-briefcase" /> Suggested Jobs</h2>
                                <p className={Styles.noJobsMsg}>{jobSearchMessage}</p>
                            </div>
                        }
                    </div>
                </div>
            }

            {status === "error" &&
                <div className={Styles.errWrap}>
                    <i className="fa-solid fa-triangle-exclamation" />
                    <h1 className={Styles.errinfo}>
                        Something went wrong, please try again after some time!
                    </h1>
                </div>
            }
        </div>
    )
}

export default Analyse
