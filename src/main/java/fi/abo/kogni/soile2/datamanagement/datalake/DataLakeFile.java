package fi.abo.kogni.soile2.datamanagement.datalake;

import java.io.File;

/**
 * This is a wrapper for a File in the datalake. While the datalake has unspecified filenames 
 * This wrapper allows associating a filename with such a randomly generated filename
 * In order to provide a human readable piece of information.  
 * @author Thomas Pfau 
 */
public class DataLakeFile extends File {
	
	private static final long serialVersionUID = 1L;
	private String originalFileName;

	
	public DataLakeFile(String pathname, String originalFileName) {
		super(pathname);
		// TODO Auto-generated constructor stub
		this.originalFileName = originalFileName;
	}
	
	public String getOriginalFileName()
	{
		return originalFileName;
	}
	


}
