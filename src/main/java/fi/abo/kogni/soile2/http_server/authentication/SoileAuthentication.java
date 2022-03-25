package fi.abo.kogni.soile2.http_server.authentication;

import java.util.List;

import fi.abo.kogni.soile2.http_server.userManagement.SoileHashing;
import fi.abo.kogni.soile2.http_server.userManagement.exceptions.DuplicateUserEntryInDBException;
import fi.abo.kogni.soile2.http_server.userManagement.exceptions.InvalidLoginException;
import fi.abo.kogni.soile2.utils.SoileCommUtils;
import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.HashingStrategy;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.authentication.AuthenticationProvider;
import io.vertx.ext.mongo.MongoClient;

/**
 * This Verticle will handle User Authentication 
 * @author thomas
 *
 */
public class SoileAuthentication implements AuthenticationProvider{

	private final MongoClient client;
	private final HashingStrategy strategy;
	
	public SoileAuthentication(MongoClient client)
	{		
		this.client = client;
		strategy = new SoileHashing(SoileConfigLoader.getUserProperty("serverSalt"));
	}
	
	
	
	@Override
	public void authenticate(JsonObject credentials, Handler<AsyncResult<User>> resultHandler) 
	{
		System.out.println("Trying to authenticate");
		   try {
			   	  String unameField = SoileConfigLoader.getdbField("usernameField");
			      //no credentials provided
			      if (credentials == null || credentials.getString(unameField) == null ) {
			    	
			        resultHandler.handle((Future.failedFuture("Invalid Credentials.")));
			        return;
			      }
			      if (credentials.getString(SoileConfigLoader.getdbField("passwordField")) == null 
			    	  ||credentials.getString(SoileConfigLoader.getdbField("passwordField")).isEmpty())
			    	  
			      {
			    	  resultHandler.handle((Future.failedFuture("Invalid Password.")));
				      return;  
			      }

			      JsonObject query = new JsonObject().put(unameField, credentials.getString(unameField));
			      client.find(SoileConfigLoader.getdbProperty("userCollection"), query, res -> {
			        try {
			          if (res.succeeded()) {			        	  
			            User user = getUser(res.result(), credentials);
			            //System.out.println("User found and authenticated");
			            resultHandler.handle(Future.succeededFuture(user));			            
			            return;
			          } else {
			        	//System.out.println("Could not find user in DB");
			            resultHandler.handle(Future.failedFuture(res.cause()));
			            return;
			          }
			        } catch (Exception e) {
			          //System.out.println(e);
			          //e.printStackTrace(System.out);
			          //System.out.println(resultHandler.getClass());
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
    	String username = credentials.getString(SoileConfigLoader.getdbField("usernameField"));
		User user = UserUtils.buildUserFromDBEntry(resultList,username);
	    //user.principal().put(sessionCfg.getString("userTypeField"), userType);
	    if(strategy.verify(credentials.getString(SoileConfigLoader.getdbField("passwordField")),
	    	credentials.getString(SoileConfigLoader.getdbField("passwordField"))))
	    {
	    	//User authenticated!!
	    	return user;
	    }
	    else
	    {
	    	throw new InvalidLoginException(username);
	    }

	}	
}
