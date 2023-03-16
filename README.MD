# ACME Server
This is a proof-of-concept Automatic Certificate Management Environment Server (ACME), fully written in Java

- Java 8 and up
- MariaDB as Database
- Proxy support for HTTP Challenge

## Note
This version is not fully RFC compliant, but should work with most clients. Currently, there is no signature check of the client. Please notice that.

## Current limitations
- Only one domain per order is currently allowed
- Only HTTP Challenge

## Tested ACME Clients
- acme.sh (works)
- Certbot (works, but not in standalone mode)

## Tested Operating Systems
- Linux Mint 21
- Debian 11
- Windows 10
- FreeBSD