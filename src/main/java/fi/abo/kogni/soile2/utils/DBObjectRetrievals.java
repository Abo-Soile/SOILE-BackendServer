package fi.abo.kogni.soile2.utils;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class DBObjectRetrievals {

	public static JsonObject getExperimentPermissionData()
	{
		return new JsonObject()
				   .put("name","")
				   .put("elements",new JsonArray())
				   .put("random", false)
				   .put("startDate", 0)
				   .put("endDate", 0)
				   .put("private", false);
	}
	
}
