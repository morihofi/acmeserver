package de.morihofi.acmeserver.types.config;

import lombok.Data;
import java.io.Serializable;

/**
 * Configuration for gRPC based clustering.
 */
@Data
public class GrpcConfig implements Serializable {
    private boolean enabled = false;
    private String host = "0.0.0.0";
    private int port = 50051;
}
