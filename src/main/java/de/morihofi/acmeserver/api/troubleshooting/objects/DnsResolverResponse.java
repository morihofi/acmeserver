/*
 * Copyright (c) 2024 Moritz Hofmann <info@morihofi.de>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so,
 *  subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package de.morihofi.acmeserver.api.troubleshooting.objects;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class DnsResolverResponse {

    @SerializedName("resolved-doh")
    private List<Item> dnsOverHttpsResolved = new ArrayList<>();

    @SerializedName("resolved-dns")
    private List<Item> dnsResolved = new ArrayList<>();

    public List<Item> getDnsOverHttpsResolved() {
        return dnsOverHttpsResolved;
    }

    public void setDnsOverHttpsResolved(
            List<Item> dnsOverHttpsResolved) {
        this.dnsOverHttpsResolved = dnsOverHttpsResolved;
    }

    public List<Item> getDnsResolved() {
        return dnsResolved;
    }

    public void setDnsResolved(List<Item> dnsResolved) {
        this.dnsResolved = dnsResolved;
    }

    public static class Item {
        public String name;
        public String data;
        public long ttl;

        public Item() {
        }

        public Item(String name, String data, int ttl) {
            this.name = name;
            this.data = data;
            this.ttl = ttl;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getData() {
            return data;
        }

        public void setData(String data) {
            this.data = data;
        }

        public long getTtl() {
            return ttl;
        }

        public void setTtl(long ttl) {
            this.ttl = ttl;
        }
    }
}
