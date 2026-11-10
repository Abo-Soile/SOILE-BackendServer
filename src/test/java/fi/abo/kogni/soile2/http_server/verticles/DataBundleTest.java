package fi.abo.kogni.soile2.http_server.verticles;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import fi.abo.kogni.soile2.ElementTester;
import fi.abo.kogni.soile2.datamanagement.datalake.DataLakeFile;
import fi.abo.kogni.soile2.datamanagement.datalake.ParticipantDataLakeManager;
import fi.abo.kogni.soile2.http_server.verticles.DataBundleGeneratorVerticle.DownloadStatus;
import fi.abo.kogni.soile2.projecthandling.exceptions.ObjectDoesNotExist;
import fi.abo.kogni.soile2.projecthandling.participant.Participant;
import fi.abo.kogni.soile2.projecthandling.participant.ParticipantHandler;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.Study;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.impl.StudyHandler;
import fi.abo.kogni.soile2.utils.DataProvider;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.web.FileUpload;

public class DataBundleTest extends ElementTester {

	StudyHandler projHandler;
	ParticipantHandler partHandler;
	DataBundleGeneratorVerticle dbg;
	private static final Logger LOGGER = LogManager.getLogger(DataBundleTest.class);
	
	@Override
	public void runBeforeTests(TestContext context)
	{
		super.runBeforeTests(context);	
		projHandler = new StudyHandler(mongo_client, vertx);
		partHandler = new ParticipantHandler(mongo_client, projHandler, vertx);
		dbg = new DataBundleGeneratorVerticle(mongo_client, projHandler, partHandler);
		vertx.deployVerticle(dbg);
	}
	private class BundleData {
		Study study;
		Participant part1;
		Participant part2;
		String position1;
		String position2;
		
		public BundleData(Study study, Participant part1, Participant part2,String position1,String position2)
		{
			this.study = study;
			this.part1 = part1;
			this.part2 = part2;
			this.position1 = position1;
			this.position2= position2;
				
		}				
	}
	private Future<BundleData> setupBundle(TestContext context, String dataDir)
	{
		Promise<BundleData> dataPromise = Promise.promise();
		ParticipantDataLakeManager dlm = new ParticipantDataLakeManager(resultDataLakeDir, vertx);
		createProject(context).onSuccess(project ->
		{
			projHandler.createStudy(getCreationJson(project, "TestProject", ""))
			.onSuccess(projInstance -> {
				projInstance.activate()
				.onSuccess(active -> {
					partHandler.create(projInstance)				
					.onSuccess(participant -> {
						projInstance.startStudy(participant)
						.onSuccess(position1 -> { 
							partHandler.create(projInstance)
							.onSuccess(participant2 -> {
								projInstance.startStudy(participant2)
								.onSuccess(position2 -> {
									participant.getCurrentStep().onSuccess(p1step -> {
										participant2.getCurrentStep().onSuccess(p2step -> {

											try {
												String tempDataDir = DataProvider.createTempDataDirectory(dataDir);
												// Add some Files for the participants
												FileUpload tempUpload = DataProvider.getFileUploadForTarget(Path.of(tempDataDir,"ImageData.jpg").toString(), "TestImage.jpg", "image/jpg");
												dlm.storeParticipantData(participant.getID(), p1step, position1, tempUpload)
												.onSuccess(fileID1 -> {
													FileUpload tempUpload2 = DataProvider.getFileUploadForTarget(Path.of(tempDataDir,"textData.txt").toString(), "TestData.txt", "text/plain");
													dlm.storeParticipantData(participant2.getID(), p2step, position2, tempUpload2).onSuccess(fileID2 -> {
														JsonObject results1 = new JsonObject().put("taskID",position1)
																.put("outputData", new JsonArray())
																.put("resultData", new JsonObject().put("fileData", new JsonArray().add(new JsonObject().put("targetid", fileID1)
																		.put("fileformat", "image/jpg")
																		.put("filename", "TestImage.jpg")))
																		.put("jsonData", new JsonArray().add(new JsonObject().put("name", "something")
																				.put("value", "more"))));
														JsonObject results2 = new JsonObject().put("taskID",position2)
																.put("outputData", new JsonArray())
																.put("resultData", new JsonObject().put("fileData",  new JsonArray().add(new JsonObject().put("targetid", fileID2)
																		.put("fileformat", "text/plain")
																		.put("filename", "TestData.txt")))
																		.put("jsonData", new JsonArray().add(new JsonObject().put("name", "something")
																				.put("value", "else"))));

														participant.addResult(position1, projInstance.getResultDataFromTaskData(results1))
														.onSuccess( v ->
														{
															participant2.addResult(position2, projInstance.getResultDataFromTaskData(results2))
															.onSuccess(v2 -> {
																dataPromise.complete(new BundleData(
																		projInstance,
																		participant,
																		participant2,
																		position1,
																		position2));
															})

															.onFailure(err -> context.fail(err));
														})
														.onFailure(err -> context.fail(err));
													})
													.onFailure(err -> context.fail(err));
												})
												.onFailure(err -> context.fail(err));

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
															
		
		return dataPromise.future();
	}
	
	@Test
	public void testCleanup(TestContext context)
	{
		System.out.println("--------------------------- Testing Data Bundling ----------------------------");
		// this test is somewhat overloaded. However this allows testing multiple downloads simultaneously.
		String dataDir = DataBundleTest.class.getClassLoader().getResource("FileTestData").getPath();
		Async testComplete = context.async();
		setupBundle(context, dataDir)
		.onSuccess(data ->
		{			
			Async participantBundleAsync = context.async();
			eb.request("fi.abo.soile.DLCreate", new JsonObject().put("requestType", "participants")
					.put("participants",new JsonArray().add(data.part1.getID()).add(data.part2.getID()))
					.put("projectID", data.study.getID()))
			.onSuccess(reply -> {
				String dlID = (String)reply.body();
				awaitReady(dbg, dlID, Promise.<Void>promise())
				.onSuccess(dlReady -> {					
					participantBundleAsync.complete();
					Async statusAsync = context.async();
					eb.request("fi.abo.soile.DLStatus", new JsonObject().put("downloadID", dlID))																			
					.onSuccess( response -> {
						JsonObject status = (JsonObject) response.body();
						context.assertEquals(status.getString("status"), DownloadStatus.downloadReady.toString());
						statusAsync.complete();
					})
					.onFailure(err -> context.fail(err));
					Async fileListAsync = context.async();
					eb.request("fi.abo.soile.DLFiles", new JsonObject().put("downloadID", dlID))
					.onSuccess( response -> {	
						fileListAsync.complete();
						JsonArray files = ((JsonObject) response.body()).getJsonArray("files"); 
						Path dataFileLocation = null;
						boolean  fileChecked = false; 
						for(int i = 0; i < files.size(); ++i)
						{
							if(files.getJsonObject(i).getString("originalFileName").equals("data.json"))
							{											
								dataFileLocation = Path.of(new DataLakeFile(files.getJsonObject(i)).getAbsolutePath());									
								context.assertTrue(Files.exists(dataFileLocation));
								fileChecked = true;
							}
						}
						context.assertTrue(fileChecked);
						final Path actualLocation = dataFileLocation;
						context.assertTrue(Files.exists(actualLocation));
						eb.request("fi.abo.soile.DLDelete", new JsonObject().put("downloadID", dlID))
						.onSuccess(deleted -> {
							JsonObject result = (JsonObject) deleted.body();
							context.assertFalse(Files.exists(actualLocation));
							context.assertEquals(result.getString("status"), "success");
							Async noLongerThere = context.async(); 
							eb.request("fi.abo.soile.DLStatus", new JsonObject().put("downloadID", dlID))																			
							.onSuccess( failed_info-> {
								JsonObject status = (JsonObject) failed_info.body();
								context.assertEquals(status.getString("status"), DownloadStatus.notExistent.toString());
								noLongerThere.complete();
							});
							// Test that the json file was also deleted.
							testComplete.complete();							
						});
					})
					.onFailure(err -> context.fail(err));
				})
				.onFailure(err -> context.fail(err));
			})
			.onFailure(err -> context.fail(err));
		})
		.onFailure(err -> context.fail(err));
				
	}
	
	@Test
	public void testAutoCleanup(TestContext context)
	{
		System.out.println("--------------------------- Testing Data Bundling ----------------------------");
		// this test is somewhat overloaded. However this allows testing multiple downloads simultaneously.
		String dataDir = DataBundleTest.class.getClassLoader().getResource("FileTestData").getPath();
		DataBundleGeneratorVerticle dbg3 = new DataBundleGeneratorVerticle(mongo_client, projHandler, partHandler, 1000);
		Future<String> deploymentID = vertx.deployVerticle(dbg3);

		Async testComplete = context.async();
		setupBundle(context, dataDir)
		.onSuccess(data ->
		{						
			eb.request("fi.abo.soile.DLCreate", new JsonObject().put("requestType", "participants")
					.put("participants",new JsonArray().add(data.part1.getID()).add(data.part2.getID()))
					.put("projectID", data.study.getID()))
			.onSuccess(reply -> {
				String dlID = (String)reply.body();
				awaitReady(dbg, dlID, Promise.<Void>promise())
				.onSuccess(dlReady -> {
					LOGGER.info("Download Ready");
					// Lets wait for 5 seconds
					awaitStatus(dbg, dlID, Promise.promise() , DownloadStatus.notExistent,5000)
					.onComplete(finished -> {
						LOGGER.info("Status awaited");
						if(finished.failed())
						{
							context.fail(finished.cause());
							deploymentID.onSuccess(id -> {
								vertx.undeploy(id);
							});
						}
						else
						{
							testComplete.complete();
							deploymentID.onSuccess(id -> {
								vertx.undeploy(id);
							});
						}
						LOGGER.info("Test Done");
					});															
				})
				.onFailure(err -> context.fail(err));	
				
			})
			.onFailure(err -> context.fail(err));
		})
		.onFailure(err -> context.fail(err));
				
	}
	@Test
	public void testDataBundling(TestContext context)
	{
		System.out.println("--------------------------- Testing Data Bundling ----------------------------");
		// this test is somewhat overloaded. However this allows testing multiple downloads simultaneously.
		String dataDir = DataBundleTest.class.getClassLoader().getResource("FileTestData").getPath();
		Async testComplete = context.async();
		setupBundle(context, dataDir)
				.onSuccess(data ->
		{
			Study projInstance = data.study;
			Participant participant = data.part1;
			Participant participant2 = data.part2;
			String position1 = data.position1; 

			// so, now we have two participants each with Result Data.
			Async participantBundleAsync = context.async();
			eb.request("fi.abo.soile.DLCreate", new JsonObject().put("requestType", "participants")
					.put("participants",new JsonArray().add(participant.getID()).add(participant2.getID()))
					.put("projectID", projInstance.getID()))
			.onSuccess(reply -> {
				String dlID = (String)reply.body();
				awaitReady(dbg, dlID, Promise.<Void>promise())
				.onSuccess(dlReady -> {																	
					Async statusAsync = context.async();
					eb.request("fi.abo.soile.DLStatus", new JsonObject().put("downloadID", dlID))																			
					.onSuccess( response -> {
						JsonObject status = (JsonObject) response.body();
						context.assertEquals(status.getString("status"), DownloadStatus.downloadReady.toString());
						statusAsync.complete();
					})
					.onFailure(err -> context.fail(err));
					Async fileListAsync = context.async();
					eb.request("fi.abo.soile.DLFiles", new JsonObject().put("downloadID", dlID))
					.onSuccess( response -> {
						JsonArray files = ((JsonObject) response.body()).getJsonArray("files"); 
						context.assertEquals(3, files.size());
						int found = 0;
						for(int i = 0; i < files.size(); ++i)
						{
							if(files.getJsonObject(i).getString("originalFileName").contains("TestData.txt"))
							{
								checkFileSame(Path.of(dataDir, "textData.txt").toString(), new DataLakeFile(files.getJsonObject(i)).getAbsolutePath(), context);
								found++;
							}
							if(files.getJsonObject(i).getString("originalFileName").contains("TestImage.jpg"))
							{
								checkFileSame(Path.of(dataDir, "ImageData.jpg").toString(), new DataLakeFile(files.getJsonObject(i)).getAbsolutePath(), context);
								found++;
							}
							if(files.getJsonObject(i).getString("originalFileName").equals("data.json"))
							{			
								found++;
								try {																					
									JsonObject dataJson = new JsonObject(Files.readString(Path.of(new DataLakeFile(files.getJsonObject(i)).getAbsolutePath())));

									context.assertTrue(dataJson.containsKey("participantResults"));
									context.assertEquals(2,dataJson.getJsonArray("participantResults").size());
									JsonObject part1Results = dataJson.getJsonArray("participantResults").getJsonObject(0);
									JsonObject part2Results = dataJson.getJsonArray("participantResults").getJsonObject(1);
									if(dataJson.getJsonArray("participantResults").getJsonObject(0).getString("participantID").equals(participant2.getID()))
									{
										JsonObject tmp = part1Results;
										part1Results = part2Results;
										part2Results = tmp;
									}
									context.assertEquals(participant.getID(), part1Results.getString("participantID"));
									context.assertEquals(1, part1Results.getJsonArray("resultData").size());
									context.assertEquals(1, part1Results.getJsonArray("resultData").getJsonObject(0).getJsonArray("dbData").size());
									context.assertEquals("more", part1Results.getJsonArray("resultData").getJsonObject(0).getJsonArray("dbData").getJsonObject(0).getString("value"));
									context.assertEquals(1, part1Results.getJsonArray("resultData").getJsonObject(0).getJsonArray("files").size());
									context.assertEquals("image/jpg",part1Results.getJsonArray("resultData").getJsonObject(0).getJsonArray("files").getJsonObject(0).getString("mimeType"));
									context.assertTrue(part1Results.getJsonArray("resultData").getJsonObject(0).getJsonArray("files").getJsonObject(0).getString("originalFileName").contains("TestImage.jpg"));
									context.assertEquals(participant2.getID(), part2Results.getString("participantID"));
									context.assertEquals(1, part2Results.getJsonArray("resultData").size());
									context.assertEquals(1, part2Results.getJsonArray("resultData").getJsonObject(0).getJsonArray("dbData").size());
									context.assertEquals("else", part2Results.getJsonArray("resultData").getJsonObject(0).getJsonArray("dbData").getJsonObject(0).getString("value"));
									context.assertEquals("text/plain",part2Results.getJsonArray("resultData").getJsonObject(0).getJsonArray("files").getJsonObject(0).getString("mimeType"));
									context.assertTrue(part2Results.getJsonArray("resultData").getJsonObject(0).getJsonArray("files").getJsonObject(0).getString("originalFileName").contains("TestData.txt"));
								}
								catch(IOException e)
								{
									context.fail(e);
								}
							}
						}
						context.assertEquals(3, found);
						fileListAsync.complete();
					})
					.onFailure(err -> context.fail(err));

					participantBundleAsync.complete();
				})
				.onFailure(err -> context.fail(err));

			})
			.onFailure(err -> context.fail(err));
			// we will simply collect data for the first task.
			Async taskBundling = context.async();
			dbg.buildTasksBundle(new JsonArray().add(position1), projInstance.getID())
			.onSuccess(dlID -> {
				awaitReady(dbg, dlID, Promise.<Void>promise())
				.onSuccess(dlReady -> {
					Async statusAsync = context.async();
					dbg.getDownloadStatus(dlID).onSuccess( status -> {
						context.assertEquals(status.getString("status"), DownloadStatus.downloadReady.toString());
						statusAsync.complete();
					})
					.onFailure(err -> context.fail(err));
					Async fileListAsync = context.async();
					dbg.getDownloadFilesFromDB(dlID).onSuccess( files -> {
						context.assertEquals(3, files.size());
						int found = 0;
						for(int i = 0; i < files.size(); ++i)
						{
							if(files.getJsonObject(i).getString("originalFileName").contains("TestData.txt"))
							{
								checkFileSame(Path.of(dataDir, "textData.txt").toString(), new DataLakeFile(files.getJsonObject(i)).getAbsolutePath(), context);
								found++;
							}
							if(files.getJsonObject(i).getString("originalFileName").contains("TestImage.jpg"))
							{
								checkFileSame(Path.of(dataDir, "ImageData.jpg").toString(), new DataLakeFile(files.getJsonObject(i)).getAbsolutePath(), context);
								found++;
							}
							if(files.getJsonObject(i).getString("originalFileName").equals("data.json"))
							{		
								found++;
								try {																					
									JsonObject dataJson = new JsonObject(Files.readString(Path.of(new DataLakeFile(files.getJsonObject(i)).getAbsolutePath())));
									context.assertTrue(dataJson.containsKey("taskResults"));
									context.assertEquals(1,dataJson.getJsonArray("taskResults").size());
									JsonObject taskResults = dataJson.getJsonArray("taskResults").getJsonObject(0);																					
									context.assertEquals(position1, taskResults.getString("taskID"));
									context.assertEquals(2, taskResults.getJsonArray("resultData").size());
									JsonObject part1Results = taskResults.getJsonArray("resultData").getJsonObject(0);
									JsonObject part2Results = taskResults.getJsonArray("resultData").getJsonObject(1);
									if(part1Results.getString("participantID").equals(participant2.getID()))
									{
										JsonObject tmp = part1Results;
										part1Results = part2Results;
										part2Results = tmp;
									}
									context.assertEquals(1, part1Results.getJsonArray("dbData").size());
									context.assertEquals("more", part1Results.getJsonArray("dbData").getJsonObject(0).getString("value"));
									context.assertEquals(1, part1Results.getJsonArray("files").size());
									context.assertEquals("image/jpg",part1Results.getJsonArray("files").getJsonObject(0).getString("mimeType"));
									context.assertTrue(part1Results.getJsonArray("files").getJsonObject(0).getString("originalFileName").contains("TestImage.jpg"));
									context.assertEquals(participant2.getID(), part2Results.getString("participantID"));
									context.assertEquals(1, part2Results.getJsonArray("dbData").size());																					
									context.assertEquals("else", part2Results.getJsonArray("dbData").getJsonObject(0).getString("value"));
									context.assertEquals("text/plain",part2Results.getJsonArray("files").getJsonObject(0).getString("mimeType"));
									context.assertTrue(part2Results.getJsonArray("files").getJsonObject(0).getString("originalFileName").contains("TestData.txt"));
								}
								catch(IOException e)
								{
									context.fail(e);
								}
							}
						}
						context.assertEquals(3, found);
						fileListAsync.complete();
					})
					.onFailure(err -> context.fail(err));

					taskBundling.complete();
				})
				.onFailure(err -> context.fail(err));

			})
			.onFailure(err -> context.fail(err));
			Async taskRequest = context.async();
			dbg.buildTaskBundle(position1, projInstance.getID())
			.onSuccess(dlID -> {
				awaitReady(dbg, dlID, Promise.<Void>promise())
				.onSuccess(dlReady -> {
					Async statusAsync = context.async();
					dbg.getDownloadStatus(dlID).onSuccess( status -> {
						context.assertEquals(status.getString("status"), DownloadStatus.downloadReady.toString());
						statusAsync.complete();
					})
					.onFailure(err -> context.fail(err));
					Async fileListAsync = context.async();
					dbg.getDownloadFilesFromDB(dlID).onSuccess( files -> {
						context.assertEquals(3, files.size());
						int found = 0;
						for(int i = 0; i < files.size(); ++i)
						{
							if(files.getJsonObject(i).getString("originalFileName").contains("TestData.txt"))
							{
								checkFileSame(Path.of(dataDir, "textData.txt").toString(), new DataLakeFile(files.getJsonObject(i)).getAbsolutePath(), context);
								found++;
							}
							if(files.getJsonObject(i).getString("originalFileName").contains("TestImage.jpg"))
							{
								checkFileSame(Path.of(dataDir, "ImageData.jpg").toString(), new DataLakeFile(files.getJsonObject(i)).getAbsolutePath(), context);
								found++;
							}
							if(files.getJsonObject(i).getString("originalFileName").equals("data.json"))
							{	
								found++;
								try {
									JsonObject dataJson = new JsonObject(Files.readString(Path.of(new DataLakeFile(files.getJsonObject(i)).getAbsolutePath())));
									context.assertFalse(dataJson.containsKey("taskResults"));																					
									JsonObject taskResults = dataJson;																					
									context.assertEquals(position1, taskResults.getString("taskID"));
									context.assertEquals(2, taskResults.getJsonArray("resultData").size());
									JsonObject part1Results = taskResults.getJsonArray("resultData").getJsonObject(0);
									JsonObject part2Results = taskResults.getJsonArray("resultData").getJsonObject(1);
									if(part1Results.getString("participantID").equals(participant2.getID()))
									{
										JsonObject tmp = part1Results;
										part1Results = part2Results;
										part2Results = tmp;
									}
									context.assertEquals(1, part1Results.getJsonArray("dbData").size());
									context.assertEquals("more", part1Results.getJsonArray("dbData").getJsonObject(0).getString("value"));
									context.assertEquals(1, part1Results.getJsonArray("files").size());
									context.assertEquals("image/jpg",part1Results.getJsonArray("files").getJsonObject(0).getString("mimeType"));
									context.assertTrue(part1Results.getJsonArray("files").getJsonObject(0).getString("originalFileName").contains("TestImage.jpg"));
									context.assertEquals(participant2.getID(), part2Results.getString("participantID"));
									context.assertEquals(1, part2Results.getJsonArray("dbData").size());																					
									context.assertEquals("else", part2Results.getJsonArray("dbData").getJsonObject(0).getString("value"));
									context.assertEquals("text/plain",part2Results.getJsonArray("files").getJsonObject(0).getString("mimeType"));
									context.assertTrue(part2Results.getJsonArray("files").getJsonObject(0).getString("originalFileName").contains("TestData.txt"));
								}
								catch(IOException e)
								{
									context.fail(e);
								}
							}
						}
						context.assertEquals(3, found);
						fileListAsync.complete();
					})
					.onFailure(err -> context.fail(err));

					taskRequest.complete();
				})
				.onFailure(err -> context.fail(err));

			})															
			.onFailure(err -> context.fail(err));
			Async partRequest = context.async();
			dbg.buildParticipantBundle(participant.getID(), projInstance.getID())
			.onSuccess(dlID -> {
				awaitReady(dbg, dlID, Promise.<Void>promise())
				.onSuccess(dlReady -> {
					Async statusAsync = context.async();
					dbg.getDownloadStatus(dlID).onSuccess( status -> {
						context.assertEquals(status.getString("status"), DownloadStatus.downloadReady.toString());
						statusAsync.complete();
					})
					.onFailure(err -> context.fail(err));
					Async fileListAsync = context.async();
					dbg.getDownloadFilesFromDB(dlID).onSuccess( files -> {
						context.assertEquals(2, files.size());
						int found = 0; 
						for(int i = 0; i < files.size(); ++i)
						{
							if(files.getJsonObject(i).getString("originalFileName").contains("TestImage.jpg"))
							{
								checkFileSame(Path.of(dataDir, "ImageData.jpg").toString(), new DataLakeFile(files.getJsonObject(i)).getAbsolutePath(), context);
								found++;
							}
							if(files.getJsonObject(i).getString("originalFileName").equals("data.json"))
							{	
								found++;																			
								try {
									JsonObject dataJson = new JsonObject(Files.readString(Path.of(new DataLakeFile(files.getJsonObject(i)).getAbsolutePath())));

									context.assertFalse(dataJson.containsKey("participantResults"));																					
									JsonObject part1Results = dataJson;																					
									context.assertEquals(participant.getID(), part1Results.getString("participantID"));
									context.assertEquals(1, part1Results.getJsonArray("resultData").size());
									context.assertEquals(1, part1Results.getJsonArray("resultData").getJsonObject(0).getJsonArray("dbData").size());
									context.assertEquals("more", part1Results.getJsonArray("resultData").getJsonObject(0).getJsonArray("dbData").getJsonObject(0).getString("value"));
									context.assertEquals(1, part1Results.getJsonArray("resultData").getJsonObject(0).getJsonArray("files").size());
									context.assertEquals("image/jpg",part1Results.getJsonArray("resultData").getJsonObject(0).getJsonArray("files").getJsonObject(0).getString("mimeType"));
									context.assertTrue(part1Results.getJsonArray("resultData").getJsonObject(0).getJsonArray("files").getJsonObject(0).getString("originalFileName").contains("TestImage.jpg"));																					
								}
								catch(IOException e)
								{
									context.fail(e);
								}
							}																			
						}
						context.assertEquals(2, found);
						fileListAsync.complete();
					})
					.onFailure(err -> context.fail(err));

					partRequest.complete();
				})
				.onFailure(err -> context.fail(err));

			})
			.onFailure(err -> context.fail(err));
			testComplete.complete();
		})
				.onFailure(err -> context.fail(err));

	}

	private Future<Void> awaitStatus(DataBundleGeneratorVerticle dbg, String dlID,
			Promise<Void> readyPromise, DownloadStatus expectedState, long timeout)
	{		
		if(timeout < 0)
		{
			readyPromise.fail(new TimeoutException("Expected Status not achieved during timout period"));
		}
		long startTime = System.currentTimeMillis();
		dbg.getDownloadStatus(dlID).onSuccess(status -> 
		{
			if(status.getString("status").equals(expectedState.toString()))
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
						long waited = System.currentTimeMillis() - startTime;
						awaitStatus(dbg, dlID, readyPromise, expectedState, timeout - waited);
					}
					catch(InterruptedException e)
					{
						readyPromise.fail(e);
					}
				}
			}											
		})
		.onFailure(err -> {
			if(err instanceof ObjectDoesNotExist)
			{
				if(expectedState == DownloadStatus.notExistent)
				{
					readyPromise.complete();
				}
				else {
					try {
						// we need to wait a tiny bit... 
						TimeUnit.MILLISECONDS.sleep(50);
						long waited = System.currentTimeMillis() - startTime;
						awaitStatus(dbg, dlID, readyPromise, expectedState, timeout - waited);
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
	
	private Future<Void> awaitReady(DataBundleGeneratorVerticle dbg, String dlID, Promise<Void> readyPromise)
	{		
		return awaitStatus(dbg, dlID, readyPromise, DownloadStatus.downloadReady, 3000);
	}

	private void checkFileSame(String File1, String File2, TestContext context)
	{
		try
		{
			File f = new File(File1);
			File f2 =  new File(File2);
			InputStream inputStream1 = new FileInputStream(f);
			InputStream inputStream2 = new FileInputStream(f2);
			context.assertTrue(IOUtils.contentEquals(inputStream1, inputStream2));			
		}
		catch(IOException e)
		{
			context.fail(e);
		}
	}

}
