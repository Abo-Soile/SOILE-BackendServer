package fi.abo.kogni.soile2.projecthandling.data;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fi.aalto.scicomp.zipper.FileDescriptor;
import fi.aalto.scicomp.zipper.Zipper;
import fi.abo.kogni.soile2.datamanagement.datalake.DataLakeFile;
import fi.abo.kogni.soile2.projecthandling.participant.ParticipantHandler;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.impl.ProjectInstanceHandler;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.impl.TaskFileResult;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class DataBundleGenerator extends AbstractVerticle{

	
	ConcurrentHashMap<String, DownloadStatus> downloadStatus;
	ConcurrentHashMap<String, String> downloadErrors;
	ConcurrentHashMap<String, Long> downloadStart;
	ConcurrentHashMap<String, List<DataLakeFile>> downloadFiles;	
	ParticipantHandler partHandler;
	ProjectInstanceHandler projHandler;
	String dataLakeFolder;
	static final Logger LOGGER = LogManager.getLogger(DataBundleGenerator.class);

	@Override
	public void start(Promise<Void> startPromise)
	{
		vertx.eventBus().consumer("fi.abo.soile.DLStatus", this::getStatus);
		vertx.eventBus().consumer("fi.abo.soile.DLFiles", this::getDownloadFiles);
		startPromise.complete();
	}
	
	@Override
	public void stop(Promise<Void> stopPromise)
	{
		vertx.eventBus().consumer("fi.abo.soile.DLStatus", this::getStatus).unregister()
		.onSuccess(success -> stopPromise.complete())
		.onFailure(err -> stopPromise.fail(err));
	}
	
	public enum DownloadStatus
	{
		creating,
		collecting,		
		downloadReady,
		problems,
		failed
	}
	
	
	
	/**
	 * Create a download ID. This is synchronized so that we don't create the same ID twice.
	 * @return
	 */
	public synchronized String createDLID()
	{		
		UUID dlID = UUID.randomUUID();
		while(downloadStatus.contains(dlID.toString()))
		{
			dlID = UUID.randomUUID();			
		}
		downloadStatus.put(dlID.toString(), DownloadStatus.creating);
		downloadStart.put(dlID.toString(), System.currentTimeMillis());
		return dlID.toString();
	}
	
	/**
	 * Create a download for the given participants.  
	 * @param participants
	 * @return
	 */
	public Future<String> buildParticipantBundle(JsonArray participants, String projectID)
	{
		String dlID = createDLID();					
		return collectParticipantFiles(dlID, participants, projectID).map(dlID);
	}
	
	Future<Void> collectParticipantFiles(String dlID, JsonArray participants, String projectID)
	{
		Promise<Void> collectionStartedPromise = Promise.promise();
		projHandler.loadProject(projectID)
		.onSuccess(projectInstance ->
		{		
			downloadStatus.put(dlID, DownloadStatus.collecting);
			collectionStartedPromise.complete();
			
			partHandler.getParticipantData(projectInstance,participants)
			.onSuccess(participantsData -> {
				// This is a particpant with _id, resultData
				List<DataLakeFile> DataLakeFiles = new LinkedList<>();
				JsonArray jsonData = new JsonArray(); 
				
				for(JsonObject participantData : participantsData )
				{
					participantData.put("participantID", participantData.getValue("_id")).remove("_id");					
					DataLakeFiles.addAll(extractFilesandUpdateParticipantData(participantData.getJsonArray("resultData"), participantData.getString("participantID")));					
					jsonData.add(participantData.getJsonArray("resultData"));
						//TODO: Build Json Result File
						//TODO: Update Results to point to the actual files in the zip.
						//TODO: Build Result Zip..		
					JsonObject partObject = new JsonObject()
							.put("participantID", participantData.getString("participantID"))
							.put("resultData", participantData.getJsonArray("resultData"));
					jsonData.add(partObject);
				}								
				checkForFileProblems(DataLakeFiles)
				.onSuccess(filesExist -> {					
					// now, we need to filter and update the error.
					filterFilesAndStatus(DataLakeFiles, filesExist, dlID);
					// Now we have all result files filtered. Lets build the result Json. 
					// TODO: Allow XLS download for projects which can be flattened (i.e. which have no repeating tasks.)
					JsonObject JsonFile = new JsonObject()
											  .put("project", new JsonObject()
													              .put("id", projectInstance.getID())
													              .put("name", projectInstance.getName()))
											  .put("participantResults", jsonData);
					vertx.fileSystem().createTempFile(projectID, null)
					.onSuccess(fileName -> {
						vertx.fileSystem().writeFile(fileName, Buffer.buffer(JsonFile.encodePrettily()))
						.onSuccess(written -> 
						{
							DataLakeFiles.add(new DataLakeFile(fileName,"data.json","application/json"));
							downloadFiles.put(dlID, DataLakeFiles);
							downloadStatus.put(dlID, DownloadStatus.downloadReady);							
						})
						.onFailure(err -> {
							LOGGER.error("Could not create Temporary file.");
							LOGGER.error(err);
							failDownload(err, dlID);								
						});
					});
					
				});
			})
			.onFailure(err -> {
				LOGGER.error("Could not retrieve data for participants");
				LOGGER.error(err);
				failDownload(err, dlID);								
			});
		})
		.onFailure(err -> collectionStartedPromise.fail(err));
		return collectionStartedPromise.future();
		
	}
	
	
	/**
	 * This function assumes to have been provided with the resultData {@link JsonArray} from a participant.
	 * @param resultData
	 * @param participantID
	 * @return
	 */
	private List<DataLakeFile> extractFilesandUpdateParticipantData(JsonArray resultData, String participantID) 
	{
		List<DataLakeFile> files = new LinkedList<>();
		for(int i = 0; i < resultData.size(); i++)
		{
			JsonArray fileNames = new JsonArray();
			int step = resultData.getJsonObject(i).getInteger("step");
			String task = resultData.getJsonObject(i).getString("task");
			JsonArray fileData = resultData.getJsonObject(i).getJsonArray("fileData", new JsonArray());
			for(int fileEntry = 0; fileEntry < fileData.size(); fileEntry++)
			{
				JsonObject fileResult = fileData.getJsonObject(fileEntry);
				TaskFileResult res = new TaskFileResult(fileResult.getString("filename"),
														fileResult.getString("targetid"),
														fileResult.getString("fileformat"),
														step,
														task,
														participantID);				
				JsonObject fileInfo = new JsonObject().put("filInZip", res.getFile(dataLakeFolder).getOriginalFileName()).put("originalName", fileResult.getString("filename"));
				fileNames.add(fileInfo);
				files.add(res.getFile(dataLakeFolder));
			}
			// update the file data to only point at the actual files.
			resultData.getJsonObject(i).put("files", fileNames);
			resultData.getJsonObject(i).remove("fileData");			
		}
		return files;
	}			

	
	public void getStatus(Message<JsonObject> message)
	{
		String dlID = message.body().getString("downloadID");
		message.reply(downloadStatus.get(dlID));
	}

	public void getDownloadFiles(Message<JsonObject> message)
	{
		String dlID = message.body().getString("downloadID");
		if(downloadStatus.get(dlID) == DownloadStatus.downloadReady)
		{
		//TODO: No implemented yet	
		}
	}
	
	public void filterFilesAndStatus(List<DataLakeFile> files, List<Boolean> existsIndicator, String dlID)
	{
		List<String> missingFileNames = new LinkedList<String>();
		List<DataLakeFile> toRemove = new LinkedList<>();
		for(int i = 0; i < existsIndicator.size(); ++i)
		{
			if(!existsIndicator.get(i)) {
				missingFileNames.add(files.get(i).getAbsolutePath().replace(dataLakeFolder, ""));
				toRemove.add(files.get(i));
			}
		}
		if(toRemove.size() > 0)
		{
			downloadStatus.put(dlID, DownloadStatus.problems);
			downloadErrors.put(dlID, String.join("", missingFileNames));
			files.removeAll(toRemove);
		}				
	}
	
	public Future<List<Boolean>> checkForFileProblems(List<DataLakeFile> files)
	{
		Promise<List<Boolean>> errorPromise = Promise.promise();
		List<Future> filesExistFutures = new LinkedList<Future>();
		
		for(DataLakeFile file : files)
		{
			filesExistFutures.add(checkFileExist(file));									
		}
		CompositeFuture filesExist =  CompositeFuture.join(filesExistFutures);	
		filesExist.onComplete(finishedCheck -> {
			List<Boolean> fileExistIndicator = new LinkedList<>();
			for(int i = 0; i < filesExistFutures.size(); ++i)				
			{
				fileExistIndicator.add(filesExist.succeeded(i));
			}
			errorPromise.complete(fileExistIndicator);
		})
		.onFailure(err -> errorPromise.fail(err));
		return errorPromise.future();
		
	}
	

	Future<Void> checkFileExist(DataLakeFile f)
	{
		Promise<Void> fileExists = Promise.promise();
		vertx.fileSystem().exists(f.getAbsolutePath())
		.onSuccess(res -> {
			if(res)
			{
				fileExists.complete();
			}
			else
			{
				fileExists.fail("File does not exist");
			}
		})
		.onFailure(err -> fileExists.fail(err));
		return fileExists.future();
	}
	
	private void failDownload(Throwable err, String dlID)
	{
		downloadStatus.put(dlID, DownloadStatus.failed);
		downloadErrors.put(dlID, err.getMessage());
		downloadFiles.remove(dlID);
		downloadStart.remove(dlID);
	}
	
}
