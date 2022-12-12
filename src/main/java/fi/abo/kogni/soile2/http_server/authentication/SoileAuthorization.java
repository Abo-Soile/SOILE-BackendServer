package fi.abo.kogni.soile2.http_server.authentication;

import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.vertx.ext.auth.mongo.MongoAuthorization;
import io.vertx.ext.mongo.MongoClient;

public class SoileAuthorization{

	MongoAuthorization taskAuthorization;
	MongoAuthorization projectAuthorization;
	MongoAuthorization experimentAuthorization;
	MongoAuthorization instanceAuthorization;	
	
	public SoileAuthorization(MongoClient client)
	{
		taskAuthorization = MongoAuthorization.create("taskProvider", client, SoileConfigLoader.getMongoTaskAuthorizationOptions());
		experimentAuthorization = MongoAuthorization.create("experimentProvider", client, SoileConfigLoader.getMongoExperimentAuthorizationOptions());
		projectAuthorization = MongoAuthorization.create("projectProvider", client, SoileConfigLoader.getMongoProjectAuthorizationOptions());
		instanceAuthorization = MongoAuthorization.create("instanceProvider", client, SoileConfigLoader.getMongoInstanceAuthorizationOptions());		
	}
	
	
	
}
