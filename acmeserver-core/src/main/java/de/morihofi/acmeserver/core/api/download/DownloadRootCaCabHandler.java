package de.morihofi.acmeserver.core.api.download;

import de.morihofi.acmeserver.core.tools.fileformats.archive.cab.CabFile;
import de.morihofi.acmeserver.server.common.intf.Handler;
import de.morihofi.acmeserver.server.common.intf.HandlerContext;
import de.morihofi.acmeserver.types.database.entities.RootCa;
import de.morihofi.acmeserver.types.intf.IServerInstance;
import lombok.NonNull;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.cert.X509Certificate;
import java.util.Base64;

/**
 * Handler returning the root certificate wrapped into a CAB file for Windows CE/mobile.
 */
public class DownloadRootCaCabHandler implements Handler {
    private final IServerInstance serverInstance;

    public DownloadRootCaCabHandler(@NonNull IServerInstance serverInstance) {
        this.serverInstance = serverInstance;
    }

    private static String toHex(byte[] data) {
        StringBuilder sb = new StringBuilder(data.length * 2);
        for (byte b : data) {
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }

    @Override
    public void handle(@NonNull HandlerContext ctx) throws Exception {
        String uuid = ctx.pathParam("uuid");
        RootCa ca = RootCa.getForUuid(serverInstance, uuid);
        if (ca == null) {
            ctx.status(404);
            return;
        }
        ctx.header("Content-Type", "application/vnd.ms-cab-compressed");

        X509Certificate cert = serverInstance.getCryptoStoreManager().getCertficateAuthorityX509Certificate(ca);
        String xml = createXmlWithCertificate(cert);
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
        Base64.Encoder encoder = Base64.getMimeEncoder(64, "\r\n".getBytes(StandardCharsets.UTF_8));
        byte[] derCert = certificate.getEncoded();
        return encoder.encodeToString(derCert);
    }

    public String createXmlWithCertificate(X509Certificate certificate) throws Exception {
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        Document doc = docBuilder.newDocument();

        Element rootElement = doc.createElement("wap-provisioningdoc");
        doc.appendChild(rootElement);

        Element certStore = doc.createElement("characteristic");
        certStore.setAttribute("type", "CertificateStore");
        rootElement.appendChild(certStore);

        Element root = doc.createElement("characteristic");
        root.setAttribute("type", "ROOT");
        certStore.appendChild(root);

        String fingerprint = getFingerprint(certificate);
        Element fingerprintElement = doc.createElement("characteristic");
        fingerprintElement.setAttribute("type", fingerprint);
        root.appendChild(fingerprintElement);

        Element encodedCert = doc.createElement("parm");
        encodedCert.setAttribute("name", "EncodedCertificate");
        encodedCert.setAttribute("value", getPEMWithoutHeaderAndFooter(certificate));
        fingerprintElement.appendChild(encodedCert);

        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        DOMSource source = new DOMSource(doc);
        StringWriter sw = new StringWriter();
        try (Writer writer = new PrintWriter(sw)) {
            StreamResult result = new StreamResult(writer);
            transformer.transform(source, result);
        }

        return sw.toString().replace("&#10;", "\r\n").replace("&#13;", "");
    }
}
