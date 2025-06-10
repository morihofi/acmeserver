package de.morihofi.acmeserver.core.tools.crypto;

/*
 * acme4j - Java ACME client
 *
 * Copyright (C) 2016 Richard "Shred" Körber
 *   http://acme4j.shredzone.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

import de.morihofi.acmeserver.utils.base64.Base64Tools;
import edu.umd.cs.findbugs.annotations.Nullable;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.x509.AuthorityKeyIdentifier;
import org.bouncycastle.asn1.x509.Certificate;
import org.bouncycastle.cert.X509CertificateHolder;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.IDN;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Base64;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Contains utility methods that are frequently used for the ACME protocol.
 * <p>
 * This class is internal. You may use it in your own code, but be warned that methods may change their signature or disappear without prior
 * announcement.
 */

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AcmeUtils {


    /**
     * Hexadecimal characters for encoding.
     */
    private static final char[] HEX = "0123456789abcdef".toCharArray();

    /**
     * Prefix for ACME error URNs.
     */
    private static final String ACME_ERROR_PREFIX = "urn:ietf:params:acme:error:";

    /**
     * Pattern for matching RFC 3339 date strings.
     */
    private static final Pattern DATE_PATTERN = Pattern.compile(
            "^(\\d{4})-(\\d{2})-(\\d{2})T"
                    + "(\\d{2}):(\\d{2}):(\\d{2})"
                    + "(?:\\.(\\d{1,3})\\d*)?"
                    + "(Z|[+-]\\d{2}:?\\d{2})$", Pattern.CASE_INSENSITIVE);

    /**
     * Pattern for matching timezone offsets in RFC 3339 date strings.
     */
    private static final Pattern TZ_PATTERN = Pattern.compile(
            "([+-])(\\d{2}):?(\\d{2})$");

    /**
     * Pattern for matching Content-Type headers.
     */
    private static final Pattern CONTENT_TYPE_PATTERN = Pattern.compile(
            "([^;]+)(?:;.*?charset=(\"?)([a-z0-9_-]+)(\\2))?.*", Pattern.CASE_INSENSITIVE);

    /**
     * Pattern for matching email addresses in contact URIs.
     */
    private static final Pattern MAIL_PATTERN = Pattern.compile("\\?|@.*,");

    /**
     * Pattern for matching Base64 URL encoded strings.
     */
    private static final Pattern BASE64URL_PATTERN = Pattern.compile("[0-9A-Za-z_-]*");

    /**
     * Base64 encoder for PEM format.
     */
    private static final Base64.Encoder PEM_ENCODER = Base64.getMimeEncoder(64,
            "\n".getBytes(StandardCharsets.US_ASCII));


    /**
     * Computes a SHA-256 hash of the given string.
     *
     * @param z String to hash
     * @return Hash
     */
    public static byte[] sha256hash(String z) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(z.getBytes(UTF_8));
            return md.digest();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalArgumentException("Could not compute hash", ex);
        }
    }

    /**
     * Validates that the given {@link String} is a valid base64url encoded value.
     *
     * @param base64 {@link String} to validate
     * @return {@code true}: String contains a valid base64url encoded value. {@code false} if the {@link String} was {@code null} or
     * contained illegal characters.
     * @since 2.6
     */
    public static boolean isValidBase64Url(@Nullable String base64) {
        return base64 != null && BASE64URL_PATTERN.matcher(base64).matches();
    }

    /**
     * ASCII encodes a domain name.
     * <p>
     * The conversion is done as described in
     * <a href="http://www.ietf.org/rfc/rfc3490.txt">RFC 3490</a>. Additionally, all
     * leading and trailing white spaces are trimmed, and the result is lowercased.
     * <p>
     * It is safe to pass in ACE encoded domains, they will be returned unchanged.
     *
     * @param domain Domain name to encode
     * @return Encoded domain name, white space trimmed and lower cased.
     */
    public static String toAce(String domain) {
        Objects.requireNonNull(domain, "domain");
        return IDN.toASCII(domain.trim()).toLowerCase(Locale.ENGLISH);
    }

    /**
     * Parses a RFC 3339 formatted date.
     *
     * @param str Date string
     * @return {@link Instant} that was parsed
     * @throws IllegalArgumentException if the date string was not RFC 3339 formatted
     * @see <a href="https://www.ietf.org/rfc/rfc3339.txt">RFC 3339</a>
     */
    public static Instant parseTimestamp(String str) {
        Matcher m = DATE_PATTERN.matcher(str);
        if (!m.matches()) {
            throw new IllegalArgumentException("Illegal date: " + str);
        }

        var year = Integer.parseInt(m.group(1));
        var month = Integer.parseInt(m.group(2));
        var dom = Integer.parseInt(m.group(3));
        var hour = Integer.parseInt(m.group(4));
        var minute = Integer.parseInt(m.group(5));
        var second = Integer.parseInt(m.group(6));

        var msStr = new StringBuilder();
        if (m.group(7) != null) {
            msStr.append(m.group(7));
        }
        while (msStr.length() < 3) {
            msStr.append('0');
        }
        var ms = Integer.parseInt(msStr.toString());

        var tz = m.group(8);
        if ("Z".equalsIgnoreCase(tz)) {
            tz = "GMT";
        } else {
            tz = TZ_PATTERN.matcher(tz).replaceAll("GMT$1$2:$3");
        }

        return ZonedDateTime.of(
                year, month, dom, hour, minute, second, ms * 1_000_000,
                ZoneId.of(tz)).toInstant();
    }

    /**
     * Converts the given locale to an Accept-Language header value.
     *
     * @param locale {@link Locale} to be used in the header
     * @return Value that can be used in an Accept-Language header
     */
    public static String localeToLanguageHeader(@Nullable Locale locale) {
        if (locale == null || "und".equals(locale.toLanguageTag())) {
            return "*";
        }

        var langTag = locale.toLanguageTag();

        var header = new StringBuilder(langTag);
        if (langTag.indexOf('-') >= 0) {
            header.append(',').append(locale.getLanguage()).append(";q=0.8");
        }
        header.append(",*;q=0.1");

        return header.toString();
    }

    /**
     * Strips the acme error prefix from the error string.
     * <p>
     * For example, for "urn:ietf:params:acme:error:unauthorized", "unauthorized" is returned.
     *
     * @param type Error type to strip the prefix from. {@code null} is safe.
     * @return Stripped error type, or {@code null} if the prefix was not found.
     */
    @Nullable
    public static String stripErrorPrefix(@Nullable String type) {
        if (type != null && type.startsWith(ACME_ERROR_PREFIX)) {
            return type.substring(ACME_ERROR_PREFIX.length());
        } else {
            return null;
        }
    }

    /**
     * Extracts the content type of a Content-Type header.
     *
     * @param header Content-Type header
     * @return Content-Type, or {@code null} if the header was invalid or empty
     * @throws IllegalArgumentException if the Content-Type header contains a different charset than "utf-8".
     */
    @Nullable
    public static String getContentType(@Nullable String header) {
        if (header != null) {
            var m = CONTENT_TYPE_PATTERN.matcher(header);
            if (m.matches()) {
                var charset = m.group(3);
                if (charset != null && !"utf-8".equalsIgnoreCase(charset)) {
                    throw new IllegalArgumentException("Unsupported charset " + charset);
                }
                return m.group(1).trim().toLowerCase(Locale.ENGLISH);
            }
        }
        return null;
    }

    /**
     * Validates a contact {@link URI}.
     *
     * @param contact Contact {@link URI} to validate
     * @throws IllegalArgumentException if the contact {@link URI} is not suitable for account contacts.
     */
    public static void validateContact(URI contact) {
        if ("mailto".equalsIgnoreCase(contact.getScheme())) {
            var address = contact.toString().substring(7);
            if (MAIL_PATTERN.matcher(address).find()) {
                throw new IllegalArgumentException(
                        "multiple recipients or hfields are not allowed: " + contact);
            }
        }
    }

    /**
     * Returns the certificate's unique identifier for renewal according to draft-ietf-acme-ari-03.
     *
     * @param certificate Certificate to get the unique identifier for.
     * @return Unique identifier
     * @throws IllegalArgumentException if the certificate is invalid or does not provide the necessary information.
     */
    public static String getRenewalUniqueIdentifier(X509Certificate certificate) {
        try {
            var cert = new X509CertificateHolder(certificate.getEncoded());

            var aki = Optional.of(cert)
                    .map(X509CertificateHolder::getExtensions)
                    .map(AuthorityKeyIdentifier::fromExtensions)
                    .map(AuthorityKeyIdentifier::getKeyIdentifier)
                    .map(Base64Tools::base64UrlEncode)
                    .orElseThrow(() -> new IllegalArgumentException("Missing or invalid Authority Key Identifier"));

            var sn = Optional.of(cert)
                    .map(X509CertificateHolder::toASN1Structure)
                    .map(Certificate::getSerialNumber)
                    .map(AcmeUtils::getRawInteger)
                    .map(Base64Tools::base64UrlEncode)
                    .orElseThrow(() -> new IllegalArgumentException("Missing or invalid serial number"));

            return aki + '.' + sn;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid certificate", ex);
        }
    }

    /**
     * Gets the raw integer array from ASN1Integer. This is done by encoding it to a byte array, and then skipping the INTEGER identifier.
     * Other methods of ASN1Integer only deliver a parsed integer value that might have been mangled.
     *
     * @param integer ASN1Integer to convert to raw
     * @return Byte array of the raw integer
     */
    private static byte[] getRawInteger(ASN1Integer integer) {
        try {
            var encoded = integer.getEncoded();
            return Arrays.copyOfRange(encoded, 2, encoded.length);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}
