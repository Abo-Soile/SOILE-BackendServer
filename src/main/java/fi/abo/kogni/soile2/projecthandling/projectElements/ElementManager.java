package fi.abo.kogni.soile2.projecthandling.projectElements;

import fi.abo.kogni.soile2.datamanagement.git.GitFile;
import fi.abo.kogni.soile2.datamanagement.git.GitManager;
import fi.abo.kogni.soile2.projecthandling.apielements.APIProject;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;

public abstract class ElementManager<T extends ElementBase> {

	public Future<T> createProject(String name)
	{
		Promise<T> projectPromise = Promise.<T>promise();
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

	public Future<String> updateProject(APIProject newData)
	{
		Promise<String> projectPromise = Promise.<String>promise();
		GitFile currentVersion = new GitFile("Object.json", newData.getUUID(), newData.getVersion());
		newData.getDBElement(client).onSuccess(project -> 
		{

			gitManager.writeGitFile(currentVersion, newData.getGitJson())
			.onSuccess(version -> {
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
