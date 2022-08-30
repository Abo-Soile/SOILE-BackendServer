package fi.abo.kogni.soile2.project.data;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import fi.abo.kogni.soile2.project.instance.ProjectInstance;
import fi.abo.kogni.soile2.project.participant.Participant;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;

public class DataBundleGenerator {

	MongoClient client;
	String participantDB;
	String projectInstanceDB;
	ConcurrentHashMap<UUID, DownloadStatus> downloadStatus;
	
	
	public enum DownloadStatus
	{
		participantsCollected,
		dataCollected,
		filesCollected,
		downloadReady
	}
	
	/**
	 * Get a UUID that can be used to obtain a  
	 * @param p
	 * @param resultHandler
	 * @return
	 */
	public Future<UUID> getDataBundleForProject(JsonObject projectData)
	{
		// This promise is a promise that can be used in a reply and thatwill point to a specific download which is being generated   
		Promise<UUID> filePromise = Promise.<UUID>promise();
		// Now, we collect all data. This can take some time
		JsonObject query = new JsonObject().put("_id", new JsonObject().put("$in", projectData.getJsonArray("participants")));
		
		client.find(participantDB, query).onSuccess(participants -> {
			// we have collected all Participants successfully. Now we can return the UUID for the download.
			UUID newID = UUID.randomUUID();
			downloadStatus.put(newID, DownloadStatus.participantsCollected);
			filePromise.complete(newID);
			
			for(JsonObject participant :  participants)				
			{
				//TODO: Create a Data Object for all Json Results (maybe we can at some point offer different formats, 
				//but first we will have to specify what exactly we allow to come in here.			
			}
			downloadStatus.put(newID, DownloadStatus.dataCollected);
			//We will write the data 
			// Now we collect the files, which can then be used to, on the fly, zip and and stream a output file on request.
			HashMap<String,HashMap<String,Object>> fileResults;
			
			
		});
		return filePromise.future();
	}
	
	public void addParticipantToXLS()
	{
		
	}
	
	
}
