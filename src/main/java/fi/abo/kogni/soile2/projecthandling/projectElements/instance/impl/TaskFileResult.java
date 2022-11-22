package fi.abo.kogni.soile2.projecthandling.projectElements.instance.impl;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * This class is more of a placeholder to keep a few things together:
 * 
 * @author Thomas Pfau
 *
 */
public class TaskFileResult {

	public String fileID;
	public String localFileName;
	public String fileFormat;
	public String fileFolder;

	/**
	 * Generate a new Fileresult for a given task. 
	 * @param fileID the original ID of the file as supplied by the project generation
	 * @param localFileName the name of the file stored locally
	 * @param fileFormat the type of the file, essentially the file extension
	 * @param taskID the id of the task this file belongs to.
	 * @param participantID the id of the participant this file belongs to.
	 */
	public TaskFileResult(String fileID, String localFileName, String fileFormat, String taskID, String participantID) {
		this.fileID = fileID;
		this.localFileName = localFileName;
		this.fileFormat = fileFormat;
		this.fileFolder = taskID + File.separator + participantID;
	}

	/**
	 * Get the filename that was originally set for this result.
	 * @return The original filename
	 */
	public String getPublicFileName()
	{
		return fileID;
	}
	
	/**
	 * get the File object corresponding to this Result
	 * @param ProjectFolder the project folder to which this task belongs, and where it is herefore stored.
	 * @return a File handle, if the file exists.
	 * @throws FileNotFoundException if the indicated file does not exist.
	 */
	public File getFile(String ProjectFolder) throws FileNotFoundException
	{
		File f = new File(ProjectFolder + File.separator + fileFolder+ File.separator + localFileName);
		if(f.exists())
		{
			return f;
		}
		else
		{
			throw new FileNotFoundException(f.getAbsolutePath());
		}
	}
	
}
