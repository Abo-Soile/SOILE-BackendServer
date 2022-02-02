package fi.abo.kogni.soile2.http_server.authentication;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.mongo.MongoAuthenticationOptions;

public class SoileAuthenticationOptions extends MongoAuthenticationOptions {

	private String userType;

	public SoileAuthenticationOptions(JsonObject config)
	{
		super(config);
		userType = config.getString("authUserType");
	}
	
	public String getUserType() {
		return userType;
	}

	public SoileAuthenticationOptions setUserType(String userType) {
		this.userType = userType;
		return this;
	}
	
}
