package fi.abo.kogni.soile2.project.instance.impl;

import fi.abo.kogni.soile2.project.instance.ProjectInstance;
import io.vertx.ext.mongo.MongoClient;

public class JsonToDBProjectFactory extends DBProjectFactory {

	/**
	 * Default constructor
	 * @param client
	 * @param projectInstanceDB
	 */
	public JsonToDBProjectFactory(MongoClient client, String projectInstanceDB) {
		super(client, projectInstanceDB);
	}

	@Override
	public ProjectInstance createInstance() { 
		return new JsonToDBProject(client, projectInstanceDB);
	}

}
