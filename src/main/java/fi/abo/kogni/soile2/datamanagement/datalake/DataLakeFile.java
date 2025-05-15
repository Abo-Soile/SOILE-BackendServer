package fi.abo.kogni.soile2.datamanagement.datalake;

import java.io.File;

import fi.aalto.scicomp.zipper.FileDescriptor;
import io.vertx.core.json.JsonObject;

/**
 * This is a wrapper for a File in the datalake. While the datalake has unspecified filenames 
 * This wrapper allows associating a filename with such a randomly generated filename
 * In order to provide a human readable piece of information.  
 * @author Thomas Pfau 
 */
public class DataLakeFile extends File implements FileDescriptor {
	
	private static final long serialVersionUID = 1L;
	/**
	 * The original file name
	 */
	private String originalFileName;
	/**
	 * the mime format (file format)
	 */
	private String mimeFormat;
	/**
	 * The path in the datalake
	 */
	private String pathInLake;
	/**
	 * the data lake path on the os
	 */
	private String dataLake;
	/**
	 * Basic constructor given a real filename, the original Filename and the format of the file contents.
	 * @param dataLakePath the actual location of the file
	 * @param pathInLake the path within the lake
	 * @param originalFileName the Name this file originally had (as the path likely points to a random file name)
	 * @param mimeFormat the format of the file contents.
	 */
	public DataLakeFile(String dataLakePath, String pathInLake, String originalFileName, String mimeFormat) {
		super(dataLakePath + File.separator + pathInLake);
		this.pathInLake = pathInLake;
		this.dataLake = dataLakePath; 
		this.originalFileName = originalFileName;
		this.mimeFormat = mimeFormat;
	}
	
	/**
	 * Constructor retrieving the necessary information from a {@link JsonObject}
	 * @param source description of the Source as a JsonObject
	 */
	public DataLakeFile(JsonObject source) {
		super(source.getString("dataLake") + File.separator + source.getString("pathInLake"));
		this.originalFileName = source.getString("originalFileName");
		this.mimeFormat = source.getString("mimeFormat");
		this.dataLake = source.getString("dataLake") ;
		this.pathInLake = source.getString("pathInLake"); 
	}
	
	/**
	 * Get the original File name, i.e. how this file should normally be referred to.
	 * @return the name for the file
	 */
	public String getOriginalFileName()
	{
		return originalFileName;
	}

	/**
	 * Set the original File name, i.e. the place this file is expected to be at.
	 * @param filename the original filename to set
	 */
	public void setOriginalFileName(String filename)
	{
		originalFileName = filename;
	}
	
	/**
	 * Get the file name within the datalake
	 * @return the name of the file in the datalake
	 */
	 
	public String getFileNameInLake()
	{
		return pathInLake;
	}
	/**
	 * Get the format of this file (mimetype)
	 * @return the format specifier
	 */
	public String getFormat()
	{
		return mimeFormat;
	}

	@Override
	public String getFileName() { 
		return getOriginalFileName();
	}

	@Override
	public String getFilePath() {
		return getAbsolutePath();
	}
	
	/**
	 * Convert into Json Object for communication;
	 * @return a {@link JsonObject} representing this file
	 */
	public JsonObject toJson()
	{
		return new JsonObject()
				.put("pathInLake", this.pathInLake)
				.put("dataLake", this.dataLake)
				.put("originalFileName", originalFileName)
				.put("mimeFormat", mimeFormat);
	}

	

}
