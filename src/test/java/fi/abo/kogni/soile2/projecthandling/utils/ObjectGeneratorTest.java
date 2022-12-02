package fi.abo.kogni.soile2.projecthandling.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.After;
import org.junit.Test;

import fi.abo.kogni.soile2.MongoTest;
import fi.abo.kogni.soile2.datamanagement.git.GitManager;
import fi.abo.kogni.soile2.projecthandling.apielements.APIExperiment;
import fi.abo.kogni.soile2.projecthandling.apielements.APIProject;
import fi.abo.kogni.soile2.projecthandling.apielements.APITask;
import fi.abo.kogni.soile2.projecthandling.projectElements.ElementManager;
import fi.abo.kogni.soile2.projecthandling.projectElements.Experiment;
import fi.abo.kogni.soile2.projecthandling.projectElements.Project;
import fi.abo.kogni.soile2.projecthandling.projectElements.Task;
import fi.abo.kogni.soile2.utils.VerticleInitialiser;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;

public class ObjectGeneratorTest extends MongoTest {
	
	String gitDir;
	GitManager gitManager;
	ElementManager<Task>  taskManager;
	ElementManager<Experiment>  expManager;
	ElementManager<Project>  projManager;
	
	@Override
	public void runBeforeTests(TestContext context)
	{		
		super.runBeforeTests(context);
		Async gitInit = context.async();
		VerticleInitialiser.startGitVerticle(vertx).onSuccess( dir -> {
			gitDir = dir;
			gitInit.complete();
		})
		.onFailure(err -> {
			context.fail(err);
			gitInit.complete();
		});
		gitManager = new GitManager(vertx.eventBus());
		projManager = new ElementManager<Project>(Project::new, APIProject::new, mongo_client,gitManager);
		expManager = new ElementManager<Experiment>(Experiment::new, APIExperiment::new, mongo_client,gitManager);
		taskManager = new ElementManager<Task>(Task::new, APITask::new, mongo_client,gitManager);
	}
	
	@After
	public void cleanupGit(TestContext context)
	{
		try
		{
			// clean up git Repo
			Files.deleteIfExists(Path.of(gitDir));
		}
		catch(IOException e)
		{
			System.out.println("Could not clean up git repo");
		}
	}
	
	@Test
	public void testBuildExperiment(TestContext context)
	{		
		Async expAsync = context.async();
		ObjectGenerator.buildAPIExperiment(expManager, taskManager,mongo_client, "TestExperiment1")
		.onSuccess(apiexp -> {
			Async listAsync = context.async();
			context.assertEquals("tabcdefg2",apiexp.getElements().getJsonObject(0).getJsonObject("data").getString("instanceID"));
			
			// check, that the created Elements actually exist.
			taskManager.getElementList().onSuccess(list -> {
				context.assertEquals(2,list.size());				
				Async expAsync2 = context.async();				
				ObjectGenerator.buildAPIExperiment(expManager, taskManager,mongo_client, "TestExperiment2")
				.onSuccess(apiexp2 -> {
					Async listAsync2 = context.async();
					System.out.println(apiexp2.getJson().encodePrettily());
					context.assertEquals("tabcdefg4",apiexp2.getElements().getJsonObject(0).getJsonObject("data").getString("instanceID"));					
					taskManager.getElementList().onSuccess(list2 -> {
						context.assertEquals(4,list2.size());				
						listAsync2.complete();
					});
					Async listAsync3 = context.async();
					expManager.getElementList().onSuccess(list2 -> {
						context.assertEquals(2,list2.size());				
						listAsync3.complete();
					});
					expAsync2.complete();
				})
				.onFailure(err -> context.fail(err));
				listAsync.complete();
			});
			expAsync.complete();
		})
		.onFailure(err -> context.fail(err));
	}
	
	@Test
	public void testBuildProject(TestContext context)
	{		
		Async projAsync = context.async();
		ObjectGenerator.buildAPIProject(projManager, expManager, taskManager,mongo_client, "Testproject")
		.onSuccess(apiproj -> {
			Async plistAsync = context.async();
			Async elistAsync = context.async();
			Async tlistAsync = context.async();
			System.out.println("The Resulting API Project is: \n" + apiproj.getJson().encodePrettily());
			// check, that the created Elements actually exist.
			expManager.getElementList()
			.onSuccess(expList -> 
			{					
				context.assertEquals(2, expList.size());
				elistAsync.complete();
			})
			.onFailure(err -> context.fail(err));
			
			projManager.getElementList().onSuccess(list -> {
				context.assertEquals(1,list.size());
				plistAsync.complete();
			})
			.onFailure(err -> context.fail(err));

			taskManager.getElementList().onSuccess(list -> {
				context.assertEquals(6,list.size());
				tlistAsync.complete();
			})
			.onFailure(err -> context.fail(err));
			projAsync.complete();
		})
		.onFailure(err -> context.fail(err));
	}
}
