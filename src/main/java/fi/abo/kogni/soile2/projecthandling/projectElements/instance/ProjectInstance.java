package fi.abo.kogni.soile2.projecthandling.projectElements.instance;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import fi.abo.kogni.soile2.projecthandling.exceptions.InvalidPositionException;
import fi.abo.kogni.soile2.projecthandling.participant.Participant;
import fi.abo.kogni.soile2.projecthandling.projectElements.Project;
import fi.abo.kogni.soile2.projecthandling.projectElements.ProjectManager;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.impl.ExperimentObjectInstance;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.impl.FilterObjectInstance;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.impl.TaskObjectInstance;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * Instance of a Project in Soile. 
 * This reflects  running instance of a project. The Project Management (during creation and modfication of the project)
 * is handled in the {@link Project} class, which represents a project in the database that the {@link ProjectManager} 
 * links with the respective code in the git Repository.  
 * @author Thomas Pfau
 *
 */
public abstract class ProjectInstance {

	/**
	 * This is the projectInstance _id
	 */
	protected String instanceID;
	/**
	 * This is the reference project ID.
	 */
	protected String sourceUUID;
	protected String version;
	protected List<String> participants;	
	protected String name;	
	// This reflects all tasks/experiments and their respective String representation
	protected HashMap<String, ElementInstance> elements;
	protected String start;		
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
	 * Instantiate the project (return a {@link ProjectInstance} when finished), where data is data the probvided project implementation
	 * can use to retrieve the information required by setupProject.
	 * @param p The (uninitialized base project)
	 * @param data The data required by the project implementation to build the Project data
	 * @param pManager the project Manager 
	 * @return a Fture that will have the instantiated project created from the factory.
	 */
	public static Future<ProjectInstance> instantiateProject(JsonObject data, ProjectInstanceFactory factory) 
	{
		Promise<ProjectInstance> projPromise = Promise.<ProjectInstance>promise();	
		ProjectInstance p = factory.createInstance();
		p.load(data).onSuccess(dataJson -> {
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
		sourceUUID = data.getString("sourceUUID");
		start = data.getString("start"); 
		instanceID = data.getString("_id");
		version = data.getString("version");
		name = data.getString("name");
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
		for(Object cElementData : experiment.getJsonArray("elements", new JsonArray()))
		{			
			JsonObject element = (JsonObject)cElementData;
			switch(element.getString("elementtype"))
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
	 * Create the Json of the data relevant for this instance (data that can be retrieved from the project is ignored.
	 * @return the {@link JsonObject} representing a projectInstance schema
	 */
	public JsonObject toDBJson()
	{
		JsonObject dbData = new JsonObject()
								.put("_id",instanceID)
								.put("sourceUUID",sourceUUID)
								.put("version", version)
								.put("participants", new JsonArray(participants));
		return dbData;
	}	
	
	/**
	 * Add a participant to the list of participants of this projects
	 * @param p the participant to add
	 */
	public void addParticipant(Participant p)
	{
		participants.add(p.getID());
	}
	
	/**
	 * Delete a participant from the list of participants of this projects
	 * @param p the participant to remove
	 */
	public void deleteParticipant(Participant p)
	{
		participants.remove(p.getID());
	}
	
	public List<String> getParticipants()
	{
		return List.copyOf(participants);
	}
	
	/**
	 * Finish a step for a particular participant storing the supplied output information obtained for the user.
	 * @param user
	 * @param resultData
	 */
	public Future<String> finishStep(Participant user, JsonObject resultData) throws InvalidPositionException
	{
		if(!resultData.getString("taskID").equals(user.getProjectPosition()))
		{
			throw new InvalidPositionException(user.getProjectPosition(), resultData.getString("taskID"));
		}
		user.setOutputDataForCurrentTask(resultData.getJsonArray("outputdata",new JsonArray()));
		user.setResultDataForCurrentTask(resultData.getJsonObject("resultdata"));
		user.finishCurrentTask();
		return user.save();
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
	 * @return
	 */
	public String getNextTask(String nextElementID, Participant user)
	{
		return elements.get(nextElementID).nextTask(user);
	}
	
	/**
	 * Start the project for the provided user.
	 * @param user
	 */
	public void startProject(Participant user)
	{
		user.setProjectPosition(start);
	}
	
	/**
	 * Proceed the user to the next step within this project (depending on Filters etc pp).
	 * @param user The user that proceeds to the next step.
	 */
	public void setNextStep(Participant user)
	{
		
		ElementInstance current = getElement(user.getProjectPosition());
		String nextElement = current.nextTask(user);
		System.out.println("Updating user position:" + current.getInstanceID() + " -> " + nextElement);		
		user.setProjectPosition(nextElement);
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
	 * @param object The object to retrieve the data from.
	 * @return A Future containing all data actually necessary for {@link ProjectInstance} to reconstruct all necessary fields. 
	 */
	public abstract Future<JsonObject> load(JsonObject object);
	
	/**
	 * Delete the project instance represented by this Object. Note: This must NOT delete the actual Project data, but only the data 
	 * associated with this run of the project.
	 * @return A future that contains the data that was associated with the ProjectInstance. This function must NOT handle the deletion of it's 
	 * participants, that deletion should be handled at whatever place this Future is requested. 
	 */
	public abstract Future<JsonObject> delete();
	
}
