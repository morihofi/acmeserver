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
package de.morihofi.acmeserver.core.api.mgmt.troubleshooting;

import de.morihofi.acmeserver.core.api.mgmt.troubleshooting.objects.DnsResolverRequest;
import de.morihofi.acmeserver.core.api.mgmt.troubleshooting.objects.DnsResolverResponse;
import de.morihofi.acmeserver.server.common.intf.Handler;
import de.morihofi.acmeserver.server.common.intf.HandlerContext;
import de.morihofi.acmeserver.types.intf.IServerInstance;
import de.morihofi.acmeserver.utils.network.dns.DNSLookup;

import lombok.NonNull;
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
import java.util.Arrays;
import java.util.List;

public class DnsResolverHandler implements Handler {

    @Override
    public void handle(@NonNull HandlerContext context) throws Exception {
        DnsResolverRequest request = context.bodyAsClass(DnsResolverRequest.class);

        DnsResolverResponse response = new DnsResolverResponse();

        int dnsType = Type.value(request.getType().toUpperCase());

        response.getDnsOverHttpsResolved().addAll(
                convertToItems(
                        DNSLookup.performDoHLookup(
                                request.getDnsName(),
                                dnsType,
                                serverInstance.getNetworkClient()
                                        .getDoHClient()
                        )
                )
        );

        response.getDnsResolved().addAll(convertToItems(
                DNSLookup.performDnsServerLookup(
                        request.getDnsName(),
                        dnsType,
                        serverInstance.getAppConfig()
                                .getNetwork()
                                .getDnsConfig()
                                .getDnsServers()
                )
        ));

        context.json(response);
    }

    private final IServerInstance serverInstance;

    public DnsResolverHandler(IServerInstance serverInstance) {
        this.serverInstance = serverInstance;
    }

    private List<DnsResolverResponse.Item> convertToItems(List<Record> records) {
        List<DnsResolverResponse.Item> items = new ArrayList<>();
        for (Record dnsRecord : records) {
            DnsResolverResponse.Item.ItemBuilder itemBuilder = DnsResolverResponse.Item.builder();
            itemBuilder.name(dnsRecord.getName().toString());
            itemBuilder.ttl(dnsRecord.getTTL());

            // Determine the type of the record and extract data accordingly
            switch (dnsRecord.getType()) {
                case Type.A -> {
                    ARecord aRecord = (ARecord) dnsRecord;
                    itemBuilder.data(aRecord.getAddress().getHostAddress());
                }
                case Type.NS -> {
                    NSRecord nsRecord = (NSRecord) dnsRecord;
                    itemBuilder.data(nsRecord.getTarget().toString());
                }
                case Type.CNAME -> {
                    CNAMERecord cnameRecord = (CNAMERecord) dnsRecord;
                    itemBuilder.data(cnameRecord.getTarget().toString());
                }
                case Type.SOA -> {
                    SOARecord soaRecord = (SOARecord) dnsRecord;
                    itemBuilder.data(soaRecord.getHost().toString() + " " + soaRecord.getAdmin().toString());
                }
                case Type.MB -> {
                    MBRecord mbRecord = (MBRecord) dnsRecord;
                    itemBuilder.data(mbRecord.getMailbox().toString());
                }
                case Type.MG -> {
                    MGRecord mgRecord = (MGRecord) dnsRecord;
                    itemBuilder.data(mgRecord.getMailbox().toString());
                }
                case Type.MR -> {
                    MRRecord mrRecord = (MRRecord) dnsRecord;
                    itemBuilder.data(mrRecord.getNewName().toString());
                }
                case Type.NULL -> {
                    NULLRecord nullRecord = (NULLRecord) dnsRecord;
                    itemBuilder.data(nullRecord.rdataToString());
                }
                case Type.WKS -> {
                    WKSRecord wksRecord = (WKSRecord) dnsRecord;
                    itemBuilder.data(wksRecord.getAddress().getHostAddress() + " " + wksRecord.getProtocol() + " " + Arrays.toString(wksRecord.getServices()));
                }
                case Type.PTR -> {
                    PTRRecord ptrRecord = (PTRRecord) dnsRecord;
                    itemBuilder.data(ptrRecord.getTarget().toString());
                }
                case Type.HINFO -> {
                    HINFORecord hinfoRecord = (HINFORecord) dnsRecord;
                    itemBuilder.data(hinfoRecord.getCPU() + " " + hinfoRecord.getOS());
                }
                case Type.MINFO -> {
                    MINFORecord minfoRecord = (MINFORecord) dnsRecord;
                    itemBuilder.data(minfoRecord.getResponsibleAddress().toString() + " " + minfoRecord.getErrorAddress().toString());
                }
                case Type.MX -> {
                    MXRecord mxRecord = (MXRecord) dnsRecord;
                    itemBuilder.data(mxRecord.getTarget().toString() + " " + mxRecord.getPriority());
                }
                case Type.TXT -> {
                    TXTRecord txtRecord = (TXTRecord) dnsRecord;
                    itemBuilder.data(txtRecord.getStrings().toString());
                }
                case Type.RP -> {
                    RPRecord rpRecord = (RPRecord) dnsRecord;
                    itemBuilder.data(rpRecord.getMailbox().toString() + " " + rpRecord.getTextDomain().toString());
                }
                case Type.AFSDB -> {
                    AFSDBRecord afsdbRecord = (AFSDBRecord) dnsRecord;
                    itemBuilder.data(afsdbRecord.getHost().toString() + " " + afsdbRecord.getSubtype());
                }
                case Type.X25 -> {
                    X25Record x25Record = (X25Record) dnsRecord;
                    itemBuilder.data(x25Record.getAddress());
                }
                case Type.ISDN -> {
                    ISDNRecord isdnRecord = (ISDNRecord) dnsRecord;
                    itemBuilder.data(isdnRecord.getAddress() + " " + isdnRecord.getSubAddress());
                }
                case Type.RT -> {
                    RTRecord rtRecord = (RTRecord) dnsRecord;
                    itemBuilder.data(rtRecord.getIntermediateHost() + " " + rtRecord.getPreference());
                }
                case Type.NSAP -> {
                    NSAPRecord nsapRecord = (NSAPRecord) dnsRecord;
                    itemBuilder.data(nsapRecord.getAddress());
                }
                case Type.NSAP_PTR -> {
                    NSAP_PTRRecord nsapPtrRecord = (NSAP_PTRRecord) dnsRecord;
                    itemBuilder.data(nsapPtrRecord.getTarget().toString());
                }
                case Type.SIG -> {
                    SIGRecord sigRecord = (SIGRecord) dnsRecord;
                    itemBuilder.data(sigRecord.rdataToString());
                }
                case Type.KEY -> {
                    KEYRecord keyRecord = (KEYRecord) dnsRecord;
                    itemBuilder.data(keyRecord.rdataToString());
                }
                case Type.PX -> {
                    PXRecord pxRecord = (PXRecord) dnsRecord;
                    itemBuilder.data(pxRecord.getMap822().toString() + " " + pxRecord.getMapX400().toString());
                }
                case Type.GPOS -> {
                    GPOSRecord gposRecord = (GPOSRecord) dnsRecord;
                    itemBuilder.data(gposRecord.getLongitude() + " " + gposRecord.getLatitude() + " " + gposRecord.getAltitude());
                }
                case Type.AAAA -> {
                    AAAARecord aaaaRecord = (AAAARecord) dnsRecord;
                    itemBuilder.data(aaaaRecord.getAddress().getHostAddress());
                }
                case Type.LOC -> {
                    LOCRecord locRecord = (LOCRecord) dnsRecord;
                    itemBuilder.data(locRecord.toString());
                }
                case Type.NXT -> {
                    NXTRecord nxtRecord = (NXTRecord) dnsRecord;
                    itemBuilder.data(nxtRecord.toString());
                }
                case Type.SRV -> {
                    SRVRecord srvRecord = (SRVRecord) dnsRecord;
                    itemBuilder.data(srvRecord.getTarget().toString() + " " + srvRecord.getPort() + " " + srvRecord.getPriority() + " "
                            + srvRecord.getWeight());
                }
                case Type.NAPTR -> {
                    NAPTRRecord naptrRecord = (NAPTRRecord) dnsRecord;
                    itemBuilder.data(naptrRecord.getOrder() + " " + naptrRecord.getPreference() + " " + naptrRecord.getFlags() + " "
                            + naptrRecord.getService() + " " + naptrRecord.getRegexp() + " " + naptrRecord.getReplacement());
                }
                case Type.KX -> {
                    KXRecord kxRecord = (KXRecord) dnsRecord;
                    itemBuilder.data(kxRecord.getTarget().toString() + " " + kxRecord.getPreference());
                }
                case Type.CERT -> {
                    CERTRecord certRecord = (CERTRecord) dnsRecord;
                    itemBuilder.data(certRecord.rdataToString());
                }
                case Type.A6 -> {
                    A6Record a6Record = (A6Record) dnsRecord;
                    itemBuilder.data(a6Record.rdataToString());
                }
                case Type.DNAME -> {
                    DNAMERecord dnameRecord = (DNAMERecord) dnsRecord;
                    itemBuilder.data(dnameRecord.getTarget().toString());
                }
                case Type.OPT -> {
                    OPTRecord optRecord = (OPTRecord) dnsRecord;
                    itemBuilder.data(optRecord.rdataToString());
                }
                case Type.APL -> {
                    APLRecord aplRecord = (APLRecord) dnsRecord;
                    itemBuilder.data(aplRecord.rdataToString());
                }
                case Type.DS -> {
                    DSRecord dsRecord = (DSRecord) dnsRecord;
                    itemBuilder.data(dsRecord.rdataToString());
                }
                case Type.SSHFP -> {
                    SSHFPRecord sshfpRecord = (SSHFPRecord) dnsRecord;
                    itemBuilder.data(sshfpRecord.rdataToString());
                }
                case Type.IPSECKEY -> {
                    IPSECKEYRecord ipseckeyRecord = (IPSECKEYRecord) dnsRecord;
                    itemBuilder.data(ipseckeyRecord.rdataToString());
                }
                case Type.RRSIG -> {
                    RRSIGRecord rrsigRecord = (RRSIGRecord) dnsRecord;
                    itemBuilder.data(rrsigRecord.rdataToString());
                }
                case Type.NSEC -> {
                    NSECRecord nsecRecord = (NSECRecord) dnsRecord;
                    itemBuilder.data(nsecRecord.rdataToString());
                }
                case Type.DNSKEY -> {
                    DNSKEYRecord dnskeyRecord = (DNSKEYRecord) dnsRecord;
                    itemBuilder.data(dnskeyRecord.rdataToString());
                }
                case Type.DHCID -> {
                    DHCIDRecord dhcidRecord = (DHCIDRecord) dnsRecord;
                    itemBuilder.data(dhcidRecord.rdataToString());
                }
                case Type.NSEC3 -> {
                    NSEC3Record nsec3Record = (NSEC3Record) dnsRecord;
                    itemBuilder.data(nsec3Record.rdataToString());
                }
                case Type.NSEC3PARAM -> {
                    NSEC3PARAMRecord nsec3paramRecord = (NSEC3PARAMRecord) dnsRecord;
                    itemBuilder.data(nsec3paramRecord.rdataToString());
                }
                case Type.TLSA -> {
                    TLSARecord tlsaRecord = (TLSARecord) dnsRecord;
                    itemBuilder.data(tlsaRecord.rdataToString());
                }
                case Type.SMIMEA -> {
                    SMIMEARecord smimeaRecord = (SMIMEARecord) dnsRecord;
                    itemBuilder.data(smimeaRecord.rdataToString());
                }
                case Type.HIP -> {
                    HIPRecord hipRecord = (HIPRecord) dnsRecord;
                    itemBuilder.data(hipRecord.rdataToString());
                }
                case Type.CDS -> {
                    CDSRecord cdsRecord = (CDSRecord) dnsRecord;
                    itemBuilder.data(cdsRecord.rdataToString());
                }
                case Type.CDNSKEY -> {
                    CDNSKEYRecord cdnskeyRecord = (CDNSKEYRecord) dnsRecord;
                    itemBuilder.data(cdnskeyRecord.rdataToString());
                }
                case Type.OPENPGPKEY -> {
                    OPENPGPKEYRecord openpgpkeyRecord = (OPENPGPKEYRecord) dnsRecord;
                    itemBuilder.data(openpgpkeyRecord.rdataToString());
                }
                case Type.SVCB -> {
                    SVCBRecord svcbRecord = (SVCBRecord) dnsRecord;
                    itemBuilder.data(svcbRecord.rdataToString());
                }
                case Type.HTTPS -> {
                    HTTPSRecord httpsRecord = (HTTPSRecord) dnsRecord;
                    itemBuilder.data(httpsRecord.rdataToString());
                }
                case Type.SPF -> {
                    SPFRecord spfRecord = (SPFRecord) dnsRecord;
                    itemBuilder.data(spfRecord.getStrings().toString());
                }
                case Type.URI -> {
                    URIRecord uriRecord = (URIRecord) dnsRecord;
                    itemBuilder.data(uriRecord.getTarget());
                }
                case Type.CAA -> {
                    CAARecord caaRecord = (CAARecord) dnsRecord;
                    itemBuilder.data(caaRecord.getValue());
                }
                case Type.DLV -> {
                    DLVRecord dlvRecord = (DLVRecord) dnsRecord;
                    itemBuilder.data(dlvRecord.rdataToString());
                }
                default -> itemBuilder.data(dnsRecord.rdataToString());
            }

            items.add(itemBuilder.build());
        }
        return items;
    }
}
