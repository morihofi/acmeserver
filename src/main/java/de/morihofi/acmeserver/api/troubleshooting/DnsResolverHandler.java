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
package de.morihofi.acmeserver.api.troubleshooting;

import de.morihofi.acmeserver.Main;
import de.morihofi.acmeserver.api.troubleshooting.objects.DnsResolverRequest;
import de.morihofi.acmeserver.api.troubleshooting.objects.DnsResolverResponse;
import de.morihofi.acmeserver.tools.network.dns.DNSLookup;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.openapi.HttpMethod;
import io.javalin.openapi.OpenApi;
import io.javalin.openapi.OpenApiContent;
import io.javalin.openapi.OpenApiRequestBody;
import io.javalin.openapi.OpenApiResponse;
import org.jetbrains.annotations.NotNull;
import org.xbill.DNS.A6Record;
import org.xbill.DNS.AAAARecord;
import org.xbill.DNS.AFSDBRecord;
import org.xbill.DNS.APLRecord;
import org.xbill.DNS.ARecord;
import org.xbill.DNS.CAARecord;
import org.xbill.DNS.CDNSKEYRecord;
import org.xbill.DNS.CDSRecord;
import org.xbill.DNS.CERTRecord;
import org.xbill.DNS.CNAMERecord;
import org.xbill.DNS.DHCIDRecord;
import org.xbill.DNS.DLVRecord;
import org.xbill.DNS.DNAMERecord;
import org.xbill.DNS.DNSKEYRecord;
import org.xbill.DNS.DSRecord;
import org.xbill.DNS.GPOSRecord;
import org.xbill.DNS.HINFORecord;
import org.xbill.DNS.HIPRecord;
import org.xbill.DNS.HTTPSRecord;
import org.xbill.DNS.IPSECKEYRecord;
import org.xbill.DNS.ISDNRecord;
import org.xbill.DNS.KEYRecord;
import org.xbill.DNS.KXRecord;
import org.xbill.DNS.LOCRecord;
import org.xbill.DNS.MBRecord;
import org.xbill.DNS.MGRecord;
import org.xbill.DNS.MINFORecord;
import org.xbill.DNS.MRRecord;
import org.xbill.DNS.MXRecord;
import org.xbill.DNS.NAPTRRecord;
import org.xbill.DNS.NSAPRecord;
import org.xbill.DNS.NSAP_PTRRecord;
import org.xbill.DNS.NSEC3PARAMRecord;
import org.xbill.DNS.NSEC3Record;
import org.xbill.DNS.NSECRecord;
import org.xbill.DNS.NSRecord;
import org.xbill.DNS.NULLRecord;
import org.xbill.DNS.NXTRecord;
import org.xbill.DNS.OPENPGPKEYRecord;
import org.xbill.DNS.OPTRecord;
import org.xbill.DNS.PTRRecord;
import org.xbill.DNS.PXRecord;
import org.xbill.DNS.RPRecord;
import org.xbill.DNS.RRSIGRecord;
import org.xbill.DNS.RTRecord;
import org.xbill.DNS.Record;
import org.xbill.DNS.SIGRecord;
import org.xbill.DNS.SMIMEARecord;
import org.xbill.DNS.SOARecord;
import org.xbill.DNS.SPFRecord;
import org.xbill.DNS.SRVRecord;
import org.xbill.DNS.SSHFPRecord;
import org.xbill.DNS.SVCBRecord;
import org.xbill.DNS.TLSARecord;
import org.xbill.DNS.TXTRecord;
import org.xbill.DNS.Type;
import org.xbill.DNS.URIRecord;
import org.xbill.DNS.WKSRecord;
import org.xbill.DNS.X25Record;

import java.util.ArrayList;
import java.util.List;

public class DnsResolverHandler implements Handler {
    @Override
    @OpenApi(
            summary = "Resolve a DNS Query",
            operationId = "resolveDns",
            path = "/api/troubleshooting/dns-resolver",
            methods = HttpMethod.POST,
            tags = {"Troubleshooting"},
            requestBody = @OpenApiRequestBody(
                    required = true,
                    description = "DNS Query Request",
                    content = {
                            @OpenApiContent(
                                    from = DnsResolverRequest.class,
                                    mimeType = "application/json"
                            )
                    }),
            responses = {
                    @OpenApiResponse(
                            status = "200",
                            content = {
                                    @OpenApiContent(
                                            from = DnsResolverResponse.class,
                                            mimeType = "application/json"
                                    )
                            })
            }
    )
    public void handle(@NotNull Context context) throws Exception {
        DnsResolverRequest request = context.bodyAsClass(DnsResolverRequest.class);

        DnsResolverResponse response = new DnsResolverResponse();

        int dnsType = Type.value(request.getType().toUpperCase());

        response.getDnsOverHttpsResolved().addAll(
                convertToItems(
                        DNSLookup.performDoHLookup(
                                request.getDnsName(),
                                dnsType,
                                Main.networkClient.getDoHClient()
                        )
                )
        );

        response.getDnsResolved().addAll(convertToItems(
                DNSLookup.performDnsServerLookup(
                        request.getDnsName(),
                        dnsType,
                        Main.appConfig.getNetwork().getDnsConfig().getDnsServers()
                )
        ));

        context.json(response);
    }

    private List<DnsResolverResponse.Item> convertToItems(List<Record> records) {
        List<DnsResolverResponse.Item> items = new ArrayList<>();
        for (Record dnsRecord : records) {
            DnsResolverResponse.Item item = new DnsResolverResponse.Item();
            item.setName(dnsRecord.getName().toString());
            item.setTtl(dnsRecord.getTTL());

            // Determine the type of the record and extract data accordingly
            switch (dnsRecord.getType()) {
                case Type.A -> {
                    ARecord aRecord = (ARecord) dnsRecord;
                    item.setData(aRecord.getAddress().getHostAddress());
                }
                case Type.NS -> {
                    NSRecord nsRecord = (NSRecord) dnsRecord;
                    item.setData(nsRecord.getTarget().toString());
                }
                case Type.CNAME -> {
                    CNAMERecord cnameRecord = (CNAMERecord) dnsRecord;
                    item.setData(cnameRecord.getTarget().toString());
                }
                case Type.SOA -> {
                    SOARecord soaRecord = (SOARecord) dnsRecord;
                    item.setData(soaRecord.getHost().toString() + " " + soaRecord.getAdmin().toString());
                }
                case Type.MB -> {
                    MBRecord mbRecord = (MBRecord) dnsRecord;
                    item.setData(mbRecord.getMailbox().toString());
                }
                case Type.MG -> {
                    MGRecord mgRecord = (MGRecord) dnsRecord;
                    item.setData(mgRecord.getMailbox().toString());
                }
                case Type.MR -> {
                    MRRecord mrRecord = (MRRecord) dnsRecord;
                    item.setData(mrRecord.getNewName().toString());
                }
                case Type.NULL -> {
                    NULLRecord nullRecord = (NULLRecord) dnsRecord;
                    item.setData(nullRecord.rdataToString());
                }
                case Type.WKS -> {
                    WKSRecord wksRecord = (WKSRecord) dnsRecord;
                    item.setData(wksRecord.getAddress().getHostAddress() + " " + wksRecord.getProtocol() + " " + wksRecord.getServices()
                            .toString());
                }
                case Type.PTR -> {
                    PTRRecord ptrRecord = (PTRRecord) dnsRecord;
                    item.setData(ptrRecord.getTarget().toString());
                }
                case Type.HINFO -> {
                    HINFORecord hinfoRecord = (HINFORecord) dnsRecord;
                    item.setData(hinfoRecord.getCPU() + " " + hinfoRecord.getOS());
                }
                case Type.MINFO -> {
                    MINFORecord minfoRecord = (MINFORecord) dnsRecord;
                    item.setData(minfoRecord.getResponsibleAddress().toString() + " " + minfoRecord.getErrorAddress().toString());
                }
                case Type.MX -> {
                    MXRecord mxRecord = (MXRecord) dnsRecord;
                    item.setData(mxRecord.getTarget().toString() + " " + mxRecord.getPriority());
                }
                case Type.TXT -> {
                    TXTRecord txtRecord = (TXTRecord) dnsRecord;
                    item.setData(txtRecord.getStrings().toString());
                }
                case Type.RP -> {
                    RPRecord rpRecord = (RPRecord) dnsRecord;
                    item.setData(rpRecord.getMailbox().toString() + " " + rpRecord.getTextDomain().toString());
                }
                case Type.AFSDB -> {
                    AFSDBRecord afsdbRecord = (AFSDBRecord) dnsRecord;
                    item.setData(afsdbRecord.getHost().toString() + " " + afsdbRecord.getSubtype());
                }
                case Type.X25 -> {
                    X25Record x25Record = (X25Record) dnsRecord;
                    item.setData(x25Record.getAddress());
                }
                case Type.ISDN -> {
                    ISDNRecord isdnRecord = (ISDNRecord) dnsRecord;
                    item.setData(isdnRecord.getAddress() + " " + isdnRecord.getSubAddress());
                }
                case Type.RT -> {
                    RTRecord rtRecord = (RTRecord) dnsRecord;
                    item.setData(rtRecord.getIntermediateHost() + " " + rtRecord.getPreference());
                }
                case Type.NSAP -> {
                    NSAPRecord nsapRecord = (NSAPRecord) dnsRecord;
                    item.setData(nsapRecord.getAddress());
                }
                case Type.NSAP_PTR -> {
                    NSAP_PTRRecord nsapPtrRecord = (NSAP_PTRRecord) dnsRecord;
                    item.setData(nsapPtrRecord.getTarget().toString());
                }
                case Type.SIG -> {
                    SIGRecord sigRecord = (SIGRecord) dnsRecord;
                    item.setData(sigRecord.rdataToString());
                }
                case Type.KEY -> {
                    KEYRecord keyRecord = (KEYRecord) dnsRecord;
                    item.setData(keyRecord.rdataToString());
                }
                case Type.PX -> {
                    PXRecord pxRecord = (PXRecord) dnsRecord;
                    item.setData(pxRecord.getMap822().toString() + " " + pxRecord.getMapX400().toString());
                }
                case Type.GPOS -> {
                    GPOSRecord gposRecord = (GPOSRecord) dnsRecord;
                    item.setData(gposRecord.getLongitude() + " " + gposRecord.getLatitude() + " " + gposRecord.getAltitude());
                }
                case Type.AAAA -> {
                    AAAARecord aaaaRecord = (AAAARecord) dnsRecord;
                    item.setData(aaaaRecord.getAddress().getHostAddress());
                }
                case Type.LOC -> {
                    LOCRecord locRecord = (LOCRecord) dnsRecord;
                    item.setData(locRecord.toString());
                }
                case Type.NXT -> {
                    NXTRecord nxtRecord = (NXTRecord) dnsRecord;
                    item.setData(nxtRecord.toString());
                }
                case Type.SRV -> {
                    SRVRecord srvRecord = (SRVRecord) dnsRecord;
                    item.setData(srvRecord.getTarget().toString() + " " + srvRecord.getPort() + " " + srvRecord.getPriority() + " "
                            + srvRecord.getWeight());
                }
                case Type.NAPTR -> {
                    NAPTRRecord naptrRecord = (NAPTRRecord) dnsRecord;
                    item.setData(naptrRecord.getOrder() + " " + naptrRecord.getPreference() + " " + naptrRecord.getFlags() + " "
                            + naptrRecord.getService() + " " + naptrRecord.getRegexp() + " " + naptrRecord.getReplacement());
                }
                case Type.KX -> {
                    KXRecord kxRecord = (KXRecord) dnsRecord;
                    item.setData(kxRecord.getTarget().toString() + " " + kxRecord.getPreference());
                }
                case Type.CERT -> {
                    CERTRecord certRecord = (CERTRecord) dnsRecord;
                    item.setData(certRecord.rdataToString());
                }
                case Type.A6 -> {
                    A6Record a6Record = (A6Record) dnsRecord;
                    item.setData(a6Record.rdataToString());
                }
                case Type.DNAME -> {
                    DNAMERecord dnameRecord = (DNAMERecord) dnsRecord;
                    item.setData(dnameRecord.getTarget().toString());
                }
                case Type.OPT -> {
                    OPTRecord optRecord = (OPTRecord) dnsRecord;
                    item.setData(optRecord.rdataToString());
                }
                case Type.APL -> {
                    APLRecord aplRecord = (APLRecord) dnsRecord;
                    item.setData(aplRecord.rdataToString());
                }
                case Type.DS -> {
                    DSRecord dsRecord = (DSRecord) dnsRecord;
                    item.setData(dsRecord.rdataToString());
                }
                case Type.SSHFP -> {
                    SSHFPRecord sshfpRecord = (SSHFPRecord) dnsRecord;
                    item.setData(sshfpRecord.rdataToString());
                }
                case Type.IPSECKEY -> {
                    IPSECKEYRecord ipseckeyRecord = (IPSECKEYRecord) dnsRecord;
                    item.setData(ipseckeyRecord.rdataToString());
                }
                case Type.RRSIG -> {
                    RRSIGRecord rrsigRecord = (RRSIGRecord) dnsRecord;
                    item.setData(rrsigRecord.rdataToString());
                }
                case Type.NSEC -> {
                    NSECRecord nsecRecord = (NSECRecord) dnsRecord;
                    item.setData(nsecRecord.rdataToString());
                }
                case Type.DNSKEY -> {
                    DNSKEYRecord dnskeyRecord = (DNSKEYRecord) dnsRecord;
                    item.setData(dnskeyRecord.rdataToString());
                }
                case Type.DHCID -> {
                    DHCIDRecord dhcidRecord = (DHCIDRecord) dnsRecord;
                    item.setData(dhcidRecord.rdataToString());
                }
                case Type.NSEC3 -> {
                    NSEC3Record nsec3Record = (NSEC3Record) dnsRecord;
                    item.setData(nsec3Record.rdataToString());
                }
                case Type.NSEC3PARAM -> {
                    NSEC3PARAMRecord nsec3paramRecord = (NSEC3PARAMRecord) dnsRecord;
                    item.setData(nsec3paramRecord.rdataToString());
                }
                case Type.TLSA -> {
                    TLSARecord tlsaRecord = (TLSARecord) dnsRecord;
                    item.setData(tlsaRecord.rdataToString());
                }
                case Type.SMIMEA -> {
                    SMIMEARecord smimeaRecord = (SMIMEARecord) dnsRecord;
                    item.setData(smimeaRecord.rdataToString());
                }
                case Type.HIP -> {
                    HIPRecord hipRecord = (HIPRecord) dnsRecord;
                    item.setData(hipRecord.rdataToString());
                }
                case Type.CDS -> {
                    CDSRecord cdsRecord = (CDSRecord) dnsRecord;
                    item.setData(cdsRecord.rdataToString());
                }
                case Type.CDNSKEY -> {
                    CDNSKEYRecord cdnskeyRecord = (CDNSKEYRecord) dnsRecord;
                    item.setData(cdnskeyRecord.rdataToString());
                }
                case Type.OPENPGPKEY -> {
                    OPENPGPKEYRecord openpgpkeyRecord = (OPENPGPKEYRecord) dnsRecord;
                    item.setData(openpgpkeyRecord.rdataToString());
                }
                case Type.SVCB -> {
                    SVCBRecord svcbRecord = (SVCBRecord) dnsRecord;
                    item.setData(svcbRecord.rdataToString());
                }
                case Type.HTTPS -> {
                    HTTPSRecord httpsRecord = (HTTPSRecord) dnsRecord;
                    item.setData(httpsRecord.rdataToString());
                }
                case Type.SPF -> {
                    SPFRecord spfRecord = (SPFRecord) dnsRecord;
                    item.setData(spfRecord.getStrings().toString());
                }
                case Type.URI -> {
                    URIRecord uriRecord = (URIRecord) dnsRecord;
                    item.setData(uriRecord.getTarget());
                }
                case Type.CAA -> {
                    CAARecord caaRecord = (CAARecord) dnsRecord;
                    item.setData(caaRecord.getValue());
                }
                case Type.DLV -> {
                    DLVRecord dlvRecord = (DLVRecord) dnsRecord;
                    item.setData(dlvRecord.rdataToString());
                }
                default -> item.setData(dnsRecord.rdataToString());
            }

            items.add(item);
        }
        return items;
    }
}
