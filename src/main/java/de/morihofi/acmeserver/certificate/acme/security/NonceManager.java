package de.morihofi.acmeserver.certificate.acme.security;

import de.morihofi.acmeserver.database.HibernateUtil;
import de.morihofi.acmeserver.database.objects.HttpNonces;
import de.morihofi.acmeserver.exception.exceptions.ACMEBadNonceException;
import jakarta.persistence.Query;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.json.JSONObject;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public class NonceManager {
    public static final Logger log = LogManager.getLogger(NonceManager.class);

    public static void checkNonceFromDecodedProtected(String decodedProtected) {
        JSONObject reqBodyProtectedObj = new JSONObject(decodedProtected);
        String nonce = reqBodyProtectedObj.getString("nonce");

        if(isNonceUsed(nonce)){
           throw new ACMEBadNonceException("Nonce already used");
        }

    }

    /**
     * Checks if the nonce has already been used. If not, it adds the nonce to the database.
     *
     * @param nonce The nonce to be checked
     * @return true if the nonce already exists, false if it was added
     */
    public static boolean isNonceUsed(String nonce) {
        try (Session session = Objects.requireNonNull(HibernateUtil.getSessionFactory()).openSession()) {
            assert session != null;
            org.hibernate.Transaction transaction = session.beginTransaction();

            // Check if the nonce exists in the database
            String hql = "SELECT 1 FROM HttpNonces hn WHERE hn.nonce = :nonce";
            Query query = session.createQuery(hql, HttpNonces.class);
            query.setParameter("nonce", nonce);
            List<HttpNonces> results = query.getResultList();
            boolean nonceExists = !results.isEmpty();

            if (!nonceExists) {
                // If the nonce does not exist, add it to the database
                HttpNonces newNonce = new HttpNonces(nonce, LocalDateTime.now());
                session.persist(newNonce);
            }

            transaction.commit();
            // Return true if nonce exists, false if it was added
            return nonceExists;
        } catch (Exception e) {
            log.error("Error checking or adding nonce", e);
            return false;
        }
    }

}
