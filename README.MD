# ACME Server

[![Get latest Release here](https://img.shields.io/badge/Get_lastest_Release-here-blue?style=flat)
](https://github.com/morihofi/acmeserver/releases/latest)
[![Report a bug](https://img.shields.io/badge/Report_a-bug-red?style=flat)
](https://github.com/morihofi/acmeserver/issues)

## 📃 About

This is an implementation of an Automatic Certificate Management Environment (ACME) Server, fully written in Java

- Java 17 and up
- Any JDBC (and Hibernate compatible) database. Currently, H2, MariaDB and PostgreSQL drivers are in classpath
- Proxy support for HTTP Challenge

## 🎉 ACME Server is now one year old!

One year ago, I started this project. I'm happy to announce that this project is now one year old!
The first release candidate of version 2.0 is now out!

## ⚠ Breaking Changes - Version 2.x

- Provisioner directory is now in /acme/provisionername/directory
- Database scheme has been complete rebuilt, please clean database before use
- Due to move of provisioner directory, the URLs to OCSP and CRL have also been moved, please regenerate your
  intermediate certificates by delete the corresponding entries in the keystore

### 📰 Changelog

Changelog has been moved [here](CHANGELOG.MD)

## 🐳 Getting started with Docker (recommended)

1. Install [Docker Engine](https://docs.docker.com/engine/install/) with `docker compose` plugin, if you haven't already
2. Create two directories called `serverdata` and `logs` in this directory
3. Copy the `settings.sample.json` into the new `serverdata` directory and rename it to `settings.json`
4. Adjust the settings, especially the `dnsName` (of your host), and the `http`/`https` ports. (Don't forget to change
   these also in the `docker-compose.yml`-file) You can't change these later, because they will be written into the
   intermediate/client certificates for finding revokation list etc. and ACME client configuration will be get broken.
5. Run `docker compose up -d` in this directory. That's all!

> You can run `docker compose logs -f` if you want to see the logs of the server. Press `Crtl`+`C` to quit

## 🏗 Building

### Prerequisites
You'll need the following prerequisites to be able to build ACME Server

- Java 17
- Maven

### Initiating the build
Run the following command to build this application

```bash
mvn clean package
```

### Running on bare metal

You'll find then the jar file, you've built in the previous step, 
in the `target/` folder. 

Then you can execute it with the following command to run it:

```bash
java -jar acmeserver-VERSION.jar
```

## ✨ Features

- Self-hosted ACME Server for use with your own CA
- Download CA support 
  - Download in standard formats like CRT, PEM, DER
  - [CAB file CA export](http://web.archive.org/web/20060427213220/http://blogs.msdn.com/windowsmobile/archive/2006/01/28/making_a_root_cert_cab_file.aspx) for install on legacy Windows Mobile based devices (e.g. older embedded devices, old PDAs, ...)
- Support for PKCS#12 (.p12) KeyStore and PKCS#11 Hardware Security Modules (HSM)
- Revokation support for certificates
- ACME Account deactivation
- Mostly ACME RFC compliant in its behaviour
- Multiple provisioners support with own intermediate CA
    - Set expiration and key algorithm for each intermediate
    - Set expiration for each issued certificate in provisioner
    - Each provisioner has its own CRL (Certificate Revokation List)
    - Set a domain naming restriction for each provisioner (e.g. domain must end with ".example.com")
    - Enable wildcard support individually for each provisioner (e.g. *.example.com)
- Built-in OCSP (Online Certificate Status Protocol) server
- Signature check of client's requests
- Support for multiple identifiers in one order (multiple domains)
- Nonce validation (Replay protection)
- Supported challenges:
    - HTTP-01
    - DNS-01
- Statistics
- API and WebUI TLS Security can be automatically configured
  using [Mozilla's SSL Config Guidelines](https://ssl-config.mozilla.org/),
  see [Wiki](https://wiki.mozilla.org/Security/Server_Side_TLS) here (JSON configuration from 4.0 until 5.7 (latest at
  writing this) are included, if specified version not available it will try to fetch these from the Mozilla server.
  Note: Version before 5.1 might be a bit buggy)

## 📒 Disclaimer

This version is not fully RFC 8555 compliant, but should work with most clients.

## ℹ Command line switches

| Command line option                      | Description                                                                                                        |
|------------------------------------------|--------------------------------------------------------------------------------------------------------------------|
| `--option-use-async-certificate-issuing` | Issues certificates in a separate thread (⚠ do not use with certbot at the moment)                                 |
| `--debug`                                | Debug mode (see above)                                                                                             |
| `--migrate-pem-to-keystore`              | If you use an old Version 1.x, that uses the PEM files in filesystem, you can use this to mirgrate into a Keystore |

## ✅ Testing status

### 🤖 Tested ACME Clients

|                                                               | HTTP-01 | DNS-01 |                                                                                                                         |
|---------------------------------------------------------------|---------|--------|-------------------------------------------------------------------------------------------------------------------------|
| [GetHttpsForFree](https://github.com/diafygi/gethttpsforfree) | ✅       | ✅      | -> modified version is included                                                                                         |
| [Certbot](https://certbot.eff.org)                            | ✅       | ✅      | ℹ Note, works only correctly, if certificate issuing is not async in the server (default)                               |
| [acme.sh](https://github.com/acmesh-official/acme.sh)         | 🐞      | ✅      | For HTTP-01 use Standalone mode, nginx mode won't work for no reason                                                    |
| [Traefik](https://github.com/traefik/traefik)                 | ✅       | ❓      |                                                                                                                         |
| [win-acme](https://www.win-acme.com/)                         | ✅       | ❓      | Tested with IIS 8.5 on Win Server 2012 r2. Only use Provisioner with RSA, because IIS doesn't support Elliptical Curves |
| [acme4j](https://acme4j.shredzone.org)                        | ✅       | ✅      |                                                                                                                         |

### 💻 Tested Operating Systems

|                                                       | Test status | Notes                               |
|-------------------------------------------------------|-------------|-------------------------------------|
| [Debian](https://debian.org/) 11, 12                  | ✅           |                                     |
| [FreeBSD](https://freebsd.org) 13.2, 13.3, 14         | ✅           | Only the RELEASE Builds were tested |
| [Windows](https://www.microsoft.com/de-de/windows) 10 | ✅           |                                     |

### 🗃️ Tested Databases

|                                           | JDBC Driver Built-in | Test status | Notes                                                                                                 |
|-------------------------------------------|----------------------|-------------|-------------------------------------------------------------------------------------------------------|
| [MariaDB](https://mariadb.org)            | Yes                  | ✅           |                                                                                                       |
| [H2](https://h2database.com)              | Yes                  | ✅           | Only embedded mode has been tested, cause this is the only supported at the moment                    |
| [PostgreSQL](https://www.postgresql.org/) | Yes                  | ✅           | Use a up-to-date database version. Tested with PostgreSQL 16.2, older version may won't work properly |
| [MySQL](https://mysql.com/)               | No                   | ❓           | Hibernate configuration has been prepared, but not tested yet                                         |

If you want to use a database engine, where the JDBC driver isn't built in,
you have to add the driver manually to the classpath and specify the Main-Class manually.

### 🔎 Meanings of the Emojis

| ✅                    | ❌                         | 🐞    | ❓          |
|----------------------|---------------------------|-------|------------|
| works without issues | not working at the moment | buggy | not tested |

## 🐛 Debug Mode

Enable debug mode by set environment variable `DEBUG` to `TRUE` or start ACME Server with `--debug`.
Do not use this in production as it disables some security mechanisms (e.g. nonce check) and can lead to misuse of your
CA.

## 💻 Development Contributions

We welcome every contribution to the ACME Server project. Please refer to our style guide located in the `styleguide` directory for more information 
on coding standards and practices. This ensures that the code keeps readable and easier to test.

---

## ❤ Thanks to

- [OpenAI's ChatGPT](https://chat.openai.com) (Helped me, when I was struggling)
- [BouncyCastle](https://bouncycastle.org) (For a great cryptographic library in Java)
- [KeyStore Explorer](https://keystore-explorer.org/) (Helped me debugging my generated certificates)
- [GetHttpsForFree](https://github.com/diafygi/gethttpsforfree) (For debugging my ACME Server and understanding the ACME
  protocol, a modified version is built-in the server)
- [Acme4j](https://github.com/shred/acme4j) (It's client implementation helped me to generate the expected DNS Challenge
  value on the server side)
- [CabinetMaker](https://github.com/grahamrb/CabinetMaker) for generating CAB file using pure Java, it has been
  refactored for Java 17+
