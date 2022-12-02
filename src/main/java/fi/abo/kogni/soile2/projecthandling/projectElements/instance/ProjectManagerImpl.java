package fi.abo.kogni.soile2.projecthandling.projectElements.instance;

import java.util.function.Supplier;

import fi.abo.kogni.soile2.datamanagement.git.GitManager;
import fi.abo.kogni.soile2.projecthandling.apielements.APIElement;
import fi.abo.kogni.soile2.projecthandling.projectElements.ElementManager;
import fi.abo.kogni.soile2.projecthandling.projectElements.Project;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.impl.ProjectInstanceHandler;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;

public class ProjectManagerImpl extends ElementManager<Project> {

	ProjectInstanceHandler projectInstances;
	public ProjectManagerImpl(Supplier<Project> supplier, Supplier<APIElement<Project>> apisupplier, MongoClient client, GitManager manager, ProjectInstanceHandler projectInstanceHandler) {
		super(supplier, apisupplier, client, manager);
		// TODO Auto-generated constructor stub
		this.projectInstances = projectInstanceHandler;
	}

	
	public Future<String> startProject(JsonObject projectInfo )
	{
		Promise<String> idPromise = Promise.promise();
		projectInstances.createProject(projectInfo)
		.onSuccess(instance ->  {
			
		})
		.onFailure(err -> idPromise.fail(err));
		return idPromise.future();
	}
	
}
