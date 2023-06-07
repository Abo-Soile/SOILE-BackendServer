package fi.abo.kogni.soile2.http_server.verticles;

import java.nio.file.Path;

import org.junit.Test;

import fi.abo.kogni.soile2.datamanagement.datalake.ParticipantDataLakeManager;
import fi.abo.kogni.soile2.http_server.SoileVerticleTest;
import fi.abo.kogni.soile2.projecthandling.participant.Participant;
import fi.abo.kogni.soile2.projecthandling.participant.ParticipantHandler;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.impl.StudyHandler;
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
		System.out.println("--------------------------------------------------------------------  Testing Participant Deletion via verticle ----------------------");
		Async testAsync = context.async();
		JsonObject smokerOutput = new JsonObject()
				.put("name", "smoker")
				.put("value", 1);
		ParticipantDataLakeManager dlm = new ParticipantDataLakeManager(resultDataLakeDir, vertx);
		JsonObject nonSmokerQuestionaireOutput = new JsonObject().put("outputData", new JsonArray().add(smokerOutput));		
		StudyHandler projHandler = new StudyHandler(mongo_client, vertx);
		ParticipantHandler partHandler = new ParticipantHandler(mongo_client, projHandler, vertx);
		ObjectGenerator.createProject(mongo_client, vertx, "Testproject")
		.onSuccess(projectData -> {
			System.out.println("------------------------------------------------Project created");
			projHandler.createProjectInstance(projectData.put("name", "NewProjectInstance").put("private", false))
			.onSuccess(proj -> {
				proj.activate()
				.onSuccess(activated -> {
					System.out.println("------------------------------------------------Project Instance created");
					partHandler.create(proj)
					.onSuccess(participant1 -> {
						System.out.println("------------------------------------------------Participant 1 created");
						proj.startStudy(participant1)
						.onSuccess(taskID -> {
							System.out.println("------------------------------------------------Participant 1 started");
							participant1.getCurrentStep()
							// create The File Data
							.onSuccess(stepVal -> 
							{
								System.out.println("------------------------------------------------Current step obtained");
								createUpload(participant1, stepVal, taskID, dlm)
								.onSuccess( fileData -> {
									System.out.println("------------------------------------------------Upload created");
									//now, build the result data
									nonSmokerQuestionaireOutput.put("resultData", new JsonObject().put("fileData", fileData)
											.put("jsonData", new JsonArray().add(new JsonObject().put("name", "something")
													.put("value", "more"))))
									.put("taskID", taskID);
									proj.finishStep(participant1, nonSmokerQuestionaireOutput)
									.onSuccess(res -> {
										System.out.println("------------------------------------------------Step finished");
										JsonObject fileResult = fileData.getJsonObject(0);
										TaskFileResult fileRes = new TaskFileResult(fileResult.getString("targetid"),
												fileResult.getString("filename"),												
												fileResult.getString("fileformat"),
												stepVal,
												taskID,
												participant1.getID());
										context.assertTrue(dlm.getFile(fileRes).exists());
										mongo_client.findOne(SoileConfigLoader.getdbProperty("participantCollection"), new JsonObject().put("_id", participant1.getID()),null)
										.onSuccess(dbData -> {
											System.out.println("------------------------------------------------Mongo Data retrieved");
											context.assertEquals(taskID, dbData.getJsonArray("outputData").getJsonObject(0).getString("task"));
											context.assertEquals("smoker", dbData.getJsonArray("outputData").getJsonObject(0).getJsonArray("outputs").getJsonObject(0).getString("name"));
											context.assertEquals(1, dbData.getJsonArray("outputData").getJsonObject(0).getJsonArray("outputs").getJsonObject(0).getValue("value"));
											context.assertEquals("something", dbData.getJsonArray("resultData").getJsonObject(0).getJsonArray("dbData").getJsonObject(0).getString("name"));
											context.assertEquals("more", dbData.getJsonArray("resultData").getJsonObject(0).getJsonArray("dbData").getJsonObject(0).getValue("value"));
											// And now finally we delete the participant
											JsonArray deletionCommand = new JsonArray().add(new JsonObject().put("participantID", participant1.getID()).put("projectID", proj.getID()));
											eb.request("soile.participant.delete", deletionCommand)
											.onSuccess(reply -> {
												System.out.println("------------------------------------------------Deletion succeeded");
												mongo_client.findOne(SoileConfigLoader.getdbProperty("participantCollection"), new JsonObject().put("_id", participant1.getID()),null)
												.onSuccess(nullRes -> {
													System.out.println("------------------------------------------------Participant no longer present");
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
		})
		.onFailure(err -> context.fail(err));
	}

	private Future<JsonArray> createUpload(Participant participant, int p1step, String position1, ParticipantDataLakeManager dlm)
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
