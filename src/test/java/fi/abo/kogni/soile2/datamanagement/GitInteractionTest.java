package fi.abo.kogni.soile2.datamanagement;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import fi.aalto.scicomp.gitFs.gitProviderVerticle;
import fi.abo.kogni.soile2.SoileBaseTest;
import fi.abo.kogni.soile2.datamanagement.git.GitFile;
import fi.abo.kogni.soile2.datamanagement.git.ObjectManager;
import fi.abo.kogni.soile2.datamanagement.git.GitResourceManager;
import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.FileUpload;

@RunWith(VertxUnitRunner.class)
public class GitInteractionTest extends SoileBaseTest{

	File tmpFolder;
	
	@Override
	public void runBeforeTests(TestContext context)
	{
		try {
			tmpFolder = Files.createTempDirectory("SoileTMP").toFile();	
		}
		catch(IOException e)
		{
			context.fail(e);
			return;
		}
		vertx.deployVerticle(new gitProviderVerticle(SoileConfigLoader.getServerProperty("gitVerticleAddress"), tmpFolder.getAbsolutePath()), context.asyncAssertSuccess());
	}
	@Test
	public void testGitRepoExists(TestContext context)
	{
		String targetElement = "TestElement";
		GitResourceManager rm = new GitResourceManager(vertx.eventBus());
		Async notExistAsync = context.async();
		rm.existElementRepo(targetElement).onSuccess(exists -> {
			Async existAsync = context.async();
			context.assertFalse(exists);
			initGitRepo(targetElement, context).onSuccess(unused -> {
				rm.existElementRepo(targetElement).onSuccess(existNow -> {
					context.assertTrue(existNow);
					existAsync.complete();
				}).onFailure(fail -> {
					context.fail(fail);
					existAsync.complete();
				});
			}).onFailure(fail -> {
				context.fail(fail);
				existAsync.complete();
			});
			notExistAsync.complete();
		});
	}
	@Test
	public void testGitResourceItemManagement(TestContext context) 
	{
		// set up the git Repository and the DataLake folder we will use for the test
		Async initAsync = context.async();
		String targetElement = "TestElement";
		GitResourceManager rm = new GitResourceManager(vertx.eventBus());
		ObjectManager om = new ObjectManager(vertx.eventBus());		
		initGitRepo(targetElement, context).onSuccess(initialVersion -> 
		{		
			Path dataPath = Paths.get(getClass().getClassLoader().getResource("FilterData.json").getPath());
			Path dataLakePath = Paths.get(SoileConfigLoader.getServerProperty("soileGitDataLakeFolder"), targetElement, dataPath.getFileName().toString());
			try
			{	
				// To properly test this, we need to manually create the git DataLake Folder for this element.
				FileUtils.forceMkdir(Paths.get(SoileConfigLoader.getServerProperty("soileGitDataLakeFolder"), targetElement).toFile());
				Files.copy(dataPath,dataLakePath);				
			}
			catch(Exception e)
			{
				e.printStackTrace(System.out);
				context.fail(e.getMessage());
				initAsync.complete();
				return;
			}
			SimpleFileUpload upload = new SimpleFileUpload(dataLakePath.getFileName().toString(), dataPath.getFileName().toString());
			
			Async writeAsync = context.async();
			rm.writeElement(new GitFile("NewFile.txt", targetElement, initialVersion), upload).onSuccess(newVersion -> 
			{
				Async reloadAsync = context.async();
				// test that the new version has the file
				rm.getElement(new GitFile("NewFile.txt", targetElement, newVersion)).onSuccess(targetFile -> 
				{
					context.assertEquals(targetFile.getOriginalFileName(), "NewFile.txt");
					context.assertEquals(targetFile.getFilePath(), dataLakePath.toFile().getPath());
					reloadAsync.complete();
				}).onFailure(fail -> {	
					fail.printStackTrace(System.out);
					context.fail(fail.getMessage());
					reloadAsync.complete();
				});
				Async omTestFileFail = context.async();
				// now testing, that om cannot access the file without resource/ being specified as prefix
				om.getElement(new GitFile("NewFile.txt", targetElement, newVersion)).onSuccess(targetFile -> 
				{
					context.fail("This should not be retrievable");
					omTestFileFail.complete();
				}).onFailure(fail -> {	
					omTestFileFail.complete();					
				});
				// But it should be obtainable IF we put in the rprefix
				Async omTestFile = context.async();

				om.getElement(new GitFile("resources/NewFile.txt", targetElement, newVersion)).onSuccess(json -> 
				{
					context.assertTrue(json.containsKey("filename"));
					context.assertEquals("NewFile.txt", json.getString("filename"));
					// Now this will be incorrect, as the file contents indicate a different file name, which should make you suspect that this is NOT . 					
					omTestFile.complete();
				}).onFailure(fail -> {	
					context.fail("This should not be retrievable");
					omTestFile.complete();					
				});
				
				Async origHasNoFile = context.async();
				rm.getElement(new GitFile("NewFile.txt", targetElement, initialVersion)).onSuccess(targetFile -> 
				{
					context.fail("This should not succeed");
					origHasNoFile.complete();
				}).onFailure(fail -> {					
					origHasNoFile.complete();
				});
				writeAsync.complete();
			}).onFailure(fail -> {
				context.fail(fail.getMessage());
				writeAsync.complete();
			});
			initAsync.complete();
		}).onFailure(fail -> {
			System.out.println(fail);
			context.fail(fail.getMessage());
		});		
		
	}
	
	@Test
	public void testGitObjectItemManagement(TestContext context) 
	{
		// set up the git Repository and the DataLake folder we will use for the test
		Async initAsync = context.async();
		String targetElement = "TestElement";		
		initGitRepo(targetElement, context).onSuccess(initialVersion -> 
		{		
			ObjectManager om = new ObjectManager(vertx.eventBus());
			Path dataPath = Paths.get(getClass().getClassLoader().getResource("FilterData.json").getPath());
			Path dataLakePath = Paths.get(SoileConfigLoader.getServerProperty("soileGitDataLakeFolder"), targetElement, dataPath.getFileName().toString());
			try
			{	
				// To properly test this, we need to manually create the git DataLake Folder for this element.
				FileUtils.forceMkdir(Paths.get(SoileConfigLoader.getServerProperty("soileGitDataLakeFolder"), targetElement).toFile());
				Files.copy(dataPath,dataLakePath);				
			}
			catch(Exception e)
			{
				e.printStackTrace(System.out);
				context.fail(e.getMessage());
				initAsync.complete();
				return;
			}
			JsonObject testJson = new JsonObject().put("testField", "testData").put("testField2", false);
			Async writeAsync = context.async();
			om.writeElement(new GitFile("NewFile.txt", targetElement, initialVersion), testJson).onSuccess(newVersion -> 
			{
				Async reloadAsync = context.async();
				// test that the new version has the file
				om.getElement(new GitFile("NewFile.txt", targetElement, newVersion)).onSuccess(targetJson -> 
				{
					context.assertEquals("testData",targetJson.getString("testField"));
					context.assertFalse(targetJson.getBoolean("testField2"));
					reloadAsync.complete();
				}).onFailure(fail -> {	
					fail.printStackTrace(System.out);
					context.fail(fail.getMessage());
					reloadAsync.complete();
				});
				Async origHasNoFile = context.async();
				om.getElement(new GitFile("NewFile.txt", targetElement, initialVersion)).onSuccess(targetFile -> 
				{
					context.fail("This should not succeed");
					origHasNoFile.complete();
				}).onFailure(fail -> {					
					origHasNoFile.complete();
				});
				writeAsync.complete();
			}).onFailure(fail -> {
				context.fail(fail.getMessage());
				writeAsync.complete();
			});
			initAsync.complete();
		}).onFailure(fail -> {
			System.out.println(fail);
			context.fail(fail.getMessage());
		});		
		
	}
	
	
	@After
	public void clearGitRepo()
	{
		try
		{
			FileUtils.deleteDirectory(tmpFolder);
		}
		catch(Exception e)
		{
			e.printStackTrace(System.out);
		}
	}
	@After
	public void cleargitLake()
	{
		try
		{
			FileUtils.deleteDirectory(new File(SoileConfigLoader.getServerProperty("soileGitDataLakeFolder")));
		}
		catch(Exception e)
		{
			e.printStackTrace(System.out);
		}
	}
	
	public Future<String> initGitRepo(String gitRepoName, TestContext context)
	{		
		Promise<String> versionPromise = Promise.<String>promise();
		System.out.println("Requesting handling for git command:\n " + gitProviderVerticle.createInitCommand(gitRepoName).encodePrettily() + "\n\nUsing address: " + SoileConfigLoader.getServerProperty("gitVerticleAddress"));
		Async repoCreationAsync = context.async();
		vertx.eventBus().request(SoileConfigLoader.getServerProperty("gitVerticleAddress"), gitProviderVerticle.createInitCommand(gitRepoName))
		.onSuccess( res -> {
			versionPromise.complete(((JsonObject)res.body()).getString(gitProviderVerticle.COMMITHASHFIELD));
			repoCreationAsync.complete();
		}		
		).onFailure(fail ->
		{
			versionPromise.fail(fail);
			repoCreationAsync.complete();
		});		
		return versionPromise.future();
	}
	
	
	private class SimpleFileUpload implements FileUpload	
	{
		
		private String uploadFileName;
		private String originalFileName;
		public SimpleFileUpload(String uploadedFileName, String originalFileName)
		{	
			System.out.println(uploadedFileName);
			System.out.println(originalFileName);
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
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public String contentType() {
			// TODO Auto-generated method stub
			return null;
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
}
