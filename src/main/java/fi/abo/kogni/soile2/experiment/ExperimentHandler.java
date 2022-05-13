package fi.abo.kogni.soile2.experiment;

import io.vertx.ext.mongo.MongoClient;

public class ExperimentHandler {

	final MongoClient client;
	
	/**
	 * Create a handler for experiment database interactions.
	 * @param client
	 */
	public ExperimentHandler(MongoClient client)
	{
		this.client = client;
	}
	
}
