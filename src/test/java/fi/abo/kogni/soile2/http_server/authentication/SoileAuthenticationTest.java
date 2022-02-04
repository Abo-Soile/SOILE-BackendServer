package fi.abo.kogni.soile2.http_server.authentication;

import org.junit.Test;
import org.junit.runner.RunWith;

import fi.abo.kogni.soile2.http_server.MongoTestBase;
import fi.abo.kogni.soile2.http_server.utils.SoileConfigLoader;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class SoileAuthenticationTest extends MongoTestBase{
	
	
	@Test	
	public void testAuthentication(TestContext context) {
		// create a user, that we want to authenticate later on.
		String usernameField = cfg.getJsonObject(SoileConfigLoader.DB_FIELDS)
				  .getString("usernameField");
		String userTypeField = cfg.getJsonObject(SoileConfigLoader.DB_FIELDS)
				  .getString("userTypeField");
		String passwordField = cfg.getJsonObject(SoileConfigLoader.DB_FIELDS)
				  .getString("passwordField");
		JsonObject participant1 = new JsonObject().put(usernameField, "participant")
										   .put(passwordField, "password")
										   .put(userTypeField, "participant");
		JsonObject user1 = new JsonObject().put(usernameField, "participant")
										   .put(passwordField, "password")
										   .put(userTypeField, "user");
		
		JsonObject user2 = new JsonObject().put(usernameField, "user2")
										   .put(passwordField, "password2")
										   .put(userTypeField, "user");
		Async overall = context.async();
		//we crate the same user in both participant and user databases, along with a 
		createUser(participant1, authOptions, context).onComplete(x1 -> 
		{
		createUser(user2, authOptions, context)
		.onComplete(x2 -> 
		{
						
		
		SoileAuthentication auth = new SoileAuthentication(mongo_client, authOptions, cfg);		
		final Async async = context.async();
		//test auth against the correct database
		auth.authenticate(participant1).onComplete( user -> {
			if(user.succeeded())
			{
				System.out.println("Auth participant Success Tested");
				context.assertEquals(participant1.getString(usernameField), user.result().principal().getString(usernameField));
				async.complete();
			}			
			else
			{
				context.fail("Could not authenticate");
				async.complete();
			}
		});
		final Async invalidAuth = context.async();
		auth.authenticate(user1).onComplete( user -> {
			if(user.succeeded())
			{				
				context.fail("Invalid authentication succeeded");
				invalidAuth.complete();
			}
			else
			{
				System.out.println("Invalid auth Tested");
				context.assertEquals("Invalid user or wrong password for participant", user.cause().getMessage());
				invalidAuth.complete();
			}
		});
		final Async invalidAuth2 = context.async();
		auth.authenticate(user2).onComplete( user -> {
			if(user.succeeded())
			{
				System.out.println("Valid user auth tested");
				context.assertEquals(user2.getString(usernameField), user.result().principal().getString(usernameField));
				invalidAuth2.complete();
			}
			else
			{
				context.fail("Could not authenticate");
				invalidAuth2.complete();
			}
			
		});
		overall.complete();
		});
		});
	}
}
