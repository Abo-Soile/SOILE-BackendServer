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
	
	/**
	 * Basic constructor given a real filename, the original Filename and the format of the file contents.
	 * @param path the actual location of the file
	 * @param originalFileName the Name this file originally had (as the path likely points to a random file name)
	 * @param mimeFormat the format of the file contents.
	 */
	public DataLakeFile(String path, String originalFileName, String mimeFormat) {
		super(path);
		this.originalFileName = originalFileName;
		this.mimeFormat = mimeFormat;
	}
	
	/**
	 * Constructor retrieving the necessary information from a {@link JsonObject}
	 * @param source
	 */
	public DataLakeFile(JsonObject source) {
		super(source.getString("AbsolutPath"));
		this.originalFileName = source.getString("originalFileName");
		this.mimeFormat = source.getString("mimeFormat");
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
				.put("absolutePath", getAbsolutePath())
				.put("originalFileName", originalFileName)
				.put("mimeFormat", mimeFormat);
	}

	

}
