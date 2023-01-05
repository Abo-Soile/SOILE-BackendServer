package fi.abo.kogni.soile2.http_server.verticles;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import fi.abo.kogni.soile2.ElementTester;
import fi.abo.kogni.soile2.datamanagement.datalake.DataLakeManager;
import fi.abo.kogni.soile2.http_server.verticles.DataBundleGeneratorVerticle.DownloadStatus;
import fi.abo.kogni.soile2.projecthandling.participant.ParticipantHandler;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.impl.ProjectInstanceHandler;
import fi.abo.kogni.soile2.utils.DataProvider;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.web.FileUpload;

public class DataBundleTest extends ElementTester {

	ProjectInstanceHandler projHandler;
	ParticipantHandler partHandler;
	DataBundleGeneratorVerticle dbg;
	@Override
	public void runBeforeTests(TestContext context)
	{
		super.runBeforeTests(context);	
		projHandler = new ProjectInstanceHandler(mongo_client, vertx.eventBus());
		partHandler = new ParticipantHandler(mongo_client, projHandler, vertx);
		dbg = new DataBundleGeneratorVerticle(mongo_client, projHandler, partHandler);
		vertx.deployVerticle(dbg);
	}

	@Test
	public void testDataBundling(TestContext context)
	{
		// this test is somewhat overloaded. However this allows testing multiple downloads simultaneously.
		String dataDir = DataBundleTest.class.getClassLoader().getResource("FileTestData").getPath();

		Async testComplete = context.async();
		DataLakeManager dlm = new DataLakeManager(resultDataLakeDir, vertx);
		createProject(context).onSuccess(project ->
		{
			projHandler.createProjectInstance(getCreationJson(project, "TestProject", ""))
			.onSuccess(projInstance -> {
				partHandler.create(projInstance)				
				.onSuccess(participant -> {
					projInstance.startProject(participant)
					.onSuccess(position1 -> { 
						partHandler.create(projInstance)
						.onSuccess(participant2 -> {
							projInstance.startProject(participant2)
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
															// so, now we have two participants each with Result Data.
															System.out.println("trying to obtain a bundle");
															Async participantBundleAsync = context.async();
															dbg.buildParticipantsBundle(new JsonArray().add(participant.getID()).add(participant2.getID()), projInstance.getID())
															.onSuccess(dlID -> {
																System.out.println("Bundle generation started");
																awaitReady(dbg, dlID, Promise.<Void>promise())
																.onSuccess(dlReady -> {
																	System.out.println("Bundle Generated");
																	Async statusAsync = context.async();
																	dbg.getDownloadStatus(dlID).onSuccess( status -> {
																		context.assertEquals(status.getString("status"), DownloadStatus.downloadReady.toString());
																		statusAsync.complete();
																	})
																	.onFailure(err -> context.fail(err));
																	Async fileListAsync = context.async();
																	dbg.getDownloadFilesFromDB(dlID).onSuccess( files -> {
																		System.out.println(files.encodePrettily());
																		context.assertEquals(3, files.size());
																		int found = 0;
																		for(int i = 0; i < files.size(); ++i)
																		{
																			if(files.getJsonObject(i).getString("originalFileName").contains("TestData.txt"))
																			{
																				checkFileSame(Path.of(dataDir, "textData.txt").toString(), files.getJsonObject(i).getString("absolutePath"), context);
																				found++;
																			}
																			if(files.getJsonObject(i).getString("originalFileName").contains("TestImage.jpg"))
																			{
																				checkFileSame(Path.of(dataDir, "ImageData.jpg").toString(), files.getJsonObject(i).getString("absolutePath"), context);
																				found++;
																			}
																			if(files.getJsonObject(i).getString("originalFileName").equals("data.json"))
																			{			
																				found++;
																				try {
																					JsonObject dataJson = new JsonObject(Files.readString(Path.of(files.getJsonObject(i).getString("absolutePath"))));
																					
																					context.assertTrue(dataJson.containsKey("participantResults"));
																					context.assertEquals(2,dataJson.getJsonArray("participantResults").size());
																					JsonObject part1Results = dataJson.getJsonArray("participantResults").getJsonObject(0);
																					JsonObject part2Results = dataJson.getJsonArray("participantResults").getJsonObject(1);
																					System.out.println(part1Results.encodePrettily());
																					System.out.println(part2Results.encodePrettily());
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
																System.out.println("Bundle generation started");
																awaitReady(dbg, dlID, Promise.<Void>promise())
																.onSuccess(dlReady -> {
																	System.out.println("Bundle Generated");
																	Async statusAsync = context.async();
																	dbg.getDownloadStatus(dlID).onSuccess( status -> {
																		context.assertEquals(status.getString("status"), DownloadStatus.downloadReady.toString());
																		statusAsync.complete();
																	})
																	.onFailure(err -> context.fail(err));
																	Async fileListAsync = context.async();
																	dbg.getDownloadFilesFromDB(dlID).onSuccess( files -> {
																		System.out.println(files.encodePrettily());
																		context.assertEquals(3, files.size());
																		int found = 0;
																		for(int i = 0; i < files.size(); ++i)
																		{
																			if(files.getJsonObject(i).getString("originalFileName").contains("TestData.txt"))
																			{
																				checkFileSame(Path.of(dataDir, "textData.txt").toString(), files.getJsonObject(i).getString("absolutePath"), context);
																				found++;
																			}
																			if(files.getJsonObject(i).getString("originalFileName").contains("TestImage.jpg"))
																			{
																				checkFileSame(Path.of(dataDir, "ImageData.jpg").toString(), files.getJsonObject(i).getString("absolutePath"), context);
																				found++;
																			}
																			if(files.getJsonObject(i).getString("originalFileName").equals("data.json"))
																			{		
																				found++;
																				try {
																					JsonObject dataJson = new JsonObject(Files.readString(Path.of(files.getJsonObject(i).getString("absolutePath"))));
																					System.out.println(files.encodePrettily());
																					context.assertTrue(dataJson.containsKey("taskResults"));
																					context.assertEquals(1,dataJson.getJsonArray("taskResults").size());
																					JsonObject taskResults = dataJson.getJsonArray("taskResults").getJsonObject(0);																					
																					System.out.println(taskResults.encodePrettily());																					
																					context.assertEquals(position1, taskResults.getString("taskID"));
																					context.assertEquals(2, taskResults.getJsonArray("resultData").size());
																					JsonObject part1Results = taskResults.getJsonArray("resultData").getJsonObject(0);
																					JsonObject part2Results = taskResults.getJsonArray("resultData").getJsonObject(1);
																					System.out.println(part1Results.encodePrettily());
																					System.out.println(part2Results.encodePrettily());
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
																System.out.println("Bundle generation started");
																awaitReady(dbg, dlID, Promise.<Void>promise())
																.onSuccess(dlReady -> {
																	System.out.println("Bundle Generated");
																	Async statusAsync = context.async();
																	dbg.getDownloadStatus(dlID).onSuccess( status -> {
																		context.assertEquals(status.getString("status"), DownloadStatus.downloadReady.toString());
																		statusAsync.complete();
																	})
																	.onFailure(err -> context.fail(err));
																	Async fileListAsync = context.async();
																	dbg.getDownloadFilesFromDB(dlID).onSuccess( files -> {
																		System.out.println(files.encodePrettily());
																		context.assertEquals(3, files.size());
																		int found = 0;
																		for(int i = 0; i < files.size(); ++i)
																		{
																			if(files.getJsonObject(i).getString("originalFileName").contains("TestData.txt"))
																			{
																				checkFileSame(Path.of(dataDir, "textData.txt").toString(), files.getJsonObject(i).getString("absolutePath"), context);
																				found++;
																			}
																			if(files.getJsonObject(i).getString("originalFileName").contains("TestImage.jpg"))
																			{
																				checkFileSame(Path.of(dataDir, "ImageData.jpg").toString(), files.getJsonObject(i).getString("absolutePath"), context);
																				found++;
																			}
																			if(files.getJsonObject(i).getString("originalFileName").equals("data.json"))
																			{	
																				found++;
																				try {
																					JsonObject dataJson = new JsonObject(Files.readString(Path.of(files.getJsonObject(i).getString("absolutePath"))));
																					System.out.println(files.encodePrettily());
																					context.assertFalse(dataJson.containsKey("taskResults"));																					
																					JsonObject taskResults = dataJson;																					
																					System.out.println(taskResults.encodePrettily());																					
																					context.assertEquals(position1, taskResults.getString("taskID"));
																					context.assertEquals(2, taskResults.getJsonArray("resultData").size());
																					JsonObject part1Results = taskResults.getJsonArray("resultData").getJsonObject(0);
																					JsonObject part2Results = taskResults.getJsonArray("resultData").getJsonObject(1);
																					System.out.println(part1Results.encodePrettily());
																					System.out.println(part2Results.encodePrettily());
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
																System.out.println("Bundle generation started");
																awaitReady(dbg, dlID, Promise.<Void>promise())
																.onSuccess(dlReady -> {
																	System.out.println("Bundle Generated");
																	Async statusAsync = context.async();
																	dbg.getDownloadStatus(dlID).onSuccess( status -> {
																		context.assertEquals(status.getString("status"), DownloadStatus.downloadReady.toString());
																		statusAsync.complete();
																	})
																	.onFailure(err -> context.fail(err));
																	Async fileListAsync = context.async();
																	dbg.getDownloadFilesFromDB(dlID).onSuccess( files -> {
																		System.out.println(files.encodePrettily());
																		context.assertEquals(2, files.size());
																		int found = 0; 
																		for(int i = 0; i < files.size(); ++i)
																		{
																			if(files.getJsonObject(i).getString("originalFileName").contains("TestImage.jpg"))
																			{
																				checkFileSame(Path.of(dataDir, "ImageData.jpg").toString(), files.getJsonObject(i).getString("absolutePath"), context);
																				found++;
																			}
																			if(files.getJsonObject(i).getString("originalFileName").equals("data.json"))
																			{	
																				found++;																			
																				try {
																					JsonObject dataJson = new JsonObject(Files.readString(Path.of(files.getJsonObject(i).getString("absolutePath"))));
																					
																					context.assertFalse(dataJson.containsKey("participantResults"));																					
																					JsonObject part1Results = dataJson;																					
																					System.out.println(part1Results.encodePrettily());
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
			});

		});
	}

	private Future<Void> awaitReady(DataBundleGeneratorVerticle dbg, String dlID, Promise<Void> readyPromise)
	{		
		dbg.getDownloadStatus(dlID).onSuccess(status -> 
		{
			if(status.getString("status").equals(DownloadStatus.downloadReady.toString()))
			{
				System.out.println("Download Ready");
				readyPromise.complete();
				return;
			}
			else
			{
				if(status.getString("status").equals(DownloadStatus.failed.toString()))
				{
					System.out.println("Download Failed");
					readyPromise.fail(new Exception("Download Failed"));
				}
				else
				{
					try {
						System.out.println("Download not ready waiting");
						// we need to wait a tiny bit... 
						Thread.sleep(100);
						System.out.println("Testing again");
						awaitReady(dbg, dlID, readyPromise);
					}
					catch(InterruptedException e)
					{
						System.out.println("Download waiting interrupted");
						readyPromise.fail(e);
					}
				}
			}											
		});
		return readyPromise.future();
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
