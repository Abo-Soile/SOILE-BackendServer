package fi.abo.kogni.soile2.datamanagement;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Test;

import fi.abo.kogni.soile2.SoileBaseTest;
import fi.abo.kogni.soile2.datamanagement.datalake.ParticipantDataLakeManager;
import fi.abo.kogni.soile2.datamanagement.datalake.ParticipantFileResult;
import fi.abo.kogni.soile2.projecthandling.projectElements.ProjectTest;
import fi.abo.kogni.soile2.utils.DataProvider;
import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.web.FileUpload;

public class DataLakeManagerTest extends SoileBaseTest{

	String tempDataDir;

	@Override
	public void runBeforeTests(TestContext context)
	{	
		try 
		{
			String dataDir = DataLakeManagerTest.class.getClassLoader().getResource("FileTestData").getPath();
			tempDataDir = DataProvider.createTempDataDirectory(dataDir);
		}
		catch(IOException e)
		{
			context.fail(e);
		}
	}
	
	@After
	public void cleanUp(TestContext context)
	{	
		try 
		{
			FileUtils.deleteDirectory(new File(tempDataDir));
					
		}
		catch(IOException e)
		{
			context.fail(e);
		}
	}
	
	@Test
	public void storeFiles(TestContext context)
	{		
		Async testAsync = context.async();


		System.out.println("--------------------  Testing File storing ----------------------");		 
		ParticipantDataLakeManager dlm = new ParticipantDataLakeManager(SoileConfigLoader.getServerProperty("soileResultDirectory"), vertx);
		String partID = "Test";
		int step = 0;
		String taskID = "testTask";
		FileUpload tempUpload = DataProvider.getFileUploadForTarget(Path.of(tempDataDir,"ImageData.jpg").toString(), "TestImage.jpg", "image/jpg");		
		dlm.storeParticipantData(partID, step, taskID, tempUpload)
		.onSuccess(TargetFileName -> {			
			ParticipantFileResult res = new ParticipantFileResult(TargetFileName, "TestImage.jpg", "image/jpg", step, taskID, partID);
			context.assertTrue(vertx.fileSystem().existsBlocking(dlm.getFile(res).getAbsolutePath()));
			try
			{
				File f = new File(ProjectTest.class.getClassLoader().getResource("FileTestData/ImageData.jpg").getPath());
				File f2 =  dlm.getFile(res);
				InputStream inputStream1 = new FileInputStream(f);
				InputStream inputStream2 = new FileInputStream(f2);
				context.assertTrue(IOUtils.contentEquals(inputStream1, inputStream2));
				testAsync.complete();
			}
			catch(IOException e)
			{
				context.fail(e);
			}
		});


	}

}
