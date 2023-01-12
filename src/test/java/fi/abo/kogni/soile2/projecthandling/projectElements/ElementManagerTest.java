package fi.abo.kogni.soile2.projecthandling.projectElements;

import javax.naming.directory.InvalidAttributesException;

import org.junit.Test;

import fi.abo.kogni.soile2.GitTest;
import fi.abo.kogni.soile2.projecthandling.apielements.APIExperiment;
import fi.abo.kogni.soile2.projecthandling.apielements.APIProject;
import fi.abo.kogni.soile2.projecthandling.apielements.APITask;
import fi.abo.kogni.soile2.projecthandling.exceptions.ElementNameExistException;
import fi.abo.kogni.soile2.projecthandling.exceptions.NoNameChangeException;
import fi.abo.kogni.soile2.projecthandling.utils.ObjectGenerator;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;

public class ElementManagerTest extends GitTest{

	ElementManager<Project> projManager;
	ElementManager<Experiment> expManager;
	ElementManager<Task> taskManager;
	@Override
	public void runBeforeTests(TestContext context)
	{		
		super.runBeforeTests(context);	
		projManager = new ElementManager<Project>(Project::new, APIProject::new, mongo_client,gitManager);
		taskManager = new ElementManager<Task>(Task::new, APITask::new, mongo_client,gitManager);
		expManager = new ElementManager<Experiment>(Experiment::new, APIExperiment::new, mongo_client,gitManager);
	}

	@Test
	public void testGetList(TestContext context)
	{		
		System.out.println("--------------------  Testing Get Element List ----------------------");
		Async projectAsync = context.async();		
		projManager.createElement("NewProject")
		.onSuccess(project -> {
			Async twoProjects = context.async();
			projManager.createElement("NewProject2")
			.onSuccess(p2 -> {
				projManager.getElementList(new JsonArray())
				.onSuccess(list -> {
					context.assertEquals(2, list.size()); // default is non private
					Project[] projects = new Project[] {project, p2};
					for(Project p : projects)
					{
						for(int i = 0; i < list.size(); i++)
						{
							JsonObject current = list.getJsonObject(i); 
							if(current.getString("uuid").equals(p.getUUID()))
							{
								context.assertEquals(p.getName(), current.getString("name"));
								list.remove(i);
								break;
							}							
						}						
					}
					// everything was removed
					context.assertEquals(0, list.size());
					twoProjects.complete();
				})
				.onFailure(err -> {					
					context.fail(err);
					twoProjects.complete();
				});	
			})
			.onFailure(err -> {
				context.fail(err);
				twoProjects.complete();
			});
			projectAsync.complete();
		})
		.onFailure(err -> {
			context.fail(err);
			projectAsync.complete();

		});
	}



	@Test
	public void testCreation(TestContext context)
	{
		System.out.println("--------------------  Testing Element Creation ----------------------");
		Async projectAsync = context.async();
		projManager.createElement("NewProject").onSuccess(project -> {
			Async sameNameAsync = context.async();
			projManager.createElement("NewProject").onSuccess(Void -> {
				context.fail("This should not be possible. name already exists");
				sameNameAsync.complete();
			})
			.onFailure(err -> {
				context.assertEquals(err.getClass(), ElementNameExistException.class);
				sameNameAsync.complete();				
			});
			Async loadedElementAsync = context.async();
			projManager.createOrLoadElement("NewProject").onSuccess(loadedProj-> {				
				context.assertEquals(project.getUUID(), loadedProj.getUUID());
				loadedElementAsync.complete();
			})
			.onFailure(err -> context.fail(err));

			context.assertEquals(project.getName(), "NewProject");
			projectAsync.complete();
		}).
		onFailure(err ->{
			context.fail(err);
			projectAsync.complete();

		});		
	}

	@Test
	public void testDeletion(TestContext context)
	{
		System.out.println("--------------------  Testing Element deletion ----------------------");

		Async testAsync = context.async();

		ObjectGenerator.buildAPIProject(projManager,expManager, taskManager, mongo_client, "Testproject")
		.onSuccess(apiProject-> {
			Async taskAsync = context.async();
			taskManager.getElementList(new JsonArray())
			.onSuccess(taskList -> {
				String taskID = taskList.getJsonObject(0).getString("uuid");
				taskManager.getElement(taskID)
				.onSuccess(task -> {
					taskManager.getAPIElementFromDB(task.getUUID(),task.getCurrentVersion())
					.onSuccess(apiTask -> {
						context.assertTrue(task.getVisible());
						taskManager.deleteElement(apiTask)
						.onSuccess(deleted -> {				
							taskManager.getElement(apiTask.getUUID())
							.onSuccess(ctask -> {
								context.assertFalse(ctask.getVisible());
								// no Permissions
								taskManager.getElementList(new JsonArray())						
								.onSuccess(taskLists -> {					
									// only one was created and that was deleted. Nothing left.
									// there is one other, but that is private.
									boolean elementFound = false;
									for(int i = 0; i < taskLists.size(); ++i)
									{
										if(taskLists.getJsonObject(i).getBinary("uuid").equals(ctask.getUUID()))
										{
											elementFound = true;
										}
									}
									context.assertFalse(elementFound);
									taskAsync.complete();	
								})
								.onFailure(err -> context.fail(err));					
							})
							.onFailure(err ->context.fail(err));
						})
						.onFailure(err ->context.fail(err));
					})
					.onFailure(err -> context.fail(err));
				})
				.onFailure(err -> context.fail(err));
			})
			.onFailure(err -> context.fail(err));
			// Test for experiments
			Async expAsync = context.async();

			expManager.getElementList(new JsonArray())
			.onSuccess(expList -> {
				String expID = expList.getJsonObject(0).getString("uuid");
				expManager.getElement(expID)
				.onSuccess(experiment -> {
					expManager.getAPIElementFromDB(experiment.getUUID(),experiment.getCurrentVersion())
					.onSuccess(apiExperiment -> {
						context.assertTrue(experiment.getVisible());
						expManager.deleteElement(apiExperiment)
						.onSuccess(deleted -> {				
							expManager.getElement(apiExperiment.getUUID())
							.onSuccess(cExp -> {
								context.assertFalse(cExp.getVisible());
								// no Permissions
								expManager.getElementList(new JsonArray())						
								.onSuccess(expLists -> {					
									// only one was created and that was deleted. Nothing left.
									// there is one other, but that is private.
									context.assertEquals(0,expLists.size());
									expAsync.complete();	
								})
								.onFailure(err -> context.fail(err));					
							})
							.onFailure(err ->context.fail(err));
						})
						.onFailure(err ->context.fail(err));
					})
					.onFailure(err -> context.fail(err));
				})
				.onFailure(err -> context.fail(err));
			})
			.onFailure(err -> context.fail(err));
			Async projAsync = context.async();
			projManager.getElement(apiProject.getUUID())
			.onSuccess(project -> {
				context.assertTrue(project.getVisible());
				projManager.deleteElement(apiProject)
				.onSuccess(deleted -> {				
					projManager.getElement(apiProject.getUUID())
					.onSuccess(cProj -> {
						context.assertFalse(cProj.getVisible());
						// no Permissions
						projManager.getElementList(new JsonArray())						
						.onSuccess(projLists -> {					
							// only one was created and that was deleted. Nothing left.
							context.assertEquals(0,projLists.size());
							projAsync.complete();	
						})
						.onFailure(err -> context.fail(err));					
					})
					.onFailure(err ->context.fail(err));
				})
				.onFailure(err ->context.fail(err));
			})
			.onFailure(err -> context.fail(err));
			testAsync.complete();
		})		
		.onFailure(err ->{
			context.fail(err);
		});			
	}

	@Test
	public void testGetAPIElementFromDB(TestContext context)
	{
		System.out.println("--------------------  Testing API Element Retrieval ----------------------");
		Async testAsync = context.async();
		ObjectGenerator.buildAPITask(taskManager, "FirstTask", mongo_client)
		.onSuccess(apiTask -> {
			taskManager.getAPIElementFromDB(apiTask.getUUID(), apiTask.getVersion())
			.onSuccess(secondAPITask -> {
				APITask apiTask2 = (APITask) secondAPITask;
				context.assertEquals(apiTask.getCode(), apiTask2.getCode());
				context.assertEquals(apiTask.getCodetype(), apiTask2.getCodetype());
				testAsync.complete();
			})
			.onFailure(err -> context.fail(err));

		})		
		.onFailure(err ->{
			context.fail(err);
		});	

	}
	
	@Test
	public void testChangeName(TestContext context)
	{
		System.out.println("--------------------  Testing No Name change ----------------------");
		Async testAsync = context.async();
		ObjectGenerator.buildAPITask(taskManager, "FirstTask", mongo_client)
		.onSuccess(apiTask -> {
			String currentName = apiTask.getName();
			apiTask.setName("NewName");
			taskManager.updateElement(apiTask)
			.onSuccess(newVersion -> {
				context.fail("Should not be possible. The name should not be changeable");
			})
			.onFailure(err -> {
				System.out.println("Getting error");
				context.assertEquals(err.getClass(), NoNameChangeException.class);
				taskManager.getElement(apiTask.getUUID())
				.onSuccess(Task -> 
				{
					context.assertEquals(currentName, Task.getName());
					testAsync.complete();
				})
				.onFailure(err2 -> context.fail(err2));
				
			});

		})		
		.onFailure(err -> context.fail(err));	

	}
	
	@Test
	public void testVersionListForElement(TestContext context)
	{
		System.out.println("--------------------  Testing Version List Retrieval ----------------------");
		Async testAsync = context.async();
		ObjectGenerator.buildAPITask(taskManager, "FirstTask", mongo_client)
		.onSuccess(apiTask -> {
			taskManager.getVersionListForElement(apiTask.getUUID())
			.onSuccess(VersionList -> {
				// The is the creation (i.e. empty + the data from the task).
				context.assertEquals(2, VersionList.size());				
				testAsync.complete();
			})
			.onFailure(err -> context.fail(err));

		})		
		.onFailure(err ->{
			context.fail(err);
		});	

	}	
	
	@Test
	public void testTagListForElement(TestContext context)
	{
		System.out.println("--------------------  Testing Version List Retrieval ----------------------");
		Async testAsync = context.async();
		ObjectGenerator.buildAPITask(taskManager, "FirstTask", mongo_client)
		.onSuccess(apiTask -> {
			taskManager.getTagListForElement(apiTask.getUUID())
			.onSuccess(VersionList -> {
				// No tags have been added yet.
				context.assertEquals(0, VersionList.size());				
				testAsync.complete();
			})
			.onFailure(err -> context.fail(err));

		})		
		.onFailure(err ->{
			context.fail(err);
		});	

	}	
}
