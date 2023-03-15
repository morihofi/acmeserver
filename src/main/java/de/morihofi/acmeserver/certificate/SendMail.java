package de.morihofi.acmeserver.certificate;

import de.morihofi.acmeserver.Main;
import jakarta.mail.*;
import jakarta.mail.internet.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Properties;

public class SendMail {

    public static final Logger log = LogManager.getLogger(SendMail.class);

    public static void sendMail(String toEmail, String subject, String content) throws MessagingException {

        Properties emailProp = new Properties();
        emailProp.put("mail.smtp.auth", true);
        if (Main.emailSMTPEncryption.equals("starttls")){
            emailProp.put("mail.smtp.starttls.enable", "true");
        }
        emailProp.put("mail.smtp.host", Main.emailSMTPServer);
        emailProp.put("mail.smtp.port", String.valueOf(Main.emailSMTPPort));
        emailProp.put("mail.smtp.ssl.trust", Main.emailSMTPServer);

        Session session = Session.getInstance(emailProp, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(Main.emailSMTPUsername, Main.emailSMTPPassword);
            }
        });

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(Main.emailSMTPUsername));
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
