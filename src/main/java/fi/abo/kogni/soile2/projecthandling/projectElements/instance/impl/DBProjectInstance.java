package fi.abo.kogni.soile2.projecthandling.projectElements.instance.impl;



import fi.aalto.scicomp.gitFs.gitProviderVerticle;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.ProjectInstance;
import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
/**
 * This is a Project stored in a (most likely git) Database. 
 * @author thomas
 *
 */
public class DBProjectInstance extends ProjectInstance{
	
	MongoClient client;
	String projectInstanceDB;
	EventBus eb;
	public DBProjectInstance(MongoClient client, String projectInstanceDB, EventBus eb) {
		super();
		this.client = client;
		this.projectInstanceDB = projectInstanceDB;
		this.eb = eb;
		// TODO Auto-generated constructor stub
	}

	@Override
	public Future<JsonObject> save() {
		Promise<JsonObject> saveSuccess = Promise.<JsonObject>promise();		
		client.save(projectInstanceDB, toDBJson()).onSuccess(result ->
		{
			saveSuccess.complete(toDBJson());
		}).onFailure(fail ->{
			saveSuccess.fail(fail);
		});
		return saveSuccess.future();		
	}

	/**
	 * This loader expects a _id field in the json.
	 * It will then extract the remaining data first from the Project Instance database and then from the git repository.
	 */
	@Override
	public Future<JsonObject> load(JsonObject object) {
		Promise<JsonObject> loadSuccess = Promise.<JsonObject>promise();		
		JsonObject query = new JsonObject().put("_id", object.getString("_id"));
		client.findOne(projectInstanceDB, query, null).onSuccess(instanceJson -> {
			eb.request(SoileConfigLoader.getServerProperty("gitVerticleAddress"),
					   gitProviderVerticle.createGetCommand(instanceJson.getString("_id"),
							   instanceJson.getString("version"),
							   "project.json"))
			.onSuccess(response -> {
				// we got a positive reply.
				JsonObject projectData = new JsonObject(((JsonObject)response.body()).getString(gitProviderVerticle.DATAFIELD));
				instanceJson.mergeIn(projectData);
				loadSuccess.complete(instanceJson);
			}).onFailure(fail -> {
				loadSuccess.fail(fail);
			});
		}).onFailure(fail -> {
			loadSuccess.fail(fail);
		});
		return loadSuccess.future();		
	}

	@Override
	public Future<JsonObject> delete() {
		JsonObject query = new JsonObject().put("_id", this.instanceID);
		return client.findOneAndDelete(projectInstanceDB, query);		
	}
	
	

}
