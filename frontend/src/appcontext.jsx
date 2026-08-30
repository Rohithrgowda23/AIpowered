import { useEffect, useState } from "react";
import { UserContext } from "./context/usercontext";

function AppContext({ children }) {
    const apiBase = import.meta.env.VITE_API_URL;

    const backendURL = apiBase + "/api/auth";       // -> auth-service
    const analysisURL = apiBase + "/api/analyses";  // -> analysis-service
    const reportURL = apiBase + "/api/reports";     // -> report-service
    const jobURL = apiBase + "/api/jobs";           // -> job-service

    const serviceURL = analysisURL;

    const [islogged, setislogged] = useState(false);
    const [isprevious, setisprevious] = useState(false);
    const [username, setusername] = useState("");
    const [isauthenticated, setisauthenticated] = useState(false);

    useEffect(() => {
        const params = new URLSearchParams(window.location.search);
        const urlToken = params.get("token");
        if (urlToken) {
            localStorage.setItem("token", urlToken);
            window.history.replaceState({}, document.title, window.location.pathname);
        }

        fetch(`${backendURL}/validate-token`, {
            method: "POST",
            headers: {
                "Authorization": `Bearer ${localStorage.getItem("token")}`
            },
        })
            .then((res) => res.ok ? res.json() : null)
            .then((data) => {
                if (data) {
                    setusername(data.username);
                    setisprevious(data.isPrevious);
                    setislogged(true);
                }
                setisauthenticated(true);
            })
            .catch(() => setisauthenticated(true));
    }, [backendURL]);

    return (
        <UserContext.Provider
            value={{
                islogged,
                setislogged,
                isprevious,
                setisprevious,
                username,
                setusername,
                backendURL,
                serviceURL,
                analysisURL,
                reportURL,
                jobURL,
                isauthenticated,
            }}
        >
            {children}
        </UserContext.Provider>
    );
}

export default AppContext;
