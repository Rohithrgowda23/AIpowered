import { useContext, useState, useEffect } from "react";
import Styles from "./login.module.css";
import { toast } from "react-toastify";
import { UserContext } from "../context/usercontext";
import { useNavigate, Link } from "react-router-dom";
import GoogleButton from "../googlebtn.jsx";
import Navbar from "../components/Navbar.jsx";

function Login() {
    const navigate = useNavigate();

    const [islogin, setislogin] = useState(true);

    const {
        backendURL,
        setisprevious,
        setusername,
        setislogged,
        islogged
    } = useContext(UserContext);

    const [name, setname] = useState("");
    const [email, setemail] = useState("");
    const [password, setpassword] = useState("");
    const [confirmpassword, setconfirmpassword] = useState("");

    // OTP
    const [verifyotp, setverifyotp] = useState("");
    const [otpsent, setotpsent] = useState(false);

    const [isloading, setisloading] = useState(false);

    const [showpass, setshowpass] = useState(false);
    const [showconfirmpass, setshowconfirmpass] = useState(false);

    useEffect(() => {
        if (islogged) {
            navigate("/");
        }
    }, [islogged, navigate]);

    function validateEmail(email) {
        const emailregex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        return emailregex.test(email);
    }

    async function sendOtp() {

        if (name.trim() === "") {
            toast.warn("Username must not be empty");
            return;
        }

        if (email.trim() === "") {
            toast.warn("Email must not be empty");
            return;
        }

        if (!validateEmail(email.trim())) {
            toast.warn("Invalid Email");
            return;
        }

        if (password.length < 6) {
            toast.warn("Password must contain at least 6 characters");
            return;
        }

        if (password !== confirmpassword) {
            toast.warn("Passwords don't match");
            return;
        }

        setisloading(true);

        try {

            const response = await fetch(`${backendURL}/verify-email`, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({
                    username: name.trim(),
                    email: email.trim()
                })
            });

            const text = await response.text();

            if (response.ok) {
                toast.success(text);
                setotpsent(true);
            }
            else {
                toast.error(text);
            }

        }
        catch {
            toast.error("Unable to send OTP");
        }

        setisloading(false);
    }

    const submit = async (event) => {
        event.preventDefault();

        if (!islogin) {

            if (name.trim() === "") {
                toast.warn("Username must not be empty");
                return;
            }

            if (email.trim() === "") {
                toast.warn("Email must not be empty");
                return;
            }

            if (!validateEmail(email.trim())) {
                toast.warn("Invalid Email");
                return;
            }

            if (password.length < 6) {
                toast.warn("Password must contain at least 6 characters");
                return;
            }

            if (password !== confirmpassword) {
                toast.warn("Passwords don't match");
                return;
            }

            if (!otpsent) {
                toast.warn("Please send OTP first");
                return;
            }

            if (verifyotp.trim() === "") {
                toast.warn("Please enter OTP");
                return;
            }

            setisloading(true);

            try {

                const response = await fetch(`${backendURL}/register`, {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/json"
                    },
                    body: JSON.stringify({
                        username: name.trim(),
                        email: email.trim(),
                        password: password,
                        verifyotp: verifyotp.trim()
                    })
                });

                const text = await response.text();

                if (response.ok) {

                    toast.success(text);

                    setname("");
                    setemail("");
                    setpassword("");
                    setconfirmpassword("");
                    setverifyotp("");

                    setshowpass(false);
                    setshowconfirmpass(false);

                    setotpsent(false);
                    setislogin(true);

                } else {

                    toast.error(text);

                }

            } catch {
                toast.error("Registration failed");
            }

            setisloading(false);

        } else {

            if (email.trim() === "") {
                toast.warn("Email must not be empty");
                return;
            }

            if (!validateEmail(email.trim())) {
                toast.warn("Invalid Email");
                return;
            }

            if (password.length < 6) {
                toast.warn("Password must contain at least 6 characters");
                return;
            }

            setisloading(true);

            fetch(`${backendURL}/login`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ email: email.trim(), password: password })
            })
                .then(response => response.ok ? response.json() : null)
                .then(data => {
                    if (data) {
                        localStorage.setItem("token", data.token);
                        setislogged(true);
                        setusername(data.username);
                        setisprevious(data.isPrevious);
                        navigate("/");
                    } else {
                        toast.error("Invalid credentials");
                    }
                })
                .catch(() => toast.error("Login failed"))
                .finally(() => setisloading(false));

        }

    };

    function switchmth() {

        setname("");
        setemail("");
        setpassword("");
        setconfirmpassword("");
        setverifyotp("");

        setshowpass(false);
        setshowconfirmpass(false);

        setotpsent(false);

        setislogin(!islogin);

    }

    return (
        <div className={Styles.container}>

            <Navbar
                rightSlot={
                    <Link to="/">
                        <button className="btn btn-ghost btn-sm">
                            <i className="fa-solid fa-arrow-left" /> Back to home
                        </button>
                    </Link>
                }
            />

            <div className={Styles.split}>
                <div className={Styles.showcase}>
                    <div className={Styles.showcaseInner}>
                        <span className="badge">AI-powered resume feedback</span>
                        <h2>
                            {islogin ? "Welcome back." : "Create your account."}
                            <br />
                            Your next interview starts here.
                        </h2>
                        <p>
                            Instant ATS scoring, skill-gap analysis, and tailored job matches —
                            all built from your actual resume.
                        </p>
                        <ul className={Styles.showcaseList}>
                            <li><i className="fa-solid fa-check" /> Free to try, takes under a minute</li>
                            <li><i className="fa-solid fa-check" /> Private, deletable account data</li>
                            <li><i className="fa-solid fa-check" /> Tailored job recommendations</li>
                        </ul>
                    </div>
                </div>

                <div className={Styles.formSide}>
                    <div className={`${Styles.logincontainer} card reveal`}>

                        <h1>{islogin ? "Login" : "Signup"}</h1>
                        <p className={Styles.subhead}>
                            {islogin ? "Enter your details to continue." : "Let's get your account set up."}
                        </p>

                        {!islogin &&
                            <input
                                className={Styles.logincontainerinput}
                                type="text"
                                maxLength={20}
                                autoComplete="off"
                                placeholder="Username"
                                value={name}
                                onChange={(e) => setname(e.target.value)}
                            />
                        }

                        <input
                            className={Styles.logincontainerinput}
                            type="email"
                            autoComplete="off"
                            placeholder="Email"
                            value={email}
                            onChange={(e) => setemail(e.target.value)}
                        />

                        <div className={Styles.passdiv}>
                            <input
                                type={showpass ? "text" : "password"}
                                autoComplete="off"
                                placeholder="Password"
                                value={password}
                                onChange={(e) => setpassword(e.target.value)}
                            />
                            <i
                                className={`fa-solid ${showpass ? "fa-eye-slash" : "fa-eye"}`}
                                onClick={() => setshowpass(!showpass)}
                            />
                        </div>

                        {!islogin &&
                            <div className={Styles.passdiv}>
                                <input
                                    type={showconfirmpass ? "text" : "password"}
                                    autoComplete="off"
                                    placeholder="Confirm Password"
                                    value={confirmpassword}
                                    onChange={(e) => setconfirmpassword(e.target.value)}
                                />
                                <i
                                    className={`fa-solid ${showconfirmpass ? "fa-eye-slash" : "fa-eye"}`}
                                    onClick={() => setshowconfirmpass(!showconfirmpass)}
                                />
                            </div>
                        }

                        {!islogin && otpsent &&
                            <input
                                className={Styles.logincontainerinput}
                                type="text"
                                maxLength={6}
                                placeholder="Enter OTP"
                                value={verifyotp}
                                onChange={(e) => setverifyotp(e.target.value)}
                            />
                        }

                        {islogin &&
                            <Link className={Styles.linkdis} to="/forgotpassword">
                                <p className={Styles.forgetpass}>Forgot password?</p>
                            </Link>
                        }

                        {!islogin && !otpsent &&
                            <button
                                className={`btn btn-secondary btn-block ${Styles.actionBtn}`}
                                onClick={sendOtp}
                                disabled={isloading}
                            >
                                {isloading ? "Sending OTP..." : "Send OTP"}
                            </button>
                        }

                        <button
                            className={`btn btn-primary btn-block ${Styles.actionBtn}`}
                            onClick={submit}
                            disabled={isloading}
                        >
                            {isloading
                                ? "Loading..."
                                : islogin
                                    ? "Login"
                                    : "Signup"}
                        </button>

                        <p className={Styles.switchLine}>
                            {islogin
                                ? "Doesn't have an account? "
                                : "Already have an account? "}

                            <span
                                className={Styles.logincontainerspan}
                                onClick={switchmth}
                            >
                                {islogin ? "Signup" : "Login"}
                            </span>
                        </p>

                        <hr className={Styles.ghr} />

                        <GoogleButton />

                    </div>
                </div>
            </div>

        </div>
    );
}

export default Login;
