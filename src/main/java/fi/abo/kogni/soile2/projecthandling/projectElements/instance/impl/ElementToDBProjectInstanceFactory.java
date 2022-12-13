package fi.abo.kogni.soile2.projecthandling.projectElements.instance.impl;

import fi.abo.kogni.soile2.projecthandling.projectElements.ElementManager;
import fi.abo.kogni.soile2.projecthandling.projectElements.Project;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.ProjectInstance;
import io.vertx.core.eventbus.EventBus;
import io.vertx.ext.mongo.MongoClient;

public class ElementToDBProjectInstanceFactory extends DBProjectInstanceFactory {

	/**
	 * Default constructor
	 * @param client
	 * @param projectInstanceDB
	 */
	public ElementToDBProjectInstanceFactory(ElementManager<Project> manager, MongoClient client, EventBus eb) {
		super(manager, client, eb);
	}

	@Override
	public ProjectInstance createInstance() { 		
		return new ElementToDBProjectInstance(projManager, client, eb);
	}

}
