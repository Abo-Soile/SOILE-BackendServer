package fi.abo.kogni.soile2.projecthandling.projectElements.instance;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fi.abo.kogni.soile2.http_server.auth.SoileAuthorization.TargetElementType;
import fi.abo.kogni.soile2.http_server.authentication.utils.AccessElement;
import fi.abo.kogni.soile2.projecthandling.exceptions.InvalidPositionException;
import fi.abo.kogni.soile2.projecthandling.exceptions.ProjectIsInactiveException;
import fi.abo.kogni.soile2.projecthandling.exceptions.UnknownRandomizerException;
import fi.abo.kogni.soile2.projecthandling.participant.Participant;
import fi.abo.kogni.soile2.projecthandling.projectElements.impl.Project;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.impl.ExperimentObjectInstance;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.impl.FieldSpecifications;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.impl.FilterObjectInstance;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.impl.TaskObjectInstance;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.randomizers.Randomizer;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.randomizers.RandomizerFactory;
import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * Instance of a Study in Soile. 
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
public abstract class Study implements AccessElement{

	static final Logger LOGGER = LogManager.getLogger(Study.class);

	/**
	 * This is the study _id
	 */
	protected String instanceID;
	/**
	 * This is the reference project ID.
	 */
	protected JsonObject sourceProject;
	/**
	 * Access to the participants array needs to be synchronized, at least after initiation of the object. 
	 */
	//protected List<String> participants;


	/**
	 * Fields reflecting the other db Fields
	 */
	protected String name;	
	protected String shortcut;	
	protected String shortDescription;
	protected String description;
	protected boolean isPrivate;
	protected Long modifiedStamp;	
	
	/**
	 *  This reflects all tasks/experiments and their respective String representation
	 *  They are generated on load and not stored in the database, but retrieved from the source Project.
	 */

	protected HashMap<String, ElementInstance> elements;

	/**
	 * A basic constructor that can be called by any Implementing class. 
	 */
	protected Study()
	{		
		//participants = new LinkedList<String>();
		elements = new HashMap<String, ElementInstance>();
		sourceProject = new JsonObject();
		shortcut = "";
		name = "";
		description = "";
		shortDescription = "";
		isPrivate = false; 
		modifiedStamp = new Date().getTime();
	};

	/**
	 * Set the modification date.
	 * @param date
	 */
	public void setModifiedDate()
	{
		modifiedStamp = new Date().getTime();		
	}

	/**
	 * Get the last modification date of this OBJECT. Note this is not necessarily the same as the one in the database.
	 * @return the modification date of this object
	 */
	public long getModifiedDate()
	{
		return modifiedStamp;		
	}

	/**
	 * A Project will be set up based on the data provided here.
	 * The fields defined in the "Project" and "Study" schemas need to be specified in this 
	 * json.
	 * @param data
	 */
	protected void setupProject(JsonObject data) throws UnknownRandomizerException
	{
		elements = new HashMap<String, ElementInstance>();
		LOGGER.debug("Loading data into project from: " + data);
		parseProject(data);
	}		

	/**
	 * Instantiate the project (return a {@link Study} when finished), where data is data the provided project implementation
	 * can use to retrieve the information required by setupProject.
	 * @param instantiationInfo All information needed by the Instance generated from the provided factory to create the instance.
	 * @param data The data required by the project implementation to build the Project data
	 * @param pManager the project Manager 
	 * @return a Future that will have the instantiated project created from the factory.
	 */
	public static Future<Study> instantiateStudy(JsonObject instantiationInfo, StudyFactory factory) 
	{
		Promise<Study> projPromise = Promise.<Study>promise();
		LOGGER.debug("Creating instance");
		LOGGER.debug(factory.getClass());
		Study p = factory.createInstance();
		LOGGER.debug(instantiationInfo.encodePrettily());
		p.load(instantiationInfo)
		.onSuccess(dataJson -> {
			LOGGER.debug("Trying to set up project from data: \n " + dataJson.encodePrettily());
			try {
				p.setupProject(dataJson);
			}
			catch(UnknownRandomizerException e)
			{
				projPromise.fail(e);
				return;
			}
			projPromise.complete(p);
		}).onFailure(fail -> {
			projPromise.fail(fail);
		});
		return projPromise.future();
	}
	/***
	 * Get the UUID of the project run by this study.
	 * @return The UUID of this studies source project
	 */
	public String getSourceUUID()
	{
		return this.sourceProject.getString("UUID");
	}
	/***
	 * Set the UUID of the project run by this study.
	 * @param newUUID The UUID of this studies source project
	 */
	public void setSourceUUID(String newUUID)
	{
		this.sourceProject.put("UUID", newUUID);
	}

	/***
	 * Get the version of the project run by this study.
	 * @return The version of this studies source project
	 */
	public String getSourceVersion()
	{
		return this.sourceProject.getString("version");
	}
	/***
	 * Set the version of the project run by this study.
	 * @param newVersion The new version of this studies source project
	 */
	public void setSourceVersion(String newVersion)
	{
		this.sourceProject.put("version", newVersion);
	}

	/**
	 * Get the ID of the start element of the project run by this study.
	 * @return newStart The new start ID of this studies source project
	 */
	public String getStart()
	{
		return this.sourceProject.getString("start");
	}

	/**
	 * Parse a project from Json data.
	 * @param data
	 */
	private void parseProject(JsonObject data) throws UnknownRandomizerException
	{
		// Get the top-level information
		LOGGER.debug("Parsing data");
		LOGGER.debug(data.encodePrettily());	
		instanceID = data.getString("_id");
		name = data.getString("name");
		shortcut = data.getString("shortcut",null);
		sourceProject = data.getJsonObject("sourceProject");
		shortDescription = data.getString("shortDescription", "");
		description = data.getString("description","");
		isPrivate = data.getBoolean("private",false);
		modifiedStamp = data.getLong("modifiedStamp", new Date().getTime());
		for(Object cTaskData : sourceProject.getJsonArray("tasks", new JsonArray()))
		{
			TaskObjectInstance cTask = new TaskObjectInstance((JsonObject)cTaskData, this);
			elements.put(cTask.getInstanceID(), cTask);
		}
		for(Object cElementData : sourceProject.getJsonArray("randomizers", new JsonArray()))
		{
			Randomizer rand = RandomizerFactory.create((JsonObject)cElementData, this);
			elements.put(rand.getInstanceID(), rand);
		}
		for(Object cFilterData : sourceProject.getJsonArray("filters", new JsonArray()))
		{
			FilterObjectInstance cFilter= new FilterObjectInstance((JsonObject)cFilterData, this);
			elements.put(cFilter.getInstanceID(), cFilter);
		}
		for(Object cExperimentData : sourceProject.getJsonArray("experiments", new JsonArray()))
		{
			parseExperiment((JsonObject)cExperimentData);
		}
		// these are all strings...
		/*	for(Object o : data.getJsonArray("participants"))
		{
			participants.add(o.toString());
		}*/
	}

	/**
	 * Get the name of the project Instance
	 * @return
	 */
	public String getName()
	{
		return name;
	}


	@Override
	public String toString()
	{
		return toDBJson().encodePrettily() + elements.toString();
	}

	public String getShortCut()
	{
		return shortcut.equals("") ? null : shortcut;
	}
	/**
	 * Parse an experiment from the given experiment Json. 
	 * @param experiment
	 */
	private void parseExperiment(JsonObject experiment) throws UnknownRandomizerException
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
			case "randomizer":
				Randomizer rand = RandomizerFactory.create(((JsonObject)cElementData).getJsonObject("data"), this);
				elements.put(rand.getInstanceID(), rand);
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
				.put("sourceUUID",getSourceUUID())					
				.put("version", getSourceVersion())
				.put("name", name)
				.put("description", description )
				.put("shortDescription", shortDescription)
				.put("private", isPrivate)
				.put("modifiedStamp", modifiedStamp);				

		if(shortcut != null)
		{
			dbData.put("shortcut", shortcut);
		}
		return dbData;
	}			

	/**
	 * Create the Json to return for the API. This will contain a UUID instead of an _id field and no modification stamp.  
	 * @return the {@link JsonObject} representing a study schema
	 */
	public JsonObject toAPIJson()
	{		
		JsonObject data = toDBJson();
		data.put("UUID", data.remove("_id"));
		data.remove("modifiedStamp");			
		return data;
	}	


	/**
	 * Finish a step for a particular participant storing the supplied output information obtained for the user.
	 * @param participant the participant for whom to finish the step
	 * @param taskData the participants data for this task
	 * @return A Future of the next step for the given participant (which is also set if this is completed). 
	 */
	public Future<String> finishStep(Participant participant, JsonObject taskData)
	{
		LOGGER.debug("Handling participant: " + participant.toString() + " with data " + taskData.encodePrettily());
		Promise<String> finishedPromise = Promise.promise();

		isActive()
		.onSuccess(active -> {
			if(!active)
			{
				finishedPromise.fail(new ProjectIsInactiveException(name));
			}
			else
			{
				LOGGER.debug(taskData.encodePrettily());
				if(taskData.getString("taskID") == null || !taskData.getString("taskID").equals(participant.getStudyPosition()))
				{
					finishedPromise.fail( new InvalidPositionException(participant.getStudyPosition(), taskData.getString("taskID")));			
				}
				else
				{
					//TODO: Check, whether the indicated outputs are actually present!
					if(isDataOkForTask(taskData)) 
					{
						participant.setOutputDataForTask(participant.getStudyPosition(),taskData.getJsonArray("outputData",new JsonArray()))
						.onSuccess(outputsSet -> {
							participant.setPersistentData(taskData.getJsonArray("persistentData",new JsonArray()))
							.onSuccess(persistentSet -> {
								participant.addResult(participant.getStudyPosition(), getResultDataFromTaskData(taskData))
								.onSuccess(v2 -> {
									participant.finishCurrentTask()
									.onSuccess(v3 -> {
										setNextStep(participant).onSuccess( next -> 
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

						})
						.onFailure(err -> finishedPromise.fail(err));
					}
					else
					{
						// TODO: Create its own Exception
						finishedPromise.fail(new Exception("Data Missing for task"));
					}

				}
			}
		})
		.onFailure(err -> finishedPromise.fail(err));

		return finishedPromise.future();
	}	

	private boolean isDataOkForTask(JsonObject taskData)
	{
		TaskObjectInstance currentTask = (TaskObjectInstance)getElement(taskData.getString("taskID"));
		HashSet<String> outputs = new HashSet<>();
		HashSet<String> persistent = new HashSet<>();
		JsonArray outputData = taskData.getJsonArray("outputData", new JsonArray());		
		for(int i = 0; i < outputData.size(); i++)
		{
			outputs.add(outputData.getJsonObject(i).getString("name"));
		}
		JsonArray currentOutputs = currentTask.getOutputs(); 
		for(int i = 0; i < currentOutputs.size(); i++)
		{
			if(!outputs.contains(currentOutputs.getString(i))) {
				return false;
			}
		}		
		LOGGER.debug(taskData.encodePrettily());		
		JsonArray persistentData = taskData.getJsonArray("persistentData", new JsonArray());
		LOGGER.debug(currentOutputs.encodePrettily());

		for(int i = 0; i < persistentData.size(); i++)
		{
			persistent.add(persistentData.getJsonObject(i).getString("name"));
		}
		JsonArray currentPersistent = currentTask.getPersistent(); 
		for(int i = 0; i < currentPersistent.size(); i++)
		{
			if(!persistent.contains(currentPersistent.getString(i))) {
				return false;
			}
		}
		LOGGER.debug(currentPersistent.encodePrettily());
		return true;
	}

	/**
	 * Retrieve the results part of the data provided when a Task is submitted. 
	 * @param taskData the data submitted when a taskk is finished.
	 * @return the results data to be stored in a db.
	 */
	public JsonObject getResultDataFromTaskData(JsonObject taskData)
	{
		JsonObject resultData = new JsonObject();
		resultData.put("task", taskData.getString("taskID"))
		.put("dbData", taskData.getJsonObject("resultData",new JsonObject()).getJsonArray("jsonData", new JsonArray()))
		.put("fileData", taskData.getJsonObject("resultData",new JsonObject()).getJsonArray("fileData", new JsonArray()));
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
	 * @return a list of the element IDs in this project
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
	public Future<String> getNextTask(String nextElementID, Participant user)
	{
		// if we don't get a nextElementID, we are done with the Project.
		if(nextElementID == null || "".equals(nextElementID) || "end".equals(nextElementID))
		{
			return Future.succeededFuture(null);
		}
		return elements.get(nextElementID).nextTask(user);
	}

	/**
	 * Start the project for the provided user.
	 * @param user
	 * @return A Future of the position the user will be at after they have started.
	 */
	public Future<String> startStudy(Participant user)
	{		

		Promise<String> startPositionPromise = Promise.promise();
		isActive()
		.onSuccess(active -> {
			if(!active)
			{
				startPositionPromise.fail(new ProjectIsInactiveException(name));
			}
			else
			{
				LOGGER.debug("Trying to start User at position: " + getStart());
				user.startStudy(getStart()).map(getStart())
				.onSuccess(startElement -> {		
					LOGGER.debug("StartElement id is: " + startElement);
					if(elements.get(startElement) instanceof TaskObjectInstance)
					{
						startPositionPromise.complete(startElement);
					}
					else
					{
						LOGGER.debug("Element at start is: " + elements.get(startElement));
						setNextStep(user)
						.onSuccess(taskID -> {
							user.startStudy(taskID)
							.onSuccess(success -> {
								startPositionPromise.complete(taskID);
							})
							.onFailure(err -> startPositionPromise.fail(err));
						})
						.onFailure(err -> startPositionPromise.fail(err));
					}
				})
				.onFailure(err -> startPositionPromise.fail(err));
			}
		})
		.onFailure(err -> startPositionPromise.fail(err));


		return startPositionPromise.future();
	}

	/**
	 * Proceed the user to the next step within this project (depending on Filters etc pp).
	 * This will return the position the user was set to if everything succeeds.
	 * @param participant The user that proceeds to the next step.
	 * @return A future of the next step instance ID, or null if this is participant is finished.
	 */
	public Future<String> setNextStep(Participant participant)
	{		
		Promise<String> nextStepPromise = Promise.promise(); 
		isActive()
		.onSuccess(active -> {
			if(!active)
			{
				nextStepPromise.fail(new ProjectIsInactiveException(name));
			}
			else
			{
				LOGGER.debug("Trying to set next step for user currently at position: " + participant.getStudyPosition());		
				ElementInstance current = getElement(participant.getStudyPosition());
				LOGGER.debug("Element is : " + current);
				current.nextTask(participant)
				.onSuccess(nextElement -> {
					LOGGER.debug("Next element is : " + nextElement);
					if("".equals(nextElement) || nextElement == null)
					{
						// This indicates we are done. 
						participant.setProjectPosition(null)
						.onSuccess(nextStep -> 
						{
							nextStepPromise.complete(nextStep);
						})
						.onFailure(err -> nextStepPromise.fail(err));

					}
					else
					{

						LOGGER.debug("Updating user position:" + current.getInstanceID() + " -> " + nextElement);		
						participant.setProjectPosition(nextElement)
						.onSuccess(nextStep -> 
						{
							nextStepPromise.complete(nextStep);
						})
						.onFailure(err -> nextStepPromise.fail(err));

					}
				}).onFailure(err -> nextStepPromise.fail(err));

			}
		})
		.onFailure(err -> nextStepPromise.fail(err));
		return nextStepPromise.future();

	}				

	/**
	 * Get the elementtype of instances
	 * @return the TargetElementType for instances
	 */
	public TargetElementType getElementType()
	{
		return TargetElementType.STUDY;
	}
	/**
	 * Get the collection in which projects Instances are stored.
	 * @return The Name of the MongoDB collection where this is stored.
	 */
	public String getTargetCollection()
	{
		return SoileConfigLoader.getCollectionName("studyCollection");
	}

	/**
	 * Get a list of Task instanceIDs with their respective name
	 * The returned JsonArray has elements of the following form:
	 * { "taskID" : "instanceIDOfTheTask" , "taskName" : "name of the task"}
	 * @return A jsonArray of the format described above
	 */
	public JsonArray getTasksInstancesWithNames()
	{
		JsonArray result = new JsonArray();
		for(ElementInstance element : elements.values())
		{
			if(element instanceof TaskObjectInstance)
			{
				result.add(new JsonObject().put("taskID",element.getInstanceID()).put("taskName",element.getName()));
			}
		}
		return result;
	}	

	/**
	 * Update the Study, returns the modification date set in the database.
	 * @param updateInformation the updating json information. 
	 */
	public Future<Long> updateStudy(JsonObject updateInformation)
	{
		Promise<Long> updatePromise = Promise.<Long>promise();
		isActive()
		.compose(active -> {
			if(active) 
			{				
				return Future.failedFuture("You cannot modify an active study");
			}
			else
			{
				return getParticipants();
			}
		})		
		.onSuccess(participants -> {
			boolean hasParticipants = participants.size() > 0;
			boolean projectChange = !getSourceUUID().equals(updateInformation.getValue("sourceUUID", getSourceUUID()));
			boolean versionChange = !getSourceVersion().equals(updateInformation.getValue("version", getSourceVersion()));
			boolean descChange = !description.equals(updateInformation.getValue("description", "")) || !shortDescription.equals(updateInformation.getValue("shortDescription", ""));
			// if it has it it must be equal, or this fails.
			if(hasParticipants && (projectChange || versionChange || descChange) )
			{			
				updatePromise.fail("Cannot change underlying project or descriptions for Studies with Participants");
				return;
			}
			
				
			checkShortCutAvailable(updateInformation.getString("shortcut",shortcut))
			.onSuccess(allowed -> {
				if(allowed)
				{
					setSourceUUID(updateInformation.getString("sourceUUID", getSourceUUID()));
					setSourceVersion(updateInformation.getString("version", getSourceVersion()));
					isPrivate = updateInformation.getBoolean("private", isPrivate);
					description = updateInformation.getString("description", description);
					shortDescription = updateInformation.getString("shortDescription", shortDescription);
					shortcut = updateInformation.getString("shortcut", shortcut);				
					setModifiedDate();				
					save(projectChange || versionChange)
					.onSuccess(res -> {
						updatePromise.complete(this.modifiedStamp);
					})
					.onFailure(err -> updatePromise.fail(err));
				}
				else
				{
					updatePromise.fail("Conflicing shortcuts");
				}
			})
			.onFailure(err -> updatePromise.fail(err));		
		})
		.onFailure(err -> updatePromise.fail(err));
				

		return updatePromise.future();			
	}

	protected abstract Future<Boolean> checkShortCutAvailable(String shortcut);

	/**
	 * This operation saves the Project. It should ensure that the data can be reconstructed by supplying what is returned 
	 * form this function to the load() function of this class.
	 * @param sourceProjectChanged indicating whether the source project has changed for this save call
	 * @return A JsonObject 
	 */
	public abstract Future<JsonObject> save(boolean sourceProjectChanged);

	/**
	 * Load all data that is necessary for the project. This function should work with the Data contained in the future provided by 
	 * the save function.  
	 * @param object The object to retrieve the data from. Can be specific to the Actual implementation used. e.g. can only contain one ID if loading from a DB or multiple fields if construction from multiple DBs. 
	 * @return A Future containing all data actually necessary for {@link Study} to reconstruct all necessary fields. 
	 */
	public abstract Future<JsonObject> load(JsonObject instanceInfo);

	/**
	 * Delete the project instance represented by this Object. Note: This must NOT delete the actual Project data, but only the data 
	 * associated with this run of the project.
	 * @return A future that contains the data that was associated with the Study. This function must NOT handle the deletion of it's 
	 * participants, that deletion should be handled at whatever place this Future is requested. 
	 */
	public abstract Future<JsonObject> delete();

	/**
	 * Reset the project (this will remove any access tokens and clear the participants array for this study). 
	 * @return A future that contains the participants array of this Study in order to remove their data. 
	 */
	public abstract Future<JsonArray> reset();

	/**
	 * Deactivate a project
	 * @return A Future that succeeded if the project was successfully deactivated (or was already inactive)
	 */
	public abstract Future<Void> deactivate();	

	/**
	 * Restart a project if it was deactivated. By default a project is active.
	 * @return A Future that succeeded if the project was successfully activated (or was already active)
	 */
	public abstract Future<Void> activate();

	/**
	 * Check, whether the current project is active.
	 * @return A Future o the activity status of this Study.
	 */
	public abstract Future<Boolean> isActive();

	/**
	 * Add a participant to the list of participants of this projects
	 * @param participant the participant to add
	 * @return A future that succeeded if the participant was successfully added
	 */
	public abstract Future<Void> addParticipant(Participant participant);

	/**
	 * Delete a participant from the list of participants of this projects
	 * Will return false, if the participant was not removed 
	 * @param p the participant to remove
	 * @ A Future that returning true, if the participant was removed, or false if it wasn't
	 */
	public abstract Future<Boolean> deleteParticipant(Participant participant);

	/**
	 * Get a list of Participants in the project 
	 * @return A Future of a Jsonarray with all participant ids.
	 */
	public abstract Future<JsonArray> getParticipants();

	/**
	 * Create a given number of Access tokens for this project. 
	 * @param count - the number of access tokens to create
	 * @return - a {@link JsonArray} of one-time access tokens that can be used to sign up for this project
	 */
	public abstract Future<JsonArray> createSignupTokens(int count);

	/**
	 * Create a general access token for this project. This token is reusable and not linked to a specific user.
	 * On signup, it will be exchanged against a individual access token.
	 * @return
	 */
	public abstract Future<String> createPermanentAccessToken();

	/**
	 * Get information about the tokens for this Study, master, access and used tokens
	 * @return a {@link Future} of a {@link JsonObject} with entries for masterToken, signupTokens( {@link JsonArray} ) and usedTokens ( {@link JsonArray} ) 
	 */
	public abstract Future<JsonObject> getTokenInformation();

	/**
	 * Use a token for the given project. If the provided token is a one-time token it will deactivate this token.
	 * @param token The token to use
	 * @return A successfull future, if the token is available.
	 */
	public abstract Future<Void> useToken(String token);

	/**
	 * 
	 */
	public abstract FieldSpecifications getUpdateableDBFields();

	/**
	 * Get the modification date of this study stored in the database.
	 * This is only updated in the save() method of the Study, and does not consider modifications of fields 
	 * which are accessed directly in the db and not stored on the project instance. 
	 */
	public abstract Future<Long> getStoredModificationDate();
	
	
	/**
	 *  This will search for the given filtrID in the filterPasses and update it to "one more"  
	 */
	public abstract Future<Integer> getFilterPassesAndAddOne(String filterID);
}
