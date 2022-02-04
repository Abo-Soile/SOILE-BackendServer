package fi.abo.kogni.soile2.http_server.authentication;

import fi.abo.kogni.soile2.http_server.utils.SoileConfigLoader;
import io.vertx.core.json.JsonObject;

public class SoileAuthenticationOptions{

	private JsonObject collections;
    private String usernameField;
	private String passwordField;
    private String collectionsField;
    
	public SoileAuthenticationOptions() {
	    usernameField = "username";
	    passwordField = "password";
	  }
	
	public SoileAuthenticationOptions(JsonObject config)
	{
		this();		
		collections = config.getJsonObject("userCollections");
		usernameField = config.getJsonObject(SoileConfigLoader.DB_FIELDS).getString("usernameField");
		passwordField = config.getJsonObject(SoileConfigLoader.DB_FIELDS).getString("passwordField");
		collectionsField = config.getJsonObject(SoileConfigLoader.DB_FIELDS).getString("userTypeField");
	}
	
	public String getCollectionForType(String type) {
		return collections.getString(type);
	}	


	  public String getUsernameField() {
	    return usernameField;
	  }


	  public String getPasswordField() {
	    return passwordField;
	  }
	  
	  public String getCollectionsField() {
		    return collectionsField;
		  }
}
