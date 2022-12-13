package fi.abo.kogni.soile2.projecthandling.utils;

import org.junit.Test;

import fi.abo.kogni.soile2.GitTest;
import fi.abo.kogni.soile2.projecthandling.apielements.APIExperiment;
import fi.abo.kogni.soile2.projecthandling.apielements.APIProject;
import fi.abo.kogni.soile2.projecthandling.apielements.APITask;
import fi.abo.kogni.soile2.projecthandling.projectElements.ElementManager;
import fi.abo.kogni.soile2.projecthandling.projectElements.Experiment;
import fi.abo.kogni.soile2.projecthandling.projectElements.Project;
import fi.abo.kogni.soile2.projecthandling.projectElements.Task;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;

public class ObjectGeneratorTest extends GitTest {
	
	ElementManager<Task>  taskManager;
	ElementManager<Experiment>  expManager;
	ElementManager<Project>  projManager;
	
	@Override
	public void runBeforeTests(TestContext context)
	{		
		super.runBeforeTests(context);
		projManager = new ElementManager<Project>(Project::new, APIProject::new, mongo_client,gitManager);
		expManager = new ElementManager<Experiment>(Experiment::new, APIExperiment::new, mongo_client,gitManager);
		taskManager = new ElementManager<Task>(Task::new, APITask::new, mongo_client,gitManager);
	}
	
	
	@Test
	public void testBuildExperiment(TestContext context)
	{		
		Async expAsync = context.async();
		JsonArray Permissions1 = new JsonArray();
		ObjectGenerator.buildAPIExperiment(expManager, taskManager,mongo_client, "TestExperiment1")
		.onSuccess(apiexp -> {
			Async listAsync = context.async();
			context.assertEquals("tabcdefg2",apiexp.getElements().getJsonObject(0).getJsonObject("data").getString("instanceID"));
			Permissions1.add(apiexp.getUUID());
			System.out.println(apiexp.getElements().encodePrettily());
			for(int i = 0; i < apiexp.getElements().size(); i++)
			{
				Permissions1.add(apiexp.getElements().getJsonObject(i).getJsonObject("data").getString("UUID"));
			}
			// check, that the created Elements actually exist.
			taskManager.getElementList(Permissions1).onSuccess(list -> {
				System.out.println(list.encodePrettily());
				context.assertEquals(2,list.size());					
				Async expAsync2 = context.async();				
				ObjectGenerator.buildAPIExperiment(expManager, taskManager,mongo_client, "TestExperiment2")
				.onSuccess(apiexp2 -> {
					Permissions1.add(apiexp2.getUUID());
					Async listAsync2 = context.async();
					System.out.println(apiexp2.getJson().encodePrettily());
					context.assertEquals("tabcdefg4",apiexp2.getElements().getJsonObject(0).getJsonObject("data").getString("instanceID"));
					for(int i = 0; i < apiexp2.getElements().size(); i++)
					{
						Permissions1.add(apiexp2.getElements().getJsonObject(i).getJsonObject("data").getString("UUID"));
					}
					taskManager.getElementList(Permissions1).onSuccess(list2 -> {
						context.assertEquals(4,list2.size());				
						listAsync2.complete();
					});
					Async listAsync3 = context.async();
					expManager.getElementList(Permissions1).onSuccess(list2 -> {
						context.assertEquals(2,list2.size());				
						listAsync3.complete();
					});
					Async listAsync4 = context.async();
					expManager.getElementList(new JsonArray()).onSuccess(list2 -> {
						context.assertEquals(1,list2.size());				
						listAsync4.complete();
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
			expManager.getElementList(new JsonArray())
			.onSuccess(expList -> 
			{					
				context.assertEquals(1, expList.size()); // one private experiment
				elistAsync.complete(); 
			})
			.onFailure(err -> context.fail(err));
			
			projManager.getElementList(new JsonArray()).onSuccess(list -> {
				context.assertEquals(1,list.size()); // no private data
				plistAsync.complete();
			})
			.onFailure(err -> context.fail(err));

			taskManager.getElementList(new JsonArray()).onSuccess(list -> {
				context.assertEquals(5,list.size()); // one private task
				tlistAsync.complete();
			})
			.onFailure(err -> context.fail(err));
			projAsync.complete();
		})
		.onFailure(err -> context.fail(err));
	}
}
