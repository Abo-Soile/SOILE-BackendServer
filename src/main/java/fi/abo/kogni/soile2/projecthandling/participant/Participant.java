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
	 * @return a Map of all {@literal <}TaskUUID>.{@literal <}output> -> value mappings. 
	 */
	Map<String, Double> getOutputs();

	/**
	 * Get the array of persistent data.
	 * @return a {@link JsonArray} of {"name" : string, "value" : any} values, containing the persistent data of the user.  
	 */
	JsonObject getPersistentData();
	
	/**
	 * Get the position of this participant within its project
	 * @return the position in the current study
	 */
	String getStudyPosition();

	/**
	 * Get the ID of the project this participant is in.
	 * @return the ID of the study of this participant
	 */
	String getStudyID();

	/**
	 * Get all tasks done for a given experiment
	 * @param experimentID the experiment to check the status for
	 * @return a list of finished tasks in the given experiment
	 */
	List<String> getFinishedExpTasks(String experimentID);

	/**
	 * Get the Value that was assigned to this participant by a specific randomizer (if any is assigned).
	 * This will ensure, that if a a participant encounters a randomizer twice the value is not changed. 
	 * @param randomizerID the ID of the randomizer
	 * @return The value that was saved for this specific randomizer. The randomizer needs to know what this value is and it has to be depositable in a JsonArray.
	 */
	Object getValueForRandomizationGroup(String randomizerID);
	
	/**
	 * Set the Value that is assigned to this participant by a specific randomizer.
	 * This will ensure, that if a a participant encounters a randomizer twice the value is not changed. 
	 * @param randomizerID the ID of the randomizer 
	 * @param value the value to set
	 */
	void setValueForRandomizationGroup(String randomizerID, Object value);		
	
	/**
	 * Get all tasks done for a given experiment
	 * @param experimentID the experiment to set a finished task in
	 * @param TaskID the task to indicate as finished in the experiment
	 */
	void addFinishedExpTask(String experimentID, String TaskID);

	/**
	 * Add an experiment that is currently active (i.e. the participant is currently in this experiment).
	 * This is used to indicate to the experiment, whether we are currently in it, or whether we got a callback and should leave. 
	 * @param experimentID the experiment to add as active
	 */
	void addActiveExperiment(String experimentID);

	/**
	 * Check, whether an experiment is currently active 
	 * @param experimentID the experiment to check
	 * @return whether the experiment is active
	 */
	boolean isActiveExperiment(String experimentID);

	/**
	 * Remove an experiment from the currently active experiments. 
	 * @param experimentID the experiment to remove from those active
	 */
	void endActiveExperiment(String experimentID);

	/**
	 * Get the current step of this participant
	 * @return A {@link Future} indicating he current step of the {@link Participant}
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
	 * This will ONLY set it within this instance and will NOT update 
	 * @param taskID The taskID for the output
	 * @param outputName the name of the output in the task
	 * @param value the value of the output
	 * @param outputDate the Date the output was added (to potentially replace previous ones
	 */
	void addOutput(String taskID, String outputName, Number value, Date outputDate);
	
	/**
	 * Set outputs for the specified task This function should ALSO handle Database storage and update of the results.
	 * Note, that only the latest results of a task will be available in the outputs.
	 * @param taskID he task for which to store data
	 * @param taskOutputData The output data in the format as specified for TaskData.outputdata
	 * @return A {@link Future} indicating whether setting was successful
	 */
	Future<Void> setOutputDataForTask(String taskID, JsonArray taskOutputData);

	
	/**
	 * Add a piece of persistent data to the participant 
	 * @param outputName the name of the output in the task
	 * @param value the value of the output
	 */
	void addPersistentData(String outputName, Object value);
	
	/**
	 * Set a set of persistent data for the given user.
	 * Note, that only the latest results of a task will be available in the outputs.
	 * @param taskOutputData The output data in the format as specified for TaskData.outputdata
	 * @return a {@link Future} indicating whether setting was successful
	 */
	Future<Void> setPersistentData(JsonArray taskOutputData);	
	
	/**
	 * Add the result to this participant
	 * @param taskID The taskID for the result
	 * @param result the Result Json ({ task : id , dbData : resultID, fileData:  [ {fileformat : format, filename : name, targetid : datalakeID}]})
	 * @return a {@link Future} indicating whether setting was successful 
	 */
	Future<Void> addResult(String taskID, JsonObject result);

	/**
	 * Finish the current task for the participant
	 * @return a {@link Future} indicating whether setting was successful
	 */
	Future<Void> finishCurrentTask();

	
	/**
	 * Set the position (i.e. elementID of the Participant.
	 * @param taskID the id of the task to set the position to
	 * @return A {@link Future} of the task that was set
	 */
	Future<String> setProjectPosition(String taskID);

	/**
	 * Get the unique ID of this participant
	 * @return the id of the participant
	 */
	String getID();

	/**
	 * Start the project for this user, resetting the data
	 * @param taskID The task to set as the starting task
	 * @return Returning the position of this participant
	 */
	Future<Void> startStudy(String taskID);

	/**
	 * Get a Json of this participant that contains all necessary information to continue the project.
	 * i.e. This will contain, current position, outputData, finishedExperimentTasks, modifiedStamp, project
	 * @return a json with the Participant schema
	 */
	Future<JsonObject> toJson();

	/**
	 * Whether this is equal to another participant. They are equal if they have the same ID.
	 * @param other the other participant 
	 * @return whether the participants have the same ID
	 */
	boolean equals(Object other);
	
	/**
	 * Check whether the participant has finished its project.
	 * @return whether the participant has finished this project.
	 */
	boolean isFinished();

	
	/**
	 * Save this Participant updating all relevant Information except resultData.
	 * @return A {@link Future} of the ID of the participant if successful
	 */
	Future<String> save();
	
	/**
	 * Indicate whether this participant has a token associated with it.
	 * @return whether the Partiicpant has a token
	 */
	boolean hasToken();
	
	/**
	 * Get the token associated with this participant.
	 * @return the token used by this participant
	 */
	String getToken();

}
