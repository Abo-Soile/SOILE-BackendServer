package fi.abo.kogni.soile2.projecthandling.projectElements.instance.impl;

import fi.abo.kogni.soile2.projecthandling.projectElements.instance.ProjectInstance;
import io.vertx.core.eventbus.EventBus;
import io.vertx.ext.mongo.MongoClient;

public class JsonToDBProjectInstanceFactory extends DBProjectInstanceFactory {

	/**
	 * Default constructor
	 * @param client
	 * @param projectInstanceDB
	 */
	public JsonToDBProjectInstanceFactory(MongoClient client, String projectInstanceDB, EventBus eb) {
		super(client, projectInstanceDB, eb);
	}

	@Override
	public ProjectInstance createInstance() { 
		return new JsonToDBProjectInstance(client, projectInstanceDB, eb);
	}

}
