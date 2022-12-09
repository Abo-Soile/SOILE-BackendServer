package fi.abo.kogni.soile2.projecthandling.participant;

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

import fi.abo.kogni.soile2.datamanagement.utils.DatedDataMap;
import fi.abo.kogni.soile2.datamanagement.utils.TimeStampedData;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.ProjectInstance;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.impl.TaskFileResult;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.impl.TaskObjectInstance;
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
public abstract class DataParticipantImpl extends ParticipantImpl implements DataParticipant {

	/**
	 * This is the _id field from the spec.
	 */
	protected JsonArray resultData;	
	
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
	public DataParticipantImpl(JsonObject data)
	{
		super(data);		
	}

	@Override
	protected void setupParticipant(JsonObject participantInfo)
	{				
		//finished = participantInfo.getJsonArray("finished",new JsonArray());
		super.setupParticipant(participantInfo);		
		resultData = participantInfo.getJsonArray("resultData", new JsonArray());	
		
	}

	/**
	 * Get the File Results for a specific task for this participant
	 * @param taskID the task to retrieve the file results for
	 * @return a {@link Set} of {@link TaskFileResult}s representing all results stored for this user.
	 */
	@Override
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
	 * Get All tasks that have files for this {@link DataParticipantImpl} 
	 * @param taskID the task to retrieve the file results for
	 * @return a {@link Set} of {@link TaskFileResult}s representing all results stored for this user.
	 */
	@Override
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
	@Override
	public Set<TaskFileResult> getFileResults()
	{
		Set<TaskFileResult> fileResults = new HashSet<TaskFileResult>();		
		for(String taskID : getTasksWithFiles())
		{
			fileResults.addAll(getFileResultsForTask(taskID));
		}		
		return fileResults;
	}
	
	@Override
	public abstract Future<String> save();
	
}
