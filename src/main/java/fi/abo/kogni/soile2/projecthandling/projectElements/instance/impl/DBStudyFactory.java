package fi.abo.kogni.soile2.projecthandling.projectElements.instance.impl;

import fi.abo.kogni.soile2.projecthandling.projectElements.impl.ElementManager;
import fi.abo.kogni.soile2.projecthandling.projectElements.impl.Project;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.Study;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.StudyFactory;
import io.vertx.core.eventbus.EventBus;
import io.vertx.ext.mongo.MongoClient;

/**
 * Factory for Database based project instances.
 * @author Thomas Pfau
 *
 */
public class DBStudyFactory implements StudyFactory {

	/**
	 * MongoClient
	 */
	protected MongoClient client;
	/**
	 * The project instance database name (i.e. study database)
	 */
	protected String projectInstanceDB;
	/**
	 * The eventbus used for communication
	 */
	protected EventBus eb;
	/**
	 * The {@link ElementManager} for Projects
	 */
	protected ElementManager<Project> projManager;
	/**
	 * Default constructor
	 * @param manager {@link ElementManager} for Project access
	 * @param client {@link MongoClient} for db access
	 * @param eb {@link EventBus} for communication
	 */
	public DBStudyFactory(ElementManager<Project> manager, MongoClient client, EventBus eb) {
		super();
		this.client = client;
		this.eb = eb;
		projManager= manager;
	}

	@Override
	public Study createInstance() {
		return new DBStudy(projManager, client, eb);
	}

}
