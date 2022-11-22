package fi.abo.kogni.soile2.projecthandling.apielements;

import fi.abo.kogni.soile2.projecthandling.projectElements.Project;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;

public class APIProject extends APIElementBase<Project>{

	
	private static String[] gitFields = new String[] {"name","tasks","experiments","filters","start"};
	private static Object[] gitDefaults = new Object[] {"",new JsonArray(),new JsonArray(),new JsonArray(),null};

	
	public APIProject(JsonObject data) {
		super(data);
		// TODO Auto-generated constructor stub
	}

		
	@Override
	public Future<Project> getDBElement(MongoClient client) {
		Promise<Project> projectPromise = Promise.<Project>promise();

		Project.loadProject(client, getUUID()).onSuccess(project -> 
		{
			setDefaultProperties(project);
			JsonArray currentTasks = data.getJsonArray("tasks",new JsonArray()); 
			for(int i = 0; i < currentTasks.size(); i++)
			{
				project.addElement(currentTasks.getJsonObject(i).getString("UUID"));
			}
			JsonArray currentExperiments = data.getJsonArray("experiments",new JsonArray()); 
			for(int i = 0; i < currentExperiments.size(); i++)
			{
				project.addElement(currentExperiments.getJsonObject(i).getString("UUID"));
			}
			projectPromise.complete(project);
			
		})
		.onFailure(fail -> projectPromise.fail(fail));
		return projectPromise.future();
	}



	@Override
	public JsonObject getGitJson() {
		JsonObject gitData = new JsonObject();
		for(int i = 0; i < gitFields.length ; ++i)
		{
			gitData.put(gitFields[i], data.getValue(gitFields[i], gitDefaults[i]));	
		}
		return gitData;
	}

	
}
