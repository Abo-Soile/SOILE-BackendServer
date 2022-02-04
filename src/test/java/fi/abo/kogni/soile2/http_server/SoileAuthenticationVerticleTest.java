package fi.abo.kogni.soile2.http_server;

import java.net.HttpURLConnection;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import fi.abo.kogni.soile2.http_server.utils.SoileCommUtils;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.MultiMap;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.client.WebClient;

@RunWith(VertxUnitRunner.class)
public class SoileAuthenticationVerticleTest extends MongoTestBase{

	private WebClient webclient;
	private int port;
	@Before
	public void setUp(TestContext context){
		super.setUp(context);
		// We pass the options as the second parameter of the deployVerticle method.
		webclient = WebClient.create(vertx);
		vertx.deployVerticle(SoileServerVerticle.class.getName(), new DeploymentOptions(), context.asyncAssertSuccess());
		port = cfg.getJsonObject("http_server").getInteger("port");
	}
	
	
	
	@Test
	public void testAuthentication(TestContext context) {
		Async async = context.async();
		try {
			JsonObject userObject = new JsonObject()
					.put("username", "testUser")
					.put("password", "testpw")
					.put("email", "This@that.com")
					.put("type", "participant")
					.put("fullname","Test User")
					.put("remember","1");
			vertx.eventBus().request(SoileCommUtils.getEventBusCommand(uCfg,"addUser"), 
					userObject,res -> {
							if(res.succeeded())
							{
								//Now lets see if we can authenticate this.
								//This is a participant user, so we need to auth against the participant API
								MultiMap map = createFormFromJson(userObject);
								final Async sucAsync = context.async();
								webclient.post(port,"localhost","/services/auth").sendForm(map).onSuccess(authed ->
								{
									System.out.println("Valid Auth: " + authed.body().toString());
									context.assertTrue(authed.body().toString().contains("Login successful"));
									sucAsync.complete();	
								});
								final Async unsucAsync = context.async();
								userObject.put("type", "user");
								map = createFormFromJson(userObject);
								webclient.post(port,"localhost","/services/auth").sendForm(map).onSuccess(authed ->
								{
									System.out.println("Invalid Auth: " +  authed.body().toString());
									context.assertTrue(authed.body().toString().contains("Redirecting to /login"));
									context.assertEquals(302,authed.statusCode());
									unsucAsync.complete();	
								});
								async.complete();
							}
							else
							{
								context.fail("Couldn't create user");
							}
							
						});
		}
		catch(Exception e)
		{
			context.fail(e.getMessage());
			async.complete();
		}
	}  
}
