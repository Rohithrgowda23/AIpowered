import Styles from "./Navbar.module.css";
import { Link } from "react-router-dom";

function Navbar({ rightSlot = null, brandHref = "/" }) {
    return (
        <header className={Styles.nav}>
            <div className={Styles.navInner}>
                <Link to={brandHref} className={Styles.brand}>
                    <span className={Styles.brandMark}>RA</span>
                    <span className={Styles.brandText}>Resume Analyser</span>
                </Link>
                <div className={Styles.actions}>{rightSlot}</div>
            </div>
        </header>
    );
}

export default Navbar;
