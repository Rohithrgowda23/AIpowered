

class ApiError extends Error {
    constructor(message, status) {
        super(message);
        this.status = status;
    }
}

async function readErrorMessage(response) {
    try {
        const text = await response.text();
        return text && text.length < 300 ? text : null;
    } catch {
        return null;
    }
}

function messageForStatus(status, fallback) {
    switch (status) {
        case 401:
            return "Your session has expired. Please log in again.";
        case 429:
            return "AI service is temporarily busy. Please try again in a moment.";
        case 502:
        case 503:
            return "Analysis service is temporarily unavailable. Please try again shortly.";
        default:
            return fallback;
    }
}

export async function startAnalysis(analysisURL, formData) {
    let response;
    try {
        response = await fetch(`${analysisURL}`, {
            method: "POST",
            body: formData,
            headers: { "Authorization": `Bearer ${localStorage.getItem("token")}` },
        });
    } catch {
        throw new ApiError("Network error. Please check your connection and try again.", 0);
    }

    if (!response.ok) {
        const bodyMessage = await readErrorMessage(response);
        throw new ApiError(
            messageForStatus(response.status, bodyMessage || "Something went wrong starting the analysis."),
            response.status
        );
    }

    return response.json(); // { analysisId, status }
}

export async function getAnalysisStatus(analysisURL, analysisId) {
    let response;
    try {
        response = await fetch(`${analysisURL}/${analysisId}/status`, {
            headers: { "Authorization": `Bearer ${localStorage.getItem("token")}` },
        });
    } catch {
        throw new ApiError("Network error while checking analysis status.", 0);
    }

    if (!response.ok) {
        const bodyMessage = await readErrorMessage(response);
        throw new ApiError(
            messageForStatus(response.status, bodyMessage || "Couldn't fetch analysis status."),
            response.status
        );
    }

    return response.json(); // AnalysisStatusDto
}

export { ApiError };
