package fi.abo.kogni.soile2.http_server.authentication;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.authentication.CredentialValidationException;
import io.vertx.ext.auth.authentication.UsernamePasswordCredentials;

public class SoileCredentials extends UsernamePasswordCredentials{

	String userType;
	String userTypeField;
	public SoileCredentials(String username, String password, String userType, JsonObject dbConfig) {
		super(username,password);
		this.userType = userType;
		userTypeField = dbConfig.getString("userTypeField");
	}

	@Override
	public JsonObject toJson() {		
		return super.toJson().put(userTypeField, userType);
	}
	
	public void setUserType(String userType)
	{
		this.userType = userType;
	}
	
	public String getUserType()
	{
		return userType;
	}
	
	  @Override
	  public <V> void checkValid(V arg) throws CredentialValidationException {
	    super.checkValid(arg);		 
	    // passwords are allowed to be empty
	    // for example this is used by basic auth
	    if (getPassword().length() == 0) {
	      throw new CredentialValidationException("password cannot be null or empty");
	    }
	    if (userType == null || ( !userType.equals("user") && !userType.equals("participant"))) {
		      throw new CredentialValidationException("usertype must be either participant or user. Was : " + userType);
		    }
	  }
}
