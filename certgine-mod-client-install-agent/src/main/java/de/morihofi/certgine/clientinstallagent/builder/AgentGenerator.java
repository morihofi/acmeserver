package de.morihofi.certgine.clientinstallagent.builder;


import com.google.gson.Gson;
import de.morihofi.certgine.types.modules.CertgineModuleInstance;
import lombok.AllArgsConstructor;
import lombok.NonNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPOutputStream;

@AllArgsConstructor
public class AgentGenerator {

    @NonNull
    private final CertgineModuleInstance moduleInstance;

    public ByteBuffer generateAgent(AgentConfig configObj) throws IOException {

        // Load and patch a PE file from resources
        byte[] jsonConfig = new Gson()
                .toJson(configObj)
                .getBytes(StandardCharsets.UTF_8);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(baos)) {
            gzip.write(jsonConfig);
        }

        byte[] compressedConfig = baos.toByteArray();

        return WinPeResourceLoader.loadAndPatchPe("stub.exe", compressedConfig);
    }


}
