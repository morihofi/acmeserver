package de.morihofi.certgine.core.servlet.api.core;

import de.morihofi.certgine.core.service.UserService;
import de.morihofi.certgine.server.common.intf.Handler;
import de.morihofi.certgine.server.common.intf.HandlerContext;
import lombok.Data;
import lombok.RequiredArgsConstructor;

/**
 * Handles user registration.
 */
@RequiredArgsConstructor
public class RegisterHandler implements Handler {
    private final UserService userService;

    @Data
    private static class RegisterRequest {
        private String email;
        private String password;
    }

    @Override
    public void handle(HandlerContext context) throws Exception {
        RegisterRequest req = context.bodyAsClass(RegisterRequest.class);
        userService.register(req.getEmail(), req.getPassword());
        context.status(200);
        context.result();
    }
}
