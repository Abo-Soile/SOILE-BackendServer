package fi.abo.kogni.soile2.projecthandling.projectElements.instance;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fi.abo.kogni.soile2.http_server.auth.SoileAuthorization.TargetElementType;
import fi.abo.kogni.soile2.http_server.authentication.utils.AccessElement;
import fi.abo.kogni.soile2.projecthandling.exceptions.InvalidPositionException;
import fi.abo.kogni.soile2.projecthandling.exceptions.ProjectIsInactiveException;
import fi.abo.kogni.soile2.projecthandling.participant.Participant;
import fi.abo.kogni.soile2.projecthandling.projectElements.Project;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.impl.ExperimentObjectInstance;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.impl.FilterObjectInstance;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.impl.TaskObjectInstance;
import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * Instance of a Project in Soile. 
 * This reflects  running instance of a project. The Project Management (during creation and modfication of the project)
 * is handled in the {@link Project} class, which represents a project in the database that the {@link ProjectManagerImpl} 
 * links with the respective code in the git Repository.
 * To walk through a Project the following steps are necessary:
 * Participant participant 
 * taskID = proj.startProject(participant) 
 * taskID = proj.finishStep(participant,taskDataJson) // taskID must be submitted in the finishStep taskID field, to confirm this is the right task.
 * taskID = proj.finishStep(participant,taskDataJson)// taskID must be submitted in the finishStep taskID field, to confirm this is the right task. 
 * proj.finishStep(participant,taskDataJson)
 * ...
 *    
 * @author Thomas Pfau
 *
 */
public abstract class ProjectInstance implements AccessElement{

	static final Logger LOGGER = LogManager.getLogger(ProjectInstance.class);

	/**
	 * This is the projectInstance _id
	 */
	protected String instanceID;
	/**
	 * This is the reference project ID.
	 */
	protected String sourceUUID;
	protected String version;
	/**
	 * Access to the participants array needs to be synchronized, at least after initiation of the object. 
	 */
	protected List<String> participants;	
	protected String name;	
	// This reflects all tasks/experiments and their respective String representation
	protected HashMap<String, ElementInstance> elements;
	protected String start;
	// a url shortcut.
	protected String shortcut;	
	protected boolean isActive;
	/**
	 * A basic constructor that can be called by any Implementing class. 
	 */
	protected ProjectInstance()
	{		
		participants = new LinkedList<String>();
	};
	
	/**
	 * A Project will be set up based on the data provided here.
	 * The fields defined in the "Project" and "projectInstance" schemas need to be specified in this 
	 * json.
	 * @param data
	 */
	protected void setupProject(JsonObject data)
	{
		elements = new HashMap<String, ElementInstance>();		
		parseProject(data);
	}		
	
	/**
	 * Instantiate the project (return a {@link ProjectInstance} when finished), where data is data the provided project implementation
	 * can use to retrieve the information required by setupProject.
	 * @param instantiationInfo All information needed by the Instance generated from the provided factory to create the instance.
	 * @param data The data required by the project implementation to build the Project data
	 * @param pManager the project Manager 
	 * @return a Future that will have the instantiated project created from the factory.
	 */
	public static Future<ProjectInstance> instantiateProject(JsonObject instantiationInfo, ProjectInstanceFactory factory) 
	{
		Promise<ProjectInstance> projPromise = Promise.<ProjectInstance>promise();
		LOGGER.debug("Creating instance");
		ProjectInstance p = factory.createInstance();
		LOGGER.debug(p);
		LOGGER.debug(factory);
		p.load(instantiationInfo).onSuccess(dataJson -> {
			LOGGER.debug("Trying to set up project from data: \n " + dataJson.encodePrettily());
			p.setupProject(dataJson);
			projPromise.complete(p);
		}).onFailure(fail -> {
			projPromise.fail(fail);
		});
		return projPromise.future();
	}
	
	
	/**
	 * Parse a project from Json data.
	 * @param data
	 */
	private void parseProject(JsonObject data)
	{
		// Get the top-level information
		LOGGER.debug("Parsing data");
		LOGGER.debug(data.encodePrettily());
		sourceUUID = data.getString("sourceUUID");
		start = data.getString("start"); 
		instanceID = data.getString("_id");
		version = data.getString("version");
		name = data.getString("name");
		shortcut = data.getString("shortcut",null);
		for(Object cTaskData : data.getJsonArray("tasks", new JsonArray()))
		{
			TaskObjectInstance cTask = new TaskObjectInstance((JsonObject)cTaskData, this);
			elements.put(cTask.getInstanceID(), cTask);
		}
		for(Object cTaskData : data.getJsonArray("filters", new JsonArray()))
		{
			FilterObjectInstance cFilter= new FilterObjectInstance((JsonObject)cTaskData, this);
			elements.put(cFilter.getInstanceID(), cFilter);
		}
		for(Object cExperimentData : data.getJsonArray("experiments", new JsonArray()))
		{
			parseExperiment((JsonObject)cExperimentData);
		}
		// these are all strings...
		for(Object o : data.getJsonArray("participants"))
		{
			participants.add(o.toString());
		}
	}
	/**
	 * Parse an experiment from the given experiment Json. 
	 * @param experiment
	 */
	private void parseExperiment(JsonObject experiment)
	{
		ExperimentObjectInstance cExperiment = new ExperimentObjectInstance(experiment, this);
		elements.put(cExperiment.getInstanceID(), cExperiment);
		LOGGER.debug("Experiment info is: " + experiment.encodePrettily());
		for(Object cElementData : experiment.getJsonArray("elements", new JsonArray()))
		{			
			LOGGER.debug("Experiment info is: " + cElementData);
			JsonObject element = (JsonObject)cElementData;
			switch(element.getString("elementType"))
			{
			case "task": 
				TaskObjectInstance cTask = new TaskObjectInstance(((JsonObject)cElementData).getJsonObject("data"), this);
				elements.put(cTask.getInstanceID(), cTask);
				break;
			case "experiment":
				parseExperiment(element);
				break;
			case "filter":
				FilterObjectInstance cFilter = new FilterObjectInstance(((JsonObject)cElementData).getJsonObject("data"), this);
				elements.put(cFilter.getInstanceID(), cFilter);
				break;
				
			}
		}
		
	}
	/**
	 * Get the instance ID of this project
	 * @return the instance ID of this project
	 */
	public String getID()
	{
		return instanceID;
	}
	
	/**
	 * Create the Json of the data relevant for this instance (data that can be retrieved from the project is ignored)
	 * Participants are excluded from this. 
	 * @return the {@link JsonObject} representing a projectInstance schema
	 */
	public JsonObject toDBJson()
	{		
			JsonObject dbData = new JsonObject()
					.put("_id",instanceID)
					.put("sourceUUID",sourceUUID)
					.put("version", version)
					.put("name", name)
					.put("shortcut", shortcut)
					.put("isActive", isActive);				
		return dbData;
	}	
	
	/**
	 * Add a participant to the list of participants of this projects
	 * @param p the participant to add
	 */
	public abstract Future<Boolean> addParticipant(Participant p);
	
	/**
	 * Delete a participant from the list of participants of this projects
	 * @param p the participant to remove
	 */
	public abstract Future<Boolean> deleteParticipant(Participant p);
	
	/**
	 * Get a list of Participants in the project 
	 * @return A Future of a Jsonarray with all participant ids.
	 */
	public abstract Future<JsonArray> getParticipants();
	
	/**
	 * Finish a step for a particular participant storing the supplied output information obtained for the user.
	 * @param user
	 * @param taskData
	 */
	public Future<String> finishStep(Participant user, JsonObject taskData)
	{
		if(!isActive)
		{
			return Future.failedFuture(new ProjectIsInactiveException(name));
		}		
		Promise<String> finishedPromise = Promise.promise();
		LOGGER.debug(taskData.encodePrettily());
		if(!taskData.getString("taskID").equals(user.getProjectPosition()))
		{
			finishedPromise.fail( new InvalidPositionException(user.getProjectPosition(), taskData.getString("taskID")));			
		}
		else
		{
			user.setOutputDataForTask(user.getProjectPosition(),taskData.getJsonArray("outputdata",new JsonArray()))
			.onSuccess(v -> {
				user.addResult(user.getProjectPosition(), getResultDataFromTaskData(taskData))
				.onSuccess(v2 -> {
					user.finishCurrentTask()
					.onSuccess(v3 -> {
						setNextStep(user).onSuccess( next -> 
						{
							finishedPromise.complete(next);
						})
						.onFailure(err -> finishedPromise.fail(err));
					})
					.onFailure(err -> finishedPromise.fail(err));					
				})
				.onFailure(err -> finishedPromise.fail(err));
				
			})
			.onFailure(err -> finishedPromise.fail(err));
			
		}
		return finishedPromise.future();
	}	
	
	private JsonObject getResultDataFromTaskData(JsonObject taskData)
	{
		JsonObject resultData = new JsonObject();
		resultData.put("task", taskData.getString("taskID"))
				  .put("dbData", taskData.getJsonObject("resultdata",new JsonObject()).getJsonArray("jsonData", new JsonArray()))
				  .put("fileData", taskData.getJsonObject("resultdata",new JsonObject()).getJsonArray("fileData", new JsonArray()));
		return resultData;
	}
	
	/**
	 * Get a specific Task/Experiment element for the given Element ID.
	 * @param elementID
	 * @return
	 */
	public ElementInstance getElement(String elementID)
	{
		return elements.get(elementID);
	}
	
	/**
	 * Get all element IDs in this Project
	 * @return
	 */
	public List<String> getElements()
	{
		return List.copyOf(elements.keySet());
	}
	
	/**
	 * Obtain the next element for the given user using the given element ID to query the next element 
	 * @param nextElementID
	 * @param user
	 * @return The next Task as defined by the element with the given ID, or null if the element is not defined (which means that we have reached an end-point). 
	 */
	public String getNextTask(String nextElementID, Participant user)
	{
		// if we don't get a nextElementID, we are done with the Project.
		if("".equals(nextElementID) || nextElementID == null)
		{
			return null;
		}
		return elements.get(nextElementID).nextTask(user);
	}
	
	/**
	 * Start the project for the provided user.
	 * @param user
	 */
	public Future<String> startProject(Participant user)
	{
		if(!isActive)
		{
			return Future.failedFuture(new ProjectIsInactiveException(name));
		}
		return user.setProjectPosition(start);		
	}
	
	/**
	 * Proceed the user to the next step within this project (depending on Filters etc pp).
	 * This will return the position the user was set to if everything succeeds.
	 * @param user The user that proceeds to the next step.
	 * @return A future of the next step instance ID, or null if this is participant is finished.
	 */
	public Future<String> setNextStep(Participant user)
	{		
		if(!isActive)
		{
			return Future.failedFuture(new ProjectIsInactiveException(name));
		}		
		ElementInstance current = getElement(user.getProjectPosition());
		String nextElement = current.nextTask(user);
		if("".equals(nextElement) || nextElement == null)
		{
			// This indicates we are done. 
			return user.setProjectPosition(null);	
		}
		LOGGER.debug("Updating user position:" + current.getInstanceID() + " -> " + nextElement);		
		return user.setProjectPosition(nextElement);
	}				
		
	
	public TargetElementType getElementType()
	{
		return TargetElementType.INSTANCE;
	}

	public String getTargetCollection()
	{
		return SoileConfigLoader.getCollectionName("projectInstanceCollection");
	}
	
	/**
	 * This operation saves the Project. It should ensure that the data can be reconstructed by supplying what is returned 
	 * form this function to the load() function of this class.
	 * @return A JsonObject 
	 */
	public abstract Future<JsonObject> save();
	
	/**
	 * Load all data that is necessary for the project. This function should work with the Data contained in the future provided by 
	 * the save function.  
	 * @param object The object to retrieve the data from. Can be specific to the Actual implementation used. e.g. can only contain one ID if loading from a DB or multiple fields if construction from multiple DBs. 
	 * @return A Future containing all data actually necessary for {@link ProjectInstance} to reconstruct all necessary fields. 
	 */
	public abstract Future<JsonObject> load(JsonObject instanceInfo);
	
	/**
	 * Delete the project instance represented by this Object. Note: This must NOT delete the actual Project data, but only the data 
	 * associated with this run of the project.
	 * @return A future that contains the data that was associated with the ProjectInstance. This function must NOT handle the deletion of it's 
	 * participants, that deletion should be handled at whatever place this Future is requested. 
	 */
	public abstract Future<JsonObject> delete();
	
	/**
	 * Stop a project
	 */
	public abstract Future<Void> deactivate();
	
	/**
	 * Restart a project if it was deactivated. By default a project is active.
	 */
	public abstract Future<Void> activate();
}
