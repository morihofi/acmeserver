package de.morihofi.acmeserver.tools.email;

import de.morihofi.acmeserver.Main;
import de.morihofi.acmeserver.config.EmailConfig;
import jakarta.mail.*;
import jakarta.mail.internet.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Properties;

public class SendMail {

    private SendMail(){}

    public static final Logger log = LogManager.getLogger(SendMail.class);

    /**
     * Sends an email using the specified email configuration, including optional encryption settings.
     *
     * @param toEmail The recipient's email address.
     * @param subject The subject of the email.
     * @param content The content of the email.
     * @throws MessagingException If there is an issue with the email sending process.
     */
    public static void sendMail(String toEmail, String subject, String content) throws MessagingException {
        EmailConfig emailConfig = Main.appConfig.getEmailSmtp();

        if (!emailConfig.getEnabled()) {
            log.info("Not sending email, because email sending is disabled in the config");
            return;
        }

        Properties emailProp = new Properties();
        emailProp.put("mail.smtp.auth", "true");
        emailProp.put("mail.smtp.host", emailConfig.getHost());
        emailProp.put("mail.smtp.port", String.valueOf(emailConfig.getPort()));

        if ("starttls".equals(emailConfig.getEncryption())) {
            emailProp.put("mail.smtp.starttls.enable", "true");
        } else if ("ssl".equals(emailConfig.getEncryption()) || "tls".equals(emailConfig.getEncryption())) {
            emailProp.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            emailProp.put("mail.smtp.ssl.checkserveridentity", "true");
        } else {
            // No encryption, use a plain connection
            emailProp.put("mail.smtp.starttls.enable", "false");
            emailProp.put("mail.smtp.ssl.trust", "*");
        }

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

        log.info("Sending E-Mail with subject \"{}\" to \"{}\"", subject, toEmail);
        Transport.send(message);
        log.info("E-Mail has been sent");
    }
}
