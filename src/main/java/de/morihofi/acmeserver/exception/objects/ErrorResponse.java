package de.morihofi.acmeserver.exception.objects;

/**
 * A class representing an error response in an API or web service. It typically contains information about the error type and additional
 * details.
 */
public class ErrorResponse {
    private String type;
    private String detail;

    /**
     * Get the type of the error.
     *
     * @return The error type, which can be a short description or code for the error.
     */
    public String getType() {
        return type;
    }

    /**
     * Set the type of the error.
     *
     * @param type The error type to set.
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Get additional details about the error.
     *
     * @return Additional error details, which may provide more context or information about the error.
     */
    public String getDetail() {
        return detail;
    }

    /**
     * Set additional details about the error.
     *
     * @param detail Additional error details to set.
     */
    public void setDetail(String detail) {
        this.detail = detail;
    }
}
