# Changelog

## Changes to from 1.x to 2.0-rc0 include:

- new interactive WebUI (management UI is coming soon)
- better Database design
- certificates can now be issued in a separate thread
- IP support works now (IPv4 and IPv6)
- error messages of failed challenges are now more informative
- multiple Domains in one order works now
- fixed several security issues

## Changes to from 2.0-rc0 to 2.0-rc1 include:

- fixed update available badge always visible
- faster statistics loading incl. interactive charts
- more efficient CRL generation and certificate expiration/renew watcher
- Database connection pooling
- Database connection jdbc url is now in configuration file -> will be automatically migrated
- PostgreSQL database is now officially tested and supported (most recent version)

## Changes to from 2.0-rc1 to 2.0-rc2 include:
- removed org.json dependency
- Moved code from `Database` class into its identifiers for better code cleanness
- built-in support for Mozilla's SSL Config JSONs
- `*.cab` CA download support for legacy Windows Mobile 5+ devices
- Legacy CA download page for legacy user agents (IE 9 and older, incl. Pocket PC); Must be used with Mozilla's old ssl config and enabled `allowLegacyResumption` in configuration to access web UI using https
- Database connection pool is now only in debug mode disabled
- IP issuing support can now be manually enabled/disabled for a provisioner
- Better error message when a IP Address has been used in an `dns`-identifier instead of an `ip`-identifier
- Logger in classes is now private and final everywhere and has been renamed from `log` to `LOG`

## Changes to from 2.0-rc2 to 2.0 release include:
- Database reconnect no longer fails
- Better documentation