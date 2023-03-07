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
	private String originalFileName;
	private String mimeFormat;
	private String pathInLake;
	private String dataLake;
	/**
	 * Basic constructor given a real filename, the original Filename and the format of the file contents.
	 * @param path the actual location of the file
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
	 * @param source
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
	 * @return the name for the file
	 */
	public void setOriginalFileName(String filename)
	{
		originalFileName = filename;
	}
	
	/**
	 * Get the file name within the datalake
	 * @return
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
	 * @return
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
