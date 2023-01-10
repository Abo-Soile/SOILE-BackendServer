package fi.abo.kogni.soile2.http_server.verticles;

import java.nio.file.Path;

import org.junit.Test;

import fi.abo.kogni.soile2.datamanagement.datalake.DataLakeManager;
import fi.abo.kogni.soile2.datamanagement.git.GitManager;
import fi.abo.kogni.soile2.http_server.SoileVerticleTest;
import fi.abo.kogni.soile2.projecthandling.exceptions.InvalidPositionException;
import fi.abo.kogni.soile2.projecthandling.participant.Participant;
import fi.abo.kogni.soile2.projecthandling.participant.ParticipantHandler;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.impl.ProjectInstanceHandler;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.impl.TaskFileResult;
import fi.abo.kogni.soile2.projecthandling.utils.ObjectGenerator;
import fi.abo.kogni.soile2.utils.DataProvider;
import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.web.FileUpload;

public class ParticipantVerticleTest extends SoileVerticleTest {

	@Test
	public void taskdeleteParticipantTest(TestContext context)
	{		
		Async testAsync = context.async();
		JsonObject smokerOutput = new JsonObject()
				.put("name", "smoker")
				.put("value", 1);
		DataLakeManager dlm = new DataLakeManager(resultDataLakeDir, vertx);
		JsonObject nonSmokerQuestionaireOutput = new JsonObject().put("outputData", new JsonArray().add(smokerOutput));		
		ProjectInstanceHandler projHandler = new ProjectInstanceHandler(mongo_client, eb);
		ParticipantHandler partHandler = new ParticipantHandler(mongo_client, projHandler, vertx);
		ObjectGenerator.createProject(mongo_client, new GitManager(eb), "Testproject")
		.onSuccess(projectData -> {
			projHandler.createProjectInstance(projectData.put("name", "NewProjectInstance").put("private", false))
			.onSuccess(proj -> {
				partHandler.create(proj)
				.onSuccess(participant1 -> {
					proj.startProject(participant1)
					.onSuccess(taskID -> {
						participant1.getCurrentStep()
						// create The File Data
						.onSuccess(val -> 
						{
							createUpload(participant1, val, taskID, dlm)
								.onSuccess( fileData -> {
									//now, build the result data
									nonSmokerQuestionaireOutput.put("resultData", new JsonObject().put("fileData", fileData)
											.put("jsonData", new JsonArray().add(new JsonObject().put("name", "something")
											.put("value", "more"))))
											.put("taskID", taskID);
									proj.finishStep(participant1, nonSmokerQuestionaireOutput)
									.onSuccess(res -> {
										JsonObject fileResult = fileData.getJsonObject(0);
										TaskFileResult fileRes = new TaskFileResult(fileResult.getString("filename"),
												fileResult.getString("targetid"),
												fileResult.getString("fileformat"),
												val,
												taskID,
												participant1.getID());
										context.assertTrue(dlm.getFile(fileRes).exists());
										mongo_client.findOne(SoileConfigLoader.getdbProperty("participantCollection"), new JsonObject().put("_id", participant1.getID()),null)
										.onSuccess(dbData -> {
											System.out.println(dbData.encodePrettily());
											context.assertEquals(taskID, dbData.getJsonArray("outputData").getJsonObject(0).getString("task"));
											context.assertEquals("smoker", dbData.getJsonArray("outputData").getJsonObject(0).getJsonArray("outputs").getJsonObject(0).getString("name"));
											context.assertEquals(1, dbData.getJsonArray("outputData").getJsonObject(0).getJsonArray("outputs").getJsonObject(0).getValue("value"));
											context.assertEquals("something", dbData.getJsonArray("resultData").getJsonObject(0).getJsonArray("dbData").getJsonObject(0).getString("name"));
											context.assertEquals("more", dbData.getJsonArray("resultData").getJsonObject(0).getJsonArray("dbData").getJsonObject(0).getValue("value"));
											// And now finally we delete the participant
											JsonArray deletionCommand = new JsonArray().add(new JsonObject().put("participantID", participant1.getID()).put("projectID", proj.getID()));
											eb.request("soile.participant.delete", deletionCommand)
											.onSuccess(reply -> {
												mongo_client.findOne(SoileConfigLoader.getdbProperty("participantCollection"), new JsonObject().put("_id", participant1.getID()),null)
												.onSuccess(nullRes -> {
													context.assertNull(nullRes);
													context.assertFalse(dlm.getFile(fileRes).exists());
													testAsync.complete();
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
	})
		.onFailure(err -> context.fail(err));
}

private Future<JsonArray> createUpload(Participant participant, int p1step, String position1, DataLakeManager dlm)
{
	Promise<JsonArray> resultData = Promise.promise();
	String dataDir = DataBundleTest.class.getClassLoader().getResource("FileTestData").getPath();		
	try {
		String tempDataDir = DataProvider.createTempDataDirectory(dataDir);
		// Add some Files for the participants
		FileUpload tempUpload = DataProvider.getFileUploadForTarget(Path.of(tempDataDir,"ImageData.jpg").toString(), "TestImage.jpg", "image/jpg");
		dlm.storeParticipantData(participant.getID(), p1step, position1, tempUpload)
		.onSuccess(fileID1 -> {					

			resultData.complete(new JsonArray().add(new JsonObject().put("targetid", fileID1)
					.put("fileformat", "image/jpg")
					.put("filename", "TestImage.jpg")));
		})
		.onFailure(err -> resultData.fail(err));
	}
	catch(Exception e)
	{
		resultData.fail(e);					
	}
	return resultData.future();
}
}
