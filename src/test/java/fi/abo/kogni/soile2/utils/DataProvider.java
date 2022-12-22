package fi.abo.kogni.soile2.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.apache.commons.io.FileUtils;

import io.vertx.ext.web.FileUpload;

public class DataProvider {
	public static String createTempDataDirectory(String DirToCopy) throws IOException
	{
		String tmpDir = Files.createTempDirectory("SoileDataDirectory").toString();
		FileUtils.copyDirectory(new File(DirToCopy), new File(tmpDir));
		return tmpDir;
	}
	
	public static FileUpload getFileUploadForTarget(String localFile, String sourceFileName, String contentType)
	{
		return new MockFileUpload(localFile, sourceFileName, contentType);
		};
	}
	
	class MockFileUpload implements FileUpload
	{

		public MockFileUpload(String localFile, String sourceFileName, String contentType) {
			super();
			this.localFile = localFile;
			this.sourceFileName = sourceFileName;
			this.contentType = contentType;
		}

		String localFile;
		String sourceFileName;
		String contentType;
		@Override
		public String name() {
			// TODO Auto-generated method stub
			return localFile;
		}

		@Override
		public String uploadedFileName() {
			// TODO Auto-generated method stub
			return localFile;
		}

		@Override
		public String fileName() {
			// TODO Auto-generated method stub
			return sourceFileName;
		}

		@Override
		public long size() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public String contentType() {
			// TODO Auto-generated method stub
			return contentType;
		}

		@Override
		public String contentTransferEncoding() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String charSet() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public boolean cancel() {
			// TODO Auto-generated method stub
			return false;
		}
}
