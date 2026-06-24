package fi.abo.kogni.soile2.utils;

import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fi.abo.kogni.soile2.http_server.auth.PasswordReset;
import io.vertx.core.Vertx;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

public class MailSender {
	static final Logger LOGGER = LogManager.getLogger(MailSender.class);
	
	Vertx vertx;
	public MailSender(Vertx vertx)
	{
		this.vertx = vertx; 
	}
	
	public Future<Void> sendMail(String mailAddress, String content, String subject, String from_name) {
		Promise<Void> promise = Promise.<Void>promise();
		this.vertx.executeBlocking(future -> {
			try {
				String domain = SoileConfigLoader.getServerProperty("domain");				
				String from = "no-reply@" + domain;
				
				Properties props = new Properties();
				props.put("mail.smtp.host", "localhost"); // we always send via the local mail server
				props.put("mail.smtp.port", 25);
				props.put("mail.smtp.auth", "false");
				props.put("mail.smtp.starttls.enable", "false");
				LOGGER.debug("Mailing to: " + mailAddress);
				Session session = Session.getInstance(props);
				Message message = new MimeMessage(session);
				message.setFrom(new InternetAddress(from, from_name));
				message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(mailAddress));
				message.setSubject(subject);
				message.setText(content);
				Transport.send(message);
				future.complete();
			} catch (Exception e) {
				LOGGER.error("Failed to send password reset email", e);
				future.fail(e);
			}
		}, res -> {
			if (res.succeeded()) {
				promise.complete();
			} else {
				promise.fail(res.cause());
			}
		});
		return promise.future();

	}
}
