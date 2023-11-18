# ACME Server
This is a proof-of-concept Automatic Certificate Management Environment Server (ACME), fully written in Java

- Java 17 and up
- Any JDBC (and Hibernate compatible) database
- Proxy support for HTTP Challenge

## Building
Run the following command to build this Application
```bash
mvn clean assembly:assembly
```
then start it with
```bash
java -jar acmeserver-VERSION.jar
```

## Features
- Self-hosted ACME Server for use with your own CA
- CRL support
- Revokation support
- Mostly ACME RFC compliant
- Multiple provisioners support with own intermediate CA
  - Set expiration and key algorithm for each intermediate 
- Included OCSP server
- Signature check

## Note
This version is not fully RFC compliant, but should work with most clients. 

## Current limitations
- Only one domain per order is currently allowed
- Only HTTP Challenge

## Tested ACME Clients
- acme.sh (works)
- Certbot (works)

## Tested Operating Systems
- Linux Mint 21
- Debian 11
- Windows 10
- ~~FreeBSD 13 (openjdk8)~~ has to be retested