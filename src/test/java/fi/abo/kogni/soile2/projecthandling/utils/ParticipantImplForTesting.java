package fi.abo.kogni.soile2.projecthandling.utils;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.UUID;

import fi.abo.kogni.soile2.projecthandling.participant.Participant;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.ProjectInstance;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;

public class ParticipantImplForTesting extends Participant {

	private HashMap<String, JsonArray> resultsMap;

	public ParticipantImplForTesting(JsonObject data, ProjectInstance p) {
		super(data, p);
		resultsMap = new HashMap<String, JsonArray>();
		// TODO Auto-generated constructor stub
	}

	@Override
	public Future<String> save() {
				return Future.succeededFuture(this.uuid);
	}

	@Override
	public Future<String> saveJsonResults(JsonArray results) {
		String newUUID = UUID.randomUUID().toString();
		resultsMap.put(newUUID, results);
		return Future.succeededFuture(newUUID);		
	}
	
	public static Future<Participant> getTestParticipant(TestContext context, int i, JsonObject projectData)
	{
		Promise<Participant> partProm = Promise.<Participant>promise();
		JsonArray Participant_data;
		try
		{			
			Participant_data = new JsonObject(Files.readString(Paths.get(ParticipantImplForTesting.class.getClassLoader().getResource("Participant.json").getPath()))).getJsonArray("data");
		}
		catch(Exception e)
		{			
			partProm.fail(e);
			return partProm.future();
		}
		Async partCreation = context.async();
		ProjectFactoryImplForTesting fac = new ProjectFactoryImplForTesting();
		ProjectInstance.instantiateProject(projectData, fac).onSuccess(project -> {
			partProm.complete(new ParticipantImplForTesting(Participant_data.getJsonObject(i), project));
			partCreation.complete();
		}).onFailure(fail ->{
			partProm.fail(fail);
			partCreation.complete();
		});
		return partProm.future();
		
	}	
	
}