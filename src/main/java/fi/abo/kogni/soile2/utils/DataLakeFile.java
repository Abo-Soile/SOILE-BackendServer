package fi.abo.kogni.soile2.utils;

import java.io.File;

public class DataLakeFile extends File {
	
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
