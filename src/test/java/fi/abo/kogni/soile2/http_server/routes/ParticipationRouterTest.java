package fi.abo.kogni.soile2.http_server.routes;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.Test;

import fi.abo.kogni.soile2.datamanagement.DataLakeManagerTest;
import fi.abo.kogni.soile2.http_server.SoileWebTest;
import fi.abo.kogni.soile2.http_server.auth.SoileAuthorization.Roles;
import fi.abo.kogni.soile2.http_server.verticles.DataBundleGeneratorVerticle.DownloadStatus;
import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.impl.MimeMapping;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientSession;
import io.vertx.ext.web.handler.HttpException;

public class ParticipationRouterTest extends SoileWebTest{

	private WebClient generatorSession;
	@Test
	public void runTaskTest(TestContext context)
	{
		System.out.println("--------------------  Running Task Tests  ----------------------");    
		Async creationAsync = context.async();
		createAndStartProject(true)
		.onSuccess(instanceID -> {
			createTokenAndSignupUser(generatorSession, instanceID)
			.onSuccess(authToken -> {
				Async codeTypeAsync = context.async();
				WebClientSession tempSession = createSession();
				tempSession.addHeader("Authorization", authToken);
				GET(tempSession, "/projectexec/" + instanceID + "/getcurrenttaskinfo", null, null)
				.onSuccess(response -> {
					JsonObject codeTypeInfo = response.bodyAsJsonObject();
					context.assertEquals("qmarkup", codeTypeInfo.getJsonObject("codeType", new JsonObject()).getString("language"));
					
					context.assertEquals(false, codeTypeInfo.getBoolean("finished") == null ? false : codeTypeInfo.getBoolean("finished"));
					codeTypeAsync.complete();
				})
				.onFailure(err -> context.fail(err));
				
				Async codeAsync = context.async();
				GET(tempSession, "/run/" + instanceID , null, null)
				.onSuccess(response -> {
					context.assertEquals("application/json", response.headers().get("content-type"));					
					JsonObject compiledCode = response.bodyAsJsonObject();
					context.assertTrue(compiledCode.containsKey("elements"));
					context.assertEquals("html", compiledCode.getJsonArray("elements").getJsonArray(0).getJsonObject(0).getString("type"));
					context.assertEquals("",compiledCode.getString("title"));
					codeAsync.complete();
				})
				.onFailure(err -> context.fail(err));
				creationAsync.complete();
			})
			.onFailure(err -> context.fail(err));
		})
		.onFailure(err -> context.fail(err));
	}
	
	
	
	@Test
	public void submitTest(TestContext context)
	{
		System.out.println("--------------------  Running Data Submission Tests  ----------------------");    

		Async creationAsync = context.async();
		JsonArray OutputData = new JsonArray().add(new JsonObject().put("name", "smoker")
																   .put("value", 0)
																   .put("timestamp", System.currentTimeMillis()));
		JsonObject resultData = new JsonObject().put("jsonData",new JsonArray().add(new JsonObject().put("name", "smoker")
																								    .put("value", 0)
																								    .put("timestamp", System.currentTimeMillis())
																					)
																				.add(new JsonObject().put("name", "smoker2")
																					    .put("value", "something")
																					    .put("timestamp", System.currentTimeMillis())
																		)
													)
												.put("fileData", new JsonArray());
		JsonObject result = new JsonObject().put("outputData", OutputData).put("resultData", resultData);
		
		createAndStartProject(true)
		.onSuccess(instanceID -> {
			createTokenAndSignupUser(generatorSession, instanceID)			
			.onSuccess(authToken -> {
				Async submitAsync = context.async();
				WebClientSession tempSession = createSession();										
				tempSession.addHeader("Authorization", authToken);				
				submitResult(tempSession, result, instanceID)
				.onSuccess(done -> {
					getParticipantInfoForToken(authToken)
					.onSuccess(partInfo -> {
						context.assertNotNull(partInfo);
						// there is only one output
						context.assertEquals(OutputData,partInfo.getJsonArray("outputData").getJsonObject(0).getJsonArray("outputs"));
						submitAsync.complete();
					});
				})
				.onFailure(err -> context.fail(err));
				creationAsync.complete();
			})
			.onFailure(err -> context.fail(err));
		})
		.onFailure(err -> context.fail(err));
	}
	
	@Test
	public void dataUploadAndRetrievalTest(TestContext context)
	{
		System.out.println("--------------------  Running Data Upload and retrieval tests  ----------------------");    

		Async creationAsync = context.async();
		JsonArray OutputData = new JsonArray().add(new JsonObject().put("name", "smoker")
																   .put("value", 0)
																   .put("timestamp", System.currentTimeMillis()));
		JsonArray fileData = new JsonArray();
		JsonObject resultData = new JsonObject().put("jsonData",new JsonArray().add(new JsonObject().put("name", "smoker")
																								    .put("value", 0)
																								    .put("timestamp", System.currentTimeMillis())
																					)
																				.add(new JsonObject().put("name", "smoker2")
																					    .put("value", "something")
																					    .put("timestamp", System.currentTimeMillis())
																		)
													)
												.put("fileData", fileData);
		
		JsonObject result = new JsonObject().put("outputData", OutputData).put("resultData", resultData);
		String TestDataFolder = WebObjectCreator.class.getClassLoader().getResource("FileTestData").getPath();
		String filename = "Image.jpg";
		File upload = new File(Path.of(TestDataFolder, "ImageData.jpg").toString());
		createAndStartProject(true)
		.onSuccess(instanceID -> {
			createTokenAndSignupUser(generatorSession, instanceID)			
			.onSuccess(authToken -> {
				Async submitAsync = context.async();
				WebClientSession tempSession = createSession();										
				tempSession.addHeader("Authorization", authToken);
				uploadResult(tempSession, instanceID, upload, filename, "image/jpeg")
				.onSuccess( uploadID -> {
					fileData.add(new JsonObject().put("fileformat", "image/jpeg")
												 .put("filename", filename)
												 .put("targetid", uploadID));
					submitResult(tempSession, result, instanceID)
					.onSuccess(done -> {
						Async dlNotReadyAsync = context.async();
						POST(generatorSession,"/projectexec/" + instanceID + "/data", null, "" )
						.onSuccess(response -> {
							String dlID = response.bodyAsJsonObject().getString("downloadID");
							GET(generatorSession,"/projectexec/" + instanceID + "/download/" + dlID, null, null)
							.onSuccess(res -> {
								// This is ok, the server was just really fast.
								dlNotReadyAsync.complete();
								})
							.onFailure(rejected -> {
								context.assertEquals(503, ((HttpException)rejected).getStatusCode());
								dlNotReadyAsync.complete();
							});					 
							awaitDownloadReady(generatorSession,instanceID,dlID, Promise.promise())
							.onSuccess(dlReady -> {
								GET(generatorSession,"/projectexec/" + instanceID + "/download/" + dlID, null, null)
								.onSuccess(download -> {
									// This is ok, the server was just really fast.
									String targetFileName = tmpDir + File.separator + "WebTestdownload.tar.gz";
									vertx.fileSystem().writeFile(targetFileName , download.bodyAsBuffer())
									.onSuccess( dlSaved -> 
									{
										String targetDir = "/tmp" + File.separator + "WebTestdownload" + File.separator + "out";
										JsonObject DataJson = null; 
										try {
											unzip(targetFileName, targetDir);
											DataJson = new JsonObject(Files.readString(Path.of(targetDir,"data.json")));
											JsonArray taskResults = DataJson.getJsonArray("taskResults");
											JsonObject task1Res = null;
											for(int i = 0; i < taskResults.size(); ++i)
											{
												if(taskResults.getJsonObject(i).getString("taskID").equals("tabcdefg0"))
												{
													task1Res =  taskResults.getJsonObject(i).getJsonArray("resultData").getJsonObject(0);
													break;
												}
												
											}
											context.assertEquals(1, task1Res.getInteger("step"));
											context.assertEquals(resultData.getValue("jsonData"), task1Res.getValue("dbData"));
											context.assertTrue(areFilesEqual(upload, new File(Path.of(targetDir, task1Res.getJsonArray("files").getJsonObject(0).getString("path")).toString())));
											submitAsync.complete();
										}
										catch(Exception e)
										{
											context.fail(e);
										}										
										
										//submitAsync.complete();
									})
									.onFailure(err -> context.fail(err));	
										
									})
								.onFailure(err -> context.fail(err));
																	
							})
							.onFailure(err -> context.fail(err));	
														
						})
						.onFailure(err -> context.fail(err));
						Async invalidRequestAsync = context.async();
						POST(tempSession,"/projectexec/" + instanceID + "/data", null, "" )
						.onSuccess(fail -> {
							context.fail("Invalid access should not be possible");
						})
						.onFailure(err -> invalidRequestAsync.complete());
						
					})
					.onFailure(err -> context.fail(err));
				})
				.onFailure(err -> context.fail(err));
				creationAsync.complete();
			})
			.onFailure(err -> context.fail(err));
		})
		.onFailure(err -> context.fail(err));
	}
	
	
	@Test
	public void projectTest(TestContext context)
	{
		System.out.println("--------------------  Running Web Project Prohgression Test  ----------------------");    

		Async creationAsync = context.async();
		JsonArray OutputData = new JsonArray().add(new JsonObject().put("name", "smoker")
																   .put("value", 1)
																   .put("timestamp", System.currentTimeMillis()));
		JsonArray fileData = new JsonArray();
		JsonObject resultData = new JsonObject().put("jsonData",new JsonArray().add(new JsonObject().put("name", "smoker")
																								    .put("value", 1)
																								    .put("timestamp", System.currentTimeMillis())
																					)
																				.add(new JsonObject().put("name", "smoker2")
																					    .put("value", "something")
																					    .put("timestamp", System.currentTimeMillis())
																		)
													)
												.put("fileData", fileData);
		
		JsonObject result = new JsonObject().put("outputData", OutputData).put("resultData", resultData);
		String TestDataFolder = WebObjectCreator.class.getClassLoader().getResource("FileTestData").getPath();
		File upload = new File(Path.of(TestDataFolder, "textData.txt").toString());
		List<File> fileUploads = new LinkedList<>();
		fileUploads.add(upload);
		createAndStartProject(true)
		.onSuccess(instanceID -> {
			createTokenAndSignupUser(generatorSession, instanceID)			
			.onSuccess(authToken -> {
				Async submitAsync = context.async();
				WebClientSession tempSession = createSession();				
				tempSession.addHeader("Authorization", authToken);				
				submitFilesAndResults(tempSession, fileUploads, result.copy(), instanceID)
				.onSuccess(submitted -> {
					submitFilesAndResults(tempSession, fileUploads, result.copy(), instanceID)
					.onSuccess(submitted2 -> {
						GET(tempSession, "/run/" + instanceID , null, null)
						.onSuccess(code -> {
							context.assertEquals("application/javascript", code.headers().get("content-type"));					
							submitFilesAndResults(tempSession, fileUploads, result.copy(), instanceID)
							.onSuccess(submitted3 -> {
								submitFilesAndResults(tempSession, fileUploads, result.copy(), instanceID)
								.onSuccess(submitted4 -> {
									submitFilesAndResults(tempSession, fileUploads, result.copy(), instanceID)
									.onSuccess(submitted5 -> {
										GET(tempSession, "/projectexec/" + instanceID + "/getcurrenttaskinfo", null, null)
										.onSuccess(response -> {									
											JsonObject finalresult = response.bodyAsJsonObject();
											// now the project is done, we have passed filters and everything. 
											context.assertTrue(finalresult.getBoolean("finished"));
											submitAsync.complete();
										})
										.onFailure(err -> context.fail(err));
									})
									.onFailure(err -> context.fail(err));
								})
								.onFailure(err -> context.fail(err));
							})
							.onFailure(err -> context.fail(err));
						})
						.onFailure(err -> context.fail(err));
					})
					.onFailure(err -> context.fail(err));
				})
				.onFailure(err -> context.fail(err));
				creationAsync.complete();
			})
			.onFailure(err -> context.fail(err));
		})
		.onFailure(err -> context.fail(err));
	}
	
	@Test
	public void testRunResources(TestContext context)
	{
		System.out.println("--------------------  Running Test for /run/{id}/*  ----------------------");    

		JsonArray OutputData = new JsonArray().add(new JsonObject().put("name", "smoker")
																   .put("value", 1)
																   .put("timestamp", System.currentTimeMillis()));
		JsonArray fileData = new JsonArray();
		JsonObject resultData = new JsonObject().put("jsonData",new JsonArray().add(new JsonObject().put("name", "smoker")
																								    .put("value", 1)
																								    .put("timestamp", System.currentTimeMillis())
																					)
																				.add(new JsonObject().put("name", "smoker2")
																					    .put("value", "something")
																					    .put("timestamp", System.currentTimeMillis())
																		)
													)
												.put("fileData", fileData);
		
		JsonObject result = new JsonObject().put("outputData", OutputData).put("resultData", resultData);
		String TestDataFolder = WebObjectCreator.class.getClassLoader().getResource("FileTestData").getPath();
		File upload = new File(Path.of(TestDataFolder, "textData.txt").toString());
		List<File> fileUploads = new LinkedList<>();
		fileUploads.add(upload);
		
		Async testRunResource = context.async();
		createAndStartProject(true, "testProject")
		.onSuccess(instanceID -> {
			createTokenAndSignupUser(generatorSession, instanceID)
			.onSuccess(authToken -> {
				WebClientSession tempSession = createSession();				
				tempSession.addHeader("Authorization", authToken);
				GET(tempSession, "/run/" + instanceID + "/ImageData.jpg", null, null)
				.onSuccess(failed -> {
					context.fail("The first Task does NOT have any resources!");
				})
				.onFailure(response -> {
					context.assertEquals(404, ((HttpException)response).getStatusCode());
					submitFilesAndResults(tempSession, fileUploads, result.copy(), instanceID)
					.onSuccess(submitted -> {
						GET(tempSession, "/run/testProject/ImageData.jpg", null, null)
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
									testRunResource.complete();

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
	}
	
	@Test
	public void testRunLibs(TestContext context)
	{
		System.out.println("--------------------  Running Tests for /run/{id}/lib/*  ----------------------");    
			
		Async testRunResource = context.async();
		createAndStartProject(true)
		.onSuccess(instanceID -> {
			createTokenAndSignupUser(generatorSession, instanceID)
			.onSuccess(authToken -> {
				WebClientSession tempSession = createSession();				
				tempSession.addHeader("Authorization", authToken);
				GET(tempSession, "/run/" + instanceID + "/lib/testlib.js", null, null)
				.onSuccess(response -> {
					context.assertTrue(response.bodyAsString().contains("console.log"));
					testRunResource.complete();
				})
				.onFailure(err -> context.fail(err));
				Async failedLibAsync = context.async();
				GET(tempSession, "/run/" + instanceID + "/lib/testlib2.js", null, null)
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
	}
	
	protected Future<String> signUpToProjectWithToken(WebClient client,String Token, String projectID)
	{
		Promise<String> tokenPromise = Promise.promise();
		POST(client,"/projectexec/" + projectID + "/signup", new JsonObject().put("token", Token), null)
		.onSuccess(response -> {
			tokenPromise.complete(response.bodyAsJsonObject().getString("token"));
		})
		.onFailure(err -> tokenPromise.fail(err));
		return tokenPromise.future();
	}
	
	protected Future<Void> signUpToProject(WebClient client, String projectID)
	{
		Promise<Void> tokenPromise = Promise.promise();
		POST(client,"/projectexec/" + projectID + "/signup", null, null)
		.onSuccess(response -> {
			tokenPromise.complete();
		})
		.onFailure(err -> tokenPromise.fail(err));
		return tokenPromise.future();
	}
	
	protected Future<JsonArray> createTokens(WebClient client, String projectID, int count, boolean unique)
	{
		Promise<JsonArray> resultPromise = Promise.promise();
		POST(client,"/projectexec/" + projectID + "/createtokens", new JsonObject().put("unique", unique).put("count", count), null )
		.onSuccess(response -> {
			if(unique)
			{
				resultPromise.complete(new JsonArray().add(response.bodyAsString()));
			}
			else
			{
				resultPromise.complete(response.bodyAsJsonArray());
			}
		})
		.onFailure(err -> resultPromise.fail(err));
		
		return resultPromise.future();
	}
	
	protected Future<Void> submitResult(WebClient client, JsonObject resultData, String instanceID)
	{
		Promise<Void> submittedPromise = Promise.promise();
		GET(client, "/projectexec/" + instanceID + "/getcurrenttaskinfo", null, null)
		.onSuccess(response -> {
			String taskInstanceID = response.bodyAsJsonObject().getString("id");			
			resultData.put("taskID",taskInstanceID);
			POST(client, "/projectexec/" + instanceID + "/submit", null, resultData)
			.onSuccess(submitted -> {
				submittedPromise.complete();								
			})
			.onFailure(err -> submittedPromise.fail(err));
		})
		.onFailure(err -> submittedPromise.fail(err));
		
		return submittedPromise.future();
	}
	
	
	/**
	 * Submit files and results with the given webclient(session). 
	 * @param client
	 * @param uploads
	 * @param resultData
	 * @param instanceID
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	protected Future<Void> submitFilesAndResults(WebClient client, List<File> uploads, JsonObject resultData, String instanceID)
	{
		Promise<Void> submittedPromise = Promise.promise();
		List<Future> submissionFutures = new LinkedList<Future>();
		ConcurrentLinkedDeque<JsonObject> uploadObjects = new ConcurrentLinkedDeque<>();
		for(File f : uploads)
		{		
			Promise done = Promise.promise();
			submissionFutures.add(done.future());
			uploadResult(client, instanceID, f, f.getName(), MimeMapping.getMimeTypeForFilename(f.getName()))
			.onSuccess(id -> {
				uploadObjects.add(new JsonObject().put("fileformat", MimeMapping.getMimeTypeForFilename(f.getName()))
						.put("filename", f.getName())
						.put("targetid", id));				
				done.complete();

			})
			.onFailure(err -> done.fail(err));
		}
		CompositeFuture.all(submissionFutures).onSuccess(allSubmitted -> {
			GET(client, "/projectexec/" + instanceID + "/getcurrenttaskinfo", null, null)
			.onSuccess(response -> {
				JsonArray fileResults = resultData.getJsonObject("resultData").getJsonArray("fileData");
				for(JsonObject o : uploadObjects)
				{
					fileResults.add(o);
				}
				
				String taskInstanceID = response.bodyAsJsonObject().getString("id");				
				resultData.put("taskID",taskInstanceID);
				POST(client, "/projectexec/" + instanceID + "/submit", null, resultData)
				.onSuccess(submitted -> {
					submittedPromise.complete();								
				})
				.onFailure(err -> submittedPromise.fail(err));
			})
			.onFailure(err -> submittedPromise.fail(err));
		})
		.onFailure(err -> submittedPromise.fail(err));

		return submittedPromise.future();
	}
	
	protected Future<String> createMasterToken(WebClient client, String projectID)
	{
		return createTokens(client,projectID,0,true).map(output -> {return output.getString(0);});
	}
	
	protected Future<String> createTokenAndSignupUser(WebClient authedSession, String projectID)
	{
		return createTokens(authedSession, projectID,1,false)
		.compose(tokenArray -> {
			String token = tokenArray.getString(0);
			return signUpToProjectWithToken(createSession(), token, projectID);
		});		
	}
	
	protected Future<String> createAndStartProject(boolean priv)
	{
		return createAndStartProject(priv, "newShortCut");
	}
	
	protected Future<String> createAndStartProject(boolean priv, String shortcut)
	{
		JsonObject projectExec = new JsonObject().put("private", priv).put("name", "New Project").put("shortcut",shortcut); 

		Promise<String> projectInstancePromise = Promise.promise();
		createUserAndAuthedSession("Researcher", "test", Roles.Researcher)
		.onSuccess(authedSession -> {
			generatorSession = authedSession;
			WebObjectCreator.createProject(authedSession, "Testproject")
			.onSuccess(projectData -> {				
				String projectID = projectData.getString("UUID");
				String projectVersion = projectData.getString("version");
				POST(authedSession, "/project/" + projectID + "/" + projectVersion + "/start", null,projectExec )
				.onSuccess(response -> {
					projectInstancePromise.complete(response.bodyAsJsonObject().getString("projectID"));
				})
				.onFailure(err -> projectInstancePromise.fail(err));

			})
			.onFailure(err -> projectInstancePromise.fail(err));
		})
		.onFailure(err -> projectInstancePromise.fail(err));
		return projectInstancePromise.future();
	}
	
	protected Future<JsonObject> getParticipantInfoForToken(String token)
	{			
		return mongo_client.findOne(SoileConfigLoader.getCollectionName("participantCollection"), new JsonObject().put("token", token), null);		
	}
	
	protected Future<Void> checkTaskIsCorrect(WebClient client, String instanceID, String taskID)
	{
		Promise<Void> correctTask = Promise.promise();
		GET(client, "/projectexec/" + instanceID + "/getcurrenttaskinfo", null, null)
		.onSuccess(nexttaskID -> {
			if(nexttaskID.bodyAsJsonObject().getString("id").equals(taskID))
			{
				correctTask.complete();				
			}
			else
			{
				correctTask.fail("Got " + nexttaskID.bodyAsString() + " expected " + taskID);
			}
		})
		.onFailure(err -> correctTask.fail(err));

		
		return correctTask.future();
	}
	
	protected Future<JsonObject> getParticipantInfoForUsername(String username, String projectID)
	{
		Promise<JsonObject> participantPromise = Promise.promise();
		mongo_client.findOne(SoileConfigLoader.getCollectionName("userCollection"),
				new JsonObject().put(SoileConfigLoader.getMongoAuthNOptions().getUsernameField(), username), 
				new JsonObject().put(SoileConfigLoader.getUserdbField("participantField"),1 ))
		.onSuccess(participantJson -> {
			JsonArray participantInfo = participantJson.getJsonArray(SoileConfigLoader.getUserdbField("participantField"), new JsonArray());
			String participantID = null;
			for(int i = 0; i < participantInfo.size(); i++)
			{
				// TODO: Use an aggregation to retrieve this (that's probably faster).
				if(participantInfo.getJsonObject(i).getString("uuid").equals(projectID))
				{
					participantID = participantInfo.getJsonObject(i).getString("participantID");
					break;
				}

			}
			if(participantID != null)
			{
			mongo_client.findOne(SoileConfigLoader.getCollectionName("participantCollection"), new JsonObject().put("_id", participantID), null)
			.onSuccess(result ->
			{
					participantPromise.complete(result);
			})
			.onFailure(err -> participantPromise.fail(err));
			}
			else
			{
				participantPromise.fail("No participant for user in project");
			}
					
		})
		.onFailure(err -> participantPromise.fail(err));

		return participantPromise.future();		
	}
	
	private Future<Void> awaitDownloadReady(WebClient client, String projectID, String dlID, Promise<Void> readyPromise)
	{		
		GET(client, "/projectexec/" + projectID + "/download/" + dlID + "/check", null, null).onSuccess(response -> 
		{
			JsonObject status = response.bodyAsJsonObject();
			if(status.getString("status").equals(DownloadStatus.downloadReady.toString()))
			{
				readyPromise.complete();
				return;
			}
			else
			{
				if(status.getString("status").equals(DownloadStatus.failed.toString()))
				{
					readyPromise.fail(new Exception("Download Failed"));
				}
				else
				{
					try {
						// we need to wait a tiny bit... 
						TimeUnit.MILLISECONDS.sleep(50);
						awaitDownloadReady(client,projectID, dlID, readyPromise);
					}
					catch(InterruptedException e)
					{
						readyPromise.fail(e);
					}
				}
			}											
		});
		return readyPromise.future();
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
	
}
