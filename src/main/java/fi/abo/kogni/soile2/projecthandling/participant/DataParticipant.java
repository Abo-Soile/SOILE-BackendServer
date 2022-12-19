package fi.abo.kogni.soile2.projecthandling.participant;

import java.util.Set;

import fi.abo.kogni.soile2.projecthandling.projectElements.instance.impl.TaskFileResult;
import io.vertx.core.json.JsonArray;


/**
 * A Data participant loads all Data necessary for data retrieval for this participant.
 * 
 * @author Thomas Pfau
 *
 */
@Deprecated
public interface DataParticipant extends Participant{	
	/**
	 * Get the File Results for a specific task for this participant
	 * @param taskID the task to retrieve the file results for
	 * @return a {@link Set} of {@link TaskFileResult}s representing all results stored for this user.
	 */
	Set<TaskFileResult> getFileResultsForTask(String taskID);

	/**
	 * Get All tasks that have files for this {@link DataParticipantImpl} 
	 * @param taskID the task to retrieve the file results for
	 * @return a {@link Set} of {@link TaskFileResult}s representing all results stored for this user.
	 */
	Set<String> getTasksWithFiles();

	/**
	 * Get all File Results for this participant
	 * @return a set of all File Results
	 */
	Set<TaskFileResult> getFileResults();

	/**
	 * Get all Json Results each annotated with the task Id
	 * @return a set of all File Results
	 */
	JsonArray getJsonResults();
	
	/**
	 * Get the Json Results for the given task
	 * @return a set of all File Results
	 */
	JsonArray getJsonResults(String TaskID);

}