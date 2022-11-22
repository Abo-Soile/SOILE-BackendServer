package fi.abo.kogni.soile2.projecthandling.projectElements;

import fi.abo.kogni.soile2.datamanagement.git.GitFile;
import fi.abo.kogni.soile2.datamanagement.git.GitManager;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;

public class TaskManager {

	
	MongoClient client;
	GitManager gitManager;

	public Future<Task> createTask(String name)
	{
		Promise<Project> projectPromise = Promise.<Project>promise();
		// now we need to create a unique UUID. This should (normally) not cause any clashes, but lets be sure...
		Project.createProject(client)
		.onSuccess(project -> {
			project.setName(name);		
			gitManager.initRepo(project.getUUID())
			.onSuccess(initVersion -> 
			{
				//create an empty project file.
				JsonObject gitData = GitManager.buildBasicGitProject(name);
				gitManager.writeGitFile(new GitFile("Object.json", project.getUUID(), initVersion), gitData)
				.onSuccess(version -> {
					project.addVersion(version);			
					projectPromise.complete(project);
					// store the created project.				
				})
				.onFailure(err -> {
					projectPromise.fail(err);
				});
			})
			.onFailure(err -> 
			{
				projectPromise.fail(err);
			});
		}).onFailure(fail -> {
			projectPromise.fail(fail);
		});		
		return projectPromise.future();
	}

	public Future<String> updateTask(JsonObject newData)
	{
		Promise<String> projectPromise = Promise.<String>promise();
		GitFile currentVersion = new GitFile("Object.json", newData.getString("UUID"), newData.getString("version"));
		Project.loadProject(client, newData.getString("UUID")).onSuccess( project ->		
		{
			JsonObject gitData = new JsonObject();
			for(int i = 0; i < gitFields.length ; ++i)
			{
				gitData.put(gitFields[i], newData.getValue(gitFields[i]));	
			}			
			gitManager.writeGitFile(currentVersion, gitData).onSuccess(version -> {
				project.addVersion(version);
				project.save(client).onSuccess(res -> {
					projectPromise.complete(version);
				})
				.onFailure(fail -> projectPromise.fail(fail));
			})
			.onFailure(fail -> projectPromise.fail(fail));
		})
		.onFailure(fail -> projectPromise.fail(fail));
		return projectPromise.future();
	}
}
