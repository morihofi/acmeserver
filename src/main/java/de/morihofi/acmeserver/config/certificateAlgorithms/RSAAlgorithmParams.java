package de.morihofi.acmeserver.config.certificateAlgorithms;

import java.io.Serializable;

public class RSAAlgorithmParams extends AlgorithmParams {
        private int keySize;

        public int getKeySize() {
            return this.keySize;
        }

        public void setKeySize(int keySize) {
            this.keySize = keySize;
        }

}
