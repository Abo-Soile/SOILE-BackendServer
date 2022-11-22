package fi.abo.kogni.soile2.projecthandling.projectElements;



import java.util.function.Supplier;

import fi.abo.kogni.soile2.projecthandling.exceptions.ObjectDoesNotExist;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;

public class ElementFactory<T extends ElementBase> {	
	
	Supplier<T> DBObjectSupplier;
	
	public ElementFactory(Supplier<T> supplier)
	{		
		DBObjectSupplier = supplier;
	}
	
	/**
	 * Generic generator (as in creates an element and saves it to the 
	 * @param client
	 * @param element
	 * @return
	 */
	public Future<T> createElement(MongoClient client)
	{		
		Promise<T> elementPromise = Promise.<T>promise();
		T element = DBObjectSupplier.get();
		element.save(client)						
		.onSuccess( id -> {
			System.out.println(" Generated ID is : " + id);
			element.setUUID(id);
			elementPromise.complete(element);
		})
		.onFailure(err -> {
			elementPromise.fail(err);
		});

		return elementPromise.future();
	}
	
	
	/**
	 * Load a project as specified by the UUID. This UUID is the mongoDB id. if no project could be loaded, the promise fails.
	 * @param client
	 * @param UUID
	 * @return
	 */
	public Future<T> loadElement(MongoClient client, String UUID)
	{
		Promise<T> projectPromise = Promise.<T>promise();
		T temp = DBObjectSupplier.get();		
		client.findOne(temp.getTargetCollection(), new JsonObject().put("_id", UUID), null)
		.onSuccess(currentElement ->
				{					
					System.out.println(UUID);
					if(currentElement != null)
					{
						System.out.println("Found an existing project");
						
						T element = DBObjectSupplier.get();
						element.loadfromJson(currentElement);
						projectPromise.complete(element);
					}
					else
					{
							projectPromise.fail(new ObjectDoesNotExist(UUID));
					}
				})
		.onFailure(err -> {
			projectPromise.fail(err);
		});		
		return projectPromise.future();

	}
}
