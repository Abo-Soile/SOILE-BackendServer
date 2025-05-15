




package fi.abo.kogni.soile2.http_server.requestHandling;

import io.vertx.ext.web.FileUpload;

/**
 * Implementation for a {@link SOILEUpload}
 * @author Thomas Pfau
 *
 */
public class SOILEUploadImpl implements SOILEUpload{

	private String fileName;
	private String uploadFileName;
	private String contentType;
	
	/**
	 * Constructor based on a vertx {@link FileUpload}
	 * @param upload the FileUpload to 
	 */
	public SOILEUploadImpl(FileUpload upload)
	{
		fileName = upload.fileName();
		uploadFileName = upload.uploadedFileName();
		contentType = upload.contentType();
	}

	@Override
	public String uploadedFileName() {
		// TODO Auto-generated method stub
		return uploadFileName;
	}
	/**
	 * Constructor with file details
	 * @param fileName the name of the file
	 * @param uploadFileName the name of the uploaded file
	 * @param contentType the content type of the file
	 */
	public SOILEUploadImpl(String fileName, String uploadFileName, String contentType) {
		super();
		this.fileName = fileName;
		this.uploadFileName = uploadFileName;
		this.contentType = contentType;
	}

	@Override
	public String fileName() {
		// TODO Auto-generated method stub
		return fileName;
	}

	@Override
	public String contentType() {
		// TODO Auto-generated method stub
		return contentType;
	}
}
