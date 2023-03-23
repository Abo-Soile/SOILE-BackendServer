package fi.abo.kogni.soile2.projecthandling.utils;

import io.vertx.ext.web.FileUpload;

/**
 * A helper class to generate a simple file upload for unit testing
 * @author Thomas Pfau
 *
 */
public class SimpleFileUpload implements FileUpload	
{
	
	private String uploadFileName;
	private String originalFileName;
	public SimpleFileUpload(String uploadedFileName, String originalFileName)
	{	
		this.uploadFileName = uploadedFileName;
		this.originalFileName = originalFileName;
		
	}
	@Override
	public String name() {
		return null;
	}

	@Override
	public String uploadedFileName() {			
		return uploadFileName;
	}

	@Override
	public String fileName() {
		return originalFileName;
	}

	@Override
	public long size() {
		return 0;
	}

	@Override
	public String contentType() {
		return null;
	}

	@Override
	public String contentTransferEncoding() {
		return null;
	}

	@Override
	public String charSet() {
		return null;
	}

	@Override
	public boolean cancel() {
		return false;
	}
	
}