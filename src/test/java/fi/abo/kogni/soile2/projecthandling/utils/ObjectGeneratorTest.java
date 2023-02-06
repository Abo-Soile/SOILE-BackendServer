package fi.abo.kogni.soile2.projecthandling.utils;

import org.junit.Test;

import fi.abo.kogni.soile2.GitTest;
import fi.abo.kogni.soile2.projecthandling.apielements.APIExperiment;
import fi.abo.kogni.soile2.projecthandling.apielements.APIProject;
import fi.abo.kogni.soile2.projecthandling.apielements.APITask;
import fi.abo.kogni.soile2.projecthandling.projectElements.ElementFactory;
import fi.abo.kogni.soile2.projecthandling.projectElements.impl.ElementManager;
import fi.abo.kogni.soile2.projecthandling.projectElements.impl.Experiment;
import fi.abo.kogni.soile2.projecthandling.projectElements.impl.Project;
import fi.abo.kogni.soile2.projecthandling.projectElements.impl.Task;
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
		projManager = new ElementManager<Project>(Project::new, APIProject::new, mongo_client,vertx);
		expManager = new ElementManager<Experiment>(Experiment::new, APIExperiment::new, mongo_client,vertx);
		taskManager = new ElementManager<Task>(Task::new, APITask::new, mongo_client,vertx);
	}
	
	
	@Test
	public void testBuildExperiment(TestContext context)
	{		
		System.out.println("--------------------  Testing Experiment generation ----------------------");
		Async expAsync = context.async();
		JsonArray permissionsForAccess = new JsonArray();
		ObjectGenerator.buildAPIExperiment(expManager, taskManager,mongo_client, "TestExperiment1")
		.onSuccess(apiexp -> {
			Async listAsync = context.async();
			context.assertEquals("tabcdefg2",apiexp.getElements().getJsonObject(0).getJsonObject("data").getString("instanceID"));
			permissionsForAccess.add(apiexp.getUUID());
			for(int i = 0; i < apiexp.getElements().size(); i++)
			{
				permissionsForAccess.add(apiexp.getElements().getJsonObject(i).getJsonObject("data").getString("UUID"));
			}
			// check, that the created Elements actually exist.
			taskManager.getElementList(permissionsForAccess).onSuccess(list -> {
				context.assertEquals(2,list.size());					
				Async expAsync2 = context.async();				
				ObjectGenerator.buildAPIExperiment(expManager, taskManager,mongo_client, "TestExperiment2")
				.onSuccess(apiexp2 -> {
					permissionsForAccess.add(apiexp2.getUUID());
					Async listAsync2 = context.async();
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
								switch(currentTask.getName())
								{
								case "Test1": context.assertEquals("psychopy", currentTask.getCodetype());break;
								case "Test2": context.assertEquals("elang", currentTask.getCodetype());break;
								case "Test3": context.assertEquals("psychopy", currentTask.getCodetype());break;
								case "Test4": context.assertEquals("psychopy", currentTask.getCodetype());break;
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
		System.out.println("--------------------  Testing Project Generation ----------------------");
		Async projAsync = context.async();
		ObjectGenerator.buildAPIProject(projManager, expManager, taskManager,mongo_client, "Testproject")
		.onSuccess(apiproj -> {
			Async plistAsync = context.async();
			Async elistAsync = context.async();
			Async tlistAsync = context.async();			
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
	
	@Test
	public void testBuildTask(TestContext context)
	{		
		System.out.println("--------------------  Testing Project Generation ----------------------");
		Async projAsync = context.async();
		ElementFactory<Task> TaskFactory = new ElementFactory<Task>(Task::new);
		ObjectGenerator.buildAPITask(taskManager, "Test2", mongo_client)
		.onSuccess(apiTask -> {
			System.out.println("Task created");
			Async tlistAsync = context.async();			
			// check, that the created Elements actually exist.
			taskManager.getElementList(new JsonArray())
			.onSuccess(list -> {
				System.out.println("Element List obtained");
				context.assertEquals(1,list.size()); // one task
				Async gitAsync = context.async();
				taskManager.getAPIElementFromDB(apiTask.getUUID(), apiTask.getVersion())
				.onSuccess(element -> {
					APITask task = (APITask) element;
					System.out.println("Task: " + task.getJson().encodePrettily());
					context.assertEquals(1,task.getResources().size());					
					gitAsync.complete();
				}).onFailure(err -> context.fail(err));
				Async dbAsync = context.async();
				TaskFactory.loadElement(mongo_client, apiTask.getUUID())
				.onSuccess(Task -> {
					System.out.println("DBTask: " + Task.toJson().encodePrettily());
					context.assertEquals(1, Task.getResources().size());
					dbAsync.complete();							
				})
				.onFailure(err -> context.fail(err));
				tlistAsync.complete();
			})
			.onFailure(err -> context.fail(err));
			projAsync.complete();
		})
		.onFailure(err -> context.fail(err));
	}
	
}
