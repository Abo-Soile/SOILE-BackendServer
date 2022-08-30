package fi.abo.kogni.soile2.project.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import fi.abo.kogni.soile2.project.instance.ProjectFactory;
import fi.abo.kogni.soile2.project.instance.ProjectInstance;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

public class TestProjectFactory implements ProjectFactory{

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
			return Future.<JsonObject>succeededFuture(this.toJson());
		}

		@Override
		public Future<JsonObject> load(JsonObject object) {			
			// directly pass on the object.
			return Future.succeededFuture(object);
		}

		@Override
		public Future<JsonObject> delete() {			
			return Future.succeededFuture(toJson());
		}		
	}
	
	public static JsonObject loadProjectData() throws IOException
	{
		JsonObject ProjectInstanceDef = new JsonObject(Files.readString(Paths.get(TestProjectFactory.class.getClassLoader().getResource("ProjectDefinition.json").getPath())));
		JsonObject projectGitDef = new JsonObject(Files.readString(Paths.get(TestProjectFactory.class.getClassLoader().getResource("GitProjObj.json").getPath())));
		projectGitDef.mergeIn(ProjectInstanceDef);
		return projectGitDef;

	}
}
