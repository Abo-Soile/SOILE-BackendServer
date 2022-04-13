package fi.abo.kogni.soile2.http_server.authentication;

import java.util.List;

import fi.abo.kogni.soile2.http_server.userManagement.exceptions.DuplicateUserEntryInDBException;
import fi.abo.kogni.soile2.http_server.userManagement.exceptions.InvalidLoginException;
import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.authorization.AuthorizationProvider;
import io.vertx.ext.mongo.MongoClient;

public class UserUtils {
			
	/**
	 * Build a user from the database result adding several properties.
	 * This fails if multiple or no entry were returned.
	 * @param userJson the Json representing the data for the user (essentially the 
	 * @return 
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
	 * Build a user from an individual database entry.
	 * @param userJson the Json representing the data for the user ( 
	 * @return the generated user with all necessary properties set.
	 */
	public static User buildUserForDBEntry(JsonObject userJson, String username)
	{
	    	User user = User.fromName(userJson.getString(SoileConfigLoader.getdbField("usernameField")));
	    	// set properties of the user that are needed for session handling
	    	user.principal().put(SoileConfigLoader.getSessionProperty("userTypeField"), userJson.getValue(SoileConfigLoader.getdbField("userTypeField")));
	    	user.principal().put(SoileConfigLoader.getSessionProperty("validSessionCookies"), userJson.getValue(SoileConfigLoader.getdbField("storedSessions")));
	    	AuthorizationProvider prov = AuthorizationProvider.create("this", null);	    	
	    	return user;					
	}
	
	/**
	 * Query a Mongo Database for information about a specific user and return the 
	 * @param dbclient
	 * @param username
	 * @param resultHandler
	 */
	public static void getUserDataFromCollection(MongoClient dbclient, String username, Handler<Future<JsonObject>> resultHandler)
	{
	   	  String unameField = SoileConfigLoader.getdbField("usernameField");
	   	  String emailField = SoileConfigLoader.getdbField("emailField");
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
	    	  
	      });	     
	}
}
