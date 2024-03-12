package de.morihofi.acmeserver.certificate.acme.challenges;

public class ChallengeResult {
    private boolean successful;
    private String errorReason;

    public ChallengeResult(boolean successful, String errorReason) {
        this.successful = successful;
        this.errorReason = errorReason;
    }

    public boolean isSuccessful() {
        return successful;
    }

    public void setSuccessful(boolean successful) {
        this.successful = successful;
    }

    public String getErrorReason() {
        return errorReason;
    }

    public void setErrorReason(String errorReason) {
        this.errorReason = errorReason;
    }
}
