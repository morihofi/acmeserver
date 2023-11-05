# ACME Server
This is a proof-of-concept Automatic Certificate Management Environment Server (ACME), fully written in Java

- Java 8 and up
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
- FreeBSD 13 (openjdk8)