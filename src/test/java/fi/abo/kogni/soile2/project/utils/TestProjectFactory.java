package fi.abo.kogni.soile2.project.utils;

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
		public Future<Void> save() {
			// do nothing;
			return Future.succeededFuture();
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
}
