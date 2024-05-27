/*
 * Copyright (c) 2024 Moritz Hofmann <info@morihofi.de>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the
 "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish,
 distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to
  the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR
 ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH
 THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package de.morihofi.acmeserver.api.provisioner;

import de.morihofi.acmeserver.api.provisioner.statistics.responses.ProvisionerListEntryResponse;
import de.morihofi.acmeserver.certificate.provisioners.Provisioner;
import de.morihofi.acmeserver.certificate.provisioners.ProvisionerManager;
import de.morihofi.acmeserver.tools.certificate.cryptoops.CryptoStoreManager;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import jakarta.xml.bind.DatatypeConverter;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.ocsp.OCSPObjectIdentifiers;
import org.bouncycastle.asn1.x509.AuthorityInformationAccess;
import org.bouncycastle.asn1.x509.CRLDistPoint;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ProvisionerListHandler implements Handler {

    private final CryptoStoreManager cryptoStoreManager;

    public ProvisionerListHandler(CryptoStoreManager cryptoStoreManager) {
        this.cryptoStoreManager = cryptoStoreManager;
    }

    @Override
    public void handle(@NotNull Context context) throws Exception {

        List<ProvisionerListEntryResponse> provisionerResponse = new ArrayList<>();


        for (Provisioner provisioner : ProvisionerManager.getProvisioners()){

            ProvisionerListEntryResponse provisionerEntry = new ProvisionerListEntryResponse();
            provisionerEntry.setName(provisioner.getProvisionerName());
            provisionerEntry.setDirectoryUrl(provisioner.getApiURL() + "/directory");

            provisionerResponse.add(provisionerEntry);
        }


        context.json(provisionerResponse);
    }





}
