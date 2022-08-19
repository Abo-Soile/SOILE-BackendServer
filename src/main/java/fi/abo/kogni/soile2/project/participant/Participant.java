package fi.abo.kogni.soile2.project.participant;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fi.abo.kogni.soile2.project.instance.ProjectInstance;
import fi.abo.kogni.soile2.project.participant.ParticipantManager;
import fi.abo.kogni.soile2.project.resultDB.ResultDBHandler;
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
	protected ResultDBHandler resultHandler;
	protected ProjectInstance project;
	protected JsonArray finished;
	protected String position;
	protected JsonObject outputData;
	protected JsonArray resultData;
	/**
	 * This map is a way to get output data faster than relying on outputData
	 */
	private HashMap<String,Double> outputMap;
	
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
		setupParticipant(data);
	}


	private void setupParticipant(JsonObject participantInfo)
	{				
		finished = participantInfo.getJsonArray("finished",new JsonArray());
		position = participantInfo.getString("position","0")  ;
		parseOutputData(participantInfo.getJsonArray("outputData", new JsonArray()));	
		resultData = participantInfo.getJsonArray("resultData", new JsonArray());		
		
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
				addOutput(task, taskOutputData.getString("name"), taskOutputData.getNumber("value"));
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
			JsonObject taskData = outputData.getJsonObject(taskName);
			JsonObject OutputTaskElement = new JsonObject();
			OutputTaskElement.put("task", taskName);
			JsonArray outputs = new JsonArray();
			for(String outputName : taskData.fieldNames()) {
				outputs.add(new JsonObject().put("name", outputName).put("value", taskData.getNumber(outputName)));												
			}
			OutputTaskElement.put("outputData", OutputTaskElement);
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
		if(outputMap == null)
		{
			outputMap = new HashMap<String, Double>();
			for(String taskID : outputData.fieldNames())
			{
				JsonObject taskData = outputData.getJsonObject(taskID);
				for(String output : taskData.fieldNames())
				{
					outputMap.put(taskID + "." + output, taskData.getDouble(output));
				}
			}
		}
		return outputMap;
	}
	/**
	 * Add the output to this participant, we try not to recalculate this....
	 * @param taskID The taskID for the output
	 * @param outputName the name of the output in the task
	 * @param value the value of the output
	 */
	public void addOutput(String taskID, String outputName, Number value)
	{
		if(outputMap != null)
		{
			outputMap = new HashMap<String, Double>();
			outputMap.put(taskID + "." + outputName, value.doubleValue());		
		}
		if(!outputData.containsKey(taskID))
		{
			outputData.put(taskID, new JsonObject());
		}
		outputData.getJsonObject(taskID).put(outputName, value);		
	}

	/**
	 * Get whether a a specific task is finished for this participant
	 * @param taskID the Task to check
	 * @return whether the task is finished or not.
	 */
	public boolean finished(String taskID)
	{
		return finished.contains(taskID);
	}

	/**
	 * Get the position of this participant within its project
	 * @return
	 */
	public String getProjectPosition()
	{
		return position;
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
	 * Update the results information. This needs to update before the task is finished.    
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
		if(finished.contains(getProjectPosition()))
		{
			updatePromise.fail("Trying to set Results for a finished Task");
			return updatePromise.future();
		}		
		// check, that this Participant hasn't already got result data for this task
		JsonObject taskData = new JsonObject().put("task", position);
		JsonArray jsonData = resultData.getJsonArray("jsonData", new JsonArray());
		if(!jsonData.isEmpty())
		{
			resultHandler.createResults(jsonData).onSuccess( id -> 
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
	 * Finish a specific task, and add it to the finished tasks.
	 * @param taskID The just completed task 
	 */
	public void finishCurrentTask()
	{
		finished.add(getProjectPosition());
		// All data processed. Save this participant.
		save();
	}
	public JsonArray getFinishedTasks()
	{
		return new JsonArray().addAll(finished);
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
		JsonArray finishedTasks = new JsonArray().addAll(finished);
		current.put("_id", uuid)
		.put("position", position)
		.put("finished", finishedTasks)
		.put("project", project.getID().toString())
		.put("outputData", encodeOutputDataForDB())
		.put("resultdata", resultData);		
		return current;
	}
	
	public abstract Future<String> save();

}
