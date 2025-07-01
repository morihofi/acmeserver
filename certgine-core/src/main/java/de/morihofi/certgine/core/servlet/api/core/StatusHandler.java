package de.morihofi.certgine.core.servlet.api.core;

import de.morihofi.certgine.core.service.UserService;
import de.morihofi.certgine.server.common.intf.Handler;
import de.morihofi.certgine.server.common.intf.HandlerContext;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * Handler returning server status information.
 */
@RequiredArgsConstructor
public class StatusHandler implements Handler {
    private final UserService userService;

    @Override
    public void handle(HandlerContext context) {
        Map<String, Object> status = new HashMap<>();
        status.put("readiness", "ready");
        status.put("firstrun", userService.isFirstRun());
        context.json(status);
    }
}
