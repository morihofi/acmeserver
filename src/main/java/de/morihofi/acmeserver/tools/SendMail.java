package de.morihofi.acmeserver.tools;

import de.morihofi.acmeserver.Main;
import de.morihofi.acmeserver.config.EmailConfig;
import jakarta.mail.*;
import jakarta.mail.internet.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Properties;

public class SendMail {

    public static final Logger log = LogManager.getLogger(SendMail.class);

    public static void sendMail(String toEmail, String subject, String content) throws MessagingException {

        EmailConfig emailConfig = Main.appConfig.getEmailSmtp();

        if(!emailConfig.getEnabled()){
            log.info("Not sending email, cause email sending is disabled in config");
            return;
        }

        Properties emailProp = new Properties();
        emailProp.put("mail.smtp.auth", true);
        if (emailConfig.getEncryption().equals("starttls")){
            emailProp.put("mail.smtp.starttls.enable", "true");
        }
        emailProp.put("mail.smtp.host", emailConfig.getHost());
        emailProp.put("mail.smtp.port", String.valueOf(emailConfig.getPort()));
        emailProp.put("mail.smtp.ssl.trust", emailConfig.getHost());

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

        String msg = content;

        MimeBodyPart mimeBodyPart = new MimeBodyPart();
        mimeBodyPart.setContent(msg, "text/html; charset=utf-8");

        Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(mimeBodyPart);

        message.setContent(multipart);

        log.info("Sending E-Mail with subject \"" + subject + "\" to \"" + toEmail + "\"");
        Transport.send(message);
        log.info("E-Mail has sent");

    }
}
