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
		String usernameField = config.getJsonObject(SoileConfigLoader.DB_FIELDS)
				  .getString("usernameField");
		JsonObject participant1 = new JsonObject().put(usernameField, "participant")
										   .put(config.getJsonObject(SoileConfigLoader.DB_FIELDS)
													  .getString("passwordField"), "password");
		JsonObject user1 = new JsonObject().put(usernameField, "participant")
										   .put(config.getJsonObject(SoileConfigLoader.DB_FIELDS)
												   	  .getString("passwordField"), "password2");
		
		JsonObject user2 = new JsonObject().put(usernameField, "user2")
											.put(config.getJsonObject(SoileConfigLoader.DB_FIELDS)
													   .getString("passwordField"), "password");
		
		
		//we crate the same user in both participant and user databases, along with a 
		createUser(participant1, participant_options, context);
		createUser(user1, user_options, context);
		
		
		SoileAuthentication pauth = new SoileAuthentication(mongo_client, participant_options, config);		
		SoileAuthentication uauth = new SoileAuthentication(mongo_client, user_options, config);
		final Async async = context.async();
		//test auth against the correct database
		pauth.authenticate(participant1).onComplete( user -> {
			if(user.succeeded())
			{
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
		uauth.authenticate(participant1).onComplete( user -> {
			if(user.succeeded())
			{
				context.fail("Invalid authentication succeeded");
				invalidAuth.complete();
			}
			else
			{
				context.assertEquals("Invalid user or wrong password for user participant", user.cause().getMessage());
				invalidAuth.complete();
			}
		});
		final Async invalidAuth2 = context.async();
		uauth.authenticate(user2).onComplete( user -> {
			if(user.succeeded())
			{
				context.fail("Invalid authentication succeeded");
				invalidAuth2.complete();
			}
			else
			{
				context.assertEquals("Invalid user or wrong password for user user2", user.cause().getMessage());
				invalidAuth2.complete();
			}
		});
		
	}
}
