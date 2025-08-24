package de.morihofi.certgine.clientinstallagent.builder;


import com.google.gson.Gson;
import de.morihofi.certgine.types.modules.CertgineModuleInstance;
import lombok.AllArgsConstructor;
import lombok.NonNull;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

@AllArgsConstructor
public class AgentGenerator {

    @NonNull
    private final CertgineModuleInstance moduleInstance;

    public ByteBuffer generateAgent(AgentConfig configObj) throws IOException {

        // Load and patch a PE file from resources
        byte[] config = new Gson()
                .toJson(configObj)
                .getBytes(StandardCharsets.UTF_8);

        // Read configuration back from patched PE
        //Optional<String> configJson = WinPeResourceLoader.readConfigFromPeBuffer(patchedPe);

        return WinPeResourceLoader.loadAndPatchPe("stub.exe", config);
    }


}
