package fi.abo.kogni.soile2.projecthandling.projectElements;


import java.util.function.Supplier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fi.abo.kogni.soile2.datamanagement.datalake.DataLakeFile;
import fi.abo.kogni.soile2.datamanagement.datalake.DataLakeResourceManager;
import fi.abo.kogni.soile2.datamanagement.git.GitFile;
import fi.abo.kogni.soile2.datamanagement.utils.TimeStampedMap;
import fi.abo.kogni.soile2.projecthandling.projectElements.impl.Task;
import fi.abo.kogni.soile2.projecthandling.utils.ObjectGenerator;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.ext.web.FileUpload;

public class ElementDataHandler<T extends Element>{
	private static final Logger LOGGER = LogManager.getLogger(ElementDataHandler.class);

	protected String typeID;
	private DataLakeResourceManager resourceManager;
	private TimeStampedMap<GitFile, DataLakeFile> gitElements;
	
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
	
	public Future<DataLakeFile> handleGetFile(String taskID, String taskVersion, String filename)
	{		
		Promise<DataLakeFile> filePromise = Promise.promise();
		String repoID = Task.typeID + taskID;
		GitFile f = new GitFile(filename, repoID, taskVersion);		
		gitElements.getData(f)
		.onSuccess(datalakeFile -> {			
			filePromise.complete(datalakeFile);
		});
		return filePromise.future();
	}
	
	/**
	 * This 
	 * @param taskID
	 * @param taskVersion
	 * @param filename
	 * @param upload
	 * @return
	 */
	public Future<String> handlePostFile(String taskID, String taskVersion, String filename, FileUpload upload)
	{
		LOGGER.debug("Trying to post file: " + filename);
		Promise<String> successPromise = Promise.promise();
		String repoID = this.typeID + taskID;
		GitFile f = new GitFile(filename, repoID, taskVersion);
		resourceManager.writeUploadToGit(f, upload )			
		.onSuccess(version -> {
			LOGGER.debug("Element Written");
			successPromise.complete(version);				
		});		
		return successPromise.future();
	}
}
