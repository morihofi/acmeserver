package de.morihofi.acmeserver.tools;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import de.morihofi.acmeserver.exception.ACMEException;
import de.morihofi.acmeserver.exception.exceptions.ACMEServerInternalException;
import de.morihofi.acmeserver.tools.network.SocketUtil;
import io.javalin.Javalin;
import jdk.dynalink.NamedOperation;
import org.bouncycastle.jsse.provider.BouncyCastleJsseProvider;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.shredzone.acme4j.*;
import org.shredzone.acme4j.challenge.Challenge;
import org.shredzone.acme4j.challenge.Dns01Challenge;
import org.shredzone.acme4j.challenge.Http01Challenge;
import org.shredzone.acme4j.exception.AcmeException;
import org.shredzone.acme4j.util.KeyPairUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.*;
import java.net.URI;
import java.nio.file.*;
import java.security.KeyPair;
import java.security.KeyStore;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertThrows;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AcmeServerTest {

    private int acmeserverPort;
    private static final FileSystem fs;
    private static final Path acmeClientWorkingDir;
    private static final String PROVISIONER_NAME_NORESTRICTION = "unrestricted";
    private static final String PROVISIONER_NAME_RESTRICTION = "domainrestricted";
    private static final String keystoreName = "keystore-unittest-" + UUID.randomUUID().toString() + ".p12";

    static {
        // Create virtual filesystem
        fs = Jimfs.newFileSystem(Configuration.unix());
        acmeClientWorkingDir = fs.getPath("/client-workdir");


        try {
            Files.createDirectories(acmeClientWorkingDir);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @BeforeAll
    public void prepareServer() throws Exception {

        Path serverDataDirectory = fs.getPath("/acmeserver-serverdata");
        acmeserverPort = SocketUtil.findFreePort();
        EmbeddedServer embeddedServer = new EmbeddedServer(
                "localhost",
                acmeserverPort,
                serverDataDirectory,
                "jdbc:h2:mem:acme_unittest",
                "sample_user",
                "123456"
        );

        embeddedServer.configureKeystorePKCS12(keystoreName, "123456");
        embeddedServer.configureRsaRootCertificate(1, 0, 0, "ACME Server");
        embeddedServer.addSimpleProvisioner(PROVISIONER_NAME_NORESTRICTION, 1, 0, 0, Collections.emptyList()); // Allow all
        embeddedServer.addSimpleProvisioner(PROVISIONER_NAME_RESTRICTION, 1, 0, 0, List.of("notlocalhost")); // Restrict only to be end with notlocalhost
        embeddedServer.start();

        // TLS Configuration for trusting our just in time generated Root CA
        {
            // Create an in-memory KeyStore
            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            trustStore.load(null, null);

            trustStore.setCertificateEntry("testAcmeServerRoot", embeddedServer.getRootCertificate());

            // Initialize the TrustManagerFactory with the KeyStore
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(trustStore);

            // Initialize the SSLContext with the TrustManager
            SSLContext sslContext = SSLContext.getInstance("TLS", BouncyCastleJsseProvider.PROVIDER_NAME);
            sslContext.init(null, tmf.getTrustManagers(), new java.security.SecureRandom());

            // Set the SSLContext as the default
            SSLContext.setDefault(sslContext);

        }

        // Javalin webserver for HTTP-01 Challenge
        {
            //

            Javalin javalinApp = Javalin.create(/*config*/)
                    .get("/.well-known/acme-challenge/{token}", ctx -> ctx.result(challenges.getOrDefault(ctx.pathParam("token"),"")))
                    .start(80);
        }


    }

    // Set the Connection URI of your CA here. For testing purposes, use a staging
    // server if possible. Example: "acme://letsencrypt.org/staging" for the Let's
    // Encrypt staging server.
    private String getCaUri(String provisioner){
        return "https://localhost:"  + acmeserverPort  + "/acme/" + provisioner + "/directory";
    } ;

    // E-Mail address to be associated with the account. Optional, null if not used.
    private static final String ACCOUNT_EMAIL = "acmetest@example.com";

    // If the CA requires External Account Binding (EAB), set the provided KID and HMAC here.
    private static final String EAB_KID = null;
    private static final String EAB_HMAC = null;

    // A supplier for a new account KeyPair. The default creates a new EC key pair.
    private static Supplier<KeyPair> ACCOUNT_KEY_SUPPLIER = KeyPairUtils::createKeyPair;

    // A supplier for a new domain KeyPair. The default creates a RSA key pair.
    private static Supplier<KeyPair> DOMAIN_KEY_SUPPLIER = () -> KeyPairUtils.createKeyPair(4096);

    // File name of the User Key Pair
    private static final Path USER_KEY_FILE = acmeClientWorkingDir.resolve("user.key");

    // File name of the Domain Key Pair
    private static final Path DOMAIN_KEY_FILE = acmeClientWorkingDir.resolve("domain.key");

    // File name of the signed certificate
    private static final Path DOMAIN_CHAIN_FILE = acmeClientWorkingDir.resolve("domain-chain.crt");

    //Challenge type to be used
    private static final ChallengeType CHALLENGE_TYPE = ChallengeType.HTTP;

    // Maximum attempts of status polling until VALID/INVALID is expected
    private static final int MAX_ATTEMPTS = 50;

    private static final Logger LOG = LoggerFactory.getLogger(AcmeServerTest.class);

    private enum ChallengeType {HTTP, DNS}


    /**
     * Generates a certificate for the given domains. Also takes care for the registration
     * process.
     *
     * @param domains Domains to get a common certificate for
     */
    public void fetchCertificate(Collection<String> domains, String provisioner) throws IOException, AcmeException {
        // Load the user key file. If there is no key file, create a new one.
        KeyPair userKeyPair = loadOrCreateUserKeyPair();

        // Create a session.
        Session session = new Session(getCaUri(provisioner));

        // Get the Account.
        // If there is no account yet, create a new one.
        Account acct = findOrRegisterAccount(session, userKeyPair);

        // Load or create a key pair for the domains. This should not be the userKeyPair!
        KeyPair domainKeyPair = loadOrCreateDomainKeyPair();

        // Order the certificate
        Order order = acct.newOrder().domains(domains).create();

        // Perform all required authorizations
        for (Authorization auth : order.getAuthorizations()) {
            authorize(auth);
        }

        // Order the certificate
        order.execute(domainKeyPair);

        // Wait for the order to complete
        Status status = waitForCompletion(order::getStatus, order::fetch);
        if (status != Status.VALID) {
            LOG.error("Order has failed, reason: {}", order.getError()
                    .map(Problem::toString)
                    .orElse("unknown")
            );
            throw new AcmeException("Order failed... Giving up.");
        }

        // Get the certificate
        Certificate certificate = order.getCertificate();

        LOG.info("Success! The certificate for domains {} has been generated!", domains);
        LOG.info("Certificate URL: {}", certificate.getLocation());

        // Write a combined file containing the certificate and chain.
        StringWriter sw = new StringWriter();

        try (PrintWriter pw = new PrintWriter(sw)) {
            certificate.writeCertificate(pw);
        }

        LOG.info("Got certificate: {}", sw);

        // That's all! Configure your web server to use the DOMAIN_KEY_FILE and
        // DOMAIN_CHAIN_FILE for the requested domains.
    }

    /**
     * Loads a user key pair from {@link #USER_KEY_FILE}. If the file does not exist, a
     * new key pair is generated and saved.
     * <p>
     * Keep this key pair in a safe place! In a production environment, you will not be
     * able to access your account again if you should lose the key pair.
     *
     * @return User's {@link KeyPair}.
     */
    private KeyPair loadOrCreateUserKeyPair() throws IOException {
        if (Files.exists(USER_KEY_FILE)) {
            // If there is a key file, read it
            try (BufferedReader fr = Files.newBufferedReader(USER_KEY_FILE)) {
                return KeyPairUtils.readKeyPair(fr);
            }

        } else {
            // If there is none, create a new key pair and save it
            KeyPair userKeyPair = ACCOUNT_KEY_SUPPLIER.get();
            try (BufferedWriter writer = Files.newBufferedWriter(USER_KEY_FILE, StandardOpenOption.CREATE)) {
                KeyPairUtils.writeKeyPair(userKeyPair, writer);
            }
            return userKeyPair;
        }
    }


    /**
     * Loads a domain key pair from {@link #DOMAIN_KEY_FILE}. If the file does not exist,
     * a new key pair is generated and saved.
     *
     * @return Domain {@link KeyPair}.
     */
    private KeyPair loadOrCreateDomainKeyPair() throws IOException {
        if (Files.exists(DOMAIN_KEY_FILE)) {
            try (BufferedReader reader = Files.newBufferedReader(DOMAIN_KEY_FILE)) {
                return KeyPairUtils.readKeyPair(reader);
            }
        } else {
            KeyPair domainKeyPair = DOMAIN_KEY_SUPPLIER.get();
            try (BufferedWriter writer = Files.newBufferedWriter(DOMAIN_KEY_FILE, StandardOpenOption.CREATE)) {
                KeyPairUtils.writeKeyPair(domainKeyPair, writer);
            }
            return domainKeyPair;
        }
    }

    /**
     * Finds your {@link Account} at the ACME server. It will be found by your user's
     * public key. If your key is not known to the server yet, a new account will be
     * created.
     * <p>
     * This is a simple way of finding your {@link Account}. A better way is to get the
     * URL of your new account with {@link Account#getLocation()} and store it somewhere.
     * If you need to get access to your account later, reconnect to it via {@link
     * Session#login(URL, KeyPair)} by using the stored location.
     *
     * @param session {@link Session} to bind with
     * @return {@link Account}
     */
    private Account findOrRegisterAccount(Session session, KeyPair accountKey) throws AcmeException {
        // Ask the user to accept the TOS, if server provides us with a link.
        Optional<URI> tos = session.getMetadata().getTermsOfService();
        if (tos.isPresent()) {
            acceptAgreement(tos.get());
        }

        AccountBuilder accountBuilder = new AccountBuilder()
                .agreeToTermsOfService()
                .useKeyPair(accountKey);

        // Set your email (if available)
        if (ACCOUNT_EMAIL != null) {
            accountBuilder.addEmail(ACCOUNT_EMAIL);
        }

        // Use the KID and HMAC if the CA uses External Account Binding
        if (EAB_KID != null && EAB_HMAC != null) {
            accountBuilder.withKeyIdentifier(EAB_KID, EAB_HMAC);
        }

        Account account = accountBuilder.create(session);
        LOG.info("Registered a new user, URL: {}", account.getLocation());

        return account;
    }

    /**
     * Authorize a domain. It will be associated with your account, so you will be able to
     * retrieve a signed certificate for the domain later.
     *
     * @param auth {@link Authorization} to perform
     */
    private void authorize(Authorization auth) throws AcmeException {
        LOG.info("Authorization for domain {}", auth.getIdentifier().getDomain());

        // The authorization is already valid. No need to process a challenge.
        if (auth.getStatus() == Status.VALID) {
            return;
        }

        // Find the desired challenge and prepare it.
        Challenge challenge = null;
        switch (CHALLENGE_TYPE) {
            case HTTP:
                challenge = httpChallenge(auth);
                break;

            case DNS:
                challenge = dnsChallenge(auth);
                break;
        }

        if (challenge == null) {
            throw new AcmeException("No challenge found");
        }

        // If the challenge is already verified, there's no need to execute it again.
        if (challenge.getStatus() == Status.VALID) {
            return;
        }

        // Now trigger the challenge.
        challenge.trigger();

        // Poll for the challenge to complete.
        Status status = waitForCompletion(challenge::getStatus, challenge::fetch);
        if (status != Status.VALID) {
            LOG.error("Challenge has failed, reason: {}", challenge.getError()
                    .map(Problem::toString)
                    .orElse("unknown"));
            throw new AcmeException("Challenge failed... Giving up.");
        }

        LOG.info("Challenge has been completed. Remember to remove the validation resource.");
    }

    /**
     * Prepares a HTTP challenge.
     * <p>
     * The verification of this challenge expects a file with a certain content to be
     * reachable at a given path under the domain to be tested.
     * <p>
     * This example outputs instructions that need to be executed manually. In a
     * production environment, you would rather generate this file automatically, or maybe
     * use a servlet that returns {@link Http01Challenge#getAuthorization()}.
     *
     * @param auth {@link Authorization} to find the challenge in
     * @return {@link Challenge} to verify
     */
    public Challenge httpChallenge(Authorization auth) throws AcmeException {
        // Find a single http-01 challenge
        Http01Challenge challenge = auth.findChallenge(Http01Challenge.class)
                .orElseThrow(() -> new AcmeException("Found no " + Http01Challenge.TYPE
                        + " challenge, don't know what to do..."));

        // Output the challenge, wait for acknowledge...
        LOG.info("Please create a file in your web server's base directory.");
        LOG.info("It must be reachable at: http://{}/.well-known/acme-challenge/{}",
                auth.getIdentifier().getDomain(), challenge.getToken());
        LOG.info("File name: {}", challenge.getToken());
        LOG.info("Content: {}", challenge.getAuthorization());
        LOG.info("The file must not contain any leading or trailing whitespaces or line breaks!");
        LOG.info("If you're ready, dismiss the dialog...");


        challenges.put(challenge.getToken(), challenge.getAuthorization());


        return challenge;
    }

    private final Map<String,String> challenges = Collections.synchronizedMap(new HashMap<>());

    /**
     * Prepares a DNS challenge.
     * <p>
     * The verification of this challenge expects a TXT record with a certain content.
     * <p>
     * This example outputs instructions that need to be executed manually. In a
     * production environment, you would rather configure your DNS automatically.
     *
     * @param auth {@link Authorization} to find the challenge in
     * @return {@link Challenge} to verify
     */
    public Challenge dnsChallenge(Authorization auth) throws AcmeException {
        // Find a single dns-01 challenge
        Dns01Challenge challenge = auth.findChallenge(Dns01Challenge.TYPE)
                .map(Dns01Challenge.class::cast)
                .orElseThrow(() -> new AcmeException("Found no " + Dns01Challenge.TYPE
                        + " challenge, don't know what to do..."));

        // Output the challenge, wait for acknowledge...
        LOG.info("Please create a TXT record:");
        LOG.info("{} IN TXT {}",
                Dns01Challenge.toRRName(auth.getIdentifier()), challenge.getDigest());
        LOG.info("If you're ready, dismiss the dialog...");

        StringBuilder message = new StringBuilder();
        message.append("Please create a TXT record:\n\n");
        message.append(Dns01Challenge.toRRName(auth.getIdentifier()))
                .append(" IN TXT ")
                .append(challenge.getDigest());
       // acceptChallenge(message.toString());

        return challenge;
    }

    /**
     * Waits for completion of a resource. A resource is completed if the status is either
     * {@link Status#VALID} or {@link Status#INVALID}.
     * <p>
     * This method polls the current status, respecting the retry-after header if set. It
     * is synchronous and may take a considerable time for completion.
     * <p>
     * It is meant as a simple example! For production services, it is recommended to do
     * an asynchronous processing here.
     *
     * @param statusSupplier Method of the resource that returns the current status
     * @param statusUpdater  Method of the resource that updates the internal state and fetches the
     *                       current status from the server. It returns the instant of an optional
     *                       retry-after header.
     * @return The final status, either {@link Status#VALID} or {@link Status#INVALID}
     * @throws AcmeException If an error occured, or if the status did not reach one of the accepted
     *                       result values after a certain number of checks.
     */
    private Status waitForCompletion(Supplier<Status> statusSupplier, UpdateMethod statusUpdater)
            throws AcmeException {
        // A set of terminating status values
        Set<Status> acceptableStatus = EnumSet.of(Status.VALID, Status.INVALID);

        // Limit the number of checks, to avoid endless loops
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            LOG.info("Checking current status, attempt {} of {}", attempt, MAX_ATTEMPTS);

            Instant now = Instant.now();

            // Update the status property
            Instant retryAfter = statusUpdater.updateAndGetRetryAfter()
                    .orElse(now.plusSeconds(3L));

            // Check the status
            Status currentStatus = statusSupplier.get();
            if (acceptableStatus.contains(currentStatus)) {
                // Reached VALID or INVALID, we're done here
                return currentStatus;
            }

            // Wait before checking again
            try {
                Thread.sleep(now.until(retryAfter, ChronoUnit.MILLIS));
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new AcmeException("interrupted");
            }
        }

        throw new AcmeException("Too many update attempts, status did not change");
    }

    /**
     * Functional interface that refers to a resource update method that returns an
     * optional retry-after instant and is able to throw an {@link AcmeException}.
     */
    @FunctionalInterface
    private interface UpdateMethod {
        Optional<Instant> updateAndGetRetryAfter() throws AcmeException;
    }


    /**
     * Presents the user a link to the Terms of Service, and asks for confirmation. If the
     * user denies confirmation, an exception is thrown.
     *
     * @param agreement {@link URI} of the Terms of Service
     */
    public void acceptAgreement(URI agreement) throws AcmeException {
        LOG.info("Accepting Terms of Service at {}", agreement);
    }

    /**
     * The test
     *
     * @throws AcmeException
     * @throws IOException
     */
    @Test
    public void acmeServerTest() throws AcmeException, IOException {
        fetchCertificate(Collections.singleton("localhost"), PROVISIONER_NAME_NORESTRICTION);
    }

    /**
     * The test, that should fail
     *
     * @throws AcmeException
     * @throws IOException
     */
    @Test
    public void acmeServerTestInvalidDomain() {
        assertThrows(AcmeException.class,() -> {
            fetchCertificate(Collections.singleton("localhost"), PROVISIONER_NAME_RESTRICTION);
        });
    }

    @AfterAll
    public void cleanup() throws IOException {
        Files.deleteIfExists(Paths.get(keystoreName));
    }
}
