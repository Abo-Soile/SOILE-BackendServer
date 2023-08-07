package fi.abo.kogni.soile2.http_server.routes;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import fi.aalto.scicomp.zipper.FileDescriptor;
import fi.aalto.scicomp.zipper.FileDescriptorImpl;
import fi.abo.kogni.soile2.datamanagement.DataLakeManagerTest;
import fi.abo.kogni.soile2.http_server.SoileWebTest;
import fi.abo.kogni.soile2.http_server.auth.SoileAuthorization.Roles;
import fi.abo.kogni.soile2.http_server.verticles.CodeRetrieverVerticle;
import fi.abo.kogni.soile2.http_server.verticles.CodeRetrieverVerticleTest;
import fi.abo.kogni.soile2.projecthandling.projectElements.TaskBundler;
import fi.abo.kogni.soile2.utils.WebObjectCreator;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.web.client.WebClientSession;
import io.vertx.ext.web.handler.HttpException;

public class TaskRouterTest extends SoileWebTest{

	@Test
	public void testGetResource(TestContext context)
	{		 
		System.out.println("--------------------  Testing Web get Task Resources  ----------------------");

		Async testAsync = context.async();
		createUserAndAuthedSession("TestUser", "testpw", Roles.Researcher)
		.onSuccess(authedSession -> {
			createUserAndAuthedSession("TestUser2", "testpw", Roles.Researcher)
			.onSuccess(wrongSession -> {				 
				WebObjectCreator.createOrRetrieveTask(authedSession, "FirstTask")
				.onSuccess(taskData -> {
					Async testResource = context.async();
					String resourceAddress = "/task/" + taskData.getString("UUID") + "/" + taskData.getString("version") + "/resource/ImageData.jpg";
					GET(authedSession,resourceAddress,null,null )
					.onSuccess(result -> {
						String targetFileName = tmpDir + File.separator + "taskRouter.out"; 
						vertx.fileSystem().writeFile(targetFileName , result.bodyAsBuffer())
						.onSuccess(
								res -> 
								{
									try {
										context.assertTrue( 
												areFilesEqual(
														new File(targetFileName),
														new File(DataLakeManagerTest.class.getClassLoader().getResource("FileTestData/ImageData.jpg").getPath())
														)
												);
										testResource.complete();

									}
									catch(IOException e)
									{
										context.fail(e);
									}
								}
								// compare that the file is the same as the original one.
								)
						.onFailure(err -> context.fail(err));
					})
					.onFailure(err -> context.fail(err));
					Async invalidAccessAsync = context.async();
					GET(wrongSession,resourceAddress,null,null )
					.onSuccess(err -> {
						context.fail("Should not be allowed");
					})
					.onFailure(err -> {
						context.assertEquals(403, ((HttpException)err).getStatusCode());
						invalidAccessAsync.complete();
					});

					Async invalidFileAsync = context.async();					 
					GET(authedSession,"/task/" + taskData.getString("UUID") + "/" + taskData.getString("version") + "/resource/Something.jpg",null,null )
					.onSuccess(err -> {
						context.fail("Should not be possible");
					})
					.onFailure(err -> {
						context.assertEquals(404, ((HttpException)err).getStatusCode());
						invalidFileAsync.complete();
					});


					testAsync.complete();
				})
				.onFailure(err -> context.fail(err));

			})
			.onFailure(err -> context.fail(err));

		})
		.onFailure(err -> context.fail(err));
	}


	@Test
	public void testRunResources(TestContext context)
	{

		Async testRunResAsync = context.async();
		createUserAndAuthedSession("TestUser", "testpw", Roles.Researcher)
		.onSuccess(authedSession -> {
			createUserAndAuthedSession("TestUser2", "testpw", Roles.Researcher)
			.onSuccess(wrongSession -> {				 
				WebObjectCreator.createOrRetrieveTask(authedSession, "Initial_Questionaire")
				.onSuccess(taskData -> {									
					GET(authedSession, "/task/" + taskData.getString("UUID") + "/" + taskData.getString("version") + "/execute/ImageData.jpg", null, null)
					.onSuccess(failed -> {
						context.fail("The first Task does NOT have any resources!");
					})
					.onFailure(response -> {
						context.assertEquals(404, ((HttpException)response).getStatusCode());
						WebObjectCreator.createOrRetrieveTask(authedSession, "FirstTask")
						.onSuccess(taskWithImage -> {
							Async invalidAccess = context.async();						 
							GET(wrongSession, "/task/" + taskWithImage.getString("UUID") + "/" + taskWithImage.getString("version") + "/execute/ImageData.jpg", null, null)
							.onSuccess(failed -> {
								context.fail("The first Task does NOT have any resources!");
							})
							.onFailure(invalid -> {
								context.assertEquals(403, ((HttpException)invalid).getStatusCode());
								invalidAccess.complete();
							});
							GET(authedSession, "/task/" + taskWithImage.getString("UUID") + "/" + taskWithImage.getString("version") + "/execute/ImageData.jpg", null, null)
							.onSuccess(dataResponse ->  {
								String targetFileName = tmpDir + File.separator + "taskRouter.out"; 
								vertx.fileSystem().writeFile(targetFileName , dataResponse.bodyAsBuffer())
								.onSuccess(res -> {
									// compare that the file is the same as the original one.
									try {
										context.assertTrue( 
												areFilesEqual(
														new File(targetFileName),
														new File(DataLakeManagerTest.class.getClassLoader().getResource("FileTestData/ImageData.jpg").getPath())
														)
												);
										testRunResAsync.complete();

									}
									catch(IOException e)
									{
										context.fail(e);
									}
								})					 
								.onFailure(err -> context.fail(err));
							})
							.onFailure(err -> context.fail(err));

						})
						.onFailure(err -> context.fail(err));

					});
				})
				.onFailure(err -> context.fail(err));
			})
			.onFailure(err -> context.fail(err));
		})
		.onFailure(err -> context.fail(err));
	}

	@Test
	public void testRunLibs(TestContext context)
	{
		System.out.println("--------------------  Running Tests for /run/{id}/lib/*  ----------------------");    

		Async testRunResource = context.async();
		createUserAndAuthedSession("TestUser", "testpw", Roles.Researcher)
		.onSuccess(authedSession -> {
			createUserAndAuthedSession("TestUser2", "testpw", Roles.Researcher)
			.onSuccess(wrongSession -> {				 
				WebObjectCreator.createOrRetrieveTask(authedSession, "FirstTask")
				.onSuccess(taskData -> {
					GET(authedSession, "/task/" + taskData.getString("UUID") + "/" + taskData.getString("version") + "/execute/lib/testlib.js", null, null)
					.onSuccess(response -> {
						context.assertTrue(response.bodyAsString().contains("console.log"));
						GET(wrongSession, "/task/" + taskData.getString("UUID") + "/" + taskData.getString("version") + "/execute/lib/testlib.js", null, null)
						.onFailure(fail -> {
							context.assertEquals(403, ((HttpException)fail).getStatusCode());

							testRunResource.complete();
						})
						.onSuccess(err -> context.fail("Should not be allowed"));	
					})
					.onFailure(err -> context.fail(err));
					Async failedLibAsync = context.async();
					GET(authedSession, "/task/" + taskData.getString("UUID") + "/" + taskData.getString("version") + "/execute/lib/testlib2.js", null, null)
					.onSuccess(response -> {
						context.fail("Should not be possible");
					})
					.onFailure(err ->
					{
						if( err instanceof HttpException)
						{
							context.assertEquals(404, ((HttpException)err).getStatusCode());
							failedLibAsync.complete();
						}
						else
						{
							context.fail(err);
						}

					});
				})
				.onFailure(err -> context.fail(err));
			})
			.onFailure(err -> context.fail(err));
		})
		.onFailure(err -> context.fail(err));
	}

	@Test
	public void runTaskTest(TestContext context)
	{
		Async runTestAsync = context.async();	
		System.out.println("--------------------  Running Tests for /task/{id}/{version}/execute ----------------------");    
		createUserAndAuthedSession("TestUser", "testpw", Roles.Researcher)
		.onSuccess(authedSession -> {
			createUserAndAuthedSession("TestUser2", "testpw", Roles.Researcher)
			.onSuccess(wrongSession -> {	
				Async initQuest = context.async();				
				WebObjectCreator.createOrRetrieveTask(authedSession, "Initial_Questionaire")
				.onSuccess(taskData -> {									

					Async correctSessionAsync  = context.async();
					GET(authedSession, "/task/" + taskData.getString("UUID") + "/" + taskData.getString("version") + "/execute", null, null)
					.onSuccess(response -> {
						context.assertEquals("application/json", response.headers().get("content-type"));					
						JsonObject compiledCode = response.bodyAsJsonObject();
						context.assertTrue(compiledCode.containsKey("elements"));
						context.assertEquals("html", compiledCode.getJsonArray("elements").getJsonArray(0).getJsonObject(0).getString("type"));
						context.assertEquals("",compiledCode.getString("title"));						
						correctSessionAsync.complete();
					})
					.onFailure(err -> context.fail(err));
					Async incorrectSessionAsync  = context.async();
					GET(wrongSession, "/task/" + taskData.getString("UUID") + "/" + taskData.getString("version") + "/execute", null, null)
					.onSuccess(response -> {
						context.assertEquals("application/json", response.headers().get("content-type"));					
						JsonObject compiledCode = response.bodyAsJsonObject();
						context.assertTrue(compiledCode.containsKey("elements"));
						context.assertEquals("html", compiledCode.getJsonArray("elements").getJsonArray(0).getJsonObject(0).getString("type"));
						context.assertEquals("",compiledCode.getString("title"));						
						incorrectSessionAsync.complete();
					})
					.onFailure(err -> context.fail(err));
					initQuest.complete();
				})
				.onFailure(err -> context.fail(err));
				Async firstTask = context.async();				
				WebObjectCreator.createOrRetrieveTask(authedSession, "FirstTask")
				.onSuccess(taskData -> {									

					Async correctSessionAsync  = context.async();
					GET(authedSession, "/task/" + taskData.getString("UUID") + "/" + taskData.getString("version") + "/execute", null, null)
					.onSuccess(response -> {
						context.assertEquals("application/javascript", response.headers().get("content-type"));					
						String compiledCode = response.bodyAsString();
						context.assertTrue(compiledCode.contains("SOILE2"));
						context.assertTrue(compiledCode.contains("opcode"));
						correctSessionAsync.complete();
					})
					.onFailure(err -> context.fail(err));
					Async incorrectSessionAsync  = context.async();
					GET(wrongSession, "/task/" + taskData.getString("UUID") + "/" + taskData.getString("version") + "/execute", null, null)
					.onSuccess(response -> context.fail("User has no Access"))																
					.onFailure(invalid -> {
						context.assertEquals(403, ((HttpException)invalid).getStatusCode());
						incorrectSessionAsync.complete();
					});
					firstTask.complete();
				})
				.onFailure(err -> context.fail(err));		
				runTestAsync.complete();
			})
			.onFailure(err -> context.fail(err));
		})
		.onFailure(err -> context.fail(err));
	}

	@Test
	public void runGetFileListForTaskTest(TestContext context)
	{
		Async runTestAsync = context.async();
		String usedTask ="FileRead";
		try
		{
			JsonObject TaskDef = new JsonObject(Files.readString(Paths.get(WebObjectCreator.class.getClassLoader().getResource("APITestData/TaskData.json").getPath()))).getJsonObject(usedTask);			

			System.out.println("--------------------  Running Tests for /task/{id}/{version}/filelist ----------------------");    
			createUserAndAuthedSession("TestUser", "testpw", Roles.Researcher)
			.onSuccess(authedSession -> {
				createUserAndAuthedSession("TestUser2", "testpw", Roles.Researcher)
				.onSuccess(wrongSession -> {	
					Async firstTask = context.async();				
					WebObjectCreator.createOrRetrieveTask(authedSession, "FileRead")
					.onSuccess(taskData -> {									
						Async correctSessionAsync  = context.async();
						GET(authedSession, "/task/" + taskData.getString("UUID") + "/" + taskData.getString("version") + "/filelist", null, null)
						.onSuccess(response -> {										
							JsonArray fileList = response.bodyAsJsonArray();
							context.assertEquals(5, fileList.size());	
							JsonArray resources = TaskDef.getJsonArray("resources").copy();
							checkAndClear(resources, fileList, "", context);
							// now all expected resources should be cleared
							context.assertEquals(0, resources.size());
							correctSessionAsync.complete();
						})
						.onFailure(err -> context.fail(err));
						Async incorrectSessionAsync  = context.async();
						GET(wrongSession, "/task/" + taskData.getString("UUID") + "/" + taskData.getString("version") + "/filelist", null, null)
						.onSuccess(response -> context.fail("User has no Access"))																
						.onFailure(invalid -> {
							context.assertEquals(403, ((HttpException)invalid).getStatusCode());
							incorrectSessionAsync.complete();
						});
						firstTask.complete();
					})
					.onFailure(err -> context.fail(err));		
					runTestAsync.complete();
				})
				.onFailure(err -> context.fail(err));
			})
			.onFailure(err -> context.fail(err));
		}
		catch(Exception e)
		{
			context.fail(e);
		}
	}

	@Test
	public void testCodeCompilation(TestContext context)
	{
		System.out.println("--------------------  Testing Elang Verticle ----------------------");
		try
		{			
			String originalCode2 = Files.readString(Paths.get(CodeRetrieverVerticleTest.class.getClassLoader().getResource("CodeTestData/FirstTask.elang").getPath()));
			
			String failingCode = Files.readString(Paths.get(CodeRetrieverVerticleTest.class.getClassLoader().getResource("CodeTestData/FirstTask_Error.elang").getPath()));
			Async sessionAsync = context.async();
			createUserAndAuthedSession("TestUser", "testpw", Roles.Researcher).
			onSuccess(authedSession -> {
				Async compilation2Async = context.async();
				JsonObject CompileRequest = new JsonObject().put("code", originalCode2).put("type", CodeRetrieverVerticle.ELANG).put("version", "1.0");
				POST(authedSession, "/task/compile", null, CompileRequest)
				.onSuccess(response -> {
					String code = response.bodyAsString();
					// this could be made more explicit, testing actual contents.					
					context.assertTrue(code.contains("SOILE2"));
					compilation2Async.complete();				
				})
				.onFailure(err -> context.fail(err));
							
				Async compilationAsync = context.async();
				JsonObject CompileRequest2 = new JsonObject().put("code", failingCode).put("type", CodeRetrieverVerticle.ELANG).put("version", "1.0");			
				POST(authedSession, "/task/compile", null, CompileRequest2)
				.onSuccess(reply-> {
					System.out.println(reply.body());
					context.fail("Should have failed since code does not compile");								
				})
				.onFailure(err -> {
					compilationAsync.complete();
				});
				sessionAsync.complete();
			})
			.onFailure(err -> context.fail(err));			

		}
		catch(IOException e)
		{
			context.fail(e);
		}
	}
	
	@Test
	public void downloadTaskTest(TestContext context)
	{
		Async runTestAsync = context.async();
		Async taskDownloaded = context.async();
		Async taskUploaded = context.async();
		String usedTask ="FileRead";
		try
		{
			String ResourceFolder = Paths.get(WebObjectCreator.class.getClassLoader().getResource("FileTestData").getPath()).toString();;
			JsonObject TaskDef = new JsonObject(Files.readString(Paths.get(WebObjectCreator.class.getClassLoader().getResource("APITestData/TaskData.json").getPath()))).getJsonObject(usedTask);			

			System.out.println("--------------------  Running Tests for /task/{id}/{version}/download ----------------------");    
			createUserAndAuthedSession("TestUser", "testpw", Roles.Researcher)
			.onSuccess(authedSession -> {
				createUserAndAuthedSession("TestUser2", "testpw", Roles.Researcher)
				.onSuccess(wrongSession -> {	
					Async firstTask = context.async();				
					WebObjectCreator.createOrRetrieveTask(authedSession, "FileRead")
					.onSuccess(taskData -> {									
						Async correctSessionAsync  = context.async();
						String targetFileName = "/tmp/Task.tar.gz";
						downloadTaskAndWriteToFile(authedSession, taskData.getString("UUID"), taskData.getString("version"), targetFileName)
						.onSuccess( dlSaved -> 
						{
							String targetDir = "/tmp" + File.separator + "TaskDownload" + File.separator + "out";
							 
							JsonArray resources = TaskDef.getJsonArray("resources");
							try {
								unzip(targetFileName, targetDir);									
								JsonObject cfgJson = new JsonObject(Files.readString(Path.of(targetDir,TaskBundler.CONFIGFILE_NAME)));
								context.assertEquals("Test3", cfgJson.getString("name"));
								context.assertTrue(cfgJson.getBoolean("private"));
								context.assertEquals(TaskDef.getJsonObject("codeType"), cfgJson.getJsonObject("codeType"));
								for(int i = 0; i < resources.size(); ++i)
								{
									String fileName = resources.getString(i);									
									context.assertTrue(areFilesEqual(new File(Path.of(ResourceFolder, fileName).toString()), new File(Path.of(targetDir, fileName).toString())));
								}
								// clean up.
								FileUtils.deleteDirectory(new File(targetDir));									
							}
							catch(Exception e)
							{
								context.fail(e);
							}				
							JsonObject queryParams = new JsonObject().put("tag", "Initial Version").put("name", "NewTask");							
							upload(authedSession, "/task/upload", queryParams, "NewTask.zip", new File(targetFileName), "application/zip")
							.onSuccess(response -> {
								String newTaskID = response.getString("UUID");
								String newTaskVersion = response.getString("version");
								downloadTaskAndWriteToFile(authedSession, newTaskID, newTaskVersion, targetFileName)
								.onSuccess( dlSaved2 -> 
								{										
									try {
										unzip(targetFileName, targetDir);									
										JsonObject cfgJson = new JsonObject(Files.readString(Path.of(targetDir,TaskBundler.CONFIGFILE_NAME)));
										context.assertEquals("NewTask", cfgJson.getString("name"));
										context.assertTrue(cfgJson.getBoolean("private"));
										context.assertEquals(TaskDef.getJsonObject("codeType"), cfgJson.getJsonObject("codeType"));
										for(int i = 0; i < resources.size(); ++i)
										{
											String fileName = resources.getString(i);									
											context.assertTrue(areFilesEqual(new File(Path.of(ResourceFolder, fileName).toString()), new File(Path.of(targetDir, fileName).toString())));
										}
										// clean up.
										FileUtils.deleteDirectory(new File(targetDir));
										taskUploaded.complete();	
									}
									catch(Exception e)
									{
										context.fail(e);
									}
								})
								.onFailure(err -> context.fail(err));
							})
							.onFailure(err -> context.fail(err));

							taskDownloaded.complete();
							correctSessionAsync.complete();

							//submitAsync.complete();							
						})
						.onFailure(err -> context.fail(err));						
						firstTask.complete();
					})
					.onFailure(err -> context.fail(err));		
					runTestAsync.complete();
				})
				.onFailure(err -> context.fail(err));
			})
			.onFailure(err -> context.fail(err));
		}
		catch(Exception e)
		{
			context.fail(e);
		}
	}
	
	
	
	@Test
	public void deleteFileTest(TestContext context)
	{
		Async runTestAsync = context.async();
		String usedTask ="FileRead";
		try
		{
			JsonObject TaskDef = new JsonObject(Files.readString(Paths.get(WebObjectCreator.class.getClassLoader().getResource("APITestData/TaskData.json").getPath()))).getJsonObject(usedTask);			

			System.out.println("--------------------  Running Tests for /task/{id}/{version}/filelist ----------------------");    
			createUserAndAuthedSession("TestUser", "testpw", Roles.Researcher)
			.onSuccess(authedSession -> {
				createUserAndAuthedSession("TestUser2", "testpw", Roles.Researcher)
				.onSuccess(wrongSession -> {	
					Async firstTask = context.async();				
					WebObjectCreator.createOrRetrieveTask(authedSession, "FileRead")
					.onSuccess(taskData -> {									
						Async correctSessionAsync  = context.async();
						POST(authedSession, "/task/" + taskData.getString("UUID") + "/" + taskData.getString("version") + "/resource/" + TaskDef.getJsonArray("resources").getString(0), new JsonObject().put("delete", true),null)
						.onSuccess(versionResponse -> {		
							// this is the version of the deleted file. 
							GET(authedSession, "/task/" + taskData.getString("UUID") + "/" + versionResponse.bodyAsJsonObject().getString("version") + "/filelist", null, null)
							.onSuccess(response -> {										
								JsonArray fileList = response.bodyAsJsonArray();
								context.assertEquals(4, fileList.size());	
								JsonArray resources = TaskDef.getJsonArray("resources").copy();
								resources.remove(0);
								checkAndClear(resources, fileList, "", context);
								// now all expected resources should be cleared
								context.assertEquals(0, resources.size());
								correctSessionAsync.complete();
							})
							.onFailure(err -> context.fail(err));
							// this is the version with the file
							Async originalAsync = context.async();
							GET(authedSession, "/task/" + taskData.getString("UUID") + "/" + taskData.getString("version") + "/filelist", null, null)
							.onSuccess(response -> {									
								JsonArray fileList = response.bodyAsJsonArray();								
								context.assertEquals(5, fileList.size());	
								JsonArray resources = TaskDef.getJsonArray("resources").copy();
								checkAndClear(resources, fileList, "", context);
								// now all expected resources should be cleared
								context.assertEquals(0, resources.size());
								originalAsync.complete();
							})
							.onFailure(err -> context.fail(err));
						})
						.onFailure(err -> context.fail(err));						
						firstTask.complete();
					})
					.onFailure(err -> context.fail(err));		
					runTestAsync.complete();
				})
				.onFailure(err -> context.fail(err));
			})
			.onFailure(err -> context.fail(err));
		}
		catch(Exception e)
		{
			context.fail(e);
		}
	}
	
	
	@Test
	public void testUploadMultipleFiles(TestContext context)
	{
		Async runTestAsync = context.async();
		String usedTask ="FileRead";
		try
		{
			JsonObject TaskDef = new JsonObject(Files.readString(Paths.get(WebObjectCreator.class.getClassLoader().getResource("APITestData/TaskData.json").getPath()))).getJsonObject(usedTask);			
			String ResourceFolder = Paths.get(WebObjectCreator.class.getClassLoader().getResource("FileTestData").getPath()).toString();

			System.out.println("--------------------  Running Tests for /task/{id}/{version}/resource ----------------------");    
			createUserAndAuthedSession("TestUser", "testpw", Roles.Researcher)
			.onSuccess(authedSession -> {											
				WebObjectCreator.createOrRetrieveTask(authedSession, "FileRead")
				.onSuccess(taskData -> {
					List<FileDescriptor> uploadedFiles = new LinkedList<>();
					uploadedFiles.add(new FileDescriptorImpl(ResourceFolder + File.separator + "images" + File.separator + "apple.jpg" , "File1.jpg"));
					uploadedFiles.add(new FileDescriptorImpl(ResourceFolder + File.separator + "images" + File.separator + "teddy.jpg" , "subFolder/File1.jpg"));
					Async multiUpload1Async = context.async();
					upload(authedSession, "/task/" + taskData.getString("UUID") + "/" + taskData.getString("version") + "/resource/", null, uploadedFiles)
					.onSuccess(versionResponse -> {
						
						GET(authedSession, "/task/" + taskData.getString("UUID") + "/" + versionResponse.getString("version") + "/filelist", null, null)
						.onSuccess(response -> {										
							JsonArray fileList = response.bodyAsJsonArray();
							context.assertEquals(7, fileList.size());	
							JsonArray resources = TaskDef.getJsonArray("resources").copy();
							resources.add("subFolder/File1.jpg");
							resources.add("File1.jpg");
							checkAndClear(resources, fileList, "", context);
							// now all expected resources should be cleared
							context.assertEquals(0, resources.size());
							multiUpload1Async.complete();
						})
						.onFailure(err -> context.fail(err));
					})
					.onFailure(err -> context.fail(err));
					// test upload into a subfolder
					Async multiUpload2Async = context.async();
					upload(authedSession, "/task/" + taskData.getString("UUID") + "/" + taskData.getString("version") + "/resource/subfolder1/", null, uploadedFiles)
					.onSuccess(versionResponse -> {
						
						GET(authedSession, "/task/" + taskData.getString("UUID") + "/" + versionResponse.getString("version") + "/filelist", null, null)
						.onSuccess(response -> {										
							JsonArray fileList = response.bodyAsJsonArray();
							context.assertEquals(6, fileList.size());	
							JsonArray resources = TaskDef.getJsonArray("resources").copy();
							resources.add("subfolder1/subFolder/File1.jpg");
							resources.add("subfolder1/File1.jpg");
							checkAndClear(resources, fileList, "", context);
							// now all expected resources should be cleared
							context.assertEquals(0, resources.size());
							multiUpload2Async.complete();
						})
						.onFailure(err -> context.fail(err));
					})
					.onFailure(err -> context.fail(err));				
					
					runTestAsync.complete();
				})
				.onFailure(err -> context.fail(err));
			})
			.onFailure(err -> context.fail(err));
		}
		catch(Exception e)
		{
			context.fail(e);
		}
	}
	
	void checkAndClear(JsonArray expected, JsonArray presentFiles, String baseFolder, TestContext context)
	{
		for(int i = 0; i < presentFiles.size(); i++)
		{
			JsonObject currentElement = presentFiles.getJsonObject(i);
			if(currentElement.containsKey("children"))
			{
				checkAndClear(expected, currentElement.getJsonArray("children"), baseFolder + currentElement.getString("label") + "/" , context);
			}
			else
			{
				if(!expected.remove(baseFolder + currentElement.getString("label")))
				{
					context.fail("Found value " + baseFolder + currentElement.getString("label") + " which was not expected");
				}
			}
		}
	}
	
	
	private void unzip(String zipFilePath, String destDir) throws Exception{
		File dir = new File(destDir);
		// create output directory if it doesn't exist
		if(!dir.exists()) dir.mkdirs();
		FileInputStream fis;
		//buffer for read and write data to file
		byte[] buffer = new byte[1024];

		fis = new FileInputStream(zipFilePath);
		ZipInputStream zis = new ZipInputStream(fis);
		ZipEntry ze = zis.getNextEntry();
		while(ze != null){
			String fileName = ze.getName();
			File newFile = new File(destDir + File.separator + fileName);
			//create directories for sub directories in zip
			new File(newFile.getParent()).mkdirs();
			FileOutputStream fos = new FileOutputStream(newFile);
			int len;
			while ((len = zis.read(buffer)) > 0) {
				fos.write(buffer, 0, len);
			}
			fos.close();
			//close this ZipEntry
			zis.closeEntry();
			ze = zis.getNextEntry();
		}
		//close last ZipEntry
		zis.closeEntry();
		zis.close();
		fis.close();

	}
	
	private Future<Void> downloadTaskAndWriteToFile(WebClientSession session, String uuid, String version, String targetFileName)
	{
		return GET(session, "/task/" + uuid + "/" + version + "/download", null,null)
				.compose(download -> {		
					// this is the version of the deleted file. 		
					return vertx.fileSystem().writeFile(targetFileName , download.bodyAsBuffer());		
				});
	}
	
}
