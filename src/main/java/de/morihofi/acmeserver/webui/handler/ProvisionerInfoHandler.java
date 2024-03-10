package de.morihofi.acmeserver.webui.handler;

import de.morihofi.acmeserver.certificate.acme.api.Provisioner;
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
import org.bouncycastle.asn1.x509.*;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.util.*;

public class ProvisionerInfoHandler implements Handler {

    private final CryptoStoreManager cryptoStoreManager;
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

    @NotNull
    private Map<TableKey, TableValue> getIntermediateMetadata(Provisioner provisioner) {
        Map<TableKey, TableValue> provisionerIntermediateTableMap = new LinkedHashMap<>();
        try {
            X509Certificate provisionerCertificate = cryptoStoreManager.getX509CertificateForProvisioner(provisioner.getProvisionerName());

            // Aussteller und Subjekt
            provisionerIntermediateTableMap.put(new TableKey("web.core.certificate.subject"), new TableValue(provisionerCertificate.getSubjectX500Principal().getName()));
            provisionerIntermediateTableMap.put(new TableKey("web.core.certificate.issuer"), new TableValue(provisionerCertificate.getIssuerX500Principal().getName()));

            // Validity dates
            provisionerIntermediateTableMap.put(new TableKey("web.core.certificate.notBefore"), new TableValue(dateFormat.format(provisionerCertificate.getNotBefore())));
            provisionerIntermediateTableMap.put(new TableKey("web.core.certificate.notAfter"), new TableValue(dateFormat.format(provisionerCertificate.getNotAfter())));

            // Seriennummer
            provisionerIntermediateTableMap.put(new TableKey("web.core.certificate.serialNumber"), new TableValue(provisionerCertificate.getSerialNumber().toString()));

            // Öffentlicher Schlüssel
            provisionerIntermediateTableMap.put(new TableKey("web.core.certificate.publicKey"), new TableValue(provisionerCertificate.getPublicKey().toString()));

            // Signaturalgorithmus
            provisionerIntermediateTableMap.put(new TableKey("web.core.certificate.signatureAlgorithm"), new TableValue(provisionerCertificate.getSigAlgName()));

            // Key Usage
            boolean[] keyUsage = provisionerCertificate.getKeyUsage();
            if (keyUsage != null) {
                StringBuilder keyUsageStr = new StringBuilder();
                for (boolean usage : keyUsage) {
                    keyUsageStr.append(usage ? "1" : "0");
                }
                provisionerIntermediateTableMap.put(new TableKey("web.core.certificate.keyUsage"), new TableValue(keyUsageStr.toString()));
            }

            // Extended Key Usage
            try {
                List<String> extKeyUsage = provisionerCertificate.getExtendedKeyUsage();
                if (extKeyUsage != null) {
                    provisionerIntermediateTableMap.put(new TableKey("web.core.certificate.extendedKeyUsage"), new TableValue(String.join(", ", extKeyUsage)));
                }
            } catch (CertificateParsingException e) {
                // Behandlung der Ausnahme
            }

            // Basic Constraints
            int basicConstraints = provisionerCertificate.getBasicConstraints();
            provisionerIntermediateTableMap.put(new TableKey("web.core.certificate.basicConstraints"), new TableValue(Integer.toString(basicConstraints)));

            // Subject Alternative Names
            try {
                Collection<List<?>> subjectAltNames = provisionerCertificate.getSubjectAlternativeNames();
                if (subjectAltNames != null) {
                    StringBuilder sanStr = new StringBuilder();
                    for (List<?> san : subjectAltNames) {
                        if (!san.isEmpty()) {
                            sanStr.append(san.get(1).toString()).append("; ");
                        }
                    }
                    provisionerIntermediateTableMap.put(new TableKey("web.core.certificate.subjectAlternativeNames"), new TableValue(sanStr.toString()));
                }
            } catch (CertificateParsingException e) {
                // Behandlung der Ausnahme
            }

            // Issuer Alternative Names
            try {
                Collection<List<?>> issuerAltNames = provisionerCertificate.getIssuerAlternativeNames();
                if (issuerAltNames != null) {
                    StringBuilder ianStr = new StringBuilder();
                    for (List<?> ian : issuerAltNames) {
                        if (!ian.isEmpty()) {
                            ianStr.append(ian.get(1).toString()).append("; ");
                        }
                    }
                    provisionerIntermediateTableMap.put(new TableKey("web.core.certificate.issuerAlternativeNames"), new TableValue(ianStr.toString()));
                }
            } catch (CertificateParsingException e) {
                // Behandlung der Ausnahme
            }

            for (String crlDistPoint : getCrlDistributionPoints(provisionerCertificate)) {
                provisionerIntermediateTableMap.put(new TableKey("web.core.certificate.crlDistPoint"), new TableValue(crlDistPoint, TableValue.VALUE_TYPE.LINK));
            }
            for (String ocspEndpoint : getOcspUrls(provisionerCertificate)) {
                provisionerIntermediateTableMap.put(new TableKey("web.core.certificate.ocspUrl"), new TableValue(ocspEndpoint, TableValue.VALUE_TYPE.LINK));
            }

            // Signatur
            provisionerIntermediateTableMap.put(new TableKey("web.core.certificate.signature"), new TableValue(DatatypeConverter.printHexBinary(provisionerCertificate.getSignature())));





        } catch (Exception e) {
            provisionerIntermediateTableMap.put(new TableKey("error"), new TableValue(e.getMessage()));
            //FIXME
        }
        return provisionerIntermediateTableMap;
    }

    @NotNull
    private Map<TableKey, TableValue> getProvisionerMetadata(Provisioner provisioner) {
        Map<TableKey, TableValue> provisionerTableMap = new LinkedHashMap<>();
        provisionerTableMap.put(new TableKey("web.core.provisioner.meta.name", "fa-solid fa-info"), new TableValue(provisioner.getProvisionerName()));
        provisionerTableMap.put(new TableKey("web.core.provisioner.meta.termsOfService", "fa-regular fa-file-lines"), new TableValue(provisioner.getAcmeMetadataConfig().getTos(), TableValue.VALUE_TYPE.LINK));
        provisionerTableMap.put(new TableKey("web.core.provisioner.meta.website", "fa-solid fa-globe"), new TableValue(provisioner.getAcmeMetadataConfig().getWebsite(), TableValue.VALUE_TYPE.LINK));
        return provisionerTableMap;
    }

    // Hilfsfunktion zum Konvertieren der Extension Value in ASN.1-Objekt
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

    // Funktion zum Extrahieren der CRL Distribution Points
    public static List<String> getCrlDistributionPoints(X509Certificate certificate) throws Exception {
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

    // Direkte Verwendung des OCSP OID
    private static final ASN1ObjectIdentifier OCSP_OID = new ASN1ObjectIdentifier("1.3.6.1.5.5.7.48.1");


    // Funktion zum Extrahieren der OCSP-URL
    public static List<String> getOcspUrls(X509Certificate certificate) throws Exception {
        List<String> ocspUrls = new ArrayList<>();
        byte[] aiaExtensionValue = certificate.getExtensionValue(Extension.authorityInfoAccess.getId());
        if (aiaExtensionValue != null) {
            ASN1Sequence seq = (ASN1Sequence) toAsn1Object(aiaExtensionValue);
            AuthorityInformationAccess authorityInformationAccess = AuthorityInformationAccess.getInstance(seq);
            Arrays.stream(authorityInformationAccess.getAccessDescriptions()).forEach(ad -> {
                if (ad.getAccessMethod().equals(OCSP_OID)) {
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
