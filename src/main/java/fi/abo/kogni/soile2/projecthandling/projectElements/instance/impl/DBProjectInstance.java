package fi.abo.kogni.soile2.projecthandling.projectElements.instance.impl;



import fi.aalto.scicomp.gitFs.gitProviderVerticle;
import fi.abo.kogni.soile2.datamanagement.git.GitFile;
import fi.abo.kogni.soile2.datamanagement.git.GitManager;
import fi.abo.kogni.soile2.projecthandling.exceptions.ObjectDoesNotExist;
import fi.abo.kogni.soile2.projecthandling.projectElements.ElementManager;
import fi.abo.kogni.soile2.projecthandling.projectElements.Project;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.ProjectInstance;
import fi.abo.kogni.soile2.projecthandling.utils.ObjectGenerator;
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
	GitManager gitManager;
	public DBProjectInstance(GitManager gitManager, MongoClient client, String projectInstanceDB, EventBus eb) {
		super();
		this.gitManager = gitManager;
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
	 * This loader expects that the input json is a query for the mongo db of instances.
	 * It will then extract the remaining data first from the Project Instance database and then from the git repository.
	 */
	@Override
	public Future<JsonObject> load(JsonObject instanceInfo) {
		Promise<JsonObject> loadSuccess = Promise.<JsonObject>promise();				
		client.findOne(projectInstanceDB, instanceInfo, null).onSuccess(instanceJson -> {
			if(instanceJson == null)
			{
				loadSuccess.fail(new ObjectDoesNotExist(instanceID));
			}
			else
			{		
				GitFile proj = new GitFile("object.json",instanceJson.getString("sourceId"),instanceJson.getString("version")); 
				gitManager.getGitFileContentsAsJson(proj)
				.onSuccess(projectData -> 
				{
					// we got a positive reply.
					instanceJson.mergeIn(projectData);
					loadSuccess.complete(instanceJson);
				}
				).onFailure(fail -> {
					loadSuccess.fail(fail);
				});
			}
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
