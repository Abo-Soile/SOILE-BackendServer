package fi.abo.kogni.soile2.projecthandling.projectElements;

import java.util.function.Supplier;

import fi.abo.kogni.soile2.datamanagement.git.GitFile;
import fi.abo.kogni.soile2.datamanagement.git.GitManager;
import fi.abo.kogni.soile2.projecthandling.apielements.APIElement;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;

public abstract class ElementManager<T extends ElementBase> {
	
	Supplier<T> supplier;
	ElementFactory<T> factory;
	MongoClient client;
	GitManager gitManager;
	public ElementManager(Supplier<T> supplier, ElementFactory<T> factory, MongoClient client, GitManager manager)
	{
		this.supplier = supplier;
		this.factory = factory;
		gitManager = manager;
	}
	
	public Future<T> createProject(String name)
	{
		Promise<T> elementPromise = Promise.<T>promise();
		// now we need to create a unique UUID. This should (normally) not cause any clashes, but lets be sure...
		factory.createElement(client)
		.onSuccess(element -> {
			element.setName(name);		
			gitManager.initRepo(element.getUUID())
			.onSuccess(initVersion -> 
			{
				//create an empty project file.
				JsonObject gitData = GitManager.buildBasicGitProject(name);
				gitManager.writeGitFile(new GitFile("Object.json", element.getUUID(), initVersion), gitData)
				.onSuccess(version -> {
					element.addVersion(version);			
					elementPromise.complete(element);
					// store the created project.				
				})
				.onFailure(err -> {
					elementPromise.fail(err);
				});
			})
			.onFailure(err -> 
			{
				elementPromise.fail(err);
			});
		}).onFailure(fail -> {
			elementPromise.fail(fail);
		});		
		return elementPromise.future();
	}

	public Future<String> updateElement(APIElement<T> newData)
	{
		Promise<String> elementPromise = Promise.<String>promise();
		GitFile currentVersion = new GitFile("Object.json", newData.getUUID(), newData.getVersion());
		// This will return an updated Element given the newData object, so we don't need to update the internals of the object
		// but can directly go on to write and save the data. 
		newData.getDBElement(client, factory).onSuccess(element -> 
		{
			// the gitJson can be directly derived from the API element.
			gitManager.writeGitFile(currentVersion, newData.getGitJson())
			.onSuccess(version -> {
				if(newData.hasAdditionalContent())
				{
					// this has additional data that we need to save in git.
					newData.storeAdditionalData(version, gitManager)
					.onSuccess(newVersion -> {
						element.addVersion(version);				
						element.save(client).onSuccess(res -> {
							elementPromise.complete(version);
						})
						.onFailure(fail -> elementPromise.fail(fail));
					})
					.onFailure(fail -> elementPromise.fail(fail));
				}
				else
				{
					element.addVersion(version);				
					element.save(client).onSuccess(res -> {
						elementPromise.complete(version);
					})
					.onFailure(fail -> elementPromise.fail(fail));
				}
			})
			.onFailure(fail -> elementPromise.fail(fail));
		})
		.onFailure(fail -> elementPromise.fail(fail));		
		return elementPromise.future();		
	
	}
	
}
