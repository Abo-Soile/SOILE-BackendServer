package fi.abo.kogni.soile2.projecthandling.projectElements.instance.impl;

import fi.abo.kogni.soile2.datamanagement.git.GitManager;
import fi.abo.kogni.soile2.projecthandling.projectElements.ElementManager;
import fi.abo.kogni.soile2.projecthandling.projectElements.Project;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.ProjectInstance;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.ProjectInstanceFactory;
import io.vertx.core.eventbus.EventBus;
import io.vertx.ext.mongo.MongoClient;

public class DBProjectInstanceFactory implements ProjectInstanceFactory {

	protected MongoClient client;
	protected String projectInstanceDB;
	protected EventBus eb;
	protected ElementManager<Project> projManager;
	/**
	 * Default constructor
	 * @param client
	 * @param projectInstanceDB
	 */
	public DBProjectInstanceFactory(ElementManager<Project> manager, MongoClient client, EventBus eb) {
		super();
		this.client = client;
		this.eb = eb;
		projManager= manager;
	}

	@Override
	public ProjectInstance createInstance() {
		return new DBProjectInstance(projManager, client, eb);
	}

}
