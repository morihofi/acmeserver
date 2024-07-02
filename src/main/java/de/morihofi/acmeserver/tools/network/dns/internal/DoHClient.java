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
package de.morihofi.acmeserver.tools.network.dns.internal;

import de.morihofi.acmeserver.tools.network.NetworkClient;
import de.morihofi.acmeserver.tools.network.dns.DNSLookup;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xbill.DNS.Message;
import org.xbill.DNS.Rcode;
import org.xbill.DNS.Record;
import org.xbill.DNS.Section;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.List;

/**
 * DoHClient is a client that performs DNS queries over HTTPS (DoH). It uses the specified DoH URL and a network client to configure its
 * HTTP client.
 */
public class DoHClient {
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final String dohUrl;
    private final OkHttpClient client;

    /**
     * Constructs a DoHClient with the specified DoH URL and network client.
     *
     * @param dohUrl    the URL of the DNS over HTTPS server.
     * @param netClient the network client used to configure the HTTP client.
     */
    public DoHClient(String dohUrl, NetworkClient netClient) {
        this.dohUrl = dohUrl;
        this.client = new OkHttpClient.Builder()
                .proxy(netClient.getProxy())
                // Needed to resolve DoH DNS Host
                .dns(hostname -> DNSLookup.lookupHostnameUsingDnsServerList(hostname, netClient.getDnsServer()))
                .build();
    }

    /**
     * Queries the DoH server with the specified DNS query message and returns the list of DNS records. Based on
     * <a href="https://developers.cloudflare.com/1.1.1.1/encryption/dns-over-https/make-api-requests/dns-wireformat/">DNS Wireformat over
     * HTTPS</a>
     *
     * @param query the DNS query message.
     * @return a list of DNS records returned by the DoH server, or an empty list if the query fails or no records are found.
     * @throws IOException if an I/O error occurs during the query.
     */
    public List<Record> query(Message query) throws IOException {
        byte[] queryBytes = query.toWire();
        Request request = new Request.Builder()
                .header("Content-Type", "application/dns-message")
                .url(dohUrl)
                .post(RequestBody.create(queryBytes))
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response + " for query " + query.toString());
            }
            assert response.body() != null;
            byte[] responseBytes = response.body().bytes();

            return parseAnswer(responseBytes);
        } catch (Exception ex) {
            LOG.error("Exception occurred during DNS over HTTPS lookup", ex);
        }
        return Collections.emptyList();
    }

    /**
     * Parses the given DNS response and extracts the answer section.
     *
     * @param response The DNS response as a byte array.
     * @return An Answer object containing the answer section records.
     * @throws IllegalArgumentException If the DNS response is malformed, contains an error code, or the answer section is empty.
     */
    private List<Record> parseAnswer(byte[] response) throws IllegalArgumentException {
        Message responseMessage;
        try {
            responseMessage = new Message(response);
        } catch (IOException exc) {
            throw new IllegalArgumentException("Returned DNS message is malformed", exc);
        }

        if (responseMessage.getRcode() != Rcode.NOERROR) {
            String rcodeString = Rcode.string(responseMessage.getRcode());
            throw new IllegalArgumentException("Lookup failed with " + rcodeString + " error");
        }

        List<Record> records = responseMessage.getSection(Section.ANSWER);
        // if (records.isEmpty()) {
        //     throw new IllegalArgumentException("Answer data is empty");
        // }

        return records;
    }


}
