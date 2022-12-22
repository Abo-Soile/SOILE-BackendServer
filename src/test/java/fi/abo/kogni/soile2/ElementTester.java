package fi.abo.kogni.soile2;

import fi.abo.kogni.soile2.http_server.SoileVerticleTest;
import fi.abo.kogni.soile2.projecthandling.apielements.APIExperiment;
import fi.abo.kogni.soile2.projecthandling.apielements.APIProject;
import fi.abo.kogni.soile2.projecthandling.apielements.APITask;
import fi.abo.kogni.soile2.projecthandling.projectElements.ElementManager;
import fi.abo.kogni.soile2.projecthandling.projectElements.Experiment;
import fi.abo.kogni.soile2.projecthandling.projectElements.Project;
import fi.abo.kogni.soile2.projecthandling.projectElements.Task;
import fi.abo.kogni.soile2.projecthandling.utils.ObjectGenerator;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;

public abstract class ElementTester extends GitTest {
	ElementManager<Project> projManager;
	ElementManager<Experiment> expManager;
	ElementManager<Task> taskManager;
	
	@Override
	public void runBeforeTests(TestContext context)
	{
		super.runBeforeTests(context);	
		projManager = new ElementManager<>(Project::new, APIProject::new, mongo_client, gitManager);
		expManager = new ElementManager<>(Experiment::new, APIExperiment::new, mongo_client, gitManager);		 
		taskManager = new ElementManager<>(Task::new, APITask::new, mongo_client, gitManager);
	}
	
	public Future<Project> createProject(TestContext context)
	{
	Promise<Project> projectPromise = Promise.promise(); 
	Async APIProjectCreationAsync = context.async();		 
	ObjectGenerator.buildAPIProject(projManager, expManager, taskManager, mongo_client, "Testproject")
	.onSuccess(existingApiProject -> 
	{			
		Async projectAsync = context.async();
		projManager.getElement(existingApiProject.getUUID())
		.onSuccess(project -> {
			projectPromise.complete(project);
			projectAsync.complete();
		})
		.onFailure(err -> context.fail(err));
		APIProjectCreationAsync.complete();
	})
	.onFailure(err -> context.fail(err));
	return projectPromise.future();
	}
		
	public static JsonObject getCreationJson(Project project, String instanceName, String shortcut)
	{
		return new JsonObject()
				.put("sourceUUID", project.getUUID())
				.put("name", instanceName)
				// this works here, since no other version could be added in between.
				.put("version", project.getCurrentVersion())
				.put("private", true)
				.put("shortcut", shortcut);		
	}
				
		
}
