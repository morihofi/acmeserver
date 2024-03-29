package de.morihofi.acmeserver.certificate.acme.security;

import de.morihofi.acmeserver.Main;
import de.morihofi.acmeserver.database.HibernateUtil;
import de.morihofi.acmeserver.database.objects.HttpNonces;
import de.morihofi.acmeserver.exception.exceptions.ACMEBadNonceException;
import de.morihofi.acmeserver.tools.safety.TypeSafetyHelper;
import jakarta.persistence.Query;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.json.JSONObject;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public class NonceManager {


    /**
     * Logger
     */
    public static final Logger log = LogManager.getLogger(NonceManager.class);

    /**
     * Checks if a nonce from a decoded protected request body has already been used.
     *
     * @param decodedProtected The decoded protected request body as a JSON string.
     * @throws ACMEBadNonceException If the nonce has already been used.
     */
    public static void checkNonceFromDecodedProtected(String decodedProtected) {
        JSONObject reqBodyProtectedObj = new JSONObject(decodedProtected);
        String nonce = reqBodyProtectedObj.getString("nonce");

        if (isNonceUsed(nonce)) {
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

        if(Main.debug){
            //Nonce protection is disabled when DEBUG environment variable is set to TRUE
            return false;
        }

        try (Session session = Objects.requireNonNull(HibernateUtil.getSessionFactory()).openSession()) {
            assert session != null;
            org.hibernate.Transaction transaction = session.beginTransaction();

            // Check if the nonce exists in the database
            String hql = "SELECT 1 FROM HttpNonces hn WHERE hn.nonce = :nonce";
            Query query = session.createQuery(hql, HttpNonces.class);
            query.setParameter("nonce", nonce);
            List<HttpNonces> results = TypeSafetyHelper.safeCastToClassOfType(query.getResultList(), HttpNonces.class);

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
