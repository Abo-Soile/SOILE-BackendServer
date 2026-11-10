package fi.abo.kogni.soile2.http_server.authentication;

import org.junit.Test;
import org.junit.runner.RunWith;

import fi.abo.kogni.soile2.http_server.SoileVerticleTest;
import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class PasswordResetRouteTest extends SoileVerticleTest {

	@Test
	public void testResetRequestCreatesTokenAndReturnsGenericResponse(TestContext context) {
		Async async = context.async();
		String username = "passwordResetRouteUser";
		String usernameField = SoileConfigLoader.getUserdbField("usernameField");
		String userCollection = SoileConfigLoader.getdbProperty("userCollection");
		String tokenCollection = SoileConfigLoader.getdbProperty("tokenCollection");
		JsonObject user = new JsonObject().put(usernameField, username);

		mongo_client.save(userCollection, user)
				.compose(saved -> webclient.post(port, "localhost", "/password/request_reset")
						.sendJsonObject(new JsonObject().put("emailOrUserName", username)))
				.onSuccess(response -> {
					context.assertEquals(200, response.statusCode());
					context.assertEquals("Request received", response.bodyAsString());
					mongo_client.find(tokenCollection, new JsonObject().put(usernameField, username))
							.onSuccess(tokens -> {
								context.assertEquals(1, tokens.size());
								async.complete();
							})
							.onFailure(err -> {
								context.fail(err);
								async.complete();
							});
				})
				.onFailure(err -> {
					context.fail(err);
					async.complete();
				});
	}

	@Test
	public void testOneTimeAuthReturnsJwtAndConsumesResetToken(TestContext context) {
		Async async = context.async();
		String username = "oneTimeAuthUser";
		String resetToken = "one-time-reset-token";
		String usernameField = SoileConfigLoader.getUserdbField("usernameField");
		String userCollection = SoileConfigLoader.getdbProperty("userCollection");
		String tokenCollection = SoileConfigLoader.getdbProperty("tokenCollection");
		JsonObject user = new JsonObject()
				.put("username", username)
				.put("password", "reset-test-password")
				.put("email", "one-time-auth@example.org")
				.put("type", "participant")
				.put("fullname", "One Time Auth User");
		JsonObject token = new JsonObject()
				.put("token", resetToken)
				.put(usernameField, username)
				.put("timestamp", System.currentTimeMillis());

		vertx.eventBus().request("soile.umanager.addUser", user)
				.compose(added -> mongo_client.save(tokenCollection, token))
				.compose(saved -> webclient.post(port, "localhost", "/password/one_time_auth")
						.addQueryParam("token", resetToken)
						.send())
				.onSuccess(response -> {
					context.assertEquals(200, response.statusCode());
					JsonObject body = response.bodyAsJsonObject();
					context.assertNotNull(body.getString("token"));
					context.assertFalse(body.getString("token").isEmpty());

					mongo_client.findOne(tokenCollection, new JsonObject().put("token", resetToken), new JsonObject())
							.onSuccess(remainingToken -> {
								context.assertNull(remainingToken);
								async.complete();
							})
							.onFailure(err -> {
								context.fail(err);
								async.complete();
							});
				})
				.onFailure(err -> {
					context.fail(err);
					async.complete();
				});
	}
}