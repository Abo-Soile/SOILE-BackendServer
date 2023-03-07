package fi.abo.kogni.soile2.http_server;

import fi.abo.kogni.soile2.http_server.auth.SoileAuthorization.Roles;
import fi.abo.kogni.soile2.utils.SoileCommUtils;
import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;

public interface UserVerticleTest {

	public default Future<Void> createUser(Vertx vertx, String username, String password, String fullname, String email, Roles role)
	{
		Promise<Void> userCreatedPromise = Promise.promise(); 
		JsonObject userData = new JsonObject().put("username", username)
											  .put("password", password);

		vertx.eventBus().request(SoileCommUtils.getEventBusCommand(SoileConfigLoader.USERMGR_CFG, "addUser"), userData)
		.onSuccess(res -> 
		{
			boolean updateDetails = false;
			if(fullname != null)
			{
				userData.put("fullname", fullname);
				updateDetails = true;
			}
			if(email != null)
			{
				updateDetails = true;
				userData.put("email", email);
			}
			if(role != null)
			{
				updateDetails = true;
				userData.put("role", role.toString());
			}
			if(updateDetails)
			{				
				vertx.eventBus().request(SoileCommUtils.getEventBusCommand(SoileConfigLoader.USERMGR_CFG, "setUserInfo"), userData)
				.onSuccess(done -> {
					userCreatedPromise.complete();
				})
				.onFailure(err -> {
					vertx.eventBus().request(SoileCommUtils.getEventBusCommand(SoileConfigLoader.USERMGR_CFG, "removeUser"), userData)
					.onSuccess(done -> {
						userCreatedPromise.fail(err);
					})
					.onFailure(err2 -> {
						userCreatedPromise.fail(err2);	
					});
					
				});
			}
			else
			{
				userCreatedPromise.complete();
			}
			
		})
		.onFailure(err -> userCreatedPromise.fail(err));
		
		
		return userCreatedPromise.future();
	}
	
	public default Future<Void> createUser(Vertx vertx, String username, String password)
	{
		return createUser(vertx, username, password, null, null, null);
	}
	
	public default Future<Void> createUser(Vertx vertx, String username, String password, Roles role)
	{
		return createUser(vertx, username, password, null, null, role);
	}
	
	public default Future<JsonObject> getUserDetailsFromDB(MongoClient client, String username)
	{
		return client.findOne(SoileConfigLoader.getdbProperty("userCollection"), new JsonObject().put(SoileConfigLoader.getUserdbField("usernameField"),username), null);
	}
	
	public default Future<Void> createAdmin(Vertx vertx, String username, String password)
	{
		return createUser(vertx, username, password, Roles.Admin);
	}
	
	public default Future<Void> createResearcher(Vertx vertx, String username, String password)
	{
		return createUser(vertx, username, password, Roles.Researcher);
	}
}




	