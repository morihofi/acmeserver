# 3. CAA Records

Certificate Authority Authorization (CAA) records allow a domain owner to specify which Certificate Authorities (CAs)
are permitted to issue certificates for their domain.
ACME Server will query these records according to [RFC&nbsp;6844](https://datatracker.ietf.org/doc/html/rfc6844) before
issuing a certificate.

## How it works

1. When a certificate request is received, ACME Server performs DNS lookups for CAA records of the requested domain.
2. If no CAA records are found, issuance is allowed.
3. If one or more `issue` or `issuewild` records are present, at least one entry must match the CA domain of this
   server. Other CAs listed will cause issuance to be denied.
4. `iodef` records are ignored by the server but may be used by domain owners to receive violation reports.

## Configuring CAA records

Create `CAA` records in your authoritative DNS zone. A basic example using BIND style notation looks like:

```
example.com.  0  CAA  0 issue "acme.example.com"
example.com.  0  CAA  0 iodef "mailto:caa-reports@example.com"
```

* `issue` &ndash; Authorizes the CA with the specified domain (`acme.example.com`) to issue certificates.
* `issuewild` &ndash; Same as `issue` but specifically for wildcard certificates (`*.example.com`). Omit or set to an
  empty string to forbid wildcard issuance.
* `iodef` &ndash; Optional address (mailto or https) where policy violations are reported.

The CA domain must match the `server.dnsName` configured in `settings.json` so that ACME Server recognizes itself in the
CAA record.

### Wildcard certificates

If you plan to issue wildcard certificates, add an `issuewild` tag pointing to the server domain:

```
example.com. 0 CAA 0 issuewild "acme.example.com"
```

Domains without matching `issue`/`issuewild` records will cause requests to fail with a `caa` ACME error.

### Testing your configuration

After publishing the records, use tools such as `dig` or [DNS lookup websites](https://dns.google/) to verify that the
CAA records resolve correctly. Remember that DNS changes may take time to propagate.
