package de.morihofi.acmeserver.config.certificateAlgorithms;

import java.io.Serializable;

/**
 * Abstract base class for algorithm parameters. This class provides a common structure for various types of algorithm parameters, allowing
 * them to be serialized and managed in a unified way. Subclasses should provide specific implementations and additional properties relevant
 * to the particular algorithm.
 */
public abstract class AlgorithmParams implements Serializable {
    private String type;

    /**
     * Retrieves the type of the algorithm. The type is a string identifier that describes the specific algorithm or set of parameters
     * represented by the subclass.
     *
     * @return The type of the algorithm as a {@code String}.
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the type of the algorithm. This method allows specifying the type, providing a way to identify the specific algorithm or
     * parameters represented by the subclass.
     *
     * @param type The type of the algorithm as a {@code String}.
     */
    public void setType(String type) {
        this.type = type;
    }
}
