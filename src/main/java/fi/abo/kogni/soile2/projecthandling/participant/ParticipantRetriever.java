package fi.abo.kogni.soile2.projecthandling.participant;



import fi.abo.kogni.soile2.datamanagement.utils.DataRetriever;
import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;

public class ParticipantRetriever implements DataRetriever<String, DataParticipant>{

	MongoClient client;
	DataParticipantFactory partFactory;
	public ParticipantRetriever(MongoClient client, ParticipantManager manager)
	{
		this.client = client;
	}
	@Override
	public Future<DataParticipant> getElement(String key) {
		Promise<DataParticipant> participantPromise = Promise.<DataParticipant>promise();
		JsonObject query = new JsonObject()
							   .put("_id", key);
		client.findOne(SoileConfigLoader.getdbProperty("participantCollection"), query, null)
		.onSuccess(partJson -> 
		{						
			DataParticipant p = partFactory.createParticipant(partJson);
			participantPromise.complete(p);
			
		}).onFailure(err ->{
			participantPromise.fail(err);
		});
		return participantPromise.future();
	}

	@Override
	public void getElement(String key, Handler<AsyncResult<DataParticipant>> handler) {
		handler.handle(getElement(key));		
	}

}
