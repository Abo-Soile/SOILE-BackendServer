package fi.abo.kogni.soile2.projecthandling.projectElements.impl;

import java.nio.file.Path;

import fi.abo.kogni.soile2.datamanagement.datalake.DataLakeFile;
import fi.abo.kogni.soile2.utils.SoileConfigLoader;

/**
 * This class encapsulates information on how files for individual Elements are stored. 
 * This allows removal of whole tasks from the storage
 * @author Thomas Pfau
 *
 */
public class TaskResourceFile {

	String dataLakeFolder;
	String mimeFormat;
	String targetName;
	String originalFileName;
	String elementID;
	
	public TaskResourceFile(String elementID, String TargetName, String OriginalFileName, String mimeFormat)
	{
		this.targetName = TargetName;
		this.originalFileName = OriginalFileName;
		this.mimeFormat = mimeFormat;
		this.dataLakeFolder = SoileConfigLoader.getServerProperty("soileGitDataLakeFolder");
		this.elementID = elementID;
	}	
	
	public DataLakeFile getDataLakeFile()
	{
		return new DataLakeFile(Path.of(dataLakeFolder, elementID, targetName).toAbsolutePath().toString(), originalFileName, mimeFormat);
	}
	
	/**
	 * Get the Name of the file in the element Folder of the dataLake.
	 * @return the name of the file within the datalake folder for the element this belongs to.
	 */
	public String getFileInDataLake()
	{
		return targetName;
	}
	
}
