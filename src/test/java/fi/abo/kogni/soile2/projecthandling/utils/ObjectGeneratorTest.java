package fi.abo.kogni.soile2.projecthandling.utils;

import org.junit.Test;

import fi.abo.kogni.soile2.GitTest;
import fi.abo.kogni.soile2.datamanagement.git.GitFile;
import fi.abo.kogni.soile2.projecthandling.apielements.APIExperiment;
import fi.abo.kogni.soile2.projecthandling.apielements.APIProject;
import fi.abo.kogni.soile2.projecthandling.apielements.APITask;
import fi.abo.kogni.soile2.projecthandling.projectElements.ElementFactory;
import fi.abo.kogni.soile2.projecthandling.projectElements.impl.ElementManager;
import fi.abo.kogni.soile2.projecthandling.projectElements.impl.Experiment;
import fi.abo.kogni.soile2.projecthandling.projectElements.impl.Project;
import fi.abo.kogni.soile2.projecthandling.projectElements.impl.Task;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
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
								case "Test1": context.assertFalse(currentTask.getPrivate());break;
								case "Test2": context.assertFalse(currentTask.getPrivate());break;
								case "Test3": context.assertFalse(currentTask.getPrivate());break;
								case "Test4": context.assertFalse(currentTask.getPrivate());break;
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
					Async versionlistAsync = context.async();
					expManager.getVersionListForElement(apiexp.getUUID()).onSuccess(list2 -> {
						boolean init_version_found = false;
						context.assertEquals(2,list2.size());
						for(int i = 0; i < list2.size(); ++i)
						{
							if(list2.getJsonObject(i).containsKey("tag"))
							{
								context.assertEquals("Initial_Version", list2.getJsonObject(i).getString("tag"));
								init_version_found = true;
							}
						}
						context.assertTrue(init_version_found);
						versionlistAsync.complete();
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
			System.out.println(apiproj.getAPIJson().encodePrettily());
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
			Async versionlistAsync = context.async();
			projManager.getVersionListForElement(apiproj.getUUID()).onSuccess(list2 -> {
				boolean init_version_found = false;
				context.assertEquals(2,list2.size());
				for(int i = 0; i < list2.size(); ++i)
				{
					if(list2.getJsonObject(i).containsKey("tag"))
					{
						context.assertEquals("Initial_Version", list2.getJsonObject(i).getString("tag"));
						init_version_found = true;
						// check this project
						Async GitObjectCorrectAsync = context.async();
						projManager.getGitJson(apiproj.getUUID(), list2.getJsonObject(i).getString("version"))
						.onSuccess(json -> {
							context.assertEquals(2, json.getJsonArray("experiments").size());
							context.assertEquals(2, json.getJsonArray("tasks").size());
							context.assertEquals(1, json.getJsonArray("filters").size());
							GitObjectCorrectAsync.complete();
						})
						.onFailure(err -> context.fail(err));

					}
				}
				context.assertTrue(init_version_found);
				versionlistAsync.complete();
			})
			.onFailure(err -> context.fail(err));
			
			projAsync.complete();
		})
		.onFailure(err -> context.fail(err));
	}
		
	@Test
	public void testExampleProject(TestContext context)
	{
		System.out.println("--------------------  Testing Project Generation with Example project ----------------------");
		Async proj2Async = context.async();
		ObjectGenerator.buildAPIProject(projManager, expManager, taskManager,mongo_client, "ExampleProject")
		.onSuccess(apiproj -> {
			Async gitRepoAsync = context.async();
			vertx.eventBus().request("soile.git.getGitFileContentsAsJson",new GitFile("Object.json", "P" + apiproj.getUUID(), apiproj.getVersion()).toJson())
			.onSuccess(response -> {
				// Check, that it is correct in git.
				JsonObject gitData = (JsonObject) response.body();
				JsonArray tasks = gitData.getJsonArray("tasks");
				context.assertEquals(2, tasks.size());
				for(int i = 0; i < tasks.size(); ++i)
				{
					if(tasks.getJsonObject(i).getString("name").equals("JSExp"))
					{
						context.assertTrue(tasks.getJsonObject(i).containsKey("position"));
						context.assertEquals(950, tasks.getJsonObject(i).getJsonObject("position").getNumber("x"));
					}
					else
					{
						context.assertTrue(tasks.getJsonObject(i).containsKey("position"));
						context.assertEquals(100, tasks.getJsonObject(i).getJsonObject("position").getNumber("x"));
					}
				}

				JsonArray experiments = gitData.getJsonArray("experiments");
				context.assertTrue(experiments.getJsonObject(0).containsKey("position"));
				context.assertEquals(650, experiments.getJsonObject(0).getJsonObject("position").getNumber("x"));
				JsonArray expElements = experiments.getJsonObject(0).getJsonArray("elements");
				context.assertEquals(2, expElements.size());
				for(int i = 0; i < expElements.size(); ++i)
				{
					context.assertTrue(expElements.getJsonObject(i).containsKey("data"));
					JsonObject elementData = expElements.getJsonObject(i).getJsonObject("data"); 
					if(elementData.getString("name").equals("ElangExp"))
					{
						context.assertTrue(elementData.containsKey("position"));
						context.assertEquals(100, elementData.getJsonObject("position").getNumber("x"));
					}
					else
					{
						context.assertTrue(elementData.containsKey("position"));
						context.assertEquals(350, elementData.getJsonObject("position").getNumber("x"));
					}
				}
				JsonArray filters = gitData.getJsonArray("filters");
				context.assertTrue(filters.getJsonObject(0).containsKey("position"));
				context.assertEquals(350, filters.getJsonObject(0).getJsonObject("position").getNumber("x"));
				gitRepoAsync.complete();
			})
			.onFailure(err -> context.fail(err));
			JsonArray tasks = apiproj.getTasks();
			context.assertEquals(2, tasks.size());
			for(int i = 0; i < tasks.size(); ++i)
			{
				if(tasks.getJsonObject(i).getString("name").equals("JSExp"))
				{
					context.assertTrue(tasks.getJsonObject(i).containsKey("position"));
					context.assertEquals(950, tasks.getJsonObject(i).getJsonObject("position").getNumber("x"));
				}
				else
				{
					context.assertTrue(tasks.getJsonObject(i).containsKey("position"));
					context.assertEquals(100, tasks.getJsonObject(i).getJsonObject("position").getNumber("x"));
				}
			}

			JsonArray experiments = apiproj.getExperiments();
			context.assertTrue(experiments.getJsonObject(0).containsKey("position"));
			context.assertEquals(650, experiments.getJsonObject(0).getJsonObject("position").getNumber("x"));
			JsonArray expElements = experiments.getJsonObject(0).getJsonArray("elements");
			context.assertEquals(2, expElements.size());
			for(int i = 0; i < expElements.size(); ++i)
			{
				context.assertTrue(expElements.getJsonObject(i).containsKey("data"));
				JsonObject elementData = expElements.getJsonObject(i).getJsonObject("data"); 
				if(elementData.getString("name").equals("ElangExp"))
				{
					context.assertTrue(elementData.containsKey("position"));
					context.assertEquals(100, elementData.getJsonObject("position").getNumber("x"));
				}
				else
				{
					context.assertTrue(elementData.containsKey("position"));
					context.assertEquals(350, elementData.getJsonObject("position").getNumber("x"));
				}
			}
			JsonArray filters = apiproj.getFilters();
			context.assertTrue(filters.getJsonObject(0).containsKey("position"));
			context.assertEquals(350, filters.getJsonObject(0).getJsonObject("position").getNumber("x"));
			
			proj2Async.complete();
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
			Async tlistAsync = context.async();		
			
			// check, that the created Elements actually exist.
			taskManager.getElementList(new JsonArray())
			.onSuccess(list -> {
				context.assertEquals(1,list.size()); // one task
				Async gitAsync = context.async();
				taskManager.getAPIElementFromDB(apiTask.getUUID(), apiTask.getVersion())
				.onSuccess(element -> {
					APITask task = (APITask) element;
					context.assertEquals("elang",task.getCodeLanguage());					
					gitAsync.complete();
				}).onFailure(err -> context.fail(err));
				Async dbAsync = context.async();
				TaskFactory.loadElement(mongo_client, apiTask.getUUID())
				.onSuccess(Task -> {
					context.assertFalse(Task.getPrivate());
					dbAsync.complete();							
				})
				.onFailure(err -> context.fail(err));
				
				tlistAsync.complete();
			})
			.onFailure(err -> context.fail(err));
			Async versionlistAsync = context.async();
			taskManager.getVersionListForElement(apiTask.getUUID()).onSuccess(list2 -> {
				boolean init_version_found = false;
				// This has 3. One creation one adding the resources and the final version
				context.assertEquals(3,list2.size());
				for(int i = 0; i < list2.size(); ++i)
				{
					if(list2.getJsonObject(i).containsKey("tag"))
					{
						context.assertEquals("Initial_Version", list2.getJsonObject(i).getString("tag"));
						init_version_found = true;
					}
				}
				context.assertTrue(init_version_found);
				versionlistAsync.complete();
			})
			.onFailure(err -> context.fail(err));
			projAsync.complete();
		})
		.onFailure(err -> context.fail(err));
	}
	
}
