package de.morihofi.acmeserver.config.certificateAlgorithms;

import java.io.Serializable;

public class RSAAlgorithmParams extends AlgorithmParams {
        private Integer keySize;

        public Integer getKeySize() {
            return this.keySize;
        }

        public void setKeySize(Integer keySize) {
            this.keySize = keySize;
        }

}
