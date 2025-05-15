package fi.abo.kogni.soile2.projecthandling.projectElements;


import java.io.InputStream;
import java.io.OutputStream;
import java.util.function.Supplier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fi.abo.kogni.soile2.datamanagement.datalake.DataLakeFile;
import fi.abo.kogni.soile2.datamanagement.datalake.DataLakeResourceManager;
import fi.abo.kogni.soile2.datamanagement.git.GitFile;
import fi.abo.kogni.soile2.datamanagement.utils.TimeStampedMap;
import fi.abo.kogni.soile2.http_server.requestHandling.SOILEUpload;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.ext.web.FileUpload;

/**
 * Handler for element data interaction between The Database and the Git Repository (i.e. linking the two sides.
 * @author Thomas Pfau
 *
 * @param <T> The class of the {@link Element} handled by this handler
 */
public class ElementDataHandler<T extends Element>{
	private static final Logger LOGGER = LogManager.getLogger(ElementDataHandler.class);

	/**
	 * The Type ID to determine the Type of the Handler objects
	 */
	protected String typeID;
	private DataLakeResourceManager resourceManager;
	private TimeStampedMap<GitFile, DataLakeFile> gitElements;
	
	/**
	 * Default constructor
	 * @param manager a {@link DataLakeResourceManager} for datalake access
	 * @param supplier A Supplier of the type used for this Handler
	 */
	public ElementDataHandler(DataLakeResourceManager manager, Supplier<T> supplier)
	{
		typeID = supplier.get().getTypeID();
		resourceManager = manager;
		gitElements = new TimeStampedMap<>(resourceManager, 3600*2);
	}
	
	/**
	 * Clean up the stored elements that are older than their ttl.
	 */
	public void cleanUp()
	{
		gitElements.cleanUp();
	}
	/**
	 * Get a File for a given TaskID at a given taskVersion with a given FileName
	 * @param taskID the ID of the task (will be translated to the appropriate Git Repository ID) 
	 * @param taskVersion the version for which to get the given file
	 * @param filename the name of the file
	 * @return A {@link Future} of the requested {@link DataLakeFile}
	 */
	public Future<DataLakeFile> handleGetFile(String taskID, String taskVersion, String filename)
	{		
		Promise<DataLakeFile> filePromise = Promise.promise();
		String repoID = typeID + taskID;
		GitFile f = new GitFile(filename, repoID, taskVersion);		
		gitElements.getData(f)
		.onSuccess(datalakeFile -> {			
			filePromise.complete(datalakeFile);
		})
		.onFailure(err -> {
			filePromise.fail(err);
		});
		return filePromise.future();
	}
	
	/**
	 * Write a file from an upload to git. NOTE: This will remove the actual file of the upload and move it to the datalake
	 * @param taskID the ID of the task
	 * @param taskVersion the version of the task BEFORE adding the file (i.e. base version)
	 * @param filename The name of the file that is being added
	 * @param upload the {@link FileUpload} that has the uploaded file.  
	 * @return A {@link Future} of the Version {@link String} of the Task Version after the file was added 
	 */
	public Future<String> handlePostFile(String taskID, String taskVersion, String filename, SOILEUpload upload)
	{
		LOGGER.debug("Trying to post file: " + filename);
		Promise<String> successPromise = Promise.promise();
		String repoID = this.typeID + taskID;
		LOGGER.debug("Creating git file with name : " + filename);
		GitFile f = new GitFile(filename, repoID, taskVersion);
		resourceManager.writeUploadToGit(f, upload)			
		.onSuccess(version -> {
			LOGGER.debug("Element Written");
			successPromise.complete(version);				
		});		
		return successPromise.future();
	}
	
	/**
	 * Write a file from an upload to git. NOTE: This will remove the actual file of the upload and move it to the datalake
	 * @param taskID the ID of the task
	 * @param taskVersion the version of the task BEFORE adding the file (i.e. base version)
	 * @param filename The name of the file that is being added
	 * @param is the {@link InputStream} that has the uploaded file. 
	 * @return A {@link Future} of the Version {@link String} of the Task Version after the file was added 
	 */
	public Future<String> handleWritefile(String taskID, String taskVersion, String filename, InputStream is)
	{
		LOGGER.debug("Trying to post file: " + filename);
		Promise<String> successPromise = Promise.promise();
		String repoID = this.typeID + taskID;
		GitFile f = new GitFile(filename, repoID, taskVersion);
		resourceManager.writeStreamToGit(f, is)			
		.onSuccess(version -> {
			LOGGER.debug("Element Written");
			successPromise.complete(version);				
		});		
		return successPromise.future();
	}
	
	
	/**
	 * Delete a file in the given Version of the object.
	 * @param taskID the ID of the task
	 * @param taskVersion the version of the task BEFORE adding the file (i.e. base version)
	 * @param filename The name of the file that is being added
	 * @return A {@link Future} of the Version {@link String} of the Task Version after the file was added 
	 */
	public Future<String> handleDeleteFile(String taskID, String taskVersion, String filename)
	{
		LOGGER.debug("Trying to post file: " + filename);
		Promise<String> successPromise = Promise.promise();
		String repoID = this.typeID + taskID;
		GitFile f = new GitFile(filename, repoID, taskVersion);
		resourceManager.deleteGitFile(f)			
		.onSuccess(version -> {
			LOGGER.debug("Element Written");
			successPromise.complete(version);				
		});		
		return successPromise.future();
	}
	
	/**
	 * Delete the Folder associated with the given element in the datalake
	 * @param elementID the ID of the element
	 * @return A succeeded {@link Future} if the folder was deleted.  
	 */
	public Future<Void> deleteElementFolder(String elementID)
	{		
		Promise<Void> successPromise = Promise.promise();
		String repoID = this.typeID + elementID;		
		resourceManager.deleteElementFolder(repoID)			
		.onSuccess(version -> {
			LOGGER.debug("Folder deleted");
			successPromise.complete();				
		});		
		return successPromise.future();
	}
}
