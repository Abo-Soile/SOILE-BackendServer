package fi.abo.kogni.soile2.datamanagement.git;

import java.io.File;
import java.nio.file.Paths;

import fi.abo.kogni.soile2.datamanagement.datalake.DataLakeFile;
import fi.abo.kogni.soile2.datamanagement.utils.TimeStampedMap;
import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.vertx.core.Future;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.FileUpload;

/**
 * A Manager/Retriever for Files for which indicator files are stored in the git Repository.  
 * @author thomas
 *
 */
public class GitResourceManager extends GitDataRetriever<DataLakeFile> {

	private String dataLakeBaseFolder;
	private TimeStampedMap<GitFile,DataLakeFile> elements;
	
	public GitResourceManager(EventBus bus)
	{
		super(bus,true, true);
		dataLakeBaseFolder = SoileConfigLoader.getServerProperty("soileGitDataLakeFolder");		
	}
		
	/**
	 * Write an element to the git Repository and return the new Version.  
	 *  
	 * @return
	 */
	public Future<String> writeElement(GitFile target, FileUpload targetUpload)	
	{		
		String gitFileName = target.getFileName() == null ? targetUpload.fileName() : target.getFileName();
		String targetFileName = Paths.get(targetUpload.uploadedFileName()).getFileName().toString();		
		JsonObject fileContents = new JsonObject().put("filename", gitFileName)
												  .put("targetFile", targetFileName)
												  .put("format", targetUpload.contentType());		
		return manager.writeGitResourceFile(target, fileContents);		
	}
	
	/**
	 * Test, whether the git repository for a specific element (Task/Experiment/Project) exists
	 * @param elementID the UUID of the element
	 * @return A future whether the element exists
	 */
	public Future<Boolean> existElementRepo(String elementID)
	{
		return manager.doesRepoExist(elementID);
	}

	@Override
	public DataLakeFile createElement(Object elementData, GitFile key) {
		
		JsonObject json = (JsonObject) elementData;
		DataLakeFile target = new DataLakeFile(dataLakeBaseFolder + File.separator + key.getRepoID() + File.separator + json.getString("targetFile"), json.getString("filename"), json.getString("format"));							
		return target;
	}
	
}
