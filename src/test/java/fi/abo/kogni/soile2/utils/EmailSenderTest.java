package fi.abo.kogni.soile2.utils;
import static org.mockito.Mockito.any;

import org.junit.Test;
import org.junit.runner.RunWith;

import io.vertx.core.Vertx;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import jakarta.mail.Message;
import jakarta.mail.internet.MimeMessage;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class EmailSenderTest {

	  @Test
	  public void TestSendMail(TestContext context) {
	    Vertx vertx = Vertx.vertx();
	    EmailSender sender = new EmailSender(vertx); // or configurable ctor

	    AtomicReference<Message> captured = new AtomicReference<>();
	    CompletableFuture<Void> finished = new CompletableFuture<>();
	    
	    try (MockedStatic<jakarta.mail.Transport> mocked = Mockito.mockStatic(jakarta.mail.Transport.class)) {
	      mocked.when(() -> jakarta.mail.Transport.send(any(Message.class)))
	          .thenAnswer(inv -> {
	            Message msg = inv.getArgument(0, Message.class);
	            captured.set(msg);
	            finished.complete(null); // mark as "sent"
	            return null; // Transport.send is void
	          });

	      sender.sendMail("Hello body", "noreply@example.org", "Hi", "alice@example.org")
	          .onSuccess(ar -> {
	        	 Message msg = captured.get();
	        	 try {
	             context.assertTrue((msg instanceof MimeMessage));
	             context.assertEquals(msg.getSubject(), "Hi");
	             context.assertEquals(msg.getFrom()[0].toString(),"noreply@example.org");
	             context.assertEquals(msg.getAllRecipients()[0].toString(),"alice@example.org");
	             context.assertTrue(msg.getContent().toString().contains("Hello body"));
	        	 }
	        	 catch(Exception e) {
	        		 context.fail(e);
	        	 }
	             
	          })
	          .onFailure(err -> context.fail(err));	      
	    }	    
	    vertx.close();
	  }
}
