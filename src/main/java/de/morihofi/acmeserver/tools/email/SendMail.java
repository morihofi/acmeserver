/*
 * Copyright (c) 2024 Moritz Hofmann <info@morihofi.de>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package de.morihofi.acmeserver.tools.email;

import de.morihofi.acmeserver.config.EmailConfig;
import de.morihofi.acmeserver.tools.ServerInstance;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.invoke.MethodHandles;
import java.util.Properties;

/**
 * Class for sending E-Mails
 */
public class SendMail {

    /**
     * Logger
     */
    private static final Logger LOG = LogManager.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Sends an email using the specified email configuration, including optional encryption settings.
     *
     * @param toEmail The recipient's email address.
     * @param subject The subject of the email.
     * @param content The content of the email.
     * @param serverInstance The server instance to use.
     * @throws MessagingException If there is an issue with the email sending process.
     */
    public static void sendMail(String toEmail, String subject, String content, ServerInstance serverInstance) throws MessagingException {
        EmailConfig emailConfig = serverInstance.getAppConfig().getEmailSmtp();

        if (!emailConfig.getEnabled()) {
            LOG.info("Not sending email, because email sending is disabled in the config");
            return;
        }

        Properties emailProp = getEmailProperties(emailConfig);

        Session session = Session.getInstance(emailProp, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(emailConfig.getUsername(), emailConfig.getPassword());
            }
        });

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(emailConfig.getUsername()));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
        message.setSubject(subject);

        MimeBodyPart mimeBodyPart = new MimeBodyPart();
        mimeBodyPart.setContent(content, "text/html; charset=utf-8");

        Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(mimeBodyPart);

        message.setContent(multipart);

        LOG.info("Sending E-Mail with subject \"{}\" to \"{}\"", subject, toEmail);
        Transport.send(message);
        LOG.info("E-Mail has been sent");
    }

    /**
     * Constructs a {@link Properties} object containing SMTP configuration for sending emails, based on the provided {@link EmailConfig}.
     * This method sets up properties such as SMTP authentication, host, port, and encryption according to the configuration specified in
     * {@code emailConfig}.
     * <p>
     * The encryption can be configured to use STARTTLS or SSL/TLS. If STARTTLS is specified, it enables it by setting
     * {@code mail.smtp.starttls.enable} to {@code true}. For SSL/TLS encryption, it sets the socket factory to use
     * {@code javax.net.ssl.SSLSocketFactory} and enables server identity checking. If no encryption is specified or recognized, the method
     * configures the properties for a plain connection without STARTTLS, trusting all hosts.
     * <p>
     * This method ensures that SMTP authentication is always enabled.
     *
     * @param emailConfig The {@link EmailConfig} object containing the email server configuration, including host, port, and encryption
     *                    type.
     * @return A {@link Properties} object with SMTP settings configured per the given {@code emailConfig}.
     * @throws NullPointerException if {@code emailConfig} is null.
     */
    private static Properties getEmailProperties(EmailConfig emailConfig) {
        Properties emailProp = new Properties();
        emailProp.put("mail.smtp.auth", "true");
        emailProp.put("mail.smtp.host", emailConfig.getHost());
        emailProp.put("mail.smtp.port", emailConfig.getPort());

        switch (emailConfig.getEncryption()) {
            case "starttls" -> emailProp.put("mail.smtp.starttls.enable", "true");
            case "ssl", "tls" -> {
                emailProp.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
                emailProp.put("mail.smtp.ssl.checkserveridentity", "true");
            }
            default ->
                    LOG.warn("Unencrypted email connection. Consider using SSL/TLS or STARTTLS for enhanced security.");
        }

        return emailProp;
    }

    /**
     * Private constructor to prevent class instantiation
     */
    private SendMail() {
    }
}
