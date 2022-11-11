package fi.abo.kogni.soile2.project.participant;



import fi.abo.kogni.soile2.project.instance.impl.ProjectManager;
import fi.abo.kogni.soile2.utils.DataRetriever;
import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;

public class ParticipantRetriever implements DataRetriever<String, Participant>{

	MongoClient client;
	ProjectManager projManager;	
	ParticipantFactory partFactory;
	public ParticipantRetriever(MongoClient client, ParticipantManager manager)
	{
		this.client = client;
	}
	@Override
	public Future<Participant> getElement(String key) {
		Promise<Participant> participantPromise = Promise.<Participant>promise();
		JsonObject query = new JsonObject()
							   .put("_id", key);
		client.findOne(SoileConfigLoader.getdbProperty("participantCollection"), query, null)
		.onSuccess(partJson -> 
		{			
			projManager.getElement(partJson.getString("project"))
			.onSuccess(proj ->
			{
				Participant p = partFactory.createParticipant(partJson, proj);
				participantPromise.complete(p);
			}).onFailure(fail ->
			{
				participantPromise.fail(fail);
			});
		}).onFailure(err ->{
			participantPromise.fail(err);
		});
		return participantPromise.future();
	}

	@Override
	public void getElement(String key, Handler<AsyncResult<Participant>> handler) {
		handler.handle(getElement(key));		
	}

}
