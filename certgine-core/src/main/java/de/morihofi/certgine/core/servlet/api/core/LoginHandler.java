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
        private String webauthnId;
    }

    @Override
    public void handle(HandlerContext context) throws Exception {
        LoginRequest req = context.bodyAsClass(LoginRequest.class);
        UserService.LoginResult res = userService.login(req.getEmail(), req.getPassword(), req.getTotp(), req.getWebauthnId());
        if (res == null) {
            context.status(401);
            return;
        }
        if (res.getToken() == null) {
            Map<String, Object> r = new HashMap<>();
            r.put("totpRequired", res.isTotpRequired());
            r.put("webauthnRequired", res.isWebauthnRequired());
            context.json(r);
        } else {
            Map<String, String> r = new HashMap<>();
            r.put("token", res.getToken());
            context.json(r);
        }
    }
}
