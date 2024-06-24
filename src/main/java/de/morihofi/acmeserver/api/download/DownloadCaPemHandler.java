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
import de.morihofi.acmeserver.tools.certificate.PemUtil;
import de.morihofi.acmeserver.tools.certificate.cryptoops.CryptoStoreManager;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.openapi.HttpMethod;
import io.javalin.openapi.OpenApi;
import io.javalin.openapi.OpenApiContent;
import io.javalin.openapi.OpenApiResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.lang.invoke.MethodHandles;


@OpenApi(
        summary = "Get the Root certificate as PEM encoded certificate",
        operationId = "getPemCertificate",
        path = "/ca.pem",
        methods = HttpMethod.GET,
        tags = {"Download Root Certificate"},
        responses = {
                @OpenApiResponse(status = "200", content = {
                        @OpenApiContent(
                                from = byte[].class,
                                mimeType = "application/x-x509-ca-cert"
                        )
                })
        }
)
public class DownloadCaPemHandler implements Handler {

    /**
     * Logger
     */
    private static final Logger LOG = LogManager.getLogger(MethodHandles.lookup().getClass());

    /**
     * Instance for accessing the current provisioner
     */
    private final ServerInstance serverInstance;

    /**
     * Endpoint for downloading the root certificate
     */
    public DownloadCaPemHandler(ServerInstance serverInstance) {
        this.serverInstance = serverInstance;
    }

    /**
     * Method for handling the request
     *
     * @param ctx Javalin Context
     * @throws Exception thrown when there was an error processing the request
     */
    @Override
    public void handle(@NotNull Context ctx) throws Exception {
        ctx.header("Content-Type", "application/x-x509-ca-cert");

        String pem = PemUtil.certificateToPEM(
                serverInstance.getCryptoStoreManager()
                        .getKeyStore()
                        .getCertificate(CryptoStoreManager.KEYSTORE_ALIAS_ROOTCA)
                        .getEncoded()
        );

        ctx.result(pem);
    }
}
