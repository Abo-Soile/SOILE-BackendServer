package fi.abo.kogni.soile2.projecthandling.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fi.abo.kogni.soile2.http_server.SoileWebTest;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.impl.MimeMapping;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClientSession;
import io.vertx.ext.web.handler.HttpException;

/**
 * THIS CLASS IS NOT INTENDED TO BE USED IN THE FINAL PRODUCT, IT IS A HELPER CLASS FOR TESTS TO CREATE OBJECTS
 * Helper Class to generate a couple of Objects (mainly for testing)
 * @author Thomas Pfau
 *
 */
public class WebObjectCreator {

	private static final Logger LOGGER = LogManager.getLogger(WebObjectCreator.class);

	
	/**
	 * Create a new Task or retrieve the data for the latest version of the task.
	 * @param webClient
	 * @param elementID
	 * @return
	 */
	public static Future<JsonObject> createOrRetrieveTask(WebClientSession webClient, String elementID)
	{				
			Promise<JsonObject> infoPromise = Promise.promise();
			createTask(webClient, elementID)
			.onSuccess(res -> {
				SoileWebTest.getElement(webClient, "task", res.getString("UUID"), res.getString("version"))
				.onSuccess(json -> infoPromise.complete(json))
				.onFailure(err -> infoPromise.fail(err));
			})
			.onFailure(err -> {
				finishPromiseOnError(webClient, elementID, "task", err, infoPromise);
			});
			return infoPromise.future();
	}
	
	// The session needs to already be authenticated have all headers set.
	static Future<JsonObject> createTask(WebClientSession webClient, String elementID)
	{
		Promise<JsonObject> taskPromise = Promise.promise();
		try
		{					
			JsonObject TaskDef = new JsonObject(Files.readString(Paths.get(WebObjectCreator.class.getClassLoader().getResource("APITestData/TaskData.json").getPath()))).getJsonObject(elementID);			
			String TaskCode = Files.readString(Paths.get(WebObjectCreator.class.getClassLoader().getResource("CodeTestData/" + TaskDef.getString("codeFile")).getPath()));
			String TestDataFolder = WebObjectCreator.class.getClassLoader().getResource("FileTestData").getPath();
			JsonObject codeType = TaskDef.getJsonObject("codeType");
			JsonObject taskParameters = new JsonObject().put("codeType", codeType.getString("language")).put("codeVersion", codeType.getString("version")).put("name", TaskDef.getString("name"));			
			SoileWebTest.createNewElement(webClient, "task", taskParameters)
			.onSuccess(TaskJson -> { // we have created a new Task Object. Now we need to fill it with data
				LOGGER.debug("Task " + elementID + " Initialized: " + TaskJson.getString("UUID") + "@" + TaskJson.getString("version"));
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
						return SoileWebTest.postTaskRessource(webClient, taskID, newVersion, resourceName,new File(Path.of(TestDataFolder, resourceName).toString()) , MimeMapping.getMimeTypeForFilename(resourceName) );										
					}));
					composite.add(chain.getLast());
				}
				versionPromise.complete(TaskJson.getString("version"));
				CompositeFuture.all(composite)
				.onSuccess(done -> {					
					chain.getLast().onSuccess(latestVersion ->
					{
						//This is the version with all Files added. 
						JsonObject update = new JsonObject();
						update.put("code", TaskCode);
						update.put("private", TaskDef.getBoolean("private", false));						
						SoileWebTest.POST(webClient, "/task/" + taskID + "/" + latestVersion , null, update)
						.onSuccess(response -> {
							LOGGER.debug("Task " + elementID + " created");
							taskPromise.complete(response.bodyAsJsonObject().put("name", TaskDef.getString("name"))
																			.put("UUID", taskID)
																			.put("private", TaskDef.getBoolean("private", false))
																			.put("codeType", codeType)
																			.put("code", TaskCode)
																			.put("resources", resources));
						}).onFailure(err -> taskPromise.fail(err));
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
	
	public static Future<JsonObject> createOrRetrieveExperiment(WebClientSession webClient, String experimentName)
	{
		Promise<JsonObject> infoPromise = Promise.promise();
		createExperiment(webClient, experimentName)
		.onSuccess(res -> {
			SoileWebTest.getElement(webClient, "experiment", res.getString("UUID"), res.getString("version"))
			.onSuccess(json -> infoPromise.complete(json))
			.onFailure(err -> infoPromise.fail(err));
		})
		.onFailure(err -> {
			finishPromiseOnError(webClient, experimentName,"experiment", err, infoPromise);
		});
		return infoPromise.future();
	}
	
	
	static void finishPromiseOnError(WebClientSession session, String elementName, String elementType, Throwable err, Promise<JsonObject> promise)
	{
		if(err instanceof HttpException){
			if(((HttpException)err).getStatusCode() == 409)
			{
				SoileWebTest.retrieveElementByName(session, elementName, elementType)
				.onSuccess(res -> promise.complete(res))
				.onFailure(err2 -> promise.fail(err2));				
			}
			else
			{
				promise.fail(err);
			}
		}
		else
		{
			promise.fail(err);
		}
	}
		 
	
	
	public static Future<JsonObject> createExperiment(WebClientSession webClient, String experimentName)
	{
		Promise<JsonObject> experimentPromise = Promise.promise();
		try
		{
			JsonObject ExperimentDef = new JsonObject(Files.readString(Paths.get(WebObjectCreator.class.getClassLoader().getResource("APITestData/ExperimentData.json").getPath()))).getJsonObject(experimentName);
			JsonObject experimentSettings = new JsonObject().put("name", ExperimentDef.getString("name")).put("private", ExperimentDef.getBoolean("private", false));
			SoileWebTest.createNewElement(webClient, "experiment", experimentSettings)
			.onSuccess(experimentJson -> {
				LOGGER.debug("Experiment " + experimentName + " Initialized: " + experimentJson.getString("UUID") + "@" + experimentJson.getString("version"));
				String id = experimentJson.getString("UUID");
				String version = experimentJson.getString("version");								
				ConcurrentHashMap<String, JsonObject> elements = new ConcurrentHashMap();
				List<Future> partFutures = new LinkedList<Future>();
				for(Object item : ExperimentDef.getJsonArray("items"))
				{
					JsonObject current = (JsonObject) item;
					if(current.getString("type").equals("task"))
					{
						partFutures.add(
								createOrRetrieveTask(webClient, current.getString("name"))
								.onSuccess(taskInstance -> {																																			
									taskInstance.put("instanceID", current.getString("instanceID"))
									.put("next", current.getString("next", "end"))									
									.put("filter", current.getString("filter",""))									
									.put("outputs", current.getJsonArray("outputs",new JsonArray()));
									taskInstance.remove("code");
									elements.put(current.getString("instanceID"), new JsonObject().put("elementType", "task")
											.put("data",taskInstance));
								})
								);
					}
					if(current.getString("type").equals("filter"))
					{
						elements.put(current.getString("instanceID"), new JsonObject().put("elementType", "filter")
								.put("data",current.getJsonObject("data").put("instanceID", current.getString("instanceID"))));
					}					
					if(current.getString("type").equals("experiment"))
					{
						partFutures.add(
								createOrRetrieveExperiment(webClient,current.getString("name")).onSuccess(subexperiment -> {
									
									JsonObject experimentInstance = new JsonObject();
									experimentInstance.put("instanceID", current.getString("instanceID"))
									.put("next", current.getString("next", null))
									.put("UUID", subexperiment.getString("UUID"))
									.put("version", subexperiment.getString("version"))
									.put("randomize", current.getBoolean("randomize",false))
									.put("name", subexperiment.getString("name"))
									.put("elements", subexperiment.getJsonArray("elements"));
									elements.put(current.getString("instanceID"), 
											new JsonObject().put("elementType", "experiment")
											.put("data", experimentInstance));
								})
								);

					}
				}
				//deploymentFutures.add(Future.<String>future(promise -> vertx.deployVerticle("js:templateManager.js", opts, promise)));
				CompositeFuture.all(partFutures).mapEmpty().onSuccess(Void -> {
					// once all is done, we put it in in the right order.
					JsonArray items = experimentJson.getJsonArray("elements");
					for(Object item : ExperimentDef.getJsonArray("items"))
					{
						JsonObject current = (JsonObject) item;
						items.add(elements.get(current.getString("instanceID")));
					}
					SoileWebTest.POST(webClient, "/experiment/" + id + "/" + version , null, experimentJson)					
					.onSuccess(response -> {						
						SoileWebTest.GET(webClient, "/experiment/" + id + "/" + response.bodyAsJsonObject().getString("version") , null, null)
						.onSuccess(res -> {
							LOGGER.debug("Experiment " + experimentName +  "  created and retrieved" );							
							experimentPromise.complete(res.bodyAsJsonObject());
						})
						.onFailure(err -> {
							experimentPromise.fail(err);
						});
						
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

	public static Future<JsonObject> createProject(WebClientSession webClient, String projectName)
	{
		Promise<JsonObject> projectPromise = Promise.promise();
		try
		{
			JsonObject projectDef = new JsonObject(Files.readString(Paths.get(WebObjectCreator.class.getClassLoader().getResource("APITestData/ProjectData.json").getPath()))).getJsonObject(projectName);
			JsonObject projectSettings = new JsonObject().put("name", projectDef.getString("name"))
														 .put("private", projectDef.getBoolean("private", false));
			SoileWebTest.createNewElement(webClient, "project", projectSettings)			
			.onSuccess(projectJson -> {							
				String id = projectJson.getString("UUID");
				String version = projectJson.getString("version");
				JsonArray projectTasks = projectJson.getJsonArray("tasks");
				JsonArray projectFilters = projectJson.getJsonArray("filters");
				JsonArray projectExperiments = projectJson.getJsonArray("experiments");
				ConcurrentHashMap<String, JsonObject> tasks = new ConcurrentHashMap();
				List<Future> taskFutures = new LinkedList<Future>();
				for(Object item : projectDef.getJsonArray("tasks"))
				{
					JsonObject current = (JsonObject) item;
					taskFutures.add(
							createOrRetrieveTask(webClient,current.getString("name"))
							.onSuccess(task -> {
								task.put("instanceID", current.getString("instanceID"));
								task.put("next", current.getString("next"));
								task.put("outputs", current.getJsonArray("outputs", new JsonArray()));
								tasks.put(current.getString("instanceID"), task);
							})
					);
				}
				CompositeFuture.all(taskFutures).mapEmpty().onSuccess(Void -> {
					// all tasks created. now we add them to the project;
					LinkedList<JsonObject> taskList = new LinkedList<JsonObject>();
					taskList.addAll(tasks.values());
					JsonArray taskArray = new JsonArray(taskList);
					projectTasks.addAll(taskArray);
					// and now we do the experiments. Since they could in theory refer back to the same unique tasks, we need to have created the tasks first.
					ConcurrentHashMap<String, JsonObject> experiments = new ConcurrentHashMap();
					List<Future> experimentFutures = new LinkedList<Future>();
					// and for filters. This should work even without					
					for(Object item : projectDef.getJsonArray("filters", new JsonArray()))
					{
						projectFilters.add(item);
					}
					for(Object item : projectDef.getJsonArray("experiments"))
					{
						JsonObject current = (JsonObject) item;
						experimentFutures.add(
								createOrRetrieveExperiment(webClient, current.getString("name"))
								.onSuccess(experiment -> {
									
									
									experiment.put("instanceID",current.getString("instanceID"))
									.put("next", current.getString("next", "end"))
									.put("random", current.getBoolean("random", true));
									experiments.put(current.getString("instanceID"), experiment);								
								})
								);						
					}
					// once the futures are set up, wait for them do be done and then finish up.
					CompositeFuture.all(experimentFutures).mapEmpty()
					.onSuccess(Void2 -> {
						LinkedList<JsonObject> expList = new LinkedList<JsonObject>();
						expList.addAll(experiments.values());
						JsonArray expArray = new JsonArray(expList);
						projectExperiments.addAll(expArray);
						SoileWebTest.POST(webClient, "/project/" + id + "/" + version , null, projectJson)					
						.onSuccess(response -> {						
							SoileWebTest.GET(webClient, "/project/" + id + "/" + response.bodyAsJsonObject().getString("version") , null, null)
							.onSuccess(res -> {														
								projectPromise.complete(res.bodyAsJsonObject());
							})
							.onFailure(err -> {
								projectPromise.fail(err);
							});
							
						})
						.onFailure(err -> {
							projectPromise.fail(err);
						});
					})
					.onFailure(err -> {
						projectPromise.fail(err);
					});	

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
	
}