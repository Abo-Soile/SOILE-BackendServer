package fi.abo.kogni.soile2.utils;

import java.util.Properties;
import java.util.Vector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.Promise;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

public class EmailSender {
	private Vertx vertx;
	static final Logger LOGGER = LogManager.getLogger(EmailSender.class);
	public EmailSender(Vertx vertx)
	{
		this.vertx = vertx;
	}
	public Future<Void> sendMail(String content, String from, String subject, String mailAddress) {
		JsonArray address = new JsonArray().add(mailAddress);		
		return this.sendMail(content, from, subject, address);
	}
	public Future<Void> sendMail(String content, String from, String subject, JsonArray mailAddresses) {
		Promise<Void> promise = Promise.<Void>promise();
		try {
			InternetAddress[] addresses = new InternetAddress[mailAddresses.size()];
			for (int i = 0; i < mailAddresses.size(); i++) {
			    addresses[i] = new InternetAddress(mailAddresses.getString(i));
			}				
			this.vertx.executeBlocking(future -> {			
				try {				
						Properties props = new Properties();
						props.put("mail.smtp.host", "localhost"); // we always send via the local mail server
						props.put("mail.smtp.port", 25);
						props.put("mail.smtp.auth", "false");
						props.put("mail.smtp.starttls.enable", "false");
						LOGGER.debug("Mailing to: " + addresses);
						Session session = Session.getInstance(props);
						Message message = new MimeMessage(session);
						message.setFrom(new InternetAddress(from, "SOILE Server Password Reset"));
						message.setRecipients(Message.RecipientType.TO, addresses);
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
		}
		catch(AddressException e)
		{
			promise.fail("Error in one of the indicated email addresses: " + e.getMessage());
		
		}
		return promise.future();
	}
}
