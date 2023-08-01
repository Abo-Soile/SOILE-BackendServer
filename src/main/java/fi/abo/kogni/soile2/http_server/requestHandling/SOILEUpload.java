package fi.abo.kogni.soile2.http_server.requestHandling;

import io.vertx.ext.web.FileUpload;

public interface SOILEUpload {

	/**
	 * Get the filename/location of the file represented by this upload
	 * @return the name of the uploaded file
	 */
	public String uploadedFileName();
	
	/**
	 * Get the filename of the uploaded file (this is the filename with which the file was uploaded, not necessarily the one it is stored as)
	 * @return The filename/location of the uploaded file
	 */
	public String fileName();
	
	/**
	 * Get the content type (MIME) of the uploaded file 
	 * @return The content type of the upload
	 */
	public String contentType();
	
	public static SOILEUpload create(FileUpload upload)
	{
		return new SOILEUploadImpl(upload);
	}
	
	public static SOILEUpload create(String uploadedFileName, String fileName, String contentType)
	{
		return new SOILEUploadImpl(fileName,uploadedFileName,contentType);
	}
}
