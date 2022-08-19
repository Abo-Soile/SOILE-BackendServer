package fi.abo.kogni.soile2.experiment.task;

import fi.abo.kogni.soile2.utils.DataRetriever;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;

public class TaskRetriever implements DataRetriever<VersionedTask, JsonObject>{

	@Override
	public Future<JsonObject> getElement(VersionedTask ID) {
		
		return null;
	}

	@Override
	public void getElement(VersionedTask ID, Handler<AsyncResult<JsonObject>> handler) {
		
	}

}
