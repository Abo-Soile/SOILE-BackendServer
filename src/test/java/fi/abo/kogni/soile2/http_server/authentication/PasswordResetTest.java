package fi.abo.kogni.soile2.http_server.authentication;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import fi.abo.kogni.soile2.MongoTest;
import fi.abo.kogni.soile2.http_server.auth.PasswordReset;
import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import jakarta.mail.Message;
import jakarta.mail.Transport;

@RunWith(VertxUnitRunner.class)
public class PasswordResetTest extends MongoTest {

	@Test
	public void testCreateOneTimeLoginSendsEmailAndPersistsToken(TestContext context) {
		Async async = context.async();
		String username = "passwordResetUser";
		String email = "password-reset@example.org";
		String userCollection = SoileConfigLoader.getdbProperty("userCollection");
		JsonObject user = new JsonObject()
				.put(SoileConfigLoader.getUserdbField("usernameField"), username)
				.put(SoileConfigLoader.getUserdbField("userEmailField"), email);

		mongo_client.save(userCollection, user)
				.onSuccess(id -> vertx.runOnContext(ignored -> sendResetEmail(context, async, username, email)))
				.onFailure(err -> {
					context.fail(err);
					async.complete();
				});
	}

	private void sendResetEmail(TestContext context, Async async, String username, String email) {
		Vertx emailVertx = Mockito.mock(Vertx.class);
		doAnswer(invocation -> {
			Handler<Promise<Object>> blockingCode = invocation.getArgument(0);
			Handler<AsyncResult<Object>> resultHandler = invocation.getArgument(1);
			Promise<Object> result = Promise.promise();
			blockingCode.handle(result);
			AsyncResult<Object> outcome = result.future().succeeded()
					? Future.succeededFuture(result.future().result())
					: Future.failedFuture(result.future().cause());
			resultHandler.handle(outcome);
			return null;
		}).when(emailVertx).executeBlocking(
				Mockito.<Handler<Promise<Object>>>any(),
				Mockito.<Handler<AsyncResult<Object>>>any());

		AtomicReference<Message> sentMessage = new AtomicReference<>();
		MockedStatic<Transport> transportMock = Mockito.mockStatic(Transport.class);
		transportMock.when(() -> Transport.send(any(Message.class))).thenAnswer(invocation -> {
			sentMessage.set(invocation.getArgument(0));
			return null;
		});

		PasswordReset passwordReset = new PasswordReset(mongo_client, null, emailVertx, null);
		passwordReset.createOneTimeLoginAndSendMail(username)
				.onSuccess(done -> {
					try {
						Message message = sentMessage.get();
						context.assertNotNull(message);
						context.assertEquals("SOILE Password Reset", message.getSubject());
						context.assertEquals(email, message.getAllRecipients()[0].toString());
						String linkPrefix = "https://" + SoileConfigLoader.getServerProperty("domain")
								+ "/password_reset?token=";
						String body = message.getContent().toString();
						int tokenStart = body.indexOf(linkPrefix) + linkPrefix.length();
						context.assertTrue(tokenStart >= linkPrefix.length());
						int tokenEnd = body.indexOf('\n', tokenStart);
						String token = body.substring(tokenStart, tokenEnd);
						transportMock.verify(() -> Transport.send(any(Message.class)));

						String tokenCollection = SoileConfigLoader.getdbProperty("tokenCollection");
						mongo_client.findOne(tokenCollection, new JsonObject().put("token", token), new JsonObject())
								.onSuccess(storedToken -> {
									try {
										context.assertNotNull(storedToken);
										context.assertEquals(username, storedToken.getString(
												SoileConfigLoader.getUserdbField("usernameField")));
									} finally {
										transportMock.close();
										async.complete();
									}
								})
								.onFailure(err -> {
									transportMock.close();
									context.fail(err);
									async.complete();
								});
					} catch (Exception err) {
						transportMock.close();
						context.fail(err);
						async.complete();
					}
				})
				.onFailure(err -> {
					transportMock.close();
					context.fail(err);
					async.complete();
				});
	}
}