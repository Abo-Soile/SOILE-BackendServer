package fi.abo.kogni.soile2.project.instance;

import java.util.HashMap;
import java.util.List;

import fi.abo.kogni.soile2.project.exceptions.InvalidPositionException;
import fi.abo.kogni.soile2.project.items.ExperimentObjectInstance;
import fi.abo.kogni.soile2.project.items.ProjectDataBaseObjectInstance;
import fi.abo.kogni.soile2.project.items.TaskObjectInstance;
import fi.abo.kogni.soile2.project.participant.Participant;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * Instance of a Project in Soile. 
 * This class reflects the Project and projectInstabce schemas 
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
	protected String projectID;
	protected String version;
	protected List<String> participants;	
	protected String name;	
	// This reflects all tasks/experiments and their respective String representation
	protected HashMap<String, ProjectDataBaseObjectInstance> elements;
	protected String start;		
	/**
	 * A basic constructor that can be called by any Implementing class. 
	 */
	protected ProjectInstance()
	{		
	};
	
	/**
	 * A Project will be set up based on the data provided here.
	 * The fields defined in the "Project" and "projectInstance" schemas need to be specified in this 
	 * json.
	 * @param data
	 */
	protected void setupProject(JsonObject data)
	{
		elements = new HashMap<String, ProjectDataBaseObjectInstance>();		
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
	public static Future<ProjectInstance> instantiateProject(JsonObject data, ProjectFactory factory) 
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
	
	private void parseProject(JsonObject data)
	{
		// Get the top-level information
		projectID = data.getString("UUID");
		start = data.getString("start"); 
		instanceID = data.getString("_id");
		version = data.getString("version");
		name = data.getString("name");
		for(Object cTaskData : data.getJsonArray("tasks", new JsonArray()))
		{
			TaskObjectInstance cTask = new TaskObjectInstance((JsonObject)cTaskData, this);
			elements.put(cTask.getInstanceID(), cTask);
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
	 * Get the ID of the project this instance is based on.
	 * @return the instance ID of this project
	 */
	public String getsourceProjectID()
	{
		return projectID;
	}
	
	/**
	 * Create the Json of the data relevant for this instance (data that can be retrieved from the project is ignored.
	 * @return the {@link JsonObject} representing a projectInstance schema
	 */
	public JsonObject toJson()
	{
		JsonObject dbData = new JsonObject()
								.put("_id",instanceID)
								.put("projectID",projectID)
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
	
	/**
	 * Finish a step for a particular participant storing the supplied output information obtained for the user.
	 * @param user
	 * @param resultData
	 */
	public void finishStep(Participant user, JsonObject resultData) throws InvalidPositionException
	{
		if(!resultData.getString("taskID").equals(user.getProjectPosition()))
		{
			throw new InvalidPositionException(user.getProjectPosition(), resultData.getString("taskID"));
		}
		user.setOutputDataForCurrentTask(resultData.getJsonArray("outputdata",new JsonArray()));
		user.setResultDataForCurrentTask(resultData.getJsonObject("resultdata"));
		user.finishCurrentTask();
		/*TODO:
		 * Store the updated user in the database. 
		 */
	}	

	public ProjectDataBaseObjectInstance getElement(String elementID)
	{
		return elements.get(elementID);
	}

	public void startProject(Participant user)
	{
		user.setProjectPosition(start);
	}
	
	public void setNextStep(Participant user)
	{
		ProjectDataBaseObjectInstance current = getElement(user.getProjectPosition());
		String nextElement = current.nextTask(user);		
		user.setProjectPosition(nextElement);
	}				
	
	public abstract Future<Void> save();
	
	public abstract Future<JsonObject> load(JsonObject object);
	
	public abstract Future<JsonObject> delete();
}
