package fi.abo.kogni.soile2.http_server.authentication;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.mongo.MongoAuthorization;
import io.vertx.ext.auth.mongo.MongoAuthorizationOptions;
import io.vertx.ext.mongo.MongoClient;

/**
 * This class represents an authorization to experiment data based on types of users. e.g. participants will be matched on whether they 
 * have access permissions to the experiment, while for researchers access will be checked on whether they are part of a group for a restricted experiment,
 * or whether they are researchers for unrestricted experiments. 
 */
public class SoileAuthorization implements MongoAuthorization {

	
	final MongoAuthorization defaultAuth;
	final String userType;
	
	public SoileAuthorization(MongoClient client, MongoAuthorizationOptions opts, String userType)	
	{
		this.userType = userType;
		this.defaultAuth = MongoAuthorization.create(userType, client, opts);
	}
	
	
	@Override
	public void getAuthorizations(User user, Handler<AsyncResult<Void>> handler) {
			
		if(userType.equals(user.principal().getString("userType")))
		{
			defaultAuth.getAuthorizations(user, handler);
		}
		else
		{
			// we don't add anything to the user as it is not of the right type.
			handler.handle(Future.succeededFuture());
		}					
	}

	@Override
	public String getId() {
		return defaultAuth.getId();
	}

}
