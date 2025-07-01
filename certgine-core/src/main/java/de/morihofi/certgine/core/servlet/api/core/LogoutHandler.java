package de.morihofi.certgine.core.servlet.api.core;

import de.morihofi.certgine.server.common.intf.Handler;
import de.morihofi.certgine.server.common.intf.HandlerContext;
import de.morihofi.certgine.types.database.entities.user.UserSession;
import de.morihofi.certgine.types.intf.IServerInstance;
import lombok.RequiredArgsConstructor;
import org.hibernate.Session;

/**
 * Handles user logout by removing the JWT from the database.
 */
@RequiredArgsConstructor
public class LogoutHandler implements Handler {
    private final IServerInstance serverInstance;

    @Override
    public void handle(HandlerContext context) throws Exception {
        String token = context.header("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            try (Session s = serverInstance.getDatabaseSession()) {
                s.beginTransaction();
                UserSession us = s.createQuery("from UserSession u where u.sessionToken = :t", UserSession.class)
                        .setParameter("t", token)
                        .uniqueResult();
                if (us != null) {
                    s.remove(us);
                }
                s.getTransaction().commit();
            }
        }
        context.status(204);
    }
}
