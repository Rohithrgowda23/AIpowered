import Styles from "./Footer.module.css";

function Footer() {
    return (
        <footer className={Styles.footer}>
            <div className={Styles.inner}>
                <div className={Styles.brandCol}>
                    <div className={Styles.brand}>
                        <span className={Styles.brandMark}>RA</span>
                        <span>Resume Analyser</span>
                    </div>
                    <p className={Styles.tagline}>
                        AI-powered resume feedback, ATS scoring, and job matching — built to help you land the interview.
                    </p>
                </div>

                <div className={Styles.linkCol}>
                    <h4>Product</h4>
                    <a href="#features">Features</a>
                    <a href="#how-it-works">How it works</a>
                </div>

                <div className={Styles.linkCol}>
                    <h4>Account</h4>
                    <a href="/login">Login</a>
                    <a href="/login">Create account</a>
                </div>
            </div>

            <div className={Styles.bottom}>
                <span>© {new Date().getFullYear()} Resume Analyser. All rights reserved.</span>
            </div>
        </footer>
    );
}

export default Footer;
