package de.morihofi.acmeserver.config.certificateAlgorithms;

public class EcdsaAlgorithmParams extends AlgorithmParams {
    private String curveName;

    public String getCurveName() {
        return curveName;
    }

    public void setCurveName(String curveName) {
        this.curveName = curveName;
    }
}
