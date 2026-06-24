package fi.abo.kogni.soile2.http_server.authentication.utils;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fi.abo.kogni.soile2.http_server.userManagement.exceptions.DuplicateUserEntryInDBException;
import fi.abo.kogni.soile2.http_server.userManagement.exceptions.InvalidLoginException;
import fi.abo.kogni.soile2.utils.SoileCommUtils;
import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.mongo.MongoClient;
/**
 * Utility functions for user handling
 * @author Thomas Pfau
 *
 */
public class UserUtils {

	static final Logger LOGGER = LogManager.getLogger(UserUtils.class);
	static final Pattern emailPattern = Pattern.compile("[A-Za-z0-9.$#_%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,4}");
	/**
	 * Build a user from the database result adding several properties.
	 * This fails if multiple or no entry were returned.
	 * @param dbResultList The Result from the DB query
	 * @param username the username to generate a user for 
	 * @return the User
	 * @throws DuplicateUserEntryInDBException if the indicated use is in the DB twice (should not happen)
	 * @throws InvalidLoginException if there is no user in the db 
	 */
	public static User buildUserFromDBResult(List<JsonObject> dbResultList, String username) throws DuplicateUserEntryInDBException,InvalidLoginException
	{
		if(dbResultList.size() == 1)
		{
			JsonObject userJson = dbResultList.get(0);	    		    		    
			return buildUserForDBEntry(userJson, username);
		}
		else if(dbResultList.size() > 1)
		{
			throw new DuplicateUserEntryInDBException(username);	
		}
		else
		{
			throw new InvalidLoginException(username);
		}

	}	
	
	/**
	 * check if a provided email address is valid
	 * @param emailAddress the email addess to check
	 * @return whether its a valid email address
	 */
	public static boolean isValidEmail(String emailAddress)
	{
		if(emailAddress == null)
		{
			return false;
		}
		Matcher mat = emailPattern.matcher(emailAddress);
		return mat.matches();
	}
	/**
	 * Build a user from an individual database entry.
	 * @param userJson the Json representing the data for the user (
	 * @param username the username to build the user 
	 * @return the generated user with all necessary properties set.
	 */
	public static User buildUserForDBEntry(JsonObject userJson, String username)
	{

		User user = User.fromName(userJson.getString(SoileConfigLoader.getUserdbField("usernameField")));
		user.principal().put(SoileConfigLoader.getSessionProperty("validSessionCookies"), userJson.getValue(SoileConfigLoader.getUserdbField("storedSessions")));
		user.principal().put(SoileConfigLoader.getSessionProperty("userRoles"), userJson.getValue(SoileConfigLoader.getUserdbField("userRolesField")));	    	
		return user;					
	}

	/**
	 * Query a Mongo Database for information about a specific user and return the 
	 * @param dbclient the {@link MongoClient} to communicate with the database 
	 * @param username the name of the user
	 * @param resultHandler the handler that handles the resulting User
	 */
	public static void getUserDataFromCollection(MongoClient dbclient, String username, Handler<Future<JsonObject>> resultHandler)
	{
		String unameField = SoileConfigLoader.getUserdbField("usernameField");
		String emailField = SoileConfigLoader.getUserdbField("userEmailField");
		JsonObject query;

		if(username.contains("@"))
		{
			//this can only be present in emails. So we only check those.
			query = new JsonObject().put(emailField, username);
		}
		else
		{
			query = new JsonObject().put(unameField, username);
		}  
		dbclient.find(SoileConfigLoader.getdbProperty("userCollection"), query, res -> {
			if(res.succeeded())
			{
				List<JsonObject> dbResultList = res.result(); 				
				if(dbResultList.size() == 1)
				{	 
					//successfully found a single entry, pass it back to the handler.					
					resultHandler.handle(Future.succeededFuture(dbResultList.get(0)));	    		    		    		    
				}
				else if(dbResultList.size() > 1)
				{
					resultHandler.handle(Future.failedFuture(new DuplicateUserEntryInDBException(username)));
				}
				else
				{
					resultHandler.handle(Future.failedFuture(new InvalidLoginException(username)));
				}   		  
			}
			else 
			{
				resultHandler.handle(Future.failedFuture(res.cause()));
			}

		});	     
	}
	
	
	/**
	 * Get a users details via the event bus.
	 * Utility function to avoid having to have the message names written everywhere 
	 *  
	 * @param vertx the {@link io.vertx.core.Vertx} instance to use to send the events 
	 * @param username the name of the user
	 * @param resultHandler the handler that handles the resulting User
	 */
	public static Future<List<UserData>> getUserData(Vertx vertx, JsonArray usernames)
	{
		Promise<List<UserData>> promise = Promise.promise();
		vertx.eventBus().request("soile.umanager.getUserDetailsForMultipleUsers", new JsonObject().put("usernames", usernames))
		.onSuccess(msg -> {
			JsonObject response = (JsonObject) msg.body();
			JsonArray userData = response.getJsonArray(SoileCommUtils.DATAFIELD);
			List<UserData> result = new LinkedList<UserData>();
			for(int i = 0 ; i < userData.size(); i++)
			{
				result.add(new UserData(userData.getJsonObject(i)));
			}
			promise.complete(result);
		});
		return promise.future();		
	}
}
