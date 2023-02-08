package fi.abo.kogni.soile2.projecthandling.utils;

import java.io.File;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fi.abo.kogni.soile2.http_server.SoileWebTest;
import fi.abo.kogni.soile2.projecthandling.apielements.APIExperiment;
import fi.abo.kogni.soile2.projecthandling.apielements.APIProject;
import fi.abo.kogni.soile2.projecthandling.apielements.APITask;
import fi.abo.kogni.soile2.projecthandling.projectElements.impl.ElementManager;
import fi.abo.kogni.soile2.projecthandling.projectElements.impl.Experiment;
import fi.abo.kogni.soile2.projecthandling.projectElements.impl.Project;
import fi.abo.kogni.soile2.projecthandling.projectElements.impl.Task;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.http.impl.MimeMapping;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientSession;

/**
 * THIS CLASS IS NOT INTENDED TO BE USED IN THE FINAL PRODUCT, IT IS A HELPER CLASS FOR TESTS TO CREATE OBJECTS
 * Helper Class to generate a couple of Objects (mainly for testing)
 * @author Thomas Pfau
 *
 */
public class WebObjectCreator {

	private static final Logger LOGGER = LogManager.getLogger(WebObjectCreator.class);

	
	// The session needs to already be authenticated have all headers set.
	public static Future<JsonObject> createTask(WebClient webClient, String elementID)
	{
		Promise<JsonObject> taskPromise = Promise.promise();
		try
		{					
			JsonObject TaskDef = new JsonObject(Files.readString(Paths.get(WebObjectCreator.class.getClassLoader().getResource("APITestData/TaskData.json").getPath()))).getJsonObject(elementID);			
			String TaskCode = Files.readString(Paths.get(WebObjectCreator.class.getClassLoader().getResource("CodeTestData/" + TaskDef.getString("codeFile")).getPath()));
			String TestDataFolder = WebObjectCreator.class.getClassLoader().getResource("FileTestData").getPath();
			JsonObject taskParameters = new JsonObject().put("codeType", TaskDef.getString("codeType")).put("codeVersion", TaskDef.getString("codeVersion")).put("name", TaskDef.getString("name"));			
			SoileWebTest.createNewElement(webClient, elementID, taskParameters)
			.onSuccess(TaskJson -> { // we have created a new Task Object. Now we need to fill it with data
				String taskID = TaskJson.getString("UUID");
				JsonArray resources = TaskDef.getJsonArray("resources", new JsonArray());
				List<Future> composite = new LinkedList<>();
				Promise<String> versionPromise = Promise.promise();
				Future<String> versionFuture = versionPromise.future();
				LinkedList<Future<String>> chain = new LinkedList<>();
				chain.add(versionFuture);
				for(int i = 0; i < resources.size(); ++i)
				{					
					// create all in a compose chain...
					String resourceName = resources.getString(i);
					chain.add(chain.getLast().compose(newVersion -> {
						LOGGER.debug("Adding File " + resourceName + " to Version " + newVersion);
						return SoileWebTest.postTaskRessource(webClient, taskID, newVersion, resourceName,new File(Path.of(TestDataFolder, resourceName).toString()) , MimeMapping.getMimeTypeForFilename(resourceName) );										
					}));
					composite.add(chain.getLast());
				}
				versionPromise.complete(TaskJson.getString("version"));
				CompositeFuture.all(composite)
				.onSuccess(done -> {
					LOGGER.debug("File(s) added");
					chain.getLast().onSuccess(latestVersion ->
					{
						//This is the version with all Files added. 
						JsonObject update = new JsonObject();
						update.put("code", TaskCode);
						update.put("private", TaskDef.getBoolean("private"));
						
						
					})
					.onFailure(err -> taskPromise.fail(err));
				})
				.onFailure(err -> taskPromise.fail(err));
			})
			.onFailure(err -> taskPromise.fail(err));
		}
		catch(IOException e)
		{
			taskPromise.fail(e);
		}
		return taskPromise.future();
	}
		/*	manager.createOrLoadElement(TaskDef.getString("name"),TaskDef.getString("codeType"),TaskDef.getString("codeVersion"))
			.onSuccess(task -> {
				LOGGER.debug("Task object created");
				
				
				versionPromise.complete(task.getCurrentVersion());
				CompositeFuture.all(composite)
				.onSuccess(done -> {
					
					chain.getLast().onSuccess(latestVersion ->
					{
					APITask apiTask = new APITask(TaskDef);				
					apiTask.loadGitJson(TaskDef);
					apiTask.setCode(TaskCode);
					apiTask.setVersion(latestVersion);
					apiTask.setUUID(task.getUUID());			
					task.setPrivate(apiTask.getPrivate());				
					task.save(client)
					.onSuccess(res -> {
						LOGGER.debug("Task saved");

						manager.updateElement(apiTask)
						.onSuccess(newVersion -> {
							LOGGER.debug("Api Task Updated");
							apiTask.setVersion(newVersion);
							try {
								FileUtils.deleteDirectory(new File(dataDir));
							}
							catch(IOException e)
							{
								
							}
							taskPromise.complete(apiTask);							
						})
						.onFailure(err -> taskPromise.fail(err));

					})
					.onFailure(err -> taskPromise.fail(err));
					})
					.onFailure(err -> taskPromise.fail(err));
				})
				.onFailure(err -> taskPromise.fail(err));
				
			})
			.onFailure(fail -> taskPromise.fail(fail));
		}
		catch(IOException e)
		{
			taskPromise.fail(e);
		}
		return taskPromise.future();
	}

	public static Future<APIExperiment> buildAPIExperiment(ElementManager<Experiment> experimentManager,ElementManager<Task> taskManager,MongoClient client, String experimentName)
	{
		Promise<APIExperiment> experimentPromise = Promise.promise();
		try
		{
			JsonObject ExperimentDef = new JsonObject(Files.readString(Paths.get(WebObjectCreator.class.getClassLoader().getResource("APITestData/ExperimentData.json").getPath()))).getJsonObject(experimentName);
			APIExperiment apiExperiment = new APIExperiment();
			apiExperiment.setPrivate(ExperimentDef.getBoolean("private"));
			experimentManager.createOrLoadElement(ExperimentDef.getString("name"))
			.onSuccess(experiment -> {	
				experiment.setPrivate(apiExperiment.getPrivate());
				apiExperiment.setName(experiment.getName());
				apiExperiment.setVersion(experiment.getCurrentVersion());
				apiExperiment.setUUID(experiment.getUUID());				

				ConcurrentHashMap<String, JsonObject> elements = new ConcurrentHashMap();
				List<Future> partFutures = new LinkedList<Future>();
				for(Object item : ExperimentDef.getJsonArray("items"))
				{
					JsonObject current = (JsonObject) item;
					if(current.getString("type").equals("task"))
					{
						partFutures.add(
								buildAPITask(taskManager, current.getString("name"), client).onSuccess(task -> {
									experiment.addElement(task.getUUID());
									JsonObject taskInstance = new JsonObject();
									taskInstance.put("instanceID", current.getString("instanceID"))
									.put("next", current.getString("next", null))
									.put("UUID", task.getUUID())
									.put("version", task.getVersion())
									.put("filter", current.getString("filter",""))
									.put("name", task.getName())
									.put("outputs", current.getJsonArray("outputs",new JsonArray()));
									elements.put(current.getString("name"), new JsonObject().put("elementType", "task")
											.put("data",taskInstance));
								})
								);
					}
					if(current.getString("type").equals("filter"))
					{
						elements.put(current.getString("name"), new JsonObject().put("elementType", "filter")
								.put("data",current.getJsonObject("data")));
					}					
					if(current.getString("type").equals("experiment"))
					{
						partFutures.add(
								buildAPIExperiment(experimentManager, taskManager, client, current.getString("name")).onSuccess(subexperiment -> {
									experiment.addElement(subexperiment.getUUID());
									JsonObject experimentInstance = new JsonObject();
									experimentInstance.put("instanceID", current.getString("instanceID"))
									.put("next", current.getString("next", null))
									.put("UUID", subexperiment.getUUID())
									.put("version", subexperiment.getVersion())
									.put("randomize", current.getBoolean("randomize",false))
									.put("name", subexperiment.getName());
									elements.put(current.getString("name"), 
											new JsonObject().put("elementType", "experiment")
											.put("data", experimentInstance));
								})
								);

					}
				}
				//deploymentFutures.add(Future.<String>future(promise -> vertx.deployVerticle("js:templateManager.js", opts, promise)));
				CompositeFuture.all(partFutures).mapEmpty().onSuccess(Void -> {
					// once all is done, we put it in in the right order.
					for(Object item : ExperimentDef.getJsonArray("items"))
					{
						JsonObject current = (JsonObject) item;

						apiExperiment.getElements().add(elements.get(current.getString("name")));
					}
					experimentManager.updateElement(apiExperiment)
					.onSuccess(newVersion -> { 
						apiExperiment.setVersion(newVersion);
						experimentPromise.complete(apiExperiment);
					})
					.onFailure(err -> {
						experimentPromise.fail(err);
					});

				})
				.onFailure(err -> {
					experimentPromise.fail(err);
				});
			})
			.onFailure(fail -> experimentPromise.fail(fail));
		}
		catch(IOException e)
		{
			experimentPromise.fail(e);
		}
		return experimentPromise.future();
	}


	public static Future<APIProject> buildAPIProject(ElementManager<Project> projectManager, 
													 ElementManager<Experiment> expManager,
													 ElementManager<Task> taskManager, 
													 MongoClient client, String projectName)
	{
		Promise<APIProject> projectPromise = Promise.promise();
		try
		{
			JsonObject projectDef = new JsonObject(Files.readString(Paths.get(WebObjectCreator.class.getClassLoader().getResource("APITestData/ProjectData.json").getPath()))).getJsonObject(projectName);
			APIProject apiProject = new APIProject();		
			apiProject.setStart(projectDef.getString("start"));
			projectManager.createOrLoadElement(projectDef.getString("name"))
			.onSuccess(project -> {							
				apiProject.setName(project.getName());
				apiProject.setVersion(project.getCurrentVersion());
				apiProject.setUUID(project.getUUID());							
				ConcurrentHashMap<String, JsonObject> tasks = new ConcurrentHashMap();
				List<Future> taskFutures = new LinkedList<Future>();
				for(Object item : projectDef.getJsonArray("tasks"))
				{
					JsonObject current = (JsonObject) item;
					taskFutures.add(
							buildAPITask(taskManager, current.getString("name"), client)
							.onSuccess(task -> {
								project.addElement(task.getUUID());
								JsonObject taskInstance = new JsonObject();
								taskInstance.put("instanceID", current.getString("instanceID"))
								.put("next", current.getString("next", null))
								.put("UUID", task.getUUID())
								.put("version", task.getVersion())
								.put("filter", current.getString("filter",""))
								.put("name", task.getName())
								.put("outputs", current.getJsonArray("outputs",new JsonArray()));
								tasks.put(current.getString("name"), taskInstance);
							})
							);
				}
				CompositeFuture.all(taskFutures).mapEmpty().onSuccess(Void -> {
					LinkedList<JsonObject> taskList = new LinkedList<JsonObject>();
					taskList.addAll(tasks.values());
					JsonArray taskArray = new JsonArray(taskList);
					apiProject.setTasks(taskArray);
					// and now we do the experiments. Since they could in theory refer back to the same unique tasks, we need to have created the tasks first.
					ConcurrentHashMap<String, JsonObject> experiments = new ConcurrentHashMap();
					List<Future> experimentFutures = new LinkedList<Future>();
					// and for filters. This should work even without
					JsonArray filters = apiProject.getFilters();
					for(Object item : projectDef.getJsonArray("filters", new JsonArray()))
					{
						filters.add(item);
					}
					for(Object item : projectDef.getJsonArray("experiments"))
					{
						JsonObject current = (JsonObject) item;
						experimentFutures.add(
								buildAPIExperiment(expManager, taskManager, client, current.getString("name"))
								.onSuccess(experiment -> {
									project.addElement(experiment.getUUID());						
									JsonObject expinstance = experiment.getAPIJson();
									expinstance.put("instanceID",current.getString("instanceID"))
									.put("next", current.getString("next", null))
									.put("random", current.getBoolean("random", true));
									experiments.put(current.getString("name"), expinstance);								
								})
								);						
					}
					// once the futures are set up, wait for them do be done and then finish up.
					CompositeFuture.all(experimentFutures).mapEmpty()
					.onSuccess(Void2 -> {
						LinkedList<JsonObject> expList = new LinkedList<JsonObject>();
						expList.addAll(experiments.values());
						JsonArray expArray = new JsonArray(expList);
						apiProject.setExperiments(expArray);
						projectManager.updateElement(apiProject)
						.onSuccess( newVersion -> {
								apiProject.setVersion(newVersion);
								// Saving the associated project.								
								projectPromise.complete(apiProject);															
							})
							.onFailure(err -> projectPromise.fail(err));
						})
						.onFailure(err -> projectPromise.fail(err));
					

				})
				.onFailure(err -> projectPromise.fail(err));

				//deploymentFutures.add(Future.<String>future(promise -> vertx.deployVerticle("js:templateManager.js", opts, promise)));

			})
			.onFailure(fail -> projectPromise.fail(fail));
		}
		catch(IOException e)
		{
			projectPromise.fail(e);
		}
		return projectPromise.future();
	}
	
	public static Future<JsonObject> createProject(MongoClient client, Vertx vertx, String projectName)
	{
		ElementManager<Project> projectManager = new ElementManager<>(Project::new, APIProject::new, client, vertx); 
		 ElementManager<Experiment> expManager = new ElementManager<>(Experiment::new, APIExperiment::new, client, vertx);
		 ElementManager<Task> taskManager = new ElementManager<>(Task::new, APITask::new, client, vertx);
		 return buildAPIProject(projectManager, expManager, taskManager, client, projectName).map(res -> { return new JsonObject().put("UUID", res.getUUID()).put("version", res.getVersion());});
	}			
*/	
}
