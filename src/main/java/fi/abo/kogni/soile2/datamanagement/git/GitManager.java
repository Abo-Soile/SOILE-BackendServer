package fi.abo.kogni.soile2.datamanagement.git;

import java.io.File;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fi.aalto.scicomp.gitFs.gitProviderVerticle;
import fi.abo.kogni.soile2.http_server.auth.SoileAuthorization.TargetElementType;
import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.ReplyException;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * This class manages access to the {@link gitProviderVerticle}s via the eventbus..
 * It only communicates with the gitProviderVerticle providing some utility functions.
 * TODO: Create Tags for an active save command. 
 * TODO: Convert into a Verticle ?
 * @author Thomas Pfau
 *
 */
public class GitManager{

	EventBus eb;
	
	public GitManager(EventBus eb)
	{
		this.eb = eb;
	}
	private static final Logger LOGGER = LogManager.getLogger(GitManager.class.getName());
	public static String resourceFolder = "resources";

	
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
			if(fail instanceof ReplyException)
			{
				ReplyException ex = (ReplyException) fail;
				if(ex.failureCode() == gitProviderVerticle.REPO_DOES_NOT_EXIST)
				{
					existPromise.complete(false);
				}
				else
				{
					existPromise.fail(fail);
				}
			}
			else
			{
				existPromise.fail(fail);
			}
		});
		return existPromise.future();
	}
	
	
	/**
	 * Test whether a repo exists and the given version is in the repo asynchronosly
	 * @param elementID the ID (Project, Task or ExperimentID)
	 * @param version The version of that should be checked.
	 * @return a Future that on success indicates whether the element exists or not. Failure means either the request was inconsistent, or something went wrong 
	 * 		   in the process, and should not be taken as non existence indication. 
	 */
	public Future<Boolean> doesRepoAndVersionExist(GitElement element)
	{
		Promise<Boolean> existPromise = Promise.<Boolean>promise();
		eb.request(SoileConfigLoader.getServerProperty("gitVerticleAddress"), gitProviderVerticle.createCommandForRepoAtVersion(element.getRepoID(), element.getRepoVersion()).put(gitProviderVerticle.COMMANDFIELD, gitProviderVerticle.VERSION_EXISTS_COMMAND))
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
					initPromise.complete(info.getString(gitProviderVerticle.VERSIONFIELD));
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

	/**
	 * Delete a repository
	 * @param elementID the ID (Project, Task or ExperimentID)
	 * @return a Future that on success returns the current Version of the (empty) repository. Fails if the repository exists.    
	 */
	public Future<String> deleteRepo(String elementID)
	{
		Promise<String> deletePromise = Promise.<String>promise();
		eb.request(SoileConfigLoader.getServerProperty("gitVerticleAddress"), gitProviderVerticle.createCommandForRepo(elementID).put(gitProviderVerticle.COMMANDFIELD, gitProviderVerticle.DELETE_REPO_COMMAND))
		.onSuccess( reply ->
		{
			JsonObject replyObject = (JsonObject) reply.body();
			if(replyObject.getBoolean(gitProviderVerticle.DATAFIELD))
			{
				deletePromise.complete();	
			}
			else
			{
				deletePromise.fail("Repo not deleted.");
			}
										
		})
		.onFailure(fail ->
		{
			deletePromise.fail(fail);
		});
		return deletePromise.future();
	}
	
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
			LOGGER.error(file.toJson().encodePrettily());
			return Future.failedFuture("Supplied File was invalid");
		}
		Promise<String> dataPromise = Promise.<String>promise();
		eb.request(SoileConfigLoader.getServerProperty("gitVerticleAddress"), gitProviderVerticle.createGetCommand(file.getRepoID(),file.getRepoVersion(),file.getFileName()))
		.onSuccess( reply ->
		{
			LOGGER.debug(reply.body());
			dataPromise.complete(((JsonObject)reply.body()).getString(gitProviderVerticle.DATAFIELD));							
		})
		.onFailure(fail ->
		{
			LOGGER.debug(fail);
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
		return getGitFileContents(new GitFile(resourceFolder + File.separator + file.getFileName(), file.getRepoID(), file.getRepoVersion()));
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
				LOGGER.error(e);
				dataPromise.fail(e);
			}
			
		}).onFailure(fail -> 
		{
			dataPromise.fail(fail);
		});	
		return dataPromise.future();			
	}

	/**
	 * Get the resource files available for a specific git Version of an Object  
	 * @return A {@link JsonObject} of the contents of the git file.
	 */
	public Future<JsonArray> getResourceList(GitElement repoVersion)
	{
		if(!repoVersion.isValid())
		{
			return Future.failedFuture("Supplied Repo was invalid: " + repoVersion.toString());
		}
		Promise<JsonArray> dataPromise = Promise.promise();
		JsonObject getFilesCommand = gitProviderVerticle.createCommandForRepoAtVersion(repoVersion.getRepoID(), repoVersion.getRepoVersion())
									 .put(gitProviderVerticle.COMMANDFIELD, gitProviderVerticle.LIST_FILES_COMMAND);
		eb.request(SoileConfigLoader.getServerProperty("gitVerticleAddress"), getFilesCommand).onSuccess(fileData ->{			
			JsonArray result = ((JsonObject)fileData.body()).getJsonArray(gitProviderVerticle.DATAFIELD);
			LOGGER.debug(result.encodePrettily());
			for(int i = 0; i < result.size(); ++i)
			{		
				if(result.getJsonObject(i).getString("label").equals(resourceFolder))
				{
					dataPromise.complete(result.getJsonObject(i).getJsonArray("children"));
					return;
				}				
			}
			// we didn't find the resource folder so we return an empty array.
			dataPromise.complete(new JsonArray());
		}).onFailure(fail -> 
		{
			dataPromise.fail(fail);
		});	
		return dataPromise.future();			
	}
	
	
	/**
	 * Get the resource files available for a specific git Version of an Object  
	 * @return A {@link JsonObject} of the contents of the git file.
	 */
	public Future<JsonArray> getHistory(GitElement repoVersion)
	{
		if(!repoVersion.isValid())
		{
			return Future.failedFuture("Supplied Repo was invalid: " + repoVersion.toString());
		}
		Promise<JsonArray> dataPromise = Promise.promise();
		JsonObject getHistoryCommand = gitProviderVerticle.createCommandForRepoAtVersion(repoVersion.getRepoID(), repoVersion.getRepoVersion())
									 .put(gitProviderVerticle.COMMANDFIELD, gitProviderVerticle.GET_HISTORY_COMMAND);
		eb.request(SoileConfigLoader.getServerProperty("gitVerticleAddress"), getHistoryCommand).onSuccess(fileData ->{			
			JsonArray result = ((JsonObject)fileData.body()).getJsonArray(gitProviderVerticle.DATAFIELD); 
			dataPromise.complete(result);
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
	public Future<String> writeGitFile(GitFile file, String data, String tag)
	{
		Promise<String> versionPromise = Promise.<String>promise();
		if(!gitFileValid(file))
		{
			return Future.failedFuture("Supplied File was invalid");
		}
		JsonObject command = gitProviderVerticle.createWriteCommand(file.getRepoID(), file.getRepoVersion(), data, file.getFileName());
		if( tag != null)
		{
			// we want to create a tag for this.
			command.put(gitProviderVerticle.TAGFIELD, tag);
		}
		eb.request(SoileConfigLoader.getServerProperty("gitVerticleAddress"), command).onSuccess(reply -> {
			JsonObject info = (JsonObject)reply.body();
			// return the new version of this repository (for future changes)
			LOGGER.debug("Updated file " + file.toJson().encode() + " for version " + info.getString(gitProviderVerticle.VERSIONFIELD));
			versionPromise.complete(info.getString(gitProviderVerticle.VERSIONFIELD));
		}).onFailure(fail -> {
			versionPromise.fail(fail);
		});
		return versionPromise.future();	
	}
	
	/**
	 * Delete the specified file from the git repo. 
	 * @param file the GitFile containing name (including folders),  
	 * @param tag A Tag to give 
	 * @return
	 */
	public Future<String> deleteGitFile(GitFile file, String tag)
	{
		Promise<String> versionPromise = Promise.<String>promise();
		if(!gitFileValid(file))
		{
			return Future.failedFuture("Supplied File was invalid");
		}
		LOGGER.debug("Deleting file " + file.toJson().encodePrettily());
		JsonObject command = gitProviderVerticle.createCommandForFileInRepoAtVersion(file.getRepoID(), file.getRepoVersion(), file.getFileName()).put(gitProviderVerticle.COMMANDFIELD, gitProviderVerticle.DELETE_FILE_COMMAND);
		if( tag != null)
		{
			// we want to create a tag for this.
			command.put(gitProviderVerticle.TAGFIELD, tag);
		}
		eb.request(SoileConfigLoader.getServerProperty("gitVerticleAddress"), command).onSuccess(reply -> {
			JsonObject info = (JsonObject)reply.body();
			// return the new version of this repository (for future changes)
			LOGGER.debug("Deleted File from version. new Version is: " + info.getString(gitProviderVerticle.VERSIONFIELD));
			versionPromise.complete(info.getString(gitProviderVerticle.VERSIONFIELD));
		}).onFailure(fail -> {
			versionPromise.fail(fail);
		});
		return versionPromise.future();	
	}
		
	/**
	 * Write data to a file specified by the {@link GitFile} in the resources folder of the repo.  
	 * @param file the GitFile containing name (including folders) but excluding the resources folder,  
	 * @param data the data to be written to the file.
	 * @return a Future with the version of the git repository after execution.
	 */
	public Future<String> deleteGitResourceFile(GitFile file)
	{
		// a Resource will NEVER create a new tag!
		return deleteGitFile(new GitFile(resourceFolder + File.separator + file.getFileName(), file.getRepoID(), file.getRepoVersion()), null);
	}
	
	
	
	
	/**
	 * Same as writeGitFile with a string, but encodes the provided Json as a pretty string.
	 * @param file the file indicating where to write to.
	 * @param data the data that should be encoded in the file
	 * @return the new version of the file.
	 */
	public Future<String> writeGitFile(GitFile file, JsonObject data, String tag)
	{
		return writeGitFile(file, data.encodePrettily(), tag);
	}
		
	/**
	 * Write data to a file specified by the {@link GitFile} in the resources folder of the repo.  
	 * @param file the GitFile containing name (including folders) but excluding the resources folder,  
	 * @param data the data to be written to the file.
	 * @return a Future with the version of the git repository after execution.
	 */
	public Future<String> writeGitResourceFile(GitFile file, String data)
	{
		// a Resource will NEVER create a new tag!
		return writeGitFile(new GitFile(resourceFolder + File.separator + file.getFileName(), file.getRepoID(), file.getRepoVersion()), data, null);
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
	
	
	/**
	 * Create a basic git element for different types of elements.
	 * @param name Name of the element 
	 * @param type Class 
	 * @return
	 */
	public static JsonObject buildBasicGitElement(String name, TargetElementType type)
	{
		switch(type)
		{
			case PROJECT: return buildBasicGitProject(name);
			case TASK: return buildBasicGitTask(name);
			case EXPERIMENT: return buildBasicGitExperiment(name);
			default: return buildBasicGitObject(name);
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
		return buildBasicGitObject(name).put("code", "").put("codeType", new JsonObject().put("language", "").put("version", "")).put("resources", new JsonArray());
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
