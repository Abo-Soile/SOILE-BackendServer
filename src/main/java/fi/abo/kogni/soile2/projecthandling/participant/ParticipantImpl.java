package fi.abo.kogni.soile2.projecthandling.participant;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fi.abo.kogni.soile2.datamanagement.utils.DatedDataMap;
import fi.abo.kogni.soile2.datamanagement.utils.OutputMap;
import fi.abo.kogni.soile2.datamanagement.utils.TimeStampedData;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.Study;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * This class represents an Instance of a participant within a specific project.
 * Its fields reflect the fields of the Participant schema from the openAPI definition.
 * This instance has a reference to the ID of the participant entry in the participant database, but does NOT have
 * a link to the user that this participant refers to. It is thus disconnected from the user.
 * The connection to the user is only possible by accessing the user datbase and extracting the participatesIn field from that entry. 	 * 
 * @author Thomas Pfau
 *
 */
public abstract class ParticipantImpl implements Participant{



	/**
	 * This is the _id field from the spec.
	 */
	protected String UUID;
	/**
	 * The position of this participant in its study
	 */
	protected String position;
	/**
	 * The order of the steps of this participant
	 */
	protected JsonArray steps;
	/**
	 * A List of active experiments (i.e. a hierarchy)
	 */
	protected JsonArray activeExperiments;
	/**
	 * Map of finished tasks in active experiments
	 */
	HashMap<String,List<String>> finishedExperimentTasks;
	/**
	 * the current step number
	 */
	protected int currentStep;
	/**
	 * Whether this participant is finished with its {@link Study}
	 */
	protected boolean finished;
	/**
	 * The project/study id this participant is in
	 */
	protected String project;
	/**
	 * A JsonObject that indicates which random groups where assigned to this participant by Randomizers
	 */
	protected JsonObject assignedRandomGroups; 
	/**
	 * This map is a way to get output data faster than relying on outputData
	 */	
	protected DatedDataMap<String,Double> outputMap;
	/**
	 * the Map of persistent data that can be used in tasks for this {@link Participant}
	 */
	protected JsonObject persistentMap;
	static final Logger LOGGER = LogManager.getLogger(ParticipantImpl.class);
	private static DateFormat dateFormatter = new SimpleDateFormat("MM/dd/yyyy - HH:SS");

	/**
	 * A Participant is strictly associated with one project, thus we can construct it from the participant json and the project information. 	 
	 * @param data the data for the participant 
	 */
	public ParticipantImpl(JsonObject data)
	{
		// if this json comes from the db, we have an _id						
		//			outputData = new JsonObject();
		finishedExperimentTasks = new HashMap<>();									
		setupParticipant(data);
	}

	
	@Override
	public String getStudyID()
	{
		return project;
	}
	/**
	 * Set up a participant based on JsonData
	 * @param participantInfo the data of the participant as a json
	 */
	protected void setupParticipant(JsonObject participantInfo)
	{		
		LOGGER.debug(participantInfo.encodePrettily());
		UUID = participantInfo.getString("_id");
		position = participantInfo.getString("position","0");
		currentStep = participantInfo.getInteger("currentStep",0);
		steps = participantInfo.getJsonArray("steps",new JsonArray());
		finished = participantInfo.getBoolean("finished",false);
		parseOutputData(participantInfo.getJsonArray("outputData", new JsonArray()));
		parsePersistentData(participantInfo.getJsonArray("persistentData", new JsonArray()));
		activeExperiments = participantInfo.getJsonArray("activeExperiments",new JsonArray());
		project = participantInfo.getString("project", "");
		assignedRandomGroups = participantInfo.getJsonObject("assignedRandomGroups", new JsonObject());
		for(Object o : participantInfo.getJsonArray("finishedExperimentTasks",new JsonArray()))
		{			
			JsonObject jo = (JsonObject) o;
			LinkedList<String> tasks = new LinkedList<>(); 
			for(Object task : jo.getJsonArray("tasks"))
			{
				tasks.add(task.toString());
			}
			finishedExperimentTasks.put(jo.getString("experimentID"), tasks);
		}

	}
	/**
	 *  For faster access, we convert the stored output data into a more quickly accessible format (which unfortunately cannot be well defined in OpenAPI.
	 * @param data the outputData (i.e. results) for this participant
	 */
	private void parseOutputData(JsonArray data)
	{		
		for(Object output : data)
		{
			JsonObject outputElement = (JsonObject)output;
			String task = outputElement.getString("task");
			JsonArray outputs = outputElement.getJsonArray("outputs", new JsonArray());
			for( Object taskOutput : outputs)
			{
				JsonObject taskOutputData = (JsonObject) taskOutput;
				try
				{
					addOutput(task, taskOutputData.getString("name"), taskOutputData.getNumber("value"),dateFormatter.parse(taskOutputData.getString("value")));
				}
				catch(ParseException e)
				{					
					//TODO: LOG THE ISSUE.
					// we will add the output, but put it to the back. 
					addOutput(task, taskOutputData.getString("name"), taskOutputData.getNumber("value"),new Date(Long.MIN_VALUE));
				}
			}
		}
	}					


	@Override
	public Map<String,Double> getOutputs()
	{		
		return outputMap.getNewestData();
	}
	@Override
	public void addOutput(String taskID, String outputName, Number value)
	{
		addOutput(taskID,outputName,value,new Date());
	}

	@Override
	public void addOutput(String taskID, String outputName, Number value, Date outputDate)
	{
		if(outputMap == null)
		{
			outputMap = new DatedDataMap<>();
		}		
		outputMap.addDatedEntry(taskID + "." + outputName, new TimeStampedData<Double>(value.doubleValue()));							

	}
	
	
	/**
	 *  For faster access, we convert the stored output data into a more quickly accessible format (which unfortunately cannot be well defined in OpenAPI.
	 * @param data the outputData (i.e. results) for this participant
	 */
	private void parsePersistentData(JsonArray data)
	{		
		for(Object output : data)
		{
			JsonObject outputElement = (JsonObject)output;
			addPersistentData(outputElement.getString("name"), outputElement.getNumber("value"));
		}
	}		

	
	@Override
	public void addPersistentData(String outputName, Object value)
	{
		if(persistentMap == null)
		{
			persistentMap = new OutputMap();
		}		
		persistentMap.put(outputName, value);							

	}
	
	@Override
	public Future<Void> setPersistentData(JsonArray persistentData)
	{		
		if(persistentData != null)
		{
			for(Object output : persistentData)
			{
				JsonObject dataElement = (JsonObject) output;				
				addPersistentData(dataElement.getString("name"), dataElement.getNumber("value"));
			}
		}	
		return Future.succeededFuture();
	}

	@Override
	public JsonObject getPersistentData() {
		// TODO Auto-generated method stub
		return persistentMap == null ? new JsonObject() : persistentMap;
	}
		
	@Override
	public String getStudyPosition()
	{
		return position;
	}
	
	@Override
	public List<String> getFinishedExpTasks(String experimentID)
	{		
		return finishedExperimentTasks.get(experimentID);
	}
	
	@Override
	public void addFinishedExpTask(String experimentID, String TaskID)
	{		
		finishedExperimentTasks.get(experimentID).add(TaskID);
	}

	@Override
	public void addActiveExperiment(String experimentID)
	{
		activeExperiments.add(experimentID);
		finishedExperimentTasks.put(experimentID, new LinkedList<String>());
	}

	@Override
	public boolean isActiveExperiment(String experimentID)
	{
		return activeExperiments.contains(experimentID);
	}


	@Override
	public void endActiveExperiment(String experimentID)
	{
		activeExperiments.remove(experimentID);
		finishedExperimentTasks.remove(experimentID);
	}

	@Override
	public abstract Future<Void> addResult(String taskID, JsonObject result);

	@Override
	public Future<Void> setOutputDataForTask(String taskID, JsonArray taskOutputData)
	{		
		if(taskOutputData != null)
		{
			for(Object output : taskOutputData)
			{
				JsonObject dataElement = (JsonObject) output;				
				addOutput(taskID, dataElement.getString("name"), dataElement.getNumber("value"));
			}
		}	
		return Future.succeededFuture();
	}

	@Override
	public Future<Void> finishCurrentTask()
	{					
		// All data processed. Save this participant.
		return save().mapEmpty();
	}

	@Override 
	public Future<Void> startStudy(String taskID)
	{				
		position = taskID;
		currentStep = 1;
		steps = new JsonArray();
		finished = false;
		activeExperiments = new JsonArray();
		finishedExperimentTasks = new HashMap<>();
		return resetParticipantResults();						
	}

	@Override
	public Future<String> setProjectPosition(String taskID)
	{		
		if(taskID == null)
		{
			this.finished = true;
			if(this.position != null && !this.position.equals(""))
			{
				// this is finished.
				steps.add(position);
				this.position = null;
			}
			return save().map(taskID).onFailure(err ->
			{
				// reset the change.
				this.finished = false;
			});
		}
		LOGGER.debug("Adding step " + taskID);
		String currentPosition = position;		
		position = taskID;
		currentStep += 1;
		// the last step is finished, so we add it to the steps.
		steps.add(currentPosition);
		return save().map(taskID).onFailure(err ->
		{
			// reset the change.
			position = currentPosition;
			steps.remove(steps.size()-1);
			currentStep -=1;
		});
	}

	@Override
	public String getID()
	{
		return this.UUID;
	}	

	@Override
	public Future<JsonObject> toJson()
	{		
		JsonObject current = new JsonObject();
		current.put("_id", UUID)
		.put("currentStep", currentStep)
		.put("steps", steps)
		.put("finished", finished)
		.put("position", position)			
		.put("activeExperiments", this.activeExperiments)
		.put("project", project)
		.put("finishedExperimentTasks", convertFinishedTasks())
		.put("assignedRandomGroups", assignedRandomGroups);
		return Future.succeededFuture(current);
	}

	private JsonArray convertFinishedTasks()
	{
		JsonArray result = new JsonArray();
		for(String exp : finishedExperimentTasks.keySet())
		{
			JsonObject data = new JsonObject();			
			data.put("experimentID", exp);
			data.put("tasks", new JsonArray(finishedExperimentTasks.get(exp)));
			result.add(data);
		}
		return result;
	}	
	@Override
	public boolean equals(Object other)
	{
		if( other instanceof ParticipantImpl)
		{
			return this.getID().equals(((ParticipantImpl)other).getID());
		}
		else
		{
			return false;
		}
	}

	@Override
	public boolean isFinished()
	{
		return finished;
	}

	@Override
	public abstract Future<String> save();

	@Override
	public abstract Future<Integer> getCurrentStep();

	/**
	 * Reset the results of the participant and save it. 
	 * @return A succesfull future if the results where resetted
	 */
	public Future<Void> resetParticipantResults()
	{					
		return resetOutputs().compose(this::save).mapEmpty();
	}

	private Future<Void> save(Void unused)
	{								
		return save().mapEmpty();
	}
	/**
	 * Reset the outputs of the participant, by defa
	 * @return A succesfull future if the results where resetted
	 */
	public Future<Void> resetOutputs()
	{
		outputMap = new DatedDataMap<>();
		return Future.succeededFuture();
	}

	/**
	 * Indicate whether this participant is a authenticated via a token.
	 * @return whether this is a token user or not.
	 */
	public boolean hasToken()
	{
		return false;
	}

	/**
	 * Get this participants token (if it has one)
	 */
	public String getToken()
	{
		return null;
	}
	
	@Override
	public void setValueForRandomizationGroup(String randomizerID, Object value) {
		this.assignedRandomGroups.put(randomizerID, value);
	}
	
	@Override
	public Object getValueForRandomizationGroup(String randomizerID) {
		return this.assignedRandomGroups.getValue(randomizerID, null);
	}
}
