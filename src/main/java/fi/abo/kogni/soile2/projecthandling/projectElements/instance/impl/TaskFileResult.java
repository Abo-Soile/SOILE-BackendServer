package fi.abo.kogni.soile2.projecthandling.projectElements.instance.impl;

import java.nio.file.Path;

import fi.abo.kogni.soile2.datamanagement.datalake.DataLakeFile;

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

	public String resultFileName;
	public String fileFormat;
	public String storageFileName;
	private int step;
	private String taskID;
	private String participantID;

	/**
	 * Generate a new Fileresult for a given task. 
	 * @param originalFileName the original ID of the file as supplied by the project generation
	 * @param projectID The project ID in which this is a result 
	 * @param fileFormat the type of the file, essentially the file extension
	 * @param taskID the id of the task this file belongs to.
	 * @param participantID the id of the participant this file belongs to.
	 */
	public TaskFileResult(String storageFilename, String originalFileName, String fileFormat, int step, String taskID, String participantID) {
		this.resultFileName = originalFileName;		
		this.fileFormat = fileFormat;
		this.step = step;
		this.participantID = participantID;
		this.taskID = taskID;
		this.storageFileName = storageFilename;
	}

	/**
	 * Get the filename that was originally set for this result.
	 * @return The original filename
	 */
	public String getResultFileName()
	{
		return resultFileName;
	}
	
	/**
	 * get the File object corresponding to this Result
	 * @param dataLakeFolder the project folder to which this task belongs, and where it is herefore stored.
	 * @return a File handle, if the file exists.
	 */
	public DataLakeFile getFile(String dataLakeFolder) 
	{				
		
		return new DataLakeFile(dataLakeFolder, getFilePathInDataLake(),resultFileName, fileFormat);
	}
	
	/**
	 * Get the result in the given Project Path. 
	 * @return The relative Path of the file this result refers to. 
	 */
	public String getFilePathInDataLake()
	{
		return Path.of(getRelativeFolderPath(), storageFileName).toString();
	}

	/**
	 * Get the result in the given Project Path.
	 * @param dataLakeDirectory the datalake directory this file is stored in 
	 * @return The relative Path of the file this result refers to. 
	 */
	public String getFilePath(String dataLakeDirectory)
	{
		return Path.of(dataLakeDirectory, getFilePathInDataLake()).toString();
	}


	/**
	 * Get the Path to the file represented by this {@link TaskFileResult} based on the folder of the datalake it is stored in.
	 * @param dataLakeFolder the datalake folder.
	 * @return the absolute path of the file
	 */
	public String getFolderPath(String dataLakeFolder)
	{
		return Path.of(dataLakeFolder, getRelativeFolderPath()).toString();
	}

	/**
	 * Set the name of the local file (i.e. the actual file stored in he datalake.
	 * @param filename
	 */
	public void setLocalFileName(String filename)
	{
		this.storageFileName = filename;
	}
	
	private String getRelativeFolderPath()
	{
		return Path.of(participantID,Integer.toString(step),taskID).toString();
	}
	
	private String getFilePath()
	{
		return Path.of(getRelativeFolderPath(),storageFileName).toString();
	}
	
	public String toString()
	{
		return getFilePath();
	}
	
}
