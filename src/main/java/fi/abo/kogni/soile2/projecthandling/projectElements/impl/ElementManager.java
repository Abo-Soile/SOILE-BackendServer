package fi.abo.kogni.soile2.projecthandling.projectElements.impl;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;

import org.apache.commons.lang3.function.Failable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fi.aalto.scicomp.gitFs.gitProviderVerticle;
import fi.abo.kogni.soile2.datamanagement.datalake.DataLakeFile;
import fi.abo.kogni.soile2.datamanagement.datalake.DataLakeResourceManager;
import fi.abo.kogni.soile2.datamanagement.git.GitElement;
import fi.abo.kogni.soile2.datamanagement.git.GitFile;
import fi.abo.kogni.soile2.datamanagement.git.GitManager;
import fi.abo.kogni.soile2.http_server.auth.SoileAuthorization.TargetElementType;
import fi.abo.kogni.soile2.http_server.requestHandling.SOILEUpload;
import fi.abo.kogni.soile2.projecthandling.apielements.APIElement;
import fi.abo.kogni.soile2.projecthandling.apielements.APIExperiment;
import fi.abo.kogni.soile2.projecthandling.apielements.APIProject;
import fi.abo.kogni.soile2.projecthandling.apielements.APITask;
import fi.abo.kogni.soile2.projecthandling.exceptions.ElementNameExistException;
import fi.abo.kogni.soile2.projecthandling.exceptions.NoNameChangeException;
import fi.abo.kogni.soile2.projecthandling.exceptions.ObjectDoesNotExist;
import fi.abo.kogni.soile2.projecthandling.projectElements.Element;
import fi.abo.kogni.soile2.projecthandling.projectElements.ElementBase;
import fi.abo.kogni.soile2.projecthandling.projectElements.ElementDataHandler;
import fi.abo.kogni.soile2.projecthandling.projectElements.ElementFactory;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.FindOptions;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.mongo.UpdateOptions;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.handler.HttpException;

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
	EventBus eb;
	String typeID;
	TargetElementType type;
	ElementDataHandler<T> dataHandler;
	public static final Logger LOGGER = LogManager.getLogger(ElementManager.class);
	//private static DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss SS");

	public ElementManager(Supplier<T> supplier, Supplier<APIElement<T>> apisupplier,  MongoClient client, Vertx vertx)
	{
		this(supplier, apisupplier, client, vertx, new ElementDataHandler<T>(new DataLakeResourceManager(vertx), supplier));		
	}

	public ElementManager(Supplier<T> supplier, Supplier<APIElement<T>> apisupplier,  MongoClient client, Vertx vertx, ElementDataHandler<T> handler)
	{
		this.apisupplier = apisupplier;
		this.supplier = supplier;		
		typeID = supplier.get().getTypeID();
		type = supplier.get().getElementType();
		this.factory = new ElementFactory<T>(supplier);
		this.client = client;
		this.eb = vertx.eventBus();
		this.dataHandler = handler;
	}

	/**
	 * Get the ID of the repository for this type of Element.
	 * @param UUID the UUID of the element
	 * @return the Supplemented ID of the element representing the repository ID
	 */
	public String getGitIDForUUID(String UUID)
	{
		return typeID + UUID;
	}

	/**
	 * Clean up any caches used by this manager.
	 */
	public void cleanUp()
	{
		dataHandler.cleanUp();
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
			LOGGER.debug(element.toJson().encodePrettily());
			eb.request("soile.git.initRepo",getGitIDForUUID(element.getUUID()))
			.onSuccess(reply -> 
			{
				String initVersion = (String) reply.body();
				//create an empty project file.
				JsonObject gitData = GitManager.buildBasicGitElement(name, this.type);
				if(gitData.containsKey("codeType"))
				{
					// this is a task.
					gitData.put("codeType", new JsonObject().put("language", type).put("version", languageversion));
				}
				eb.request("soile.git.writeGitFile", new GitFile("Object.json", getGitIDForUUID(element.getUUID()), initVersion).toJson().put("data",gitData))
				.onSuccess(versionreply -> {
					String version = (String) versionreply.body();
					LOGGER.debug("Created a new element with name: " + name);
					element.addVersion(version);
					LOGGER.debug("and data: " + element.toJson().encodePrettily());
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
				LOGGER.debug("Got a request for element with name: " + name + " but this already existed, so we load it.");
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

	/**
	 * Get the Git Json for the given element at the given ID. This is the Git Object of the element.
	 * @param elementID the elementID
	 * @param elementVersion the version to retrieve
	 * @return A {@link Future} of the {@link JsonObject} that was stored in git. 
	 */
	public Future<JsonObject> getGitJson(String elementID, String elementVersion)
	{
		GitFile target = new GitFile("Object.json", getGitIDForUUID(elementID), elementVersion);
		LOGGER.debug("Requesting Data for Repo: "  + getGitIDForUUID(elementID));
		Promise<JsonObject> elementPromise = Promise.promise();
		eb.request("soile.git.getGitFileContentsAsJson",target.toJson())
		.onSuccess(res -> {
			elementPromise.complete((JsonObject) res.body());
		})
		.onFailure(err -> elementPromise.fail(err));		
		return elementPromise.future();		
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
		return updateElement(newData, null);
	}

	/**
	 * Update the given element in the database and on git using the Data from the provided API element
	 * @param newData
	 * @return
	 */
	public Future<String> updateElement(APIElement<T> newData, String tag)
	{		
		Promise<String> elementPromise = Promise.promise();
		GitFile currentVersion = new GitFile("Object.json", getGitIDForUUID(newData.getUUID()), newData.getVersion());		
		// This will return an updated Element given the newData object, so we don't need to update the internals of the object
		// but can directly go on to write and save the data. 
		newData.getDBElement(client, factory).onSuccess(element -> 
		{
			if(!element.getName().equals(newData.getName()))
			{
				elementPromise.fail(new NoNameChangeException());
				return;
			}
			JsonObject requestObject = currentVersion.toJson();
			if(tag != null)
			{
				requestObject.put("tag", tag);				
			}
			// the gitJson can be directly derived from the API element.
			eb.request("soile.git.writeGitFile", requestObject.put("data", newData.getGitJson()))
			.onSuccess(reply -> {
				String version = (String) reply.body();
				if(newData.hasAdditionalGitContent())
				{
					// this has additional data that we need to save in git.
					newData.storeAdditionalData(version, eb, getGitIDForUUID(newData.getUUID()))
					.onSuccess(newVersion -> {
						element.addVersion(newVersion);
						if( tag != null)
						{
							element.addTag(tag, newVersion);
						}
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
					if( tag != null)
					{
						element.addTag(tag, version);
					}
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

	/**
	 * Delete the given element. This does NOT actually delete the element, but makes it invisible, so it can still be used but it can no longer be modified or updated. 
	 * Elements that contain it will still be valid, but the element can no longer be updated. 
	 * @param newData
	 * @return
	 */
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
		client.findWithOptions(e.getTargetCollection(), new JsonObject(), new FindOptions().setFields(new JsonObject().put("private",1).put("visible", 1).put("name", 1).put("_id", 1)))
		.onSuccess(res -> {
			for(JsonObject current : res)
			{
				boolean addElement = true;
				LOGGER.debug("Retrieved element: \n " + current.encodePrettily());
				if(!current.getBoolean("visible",true))
				{
					// this was deleted, so no longer retrievable. 
					addElement = false;
				}
				if(addElement)
				{
					// rename the _id key to UUID.					
					current.put("UUID",current.getString("_id")).remove("_id");
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
				LOGGER.debug("Retrieved element: \n " + current.encodePrettily());
				if(current.getBoolean("private", false))
				{					
					if(permissions != null && !permissions.contains(current.getString("_id")))
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
					// rename the _id key to UUID.					
					current.put("UUID",current.getString("_id")).remove("_id");
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
				current.put("date", versionMap.get(current.getString("version")).getTime());
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
	 * Get the Tag of a specific version of an element, if it exists (null otherwise).  
	 * @param id The id of the element in question
	 * @param version The version of the element
	 * @return
	 */
	public Future<String> getTagForElementVersion(String id, String version)
	{
		Promise<String> tagPromise = Promise.<String>promise();		

		Element e = supplier.get();				
		client.findOne(e.getTargetCollection(), new JsonObject().put("_id", id).put("versions", new JsonObject().put("$elemMatch", new JsonObject().put("version", version))), new JsonObject().put("tags", 1).put("versions", 1))
		.onSuccess(res -> {		
			if(res != null)
			{
				JsonArray tagArray = res.getJsonArray("tags");			
				String tag = null;
				for( int i = 0; i < tagArray.size(); i++ )
				{
					if(tagArray.getJsonObject(i).getString("version").equals(version))
					{
						tag = tagArray.getJsonObject(i).getString("tag");
						break;
					}				
				}
				if(tag != null)
				{
					tagPromise.complete(tag);
				}
				else
				{
					tagPromise.fail(new HttpException(404, "No Tag Found for this version"));
				}
			}
			else
			{
				tagPromise.fail(new ObjectDoesNotExist(version));
			}
		})
		.onFailure(err -> {
			tagPromise.fail(err);
		});
		return tagPromise.future();		
	}

	/**
	 * Remove the tags (not the versions just the tag associations) from the element.
	 * @param elementID the elementID for which to remove versions
	 * @param tagsToRemove the tags to remove from the element.
	 * @return A successful {@link Future} if the tags were removed.
	 */
	public Future<Void> removeTagsFromElement(String elementID, JsonArray tagsToRemove)
	{
		Promise<Void> tagPromise = Promise.<Void>promise();		
		Element e = supplier.get();				
		client.findOne(e.getTargetCollection(), new JsonObject().put("_id", elementID),new JsonObject().put("tags", 1))		
		.onSuccess(res -> {
			JsonArray versionList = new JsonArray();
			JsonArray tags = res.getJsonArray("tags");
			for(int i = 0; i < tags.size(); ++i)
			{
				if(tagsToRemove.contains(tags.getJsonObject(i).getString("tag")))
				{
					versionList.add(tags.getJsonObject(i).getString("version"));
				}
			}
			JsonObject update = new JsonObject().put("$pull",
					new JsonObject().put("tags",
							new JsonObject().put("tag", 
									new JsonObject().put("$in", tagsToRemove))));
			update.put("$set", new JsonObject().put("versions.$[elem].canbetagged", true));
			UpdateOptions opts = new UpdateOptions().setArrayFilters(new JsonArray().add(new JsonObject().put("elem.version", new JsonObject().put("$in", versionList))));
			client.updateCollectionWithOptions(e.getTargetCollection(), new JsonObject().put("_id", elementID), update, opts)
			.onSuccess(res2 -> {					
				LOGGER.debug("Successfully removed " + tagsToRemove.encodePrettily());
				tagPromise.complete();
			})
			.onFailure(err -> {
				tagPromise.fail(err);
			});
			
		}).onFailure(err -> {
			tagPromise.fail(err);
		});
				
		/*client.updateCollection(e.getTargetCollection(), new JsonObject().put("_id", elementID), update)
		.onSuccess(res -> {					
			LOGGER.debug("Successfully removed " + tagsToRemove.encodePrettily());
			tagPromise.complete();
		})
		.onFailure(err -> {
			tagPromise.fail(err);
		});*/
		return tagPromise.future();	
	}

	/**
	 * Add Tag to a specific version of the element
	 * @param elementID the elementID for which to remove versions
	 * @param version the version to add the tag to.
	 * @param tagsToRemove the tags to remove from the element.
	 * @return A successful {@link Future} if the tags were removed.
	 */
	public Future<Void> addTagToVersion(String elementID, String version, String tag)
	{
		Promise<Void> tagPromise = Promise.<Void>promise();		
		Element e = supplier.get();
		client.findOne(e.getTargetCollection(), new JsonObject().put("_id", elementID), new JsonObject().put("tags", 1).put("versions", 1))
		.onSuccess(element -> {
			JsonArray versionArray = element.getJsonArray("versions");
			// We need to check, whether the version exists
			boolean versionFound = false; 
			for(int i = 0; i < versionArray.size(); i++)
			{
				if(versionArray.getJsonObject(i).getString("version").equals(version))
				{
					versionFound = true;
				}
			}
			if(!versionFound)
			{
				tagPromise.fail(new ObjectDoesNotExist(elementID +  " at version " + version));
				return;
			}
			// check that the version doesn't already have a tag
			JsonArray tagArray = element.getJsonArray("tags");
			for(int i = 0; i < tagArray.size(); i++)
			{
				if(tagArray.getJsonObject(i).getString("version").equals(version))
				{
					tagPromise.fail(new HttpException(409,"A Tag for this version already exists: " + tagArray.getJsonObject(i).getString("tag")));
					return;
				}
				if(tagArray.getJsonObject(i).getString("tag").equals(tag))
				{
					tagPromise.fail(new HttpException(409,"Tag already exists for version: " + tagArray.getJsonObject(i).getString("version")));
					return;
				}
			}
			// ok, no tag exists, and the version exists so we will no put in a tag for this version into the db.
			JsonObject update = new JsonObject().put("$push",
					new JsonObject().put("tags",
							new JsonObject().put("tag",tag).put("version", version))); 									
			client.updateCollection(e.getTargetCollection(), new JsonObject().put("_id", elementID), update)
			.onSuccess(res -> {						
				tagPromise.complete();
			})
			.onFailure(err -> {
				tagPromise.fail(err);
			});
		})
		.onFailure(err -> {
			tagPromise.fail(err);
		});

		return tagPromise.future();	
	}


	/**
	 * Get the list of all versions for the given element.  
	 * Returns a list of all versions of the element with the given ID
	 * @param id The id of the element in question
	 * @return
	 */
	public Future<JsonArray> getVersionListForElement(String id)
	{
		Promise<JsonArray> listPromise = Promise.<JsonArray>promise();		

		Element e = supplier.get();
		JsonArray result = new JsonArray();		
		// should possibly be done via findBatch
		client.findOne(e.getTargetCollection(), new JsonObject().put("_id", id), new JsonObject().put("versions", 1).put("tags", 1))
		.onSuccess(res -> {
			if( res == null)
			{
				listPromise.fail(new ObjectDoesNotExist(id));
				return;
			}
			JsonArray versionArray = res.getJsonArray("versions");
			JsonArray tagArray = res.getJsonArray("tags");
			HashMap<String, String> tagMap = new HashMap<>();
			for( int i = 0; i < tagArray.size(); i++ )
			{
				tagMap.put(tagArray.getJsonObject(i).getString("version"),tagArray.getJsonObject(i).getString("tag"));
			}
			for(int i = 0; i < versionArray.size(); i++)
			{	
				JsonObject currentVersion = versionArray.getJsonObject(i);
				JsonObject currentVersionObject = new JsonObject()
						.put("version", currentVersion.getString("version"))						
						.put("date", currentVersion.getLong("timestamp"));
				if(currentVersion.containsKey("canbetagged"))
				{
					currentVersionObject.put("canbetagged", currentVersion.getBoolean("canbetagged"));
				}				
				String tagString = tagMap.get(currentVersion.getString("version"));
				if(tagString != null)
				{
					currentVersionObject.put("tag", tagString);
				}				
				result.add(currentVersionObject);				
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
	 * @param UUID the UUID of the element to be returned
	 * @param version the version of the element to be returned
	 * @return an API object of the type appropriate for this Manager.
	 */
	public Future<APIElement<T>> getAPIElementFromDB(String UUID, String version)
	{
		Promise<APIElement<T>> elementPromise = Promise.<APIElement<T>>promise();
		APIElement<T> apiElement = apisupplier.get();
		GitFile currentVersion = new GitFile("Object.json", getGitIDForUUID(UUID), version);

		factory.loadElement(client, UUID).
		onSuccess(element -> {
			apiElement.loadFromDBElement(element);
			// the version cannot be extracted from the db element, as the db element stores all versions, and this is a specific request. 
			apiElement.setVersion(version);
			apiElement.loadAdditionalData(eb, getGitIDForUUID(apiElement.getUUID()))
			.onSuccess(Void -> {
				eb.request("soile.git.getGitFileContentsAsJson",currentVersion.toJson())
				.onSuccess(jsonReply-> {
					JsonObject gitJson = (JsonObject) jsonReply.body();					
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

	/**
	 * Get the {@link DataLakeFile} for the given FileName of the taskID  Version
	 * @param elementID the Id of the element 
	 * @param elementVersion the Version of the element
	 * @param filename the filename of the requested file
	 * @return a Future of the Datalake file associated with the given file at this version for the element.
	 */
	public Future<DataLakeFile> handleGetFile(String elementID, String elementVersion, String filename)
	{
		return dataHandler.handleGetFile(elementID, elementVersion, filename);
	}
	/**
	 * Post a given upload to the given task at the given version. Return the new version of the element with the file added.  
	 * @param elementID the id of the element
	 * @param elementVersion the version of the element to add the file to
	 * @param filename the name of the file
	 * @param upload the upload to associate with the file.
	 * @return A Future with the NEw Version of the repository for this element with the data added.
	 */
	public Future<String> handlePostFile(String elementID, String elementVersion, String filename, SOILEUpload upload)
	{
		Future<String> fileProcessedFuture = dataHandler.handlePostFile(elementID, elementVersion, filename, upload).compose(newVersion -> {
			return updateItemWithVersion(elementID, newVersion);
		});
		return fileProcessedFuture; 
	}


	/**
	 * Post a given upload to the given task at the given version. Return the new version of the element with the file added.  
	 * @param elementID the id of the element
	 * @param elementVersion the version of the element to add the file to
	 * @param the name of the directory all the files are to be extracted to.
	 * @param uploads the files to be uploaded
	 * @return A Future with the NEw Version of the repository for this element with the data added.
	 */
	public Future<String> handlePostFiles(String elementID, String elementVersion, String dirName, List<FileUpload> uploads)
	{
		Promise<String> filesUploadedPromise = Promise.promise();
		List<Future> filesUploaded = new LinkedList<Future>();
		LinkedList<Future<String>> chain = new LinkedList<>();		
		chain.add(Future.succeededFuture(elementVersion));
		for(FileUpload up : uploads)
		{
			// if we are uploading to a folder, or to the base directory, we need to add the individual file names. Otherwise the indicated path is the file name
			String currentFileName = (dirName.endsWith("/") || dirName.equals("")) ? dirName + up.fileName() : dirName ;			
			LOGGER.debug("Filename for created file is: " + currentFileName + " while request file name was " + dirName);
			chain.add(chain.getLast().compose(version -> dataHandler.handlePostFile(elementID, version, currentFileName, SOILEUpload.create(up))));
			filesUploaded.add(chain.getLast());
			
		}
		CompositeFuture.all(filesUploaded)		
		.onSuccess(finished -> {
			// this is done when the Composite is done.
			// We need to update the versions to be able to keep track of added files, and to see whether they might be unnecessary.  
			updateItemWithVersion(elementID, chain.getLast().result())
			.onSuccess(newVersion -> 
			{
				filesUploadedPromise.complete(newVersion);	
			})
			.onFailure(err -> filesUploadedPromise.fail(err));								
		})
		.onFailure(err -> filesUploadedPromise.fail(err));

		return filesUploadedPromise.future();
	}

	public Future<Boolean> doesRepoAtVersionExist(String UUID, String version)
	{
		return eb.request("soile.git.doesRepoAndVersionExist", new GitElement(getGitIDForUUID(UUID), version).toJson())
				.map(message -> ((Boolean)message.body()));		
	}
	
	/**
	 * Write a given Stream to a File with a given name and return the new git version with this file written. 
	 * @param elementID the id of the element
	 * @param elementVersion the version of the element to add the file to
	 * @param filename the name of the file
	 * @param is the {@link InputStream} thats being written to the target file.
	 * @return A Future with the NEw Version of the repository for this element with the data added.
	 */
	public Future<String> handleWriteFileToObject(String elementID, String elementVersion, String filename, InputStream is)
	{
		Future<String> fileProcessedFuture = dataHandler.handleWritefile(elementID, elementVersion, filename, is).compose(newVersion -> {
			return updateItemWithVersion(elementID, newVersion);
		});
		return fileProcessedFuture;
	}

	/**
	 * Delete a given file from the given Version returning a new version without the element.   
	 * @param elementID the id of the element
	 * @param elementVersion the version of the element to add the file to
	 * @param filename the name of the file 
	 * @return A Future with the New Version of the repository for this element with the data added.
	 */
	public Future<String> handleDeleteFile(String elementID, String elementVersion, String filename)
	{
		// we don't need to update the versions here, as the history DOES contain this element, and if this history is reachable it needs to persist.
		return dataHandler.handleDeleteFile(elementID, elementVersion, filename);
	}

	/**
	 * Get the supplier for the ElementType represented by this Manager
	 * @return
	 */
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
		apiElement.loadFromAPIJson(json);
		elementPromise.complete(apiElement);
		return elementPromise.future();			
	}

	
	/**
	 * Update an item adding a specific version. This is likely an element which has some files being added.
	 * @param elementID
	 * @param newVersion
	 * @return
	 */
	private Future<String> updateItemWithVersion(String elementID, String newVersion)
	{		
		return getElement(elementID).compose(element -> {
			element.addVersion(newVersion);
			return element.save(client).map(newVersion);
		});
	}
	/**
	 * Static method to create a Project Manager
	 * @param client
	 * @param vertx
	 * @return
	 */
	public static ElementManager<Project> getProjectManager(MongoClient client, Vertx vertx)
	{
		return new ElementManager<Project>(Project::new,APIProject::new, client, vertx);
	}

	/**
	 * Static method to create a Experiment Manager
	 * @param client
	 * @param vertx
	 * @return
	 */
	public static ElementManager<Experiment> getExperimentManager(MongoClient client, Vertx vertx)
	{
		return new ElementManager<Experiment>(Experiment::new,APIExperiment::new, client, vertx);
	}

	/**
	 * Static method to create a Task Manager
	 * @param client
	 * @param vertx
	 * @return
	 */
	public static ElementManager<Task> getTaskManager(MongoClient client, Vertx vertx)
	{
		return new ElementManager<Task>(Task::new,APITask::new, client, vertx);
	}
	
	
}
