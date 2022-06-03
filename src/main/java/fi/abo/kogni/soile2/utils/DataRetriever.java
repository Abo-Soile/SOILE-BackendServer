package fi.abo.kogni.soile2.utils;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;

/**
 * A Retriever that obtains individual Objects from a given collection.
 * @author Thomas Pfau
 *
 */
public class DataRetriever {
	
	MongoClient client;
	String collection;
	String property;
	
	public DataRetriever(MongoClient client, String collection, String targetProperty)
	{
		this.client = client;
		this.collection = collection;
		this.property = targetProperty;
	}
	
	public Future<JsonObject> getElement(String UUID)
	{
		return client.findOne(collection, new JsonObject().put(property, UUID),null);
	}

	public Future<JsonObject> getElementFields(String UUID, JsonObject Fields)
	{
		return client.findOne(collection, new JsonObject().put(property, UUID),Fields);
	}

	public void getElement(String UUID, Handler<AsyncResult<JsonObject>> handler)
	{
		client.findOne(collection, new JsonObject().put(property, UUID),null, handler);
	}

	public void getElementFields(String UUID, JsonObject Fields, Handler<AsyncResult<JsonObject>> handler)
	{
		client.findOne(collection, new JsonObject().put(property, UUID), Fields, handler);
	}

}
