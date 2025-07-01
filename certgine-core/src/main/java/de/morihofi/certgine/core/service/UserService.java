package de.morihofi.certgine.core.service;

import de.morihofi.certgine.types.database.entities.user.*;
import de.morihofi.certgine.types.intf.IServerInstance;
import lombok.RequiredArgsConstructor;
import org.hibernate.Session;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.mindrot.jbcrypt.BCrypt;
import de.morihofi.certgine.utils.crypto.TotpUtil;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.Set;
import java.util.List;
import lombok.Data;

/**
 * Service handling user registration and authentication.
 */
@RequiredArgsConstructor
public class UserService {
    private final IServerInstance serverInstance;
    private KeyPair jwtKeyPair;

    /** Result returned by {@link #login(String, String, String, String)}. */
    @Data
    public static class LoginResult {
        private String token;
        private boolean totpRequired;
        private boolean webauthnRequired;
    }

    private KeyPair getJwtKeyPair() throws Exception {
        if (jwtKeyPair == null) {
            KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
            gen.initialize(2048);
            jwtKeyPair = gen.generateKeyPair();
        }
        return jwtKeyPair;
    }

    /**
     * Checks if there are any users stored.
     */
    public boolean isFirstRun() {
        try (Session s = serverInstance.getDatabaseSession()) {
            Long count = s.createQuery("select count(u.id) from Users u", Long.class).uniqueResult();
            return count == 0;
        }
    }

    /**
     * Registers a new user.
     */
    public Users register(String email, String password) {
        try (Session s = serverInstance.getDatabaseSession()) {
            s.beginTransaction();
            Users user = new Users();
            user.setEmail(email);
            user.setPasswordHash(BCrypt.hashpw(password, BCrypt.gensalt()));
            user.setAdmin(false);
            s.persist(user);

            if (isFirstRun()) {
                UserGroup admin = new UserGroup();
                admin.setName("administrators");
                admin.setAdmin(true);
                s.persist(admin);
                user.setAdmin(true);
                user.setGroups(Set.of(admin));
            }

            s.getTransaction().commit();
            return user;
        }
    }

    /**
     * Authenticates the user. Only after the email and password are verified
     * does the method evaluate if a second factor is required. When additional
     * verification is needed the returned {@link LoginResult} indicates which
     * methods are expected.
     */
    public LoginResult login(String email, String password, String totp, String webauthnId) throws Exception {
        try (Session s = serverInstance.getDatabaseSession()) {
            Users user = s.createQuery("from Users u where u.email = :e", Users.class)
                    .setParameter("e", email)
                    .uniqueResult();
            if (user == null) {
                return null;
            }
            if (!BCrypt.checkpw(password, user.getPasswordHash())) {
                return null;
            }
            boolean hasTotp = !s.createQuery("select 1 from UserTotp t where t.user = :u")
                    .setParameter("u", user)
                    .setMaxResults(1)
                    .list().isEmpty();
            boolean hasWebAuthn = !s.createQuery("select 1 from UserWebAuthnKey w where w.user = :u")
                    .setParameter("u", user)
                    .setMaxResults(1)
                    .list().isEmpty();

            boolean totpOk = false;
            boolean webAuthnOk = false;
            if (hasTotp && totp != null) {
                for (UserTotp t : user.getTotpAuthenticators()) {
                    if (TotpUtil.verifyCode(t.getSecret(), totp)) {
                        totpOk = true;
                        break;
                    }
                }
            }
            if (hasWebAuthn && webauthnId != null) {
                webAuthnOk = user.getWebAuthnKeys().stream()
                        .anyMatch(k -> k.getCredentialId().equals(webauthnId));
            }

            if (hasTotp || hasWebAuthn) {
                if (!(totpOk || webAuthnOk)) {
                    LoginResult r = new LoginResult();
                    r.setTotpRequired(hasTotp);
                    r.setWebauthnRequired(hasWebAuthn);
                    return r;
                }
            }

            String token = buildJwt(user);
            s.beginTransaction();
            UserSession us = new UserSession();
            us.setUser(user);
            us.setSessionToken(token);
            us.setSessionCreated(java.sql.Timestamp.from(Instant.now()));
            us.setSessionExpire(java.sql.Timestamp.from(Instant.now().plusSeconds(3600)));
            s.persist(us);
            s.getTransaction().commit();
            LoginResult r = new LoginResult();
            r.setToken(token);
            return r;
        }
    }

    /**
     * Get user from session token.
     */
    public Users getUserFromToken(String token) {
        if (token == null) {
            return null;
        }
        try (Session s = serverInstance.getDatabaseSession()) {
            UserSession us = s.createQuery("from UserSession u where u.sessionToken = :t and u.sessionExpire > :now", UserSession.class)
                    .setParameter("t", token)
                    .setParameter("now", new Date())
                    .uniqueResult();
            return us == null ? null : us.getUser();
        }
    }

    /**
     * Get all sessions for a user.
     */
    public List<UserSession> getSessionsForUser(Users user) {
        try (Session s = serverInstance.getDatabaseSession()) {
            return s.createQuery("from UserSession u where u.user = :u", UserSession.class)
                    .setParameter("u", user)
                    .list();
        }
    }

    private String buildJwt(Users user) throws Exception {
        JwtClaims claims = new JwtClaims();
        claims.setSubject(user.getId().toString());
        claims.setExpirationTimeMinutesInTheFuture(60);

        JsonWebSignature jws = new JsonWebSignature();
        jws.setPayload(claims.toJson());
        jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.RSA_USING_SHA256);
        KeyPair kp = getJwtKeyPair();
        PublicKey publicKey = kp.getPublic();
        PrivateKey privateKey = kp.getPrivate();
        jws.setKey(privateKey);
        return jws.getCompactSerialization();
    }
}
