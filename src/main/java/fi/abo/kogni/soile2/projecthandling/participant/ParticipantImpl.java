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
import fi.abo.kogni.soile2.datamanagement.utils.TimeStampedData;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.impl.ProjectInstanceManager;
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
		protected String uuid;
		//protected JsonArray finished;
		protected String position;
		protected JsonObject outputData;
		protected JsonArray steps;
		protected JsonArray activeExperiments;
		HashMap<String,List> finishedExperimentTasks;
		protected int currentStep;
		protected boolean finished;
		/**
		 * This map is a way to get output data faster than relying on outputData
		 */	
		private DatedDataMap<String,Double> outputMap;	
		static final Logger LOGGER = LogManager.getLogger(ParticipantImpl.class);
		private static DateFormat dateFormatter = new SimpleDateFormat("MM/dd/yyyy - HH:SS");
		/**
		 * A Participant is strictly associated with one project, thus we can construct it from the participant json and the project information. 	 
		 * @param data the data for the participant
		 * @param p the Project it is associatd with.
		 */
		public ParticipantImpl(JsonObject data)
		{
			// if this json comes from the db, we have an _id						
			outputData = new JsonObject();
			finishedExperimentTasks = new HashMap<>();									
			setupParticipant(data);
		}


		protected void setupParticipant(JsonObject participantInfo)
		{		
			LOGGER.debug(participantInfo.encodePrettily());
			uuid = participantInfo.getString("_id");
			position = participantInfo.getString("position","0");
			currentStep = participantInfo.getInteger("currentStep",0);
			steps = participantInfo.getJsonArray("steps",new JsonArray());
			finished = participantInfo.getBoolean("finished",false);
			parseOutputData(participantInfo.getJsonArray("outputData", new JsonArray()));	
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
		@Override
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
		@Override
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
		@Override
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
//		public boolean finished(String taskID)
//		{
//			return finished.contains(taskID);
//		}

		/**
		 * Get the position of this participant within its project
		 * @return
		 */
		@Override
		public String getProjectPosition()
		{
			return position;
		}

		/**
		 * Get all tasks done for a given experiment
		 * @return
		 */
		@Override
		public List<String> getFinishedExpTasks(String experimentID)
		{		
			return finishedExperimentTasks.get(experimentID);
		}
		
		/**
		 * Get all tasks done for a given experiment
		 * @return
		 */
		@Override
		public void addFinishedExpTask(String experimentID, String TaskID)
		{		
			finishedExperimentTasks.get(experimentID).add(TaskID);
		}
		
		/**
		 * Add an experiment that is currently active (i.e. the participant is currently in this experiment).
		 * This is used to indicate to the experiment, whether we are currently in it, or whether we got a callback and should leave. 
		 * @param expeirmentID
		 */
		@Override
		public void addActiveExperiment(String experimentID)
		{
			activeExperiments.add(experimentID);
			finishedExperimentTasks.put(experimentID, new LinkedList<String>());
		}
		
		/**
		 * Check, whether an experimen is currently active 
		 * @param expeirmentID
		 */
		@Override
		public boolean isActiveExperiment(String experimentID)
		{
			return activeExperiments.contains(experimentID);
		}
		
		
		/**
		 * Remove an experiment from the currently active experiments. 
		 * @param expeirmentID
		 */
		@Override
		public void endActiveExperiment(String experimentID)
		{
			activeExperiments.remove(experimentID);
			finishedExperimentTasks.remove(experimentID);
		}
		
		/**
		 * Add the result to this participant
		 * @param taskID The taskID for the result
		 * @param result the Result Json ({ task : id , dbData : resultID, fileData:  [ {fileformat : format, filename : name, targetid : datalakeID}]}) 
		 */
		@Override
		public abstract Future<Void> addResult(String taskID, JsonObject result);

		/**
		 * Set outputs for the specified task 
		 * @param taskID he task for which to store data
		 * @param taskOutputData The output data in the format as specified for TaskData.outputdata
		 */
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

		/**
		 * Save after finishing a task.
		 * @param taskID The just completed task
		 */
		@Override
		public Future<Void> finishCurrentTask()
		{					
			// All data processed. Save this participant.
			return save().mapEmpty();
		}
		
		@Override
		public Future<String> setProjectPosition(String taskID)
		{		
			if(taskID == null)
			{
				this.finished = true;				
				return save().map(taskID).onFailure(err ->
				{
					// reset the change.
					this.finished = false;
				});
			}
			String currentPosition = position;		
			position = taskID;
			currentStep += 1;
			steps.add(taskID);
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
			return this.uuid;
		}	

		/**
		 * Get a Json representation of this Participant that adheres to the OPEANPI Participant schema
		 * @return a json with the Participant schema
		 */
		@Override
		public Future<JsonObject> toJson()
		{		
			JsonObject current = new JsonObject();
			current.put("_id", uuid)
			.put("currentStep", currentStep)
			.put("steps", steps)
			.put("finished", finished)
			.put("position", position)			
			.put("activeExperiments", this.activeExperiments)
			.put("finishedExperimentTasks", convertFinishedTasks());
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
		public abstract Future<String> save();

		@Override
		public abstract Future<Integer> getCurrentStep();
		
}
