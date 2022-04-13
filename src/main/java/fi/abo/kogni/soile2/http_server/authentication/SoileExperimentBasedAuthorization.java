package fi.abo.kogni.soile2.http_server.authentication;

import java.util.Objects;
import java.util.function.BiConsumer;

import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.authorization.Authorization;
import io.vertx.ext.auth.authorization.AuthorizationContext;
import io.vertx.ext.auth.authorization.RoleBasedAuthorization;
import io.vertx.ext.auth.authorization.impl.RoleBasedAuthorizationImpl;
import io.vertx.ext.auth.mongo.impl.MongoAuthorizationImpl;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.RoutingContext;

public class SoileExperimentBasedAuthorization implements Authorization, BiConsumer<RoutingContext, AuthorizationContext>{
	
	public static final int STATIC_DATA = 0;
	public static final int SOURCE_DATA = 1;
	public static final int RESULT_DATA = 2;
	public static final int EXECUTION = 3;
	public static final int EXPERIMENT_ACCESS = 4;
	
	public static String Owner = "Owner:";
	public static String Researcher = "Owner:";
	public static String Collaborator = "Owner:";
	public static String Participant = "Owner:";
	
	
	private final MongoClient client; 
	private final int target;
	public SoileExperimentBasedAuthorization(MongoClient client , int target) {
		// TODO Auto-generated constructor stub
		this.client = client;
		this.target = target; 
	}
	
	
	@Override
	public boolean match(AuthorizationContext context) {
		String experimentID = context.variables().get(SoileConfigLoader.getExperimentProperty("IDField"));		
		Objects.requireNonNull(context);

	    User user = context.user();
	    if (user != null) {
	      
	      Authorization resolvedAuthorization = getResolvedAuthorization(context);
	      for (String providerId: user.authorizations().getProviderIds()) {
	        for (Authorization authorization : user.authorizations().get(providerId)) {
	          if (authorization.verify(resolvedAuthorization)) {
	            return true;
	          }
	        }
	      }
	    }
	    return false;
		return false;
	}

	@Override
	public boolean verify(Authorization authorization) {
		// TODO Auto-generated method stub
		return false;
	}


	@Override
	public void accept(RoutingContext ctx, AuthorizationContext auc) {
		// TODO Auto-generated method stub
		auc.variables().add(SoileConfigLoader.getExperimentProperty("IDField"),ctx.pathParam("id"));
		//ctx.request().
		MongoAuthorizationImpl
		
	}
	
	public Authorization getValidAuthorizations(String requestType, String experimentID)
	{
		if(requestType.equals("GET"))
		{
			switch(target)
			{
				case STATIC_DATA:
					return 
			}
		}
		
		
	}

	public Authorization getAuthorizationForRoles(String[] )
	
}
