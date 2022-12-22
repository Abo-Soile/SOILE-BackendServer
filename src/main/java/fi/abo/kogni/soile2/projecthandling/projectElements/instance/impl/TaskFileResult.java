package fi.abo.kogni.soile2.projecthandling.projectElements.instance.impl;

import java.io.FileNotFoundException;
import java.nio.file.Path;

import org.bouncycastle.util.test.TestRandomEntropySourceProvider;

import fi.abo.kogni.soile2.datamanagement.datalake.DataLakeFile;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.file.FileSystem;
import io.vertx.core.impl.future.SucceededFuture;

/**
 * A Task File Result maps a large file that was a result of a task from the name the file was given in the task 
 * to the local file where it was stored. The result needs to have a "step" info, as a task might be visited twice
 * a TaskFileResult further needs a particpantID for which it is a result along with the task of which it is a result.
 * The Folder in which the File will be stored is:
 * "BaseFolder"/participantID/taskID/step/localFileName
 * It also needs the file format for the file, if it is requested individually. 
 * Access to Result files should ALWAYS happen via TaskFileResults.
 * @author Thomas Pfau
 *
 */
public class TaskFileResult {

	public String originalFileName;
	public String localFileName;
	public String fileFormat;
	private int step;
	private String taskID;
	private String participantID;

	/**
	 * Generate a new Fileresult for a given task. 
	 * @param originialFileName the original ID of the file as supplied by the project generation
	 * @param localFileName the name of the file stored locally
	 * @param fileFormat the type of the file, essentially the file extension
	 * @param taskID the id of the task this file belongs to.
	 * @param participantID the id of the participant this file belongs to.
	 */
	public TaskFileResult(String originialFileName, String localFileName, String fileFormat, int step, String taskID, String participantID) {
		this.originalFileName = originialFileName;
		this.localFileName = localFileName;
		this.fileFormat = fileFormat;
		this.step = step;
		this.participantID = participantID;
		this.taskID = taskID;
	}

	/**
	 * Get the filename that was originally set for this result.
	 * @return The original filename
	 */
	public String getPublicFileName()
	{
		return originalFileName;
	}
	
	/**
	 * get the File object corresponding to this Result
	 * @param dataLakeFolder the project folder to which this task belongs, and where it is herefore stored.
	 * @return a File handle, if the file exists.
	 */
	public DataLakeFile getFile(String dataLakeFolder) 
	{		
		String filePath = getFilePathInDataLake(dataLakeFolder);
		DataLakeFile f = new DataLakeFile(filePath,Path.of(getRelativeFolderPath(),originalFileName).toString(),fileFormat);		
		return f;		
	}
	
	public String getFilePathInDataLake(String ProjectPath)
	{
		return Path.of(ProjectPath, getFilePath()).toString();
	}
	

	public String getFolderPath(String dataLakeFolder)
	{
		return Path.of(dataLakeFolder, getRelativeFolderPath(),Integer.toString(step),taskID).toString();
	}

	public void setLocalFileName(String filename)
	{
		this.localFileName = filename;
	}
	
	private String getRelativeFolderPath()
	{
		return Path.of(participantID,Integer.toString(step),taskID).toString();
	}
	
	private String getFilePath()
	{
		return Path.of(participantID,Integer.toString(step),taskID,localFileName).toString();
	}
	
}
