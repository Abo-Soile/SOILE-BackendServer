package fi.abo.kogni.soile2.projecthandling.projectElements.instance.impl;

import fi.abo.kogni.soile2.projecthandling.projectElements.impl.ElementManager;
import fi.abo.kogni.soile2.projecthandling.projectElements.impl.Project;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.Study;
import io.vertx.core.eventbus.EventBus;
import io.vertx.ext.mongo.MongoClient;

/**
 * Factory for the creation of a Project instance that is not yet in the database. 
 * @author Thomas Pfau
 *
 */
public class ElementToDBStudyFactory extends DBStudyFactory {

	/**
	 * Default constructor
	 * @param client
	 * @param projectInstanceDB
	 */
	public ElementToDBStudyFactory(ElementManager<Project> manager, MongoClient client, EventBus eb) {
		super(manager, client, eb);
	}

	@Override
	public Study createInstance() { 		
		return new ElementToDBStudy(projManager, client, eb);
	}

}
