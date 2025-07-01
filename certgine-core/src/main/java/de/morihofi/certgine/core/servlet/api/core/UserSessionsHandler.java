package de.morihofi.certgine.core.servlet.api.core;

import de.morihofi.certgine.core.service.UserService;
import de.morihofi.certgine.server.common.intf.Handler;
import de.morihofi.certgine.server.common.intf.HandlerContext;
import de.morihofi.certgine.types.database.entities.user.UserSession;
import de.morihofi.certgine.types.database.entities.user.Users;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Handler returning all active sessions for the current user.
 */
@RequiredArgsConstructor
public class UserSessionsHandler implements Handler {
    private final UserService userService;

    @Override
    public void handle(HandlerContext context) throws Exception {
        String token = context.header("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        Users user = userService.getUserFromToken(token);
        if (user == null) {
            context.status(401);
            return;
        }
        List<UserSession> sessions = userService.getSessionsForUser(user);
        List<Object> result = sessions.stream().map(s -> {
            return new java.util.HashMap<String, Object>() {{
                put("id", s.getId());
                put("created", s.getSessionCreated());
                put("expires", s.getSessionExpire());
            }};
        }).collect(Collectors.toList());
        context.json(result);
    }
}
