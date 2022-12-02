package fi.abo.kogni.soile2.projecthandling.projectElements;

import java.util.function.Supplier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fi.abo.kogni.soile2.datamanagement.git.GitFile;
import fi.abo.kogni.soile2.datamanagement.git.GitManager;
import fi.abo.kogni.soile2.projecthandling.apielements.APIElement;
import fi.abo.kogni.soile2.projecthandling.exceptions.ElementNameExistException;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.ElementInstanceBase;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.FindOptions;
import io.vertx.ext.mongo.MongoClient;



/**
 * The element Manager combines 
 * @author Thomas Pfau
 *
 * @param <T>
 */
public class ElementManager<T extends ElementBase> {
	
	Supplier<T> supplier;
	Supplier<APIElement<T>> apisupplier;
	ElementFactory<T> factory;
	MongoClient client;
	GitManager gitManager;
	String typeID;
	public static final Logger log = LogManager.getLogger(ElementManager.class);

	public ElementManager(Supplier<T> supplier, Supplier<APIElement<T>> apisupplier,  MongoClient client, GitManager manager)
	{
		this.apisupplier = apisupplier;
		this.supplier = supplier;
		typeID = supplier.get().getTypeID();
		this.factory = new ElementFactory<T>(supplier);
		this.client = client;
		gitManager = manager;
	}
	
	private String getGitID(String uuid)
	{
		return typeID + uuid;
	}
		
	/**
	 * Create a new element.
	 * This future can fail with an {@link ElementNameExistException} with id = name, which indicates that an element with this name already exists.
	 * @param name
	 * @return
	 */
	public Future<T> createElement(String name)
	{
		Promise<T> elementPromise = Promise.<T>promise();
		// now we need to create a unique UUID. This should (normally) not cause any clashes, but lets be sure...
		factory.createElement(client, name)
		.onSuccess(element -> {
			element.setName(name);	
			log.debug(element.toJson().encodePrettily());
			gitManager.initRepo(getGitID(element.getUUID()))
			.onSuccess(initVersion -> 
			{
				//create an empty project file.
				JsonObject gitData = GitManager.buildBasicGitElement(name, element.getClass());
				gitManager.writeGitFile(new GitFile("Object.json", getGitID(element.getUUID()), initVersion), gitData)
				.onSuccess(version -> {
					log.debug("Created a new element with name: " + name);
					element.addVersion(version);
					log.debug("and data: " + element.toJson().encodePrettily());
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

	/**
	 * Load or Create an element. This should not normally be called but might be necessary for some tests. 
	 * @param name
	 * @return
	 */
	public Future<T> createOrLoadElement(String name)
	{
		Promise<T> elementPromise = Promise.<T>promise();
		// We will try to create the element with this name but if it already exists, return the element with the name.
		createElement(name)
		.onSuccess(element -> 
		{
			elementPromise.complete(element);
		})
		.onFailure(fail -> {				
			if(fail instanceof ElementNameExistException)
			{
				ElementNameExistException e = (ElementNameExistException) fail; 
				log.debug("Got a request for element with name: " + name + " but this already existed, so we load it.");
				// so, it actually already exists. In this instance we will just load the element.
				factory.loadElement(client, e.getExistingElementUUID())
				.onSuccess(loadedelement -> {
					elementPromise.complete(loadedelement);
				})
				.onFailure(err -> elementPromise.fail(err));
			}
			else
			{
				elementPromise.fail(fail);
			}
		});		
		return elementPromise.future();
	}
	
	
	public Future<String> updateElement(APIElement<T> newData)
	{
		Promise<String> elementPromise = Promise.<String>promise();
		GitFile currentVersion = new GitFile("Object.json", getGitID(newData.getUUID()), newData.getVersion());
		// This will return an updated Element given the newData object, so we don't need to update the internals of the object
		// but can directly go on to write and save the data. 
		newData.getDBElement(client, factory).onSuccess(element -> 
		{
			// the gitJson can be directly derived from the API element.
			gitManager.writeGitFile(currentVersion, newData.getGitJson())
			.onSuccess(version -> {
				if(newData.hasAdditionalGitContent())
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
	
	public Future<Boolean> deleteElement(APIElement<T> newData)
	{
		// well, we want to delete the Object. This is final 		
		Promise<Boolean> deletionPromise = Promise.<Boolean>promise();		
		// This will return an updated Element given the newData object, so we don't need to update the internals of the object
		// but can directly go on to write and save the data. 
		newData.getDBElement(client, factory).onSuccess(element -> 
		{
			element.setVisible(false);
			element.save(client).onSuccess(Void -> {
				deletionPromise.complete(true);
			})			
			.onFailure(fail -> deletionPromise.fail(fail));
		})
		.onFailure(fail -> deletionPromise.fail(fail));		
		return deletionPromise.future();			
	}
	
	/**
	 * Get the List of all elements of the specified type. 
	 * Returns a list of 
	 * @return
	 */
	public Future<JsonArray> getElementList()
	{
		Promise<JsonArray> listPromise = Promise.<JsonArray>promise();		

		Element e = supplier.get();
		JsonArray result = new JsonArray();		
		// should possibly be done via findBatch
		client.findWithOptions(e.getTargetCollection(), new JsonObject(), new FindOptions().setFields(new JsonObject().put("name", 1).put("_id", 1)))
		.onSuccess(res -> {
			for(JsonObject current : res)
			{
				// rename the _id key to uuid.
				current.put("UUID",current.getString("_id")).remove("_id");
				result.add(current);
			}
			listPromise.complete(result);
		})
		.onFailure(err -> {
			listPromise.fail(err);
		});
		return listPromise.future();		
	}
	
	/**
	 * Get an API that can be returned based on the given UUID and version.
	 * @param uuid the uuid of the element to be returned
	 * @param version the version of the element to be returned
	 * @return an API object of the type appropriate for this Manager.
	 */
	public Future<APIElement<T>> getAPIElement(String uuid, String version)
	{
		Promise<APIElement<T>> elementPromise = Promise.<APIElement<T>>promise();
		APIElement<T> apiElement = apisupplier.get();
		GitFile currentVersion = new GitFile("Object.json", uuid, version);

		factory.loadElement(client, uuid).
		onSuccess(element -> {
			apiElement.loadFromDBElement(element);
			// the version cannot be extracted from the db element, as the db element stores all versions, and this is a specific request. 
			apiElement.setVersion(version);
			apiElement.loadAdditionalData(gitManager)
			.onSuccess(Void -> {
				gitManager.getGitFileContentsAsJson(currentVersion)
				.onSuccess(gitJson -> {
					apiElement.loadGitJson(gitJson);
					elementPromise.complete(apiElement);
				})
				.onFailure(err -> {
					elementPromise.fail(err);
				});
			})
			.onFailure(err -> {
				elementPromise.fail(err);
			});
		})
		.onFailure(err -> {
			elementPromise.fail(err);
		});
		return elementPromise.future();		
	
	}
	
}
