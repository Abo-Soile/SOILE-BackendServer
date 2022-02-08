package fi.abo.kogni.soile2.http_server.authentication;

import java.util.List;

import org.apache.commons.collections4.functors.InstanceofPredicate;

import fi.abo.kogni.soile2.http_server.userManagement.SoileHashing;
import fi.abo.kogni.soile2.http_server.userManagement.exceptions.DuplicateUserEntryInDBException;
import fi.abo.kogni.soile2.http_server.userManagement.exceptions.InvalidLoginException;
import fi.abo.kogni.soile2.http_server.utils.SoileCommUtils;
import fi.abo.kogni.soile2.http_server.utils.SoileConfigLoader;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.HashingStrategy;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.authentication.AuthenticationProvider;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.HttpException;

/**
 * This Verticle will handle User Authentication 
 * @author thomas
 *
 */
public class SoileAuthentication implements AuthenticationProvider{

	private final SoileAuthenticationOptions authnOptions;
	private final MongoClient client;
	private final HashingStrategy strategy;
	private JsonObject userCf;
	private JsonObject commConfig;
	
	public SoileAuthentication(MongoClient client, SoileAuthenticationOptions authnOptions, JsonObject config)
	{
		userCf = config.getJsonObject(SoileConfigLoader.USERMGR_CFG);
		commConfig = config.getJsonObject(SoileConfigLoader.COMMUNICATION_CFG);
		this.authnOptions =  authnOptions;
		this.client = client;
		strategy = new SoileHashing(userCf.getString("serverSalt"));
	}
	
	
	
	@Override
	public void authenticate(JsonObject credentials, Handler<AsyncResult<User>> resultHandler) 
	{
		   try {
			   	  String unameField = authnOptions.getUsernameField();
			      //no credentials provided
			      if (credentials == null || credentials.getString(unameField) == null ) {
			    	
			        resultHandler.handle((Future.failedFuture("Invalid Credentials.")));
			        return;
			      }
			      if (credentials.getString(authnOptions.getPasswordField()) == null 
			    	  ||credentials.getString(authnOptions.getPasswordField()).isEmpty())
			    	  
			      {
			    	  resultHandler.handle((Future.failedFuture("Invalid Password.")));
				      return;  
			      }

			      JsonObject query = new JsonObject().put(unameField, credentials.getString(authnOptions.getUsernameField()));
			      String userType = credentials.getString(SoileCommUtils.getCommunicationField(commConfig, "userTypeField"));
			      client.find(authnOptions.getCollectionForType(userType), query, res -> {
			        try {
			          if (res.succeeded()) {			        	  
			            User user = getUser(res.result(), credentials);
			            user.principal().put(SoileCommUtils.getCommunicationField(commConfig, "userTypeField"), userType);
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
			          e.printStackTrace(System.out);
			          System.out.println(resultHandler.getClass());
			          if(e instanceof InvalidLoginException)
			          {
			        	  resultHandler.handle(Future.failedFuture(new HttpException(302,"/login", e)));
			          }
			          else
			          {
			        	  //this is an internal server Error...
			        	  resultHandler.handle(Future.failedFuture(e));  
			          }			          
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
    	String username = credentials.getString(authnOptions.getUsernameField());
		if(resultList.size() == 1)
	    {
	    	JsonObject userJson = resultList.get(0);
	    	System.out.println("Trying to retrieve user for username " + username);
	    	User user = User.fromName(username);
	    	//user.principal().put(sessionCfg.getString("userTypeField"), userType);
	    	if(strategy.verify(userJson.getString(authnOptions.getPasswordField()),
	    			credentials.getString(authnOptions.getPasswordField())))
	    	{
	    		//User authenticated!!
	    		return user;
	    	}
	    	else
	    	{
	    		throw new InvalidLoginException(username);
	    	}
	    }
	    else if(resultList.size() > 1)
	    {
	    	throw new DuplicateUserEntryInDBException(username);	
	    }
	    else
	    {
	    	throw new InvalidLoginException(username);
	    }

	}



	
	
}
