package de.morihofi.acmeserver.certificate.api.download;

import de.morihofi.acmeserver.tools.certificate.cryptoops.CryptoStoreManager;
import de.morihofi.acmeserver.tools.fileformats.archive.cab.CabFile;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.cert.X509Certificate;
import java.util.Base64;

public class DownloadCaCabEndpoint implements Handler {
    private CryptoStoreManager cryptoStoreManager;

    /**
     * Logger
     */
    private static final Logger LOG = LogManager.getLogger(MethodHandles.lookup().getClass());

    public DownloadCaCabEndpoint(CryptoStoreManager cryptoStoreManager) {
        this.cryptoStoreManager = cryptoStoreManager;
    }

    @Override
    public void handle(@NotNull Context ctx) throws Exception {
        ctx.header("Content-Type", "application/vnd.ms-cab-compressed");


        String xml = createXmlWithCertificate(
                (X509Certificate) cryptoStoreManager.getKeyStore()
                .getCertificate(CryptoStoreManager.KEYSTORE_ALIAS_ROOTCA)
        );

        byte[] generatedCab = new CabFile.Builder()
                .addFile("_setup.xml", xml.getBytes(StandardCharsets.UTF_8))
                .build().getCabFile();

        ctx.result(generatedCab);
    }

    private String getFingerprint(X509Certificate certificate) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] certBytes = certificate.getEncoded();
        byte[] fingerprintBytes = md.digest(certBytes);
        return toHex(fingerprintBytes);
    }

    private String getPEMWithoutHeaderAndFooter(X509Certificate certificate) throws Exception {
        Base64.Encoder encoder = Base64.getMimeEncoder(64, "\r\n".getBytes());
        byte[] derCert = certificate.getEncoded();
        return encoder.encodeToString(derCert);
    }

    private static String toHex(byte[] data) {
        StringBuilder sb = new StringBuilder(data.length * 2);
        for (byte b : data) {
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }

    public String createXmlWithCertificate(X509Certificate certificate) throws Exception {
        // Erstellen des Dokuments
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        Document doc = docBuilder.newDocument();

        // Root-Element
        Element rootElement = doc.createElement("wap-provisioningdoc");
        doc.appendChild(rootElement);

        // CertificateStore-Element
        Element certStore = doc.createElement("characteristic");
        certStore.setAttribute("type", "CertificateStore");
        rootElement.appendChild(certStore);

        // ROOT-Element
        Element root = doc.createElement("characteristic");
        root.setAttribute("type", "ROOT");
        certStore.appendChild(root);

        // Fingerprint-Element
        String fingerprint = getFingerprint(certificate);
        Element fingerprintElement = doc.createElement("characteristic");
        fingerprintElement.setAttribute("type", fingerprint);
        root.appendChild(fingerprintElement);

        // EncodedCertificate-Element
        Element encodedCert = doc.createElement("parm");
        encodedCert.setAttribute("name", "EncodedCertificate");
        encodedCert.setAttribute("value", getPEMWithoutHeaderAndFooter(certificate));
        fingerprintElement.appendChild(encodedCert);

        // Schreiben in Datei
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        DOMSource source = new DOMSource(doc);
        StringWriter sw = new StringWriter();
        try (Writer writer = new PrintWriter(sw)) {
            StreamResult result = new StreamResult(writer);
            transformer.transform(source, result);
        }

        return sw.toString().replace("&#10;","\r\n").replace("&#13;","");
    }



}
