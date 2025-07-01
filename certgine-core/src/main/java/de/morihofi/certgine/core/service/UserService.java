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

/**
 * Service handling user registration and authentication.
 */
@RequiredArgsConstructor
public class UserService {
    private final IServerInstance serverInstance;
    private KeyPair jwtKeyPair;

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
     * Authenticates the user and creates a session token.
     */
    public String login(String email, String password, String totp) throws Exception {
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
            if (user.getTotpAuthenticators() != null && !user.getTotpAuthenticators().isEmpty()) {
                boolean ok = false;
                for (UserTotp t : user.getTotpAuthenticators()) {
                    if (TotpUtil.verifyCode(t.getSecret(), totp)) {
                        ok = true;
                        break;
                    }
                }
                if (!ok) {
                    return null;
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
            return token;
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
