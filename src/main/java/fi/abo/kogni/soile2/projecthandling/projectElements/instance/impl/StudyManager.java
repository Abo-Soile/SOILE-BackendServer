package fi.abo.kogni.soile2.projecthandling.projectElements.instance.impl;

import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fi.abo.kogni.soile2.datamanagement.utils.DirtyDataRetriever;
import fi.abo.kogni.soile2.datamanagement.utils.TimeStampedMap;
import fi.abo.kogni.soile2.projecthandling.exceptions.ObjectDoesNotExist;
import fi.abo.kogni.soile2.projecthandling.projectElements.impl.ElementManager;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.Study;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.StudyFactory;
import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.FindOptions;
import io.vertx.ext.mongo.MongoClient;

/**
 * This class is responsible for project information in the db.
 * This includes the project reference (to the respective git repo), the participants in this 
 * project etc. 
 * TODO: Handle shortcut updates (i.e. on a sc update the old shortcut must be reomved from the projectPathes!)
 * @author Thomas Pfau
 *
 */
public class StudyManager implements DirtyDataRetriever<String, Study> {

	static final Logger LOGGER = LogManager.getLogger(StudyManager.class);
	
	MongoClient client;
	// should go to the constructor.
	private StudyFactory dbFactory;
	private StudyFactory createFactory;
	private String instanceCollection;
	private TimeStampedMap<String, String> projectPathes;
	private HashMap<String,Long> studyTimes;
	/**
	 * Default constructor
	 * @param client {@link MongoClient} for db access
	 * @param vertx {@link Vertx} instance for communication 
	 */
	public StudyManager(MongoClient client, Vertx vertx)
	{						
		this(client,
				new ElementToDBStudyFactory(ElementManager.getProjectManager(client, vertx),client, vertx.eventBus()),								
				new DBStudyFactory(ElementManager.getProjectManager(client, vertx),client, vertx.eventBus())				 
				);				
	}		
		
	/**
	 * Constructor with redefined {@link ElementToDBStudyFactory} and {@link DBStudyFactory} 
	 * @param client {@link MongoClient} for communication
	 * @param createFactory Factory that can handle Element to DB requests
	 * @param dbFactory Factory that can handle DB extraction
	 */
	public StudyManager(MongoClient client, StudyFactory createFactory, StudyFactory dbFactory)
	{
		this.client = client;
		this.dbFactory = dbFactory;
		this.createFactory = createFactory;		
		instanceCollection = SoileConfigLoader.getdbProperty("studyCollection");
		projectPathes = new TimeStampedMap<String, String>(this::getProjectIDForPathIDfromDB, 1000*60*60);
		studyTimes = new HashMap<>();
	}

	/**
	 * General cleanup function
	 */
	public void cleanUp()
	{
		projectPathes.cleanUp();
	}
	
	
	
	@Override
	public Future<Study> getElement(String key) {		
		return Study.instantiateStudy(new JsonObject().put("_id", key), dbFactory);				
	}

	@Override
	public void getElement(String key, Handler<AsyncResult<Study>> handler) {
		handler.handle(getElement(key));
	}

	@Override
	public Future<Study> getElementIfDirty(String key) {
		Promise<Study> dirtyPromise = Promise.promise();
		client.findOne(instanceCollection,  new JsonObject().put("_id", key), new JsonObject().put("modifiedStamp",1))
		.onSuccess(res -> {
			LOGGER.debug(res.encodePrettily());
			LOGGER.debug(studyTimes.get(key));
			
			if(!studyTimes.containsKey(key) || studyTimes.get(key) >= res.getLong("modifiedStamp"))
			{
				dirtyPromise.complete(null);
			}
			else
			{
				getElement(key).onSuccess(part ->
				{
					updateStudy(part);
					dirtyPromise.complete(part);
				})
				.onFailure(err -> dirtyPromise.fail(err));
			}
		})
		.onFailure(err -> dirtyPromise.fail(err));
		return dirtyPromise.future();			
	}

	@Override
	public void getElementIfDirty(String key, Handler<AsyncResult<Study>> handler) {
		// TODO Auto-generated method stub
		handler.handle(getElementIfDirty(key));
	}

	
	/**
	 * Create a new Project with empty information provide it to the Handler
	 * database.
	 * @param projectID - The UUID of the project 
	 * @param projectVersion - The version of the project to start
	 * @param projectVersion - The name of the instance.
	 * @param handler the handler to handle the created participant
	 */
	/*public StudyManager startProject(JsonObject projectInformation, Handler<AsyncResult<Study>> handler, String projectInstanceName)
	{
		handler.handle(startProject(projectInformation));
		return this;
	}*/

	/**
	 * Create a new Project with empty information and retrieve a new ID from the 
	 * database.
	 * 1. "UUID" of the project from which this was started
	 * 2. "Version" of the project from which this was started
	 * 3. "private" field wrt access for this 
	 * 4. "name" a name field.
	 * 5. "shortcut" (optional), that can be used as a shortcut to the project.
	 * @param projectInformation a JsonObject with the above mentioned entries
	 * @return A {@link Future} of the started {@link Study}
	 */
	public Future<Study> startProject(JsonObject projectInformation )
	{						
		LOGGER.debug("Trying to instanciate Project ");
		return Study.instantiateStudy(projectInformation, createFactory);
	}
	
	/**
	 * Default schema of a Participant.
	 * @param projectVersion the version of the project 
	 * @param projectID the ID of the project
	 * @return an empty participant information {@link JsonObject}
	 */
	public static JsonObject getDefaultProjectInfo(String projectVersion, String projectID)
	{
		return new JsonObject().put("projectID", projectID)
							   .put("version", projectVersion)
							   .put("participants", new JsonArray())
							   .put("private", false)
							   .put("signupTokens",new JsonArray());
	}
	
	/**
	 * Update the data relevant for the manager.
	 * @param study The study to update
	 */
	public void updateStudy(Study study)
	{
		if(study.getShortCut() != null)
		{
			projectPathes.putData(study.getShortCut(), study.getID());
		}
		studyTimes.put(study.getID(), study.getModifiedDate());
	}
	
	/**
	 * Get a list of UUID of project instances and their privacy status based on the permissions provided (those are essentially _ids)
	 * @param projectInstanceIDs A JsonArray with strings for each permission/projectID
	 * @param idsOnly whether only use the ids or also return "public ones"
	 * @param activeOnly whether only to get active ones
	 * @return A Future of a JsonArray of Study Jsons from the DB. 
	 */
	public Future<JsonArray> getProjectInstanceStatus(JsonArray projectInstanceIDs, Boolean idsOnly, Boolean activeOnly)
	{
		Promise<JsonArray> listPromise = Promise.promise();		 				
		
		JsonObject Query = new JsonObject();
		if(idsOnly)
		{
			Query.put("_id", new JsonObject().put("$in", projectInstanceIDs));
		}
		else			
		{
			Query.put("$or", new JsonArray().add(new JsonObject().put("private",false))
					  .add(new JsonObject().put("_id", new JsonObject().put("$in", projectInstanceIDs)))
					  );
		}
		if(activeOnly)
		{
			Query = new JsonObject().put("$and", new JsonArray().add(new JsonObject().put("active", true)).add(Query));
		}
		JsonObject fields = new JsonObject().put("_id",1).put("name", 1).put("description", 1).put("language", 1).put("shortDescription", 1).put("shortcut", 1);
		LOGGER.debug("Looking for Project matching:\n" + Query.encodePrettily());
		return getStudyData(Query, fields);
	}
	
	/**
	 * Get he list of studies available on this server
	 * @param Query the query to run on for the study database
	 * @param Fields which fields to return
	 * @return A Future of a JsonArray of Study Jsons from the DB with the specified fields.
	 */
	public Future<JsonArray> getStudyData(JsonObject Query, JsonObject Fields)
	{				
		Promise<JsonArray> listPromise = Promise.promise();		 				

		client.findWithOptions(instanceCollection,Query,new FindOptions().setFields(Fields))
		.onSuccess(items -> 
				{
					JsonArray result = new JsonArray();
					for(JsonObject o : items)
					{
						o.put("UUID", o.getString("_id")).remove("_id");
						result.add(o);
					}
					LOGGER.debug("Result is: " + result.encodePrettily() );
					listPromise.complete(result);
							
				})
		.onFailure(err -> listPromise.fail(err));
		
		return listPromise.future();
	}
	
	/**
	 * Get Studies (names and UUIDs)
	 * @return A {@link JsonArray} of all Studies
	 */
	public Future<JsonArray> getStudies()
	{
		return getStudyData(new JsonObject(), new JsonObject().put("_id",1).put("name",1));
	}
	
	/**
	 * Get the project ID for a given path
	 * @param pathID the pathID
	 * @return the ID for the given path 
	 */
	public Future<String> getProjectIDForPathID(String pathID)
	{
		return projectPathes.getData(pathID);
	}
	
	/**
	 * Get the projectID from the path
	 * @param pathID The {id} parameter from a path, can be either a shortcut or the actual project id.
	 * @return A Future of the actual project id
	 */
	private Future<String> getProjectIDForPathIDfromDB(String pathID)
	{
		Promise<String> idPromise = Promise.promise();		 				
		JsonObject Query = new JsonObject().put("$or", new JsonArray().add(new JsonObject().put("shortcut",pathID))
																	  .add(new JsonObject().put("_id", pathID))
																	  );
		LOGGER.debug("Looking for Project matching:\n" + Query.encodePrettily());
		client.findOne(instanceCollection,Query,new JsonObject().put("_id",1))
		.onSuccess(project -> 
		{
			if(project == null)
			{
				idPromise.fail(new ObjectDoesNotExist(pathID));
			}
			else
			{
				idPromise.complete(project.getString("_id"));
			}
		})
		.onFailure(err -> idPromise.fail(err));		
		return idPromise.future();
	}

	/**
	 * Activate a study
	 * @param study the study to activate
	 * @return A {@link Future} indicating successfull start
	 */
	public Future<Void> activate(Study study) {
		// activate the instance,		
		study.activate();
		// set the modified date
		study.setModifiedDate();
		// update the information
		updateStudy(study);
		// and save the study.
		return study.save(false).mapEmpty();		
	}

	/**
	 * Deactivate a study
	 * @param study the Study to deactivate
	 * @return A {@link Future} indicating successful deactivation
	 */
	public Future<Void> deactivate(Study study) {
		// activate the instance,		
		study.deactivate();
		// set the modified date
		study.setModifiedDate();
		// update the information
		updateStudy(study);
		// and save the study.
		return study.save(false).mapEmpty();		
	}
	
	/**
	 * Update a study. This should always reset information. 
	 * @param study the study to update
	 * @param newData the new data for the study
	 * @return  A {@link Future} indicating successful update
	 */
	public Future<Void> updateStudy(Study study, JsonObject newData)
	{
		Promise<Void> updatePromise = Promise.promise();
		study.updateStudy(newData).onSuccess(modifiedDate -> {
			updateStudy(study);
			updatePromise.complete();
		})
		.onFailure(err -> updatePromise.fail(err));
		return updatePromise.future();
	}
}