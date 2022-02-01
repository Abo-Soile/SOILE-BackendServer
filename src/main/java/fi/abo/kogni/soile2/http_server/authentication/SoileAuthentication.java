package fi.abo.kogni.soile2.http_server.authentication;

import java.util.List;
import java.util.Map;

import fi.abo.kogni.soile2.http_server.UserManagementVerticle;
import fi.abo.kogni.soile2.http_server.userManagement.SoileHashing;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.HashingStrategy;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.mongo.MongoAuthentication;
import io.vertx.ext.auth.mongo.MongoAuthenticationOptions;
import io.vertx.ext.mongo.MongoClient;

/**
 * This Verticle will handle User Authentication 
 * @author thomas
 *
 */
public class SoileAuthentication implements MongoAuthentication{

	private final MongoAuthenticationOptions authnOptions;
	private final MongoClient client;
	private final HashingStrategy strategy;	
	private final String userTypeField;
	private final String userType;
	
	public SoileAuthentication(MongoClient client, MongoAuthenticationOptions authnOptions, JsonObject userconfig, String userType)
	{
		userTypeField = userconfig.getString("userTypeField");
		this.userType = userconfig.getString(userType);
		this.authnOptions = authnOptions;
		this.client = client;
		strategy = new SoileHashing(userconfig.getString("serverSalt"));
	}
	
	
	
	@Override
	public void authenticate(JsonObject credentials, Handler<AsyncResult<User>> resultHandler) 
	{
		   try {
			   	  String unameField = authnOptions.getUsernameCredentialField();
			      //no credentials provided
			      if (credentials == null || credentials.getString(unameField) == null ) {
			    	
			        resultHandler.handle((Future.failedFuture("Invalid Credentials.")));
			        return;
			      }
			      if (credentials.getString(authnOptions.getPasswordCredentialField()) == null ||credentials.getString(authnOptions.getPasswordCredentialField()).isEmpty())
			      {
			    	  resultHandler.handle((Future.failedFuture("Invalid Password.")));
				      return;  
			      }

			      JsonObject query = new JsonObject().put(unameField, credentials.getString(unameField));
			      client.find(authnOptions.getCollectionName(), query, res -> {
			        try {
			          if (res.succeeded()) {			        	  
			            User user = getUser(res.result(), credentials);
			            System.out.println("User found and authenticated");
			            resultHandler.handle(Future.succeededFuture(user));			            
			            return;
			          } else {
			        	System.out.println("Could not find user in DB");
			            resultHandler.handle(Future.failedFuture(res.cause()));
			            return;
			          }
			        } catch (Exception e) {
			          System.out.println(e);
			          e.printStackTrace(System.out);
			          resultHandler.handle(Future.failedFuture(e));
			          return;
			        }

			      });
			    } catch (RuntimeException e) {
			      resultHandler.handle(Future.failedFuture(e));
			      return;
			    }	
	}

	public User getUser(List<JsonObject> resultList, JsonObject credentials)
		      throws Exception {
    	String username = credentials.getString(authnOptions.getUsernameCredentialField());
		if(resultList.size() == 1)
	    {
	    	JsonObject userJson = resultList.get(0);
	    	System.out.println("Trying to retrieve user for username " + username);
	    	User user = User.fromName(username);
	    	user.principal().put(userTypeField, userType);
	    	if(strategy.verify(userJson.getString(authnOptions.getPasswordCredentialField()), credentials.getString(authnOptions.getPasswordCredentialField())))
	    	{
	    		//User authenticated!!
	    		return user;
	    	}
	    	else
	    	{
	    		throw new Exception("Invalid user or wrong password for " + username);
	    	}
	    }
	    else if(resultList.size() > 1)
	    {
	    	throw new Exception("More than one user with username " + username + " in database! Usernames must be unique");	
	    }
	    else
	    {
	    	throw new Exception("Invalid user or wrong password for user " + username);
	    }

	}
	@Override
	public String hash(String id, Map<String, String> params, String salt, String password) {
		// TODO Auto-generated method stub
		return null;
	}

	
	
}
