import { useCallback, useEffect, useRef, useState } from "react";
import { startAnalysis, getAnalysisStatus, ApiError } from "../api/analysisApi";

const POLL_INTERVAL_MS = 1500;
const SLOW_AFTER_MS = 20000;

const INITIAL_STATE = {
    phase: "idle",
    status: null,
    progress: 0,
    message: "",
    errorMessage: null,
    sessionExpired: false,
    isSlow: false,
};

export function useAnalysisStatus({ analysisURL, onComplete }) {
    const [state, setState] = useState(INITIAL_STATE);

    const timeoutRef = useRef(null);
    const jobIdRef = useRef(null);
    const startedAtRef = useRef(null);
    const mountedRef = useRef(true);

    useEffect(() => {
        mountedRef.current = true;
        return () => {
            mountedRef.current = false;
            if (timeoutRef.current) {
                clearTimeout(timeoutRef.current);
                timeoutRef.current = null;
            }
        };
    }, []);

    const clearPending = () => {
        if (timeoutRef.current) {
            clearTimeout(timeoutRef.current);
            timeoutRef.current = null;
        }
    };

    const poll = useCallback((analysisId) => {
        if (!mountedRef.current || jobIdRef.current !== analysisId) return;

        getAnalysisStatus(analysisURL, analysisId)
            .then((dto) => {
                if (!mountedRef.current || jobIdRef.current !== analysisId) return; // superseded, drop it

                const isSlow = Date.now() - startedAtRef.current > SLOW_AFTER_MS;

                if (dto.status === "COMPLETED") {
                    setState((s) => ({
                        ...s,
                        phase: "completed",
                        status: dto.status,
                        progress: 100,
                        message: dto.message || "Analysis complete",
                        errorMessage: null,
                        isSlow: false,
                    }));
                    if (onComplete) onComplete();
                    return;
                }

                if (dto.status === "FAILED") {
                    setState((s) => ({
                        ...s,
                        phase: "failed",
                        status: dto.status,
                        errorMessage: dto.errorMessage || "Analysis failed. Please try again.",
                        isSlow: false,
                    }));
                    return;
                }

                setState((s) => ({
                    ...s,
                    phase: "polling",
                    status: dto.status,
                    progress: dto.progress,
                    message: dto.message || s.message,
                    isSlow,
                }));

                timeoutRef.current = setTimeout(() => poll(analysisId), POLL_INTERVAL_MS);
            })
            .catch((err) => {
                if (!mountedRef.current || jobIdRef.current !== analysisId) return;

                const sessionExpired = err instanceof ApiError && err.status === 401;

                if (sessionExpired) {
                    setState((s) => ({
                        ...s,
                        phase: "failed",
                        errorMessage: err.message,
                        sessionExpired: true,
                    }));
                    return;
                }

                timeoutRef.current = setTimeout(() => poll(analysisId), POLL_INTERVAL_MS);
            });
    }, [analysisURL, onComplete]);

    const start = useCallback((formData) => {
        if (state.phase === "starting" || state.phase === "polling") return; // guard against double-submit

        clearPending();
        setState({ ...INITIAL_STATE, phase: "starting", message: "Uploading resume..." });

        startAnalysis(analysisURL, formData)
            .then((res) => {
                if (!mountedRef.current) return;

                jobIdRef.current = res.analysisId;
                startedAtRef.current = Date.now();

                setState((s) => ({
                    ...s,
                    phase: "polling",
                    status: res.status,
                    message: "Queued for analysis...",
                }));

                timeoutRef.current = setTimeout(() => poll(res.analysisId), POLL_INTERVAL_MS);
            })
            .catch((err) => {
                if (!mountedRef.current) return;

                const sessionExpired = err instanceof ApiError && err.status === 401;

                setState((s) => ({
                    ...s,
                    phase: "failed",
                    errorMessage: err.message || "Couldn't start the analysis. Please try again.",
                    sessionExpired,
                }));
            });
    }, [analysisURL, poll, state.phase]);

    const reset = useCallback(() => {
        clearPending();
        jobIdRef.current = null;
        startedAtRef.current = null;
        setState(INITIAL_STATE);
    }, []);

    return { ...state, start, reset };
}
