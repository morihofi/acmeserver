/*
 * SPDX-FileCopyrightText: 2004-2013 Wayne Grant
 * SPDX-FileCopyrightText: 2013-2023 Kai Kramer
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * This file originates from Keystore Explorer and has been modified for use in Certgine.
 */

package de.morihofi.certgine.utils.javaversion;

import lombok.Getter;
import lombok.NonNull;


import java.text.MessageFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// @formatter:off
/**
 * Immutable version class constructed from a Java version string. The Java
 * version takes the form:
 * <p>
 * major.middle.minor[_update][-identifier]
 * <p>
 * Object's of this class can be used to compare Java different versions.
 */
// @formatter:on
public class JavaVersion implements Comparable<Object> {

    public static final JavaVersion JRE_VERSION_130 = new JavaVersion("1.3.0");
    public static final JavaVersion JRE_VERSION_140 = new JavaVersion("1.4.0");
    public static final JavaVersion JRE_VERSION_150 = new JavaVersion("1.5.0");
    public static final JavaVersion JRE_VERSION_160 = new JavaVersion("1.6.0");
    public static final JavaVersion JRE_VERSION_170 = new JavaVersion("1.7.0");
    public static final JavaVersion JRE_VERSION_180 = new JavaVersion("1.8.0");
    public static final JavaVersion JRE_VERSION_9 = new JavaVersion("9");
    public static final JavaVersion JRE_VERSION_10 = new JavaVersion("10");
    public static final JavaVersion JRE_VERSION_11 = new JavaVersion("11");
    public static final JavaVersion JRE_VERSION_12 = new JavaVersion("12");
    public static final JavaVersion JRE_VERSION_13 = new JavaVersion("13");
    public static final JavaVersion JRE_VERSION_14 = new JavaVersion("14");
    public static final JavaVersion JRE_VERSION_15 = new JavaVersion("15");
    public static final JavaVersion JRE_VERSION_16 = new JavaVersion("16");
    public static final JavaVersion JRE_VERSION_17 = new JavaVersion("17");
    public static final JavaVersion JRE_VERSION_18 = new JavaVersion("18");
    public static final JavaVersion JRE_VERSION_19 = new JavaVersion("19");
    public static final JavaVersion JRE_VERSION_20 = new JavaVersion("20");
    public static final JavaVersion JRE_VERSION_21 = new JavaVersion("21");
    public static final JavaVersion JRE_VERSION_22 = new JavaVersion("22");
    public static final JavaVersion JRE_VERSION_23 = new JavaVersion("23");
    public static final JavaVersion JRE_VERSION_24 = new JavaVersion("24");
    private static final String VERSION_NUMBER_REGEXP = "([0-9]+(?:\\.[0-9]*)*)";
    private static final String REST_REGEXP = "(?:[_\\-\\.\\+a-zA-Z0-9]*)";
    private static final String VERSION_FORMAT = "^" + VERSION_NUMBER_REGEXP + REST_REGEXP + "$";
    private static final Pattern VERSION_STRING_PATTERN = Pattern.compile(VERSION_FORMAT);
    private static JavaVersion jreVersion;

    /**
     * Get the current JRE version.
     *
     * @return The JRE version.
     * @throws VersionException If JRE's version is not parseable
     */
    public static JavaVersion getJreVersion() {
        if (jreVersion == null) {
            String jreVersionProp = System.getProperty("java.version");

            jreVersion = new JavaVersion(jreVersionProp);
        }

        return jreVersion;
    }

    private final String javaVersion;
    /**
     * Get Java version's major number.
     *
     * @return Minor number
     */
    @Getter
    private final int major;
    /**
     * Get Java version's minor number.
     *
     * @return Minor number
     */
    @Getter
    private final int minor;
    /**
     * Get Java version's security number.
     *
     * @return Minor number
     */
    @Getter
    private final int security;

    /**
     * Construct a JavaVersion object for the current Java environment.
     *
     * @throws VersionException If the Java version string cannot be parsed
     */
    public JavaVersion() {
        this(System.getProperty("java.version"));
    }

    /**
     * Construct a JavaVersion object from the supplied string.
     *
     * @param javaVersion The Java version string
     * @throws VersionException If the Java version string cannot be parsed
     */
    public JavaVersion(String javaVersion) {
        this.javaVersion = javaVersion;

        Matcher matcher = VERSION_STRING_PATTERN.matcher(javaVersion);
        if (!matcher.matches()) {
            throw new VersionException(
                    MessageFormat.format("Could not parse ''{0}'' as a Java version number", javaVersion));
        }

        String vnum = matcher.group(1);
        Version version = new Version(vnum);

        this.major = version.getMajor();
        this.minor = version.getMinor();
        this.security = version.getBugfix();
    }

    /**
     * Compares version of the current JRE with the passed version.
     *
     * @param javaVersion Java version to compare to.
     * @return True, if current JRE is same version or higher.
     */
    public boolean isAtLeast(JavaVersion javaVersion) {
        return compareTo(javaVersion) >= 0;
    }

    /**
     * Compares version of the current JRE with the passed version.
     *
     * @param javaVersion Java version to compare to.
     * @return True, if current JRE is lower version.
     */
    public boolean isBelow(JavaVersion javaVersion) {
        return compareTo(javaVersion) < 0;
    }

    @Override
    public int compareTo(@NonNull Object object) {
        JavaVersion cmpJavaVersion = (JavaVersion) object;

        if (major > cmpJavaVersion.getMajor()) {
            return 1;
        } else if (major < cmpJavaVersion.getMajor()) {
            return -1;
        }

        if (minor > cmpJavaVersion.getMinor()) {
            return 1;
        } else if (minor < cmpJavaVersion.getMinor()) {
            return -1;
        }

        if (security > cmpJavaVersion.getSecurity()) {
            return 1;
        } else if (security < cmpJavaVersion.getSecurity()) {
            return -1;
        }

        return 0;
    }

    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }

        if (!(object instanceof JavaVersion)) {
            return false;
        }

        return compareTo(object) == 0;
    }

    @Override
    public int hashCode() {
        int result = 27;

        result = 53 * result + major;
        result = 53 * result + minor;
        result = 53 * result + security;

        return result;
    }

    @Override
    public String toString() {
        return javaVersion;
    }
}
