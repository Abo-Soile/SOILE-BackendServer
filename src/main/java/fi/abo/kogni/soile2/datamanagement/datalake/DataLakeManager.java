package fi.abo.kogni.soile2.datamanagement.datalake;

import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.UUID;

import com.mchange.v3.filecache.FileNotCachedException;

import fi.abo.kogni.soile2.projecthandling.participant.Participant;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.impl.TaskFileResult;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.ext.web.FileUpload;

public class DataLakeManager {

	String datalakedirectory;
	Vertx vertx;
	public DataLakeManager(String Folder) {
		datalakedirectory = Folder;
	}

	/**
	 * Write an element to into the dataLake. The actual location in the dataLake is defind by the project ID, the task ID and the participant ID.
	 * Data will be stored in the following scheme (to be able to quickly download all data for a project):  
	 * projectInstanceID / participantID / step / taskID / filesForTask.format    
	 * This allows a quick removal of all data for a participant by deleting the respective participantID folder.
	 * @param p the participant for which to store the data
	 * @param step the step for the data 
	 * @param taskId the task for which data is stored.
	 * @param projectID
	 * @param upload
	 * @return
	 */

	public Future<String> storeParticipantData(Participant p, int step, String taskID, FileUpload upload)
	{
		Promise<String> idPromise = Promise.promise();
		TaskFileResult targetFile = new TaskFileResult(upload.fileName(), "Temp", upload.contentType(), step, taskID, p.getID()); 		
		vertx.fileSystem().mkdirs(targetFile.getFolderPath(datalakedirectory))
		.onSuccess( folderCreated -> {
			String FileID = UUID.randomUUID().toString();
			targetFile.setLocalFileName(FileID);
			// this will not block for long...
			while(vertx.fileSystem().existsBlocking(targetFile.getFilePathInDataLake(datalakedirectory)))
			{
				FileID = UUID.randomUUID().toString();
				targetFile.setLocalFileName(FileID);	
			}
			String finalID = FileID;
			vertx.fileSystem().move(upload.uploadedFileName(), targetFile.getFilePathInDataLake(taskID))
			.onSuccess(res -> {
				idPromise.complete(finalID);	
			})
			.onFailure(err -> idPromise.fail(err));		
			
		})
		.onFailure(err -> idPromise.fail(err));
		
		return idPromise.future();
		
	}
	/**
	 * Write an element to into the dataLake. The actual location in the dataLake is defind by the project ID, the task ID and the participant ID.
	 * Data will be stored in the following scheme (to be able to quickly download all data for a project):  
	 * projectInstanceID / participantID / step / taskID / filesForTask.format    
	 * This allows a quick removal of all data for a participant by deleting the respective participantID folder.
	 * @param p the participant for which to store the data
	 * @param step the step for the data 
	 * @param taskId the task for which data is stored.
	 * @param projectID
	 * @param upload
	 * @return
	 */

	public DataLakeFile getFile(TaskFileResult result)
	{
		return result.getFile(datalakedirectory);		
	}
}
