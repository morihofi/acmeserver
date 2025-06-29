/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.core.servlet.download;

import de.morihofi.certgine.core.tools.fileformats.archive.cab.CabFile;
import de.morihofi.certgine.cryptography.pem.PemUtil;
import de.morihofi.certgine.server.common.intf.Handler;
import de.morihofi.certgine.server.common.intf.HandlerContext;
import de.morihofi.certgine.types.database.entities.authority.RootCa;
import de.morihofi.certgine.types.intf.IServerInstance;
import lombok.NonNull;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.cert.X509Certificate;
import java.util.Base64;

public class DownloadRootCaHandler implements Handler {

    private final IServerInstance serverInstance;
    private final CertificateFormat format;

    public DownloadRootCaHandler(IServerInstance serverInstance, CertificateFormat format) {
        this.serverInstance = serverInstance;
        this.format = format;
    }

    public enum CertificateFormat {
        PEM,
        DER,
        CAB
    }


    @Override
    public void handle(@NonNull HandlerContext ctx) throws Exception {
        String uuid = ctx.pathParam("uuid");
        RootCa ca = RootCa.getForUuid(serverInstance, uuid);
        if (ca == null) {
            ctx.status(404);
            return;
        }

        X509Certificate cert = serverInstance.getCryptoStoreManager().getCertificateAuthorityX509Certificate(ca);

        switch (format) {
            case PEM -> {
                ctx.header("Content-Type", "application/x-x509-ca-cert");
                String pem = PemUtil.certificateToPEM(cert.getEncoded());
                ctx.result(pem);
            }
            case DER -> {
                ctx.header("Content-Type", "application/x-x509-ca-cert");
                ctx.result(cert.getEncoded());
            }
            case CAB -> {
                ctx.header("Content-Type", "application/vnd.ms-cab-compressed");
                String xml = createXmlWithCertificate(cert);
                byte[] generatedCab = new CabFile.Builder()
                        .addFile("_setup.xml", xml.getBytes(StandardCharsets.UTF_8))
                        .build().getCabFile();
                ctx.result(generatedCab);
            }
        }
    }

    private String createXmlWithCertificate(X509Certificate certificate) throws Exception {
        DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
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

        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        StringWriter sw = new StringWriter();
        try (Writer writer = new PrintWriter(sw)) {
            transformer.transform(new DOMSource(doc), new StreamResult(writer));
        }

        return sw.toString().replace("&#10;", "\r\n").replace("&#13;", "");
    }

    private String getFingerprint(X509Certificate certificate) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        return toHex(md.digest(certificate.getEncoded()));
    }

    private String toHex(byte[] data) {
        StringBuilder sb = new StringBuilder(data.length * 2);
        for (byte b : data) {
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }

    private String getPEMWithoutHeaderAndFooter(X509Certificate certificate) throws Exception {
        Base64.Encoder encoder = Base64.getMimeEncoder(64, "\r\n".getBytes(StandardCharsets.UTF_8));
        return encoder.encodeToString(certificate.getEncoded());
    }
}
