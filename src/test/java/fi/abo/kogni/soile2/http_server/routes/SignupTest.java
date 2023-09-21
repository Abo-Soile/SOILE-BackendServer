package fi.abo.kogni.soile2.http_server.routes;

import org.junit.Test;

import fi.abo.kogni.soile2.http_server.SoileWebTest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientSession;

public class SignupTest extends SoileWebTest {

	@Test
	public void testlistRunning(TestContext context)
	{
		WebClientSession client = createSession();
		Async testAsync = context.async();
		JsonObject signupRequest = new JsonObject()
									.put("username","newUser").put("email","some.address@server.fi")
									.put("password","passwordhere")
									.put("confirmPassword","passwordhere")
									.put("fullname","Full Name")
									.put("role","");					
		POST(client,"/register",null,signupRequest)
		.compose(res -> {
				return createAuthedSession("newUser","passwordhere");
		}
		)
		.onFailure(err -> context.fail(err))
		.onSuccess(loggedIn -> {
			testAsync.complete();
		});
	}
}
