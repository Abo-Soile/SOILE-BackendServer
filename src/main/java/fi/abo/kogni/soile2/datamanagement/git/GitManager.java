package fi.abo.kogni.soile2.datamanagement.git;

import java.io.File;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


import fi.aalto.scicomp.gitFs.gitProviderVerticle;
import fi.abo.kogni.soile2.experiment.task.Task;
import fi.abo.kogni.soile2.projecthandling.projectElements.Experiment;
import fi.abo.kogni.soile2.projecthandling.projectElements.Project;
import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;

/**
 * This class manages access to the {@link gitProviderVerticle}s via the eventbus..
 * It only communicates with the gitProviderVerticle providing some utility functions.
 * @author Thomas Pfau
 *
 */
public class GitManager {

	EventBus eb;
	private static final Logger log = LogManager.getLogger(GitManager.class.getName());	

	public GitManager(EventBus eb)
	{
		this.eb = eb;
	}
	
	/**
	 * Test whether a repo element exists asynchronosly
	 * @param elementID the ID (Project, Task or ExperimentID)
	 * @return a Future that on success indicates whether the element exists or not. Failure means either the request was inconsistent, or something went wrong 
	 * 		   in the process, and should not be taken as non existence indication. 
	 */
	public Future<Boolean> doesRepoExist(String elementID)
	{
		Promise<Boolean> existPromise = Promise.<Boolean>promise();
		eb.request(SoileConfigLoader.getServerProperty("gitVerticleAddress"), gitProviderVerticle.createExistRepoCommand(elementID))
		.onSuccess( reply ->
		{
			existPromise.complete(((JsonObject)reply.body()).getBoolean(gitProviderVerticle.DATAFIELD));							
		})
		.onFailure(fail ->
		{
			existPromise.fail(fail);
		});
		return existPromise.future();
	}
	
	
	/**
	 * Initialize a repository
	 * @param elementID the ID (Project, Task or ExperimentID)
	 * @return a Future that on success returns the current Version of the (empty) repository. Fails if the repository exists.    
	 */
	public Future<String> initRepo(String elementID)
	{
		Promise<String> initPromise = Promise.<String>promise();
		eb.request(SoileConfigLoader.getServerProperty("gitVerticleAddress"), gitProviderVerticle.createExistRepoCommand(elementID))
		.onSuccess( reply ->
		{
			JsonObject replyObject = (JsonObject) reply.body();
			if(replyObject.getBoolean(gitProviderVerticle.DATAFIELD))
			{
				// The repository exists.
				initPromise.fail("The requested Repository already exists");	
			}
			else
			{
				eb.request(SoileConfigLoader.getServerProperty("gitVerticleAddress"), gitProviderVerticle.createInitCommand(elementID))
				.onSuccess(initReply -> {
					JsonObject info = (JsonObject)initReply.body();
					// return the new version of this repository (for future changes)
					initPromise.complete(info.getString(gitProviderVerticle.COMMITHASHFIELD));
				})
				.onFailure(err -> 
				{
					initPromise.fail(err);
				});
			}
										
		})
		.onFailure(fail ->
		{
			initPromise.fail(fail);
		});
		return initPromise.future();
	}
	/*
	public Future<Boolean> deleteRepo(String elementID)
	{
		
	}*/


	/**
	 * Get the file contents of a file in the github repository, these are all just json/linker files). 
	 * @param fileName The name of the file
	 * @param taskID The task the file belongs to 
	 * @param taskVersion the version of the file.
	 * @return A String with the contents that can normally be parsed as json.
	 */
	public Future<String> getGitFileContents(GitFile file)
	{
		if(!gitFileValid(file))
		{
			return Future.failedFuture("Supplied File was invalid");
		}
		Promise<String> dataPromise = Promise.<String>promise();
		eb.request(SoileConfigLoader.getServerProperty("gitVerticleAddress"), gitProviderVerticle.createGetCommand(file.getRepoID(),file.getRepoVersion(),file.getFileName()))
		.onSuccess( reply ->
		{
			log.debug(reply.body());
			dataPromise.complete(((JsonObject)reply.body()).getString(gitProviderVerticle.DATAFIELD));							
		})
		.onFailure(fail ->
		{
			dataPromise.fail(fail);
		});
		return dataPromise.future();	
	}
	
	/**
	 * Get the file contents of a file in the github repository, these are all just json/linker files). 
	 * @param fileName The name of the file
	 * @param taskID The task the file belongs to 
	 * @param taskVersion the version of the file.
	 * @return A String with the contents that can normally be parsed as json.
	 */
	public Future<String> getGitResourceContents(GitFile file)
	{
		return getGitFileContents(new GitFile("resources" + File.separator + file.getFileName(), file.getRepoID(), file.getRepoVersion()));
	}
	/**
	 * Get the file contents of a git file as a Json Object. 
	 * @return A {@link JsonObject} of the contents of the git file.
	 */
	public Future<JsonObject> getGitResourceContentsAsJson(GitFile file)
	{
		if(!gitFileValid(file))
		{
			return Future.failedFuture("Supplied File was invalid");
		}
		Promise<JsonObject> dataPromise = Promise.<JsonObject>promise();		
		getGitResourceContents(file).onSuccess(jsonString -> {
			try 
			{
				JsonObject data = new JsonObject(jsonString);
				dataPromise.complete(data);
			}
			catch(Exception e)
			{
				
				dataPromise.fail(e);
			}
		}).onFailure(fail ->{
			dataPromise.fail(fail);
		});
		return dataPromise.future();
	}
	
	/**
	 * Get the file contents of a git file as a Json Object. 
	 * @return A {@link JsonObject} of the contents of the git file.
	 */
	public Future<JsonObject> getGitFileContentsAsJson(GitFile file)
	{
		if(!gitFileValid(file))
		{
			return Future.failedFuture("Supplied File was invalid");
		}
		Promise<JsonObject> dataPromise = Promise.<JsonObject>promise();		
		getGitFileContents(file).onSuccess(stringData ->{
			try
			{
				JsonObject data = new JsonObject(stringData);
				dataPromise.complete(data);
			}
			catch(Exception e)
			{
				e.printStackTrace(System.out);
				dataPromise.fail(e);
			}
			
		}).onFailure(fail -> 
		{
			dataPromise.fail(fail);
		});	
		return dataPromise.future();			
	}

	/**
	 * Write data to a file specified by the {@link GitFile}, receive the new version of the respective file. 
	 * @param file the GitFile containing name (including folders),  
	 * @param data
	 * @return
	 */
	public Future<String> writeGitFile(GitFile file, String data)
	{
		Promise<String> versionPromise = Promise.<String>promise();
		if(!gitFileValid(file))
		{
			return Future.failedFuture("Supplied File was invalid");
		}
		JsonObject command = gitProviderVerticle.createWriteCommand(file.getRepoID(), file.getRepoVersion(), data, file.getFileName());
		eb.request(SoileConfigLoader.getServerProperty("gitVerticleAddress"), command).onSuccess(reply -> {
			JsonObject info = (JsonObject)reply.body();
			// return the new version of this repository (for future changes)
			versionPromise.complete(info.getString(gitProviderVerticle.COMMITHASHFIELD));
		}).onFailure(fail -> {
			versionPromise.fail(fail);
		});
		return versionPromise.future();	
	}
	
	/**
	 * Same as writeGitFile with a string, but encodes the provided Json as a pretty string.
	 * @param file the file indicating where to write to.
	 * @param data the data that should be encoded in the file
	 * @return the new version of the file.
	 */
	public Future<String> writeGitFile(GitFile file, JsonObject data)
	{
		System.out.println(data);
		return writeGitFile(file, data.encodePrettily());
	}
		
	/**
	 * Write data to a file specified by the {@link GitFile} in the resources folder of the repo.  
	 * @param file the GitFile containing name (including folders) but excluding the resources folder,  
	 * @param data the data to be written to the file.
	 * @return a Future with the version of the git repository after execution.
	 */
	public Future<String> writeGitResourceFile(GitFile file, String data)
	{
		return writeGitFile(new GitFile("resources" + File.separator + file.getFileName(), file.getRepoID(), file.getRepoVersion()), data);
	}
	
	/**
	 * Same as writeGitFile with a string, but encodes the provided Json as a pretty string.
	 * @param file the file indicating where to write to.
	 * @param data the data that should be encoded in the file
	 * @return the new version of the file.
	 */
	public Future<String> writeGitResourceFile(GitFile file, JsonObject data)
	{
		return writeGitResourceFile(file, data.encodePrettily());
	}

	public boolean gitFileValid(GitFile file)
	{
		if(file.getRepoID() == null)
		{
			return false;
		}
		if(file.getRepoVersion() == null)
		{
			return false;
		}
		if(file.getFileName() == null)
		{
			return false;
		}
		return true;
	}
	
	
	public static JsonObject buildBasicGitElement(String name, Class type)
	{
		if( Project.class.equals(type)) {
			return buildBasicGitProject(name);
		}
		else if(Task.class.equals(type)) {
			return buildBasicGitTask(name);
		}
		else if(Experiment.class.equals(type))
		{
			return buildBasicGitExperiment(name);
		}
		else
		{
			return buildBasicGitObject(name);
		}
	}
	
	
	/**
	 * A Basic Github object with a given name.
	 * @param name
	 * @return
	 */
	public static JsonObject buildBasicGitObject(String name)
	{
		return new JsonObject().put("name", name);
	}
	
	/**
	 * Build an empty github Project object
	 * @param name
	 * @return
	 */
	public static JsonObject buildBasicGitProject(String name)
	{
		return buildBasicGitObject(name).put("tasks", new JsonArray()).put("experiments", new JsonArray()).put("filters", new JsonArray()).put("start",  "");
	}

	/**
	 * Build an empty github Task object
	 * @param name
	 * @return
	 */
	public static JsonObject buildBasicGitTask(String name)
	{
		return buildBasicGitObject(name).put("code", "").put("codetype", "").put("resources", new JsonArray());
	}
	
	/**
	 * Build an empty github experiment
	 * @param name
	 * @return
	 */
	public static JsonObject buildBasicGitExperiment(String name)
	{
		return buildBasicGitObject(name).put("elements", new JsonArray());
	}
}
