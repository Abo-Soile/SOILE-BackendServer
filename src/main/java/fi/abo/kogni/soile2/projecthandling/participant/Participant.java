package fi.abo.kogni.soile2.projecthandling.participant;

import java.util.Date;
import java.util.List;
import java.util.Map;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;


/**
 * This interface represents a participant as needed while running an experiment.
 * An implementation of this interface needs to take care of STORING information on a Participant (e.g. result data), and retrieval of task-relevant data
 * (e.g. outputs that might be needed by filters).
 * It does NOT need to take care of retrieving other data of the participant, and thus doesn't need to store that data.
 * We assume, that an instance of this Interface will be loaded at the beginning of a query. As such, it can be considered as unmutable during one call.
 * When handling it, you need to ensure that it is refreshed if necessary if it has been changed.
 * @author Thomas Pfau
 *
 */
public interface Participant {
	/**
	 * Get the map of Outputs to provide to a Math-parser. This is generated once per user on request and only updated afterwards.
	 * @return a Map of all t<TaskUUID>.<output> -> value mappings. 
	 */
	Map<String, Double> getOutputs();

	/**
	 * Get the position of this participant within its project
	 * @return
	 */
	String getProjectPosition();

	/**
	 * Get all tasks done for a given experiment
	 * @return
	 */
	List<String> getFinishedExpTasks(String experimentID);

	/**
	 * Get all tasks done for a given experiment
	 * @return
	 */
	void addFinishedExpTask(String experimentID, String TaskID);

	/**
	 * Add an experiment that is currently active (i.e. the participant is currently in this experiment).
	 * This is used to indicate to the experiment, whether we are currently in it, or whether we got a callback and should leave. 
	 * @param expeirmentID
	 */
	void addActiveExperiment(String experimentID);

	/**
	 * Check, whether an experimen is currently active 
	 * @param expeirmentID
	 */
	boolean isActiveExperiment(String experimentID);

	/**
	 * Remove an experiment from the currently active experiments. 
	 * @param expeirmentID
	 */
	void endActiveExperiment(String experimentID);

	/**
	 * Get the current step of this participant
	 */
	Future<Integer> getCurrentStep();	
	
	/**
	 * Add the output to this participant, we try not to recalculate this....
	 * @param taskID The taskID for the output
	 * @param outputName the name of the output in the task
	 * @param value the value of the output
	 */
	void addOutput(String taskID, String outputName, Number value);

	/**
		 * Add the output to this participant, we try not to recalculate this....
		 * @param taskID The taskID for the output
		 * @param outputName the name of the output in the task
		 * @param value the value of the output
		 */
	void addOutput(String taskID, String outputName, Number value, Date outputDate);
	
	/**
	 * Set outputs for the specified task 
	 * @param taskID he task for which to store data
	 * @param taskOutputData The output data in the format as specified for TaskData.outputdata
	 */
	Future<Void> setOutputDataForTask(String taskID, JsonArray taskOutputData);

	
	/**
	 * Add the result to this participant
	 * @param taskID The taskID for the result
	 * @param result the Result Json ({ task : id , dbData : resultID, fileData:  [ {fileformat : format, filename : name, targetid : datalakeID}]}) 
	 */
	Future<Void> addResult(String taskID, JsonObject result);

	/**
	 * ReturnToStep
	 * @param taskID The just completed task
	 */
	Future<Void> finishCurrentTask();

	
	/**
	 * Set the position (i.e. elementID of the Participant.
	 * @param taskID
	 * 
	 */
	Future<String> setProjectPosition(String taskID);

	/**
	 * Get the unique ID of this participant
	 * @return
	 */
	String getID();



	/**
	 * Get a Json of this participant that contains all necessary information to continue the project.
	 * i.e. This will contain, current position, outputData, finishedExperimentTasks, modifiedStamp, project
	 * @return a json with the Participant schema
	 */
	Future<JsonObject> toJson();

	boolean equals(Object other);

	/**
	 * Save this Participant updating all relevant Information except resultData.
	 * @return
	 */
	Future<String> save();
	
}
