package fi.abo.kogni.soile2;

import fi.abo.kogni.soile2.projecthandling.apielements.APIExperiment;
import fi.abo.kogni.soile2.projecthandling.apielements.APIProject;
import fi.abo.kogni.soile2.projecthandling.apielements.APITask;
import fi.abo.kogni.soile2.projecthandling.projectElements.impl.ElementManager;
import fi.abo.kogni.soile2.projecthandling.projectElements.impl.Experiment;
import fi.abo.kogni.soile2.projecthandling.projectElements.impl.Project;
import fi.abo.kogni.soile2.projecthandling.projectElements.impl.Task;
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
		projManager = new ElementManager<>(Project::new, APIProject::new, mongo_client, vertx);
		expManager = new ElementManager<>(Experiment::new, APIExperiment::new, mongo_client, vertx);		 
		taskManager = new ElementManager<>(Task::new, APITask::new, mongo_client, vertx);
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
				.put("sourceProject", new JsonObject()
										.put("UUID", project.getUUID())
										.put("version", project.getCurrentVersion()))
				.put("name", instanceName)
				// this works here, since no other version could be added in between.				
				.put("private", true)
				.put("shortcut", shortcut);		
	}
				
		
}
