package fi.abo.kogni.soile2.project.participant;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fi.abo.kogni.soile2.project.elements.TaskObjectInstance;
import fi.abo.kogni.soile2.project.instance.ProjectInstance;
import fi.abo.kogni.soile2.project.task.TaskFileResult;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * This class represents an Instance of a participant within a specific project.
 * Its fields reflect the fields of the Participant schema from the openAPI definition.
 * This instance has a reference to the ID of the participant entry in the participant database, but does NOT have
 * a link to the user that this participant refers to. It is thus disconnected from the user.
 * The connection to the user is only possible by accessing the user datbase and extracting the participatesIn field from that entry. 
 * @author Thomas Pfau
 *
 */
public abstract class Participant {

	/**
	 * This is the _id field from the spec.
	 */
	protected String uuid;
	protected ProjectInstance project;
	//protected JsonArray finished;
	protected String position;
	protected JsonObject outputData;
	protected JsonArray resultData;
	protected JsonArray activeExperiments;
	HashMap<String,List> finishedExperimentTasks;
	
	/**
	 * This map is a way to get output data faster than relying on outputData
	 */	
	private DatedDataMap<String,Double> outputMap;	
	
	private static DateFormat dateFormatter = new SimpleDateFormat("MM/dd/yyyy - HH:SS");
	/**
	 * A Participant is strictly associated with one project, thus we can construct it from the participant json and the project information. 	 
	 * @param data the data for the participant
	 * @param p the Project it is associatd with.
	 */
	public Participant(JsonObject data, ProjectInstance p)
	{
		// if this json comes from the db, we have an _id
		this.uuid = data.getString("_id");
		this.project = p;
		position = "";
		outputData = new JsonObject();
		resultData = new JsonArray();
		activeExperiments = new JsonArray();
		finishedExperimentTasks = new HashMap<>();
		setupParticipant(data);
	}


	private void setupParticipant(JsonObject participantInfo)
	{				
		//finished = participantInfo.getJsonArray("finished",new JsonArray());
		position = participantInfo.getString("position","0")  ;
		parseOutputData(participantInfo.getJsonArray("outputData", new JsonArray()));	
		resultData = participantInfo.getJsonArray("resultData", new JsonArray());	
		activeExperiments = participantInfo.getJsonArray("activeExperiments",new JsonArray());
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
			JsonArray outputs = outputElement.getJsonArray("outputs");
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
	
	/**
	 * Encode the output data into a {@link JsonArray}.
	 * @return The jsonArray of Output data as required by the database
	 */
	private JsonArray encodeOutputDataForDB()
	{
		JsonArray currentOutputs = new JsonArray();
		for(String taskName : outputData.fieldNames())
		{
			JsonArray taskData = outputData.getJsonArray(taskName);
			JsonObject OutputTaskElement = new JsonObject();
			OutputTaskElement.put("task", taskName);
			OutputTaskElement.put("outputs", taskData);			
			currentOutputs.add(OutputTaskElement);
		}
		return currentOutputs;
	}



	/**
	 * Get the map of Outputs to provide to a Math-parser. This is generated once per user on request and only updated afterwards.
	 * @return a Map of all t<TaskUUID>.<output> -> value mappings. 
	 */
	public Map<String,Double> getOutputs()
	{		
		return outputMap.getNewestData();
	}
	/**
	 * Add the output to this participant, we try not to recalculate this....
	 * @param taskID The taskID for the output
	 * @param outputName the name of the output in the task
	 * @param value the value of the output
	 */
	public void addOutput(String taskID, String outputName, Number value)
	{
		addOutput(taskID,outputName,value,new Date());
	}

	/**
	 * Add the output to this participant, we try not to recalculate this....
	 * @param taskID The taskID for the output
	 * @param outputName the name of the output in the task
	 * @param value the value of the output
	 */
	public void addOutput(String taskID, String outputName, Number value, Date outputDate)
	{
		if(outputMap == null)
		{
			outputMap = new DatedDataMap<>();
		}		
		outputMap.addDatedEntry(taskID + "." + outputName, new TimeStampedData<Double>(value.doubleValue()));		

		if(!outputData.containsKey(taskID))
		{
			outputData.put(taskID, new JsonArray());
		}
		outputData.getJsonArray(taskID).add(new JsonObject().put("name",outputName).put("value",value).put("timestamp", dateFormatter.format(outputDate)));		
	}
	/**
	 * Get whether a a specific task is finished for this participant
	 * @param taskID the Task to check
	 * @return whether the task is finished or not.
	 */
//	public boolean finished(String taskID)
//	{
//		return finished.contains(taskID);
//	}

	/**
	 * Get the position of this participant within its project
	 * @return
	 */
	public String getProjectPosition()
	{
		return position;
	}

	/**
	 * Get all tasks done for a given experiment
	 * @return
	 */
	public List<String> getFinishedExpTasks(String experimentID)
	{		
		return finishedExperimentTasks.get(experimentID);
	}
	
	/**
	 * Get all tasks done for a given experiment
	 * @return
	 */
	public void addFinishedExpTask(String experimentID, String TaskID)
	{		
		finishedExperimentTasks.get(experimentID).add(TaskID);
	}
	
	/**
	 * Add an experiment that is currently active (i.e. the participant is currently in this experiment).
	 * This is used to indicate to the experiment, whether we are currently in it, or whether we got a callback and should leave. 
	 * @param expeirmentID
	 */
	public void addActiveExperiment(String experimentID)
	{
		activeExperiments.add(experimentID);
		finishedExperimentTasks.put(experimentID, new LinkedList<String>());
	}
	
	/**
	 * Check, whether an experimen is currently active 
	 * @param expeirmentID
	 */
	public boolean isActiveExperiment(String experimentID)
	{
		return activeExperiments.contains(experimentID);
	}
	
	
	/**
	 * Remove an experiment from the currently active experiments. 
	 * @param expeirmentID
	 */
	public void endActiveExperiment(String experimentID)
	{
		activeExperiments.remove(experimentID);
		finishedExperimentTasks.remove(experimentID);
	}
	
	/**
	 * Add the result to this participant
	 * @param taskID The taskID for the output
	 * @param outputName the name of the output in the task
	 * @param value the value of the output
	 */
	public void addResult(String taskID, JsonObject result)
	{

	}

	/**
	 * Set outputs for the specified task 
	 * @param taskID he task for which to store data
	 * @param taskOutputData The output data in the format as specified for TaskData.outputdata
	 */
	public void setOutputDataForTask(String taskID, JsonArray taskOutputData)
	{		
		if(taskOutputData != null)
		{
			for(Object output : taskOutputData)
			{
				JsonObject dataElement = (JsonObject) output;				
				addOutput(taskID, dataElement.getString("name"), dataElement.getNumber("value"));
			}
		}	
	}

	/**
	 * Set outputs for the current task 
	 * @param taskOutputData The output data in the format as specified for TaskData.outputdata
	 */
	public void setOutputDataForCurrentTask(JsonArray taskOutputData)
	{		
		if(taskOutputData != null)
		{
			for(Object output : taskOutputData)
			{
				JsonObject dataElement = (JsonObject) output;				
				addOutput(position, dataElement.getString("name"), dataElement.getNumber("value"));
			}
		}	
	}


	/**
	 * Update the results information.   
	 * @param taskID the task for which to store data
	 * @param resultData The data of the outputs for this task. A {@link JsonArray} containing 
	 * 					 JsonObjects with the field 'format' which can be db or file and the field 'data'
	 * 					 containing either a json object with depth one and elements representing numbers or text
	 * 					 or the fields filename and fileformat for the name of the file and the format of the file associated with this result..
	 */
	public Future<Void> setResultDataForCurrentTask(JsonObject resultData)
	{	
		Promise<Void> updatePromise = Promise.<Void>promise();
		// we fail the future and don't do anything if the task is already finished.
		// check, that this Participant hasn't already got result data for this task
		JsonObject taskData = new JsonObject().put("task", position);
		taskData.put("timestamp", dateFormatter.format(new Date()));
		JsonArray jsonData = resultData.getJsonArray("jsonData", new JsonArray());
		if(!jsonData.isEmpty())
		{
			saveJsonResults(jsonData).onSuccess( id -> 
			{
				taskData.put("dbData", id);
				addFileResults(taskData, resultData);
				this.resultData.add(taskData);
				updatePromise.complete();
			}).onFailure(fail -> {
				updatePromise.fail(fail.getCause());
			});
		}
		else
		{
			addFileResults(taskData, resultData);
			this.resultData.add(taskData);
			updatePromise.complete();
		}

		return updatePromise.future();

	}		

	public void addFileResults(JsonObject taskData, JsonObject resultData)
	{
		if(!resultData.getJsonArray("fileData", new JsonArray()).isEmpty())
		{
			taskData.put("fileData", resultData.getJsonArray("fileData"));
		}		
	}

	/**
	 * Save after finishing a task.
	 * @param taskID The just completed task 
	 */
	public void finishCurrentTask()
	{
		// All data processed. Save this participant.
		save();
	}
	
	public void setProjectPosition(String taskID)
	{		
		position = taskID;	
	}

	public String getID()
	{
		return this.uuid;
	}	
	/**
	 * Get the File Results for a specific task for this participant
	 * @param taskID the task to retrieve the file results for
	 * @return a {@link Set} of {@link TaskFileResult}s representing all results stored for this user.
	 */
	public Set<TaskFileResult> getFileResultsForTask(String taskID)
	{
		Set<TaskFileResult> fileNames = new HashSet<TaskFileResult>();

		for(Object taskResult : resultData)
		{
			JsonObject task = ((JsonObject) taskResult);

			if(task.getString("task").equals(taskID))
			{
				for(Object fileResult : task.getJsonArray("fileData",new JsonArray()))
				{
					JsonObject fileData = (JsonObject) fileResult;
					TaskFileResult result = new TaskFileResult(fileData.getString("fileid"),
							fileData.getString("filename"),
							fileData.getString("fileformat"),
							taskID,
							this.uuid);

					fileNames.add(result);
				}
			}
		}

		return fileNames;
	}


	/**
	 * Get All tasks that have files for this {@link Participant} 
	 * @param taskID the task to retrieve the file results for
	 * @return a {@link Set} of {@link TaskFileResult}s representing all results stored for this user.
	 */
	public Set<String> getTasksWithFiles()
	{
		Set<String> taskIDs = new HashSet<String>();
		for(Object o : resultData)
		{
			JsonObject resultObject = (JsonObject) o;
			if(resultObject.containsKey("fileData"))
			{
				taskIDs.add(resultObject.getString("task"));
			}
		}
		return taskIDs;
	}

	/**
	 * Get all File Results for this participant
	 * @return a set of all File Results
	 */
	public Set<TaskFileResult> getFileResults()
	{
		Set<TaskFileResult> fileResults = new HashSet<TaskFileResult>();		
		for(String taskID : getTasksWithFiles())
		{
			fileResults.addAll(getFileResultsForTask(taskID));
		}		
		return fileResults;
	}

	/**
	 * Get a Json representation of this Participant that adheres to the OPEAPI Participant schema
	 * @return a json with the Participant schema
	 */
	public JsonObject toJson()
	{		
		JsonObject current = new JsonObject();
		current.put("_id", uuid)
		.put("position", position)
		.put("project", project.getID().toString())
		.put("outputData", encodeOutputDataForDB())
		.put("resultdata", resultData)
		.put("activeExperiments", this.activeExperiments)
		.put("finishedExperimentTasks", convertFinishedTasks());
		return current;
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
	
	public ProjectInstance getProject()
	{
		return this.project;
	}
	
	public boolean equals(Object other)
	{
		if( other instanceof Participant)
		{
			return this.toJson().equals(((Participant)other).toJson());
		}
		else
		{
			return false;
		}
	}
	
	public abstract Future<String> save();
	
	public abstract Future<String> saveJsonResults(JsonArray results);
}
