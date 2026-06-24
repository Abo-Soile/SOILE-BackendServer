package fi.abo.kogni.soile2.http_server.authentication.utils;

import io.vertx.core.json.JsonObject;

import io.vertx.core.json.JsonArray;

/**
 * Simple helper class for User Details.
 * Avoiding having to do JsonObject field
 */
public class UserData {
	String username;
	String fullname;
	JsonArray roles;
	String email;
	
	public UserData(String username, String fullname, JsonArray roles, String email) {
		super();
		this.username = username;
		this.fullname = fullname;
		this.roles = roles;
		this.email = email;
	}
	
	public UserData(JsonObject userData)
	{
		this(userData.getString("username"), 
			 userData.getString("fullname"), 
			 userData.getJsonArray("role"), 
			 userData.getString("email"));
	}
	
	public String getUsername() {
		return username;
	}
	public String getFullname() {
		return fullname;
	}
	public JsonArray getRoles() {
		return roles;
	}
	public String getEmail() {
		return email;
	}

	
}
