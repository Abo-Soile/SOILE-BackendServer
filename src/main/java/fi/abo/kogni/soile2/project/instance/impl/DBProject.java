package fi.abo.kogni.soile2.project.instance.impl;



import fi.abo.kogni.soile2.project.instance.ProjectInstance;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;

public class DBProject extends ProjectInstance{
	
	MongoClient client;
	String projectInstanceDB;
	
	public DBProject(MongoClient client, String projectInstanceDB) {
		super();
		this.client = client;
		this.projectInstanceDB = projectInstanceDB;
		// TODO Auto-generated constructor stub
	}

	@Override
	public Future<Void> save() {
		Promise<Void> saveSuccess = Promise.<Void>promise();		
		
		return saveSuccess.future();		
	}

	/**
	 * This loader expects a _id field in the json.
	 */
	@Override
	public Future<JsonObject> load(JsonObject object) {
		Promise<JsonObject> loadSuccess = Promise.<JsonObject>promise();		
		JsonObject query = new JsonObject().put("_id", object.getString("_id"));
		client.findOne(projectInstanceDB, query, null).onSuccess(InstanceJson -> {
			
		}).onFailure(fail -> {
			loadSuccess.fail(fail);
		});
		return loadSuccess.future();		
	}

	@Override
	public Future<JsonObject> delete() {
		Promise<JsonObject> loadSuccess = Promise.<JsonObject>promise();		
		JsonObject query = new JsonObject().put("_id", this.instanceID);
		return client.findOneAndDelete(projectInstanceDB, query);		
	}
	
	

}
