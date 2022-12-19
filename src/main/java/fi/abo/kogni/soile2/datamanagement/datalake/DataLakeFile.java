package fi.abo.kogni.soile2.datamanagement.datalake;

import java.io.File;
import java.io.Serializable;

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
	
	public DataLakeFile(String pathname, String originalFileName, String mimeFormat) {
		super(pathname);
		// TODO Auto-generated constructor stub
		this.originalFileName = originalFileName;
		this.mimeFormat = mimeFormat;
	}
	public DataLakeFile(JsonObject source) {
		super(source.getString("AbsolutPath"));
		// TODO Auto-generated constructor stub
		this.originalFileName = source.getString("originalFileName");
		this.mimeFormat = source.getString("mimeFormat");
	}
	
	
	public String getOriginalFileName()
	{
		return originalFileName;
	}
	
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
				.put("AbsolutPath", getAbsolutePath())
				.put("originalFileName", originalFileName)
				.put("mimeFormat", mimeFormat);
	}

	

}
