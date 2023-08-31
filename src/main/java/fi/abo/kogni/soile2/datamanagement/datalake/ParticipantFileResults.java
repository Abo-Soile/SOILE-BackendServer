package fi.abo.kogni.soile2.datamanagement.datalake;

import java.nio.file.Path;

/**
 * This class maps the participant data folder (i.e. files stored for the participant) to the actual folder on disk.   
 * The format is: "BaseFolder"/participantID"
 * @author Thomas Pfau
 *
 */
public class ParticipantFileResults {

	private String participantID;

	
	/**
	 * Generate a new Fileresult for a given task. 
	 * @param originalFileName the original ID of the file as supplied by the project generation
	 * @param projectID The project ID in which this is a result 
	 * @param fileFormat the type of the file, essentially the file extension
	 * @param taskID the id of the task this file belongs to.
	 * @param participantID the id of the participant this file belongs to.
	 */
	public ParticipantFileResults(String participantID) {
		this.participantID = participantID;
	}	

	/**
	 * Get the path to the participant folder
	 * @param dataLakeDirectory the datalake directory this file is stored in 
	 * @return The relative Path of the file this result refers to. 
	 */
	public String getParticipantFolderPath(String dataLakeDirectory)
	{
		return Path.of(dataLakeDirectory, getParticipantFolder()).toString();
	}
	
	/**
	 * Get the folder of the participant.
	 * @return
	 */
	public String getParticipantFolder()
	{
		return Path.of(participantID).toString();
	}	
}
