package fi.abo.kogni.soile2.datamanagement.cleanup;

import org.junit.Test;

import fi.abo.kogni.soile2.http_server.SoileWebTest;
import fi.abo.kogni.soile2.projecthandling.apielements.APIExperiment;
import fi.abo.kogni.soile2.projecthandling.apielements.APIProject;
import fi.abo.kogni.soile2.projecthandling.apielements.APITask;
import fi.abo.kogni.soile2.projecthandling.projectElements.impl.ElementManager;
import fi.abo.kogni.soile2.projecthandling.projectElements.impl.Experiment;
import fi.abo.kogni.soile2.projecthandling.projectElements.impl.Project;
import fi.abo.kogni.soile2.projecthandling.projectElements.impl.Task;
import fi.abo.kogni.soile2.projecthandling.utils.ObjectGenerator;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.UpdateOptions;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;

public class IntegrationsTest extends SoileWebTest{
	ElementManager<Task> taskManager;
	ElementManager<Experiment> expManager;
	ElementManager<Project> projManager;

	@Override
	public void runBeforeTests(TestContext context)
	{		
		super.runBeforeTests(context);
		projManager = new ElementManager<Project>(Project::new, APIProject::new, mongo_client,vertx);
		expManager = new ElementManager<Experiment>(Experiment::new, APIExperiment::new, mongo_client,vertx);
		taskManager = new ElementManager<Task>(Task::new, APITask::new, mongo_client,vertx);
	}


	@Test
	public void testBuildProject(TestContext context)
	{		
		System.out.println("--------------------  Testing Database updating ----------------------");
		Async projAsync = context.async();
		ObjectGenerator.buildAPIProject(projManager, expManager, taskManager,mongo_client, "ExampleProject")
		.onSuccess(exampleAPI -> {
			JsonObject exampleDependencies = exampleAPI.calcDependencies();
			JsonArray exampleTasks = exampleDependencies.getJsonArray("tasks");
			JsonArray exampleExp= exampleDependencies.getJsonArray("experiments");
			ObjectGenerator.buildAPIProject(projManager, expManager, taskManager,mongo_client, "Testproject")
			.onSuccess(apiproj -> {
				JsonObject origDependencies = apiproj.calcDependencies();
				JsonArray origTasks = origDependencies.getJsonArray("tasks");
				JsonArray origExp= origDependencies.getJsonArray("experiments");
				// next version of apiProj will be example proj.
				exampleAPI.setUUID(apiproj.getUUID());
				exampleAPI.setName(apiproj.getName());
				exampleAPI.setVersion(apiproj.getVersion());
				projManager.updateElement(exampleAPI, "New Version")
				.onSuccess(latestVersion -> {
					// now, we delete all dependencies in the database
					// dependency deletion query:
					JsonObject deletionUpdate = new JsonObject().put("$unset",new JsonObject().put("dependencies", 1));
					Async cleanUpAsync = context.async();
					projManager.getElement(apiproj.getUUID())						
					.compose(project -> {				
						JsonObject projectDependencies = project.getDependencies();
						context.assertEquals(4, projectDependencies.getJsonArray("tasks").size());						
						context.assertEquals(3, projectDependencies.getJsonArray("experiments").size());
						for(int i = 0; i < origExp.size(); i++)
						{
							context.assertTrue(projectDependencies.getJsonArray("experiments").contains(origExp.getString(i)));
						}
						for(int i = 0; i < exampleExp.size(); i++)
						{
							context.assertTrue(projectDependencies.getJsonArray("experiments").contains(exampleExp.getString(i)));
						}
						
						for(int i = 0; i < origTasks.size(); i++)
						{
							context.assertTrue(projectDependencies.getJsonArray("tasks").contains(origTasks.getString(i)));
						}
						for(int i = 0; i < exampleTasks.size(); i++)
						{
							context.assertTrue(projectDependencies.getJsonArray("tasks").contains(exampleTasks.getString(i)));
						}
						
						System.out.println(projectDependencies.encodePrettily());
						
						String expName = apiproj.getExperiments().getJsonObject(0).getString("UUID");
						return expManager.getElement(expName);
					})
					.compose(experiment -> {
						JsonObject projectDependencies = experiment.getDependencies();

						context.assertEquals(2, projectDependencies.getJsonArray("tasks").size());
						context.assertTrue( !projectDependencies.containsKey("experiments")  ||  projectDependencies.getJsonArray("experiments").size() == 0);
						return mongo_client.updateCollectionWithOptions(projManager.getElementSupplier().get().getTargetCollection(), new JsonObject(), deletionUpdate, new UpdateOptions().setMulti(true) );
					})
					.compose(projCleaned -> {
						return mongo_client.updateCollectionWithOptions(expManager.getElementSupplier().get().getTargetCollection(), new JsonObject(), deletionUpdate, new UpdateOptions().setMulti(true));
					})
					.onSuccess(expDB-> {						
						Async restoreAsync = context.async();
						projManager.cleanUp();
						expManager.cleanUp();
						String expName = apiproj.getExperiments().getJsonObject(0).getString("UUID");
						expManager.getElement(expName)
						.compose(project -> {				
							JsonObject projectDependencies = project.getDependencies();											
							context.assertTrue( !projectDependencies.containsKey("experiments")  ||  projectDependencies.getJsonArray("experiments").size() == 0);
							context.assertTrue( !projectDependencies.containsKey("tasks")  ||  projectDependencies.getJsonArray("tasks").size() == 0);												
							return projManager.getElement(apiproj.getUUID());
						})
						.compose(experiment -> {
							JsonObject projectDependencies = experiment.getDependencies();							
							context.assertTrue( !projectDependencies.containsKey("experiments")  ||  projectDependencies.getJsonArray("experiments").size() == 0);
							context.assertTrue( !projectDependencies.containsKey("tasks")  ||  projectDependencies.getJsonArray("tasks").size() == 0);							
							IntegrationBuilder builder = new IntegrationBuilder(taskManager, expManager, projManager, mongo_client);
							return builder.addDependencies();
						})
						.compose(restored -> {						
							return projManager.getElement(apiproj.getUUID());					
						})
						.compose(project -> {
							JsonObject projectDependencies = project.getDependencies();
							for(int i = 0; i < origExp.size(); i++)
							{
								context.assertTrue(projectDependencies.getJsonArray("experiments").contains(origExp.getString(i)));
							}
							for(int i = 0; i < exampleExp.size(); i++)
							{
								context.assertTrue(projectDependencies.getJsonArray("experiments").contains(exampleExp.getString(i)));
							}
							
							for(int i = 0; i < origTasks.size(); i++)
							{
								context.assertTrue(projectDependencies.getJsonArray("tasks").contains(origTasks.getString(i)));
							}
							for(int i = 0; i < exampleTasks.size(); i++)
							{
								context.assertTrue(projectDependencies.getJsonArray("tasks").contains(exampleTasks.getString(i)));
							}
							context.assertEquals(4, projectDependencies.getJsonArray("tasks").size());
							context.assertEquals(3, projectDependencies.getJsonArray("experiments").size());					
							return expManager.getElement(expName);
						})
						.compose(experiment -> {
							JsonObject expDependencies = experiment.getDependencies();
							context.assertEquals(2, expDependencies.getJsonArray("tasks").size());
							context.assertTrue( !expDependencies.containsKey("experiments")  ||  expDependencies.getJsonArray("experiments").size() == 0);
							return Future.succeededFuture();
						})
						.onSuccess(done -> restoreAsync.complete())
						.onFailure(err -> context.fail(err));

						cleanUpAsync.complete();
					})
					.onFailure(err -> context.fail(err));
					projAsync.complete();
				})
				.onFailure(err -> context.fail(err));
			})
			.onFailure(err -> context.fail(err));
		})
		.onFailure(err -> context.fail(err));
	}


}
