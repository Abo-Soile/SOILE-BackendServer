package fi.abo.kogni.soile2.projecthandling.projectElements.instance;

import org.junit.Test;

import fi.abo.kogni.soile2.ElementTester;
import fi.abo.kogni.soile2.GitTest;
import fi.abo.kogni.soile2.projecthandling.apielements.APIExperiment;
import fi.abo.kogni.soile2.projecthandling.apielements.APIProject;
import fi.abo.kogni.soile2.projecthandling.apielements.APITask;
import fi.abo.kogni.soile2.projecthandling.participant.ParticipantHandler;
import fi.abo.kogni.soile2.projecthandling.participant.impl.ElementManager;
import fi.abo.kogni.soile2.projecthandling.projectElements.impl.Experiment;
import fi.abo.kogni.soile2.projecthandling.projectElements.impl.Project;
import fi.abo.kogni.soile2.projecthandling.projectElements.impl.Task;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.impl.ProjectInstanceHandler;
import fi.abo.kogni.soile2.projecthandling.utils.ObjectGenerator;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;

public class DBProjectTest extends GitTest{



	@Test
	public void testProjectCreation(TestContext context)
	{		
		System.out.println("--------------------  Testing DB Project Creation ----------------------");
		ElementManager<Project> projManager = new ElementManager<>(Project::new, APIProject::new, mongo_client, eb);
		ElementManager<Experiment> expManager = new ElementManager<>(Experiment::new, APIExperiment::new, mongo_client, eb);		 
		ElementManager<Task> taskManager = new ElementManager<>(Task::new, APITask::new, mongo_client, eb);
		Async APIProjectCreationAsync = context.async();		 
		ProjectInstanceHandler projInstHandler = new ProjectInstanceHandler(mongo_client, vertx.eventBus());		 

		ObjectGenerator.buildAPIProject(projManager, expManager, taskManager, mongo_client, "Testproject")
		.onSuccess(existingApiProject -> 
		{	
			Async projectAsync= context.async();
			projManager.getElement(existingApiProject.getUUID())
			.onSuccess(project -> {
				Async projInstAsync = context.async();				
				projInstHandler.createProjectInstance(ElementTester.getCreationJson(project, "StartedProject1","thisIsSomeShortcut"))
				.onSuccess(projectInstance -> {					
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
		System.out.println("--------------------  Testing DB Project progression ----------------------");
		ElementManager<Project> projManager = new ElementManager<>(Project::new, APIProject::new, mongo_client, eb);
		ElementManager<Experiment> expManager = new ElementManager<>(Experiment::new, APIExperiment::new, mongo_client, eb);		 
		ElementManager<Task> taskManager = new ElementManager<>(Task::new, APITask::new, mongo_client, eb);
		Async APIProjectCreationAsync = context.async();		 
		ProjectInstanceHandler projInstHandler = new ProjectInstanceHandler(mongo_client, vertx.eventBus());		 
		ParticipantHandler partHandler = new ParticipantHandler(mongo_client, projInstHandler, vertx);
		ObjectGenerator.buildAPIProject(projManager, expManager, taskManager, mongo_client, "Testproject")
		.onSuccess(existingApiProject -> 
		{	
			Async projectAsync= context.async();
			projManager.getElement(existingApiProject.getUUID())
			.onSuccess(project -> {
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
	public void testTokens(TestContext context)
	{			
		ElementManager<Project> projManager  = new ElementManager<Project>(Project::new, APIProject::new, mongo_client,eb);
		ElementManager<Experiment> expManager  = new ElementManager<Experiment>(Experiment::new, APIExperiment::new, mongo_client,eb);;
		ElementManager<Task> taskManager = new ElementManager<Task>(Task::new, APITask::new, mongo_client,eb);
		ProjectInstanceHandler handler = new ProjectInstanceHandler(mongo_client, eb);
		Async testAsync = context.async();
		ObjectGenerator.buildAPIProject(projManager, expManager, taskManager, mongo_client, "Testproject")
		.onSuccess(apiProject -> {
			JsonObject projInstData = new JsonObject().put("UUID", apiProject.getUUID())
					.put("name", "TestingProject")
					.put("version", apiProject.getVersion())
					.put("private", false);
			Async projAsync = context.async(); 
			handler.createProjectInstance(projInstData)
			.onSuccess(projInstance -> {
				Async permAsync = context.async();
				projInstance.createPermanentAccessToken()
				.onSuccess(permToken -> {				
					projInstance.createAccessTokens(20)
					.onSuccess(tokens -> {

						mongo_client.findOne(projInstance.getTargetCollection(), new JsonObject().put("_id", projInstance.getID()), null)
						.onSuccess(element -> {
							System.out.println(element.encodePrettily());

							Async invalidAsync = context.async();
							Async tempAsync = context.async(); 
							projInstance.useToken(permToken)
							.onSuccess(res -> {
								System.out.println("Trying to use invalid permanent token");
								projInstance.useToken(permToken.replace(permToken.charAt(0),(char)(permToken.charAt(0)+1)))
								.onSuccess(invalid -> {
									context.fail("This is not a valid token");								
								})
								.onFailure(err -> {
									invalidAsync.complete();
									System.out.println("Ivalid token failed successfully");
								});
								permAsync.complete();
							})
							.onFailure(err -> context.fail(err));
							System.out.println("Trying to use non permanent token");
							projInstance.useToken(tokens.getString(0))
							.onSuccess(res -> {
								System.out.println("Successfully used " + tokens.getString(0) + " trying again");
								projInstance.useToken(tokens.getString(0))
								.onSuccess(invalid -> {
									context.fail("This token has already been used");
								})
								.onFailure(err -> {
									System.out.println("Token could not be used again");
									tempAsync.complete();
								});
							})
							.onFailure(err -> {
								System.out.println("Token could not be used a first time ");
								context.fail(err);
							});
						})
						.onFailure(err -> context.fail(err));
					})
					.onFailure(err -> context.fail(err));

					projAsync.complete();

				})
				.onFailure(err -> context.fail(err));
			})
			.onFailure(err -> context.fail(err));
			testAsync.complete();
		})
		.onFailure(err -> context.fail(err));

	}

}
