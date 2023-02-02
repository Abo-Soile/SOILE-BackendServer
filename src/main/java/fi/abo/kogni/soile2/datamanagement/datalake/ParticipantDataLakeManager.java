package fi.abo.kogni.soile2.datamanagement.datalake;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fi.abo.kogni.soile2.projecthandling.projectElements.instance.impl.TaskFileResult;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.file.CopyOptions;
import io.vertx.ext.web.FileUpload;

public class DataLakeManager{

	String datalakedirectory;
	Vertx vertx;
	static final Logger LOGGER = LogManager.getLogger(DataLakeManager.class);

	public DataLakeManager(String Folder, Vertx vertx) {
		datalakedirectory = Folder;
		this.vertx = vertx; 
	}

	/**
	 * Write an element to into the dataLake. The actual location in the dataLake is defined by the task ID and the participant ID.
	 * Data will be stored in the following scheme (to be able to quickly download all data for a project):  
	 * projectInstanceID / participantID / step / taskID / filesForTask.format    
	 * This allows a quick removal of all data for a participant by deleting the respective participantID folder.
	 * @param p the participant for which to store the data
	 * @param step the step for the data 
	 * @param taskId the task for which data is stored.
	 * @param upload
	 * @return
	 */
	public Future<String> storeParticipantData(String participantID, int step, String taskID, FileUpload upload)
	{
		Promise<String> idPromise = Promise.promise();
		TaskFileResult targetFile = new TaskFileResult("", "", "", step, taskID, participantID);
		LOGGER.debug("Creating directories");
		vertx.fileSystem().mkdirs(targetFile.getFolderPath(datalakedirectory))		
		.onSuccess( folderCreated -> {
			LOGGER.debug("Directories created, creating Temp File");
			vertx.fileSystem().createTempFile(targetFile.getFolderPath(datalakedirectory), "result", ".out","rw-rw----")
			.onSuccess(targetFileLocation -> {
				LOGGER.debug("temp File created: " + targetFileLocation);
				String fileName = targetFileLocation.replace(targetFile.getFolderPath(datalakedirectory),"");
				targetFile.setLocalFileName(fileName);
				LOGGER.debug("Trying to move file : " + upload.uploadedFileName() + " to " + targetFile.getFilePathInDataLake(datalakedirectory));
				vertx.fileSystem().move(upload.uploadedFileName(), targetFile.getFilePathInDataLake(datalakedirectory), new CopyOptions().setReplaceExisting(true))
				.onSuccess(res -> {
					LOGGER.debug("File Moved ");
					idPromise.complete(fileName);
				})
				.onFailure(err -> idPromise.fail(err));
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
