package fi.abo.kogni.soile2.projecthandling.verticles;

import fi.abo.kogni.soile2.projecthandling.projectElements.instance.impl.ProjectInstanceHandler;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.impl.ProjectInstanceManager;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;

public class ProjectInstanceVerticle extends AbstractVerticle {

	ProjectInstanceHandler instHandler;
	ProjectInstanceManager instManager;
	
	
	/**
	 * Start a project with the given project UUID as a new Project instance.
	 * @param UUID
	 * @return
	 */
	private Future<String> start(String UUID, String version)
	{
		Promise<String> idPromise = Promise.promise();
		// TODO: Implement
		return idPromise.future();
	}
	
}
