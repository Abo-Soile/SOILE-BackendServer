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
		JsonArray permissionsForAccess = new JsonArray();
		ObjectGenerator.buildAPIExperiment(expManager, taskManager,mongo_client, "TestExperiment1")
		.onSuccess(apiexp -> {
			Async listAsync = context.async();
			context.assertEquals("tabcdefg2",apiexp.getElements().getJsonObject(0).getJsonObject("data").getString("instanceID"));
			permissionsForAccess.add(apiexp.getUUID());
			System.out.println(apiexp.getElements().encodePrettily());
			for(int i = 0; i < apiexp.getElements().size(); i++)
			{
				permissionsForAccess.add(apiexp.getElements().getJsonObject(i).getJsonObject("data").getString("UUID"));
			}
			// check, that the created Elements actually exist.
			taskManager.getElementList(permissionsForAccess).onSuccess(list -> {
				System.out.println(list.encodePrettily());
				context.assertEquals(2,list.size());					
				Async expAsync2 = context.async();				
				ObjectGenerator.buildAPIExperiment(expManager, taskManager,mongo_client, "TestExperiment2")
				.onSuccess(apiexp2 -> {
					permissionsForAccess.add(apiexp2.getUUID());
					Async listAsync2 = context.async();
					System.out.println(apiexp2.getJson().encodePrettily());
					context.assertEquals("tabcdefg4",apiexp2.getElements().getJsonObject(0).getJsonObject("data").getString("instanceID"));
					for(int i = 0; i < apiexp2.getElements().size(); i++)
					{
						permissionsForAccess.add(apiexp2.getElements().getJsonObject(i).getJsonObject("data").getString("UUID"));
					}
					taskManager.getElementList(permissionsForAccess).onSuccess(list2 -> {
						context.assertEquals(4,list2.size());				
						listAsync2.complete();
						for(int j = 0; j <list2.size(); ++j)
						{
							Async taskDataAsync = context.async();
							taskManager.getElement(list2.getJsonObject(j).getString("uuid"))
							.onSuccess(currentTask -> {
								System.out.println("Current Task: \n" + currentTask.toJson().encodePrettily());
								switch(currentTask.getName())
								{
								case "Test1": context.assertEquals("javascript", currentTask.getCodetype());break;
								case "Test2": context.assertEquals("elang", currentTask.getCodetype());break;
								case "Test3": context.assertEquals("javascript", currentTask.getCodetype());break;
								case "Test4": context.assertEquals("javascript", currentTask.getCodetype());break;
								default: context.fail("Found unexpected Task with name: " + currentTask.getName());
								}
								taskDataAsync.complete();
							})
							.onFailure(err -> context.fail(err));								
						}
					});
					
					Async listAsync3 = context.async();
					expManager.getElementList(permissionsForAccess).onSuccess(list2 -> {
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
				.onFailure(err -> failContext(err,context));
				listAsync.complete();
			});
			expAsync.complete();
		})
		.onFailure(err -> failContext(err,context));
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
			.onFailure(err -> failContext(err,context));
			
			projManager.getElementList(new JsonArray()).onSuccess(list -> {
				context.assertEquals(1,list.size()); // no private data
				plistAsync.complete();
			})
			.onFailure(err -> failContext(err,context));

			taskManager.getElementList(new JsonArray()).onSuccess(list -> {
				context.assertEquals(5,list.size()); // one private task
				tlistAsync.complete();
			})
			.onFailure(err -> failContext(err,context));
			projAsync.complete();
		})
		.onFailure(err -> failContext(err,context));
	}
}
