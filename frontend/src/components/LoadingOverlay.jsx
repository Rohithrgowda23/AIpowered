import Styles from "./LoadingOverlay.module.css";


function LoadingOverlay({ id = "animate", label = "Loading" }) {
    return (
        <div className={Styles.overlay} id={id}>
            <div className={Styles.ring}>
                <span />
                <span />
                <span />
            </div>
            <h1 className={Styles.label}>{label}</h1>
        </div>
    );
}

export default LoadingOverlay;
