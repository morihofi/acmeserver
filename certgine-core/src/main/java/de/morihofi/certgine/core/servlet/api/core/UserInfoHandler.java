package de.morihofi.certgine.core.servlet.api.core;

import de.morihofi.certgine.core.service.UserService;
import de.morihofi.certgine.server.common.intf.Handler;
import de.morihofi.certgine.server.common.intf.HandlerContext;
import de.morihofi.certgine.types.database.entities.user.Users;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Handler returning information about the currently logged in user.
 */
@RequiredArgsConstructor
public class UserInfoHandler implements Handler {
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
        Map<String, Object> info = new HashMap<>();
        info.put("email", user.getEmail());
        info.put("admin", user.isAdmin());
        info.put("groups", user.getGroups() == null ? null : user.getGroups().stream().map(g -> g.getName()).collect(Collectors.toList()));
        context.json(info);
    }
}
