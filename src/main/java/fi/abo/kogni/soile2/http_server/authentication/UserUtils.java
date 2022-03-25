package fi.abo.kogni.soile2.http_server.authentication;

import java.util.List;

import fi.abo.kogni.soile2.http_server.userManagement.exceptions.DuplicateUserEntryInDBException;
import fi.abo.kogni.soile2.http_server.userManagement.exceptions.InvalidLoginException;
import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;

public class UserUtils {

	/**
	 * Build a user from the database adding several properties.
	 * @param userJson the Json representing the data for the user (essentially the 
	 * @return 
	 */
	public static User buildUserFromDBEntry(List<JsonObject> dbResultList, String username) throws DuplicateUserEntryInDBException,InvalidLoginException
	{
		if(dbResultList.size() == 1)
	    {
	    	JsonObject userJson = dbResultList.get(0);	    	
	    	User user = User.fromName(userJson.getString(SoileConfigLoader.getdbField("usernameField")));
	    	// set properties of the user that are needed for session handling
	    	user.principal().put(SoileConfigLoader.getSessionProperty("userTypeField"), userJson.getValue(SoileConfigLoader.getdbField("userTypeField")));
	    	user.principal().put(SoileConfigLoader.getSessionProperty("validSessionCookies"), userJson.getValue(SoileConfigLoader.getdbField("storedSessions")));	    
	    	return user;
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
}
