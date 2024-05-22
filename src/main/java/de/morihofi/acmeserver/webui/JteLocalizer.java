/*
 * Copyright (c) 2024 Moritz Hofmann <info@morihofi.de>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so,
 *  subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package de.morihofi.acmeserver.webui;

import io.javalin.http.Context;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.invoke.MethodHandles;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Provides localization support for the web UI, enabling the application to display content in different languages based on the user's
 * locale. This class is designed to work with Javalin's context to dynamically determine the locale for each request and provide localized
 * strings from resource bundles.
 * <p>
 * The class uses {@link ResourceBundle} for managing localization strings and supports formatting messages with variables. It integrates
 * seamlessly with gg.jte templates, allowing for dynamic content localization based on the user's preferences.
 */
public class JteLocalizer implements gg.jte.support.LocalizationSupport {

    /**
     * Logger
     */
    private static final Logger LOG = LogManager.getLogger(MethodHandles.lookup().getClass());

    /**
     * Retrieves a {@link JteLocalizer} instance from the provided Javalin {@link Context}. This method utilizes a locale attribute from the
     * context to create a localizer that is specific to the user's locale settings.
     *
     * @param context the Javalin context from which to extract the locale attribute.
     * @return a {@link JteLocalizer} instance configured with the user's locale.
     */
    public static JteLocalizer getLocalizerFromContext(Context context) {
        return new JteLocalizer(context.attribute(WebUI.ATTR_LOCALE));
    }
    /**
     * The locale for this instance of the localizer.
     */
    private final Locale locale;
    /**
     * The resource bundle that contains the localization strings for the specified locale.
     */
    private final ResourceBundle bundle;

    /**
     * Creates a new {@link JteLocalizer} instance based on the specified locale. This method prepares a resource bundle for the web UI's
     * language using the provided locale.
     *
     * @param locale the locale to use for localization.
     */
    public JteLocalizer(Locale locale) {
        this.locale = locale;
        bundle = ResourceBundle.getBundle("webui.language", locale);
    }

    /**
     * Looks up a localized string by its key. If the key is found in the resource bundle, the corresponding localized string is returned.
     * Otherwise, a warning is logged, and the key itself is returned.
     *
     * @param key the key for the localized string.
     * @return the localized string associated with the key, or the key itself if not found.
     */
    @Override
    public String lookup(String key) {
        // However this works in your localization framework
        if (bundle.containsKey(key)) {
            return bundle.getString(key);
        }
        LOG.warn("WebUI language key {} not found for language {}", key, bundle.getLocale());
        return key;
    }

    /**
     * Looks up a localized string by its key and formats it with the provided variables. If the key is found in the resource bundle, the
     * corresponding localized string is formatted with the variables and returned. Otherwise, a warning is logged, and the key itself is
     * returned.
     *
     * @param key       the key for the localized string.
     * @param variables the variables to be used in formatting the localized string.
     * @return the formatted localized string, or the key itself if not found.
     */
    public String lookup(String key, Object... variables) {
        if (bundle.containsKey(key)) {
            return String.format(bundle.getString(key), variables);
        }
        LOG.warn("WebUI language key {} not found for language {}", key, bundle.getLocale());
        return key;
    }

    /**
     * Gets the locale associated with this {@link JteLocalizer} instance.
     *
     * @return the locale.
     */
    public Locale getLocale() {
        return locale;
    }
}
