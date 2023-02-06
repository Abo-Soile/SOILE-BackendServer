package fi.abo.kogni.soile2.projecthandling.projectElements;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.function.Supplier;

import javax.naming.directory.InvalidAttributesException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fi.abo.kogni.soile2.datamanagement.git.GitFile;
import fi.abo.kogni.soile2.datamanagement.git.GitManager;
import fi.abo.kogni.soile2.http_server.auth.SoileAuthorization.TargetElementType;
import fi.abo.kogni.soile2.projecthandling.apielements.APIElement;
import fi.abo.kogni.soile2.projecthandling.apielements.APIExperiment;
import fi.abo.kogni.soile2.projecthandling.apielements.APIProject;
import fi.abo.kogni.soile2.projecthandling.apielements.APITask;
import fi.abo.kogni.soile2.projecthandling.exceptions.ElementNameExistException;
import fi.abo.kogni.soile2.projecthandling.exceptions.NoNameChangeException;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.FindOptions;
import io.vertx.ext.mongo.MongoClient;

/**
 * The element Manager handles DB elements and their link to the git Repositories.
 * TODO: We need to allow proper tagging. By default, the list of versions should only return tagged 
 * versions (i.e. versions with a tag), and only if actual versions are requested those should be returned with a date stamp.
 * There should be two different updates: Tagged updates and Automatic updates.   
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
	TargetElementType type;
	public static final Logger log = LogManager.getLogger(ElementManager.class);
	private static DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	public ElementManager(Supplier<T> supplier, Supplier<APIElement<T>> apisupplier,  MongoClient client, GitManager manager)
	{
		this.apisupplier = apisupplier;
		this.supplier = supplier;		
		typeID = supplier.get().getTypeID();
		type = supplier.get().getElementType();
		this.factory = new ElementFactory<T>(supplier);
		this.client = client;
		gitManager = manager;
	}
	
	public String getGitIDForUUID(String uuid)
	{
		return typeID + uuid;
	}
		
	/**
	 * Create a new element.
	 * This future can fail with an {@link ElementNameExistException} with id = name, which indicates that an element with this name already exists.
	 * @param name
	 * @return
	 */
	public Future<T> createElement(String name, String type, String languageversion)
	{
		Promise<T> elementPromise = Promise.<T>promise();
		// now we need to create a unique UUID. This should (normally) not cause any clashes, but lets be sure...
		factory.createElement(client, name, type, languageversion)
		.onSuccess(element -> {
			element.setName(name);	
			log.debug(element.toJson().encodePrettily());
			gitManager.initRepo(getGitIDForUUID(element.getUUID()))
			.onSuccess(initVersion -> 
			{
				//create an empty project file.
				JsonObject gitData = GitManager.buildBasicGitElement(name, this.type);
				gitManager.writeGitFile(new GitFile("Object.json", getGitIDForUUID(element.getUUID()), initVersion), gitData)
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
	 * Create a new element.
	 * This future can fail with an {@link ElementNameExistException} with id = name, which indicates that an element with this name already exists.
	 * @param name
	 * @return
	 */
	public Future<T> createElement(String name)
	{
		if(type == TargetElementType.TASK)
		{
			return Future.failedFuture("Need a codeType to create a Task");
		}
		return createElement(name, null, null);
	}
	
	/**
	 * Load or Create an element. This should not normally be called but might be necessary for some tests. 
	 * @param name
	 * @return
	 */
	public Future<T> createOrLoadElement(String name, String type, String version)
	{
		Promise<T> elementPromise = Promise.<T>promise();
		// We will try to create the element with this name but if it already exists, return the element with the name.
		createElement(name, type, version)
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
	/**
	 * Create or load an Element
	 * This future can fail with an {@link ElementNameExistException} with id = name, which indicates that an element with this name already exists.
	 * @param name The name of the element
	 * @return
	 */
	public Future<T> createOrLoadElement(String name)
	{
		if(type == TargetElementType.TASK)
		{
			return Future.failedFuture("Need a codeType to create a Task");
		}
		// need to
		return createOrLoadElement(name, null, null);
	}
	
	public Future<JsonObject> getGitJson(String elementID, String elementVersion)
	{
		GitFile target = new GitFile("Object.json", getGitIDForUUID(elementID), elementVersion);
		log.debug("Requesting Data for Repo: "  + getGitIDForUUID(elementID));
		return gitManager.getGitFileContentsAsJson(target);
	}
	
	/**
	 * Get the Element stored in the Database with the given ID.  
	 * @param elementID the ID of the element.
	 * @return a Future of the element managed by this Manager.
	 */
	public Future<T> getElement(String elementID)
	{		
		return factory.loadElement(client, elementID);
	}
	
	/**
	 * Update the given element in the database and on git using the Data from the provided API element
	 * @param newData
	 * @return
	 */
	public Future<String> updateElement(APIElement<T> newData)
	{
		Promise<String> elementPromise = Promise.promise();
		GitFile currentVersion = new GitFile("Object.json", getGitIDForUUID(newData.getUUID()), newData.getVersion());
		// This will return an updated Element given the newData object, so we don't need to update the internals of the object
		// but can directly go on to write and save the data. 
		newData.getDBElement(client, factory).onSuccess(element -> 
		{
			if(!element.getName().equals(newData.getName()))
			{
				System.out.println("Failing updateElement");
				elementPromise.fail(new NoNameChangeException());
				return;
			}
			// the gitJson can be directly derived from the API element.					
			gitManager.writeGitFile(currentVersion, newData.getGitJson())
			.onSuccess(version -> {
				if(newData.hasAdditionalGitContent())
				{
					// this has additional data that we need to save in git.
					newData.storeAdditionalData(version, gitManager, getGitIDForUUID(newData.getUUID()))
					.onSuccess(newVersion -> {
						element.addVersion(newVersion);				
						element.save(client).onSuccess(res -> {
							elementPromise.complete(newVersion);
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
	public Future<JsonArray> getElementList(JsonArray permissions)
	{
		Promise<JsonArray> listPromise = Promise.<JsonArray>promise();		

		Element e = supplier.get();
		JsonArray result = new JsonArray();		
		// should possibly be done via findBatch
		client.findWithOptions(e.getTargetCollection(), new JsonObject(), new FindOptions().setFields(new JsonObject().put("private",1).put("visible", 1).put("name", 1).put("_id", 1)))
		.onSuccess(res -> {
			for(JsonObject current : res)
			{
				boolean addElement = true;
				log.debug("Retrieved element: \n " + current.encodePrettily());
				if(current.getBoolean("private", false))
				{
					if(!permissions.contains(current.getString("uuid")))
					{
						addElement = false;
					}
				}			
				if(!current.getBoolean("visible",true))
				{
					// this was deleted, so no longer retrievable. 
					addElement = false;
				}
				if(addElement)
				{
					// rename the _id key to uuid.					
					current.put("uuid",current.getString("_id")).remove("_id");
					current.remove("private");
					current.remove("visible");
					result.add(current);
				}
			}
			listPromise.complete(result);
		})
		.onFailure(err -> {
			listPromise.fail(err);
		});
		return listPromise.future();		
	}
	
	/**
	 * Get the list of all tags for the given element.  
	 * Returns a list of 
	 * @param id The id of the element in question
	 * @return
	 */
	public Future<JsonArray> getTagListForElement(String id)
	{
		Promise<JsonArray> listPromise = Promise.<JsonArray>promise();		

		Element e = supplier.get();
		JsonArray result = new JsonArray();		
		// should possibly be done via findBatch
		client.findOne(e.getTargetCollection(), new JsonObject().put("_id", id), new JsonObject().put("tags", 1).put("versions", 1))
		.onSuccess(res -> {			
			HashMap<String,Date> versionMap = createVersionHashMap(res.getJsonArray("versions"));
			JsonArray tagArray = res.getJsonArray("tags"); 
			for(int i = 0; i < tagArray.size(); i++)
			{
				JsonObject current = tagArray.getJsonObject(i);								
				current.put("date", dateFormatter.format(versionMap.get(current.getString("version"))));
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
	 * Get the list of all versions for the given element.  
	 * Returns a list of 
	 * @param id The id of the element in question
	 * @return
	 */
	public Future<JsonArray> getVersionListForElement(String id)
	{
		Promise<JsonArray> listPromise = Promise.<JsonArray>promise();		

		Element e = supplier.get();
		JsonArray result = new JsonArray();		
		// should possibly be done via findBatch
		client.findOne(e.getTargetCollection(), new JsonObject().put("_id", id), new JsonObject().put("versions", 1))
		.onSuccess(res -> {						
			JsonArray versionArray = res.getJsonArray("versions"); 
			for(int i = 0; i < versionArray.size(); i++)
			{	
				JsonObject currentVersion = versionArray.getJsonObject(i);
				
				result.add(new JsonObject()
							   .put("version", currentVersion.getString("version"))
							   .put("date", dateFormatter.format(new Date(currentVersion.getLong("timestamp"))))						
						);				
			}
			listPromise.complete(result);
		})
		.onFailure(err -> {
			listPromise.fail(err);
		});
		return listPromise.future();		
	}
	
	private HashMap<String,Date> createVersionHashMap(JsonArray versions)
	{
		HashMap<String,Date> versionMap = new HashMap<>();
		for(int i = 0 ; i < versions.size(); i++)
		{
			JsonObject currentVersion = versions.getJsonObject(i);
			versionMap.put(currentVersion.getString("version"), new Date(currentVersion.getLong("timestamp")));
		}
		return versionMap;
	}
	
	
	/**
	 * Get an API that can be returned based on the given UUID and version.
	 * @param uuid the uuid of the element to be returned
	 * @param version the version of the element to be returned
	 * @return an API object of the type appropriate for this Manager.
	 */
	public Future<APIElement<T>> getAPIElementFromDB(String uuid, String version)
	{
		Promise<APIElement<T>> elementPromise = Promise.<APIElement<T>>promise();
		APIElement<T> apiElement = apisupplier.get();
		GitFile currentVersion = new GitFile("Object.json", getGitIDForUUID(uuid), version);

		factory.loadElement(client, uuid).
		onSuccess(element -> {
			apiElement.loadFromDBElement(element);
			// the version cannot be extracted from the db element, as the db element stores all versions, and this is a specific request. 
			apiElement.setVersion(version);
			apiElement.loadAdditionalData(gitManager, getGitIDForUUID(apiElement.getUUID()))
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

	public Supplier<T> getElementSupplier()
	{
		return supplier;
	}
	/**
	 * Build an API element based on the given Json Object.
	 * @param apiJson The json representing the API object. 
	 * @return an APIObject representing the given json
	 */
	public Future<APIElement<T>> getAPIElementFromJson(JsonObject json)
	{
		Promise<APIElement<T>> elementPromise = Promise.<APIElement<T>>promise();
		APIElement<T> apiElement = apisupplier.get();		
		apiElement.loadFromJson(json);
		elementPromise.complete(apiElement);
		return elementPromise.future();			
	}
	
	public static ElementManager<Project> getProjectManager(MongoClient client, GitManager gm)
	{
		return new ElementManager<Project>(Project::new,APIProject::new, client, gm);
	}
	
	public static ElementManager<Experiment> getExperimentManager(MongoClient client, GitManager gm)
	{
		return new ElementManager<Experiment>(Experiment::new,APIExperiment::new, client, gm);
	}
	
	public static ElementManager<Task> getTaskManager(MongoClient client, GitManager gm)
	{
		return new ElementManager<Task>(Task::new,APITask::new, client, gm);
	}	
}
