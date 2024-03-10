/*
 * Copyright (c) 2024 Moritz Hofmann <info@morihofi.de>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package de.morihofi.acmeserver.webui;

import io.javalin.http.Context;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Locale;
import java.util.ResourceBundle;

public class JteLocalizer implements gg.jte.support.LocalizationSupport {

    /**
     * Logger
     */
    public static final Logger log = LogManager.getLogger(JteLocalizer.class);
    private final Locale locale;
    private final ResourceBundle bundle;

    public static JteLocalizer getLocalizerFromContext(Context locale) {
        return new JteLocalizer(locale.attribute(WebUI.ATTR_LOCALE));
    }

    public JteLocalizer(Locale locale) {
        this.locale = locale;
        // ResourceBundle vorbereiten
        bundle = ResourceBundle.getBundle("webui.language", locale);
    }

    @Override
    public String lookup(String key) {
        // However this works in your localization framework
        if (bundle.containsKey(key)) {
            return bundle.getString(key);
        }
        log.warn("WebUI language key {} not found for language {}", key, bundle.getLocale());
        return key;
    }

    public String lookup(String key, Object... variables) {
        if (bundle.containsKey(key)) {
            return String.format(bundle.getString(key), variables);
        }
        log.warn("WebUI language key {} not found for language {}", key, bundle.getLocale());
        return key;
    }

    public Locale getLocale() {
        return locale;
    }
}