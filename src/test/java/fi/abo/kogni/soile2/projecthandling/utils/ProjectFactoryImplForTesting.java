package fi.abo.kogni.soile2.projecthandling.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import fi.abo.kogni.soile2.projecthandling.projectElements.instance.ProjectInstance;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.ProjectInstanceFactory;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class ProjectFactoryImplForTesting implements ProjectInstanceFactory{

	@Override
	public ProjectInstance createInstance() {
		// TODO Auto-generated method stub
		return new TestProject();
	}

	
	private class TestProject extends ProjectInstance
	{

		@Override
		public Future<JsonObject> save() {
			// do nothing;
			return Future.<JsonObject>succeededFuture(this.toDBJson());
		}

		@Override
		public Future<JsonObject> load(JsonObject id) {			
			Promise<JsonObject> projectPromise = Promise.promise();
			try
			{				
				projectPromise.complete(loadProjectData(id.getInteger("pos")));
			}
			catch(Exception e)
			{
				projectPromise.fail(e);
			}
			return projectPromise.future();
		}

		@Override
		public Future<JsonObject> delete() {			
			return Future.succeededFuture(toDBJson());
		}		
	}
	
	public static JsonObject loadProjectData() throws IOException
	{
		return loadProjectData(0);
	}
	
	public static JsonObject loadProjectData(int i) throws IOException
	{
		JsonArray ProjectInstanceDef = new JsonArray(Files.readString(Paths.get(ProjectFactoryImplForTesting.class.getClassLoader().getResource("ProjectDefinition.json").getPath())));
		JsonArray projectGitDef = new JsonArray(Files.readString(Paths.get(ProjectFactoryImplForTesting.class.getClassLoader().getResource("GitProjObj.json").getPath())));
		projectGitDef.getJsonObject(i).mergeIn(ProjectInstanceDef.getJsonObject(i));
		return projectGitDef.getJsonObject(i);

	}
}
