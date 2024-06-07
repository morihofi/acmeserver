# Changelog

## Changes to from 1.x to 2.0-rc0 include:

- new interactive WebUI (management UI is coming soon)
- better Database design
- certificates can now be issued in a separate thread
- IP support works now (IPv4 and IPv6)
- error messages of failed challenges are now more informative
- multiple Domains in one order works now
- fixed several security issues

## 2.0-rc1

- fixed update available badge always visible
- faster statistics loading incl. interactive charts
- more efficient CRL generation and certificate expiration/renew watcher
- Database connection pooling
- Database connection jdbc url is now in configuration file -> will be automatically migrated
- PostgreSQL database is now officially tested and supported (most recent version)

## 2.0-rc2

- removed org.json dependency
- Moved code from `Database` class into its identifiers for better code cleanness
- built-in support for Mozilla's SSL Config JSONs
- `*.cab` CA download support for legacy Windows Mobile 5+ devices
- Legacy CA download page for legacy user agents (IE 9 and older, incl. Pocket PC); Must be used with Mozilla's old ssl
  config and enabled `allowLegacyResumption` in configuration to access web UI using https
- Database connection pool is now only in debug mode disabled
- IP issuing support can now be manually enabled/disabled for a provisioner
- Better error message when a IP Address has been used in an `dns`-identifier instead of an `ip`-identifier
- Logger in classes is now private and final everywhere and has been renamed from `log` to `LOG`

## 2.0

- Database reconnect no longer fails
- Better documentation

## 2.0.1

- Dependency Updates
    - org.hibernate.orm:hibernate-core 6.4.4.Final to 6.4.5.Final
    - org.slf4j:jul-to-slf4j from 2.0.12 to 2.0.13
    - jakarta.interceptor:jakarta.interceptor-api from 2.1.0 to 2.2.0
    - com.github.spotbugs:spotbugs-annotations from 4.8.3 to 4.8.4

## 2.1

- Frontend Updates
    - Removed JTE (Server Side Rendering) for Web UI
    - Removed legacy CA download page --> Download CA using direct link
    - Use NuxtJS for WebUI
    - No CDN resources more needed for UI to display
    - Troubleshooting Options
        - DNS Query tester
- Backend Updates:
    - Configuration scheme for Proxy and Networking changed, see documentation
    - Custom DNS Server support and DNS over HTTPS support added
    - Usage of proxy is now fully used by ACME Server for all web requests (HTTP-Challenge or Version Check, ...)
    - Dedicated HTTP Access Log configurable with nginx like syntax. See documentation for more details
- API
    - Swagger and OpenAPI support (beta), on `/swagger` URL
    - Reimplemented API
- ACME Protocol
    - Support for `notAfter` property in `new-order` Endpoint, thanks to @Gabanna
- Dependency Updates
    - org.hibernate.orm:hibernate-core 6.4.5.Final to 6.5.1.Final
    - io.javalin:javalin from 6.1.3 to 6.1.6
    - com.github.spotbugs:spotbugs-annotations from 4.8.4 to 4.8.5.