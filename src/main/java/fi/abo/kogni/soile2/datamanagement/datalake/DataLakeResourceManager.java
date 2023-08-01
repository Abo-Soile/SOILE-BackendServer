package fi.abo.kogni.soile2.datamanagement.datalake;

import java.awt.datatransfer.MimeTypeParseException;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jgit.errors.ObjectWritingException;

import fi.abo.kogni.soile2.datamanagement.git.GitDataRetriever;
import fi.abo.kogni.soile2.datamanagement.git.GitFile;
import fi.abo.kogni.soile2.http_server.requestHandling.SOILEUpload;
import fi.abo.kogni.soile2.projecthandling.projectElements.impl.TaskResourceFile;
import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.CopyOptions;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.http.impl.MimeMapping;
import io.vertx.core.json.JsonObject;
import io.vertx.core.streams.Pipe;
import io.vertx.core.streams.ReadStream;
import io.vertx.ext.web.FileUpload;

/**
 * A Manager/Retriever for Files for which indicator files are stored in the git Repository.
 * Each Repository that contains resources will be represented by one folder in the datalake which will contain all data.    
 * @author thomas
 *
 */
public class DataLakeResourceManager extends GitDataRetriever<DataLakeFile> {

	private static final Logger LOGGER = LogManager.getLogger(DataLakeResourceManager.class);

	private String dataLakeDirectory;
	private Vertx vertx;
	public DataLakeResourceManager(Vertx vertx)
	{
		super(vertx.eventBus(),true, true);
		this.vertx = vertx;
		dataLakeDirectory = SoileConfigLoader.getServerProperty("soileGitDataLakeFolder");

	}

	/**
	 * Create/update a git file pointing to the actual element in the datalake that this file represents. This function will MOVE the
	 * file referenced by the provided {@link SOILEUpload}, so the {@link SOILEUpload} will not be valid afterwards.
	 * @param targetGitFile the {@link GitFile} the new data should be written to. Indicating the location in the resources
	 * @param is the {@link InputStream} that contains the data to be associated with the targetGitFile
	 * @return
	 */
	public Future<String> writeStreamToGit(GitFile targetGitFile, InputStream is)	
	{		
		Promise<String> versionUpdatePromise = Promise.promise();
		writeStreamToDataLake(targetGitFile, is)
		.onSuccess(dataFile -> {
			LOGGER.debug("File moved to target folder");
			String gitFileName = targetGitFile.getFileName();
			String targetFileName = dataFile.getFileInDataLake();		
			JsonObject fileContents = new JsonObject().put("filename", gitFileName)
					.put("targetFile", targetFileName)
					.put("format", MimeMapping.getMimeTypeForFilename(gitFileName));
			eb.request("soile.git.writeGitResourceFile", targetGitFile.toJson().put("data", fileContents))
			.onSuccess(reply -> {
						LOGGER.debug("Git File written");
						versionUpdatePromise.complete((String) reply.body());
					})
			.onFailure(err -> 
			{
				deleteFile(dataFile)
				.onFailure(err2 -> {
					// TODO: Log the error in deleting this file.
				});
				versionUpdatePromise.fail(err);
			});
		})
		.onFailure(err -> versionUpdatePromise.fail(err));
		return versionUpdatePromise.future();

	}
	
	
	/**
	 * Create/update a git file pointing to the actual element in the datalake that this file represents. This function will MOVE the
	 * file referenced by the provided {@link SOILEUpload}, so the {@link SOILEUpload} will not be valid afterwards.
	 * @param targetGitFile the {@link GitFile} the new data should be written to. Indicating the location in the resources
	 * @param fileUpload the {@link FileUpload} that contains the data to be associated with the targetGitFile
	 * @return
	 */
	public Future<String> writeUploadToGit(GitFile targetGitFile, SOILEUpload fileUpload)	
	{		
		Promise<String> versionUpdatePromise = Promise.promise();
		moveUploadToDataLake(targetGitFile, fileUpload)
		.onSuccess(dataFile -> {
			LOGGER.debug("File moved to target folder");
			String gitFileName = targetGitFile.getFileName() == null ? fileUpload.fileName() : targetGitFile.getFileName();
			String targetFileName = dataFile.getFileInDataLake();		
			JsonObject fileContents = new JsonObject().put("filename", gitFileName)
					.put("targetFile", targetFileName)
					.put("format", fileUpload.contentType());
			eb.request("soile.git.writeGitResourceFile", targetGitFile.toJson().put("data", fileContents))
			.onSuccess(reply -> {
						LOGGER.debug("Git File written");
						versionUpdatePromise.complete((String) reply.body());
					})
			.onFailure(err -> 
			{
				deleteFile(dataFile)
				.onFailure(err2 -> {
					// TODO: Log the error in deleting this file.
				});
				versionUpdatePromise.fail(err);
			});
		})
		.onFailure(err -> versionUpdatePromise.fail(err));
		return versionUpdatePromise.future();

	}

	/**
	 * Delete a file from a version in git
	 * @param targetGitFile the {@link GitFile} the new data should be written to. Indicating the location in the resources
	 * @return
	 */
	public Future<String> deleteGitFile(GitFile targetGitFile)	
	{		
		Promise<String> versionUpdatePromise = Promise.promise();
		eb.request("soile.git.deleteGitResourceFile", targetGitFile.toJson())
		.onSuccess(reply -> {
			LOGGER.debug("Git File written");
			versionUpdatePromise.complete((String) reply.body());
		})
		.onFailure(err ->versionUpdatePromise.fail(err));		
		return versionUpdatePromise.future();

	}
	
	/**
	 * Test, whether the git repository for a specific element (Task/Experiment/Project) exists
	 * @param elementID the UUID of the element
	 * @return A future whether the element exists
	 */
	public Future<Boolean> existElementRepo(String elementID)
	{
		return eb.request("soile.git.doesRepoExist",elementID).map(reply -> {return (Boolean) reply.body();});
	}

	@Override
	public DataLakeFile createElement(Object elementData, GitFile key) {

		JsonObject json = (JsonObject) elementData;
		TaskResourceFile representedElement = new TaskResourceFile(key.getRepoID(), json.getString("targetFile"), json.getString("filename"), json.getString("format"));								
		return representedElement.getDataLakeFile();
	}
	
	/**
	 * Move a File from a {@link FileUpload} to a a folder in the datalake that is associated with the repository indicated by the provided {@link GitFile} 
	 * @param targetGitFile the target {@link GitFile} the upload data will be associated with 
	 * @param upload the Upload from which to extract the actual file.
	 * @return a {@link Future} of the {@link TaskResourceFile} that contains all information about the datalake file created. 
	 */
	public Future<TaskResourceFile> moveUploadToDataLake(GitFile targetGitFile, SOILEUpload upload)
	{
		Promise<TaskResourceFile> dataLakeFilePromise = Promise.promise();
		String targetFolder = Path.of(dataLakeDirectory, targetGitFile.getRepoID()).toAbsolutePath().toString();		
		vertx.fileSystem().mkdirs(targetFolder)
		.onSuccess(folderCreated -> {			
			vertx.fileSystem().createTempFile(targetFolder , "", "","rw-rw----")
			.onSuccess(tempFileName -> {	
				vertx.fileSystem().move(upload.uploadedFileName(), tempFileName, new CopyOptions().setReplaceExisting(true))
				.onSuccess(moved -> {
					TaskResourceFile result = new TaskResourceFile(targetGitFile.getRepoID(), tempFileName.replace(targetFolder, ""), upload.fileName(), upload.contentType());
					dataLakeFilePromise.complete(result);
				})
				.onFailure(err -> dataLakeFilePromise.fail(err));
			}).onFailure(err -> dataLakeFilePromise.fail(err));
		})
		.onFailure(err -> dataLakeFilePromise.fail(err));
		return dataLakeFilePromise.future();	
	}	
	
	/**
	 * Write the data from an {@link OutputStream} to the datalake, creating a new file 
	 * @param targetGitFile the target {@link GitFile} the upload data will be associated with 
	 * @param os the {@link OutputStream} containing the data for the actual file.
	 * @return a {@link Future} of the {@link TaskResourceFile} that contains all information about the datalake file created. 
	 */
	public Future<TaskResourceFile> writeStreamToDataLake(GitFile targetGitFile, InputStream is)
	{
		Promise<TaskResourceFile> dataLakeFilePromise = Promise.promise();
		String targetFolder = Path.of(dataLakeDirectory, targetGitFile.getRepoID()).toAbsolutePath().toString();		
		vertx.fileSystem().mkdirs(targetFolder)
		.onSuccess(folderCreated -> {			
			vertx.fileSystem().createTempFile(targetFolder , "", "","rw-rw----")
			.onSuccess(tempFileName -> {
				vertx.executeBlocking(writing -> {
					try {
						FileOutputStream outputStream = new FileOutputStream(tempFileName);
						{
							int data = is.read();
							while(data != -1){
								outputStream.write(data);
								data = is.read();
							}
						}	                    
					}
					catch(IOException e)
					{
						writing.fail(e);
					}
					writing.complete();
				}).onSuccess(done -> {
					TaskResourceFile trf = new TaskResourceFile(targetGitFile.getRepoID(), tempFileName.replace(targetFolder, ""), targetGitFile.getFileName(), MimeMapping.getMimeTypeForFilename(targetGitFile.getFileName()));
					dataLakeFilePromise.complete(trf);
				})
				.onFailure(err -> dataLakeFilePromise.fail(err));

			})
			.onFailure(err -> dataLakeFilePromise.fail(err));		
		})
		.onFailure(err -> dataLakeFilePromise.fail(err));
		return dataLakeFilePromise.future();	
	}	


	public Future<Void> deleteFile(TaskResourceFile toDelete)
	{
		return vertx.fileSystem().delete(toDelete.getDataLakeFile().getAbsolutePath());
	}
	
}
