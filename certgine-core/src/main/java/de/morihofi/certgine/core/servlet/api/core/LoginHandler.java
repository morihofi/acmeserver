package de.morihofi.certgine.core.servlet.api.core;

import de.morihofi.certgine.core.service.UserService;
import de.morihofi.certgine.server.common.intf.Handler;
import de.morihofi.certgine.server.common.intf.HandlerContext;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles user login.
 */
@RequiredArgsConstructor
public class LoginHandler implements Handler {
    private final UserService userService;

    @Data
    private static class LoginRequest {
        private String email;
        private String password;
        private String totp;
    }

    @Override
    public void handle(HandlerContext context) throws Exception {
        LoginRequest req = context.bodyAsClass(LoginRequest.class);
        String token = userService.login(req.getEmail(), req.getPassword(), req.getTotp());
        if (token == null) {
            context.status(401);
            return;
        }
        Map<String, String> r = new HashMap<>();
        r.put("token", token);
        context.json(r);
    }
}
