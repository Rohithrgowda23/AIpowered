import Styles from "./home.module.css";
import { useContext, useEffect, useState } from "react";
import { UserContext } from "../context/usercontext";
import { Link, useNavigate } from "react-router-dom";
import { toast } from "react-toastify";
import Footer from "../components/Footer.jsx";

const FEATURES = [
    {
        icon: "fa-solid fa-gauge-high",
        title: "Instant ATS Score",
        desc: "See exactly how applicant tracking systems read your resume before a recruiter ever does.",
    },
    {
        icon: "fa-solid fa-magnifying-glass-chart",
        title: "Skill Gap Analysis",
        desc: "We surface the skills you already show off, and the ones missing for your target role.",
    },
    {
        icon: "fa-solid fa-file-lines",
        title: "Job Description Matching",
        desc: "Paste a job posting and get tailored insights on keywords, responsibilities, and qualifications.",
    },
    {
        icon: "fa-solid fa-briefcase",
        title: "Curated Job Matches",
        desc: "Get relevant open roles pulled in automatically based on your resume and target position.",
    },
    {
        icon: "fa-solid fa-comments",
        title: "Interview Prep Tips",
        desc: "Walk into interviews ready, with tips generated specifically from your resume and role.",
    },
    {
        icon: "fa-solid fa-shield-halved",
        title: "Private by Default",
        desc: "Your resume and reports are tied to your account only — delete your data any time.",
    },
];

const STEPS = [
    {
        num: "01",
        title: "Upload your resume",
        desc: "Drop in your PDF or DOC/DOCX file along with the role you're targeting.",
    },
    {
        num: "02",
        title: "We analyse it instantly",
        desc: "Our engine scores your resume, checks ATS compatibility, and compares it against the role.",
    },
    {
        num: "03",
        title: "Get a full report",
        desc: "Review strengths, gaps, tailored suggestions, and job matches — all in one dashboard.",
    },
];

function Home() {
    const navigate = useNavigate();

    const {
        islogged,
        username,
        isprevious,
        backendURL,
        setusername,
        setislogged,
        setisprevious,
    } = useContext(UserContext);

    const [isshow, setshow] = useState(false);
    const [isloading, setisloading] = useState(false);
    const [delloading, setdelloading] = useState(false);

    useEffect(() => {
        const func = (event) => {
            if (event.target.id !== "menu") {
                setshow(false);
            }
        };

        window.addEventListener("click", func);

        return () => window.removeEventListener("click", func);
    }, []);

    const toggle = () => {
        setshow(!isshow);
    };

    const logout = () => {
        setisloading(true);

        fetch(`${backendURL}/logout`, {
            method: "POST",
            headers: {
                "Authorization": `Bearer ${localStorage.getItem("token")}`
            },
        })
            .then((response) => {
                if (response.ok) {
                    localStorage.removeItem("token");
                    setusername("");
                    setislogged(false);
                    setisprevious(false);
                    toast.success("Successfully Logged out");
                    navigate("/login");
                } else {
                    toast.error("unauthorised access");
                }
            })
            .catch(() => toast.error("Network Error"))
            .finally(() => setisloading(false));
    };

    const upnavigate = () => {
        if (islogged) {
            navigate("/uploaddoc");
        } else {
            navigate("/login");
        }
    };

    const confirmagain = () => {
        document.getElementById("confirmdivdel").style.display = "flex";
    };

    const closedeldiv = () => {
        document.getElementById("confirmdivdel").style.display = "none";
    };

    const delaccount = () => {
        setdelloading(true);

        fetch(`${backendURL}/delete-account`, {
            method: "DELETE",
            headers: {
                "Authorization": `Bearer ${localStorage.getItem("token")}`
            },
        })
            .then((res) => {
                if (res.ok) {
                    localStorage.removeItem("token");
                    toast.success("Account deleted");
                    setusername("");
                    setislogged(false);
                    setisprevious(false);
                    navigate("/login");
                } else {
                    toast.error("Failed to delete account");
                }
            })
            .catch(() => toast.error("Network Error"))
            .finally(() => setdelloading(false));
    };

    return (
        <div className={Styles.container}>
            <header className={Styles.nav}>
                <div className={Styles.navInner}>
                    <div className={Styles.brand}>
                        <span className={Styles.brandMark}>RA</span>
                        <h1>Resume Analyser</h1>
                    </div>

                    {!islogged ? (
                        <Link to="/login">
                            <button className="btn btn-primary btn-sm">Login</button>
                        </Link>
                    ) : (
                        <h3 id="menu" onClick={toggle} className={Styles.profile}>
                            {username?.[0]?.toUpperCase()}
                        </h3>
                    )}
                </div>

                {isshow && islogged ? (
                    <div id="menu" className={Styles.profilemenu}>
                        <h2 id="menu">{username}</h2>
                        <hr id="menu" />

                        <div id="menu" className={Styles.pmenusec}>
                            <button id="menu" className="btn btn-secondary btn-sm btn-block" onClick={logout} disabled={isloading}>
                                <i id="menu" className="fa-solid fa-arrow-right-from-bracket" /> Logout
                            </button>

                            <button id="menu" className="btn btn-danger btn-sm btn-block" onClick={confirmagain} disabled={isloading}>
                                <span id="menu" className={Styles.del}>
                                    <i id="menu" className="fa-solid fa-trash" /> Delete account
                                </span>
                            </button>
                        </div>
                    </div>
                ) : null}
            </header>

            {/* HERO */}
            <section className={`${Styles.hero} reveal`}>
                <div className={Styles.heroInner}>
                    <div className={Styles.heroCopy}>
                        <span className="badge">
                            <i className="fa-solid fa-sparkles" /> AI-powered resume feedback
                        </span>

                        <h1 className={Styles.heroTitle}>
                            {islogged
                                ? <>Welcome back, <span className={Styles.grad}>{username}</span>.</>
                                : <>Land more interviews with a resume that <span className={Styles.grad}>actually gets read.</span></>
                            }
                        </h1>

                        <p className={Styles.heroDesc}>
                            {islogged
                                ? "Upload a fresh resume for a new report, or jump back into your latest analysis."
                                : "Upload your resume and get instant insights on ATS score, keywords, skills, formatting, and job matches — in seconds."
                            }
                        </p>

                        <div className={Styles.btncontainer}>
                            <button className="btn btn-primary" disabled={isloading} onClick={upnavigate}>
                                <i className="fa-solid fa-file-arrow-up" /> Analyse Resume
                            </button>

                            {isprevious ? (
                                <button className="btn btn-secondary" disabled={isloading} onClick={() => navigate("/analysereport")}>
                                    <i className="fa-solid fa-clock-rotate-left" /> Previous Analysis
                                </button>
                            ) : !islogged ? (
                                <a href="#how-it-works" className="btn btn-secondary">
                                    See how it works
                                </a>
                            ) : null}
                        </div>
                    </div>
                </div>
            </section>

            {!islogged && (
                <>
                    {/* FEATURES */}
                    <section id="features" className={Styles.section}>
                        <div className={Styles.sectionHead}>
                            <span className="badge">Features</span>
                            <h2>Everything you need to strengthen your resume</h2>
                            <p>Built to give you the same signals recruiters and ATS software look for.</p>
                        </div>

                        <div className={Styles.featureGrid}>
                            {FEATURES.map((f, i) => (
                                <div className={`${Styles.featureCard} card`} key={i}>
                                    <div className={Styles.featureIcon}>
                                        <i className={f.icon} />
                                    </div>
                                    <h3>{f.title}</h3>
                                    <p>{f.desc}</p>
                                </div>
                            ))}
                        </div>
                    </section>

                    {/* HOW IT WORKS */}
                    <section id="how-it-works" className={Styles.section}>
                        <div className={Styles.sectionHead}>
                            <span className="badge">How it works</span>
                            <h2>From upload to insights in under a minute</h2>
                        </div>

                        <div className={Styles.steps}>
                            {STEPS.map((s) => (
                                <div className={Styles.step} key={s.num}>
                                    <span className={Styles.stepNum}>{s.num}</span>
                                    <h3>{s.title}</h3>
                                    <p>{s.desc}</p>
                                </div>
                            ))}
                        </div>
                    </section>

                    {/* CTA */}
                    <section id="about" className={Styles.cta}>
                        <div className={Styles.ctaInner}>
                            <h2>Ready to see what your resume is really saying?</h2>
                            <p>Free to try. Takes less than a minute.</p>
                            <Link to="/login">
                                <button className="btn btn-primary">
                                    <i className="fa-solid fa-arrow-right" /> Get started
                                </button>
                            </Link>
                        </div>
                    </section>

                    <Footer />
                </>
            )}

            <div className={Styles.delcontainer} id="confirmdivdel">
                <div className={`${Styles.confirmcontainer} card`}>
                    <p>
                        Are you sure want to delete your account ?
                        <br />
                        <br />
                        It will permanently removes all your data and can't be recovered.
                    </p>

                    <div className={Styles.confirmationbtns}>
                        <button
                            className="btn btn-danger"
                            disabled={delloading}
                            onClick={delaccount}
                        >
                            {delloading ? "Deleting ..." : "Delete"}
                        </button>

                        <button
                            className="btn btn-secondary"
                            disabled={delloading}
                            onClick={closedeldiv}
                        >
                            Not now
                        </button>
                    </div>
                </div>
            </div>
        </div>
    );
}

export default Home;