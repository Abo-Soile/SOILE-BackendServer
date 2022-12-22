package fi.abo.kogni.soile2.projecthandling.participant;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Test;

import fi.abo.kogni.soile2.GitTest;
import fi.abo.kogni.soile2.datamanagement.utils.CheckDirtyMap;
import fi.abo.kogni.soile2.projecthandling.ProjectBaseTest;
import fi.abo.kogni.soile2.projecthandling.utils.ProjectFactoryImplForTesting;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;

public class ParticipantManagerTest extends GitTest{


	@Test
	public void testGetParticipantResults(TestContext context) {
		ParticipantManager mgr = new ParticipantManager(mongo_client);

		try
		{
			Async testAsync = context.async();
			JsonArray ResultTestData = new JsonArray(Files.readString(Paths.get(ParticipantManagerTest.class.getClassLoader().getResource("InstanceData/ResultTestData.json").getPath())));
			ProjectFactoryImplForTesting.loadProject(ProjectBaseTest.getPos(0))
			.onSuccess(project -> {
				mgr.createParticipant(project)
				.onSuccess(participant -> {
					JsonArray comparisonData = ResultTestData.getJsonObject(0).getJsonArray("dbData");
					JsonArray comparisonData2 = ResultTestData.getJsonObject(1).getJsonArray("dbData");
					participant.addResult("NewTask", ResultTestData.getJsonObject(0))
					.onSuccess(res -> {
						Async getResultsAsync = context.async();
						mgr.getParticipantsResultsForTask(new JsonArray().add(participant.getID()), "" , "NewTask")
						.onSuccess(results -> {
							// there should be exactly one element
							context.assertEquals(1, results.size());
							context.assertEquals("NewTask", results.get(0).getJsonArray("participantData").getJsonObject(0).getString("task"));
							context.assertEquals(0, results.get(0).getJsonArray("participantData").getJsonObject(0).getInteger("step"));
							JsonArray resultData = results.get(0).getJsonArray("participantData").getJsonObject(0).getJsonArray("dbData");
							context.assertEquals(comparisonData.size(),resultData.size());
							for(int i = 0; i < resultData.size(); ++i)
							{
								JsonObject resO = resultData.getJsonObject(i);
								boolean found = false;
								for(int j = 0; j < comparisonData.size(); ++j)
								{
									if(comparisonData.getJsonObject(j).getString("name").equals(resO.getString("name")))
									{
										found = true;
										context.assertEquals(comparisonData.getJsonObject(j).getValue("value"), resO.getValue("value"));
										comparisonData.remove(j);
										break;
									}
								}							
								context.assertTrue(found);
							}							
							context.assertEquals(0,comparisonData.size());
							participant.addResult("NewTask", ResultTestData.getJsonObject(1))
							.onSuccess(added -> 
							{
								mgr.getParticipantsResultsForTask(new JsonArray().add(participant.getID()), "" , "NewTask")
								.onSuccess(resultsChanged -> {
									context.assertEquals(1, resultsChanged.size());
									context.assertEquals("NewTask", resultsChanged.get(0).getJsonArray("participantData").getJsonObject(0).getString("task"));
									context.assertEquals(0, resultsChanged.get(0).getJsonArray("participantData").getJsonObject(0).getInteger("step"));
									JsonArray resultData2 = resultsChanged.get(0).getJsonArray("participantData").getJsonObject(0).getJsonArray("dbData");
									context.assertEquals(comparisonData2.size(),resultData2.size());
									for(int i = 0; i < resultData2.size(); ++i)
									{
										JsonObject resO = resultData2.getJsonObject(i);
										boolean found = false;
										for(int j = 0; j < comparisonData2.size(); ++j)
										{
											if(comparisonData2.getJsonObject(j).getString("name").equals(resO.getString("name")))
											{
												found = true;
												context.assertEquals(comparisonData2.getJsonObject(j).getValue("value"), resO.getValue("value"));
												comparisonData2.remove(j);
												break;
											}
										}							
										context.assertTrue(found);
									}							
									context.assertEquals(0,comparisonData.size());
									getResultsAsync.complete();
								})
								.onFailure(err -> context.fail(err));
							})
							.onFailure(err -> context.fail(err));

						});
						testAsync.complete();
					});

				});
			});
		}
		catch(IOException e)
		{
			context.fail(e);
		}
	}


	@Test
	public void testParticipantRetrieval(TestContext context) {
		ParticipantManager mgr = new ParticipantManager(mongo_client);
		try
		{
			Async testAsync = context.async();
			JsonArray ResultTestData = new JsonArray(Files.readString(Paths.get(ParticipantManagerTest.class.getClassLoader().getResource("InstanceData/ResultTestData.json").getPath())));
			CheckDirtyMap<String, Participant> partMap = new CheckDirtyMap<>(mgr, 3600*1000);
			ProjectFactoryImplForTesting.loadProject(ProjectBaseTest.getPos(0))
			.onSuccess(project -> {
				mgr.createParticipant(project)
				.onSuccess(participant -> {
					partMap.getData(participant.getID())
					.onSuccess(participant2 -> {
						partMap.getData(participant.getID())
						.onSuccess(participant3 -> {
						context.assertEquals(participant.getID(),participant3.getID());
						// nothing has happened in between, so this has to be the same object.
						context.assertTrue(participant2 == participant3);
						// this is false, as the participant is saved directly after creation.
						context.assertFalse(participant == participant3);
						participant.save()
						.onSuccess(id -> {
							partMap.getData(participant.getID())
							.onSuccess(participant4 -> {
								context.assertFalse(participant4 == participant);
								context.assertEquals(participant4.getID(), participant.getID());
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
		}
		catch(IOException e)
		{
			context.fail(e);
		}
	}
}