package fi.abo.kogni.soile2.projecthandling.projectElements.instance;

import org.junit.Test;

import fi.abo.kogni.soile2.ElementTester;
import fi.abo.kogni.soile2.GitTest;
import fi.abo.kogni.soile2.projecthandling.apielements.APIExperiment;
import fi.abo.kogni.soile2.projecthandling.apielements.APIProject;
import fi.abo.kogni.soile2.projecthandling.apielements.APITask;
import fi.abo.kogni.soile2.projecthandling.participant.ParticipantHandler;
import fi.abo.kogni.soile2.projecthandling.projectElements.ElementManager;
import fi.abo.kogni.soile2.projecthandling.projectElements.Experiment;
import fi.abo.kogni.soile2.projecthandling.projectElements.Project;
import fi.abo.kogni.soile2.projecthandling.projectElements.Task;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.impl.ProjectInstanceHandler;
import fi.abo.kogni.soile2.projecthandling.utils.ObjectGenerator;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;

public class DBProjectTest extends GitTest{



	@Test
	public void testProjectCreation(TestContext context)
	{		
		ElementManager<Project> projManager = new ElementManager<>(Project::new, APIProject::new, mongo_client, gitManager);
		ElementManager<Experiment> expManager = new ElementManager<>(Experiment::new, APIExperiment::new, mongo_client, gitManager);		 
		ElementManager<Task> taskManager = new ElementManager<>(Task::new, APITask::new, mongo_client, gitManager);
		Async APIProjectCreationAsync = context.async();		 
		ProjectInstanceHandler projInstHandler = new ProjectInstanceHandler(gitDataLakeDir, mongo_client, vertx.eventBus());		 

		ObjectGenerator.buildAPIProject(projManager, expManager, taskManager, mongo_client, "Testproject")
		.onSuccess(existingApiProject -> 
		{	
			System.out.println("API Project created");
			Async projectAsync= context.async();
			projManager.getElement(existingApiProject.getUUID())
			.onSuccess(project -> {
				System.out.println("Project created" + project.toJson());				
				Async projInstAsync = context.async();				
				projInstHandler.createProjectInstance(ElementTester.getCreationJson(project, "StartedProject1","thisIsSomeShortcut"))
				.onSuccess(projectInstance -> {					
					System.out.println("Instantiated Project");
					System.out.println(projectInstance.toDBJson().encodePrettily());
					Async partListAsync = context.async();
					projectInstance.getParticipants()
					.onSuccess(parts -> {
						context.assertEquals(0, parts.size());
						partListAsync.complete();
					})
					.onFailure(err -> context.fail(err));				
					context.assertEquals(10,projectInstance.getElements().size());				
					projInstAsync.complete();
				})
				.onFailure(err -> context.fail(err));
				projectAsync.complete();
			})
			.onFailure(err -> context.fail(err));
			APIProjectCreationAsync.complete();
		})
		.onFailure(err -> context.fail(err));
	}



	@Test
	public void testProgression(TestContext context)
	{
		ElementManager<Project> projManager = new ElementManager<>(Project::new, APIProject::new, mongo_client, gitManager);
		ElementManager<Experiment> expManager = new ElementManager<>(Experiment::new, APIExperiment::new, mongo_client, gitManager);		 
		ElementManager<Task> taskManager = new ElementManager<>(Task::new, APITask::new, mongo_client, gitManager);
		Async APIProjectCreationAsync = context.async();		 
		ProjectInstanceHandler projInstHandler = new ProjectInstanceHandler(gitDataLakeDir, mongo_client, vertx.eventBus());		 
		ParticipantHandler partHandler = new ParticipantHandler(mongo_client, projInstHandler, vertx);
		ObjectGenerator.buildAPIProject(projManager, expManager, taskManager, mongo_client, "Testproject")
		.onSuccess(existingApiProject -> 
		{	
			System.out.println("API Project created");
			Async projectAsync= context.async();
			projManager.getElement(existingApiProject.getUUID())
			.onSuccess(project -> {
				System.out.println("Project created" + project.toJson());				
				Async projInstAsync = context.async();
				JsonObject creationJson = new JsonObject()
						.put("sourceUUID", existingApiProject.getUUID())
						.put("name", "StartedProject1")
						// this works here, since no other version could be added in between.
						.put("version", project.getCurrentVersion())
						.put("private", true)
						.put("shortcut", "thisIsSomeShortcut");							
				projInstHandler.createProjectInstance(creationJson)
				.onSuccess(projectInstance -> {	
					partHandler.create(projectInstance).onSuccess(participant -> {
						
					});
					System.out.println("Instantiated Project");
					System.out.println(projectInstance.toDBJson().encodePrettily());
					Async partListAsync = context.async();
					projectInstance.getParticipants()
					.onSuccess(parts -> {
						context.assertEquals(0, parts.size());
						partListAsync.complete();
					})
					.onFailure(err -> context.fail(err));				
					context.assertEquals(10,projectInstance.getElements().size());				
					projInstAsync.complete();
				})
				.onFailure(err -> context.fail(err));
				projectAsync.complete();
			})
			.onFailure(err -> context.fail(err));
			APIProjectCreationAsync.complete();
		})
		.onFailure(err -> context.fail(err));
	}
	/*
	 @Test
	 public void testUpdate(TestContext context)
	 {
		Async projectAsync = context.async();
		manager.createElement("NewProject").onSuccess(project -> {
			Async updateAsync = context.async();
			APIProject project = new APIProject(null)
			manager.updateElement(null)
			projectAsync.complete();
		}).
		onFailure(err ->{
			context.fail(err);
		});		
	 }*/
}
