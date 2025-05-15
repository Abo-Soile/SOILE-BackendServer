package fi.abo.kogni.soile2.http_server.auth;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fi.abo.kogni.soile2.http_server.authentication.utils.UserUtils;
import fi.abo.kogni.soile2.http_server.userManagement.SoileHashing;
import fi.abo.kogni.soile2.http_server.userManagement.exceptions.DuplicateUserEntryInDBException;
import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.HashingStrategy;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.authentication.AuthenticationProvider;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.handler.HttpException;

/**
 * SoileAuthentication will handle the authentication of clients. 
 * @author thomas
 *
 */
public class SoileAuthentication implements AuthenticationProvider{

	private final MongoClient client;
	private final HashingStrategy strategy;

	static final Logger LOGGER = LogManager.getLogger(SoileAuthentication.class);
	/**
	 * Default constructor
	 * @param client {@link MongoClient} for authentication db access
	 */
	public SoileAuthentication(MongoClient client)
	{		
		this.client = client;
		strategy = new SoileHashing(SoileConfigLoader.getUserProperty("serverSalt"));
	}



	@Override
	public void authenticate(JsonObject credentials, Handler<AsyncResult<User>> resultHandler) 
	{
		// Authentication based on credentials provided (username/password)
		LOGGER.debug("Trying to authenticate");
		try {
			//no credentials provided
			if (credentials == null || credentials.getString("username") == null ) {

				resultHandler.handle((Future.failedFuture("Invalid Credentials.")));
				return;
			}
			if (credentials.getString(SoileConfigLoader.getUserdbField("passwordField")) == null 
					||credentials.getString(SoileConfigLoader.getUserdbField("passwordField")).isEmpty())

			{
				resultHandler.handle((Future.failedFuture("Invalid Password.")));
				return;  
			}
			LOGGER.debug("requesting user entry from database");
			String username = credentials.getString("username");
			UserUtils.getUserDataFromCollection(client, username , res ->
			{
				if(res.succeeded())
				{
					try
					{
						LOGGER.debug("Found user " + username + " , requesting handling");			    			
						User user = getUser(res.result(),credentials);
						resultHandler.handle(Future.succeededFuture(user));
					}
					catch(HttpException e)
					{
						LOGGER.error("Got an error while handling: " +e.getMessage());
						LOGGER.debug(resultHandler);			    			
						resultHandler.handle(Future.failedFuture(e));
					}
				}			    	
				else
				{
					if(res.cause() instanceof DuplicateUserEntryInDBException)
					{			    			
						resultHandler.handle(Future.failedFuture("Internal Server Error"));
					}
					else
					{
						resultHandler.handle(Future.failedFuture(res.cause()));	
					}
				}
			});

		} catch (RuntimeException e) {
			LOGGER.error("Got an error while handling: " +e.getMessage());
			resultHandler.handle(Future.failedFuture(e));
			return;
		}	
	}

	private User getUser(JsonObject dbEntry, JsonObject credentials)
			throws HttpException {
		String username = credentials.getString(SoileConfigLoader.getUserdbField("usernameField"));
		User user = UserUtils.buildUserForDBEntry(dbEntry,username);		
		if(strategy.verify(dbEntry.getString(SoileConfigLoader.getUserdbField("passwordField")),
				credentials.getString(SoileConfigLoader.getSessionProperty("passwordField"))))
		{
			//User authenticated!!
			LOGGER.debug("Successfully validated the user");
			return user;
		}
		else
		{
			LOGGER.debug("Could not validate user with the following Credentials:\n " + credentials.encodePrettily());
			throw new HttpException(401, "Invalid username or invalid password for " + username);
		}
	}		

}
