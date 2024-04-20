# 📖 ACME Server Docs

Welcome to the official ACME Server documentation. This is a work in progress documentation

TOC HERE

## What is an ACME Server?

ACME Server is a specialized software designed to automate the process of acquiring, renewing,
and deploying SSL/TLS certificates for web servers and other online services. Standing for
Automated Certificate Management Environment, ACME simplifies the once complex and time-consuming
task of managing certificates, ensuring secure and encrypted communication channels on the internet.
It is basically a fully automated, self-hosted certificate authority.

## Terminology

The keywords "MUST", "MUST NOT", "REQUIRED", "SHALL", "SHALL NOT",
"SHOULD", "SHOULD NOT", "RECOMMENDED", "NOT RECOMMENDED", "MAY", and
"OPTIONAL" in this document are to be interpreted as described in
[BCP 14](https://www.rfc-editor.org/info/bcp14) [RFC2119](https://datatracker.ietf.org/doc/html/rfc2119) [RFC8174](https://datatracker.ietf.org/doc/html/rfc8174)
when, and only when, they appear in all
capitals, as shown here.

## Getting started

### ... using Docker (recommended)

1. Install [Docker Engine](https://docs.docker.com/engine/install/) with `docker compose` plugin, if you haven't already
2. Create two directories called `serverdata` and `logs` in this directory
3. Copy the `settings.sample.json` into the new `serverdata` directory and rename it to `settings.json`
4. Adjust the settings, especially the `dnsName` (of your host), and the `http`/`https` ports. (Don't forget to change
   these also in the `docker-compose.yml`-file) You can't change these later, because they will be written into the
   intermediate/client certificates for finding revokation list etc. and ACME client configuration will be get broken.
5. Run `docker compose up -d` in this directory. That's all!

> You can run `docker compose logs -f` if you want to see the logs of the server. Press `Crtl`+`C` to quit

### ℹ Command line switches

| Command line option                      | Description                                                                                                       |
|------------------------------------------|-------------------------------------------------------------------------------------------------------------------|
| `--option-use-async-certificate-issuing` | Issues certificates in a separate thread (⚠ do not use with certbot at the moment)                                |
| `--debug`                                | Debug mode (see above)                                                                                            |
| `--migrate-pem-to-keystore`              | If you use an old Version 1.x, that uses the PEM files in filesystem, you can use this to migrate into a Keystore |

## Configuration

You'll find a sample configuration in the root of this repository with the name `settings.sample.json`.
It provides a sample configuration you should customize it be able to work with this instance.
Copy the sample file into the `serverdata` directory and call it `settings.json`.

### Basic configuration

You MUST change the `dnsName` value to a DNS name (Not an IP-Address -> won't work) that is resolvable to your ACME
server. It will be written into certificates, and also served in the ACME API.
You MAY change the ports serving the API and Website. If you set the `http`-Port to `0`, HTTP will be disabled.

It is recommended to **not run** ACME Server behind a reverse proxy.

```json
{
  /* ... */

  "server": {
    "dnsName": "acme.example.com",
    "ports": {
      "https": 443,
      "http": 80
    },
    "enableSniCheck": true,
    "mozillaSslConfig": {
      "enabled": false,
      "version": "5.7",
      "configuration": "intermediate"
    },
    "sslServerConfig": {
      "allowLegacyResumption": false
    }
  }

  /* ... */
}
```

#### Information for standalone

ACME Server has the ability to configure TLS (Protocol that HTTPS uses) with the recommended configurations from the
[Mozilla SSL Config](https://ssl-config.mozilla.org/) project. Values for `configuration` can
be `modern`, `intermediate` and `old`.

##### Use of very old clients

**Note: You SHOULD only enable very old client support as a last resort!**

If you want to be able to connect to the Website of this ACME Server instance with a very old clients (e.g. IE8) you
SHOULD enable `allowLegacyResumption`,
otherwise handshakes can be fail. For this you also SHOULD set the Mozilla SSL config to `old` to be able to support
those old clients

#### Running behind a reverse proxy

If your instance running is behind a reverse proxy (for example nginx, traefik and so on),
you SHOULD set the `enableSniCheck` to `false` to avoid problems with non-matching SNIs.

ACME Server is able to operate behind a reverse proxy, but you MUST NOT rewrite paths, because
it can cause problems resolving API Endpoints. As said earlier, it is recommended to
not run ACME Server behind a reverse proxy.

### KeyStore configuration

ACME Server uses a keystore to save its Root CA, Intermediate CAs, and it's matching private keys which is protected
with a password.
It supports PKCS#12 (`.p12`-file based keystore files) and PKCS#11 (Hardware security modules, for short called HSM)
keystores.

You SHOULD use PKCS#11 keystores, if you want to use ACME Server in a production environment. This is because
you can't extract private keys out of HSMs.

#### PKCS#12 (File based)

A keystore of the type PKCS#12 is a file based keystore. Just give it a secure password and a location, like the example
below:

```json
{
  /* ... */

  "keyStore": {
    "type": "pkcs12",
    "password": "test",
    "location": "./serverdata/acme-keystore.p12"
  }

  /* ... */
}

```

#### PKCS#11 (HSM based)

A keystore of the type PKCS#11 is a hardware keystore. You MUST provide a path to the driver library and set a password,
like in the example below.
The PKCS#11 library for you hardware security module MUST match the same architecture as the Java Runtime this ACME
Server uses.
Otherwise, it won't be able to load the library.

ACME Server was developed with [SoftHSM v2](https://github.com/opendnssec/SoftHSMv2) and should work with other real
HSMs too

**Note for Windows:** If the architecture matches, but an error occurs loading the lib, try installing
the [Microsoft Visual C++ Redistributable](https://learn.microsoft.com/en-US/cpp/windows/latest-supported-vc-redist) for
x86 and x64.

```json
{
  /* ... */

  "keyStore": {
    "type": "pkcs11",
    "password": "test",
    "libraryLocation": "/usr/local/lib/softhsm/libsofthsm2.so"
  }

  /* ... */
}
```

### Database

ACME Server supports popular relational databases, such as MariaDB, PostgreSQL, H2 and so on. You have to set a user for
the database and a matching password, and of course the database location itself.
The database location is specified in the `jdbcUrl` field and MUST start with `jdbc:`.

Following a table with a few JDBC configuration strings for the configuration. You have to edit it to make it work.

|                                           | JDBC Driver Built-in | Test status | JDBC URL                                                           | Notes                                                                                                 |
|-------------------------------------------|----------------------|-------------|--------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------|
| [MariaDB](https://mariadb.org)            | Yes                  | ✅           | `jdbc:mariadb://localhost:3306/database_name`                      |                                                                                                       |
| [H2](https://h2database.com)              | Yes                  | ✅           | `jdbc:h2:./serverdata/acme;DB_CLOSE_DELAY=-1` (database in a file) |                                                                                                       |
| [PostgreSQL](https://www.postgresql.org/) | Yes                  | ✅           | `jdbc:postgresql://localhost:5740/database_name`                   | Use a up-to-date database version. Tested with PostgreSQL 16.2, older version may won't work properly |
| [MySQL](https://mysql.com/)               | No                   | ❓           | `jdbc:mysql://localhost:3306/database_name`                        | Hibernate configuration has been prepared, but not tested yet                                         |

If you want to use a database engine, where the JDBC driver isn't built in,
you have to add the driver manually to the classpath and specify the Main-Class manually.

If you're just searching the JDBC URL you have to use, try googling `jdbc dbms_name_here connection string`
(replace `dbms_name_here` with your database management system name)

```json
{
  /* ... */
  "database": {
    "jdbcUrl": "jdbc:...",
    "user": "root",
    "password": "123456"
  }
  /* ... */
}
```

### Root CA

ACME Server requires a Root certificate authority to be able to operate and generate certificates.

```json
{
   /* ... */
   "rootCA": {
      "metadata": {
         "commonName": "ACMEServer Root CA",
         "organisation": null,
         "organisationalUnit": null,
         "countryCode": null
      },
      "expiration": {
         "days": 3,
         "months": 6,
         "years": 10
      },
      "algorithm": {
         /* see below */
      }
   }
   /* .. */
}
```

#### Metadata
The `commonName` MUST be set, but SHOULD set `organisation`, `organisationalUnit` and `countryCode`.

If you set a `countryCode`, it must be in a two letter format following the [ISO 3166-1 alpha-2](https://en.wikipedia.org/wiki/ISO_3166-1_alpha-2) standard.
For example e.g. `DE` for Germany, `US` for the United States of America and so on ...

#### Expiration

The `days`, `months`, `years` values are the specify the validity period for generate certificate after generation. 

#### Algorithm
Algorithms can be `rsa` and `ecdsa`, both are supported and take different parameters. 
For backwards compatibility it is recommended to use an RSA based certificate.

##### RSA
You MUST specify a key size. It SHOULD higher or equal than 2048 bit. It is highly RECOMMENDED to have 4096 bit.
```json
{
   /* ... */
   "algorithm": {
      "type": "rsa",
      "keySize": 4096
   }
   /* ... */
}
```

##### ECDSA
You MUST specify a curve name. You can find a list [here](https://github.com/bcgit/bc-java/wiki/Support-for-ECDSA,-ECGOST-Curves). 
Please note that the selected curve must be compatible with BouncyCastle. NIST curves are RECOMMENDED.
```json
{
   /* ... */
   "algorithm": {
      "type": "ecdsa",
      "curveName": "P-256"
   }
   /* ... */
}
```

### Provisioners

With provisioners, you can split this CA into multiple certificate generation policies.
Each provisioner operates under its own intermediate certificate, 
forming a 1:1 relationship with it, like in the following diagram:

```
Root CA (installed on client devices)
|
|--+ Intermediate for Provisioner 1 
|  |  
|  |-- ACME Generated Cert 1  
|  |-- ACME Generated Cert 2  
|  |-- ACME Generated Cert ...
|
|--+ Intermediate for Provisioner 2
|  |  
|  |-- ACME Generated Cert 1  
|  |-- ACME Generated Cert 2  
|  |-- ACME Generated Cert ...
|
...
```

You can configure the intermediate certificate the provisioner uses in the `intermediate`-subobject. 
For configuration see how to Root CA configuration above works, it uses the same syntax with the same fields.

```json
   /* ... */
  "provisioner": [
    {
      "name": "my_provisioner",
      "meta": {
        "website": "https://example.com/testing-ca",
        "tos": " https://example.com/terms-of-service-testing.html"
      },
      "intermediate": {
        /* see Root CA section above -> uses same config style */
      },
      "issuedCertificateExpiration": {
        "days": 3,
        "months": 0,
        "years": 0
      },
      "wildcardAllowed": false,
      "ipAllowed": true,
      "domainNameRestriction": {
        "enabled": false,
        "mustEndWith": [
          ".test.example.com",
          ".test.example.org"
        ]
      }
    },
    ...
  ]
  /* ... */
```
- `name`: Name of the provisioner. Provisioner name can only contain lowercase letters a-z, numbers 0-9, "-" and "_"
- `meta`: Metadata shown to the ACME client when creating account and ordering
  - `website`: Website of the CA responsible for this provisioner, probably your website
  - `tos`: Terms of Service for this provisioner, probably the CA terms of server subpage of your website
- `intermediate`: See Root CA above
- `issuedCertificateExpiration`: How long should a certificate, issued by the ACME Protocol, live after its after creation.
- `wildcardAllowed`: Should be issuing wildcards for DNS Domains allowed, e.g. `*.example.com`?
- `ipAllowed`: Should be issuing for IP Addresses enabled for both IPv4 and IPv6?
- `domainNameRestriction`: Restrict domain names allowed for issuing. This does not apply for the reverse DNS of an IP address, because reverse DNS isn't supported at the moment.
  - `enabled`: Enable this policy ìf set to `true`, otherwise it is disabled
  - `mustEndWith`: These are the names the domains must end with. Add as many as you wish.
    - For example all domains must end with `.test.example.com`:
     
      | Domain                       | Would accept |
      |------------------------------|--------------|
      | hello.world.test.example.com | Yes          |
      | hello.test.example.com       | Yes          |
      | hellotest.example.com        | No           |
      | hello.test.example.org       | No           |

    - For example all domains must end with `test.example.com` (without the dot at the beginning):
     
      | Domain                       | Would accept |
      |------------------------------|--------------|
      | hello.world.test.example.com | Yes          |
      | hello.test.example.com       | Yes          |
      | hellotest.example.com        | Yes          |
      | hello.test.example.org       | No           |

#### E-Mail sending (alpha state)
ACME Server supports sending E-Mails when an certificate has been ordered. This feature is currently in alpha state.

```json
  /* ... */
  "emailSmtp": {
    "enabled": false,
    "port": 587,
    "encryption": "starttls",
    "host": "mail.example.com",
    "username": "acme-noreply@example.com",
    "password": "insecurepassword"
  }
  /* ... */
```
- `enabled`: Enable this feature ìf set to `true`, otherwise it is disabled
- `host`: Host of the SMTP Service
- `port`: Port of the SMTP Service
- `encryption`: Encryption to use to connect to the SMTP server. Values can be one of the following:
  - `starttls`: StartTLS Encryption
  - `ssl` or `tls`: TLS Encryption
  - `none`: No encryption. This is NOT RECOMMENDED.
- `username`: Username (mostly the E-Mail) to authenticate
- `password`: Password to authenticate