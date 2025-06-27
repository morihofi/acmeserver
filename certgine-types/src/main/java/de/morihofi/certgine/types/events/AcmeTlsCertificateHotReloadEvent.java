package de.morihofi.certgine.types.events;

import de.morihofi.certgine.types.intf.IServerInstance;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Event requesting the web server to reload its TLS configuration.
 */
@AllArgsConstructor
@Getter
public class AcmeTlsCertificateHotReloadEvent extends AbstractEvent{
    private final IServerInstance serverInstance;
}
