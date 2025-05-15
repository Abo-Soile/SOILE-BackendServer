package fi.abo.kogni.soile2.datamanagement.datalake;

import java.io.File;

import fi.abo.kogni.soile2.utils.SoileConfigLoader;

/**
 * This class encapsulates information on how files for individual Elements are stored. 
 * This allows removal of whole tasks from the storage
 * @author Thomas Pfau
 *
 */
public class GitDataLakeFile {

	String dataLakeFolder;
	String mimeFormat;
	String targetName;
	String originalFileName;
	String elementID;
	
	/**
	 * Default constructor
	 * @param repoID the ID of the repo this file is in
	 * @param TargetName the name of the target file
	 * @param OriginalFileName the original file name
	 * @param mimeFormat the mime format / format of the file
	 */
	public GitDataLakeFile(String repoID, String TargetName, String OriginalFileName, String mimeFormat)
	{
		this.targetName = TargetName;
		this.originalFileName = OriginalFileName;
		this.mimeFormat = mimeFormat;
		this.dataLakeFolder = SoileConfigLoader.getServerProperty("soileGitDataLakeFolder");
		this.elementID = repoID;
	}	
	
	/**
	 * Get the DatalakeFile that this TaskResourceFile is associated with (i.e. an actual File Object, while this object only holds some data).
	 * @return The Corresponding {@link DataLakeFile} object
	 */
	public DataLakeFile getDataLakeFile()
	{
		return new DataLakeFile(dataLakeFolder, elementID + File.separator + targetName, originalFileName, mimeFormat);
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
