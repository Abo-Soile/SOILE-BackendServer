package fi.abo.kogni.soile2.http_server.authentication;

import org.junit.Test;
import org.junit.runner.RunWith;

import fi.abo.kogni.soile2.http_server.MongoTestBase;
import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class SoileAuthenticationTest extends MongoTestBase{


	@Test	
	public void testValidAuthentication(TestContext context) {
		// create a user, that we want to authenticate later on.
		JsonObject participant1 = new JsonObject().put("username", "participant")
				.put(cfg.getJsonObject(SoileConfigLoader.SESSION_CFG).getString("passwordField"), "password");

		JsonObject invalidUser = new JsonObject().put("username", "user1")
				.put(cfg.getJsonObject(SoileConfigLoader.SESSION_CFG).getString("passwordField"), "password2");

		JsonObject DB_User = new JsonObject().put(cfg.getJsonObject(SoileConfigLoader.DB_FIELDS).getString("usernameField"), "participant")
				.put(cfg.getJsonObject(SoileConfigLoader.DB_FIELDS).getString("passwordField"), "password"); 
		Async overall = context.async();
		// Lets create two users.
		createUser(DB_User, context).onComplete(x1 -> 
		{			

			SoileAuthentication auth = new SoileAuthentication(mongo_client);		
			final Async async = context.async();
			// test auth
			auth.authenticate(participant1).onComplete( user -> {
				if(user.succeeded())
				{
					System.out.println("Auth participant Success Tested");
					context.assertEquals(participant1.getString("username"),
							user.result().principal().getString("username"));
					async.complete();
				}			
				else
				{
					System.out.println(user.cause().getMessage());
					context.fail("Could not authenticate");
					async.complete();
				}
			});
			overall.complete();
		});		
	}

	@Test	
	public void testInValidAuthentication(TestContext context) {
		// create a user, that we want to authenticate later on.
		JsonObject participant1 = new JsonObject().put("username", "participant")
				.put(cfg.getJsonObject(SoileConfigLoader.SESSION_CFG).getString("passwordField"), "password");

		JsonObject invalidUser = new JsonObject().put("username", "user1")
				.put(cfg.getJsonObject(SoileConfigLoader.SESSION_CFG).getString("passwordField"), "password2");

		JsonObject DB_User = new JsonObject().put(cfg.getJsonObject(SoileConfigLoader.DB_FIELDS).getString("usernameField"), "participant")
				.put(cfg.getJsonObject(SoileConfigLoader.DB_FIELDS).getString("passwordField"), "password"); 

		
		Async overall = context.async();
		// Lets create two users.
		createUser(DB_User, context).onComplete(x1 -> 
		{					

			SoileAuthentication auth = new SoileAuthentication(mongo_client);				
			final Async invalidAuth = context.async();
			auth.authenticate(invalidUser).onComplete( user -> {
				if(user.succeeded())
				{				
					context.fail("Invalid authentication succeeded");
					invalidAuth.complete();
				}
				else
				{
					System.out.println("Invalid auth Tested");
					context.assertEquals("Invalid username or invalid password for username: user1", user.cause().getMessage());
					invalidAuth.complete();
				}
			});
			overall.complete();
		});
	}
}
