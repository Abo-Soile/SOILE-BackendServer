package fi.abo.kogni.soile2.http_server.authentication;

import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.vertx.core.json.JsonObject;

public class SoileAuthenticationOptions{

	private String collectionName;
    private String usernameField;
	private String passwordField;    
	public SoileAuthenticationOptions() {
	    usernameField = "username";
	    passwordField = "password";
	  }
	
	public SoileAuthenticationOptions(JsonObject config)
	{
		this();
		
		usernameField = SoileConfigLoader.getdbField("usernameField");
		passwordField = SoileConfigLoader.getdbField("passwordField");
	}
	public String getCollectionName()
	{
		return collectionName;
	}

	public String getUsernameField() {
		return usernameField;
	}


	public String getPasswordField() {
		return passwordField;
	}
}
