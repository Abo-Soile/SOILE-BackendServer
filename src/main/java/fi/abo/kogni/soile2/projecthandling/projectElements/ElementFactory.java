package fi.abo.kogni.soile2.projecthandling.projectElements;



import java.util.function.Supplier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fi.abo.kogni.soile2.http_server.auth.SoileAuthorization.TargetElementType;
import fi.abo.kogni.soile2.projecthandling.exceptions.ElementNameExistException;
import fi.abo.kogni.soile2.projecthandling.exceptions.ObjectDoesNotExist;
import fi.abo.kogni.soile2.projecthandling.projectElements.impl.Task;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;

/**
 * A Factory to load Database elements from the mongo DB.
 * @author Thomas Pfau
 *
 * @param <T> The type of element this factory retrieves 
 */
public class ElementFactory<T extends ElementBase> {	
	
	Supplier<T> DBObjectSupplier;
	TargetElementType type;
	public static final Logger log = LogManager.getLogger(ElementFactory.class);

	public ElementFactory(Supplier<T> supplier)
	{		
		DBObjectSupplier = supplier;
		type = supplier.get().getElementType();
	}
	
	/**
	 * Generic generator (as in creates an element and saves it to the 
	 * @param client
	 * @param element
	 * @param type Is Nullable, mainly for tasks codeType.
	 * @return
	 */
	public Future<T> createElement(MongoClient client, String name, String type, String version)
	{		
		Promise<T> elementPromise = Promise.<T>promise();
		T element = DBObjectSupplier.get();
		// make sure no element exists with the exact same name.		
		client.findOne(element.getTargetCollection(), new JsonObject().put("name",name), null)
		.onSuccess(res -> {
			// if this is not null, we already have an element with the name
			if( res == null)
			{
				log.debug("No element with the given name existed. Creating a new one");
				// otherwise, we can set the name and save the element.
				element.setName(name);					
				element.save(client)						
				.onSuccess( id -> {					
					element.setUUID(id);					
					elementPromise.complete(element);
				})
				.onFailure(err -> {
					elementPromise.fail(err);
				});		
			}
			else
			{
				log.debug("Element with the name " + name + " existed. Failing");
				element.loadfromJson(res);
				elementPromise.fail(new ElementNameExistException(name, element.getUUID()));
			}
		}).onFailure(err -> {
			elementPromise.fail(err);
		});
		

		return elementPromise.future();
	}
	/**
	 * Generic generator (as in creates an element and saves it to the 
	 * @param client
	 * @param element
	 * @param type Is Nullable, mainly for tasks codeType.
	 * @return
	 */
	public Future<T> createElement(MongoClient client, String name)	
	{		
		if(type == TargetElementType.TASK)
		{
			return Future.failedFuture("Need a codeType to create a Task");
		}
		return createElement(client, name, null, null);
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
					if(currentElement != null)
					{
						
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
