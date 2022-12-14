package fi.abo.kogni.soile2.projecthandling.utils;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;

import fi.abo.kogni.soile2.projecthandling.participant.Participant;
import fi.abo.kogni.soile2.projecthandling.participant.ParticipantImpl;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;

public class ParticipantImplForTesting extends ParticipantImpl {

	private HashMap<String, JsonArray> resultsMap;

	public ParticipantImplForTesting(JsonObject data) {
		super(data);
		resultsMap = new HashMap<String, JsonArray>();
		// TODO Auto-generated constructor stub
	}

	@Override
	public Future<String> save() {
				return Future.succeededFuture(this.uuid);
	}

	public static Future<Participant> getTestParticipant(TestContext context, int i, JsonObject id)
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
		partProm.complete(new ParticipantImplForTesting(Participant_data.getJsonObject(i)));		
		return partProm.future();
		
	}

	@Override
	public Future<Void> addResult(String taskID, JsonObject result) {
		if(!resultsMap.containsKey(taskID))
		{
			resultsMap.put(taskID, new JsonArray());
		}
		resultsMap.get(taskID).add(result);
		return Future.succeededFuture();
	}

	@Override
	public Future<Integer> getCurrentStep() { 
		return Future.succeededFuture(currentStep);
	}

	
	
}
