/*
 * Copyright (c) 2024 Moritz Hofmann <info@morihofi.de>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package de.morihofi.acmeserver.api.download;

import de.morihofi.acmeserver.tools.ServerInstance;
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
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.cert.X509Certificate;
import java.util.Base64;

public class DownloadCaCabHandler implements Handler {
    /**
     * Logger
     */
    private static final Logger LOG = LogManager.getLogger(MethodHandles.lookup().lookupClass());

    private static String toHex(byte[] data) {
        StringBuilder sb = new StringBuilder(data.length * 2);
        for (byte b : data) {
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }

    private final ServerInstance serverInstance;

    public DownloadCaCabHandler(ServerInstance serverInstance) {
        this.serverInstance = serverInstance;
    }

    @Override
    public void handle(@NotNull Context ctx) throws Exception {
        ctx.header("Content-Type", "application/vnd.ms-cab-compressed");

        String xml = createXmlWithCertificate(
                (X509Certificate) serverInstance.getCryptoStoreManager().getKeyStore()
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

        return sw.toString().replace("&#10;", "\r\n").replace("&#13;", "");
    }
}
