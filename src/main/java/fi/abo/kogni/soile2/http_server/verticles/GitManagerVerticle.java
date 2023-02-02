package fi.abo.kogni.soile2.http_server.verticles;

import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fi.abo.kogni.soile2.datamanagement.git.GitElement;
import fi.abo.kogni.soile2.datamanagement.git.GitFile;
import fi.abo.kogni.soile2.datamanagement.git.GitManager;
import fi.abo.kogni.soile2.datamanagement.utils.DataRetrieverImpl;
import fi.abo.kogni.soile2.datamanagement.utils.TimeStampedMap;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class GitManagerVerticle extends AbstractVerticle{


	EventBus eb;
	GitManager gitManager;
	TimeStampedMap<GitFile, String> resourceStringContents;
	TimeStampedMap<GitFile, JsonObject> resourceJsonContents;
	TimeStampedMap<GitFile, JsonObject> fileJsonContents;
	TimeStampedMap<GitFile, String> fileStringContents;
	TimeStampedMap<GitElement, JsonArray> resourceLists;
	
	private List<MessageConsumer> consumers;
	private static final Logger LOGGER = LogManager.getLogger(GitManagerVerticle.class);

	@Override
	public void start()
	{
		eb = vertx.eventBus();
		gitManager = new GitManager(eb);
		consumers = new LinkedList<>();
		resourceStringContents = new TimeStampedMap<GitFile,String>(new DataRetrieverImpl<GitFile,String>(gitManager::getGitResourceContents), 3600*2);
		resourceJsonContents = new TimeStampedMap<GitFile,JsonObject>(new DataRetrieverImpl<GitFile,JsonObject>(gitManager::getGitResourceContentsAsJson), 3600*2);
		fileJsonContents = new TimeStampedMap<GitFile,JsonObject>(new DataRetrieverImpl<GitFile,JsonObject>(gitManager::getGitFileContentsAsJson), 3600*2);
		fileStringContents = new TimeStampedMap<GitFile,String>(new DataRetrieverImpl<GitFile,String>(gitManager::getGitFileContents), 3600*2);
		resourceLists = new TimeStampedMap<GitElement,JsonArray>(new DataRetrieverImpl<GitElement,JsonArray>(gitManager::getResourceList), 3600*2);
		consumers.add(eb.consumer("soile.git.doesRepoExist",this::doesRepoExist));
		consumers.add(eb.consumer("soile.git.initRepo",this::initRepo));
		consumers.add(eb.consumer("soile.git.getGitFileContents",this::getGitFileContents));
		consumers.add(eb.consumer("soile.git.getGitResourceContents",this::getGitResourceContents));
		consumers.add(eb.consumer("soile.git.getGitResourceContentsAsJson",this::getGitResourceContentsAsJson));
		consumers.add(eb.consumer("soile.git.getGitFileContentsAsJson",this::getGitFileContentsAsJson));
		consumers.add(eb.consumer("soile.git.getResourceList",this::getResourceList));
		consumers.add(eb.consumer("soile.git.writeGitFile",this::writeGitFile));
		consumers.add(eb.consumer("soile.git.writeGitResourceFile",this::writeGitResourceFile));
		consumers.add(eb.consumer("soile.git.cleanUp",this::cleanUp));
	}
	
	
	@Override
	public void stop(Promise<Void> stopPromise)
	{
		List<Future> undeploymentFutures = new LinkedList<Future>();
		for(MessageConsumer consumer : consumers)
		{
			undeploymentFutures.add(consumer.unregister());
		}				
		CompositeFuture.all(undeploymentFutures).mapEmpty().
		onSuccess(v -> {
			LOGGER.debug("Successfully undeployed SoileUserManager with id : " + deploymentID());
			stopPromise.complete();
		})
		.onFailure(err -> stopPromise.fail(err));			
	}

	
	/**
	 * Test whether a repo element exists asynchronosly
	 * @param message the message with the "elementID" to check
	 */
	public void cleanUp(Message<String> message)
	{
		resourceStringContents.cleanUp();
		resourceJsonContents.cleanUp();
		fileJsonContents.cleanUp();
		fileStringContents.cleanUp();
		resourceLists.cleanUp();
		message.reply(true);
	}
	
	/**
	 * Test whether a repo element exists asynchronosly
	 * @param message the message with the "elementID" to check
	 */
	public void doesRepoExist(Message<String> message)
	{
		gitManager.doesRepoExist(message.body())
		.onSuccess(exist -> {
			message.reply(exist);
		})
		.onFailure(err -> message.fail(500, err.getMessage()));
	}


	/**
	 * Initialize a repository
	 * @param message the message with the "elementID" to create 
	 * The supplied message gets answered with the current Version of the (empty) repository. Fails if the repository exists.    
	 */
	public void initRepo(Message<String> message)
	{
		gitManager.initRepo(message.body())
		.onSuccess(version -> {
			message.reply(version);
		})
		.onFailure(err -> message.fail(500, err.getMessage()));
	}

	/**
	 * Get the file contents of a file in the github repository, these are all just json/linker files).
	 * @param message the message with the GitFile contents. 
	 * @return A String with the contents that can normally be parsed as json.
	 */
	public void getGitFileContents(Message<JsonObject> request)
	{
		fileStringContents.getData(new GitFile(request.body()))		
		.onSuccess(contents -> {
			request.reply(contents);
		})
		.onFailure(err -> request.fail(500, err.getMessage()));
	}


	/**
	 * Get the file contents of a resource file in the github repository as string, 
	 * since no binary files are in the git repo and those are all pointers this will essentially return the pointer file.
	 * @param message the message with the GitFile contents. 
	 */
	public void getGitResourceContents(Message<JsonObject> request)
	{
		resourceStringContents.getData(new GitFile(request.body()))
		.onSuccess(contents -> {
			request.reply(contents);
		})
		.onFailure(err -> request.fail(500, err.getMessage()));
	}
	/**
	 * Get the file contents of a git resource file as a Json Object. the reply will be the json object of the contents. 
	 * @return A {@link JsonObject} of the contents of the git file.
	 */
	public void getGitResourceContentsAsJson(Message<JsonObject> request)
	{
		resourceJsonContents.getData(new GitFile(request.body()))
		.onSuccess(contents -> {
			request.reply(contents);
		})
		.onFailure(err -> request.fail(500, err.getMessage()));
	}

	/**
	 * Get the file contents of a git file as a Json Object. 
	 * @return A {@link JsonObject} of the contents of the git file.
	 */
	public void getGitFileContentsAsJson(Message<JsonObject> request)
	{
		fileJsonContents.getData(new GitFile(request.body()))
		.onSuccess(contents -> {
			request.reply(contents);
		})
		.onFailure(err -> request.fail(500, err.getMessage()));
	}
	/**
	 * Get the resource files available for a specific git Version of an Object as a JsonArray  
	 * @return A {@link JsonObject} of the contents of the git file.
	 */
	public void getResourceList(Message<JsonObject> request)
	{
		LOGGER.debug("Querying resource list from gitManager");
		resourceLists.getData(new GitElement(request.body()))
		.onSuccess(fileList -> {
			request.reply(fileList);
		})
		.onFailure(err -> request.fail(500, err.getMessage()));
	}

	/**
	 * Write data to a file specified by the {@link GitFile}, reply with the new version of the respective repo. 
	 * a request with gitFile fields and a data field for the data to write.
	 * @return
	 */
	public void writeGitFile(Message<JsonObject> request)
	{
		try {
			LOGGER.debug(request.body().encodePrettily());
			gitManager.writeGitFile(new GitFile(request.body()), request.body().getJsonObject("data"))
			.onSuccess(version -> {
				request.reply(version);
			})
			.onFailure(err -> request.fail(500, err.getMessage()));
		}
		catch(ClassCastException e)
		{
			gitManager.writeGitFile(new GitFile(request.body()), request.body().getString("data"))
			.onSuccess(version -> {
				request.reply(version);
			})
			.onFailure(err -> request.fail(500, err.getMessage()));
		}
	}

	/**
	 * Write data to a file specified by the {@link GitFile} in the resources folder of the repo.  
	 * @param file the GitFile containing name (including folders) but excluding the resources folder,  
	 * @param data the data to be written to the file.
	 * @return a Future with the version of the git repository after execution.
	 */
	public void writeGitResourceFile(Message<JsonObject> request)
	{
		try {
			gitManager.writeGitResourceFile(new GitFile(request.body()), request.body().getJsonObject("data"))
			.onSuccess(version -> {
				request.reply(version);
			})
			.onFailure(err -> request.fail(500, err.getMessage()));
		}
		catch(ClassCastException e)
		{
			gitManager.writeGitResourceFile(new GitFile(request.body()), request.body().getString("data"))
			.onSuccess(version -> {
				request.reply(version);
			})
			.onFailure(err -> request.fail(500, err.getMessage()));
		}
	}

}


