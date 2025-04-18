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

package de.morihofi.acmeserver.core.api.provisioner.byname;

import de.morihofi.acmeserver.core.api.provisioner.byname.responses.ProvisionerByNameInfoResponse;
import de.morihofi.acmeserver.core.certificate.provisioners.Provisioner;
import de.morihofi.acmeserver.core.certificate.provisioners.ProvisionerManager;
import de.morihofi.acmeserver.core.tools.ServerInstance;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.ocsp.OCSPObjectIdentifiers;
import org.bouncycastle.asn1.x509.*;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;

/**
 * Handler for fetching provisioner information by name.
 * This handler processes requests to retrieve detailed information about a specific provisioner,
 * including terms of service, website, IP allowance, DNS wildcard allowance, and revocation URLs.
 */
public class ProvisionerByNameInfoHandler implements Handler {

    /**
     * The instance of the server managing the server-specific configurations and operations.
     */
    private final ServerInstance serverInstance;

    /**
     * Constructs a new handler for fetching provisioner information by name.
     *
     * @param serverInstance The instance of the server managing the server-specific configurations and operations.
     */
    public ProvisionerByNameInfoHandler(ServerInstance serverInstance) {
        this.serverInstance = serverInstance;
    }

    /**
     * Handles the request to fetch provisioner information by name.
     *
     * @param context The context of the request containing the path parameter for provisioner name.
     * @throws Exception If an error occurs while processing the request.
     */
    @Override
    public void handle(@NotNull Context context) throws Exception {
        String provisionerName = context.pathParam("provisionerName");

        Provisioner provisioner = ProvisionerManager.getProvisionerForName(provisionerName);

        if (provisioner == null) {
            return;
        }

        ProvisionerByNameInfoResponse response = new ProvisionerByNameInfoResponse();
        // Weblinks
        response.setTermsOfService(provisioner.getAcmeMetadataConfig().getTos());
        response.setWebsite(provisioner.getAcmeMetadataConfig().getWebsite());
        // Some booleans
        response.setIpAllowed(provisioner.isIpAllowed());
        response.setDnsWildcardAllowed(provisioner.isWildcardAllowed());
        // Revokation
        response.setCrlUrl(provisioner.getFullCrlUrl());
        response.setOcspUrl(provisioner.getFullOcspUrl());

        context.json(response);
    }


    /**
     * Generates a compact string representation of the signature value from a given X.509 certificate. This method converts the signature
     * bytes to a hexadecimal string and then shortens it, keeping the first and last 16 characters separated by an ellipsis for
     * readability. This shortened representation provides a glimpse of the signature without displaying its full length, which can be quite
     * long.
     *
     * @param certificate the X.509 certificate from which to extract and shorten the signature value.
     * @return a shortened string representation of the certificate's signature in hexadecimal format.
     * @throws IllegalArgumentException if the provided certificate is null, ensuring that the operation is safe from null pointer
     *                                  exceptions.
     */
    private static String getShortSignatureValue(X509Certificate certificate) {
        if (certificate == null) {
            throw new IllegalArgumentException("Certificate cannot be null.");
        }
        String signature = HexFormat.of().formatHex(certificate.getSignature());
        return shortenLongString(signature, 16);
    }

    /**
     * Shortens a given string by retaining only a specified number of characters from both the beginning and the end of the string, and
     * inserting an ellipsis ("...") in the middle. This method is useful for displaying long strings in a limited space while still
     * providing a hint of the string's start and end. If the length of the given string is less than or equal to twice the length specified
     * for the start and end, the original string is returned without modification.
     *
     * @param string          the string to be shortened.
     * @param lengthFirstLast the number of characters to keep from both the beginning and the end of the string.
     * @return the shortened string with the first and last characters retained and an ellipsis in the middle, or the original string if its
     * length is less than or equal to twice the length specified for the start and end.
     * @throws IllegalArgumentException if {@code lengthFirstLast} is negative or if the string is null.
     */
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
     * Converts a byte array into an ASN.1 object. This method is particularly useful for parsing data structures encoded in ASN.1 format.
     * If the ASN.1 object is an instance of {@code DEROctetString}, it further decodes the octet string to its contained ASN.1 object.
     *
     * @param data The byte array containing the ASN.1 encoded data.
     * @return The decoded ASN.1 object. If the byte array contains an ASN.1 octet string, the contained ASN.1 object within the octet
     * string is returned. Otherwise, the directly decoded ASN.1 object is returned.
     * @throws IOException If an I/O error occurs during reading from the byte array or if the byte array does not represent a valid ASN.1
     *                     structure.
     *
     *                     <p>Usage example:</p>
     *                     <pre>
     *                                                             byte[] asn1Data = ...; // ASN.1 encoded data
     *                                                             ASN1Primitive asn1Object = toAsn1Object(asn1Data);
     *                                                             // Proceed with processing the ASN1Primitive as needed
     *                                                             </pre>
     *
     *                     <p>This method leverages Bouncy Castle's ASN1 parsing capabilities. Ensure that
     *                     the Bouncy Castle library is included in your project's dependencies to use this method.</p>
     */
    public static ASN1Primitive toAsn1Object(byte[] data) throws IOException {
        try (ASN1InputStream asn1InputStream = new ASN1InputStream(data)) {
            ASN1Primitive obj = asn1InputStream.readObject();
            if (obj instanceof DEROctetString oct) {
                try (ASN1InputStream asn1InputStreamOct = new ASN1InputStream(new ByteArrayInputStream(oct.getOctets()))) {
                    return asn1InputStreamOct.readObject();
                }
            }
            return obj;
        }
    }

    /**
     * Extracts the CRL (Certificate Revocation List) distribution points from a given X509Certificate. This method parses the
     * CRLDistributionPoints extension (if present) in the certificate and extracts the URLs where the CRLs can be downloaded from.
     *
     * @param certificate The X509Certificate from which to extract the CRL distribution points.
     * @return A list of URLs as Strings where CRLs can be downloaded. Returns an empty list if no CRL distribution points are found or if
     * the certificate does not contain the extension.
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
     * The method parses the Authority Information Access (AIA) extension of the certificate to find and return the URLs of the OCSP
     * responders. This is useful for determining where to check the revocation status of the certificate.
     * </p>
     *
     * @param certificate The X509Certificate from which the OCSP URLs are to be extracted.
     * @return A list of OCSP URLs found in the certificate. Returns an empty list if no URLs are found or if the certificate does not
     * contain the AIA extension.
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
