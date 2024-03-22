package de.morihofi.acmeserver.webui.handler;

import de.morihofi.acmeserver.certificate.provisioners.Provisioner;
import de.morihofi.acmeserver.tools.certificate.cryptoops.CryptoStoreManager;
import de.morihofi.acmeserver.tools.collectors.SingletonCollector;
import de.morihofi.acmeserver.webui.JteLocalizer;
import de.morihofi.acmeserver.webui.WebUI;
import de.morihofi.acmeserver.webui.compontents.table.TableKey;
import de.morihofi.acmeserver.webui.compontents.table.TableValue;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import jakarta.xml.bind.DatatypeConverter;
import org.bouncycastle.asn1.*;
import org.bouncycastle.asn1.ocsp.OCSPObjectIdentifiers;
import org.bouncycastle.asn1.x509.*;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.PublicKey;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.text.DateFormat;
import java.util.*;

public class ProvisionerInfoHandler implements Handler {

    private final CryptoStoreManager cryptoStoreManager;

    /**
     * Date format for the locale of the user sent in the request
     */
    private DateFormat dateFormat;
    private JteLocalizer localizer;

    public ProvisionerInfoHandler(CryptoStoreManager cryptoStoreManager) {
        this.cryptoStoreManager = cryptoStoreManager;
    }

    @Override
    public void handle(@NotNull Context context) throws Exception {
        this.localizer = JteLocalizer.getLocalizerFromContext(context);
        this.dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM, localizer.getLocale());

        String provisionerName = context.queryParam("name");


        Provisioner provisioner = cryptoStoreManager.getProvisioners().stream()
                .filter(lstProvisioner -> lstProvisioner.getProvisionerName().equals(provisionerName))
                .collect(SingletonCollector.toSingleton());

        Map<TableKey, TableValue> provisionerTableMap = getProvisionerMetadata(provisioner);
        Map<TableKey, TableValue> provisionerIntermediateTableMap = getIntermediateMetadata(provisioner);


        Map<String, Object> params = new HashMap<>(WebUI.getDefaultFrontendMap(cryptoStoreManager, context));
        params.put("provisionerName", provisioner.getProvisionerName());
        params.put("provisionerTableMap", provisionerTableMap);
        params.put("provisionerIntermediateTableMap", provisionerIntermediateTableMap);

        context.render("pages/provisioner-info.jte", params);
    }

    /**
     * Constructs a metadata map for a given provisioner, including details such as the provisioner's
     * name, terms of service, and website. This method prepares a {@link LinkedHashMap} to preserve
     * the order of metadata entries, facilitating a more predictable display in user interfaces.
     * Icons are associated with each piece of metadata to enhance visual comprehension.
     *
     * @param provisioner the provisioner whose metadata is to be represented.
     * @return a {@link Map} where keys are {@link TableKey} objects encapsulating metadata labels and
     *         icons, and values are {@link TableValue} objects containing the metadata values. Some
     *         values, such as terms of service and website, are treated as links.
     */
    @NotNull
    private Map<TableKey, TableValue> getProvisionerMetadata(Provisioner provisioner) {
        Map<TableKey, TableValue> provisionerTableMap = new LinkedHashMap<>();
        provisionerTableMap.put(new TableKey("web.core.provisioner.meta.name", "fa-solid fa-info"), new TableValue(provisioner.getProvisionerName()));
        provisionerTableMap.put(new TableKey("web.core.provisioner.meta.termsOfService", "fa-regular fa-file-lines"), new TableValue(provisioner.getAcmeMetadataConfig().getTos(), TableValue.VALUE_TYPE.LINK));
        provisionerTableMap.put(new TableKey("web.core.provisioner.meta.website", "fa-solid fa-globe"), new TableValue(provisioner.getAcmeMetadataConfig().getWebsite(), TableValue.VALUE_TYPE.LINK));
        return provisionerTableMap;
    }

    /**
     * Retrieves intermediate metadata for a given provisioner.
     *
     * @param provisioner The provisioner for which to get the metadata.
     * @return A map containing key-value pairs of the metadata.
     */
    @NotNull
    private Map<TableKey, TableValue> getIntermediateMetadata(Provisioner provisioner) {
        Map<TableKey, TableValue> metadataMap = new LinkedHashMap<>();

        try {
            X509Certificate certificate = cryptoStoreManager.getX509CertificateForProvisioner(provisioner.getProvisionerName());

            // Add subject and issuer of the certificate
            metadataMap.put(new TableKey("web.core.certificate.subject", "fa-regular fa-id-badge"), new TableValue(certificate.getSubjectX500Principal().getName()));
            metadataMap.put(new TableKey("web.core.certificate.issuer", "fa-solid fa-building-columns"), new TableValue(certificate.getIssuerX500Principal().getName()));

            // Add validity dates of the certificate
            metadataMap.put(new TableKey("web.core.certificate.notBefore", "fa-regular fa-calendar-check"), new TableValue(dateFormat.format(certificate.getNotBefore())));
            metadataMap.put(new TableKey("web.core.certificate.notAfter", "fa-regular fa-calendar-xmark"), new TableValue(dateFormat.format(certificate.getNotAfter())));

            // Add serial number
            metadataMap.put(new TableKey("web.core.certificate.serialNumber", "fa-solid fa-barcode"), new TableValue(certificate.getSerialNumber().toString()));

            // Add public key
            metadataMap.put(new TableKey("web.core.certificate.publicKey", "fa-solid fa-key"), new TableValue(publicKeyDetails(certificate)));

            // Add signature algorithm
            metadataMap.put(new TableKey("web.core.certificate.signatureAlgorithm", "fa-solid fa-gears"), new TableValue(certificate.getSigAlgName()));

            // Add key usage if present
            addKeyUsageToMap(certificate, metadataMap);

            // Add extended key usage if present
            addExtendedKeyUsageToMap(certificate, metadataMap);


            // Add basic constraints
            addBasicConstraintsToMap(certificate, metadataMap);

            // Add Subject Alternative Names (SAN) and Issuer Alternative Names (IAN) if present
            addSubjectAndIssuerAltNamesToMap(certificate, metadataMap);

            // Add CRL Distribution Points and OCSP URLs
            addCrlAndOcspToMap(certificate, metadataMap);

            // Add signature
            metadataMap.put(new TableKey("web.core.certificate.signature", "fa-solid fa-signature"), new TableValue(getShortSignatureValue(certificate)));

        } catch (Exception e) {
            metadataMap.put(new TableKey("error"), new TableValue(e.getMessage()));
            // Log the error or handle it as per your application's error handling policy
        }
        return metadataMap;
    }

    /**
     * Adds the basic constraints information of an X.509 certificate to a given metadata map. This method
     * determines whether the certificate is for a Certificate Authority (CA) based on its basic constraints
     * value and then adds this information to the map with a descriptive icon. The basic constraints are
     * used to identify whether a certificate can be used to issue other certificates and its maximum
     * certification path length.
     *
     * @param certificate the X.509 certificate from which to extract basic constraints information.
     * @param metadataMap the map to which the basic constraints information should be added. The map is
     *                    expected to use {@link TableKey} as its keys and {@link TableValue} as its values,
     *                    allowing for a structured and possibly visually enriched representation of the
     *                    certificate's metadata in a UI context.
     */
    private void addBasicConstraintsToMap(X509Certificate certificate, Map<TableKey, TableValue> metadataMap) {
        int basicConstraints = certificate.getBasicConstraints();
        String basicConstraintsStr;
        if (basicConstraints == -1) {
            basicConstraintsStr = "CA: False";
        } else {
            basicConstraintsStr = "CA: True";
        }

        metadataMap.put(new TableKey("web.core.certificate.basicConstraints", "fa-solid fa-triangle-exclamation"), new TableValue(basicConstraintsStr));
    }

    /**
     * Generates a compact string representation of the signature value from a given X.509 certificate.
     * This method converts the signature bytes to a hexadecimal string and then shortens it, keeping
     * the first and last 16 characters separated by an ellipsis for readability. This shortened
     * representation provides a glimpse of the signature without displaying its full length, which
     * can be quite long.
     *
     * @param certificate the X.509 certificate from which to extract and shorten the signature value.
     * @return a shortened string representation of the certificate's signature in hexadecimal format.
     * @throws IllegalArgumentException if the provided certificate is null, ensuring that the operation
     *         is safe from null pointer exceptions.
     */
    private static String getShortSignatureValue(X509Certificate certificate) {
        if (certificate == null) {
            throw new IllegalArgumentException("Certificate cannot be null.");
        }
        String signature = DatatypeConverter.printHexBinary(certificate.getSignature());
        return shortenLongString(signature, 16);
    }


    /**
     * Generates a compact string representation of the public key contained within a given X.509 certificate.
     * This method supports both RSA and EC (Elliptic Curve) public keys, displaying key parameters in a
     * readable format. For RSA keys, it shows the modulus and exponent, while for EC keys, it displays
     * the affine coordinates (X, Y). Long numeric values, such as the RSA modulus or EC coordinates, are
     * shortened for readability.
     *
     * @param certificate the X.509 certificate containing the public key to detail.
     * @return a string representation of the public key's details, including key parameters like modulus
     *         and exponent for RSA keys, or affine X and Y coordinates for EC keys. Unsupported key types
     *         result in a message indicating the key type is not supported.
     * @throws IllegalArgumentException if the provided certificate is null or contains a public key type
     *         not handled by this method.
     */
    private String publicKeyDetails(X509Certificate certificate) {
        PublicKey publicKey = certificate.getPublicKey();

        // For EC Public Keys
        if (publicKey instanceof ECPublicKey ecPublicKey) {
            BigInteger x = ecPublicKey.getW().getAffineX();
            BigInteger y = ecPublicKey.getW().getAffineY();

            // Compact representation of the EC Public Key
            return String.format("EC Public Key\nX: %s\nY: %s",
                    shortenLongString(x.toString(16), 16),
                    shortenLongString(y.toString(16), 16)
            );
        }
        // For RSA Public Keys
        else if (publicKey instanceof RSAPublicKey rsaPublicKey) {
            BigInteger modulus = rsaPublicKey.getModulus();
            BigInteger exponent = rsaPublicKey.getPublicExponent();

            String modulusStr = modulus.toString(16);
            String modulusStrShort = shortenLongString(modulusStr, 16);

            // Compact representation of the RSA public key
            return String.format("RSA Public Key\nModulus: %s\nExponent: %s", modulusStrShort, exponent.toString(16));
        }
        // Further key types could be dealt with here
        else {
            // Return a message for currently unsupported key types
            return "Unsupported public key type: " + publicKey.getAlgorithm();
        }
    }

    /**
     * Shortens a given string by retaining only a specified number of characters from both the beginning
     * and the end of the string, and inserting an ellipsis ("...") in the middle. This method is useful
     * for displaying long strings in a limited space while still providing a hint of the string's start
     * and end. If the length of the given string is less than or equal to twice the length specified for
     * the start and end, the original string is returned without modification.
     *
     * @param string the string to be shortened.
     * @param lengthFirstLast the number of characters to keep from both the beginning and the end of the string.
     * @return the shortened string with the first and last characters retained and an ellipsis in the middle,
     *         or the original string if its length is less than or equal to twice the length specified for the
     *         start and end.
     * @throws IllegalArgumentException if {@code lengthFirstLast} is negative or if the string is null.
     */
    @NotNull
    private static String shortenLongString(String string, int lengthFirstLast) {
        if (string == null) {
            throw new IllegalArgumentException("String cannot be null.");
        }
        if (lengthFirstLast < 0) {
            throw new IllegalArgumentException("Length must be non-negative.");
        }
        if (string.length() <= 2 * lengthFirstLast) {
            return string;
        }
        return string.substring(0, lengthFirstLast) + "..." + string.substring(string.length() - lengthFirstLast);
    }


    /**
     * Adds key usage information to the metadata map.
     *
     * @param certificate The X509Certificate object.
     * @param metadataMap The map to which the key usage information is added.
     */
    private void addKeyUsageToMap(X509Certificate certificate, Map<TableKey, TableValue> metadataMap) {
        boolean[] keyUsage = certificate.getKeyUsage();
        if (keyUsage != null) {
            StringBuilder keyUsageStr = new StringBuilder();
            String[] keyUsages = {
                    "Digital Signature",
                    "Non Repudiation",
                    "Key Encipherment",
                    "Data Encipherment",
                    "Key Agreement",
                    "Certificate Signing",
                    "CRL Signing",
                    "Encipher Only",
                    "Decipher Only"
            };

            for (int i = 0; i < keyUsage.length; i++) {
                if (keyUsage[i]) {
                    if (!keyUsageStr.isEmpty()) {
                        keyUsageStr.append(", ");
                    }
                    keyUsageStr.append(keyUsages[i]);
                }
            }

            if (keyUsageStr.isEmpty()) {
                keyUsageStr.append("None");
            }
            metadataMap.put(new TableKey("web.core.certificate.keyUsage", "fa-solid fa-lock-open"), new TableValue(keyUsageStr.toString()));
        }
    }

    /**
     * Adds extended key usage information to the metadata map.
     *
     * @param certificate The X509Certificate object.
     * @param metadataMap The map to which the extended key usage information is added.
     */
    private void addExtendedKeyUsageToMap(X509Certificate certificate, Map<TableKey, TableValue> metadataMap) {
        try {
            List<String> extKeyUsage = certificate.getExtendedKeyUsage();
            if (extKeyUsage != null) {
                metadataMap.put(new TableKey("web.core.certificate.extendedKeyUsage"), new TableValue(String.join(", ", extKeyUsage)));
            }
        } catch (CertificateParsingException e) {
            // Handle or log the parsing exception as appropriate
        }
    }

    /**
     * Adds Subject Alternative Names (SAN) and Issuer Alternative Names (IAN) information to the metadata map.
     *
     * @param certificate The X509Certificate object.
     * @param metadataMap The map to which the SAN and IAN information is added.
     */
    private void addSubjectAndIssuerAltNamesToMap(X509Certificate certificate, Map<TableKey, TableValue> metadataMap) {
        try {
            Collection<List<?>> subjectAltNames = certificate.getSubjectAlternativeNames();
            if (subjectAltNames != null) {
                StringBuilder sanStr = new StringBuilder();
                for (List<?> san : subjectAltNames) {
                    if (!san.isEmpty()) {
                        sanStr.append(san.get(1).toString()).append("; ");
                    }
                }
                metadataMap.put(new TableKey("web.core.certificate.subjectAlternativeNames"), new TableValue(sanStr.toString()));
            }
        } catch (CertificateParsingException e) {
            // Handle or log the parsing exception as appropriate for SAN
        }

        try {
            Collection<List<?>> issuerAltNames = certificate.getIssuerAlternativeNames();
            if (issuerAltNames != null) {
                StringBuilder ianStr = new StringBuilder();
                for (List<?> ian : issuerAltNames) {
                    if (!ian.isEmpty()) {
                        ianStr.append(ian.get(1).toString()).append("; ");
                    }
                }
                metadataMap.put(new TableKey("web.core.certificate.issuerAlternativeNames"), new TableValue(ianStr.toString()));
            }
        } catch (CertificateParsingException e) {
            // Handle or log the parsing exception as appropriate for IAN
        }
    }

    /**
     * Adds CRL Distribution Points and OCSP URLs to the metadata map.
     *
     * @param certificate The X509Certificate object.
     * @param metadataMap The map to which the CRL and OCSP information is added.
     */
    private void addCrlAndOcspToMap(X509Certificate certificate, Map<TableKey, TableValue> metadataMap) throws IOException {
        // Assuming getCrlDistributionPoints and getOcspUrls are methods returning lists of strings
        // You might need to implement or adjust these methods based on your certificate handling library
        for (String crlDistPoint : getCrlDistributionPoints(certificate)) {
            metadataMap.put(new TableKey("web.core.certificate.crlDistPoint", "fa-regular fa-share-from-square"), new TableValue(crlDistPoint, TableValue.VALUE_TYPE.LINK));
        }
        for (String ocspEndpoint : getOcspUrls(certificate)) {
            metadataMap.put(new TableKey("web.core.certificate.ocspUrl", "fa-solid fa-globe"), new TableValue(ocspEndpoint, TableValue.VALUE_TYPE.LINK));
        }
    }


    /**
     * Converts a byte array into an ASN.1 object. This method is particularly useful for parsing
     * data structures encoded in ASN.1 format. If the ASN.1 object is an instance of {@code DEROctetString},
     * it further decodes the octet string to its contained ASN.1 object.
     *
     * @param data The byte array containing the ASN.1 encoded data.
     * @return The decoded ASN.1 object. If the byte array contains an ASN.1 octet string,
     * the contained ASN.1 object within the octet string is returned.
     * Otherwise, the directly decoded ASN.1 object is returned.
     * @throws IOException If an I/O error occurs during reading from the byte array or
     *                     if the byte array does not represent a valid ASN.1 structure.
     *
     *                     <p>Usage example:</p>
     *                     <pre>
     *                     byte[] asn1Data = ...; // ASN.1 encoded data
     *                     ASN1Primitive asn1Object = toAsn1Object(asn1Data);
     *                     // Proceed with processing the ASN1Primitive as needed
     *                     </pre>
     *
     *                     <p>This method leverages Bouncy Castle's ASN1 parsing capabilities. Ensure that
     *                     the Bouncy Castle library is included in your project's dependencies to use this method.</p>
     */
    public static ASN1Primitive toAsn1Object(byte[] data) throws IOException {
        try (ASN1InputStream asn1InputStream = new ASN1InputStream(data)) {
            ASN1Primitive obj = asn1InputStream.readObject();
            if (obj instanceof DEROctetString) {
                DEROctetString oct = (DEROctetString) obj;
                try (ASN1InputStream asn1InputStreamOct = new ASN1InputStream(new ByteArrayInputStream(oct.getOctets()))) {
                    return asn1InputStreamOct.readObject();
                }
            }
            return obj;
        }
    }

    /**
     * Extracts the CRL (Certificate Revocation List) distribution points from a given X509Certificate.
     * This method parses the CRLDistributionPoints extension (if present) in the certificate and extracts
     * the URLs where the CRLs can be downloaded from.
     *
     * @param certificate The X509Certificate from which to extract the CRL distribution points.
     * @return A list of URLs as Strings where CRLs can be downloaded. Returns an empty list if no CRL
     * distribution points are found or if the certificate does not contain the extension.
     * @throws IOException If an error occurs during the parsing of the CRL distribution points.
     */
    public static List<String> getCrlDistributionPoints(X509Certificate certificate) throws IOException {
        List<String> crlUrls = new ArrayList<>();
        byte[] crlDPExtensionValue = certificate.getExtensionValue(Extension.cRLDistributionPoints.getId());
        if (crlDPExtensionValue != null) {
            ASN1Sequence seq = (ASN1Sequence) toAsn1Object(crlDPExtensionValue);
            CRLDistPoint distPoint = CRLDistPoint.getInstance(seq);
            Arrays.stream(distPoint.getDistributionPoints()).forEach(dp -> {
                GeneralNames gns = GeneralNames.getInstance(dp.getDistributionPoint().getName());
                for (GeneralName gn : gns.getNames()) {
                    if (gn.getTagNo() == GeneralName.uniformResourceIdentifier) {
                        String url = gn.getName().toString();
                        crlUrls.add(url);
                    }
                }
            });
        }
        return crlUrls;
    }

    /**
     * Extracts OCSP URLs from the specified X509Certificate.
     * <p>
     * The method parses the Authority Information Access (AIA) extension of the certificate
     * to find and return the URLs of the OCSP responders. This is useful for determining where
     * to check the revocation status of the certificate.
     * </p>
     *
     * @param certificate The X509Certificate from which the OCSP URLs are to be extracted.
     * @return A list of OCSP URLs found in the certificate. Returns an empty list if no URLs are found or if the
     * certificate does not contain the AIA extension.
     * @throws IOException if an error occurs during parsing of the AIA extension.
     */
    public static List<String> getOcspUrls(X509Certificate certificate) throws IOException {
        List<String> ocspUrls = new ArrayList<>();
        byte[] aiaExtensionValue = certificate.getExtensionValue(Extension.authorityInfoAccess.getId());
        if (aiaExtensionValue != null) {
            ASN1Sequence seq = (ASN1Sequence) toAsn1Object(aiaExtensionValue);
            AuthorityInformationAccess authorityInformationAccess = AuthorityInformationAccess.getInstance(seq);
            Arrays.stream(authorityInformationAccess.getAccessDescriptions()).forEach(ad -> {
                if (ad.getAccessMethod().equals(OCSPObjectIdentifiers.id_pkix_ocsp)) {
                    GeneralName gn = ad.getAccessLocation();
                    if (gn.getTagNo() == GeneralName.uniformResourceIdentifier) {
                        String url = gn.getName().toString();
                        ocspUrls.add(url);
                    }
                }
            });
        }
        return ocspUrls;
    }


}
