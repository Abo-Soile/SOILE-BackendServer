package fi.abo.kogni.soile2.projecthandling.projectElements.instance.impl;

import java.util.HashSet;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fi.abo.kogni.soile2.datamanagement.datalake.DataLakeFile;
import fi.abo.kogni.soile2.datamanagement.utils.CheckDirtyMap;
import fi.abo.kogni.soile2.projecthandling.exceptions.ObjectDoesNotExist;
import fi.abo.kogni.soile2.projecthandling.participant.Participant;
import fi.abo.kogni.soile2.projecthandling.participant.impl.DBParticipant;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.Study;
import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;

/**
 * Handler for Project instances.
 * @author Thomas Pfau
 *
 */
public class StudyHandler {

	static final Logger LOGGER = LogManager.getLogger(StudyHandler.class);

	private CheckDirtyMap<String, Study> studies;
	private String dataLakeFolder;
	private StudyManager manager;	 
	
	/**
	 * Default constructor that sets up a Manager with DB connections.
	 * @param participants the participants handler to obtain participants
	 * @param dataLakeFolder The Folder where the dataLake for result files is located
	 * @param client the mongoclient for connecting to the mongo database
	 */
	public StudyHandler(MongoClient client, Vertx vertx) {
		this(client, new StudyManager(client, vertx) );		
	}

	/**
	 * Default constructor that sets up a Manager with DB connections.
	 * @param participants the participants handler to obtain participants
	 * @param dataLakeFolder The Folder where the dataLake for result files is located
	 * @param client the mongoclient for connecting to the mongo database
	 * @param manager a custom project Manager.
	 */
	public StudyHandler(MongoClient client, StudyManager manager) {
		super();
		this.dataLakeFolder = SoileConfigLoader.getServerProperty("soileResultDirectory");
		this.manager = manager;
		studies = new CheckDirtyMap<String, Study>(manager, 1000*60*60);
	}

	/**
	 * Clean up the data in the Data Maps
	 */
	public void cleanup()
	{
		studies.cleanup();
		manager.cleanUp();
	}
	
	/**
	 * Deactivate a study
	 * @param study the study to deactivate
	 */	
	public Future<Void> deactivate(String id)
	{		
		return studies.getData(id).compose((study) -> manager.deactivate(study));
	}
	
	/**
	 * Activate a study
	 * @param study the study to activate
	 */	
	public Future<Void> activate(String id)
	{
		return studies.getData(id).compose((study) -> manager.activate(study));
	}
	
	/**
	 * Delete a study,
	 * @return The Object that was deleted from the Study database
	 */		
	public Future<JsonObject> deleteStudy(String studyID)
	{
		Promise<JsonObject> deletionPromise = Promise.<JsonObject>promise();
		loadUpToDateStudy(studyID)
		.onSuccess(currentStudy -> {
			currentStudy.delete()
			.onSuccess(studyJson -> {
				this.studies.cleanElement(studyID);
				deletionPromise.complete(studyJson);
			})
			.onFailure(err -> deletionPromise.fail(err));
		})
		.onFailure(err -> deletionPromise.fail(err));
		
		return deletionPromise.future();
	}
	
	/**
	 * Get a list of all Files associated with the specified {@link DBParticipant} within this {@link Study}.
	 * @param p the {@link DBParticipant} for which to retrieve the file results.
	 * @return
	 */
	public Set<DataLakeFile> getFilesinProject(Set<TaskFileResult> fileResults)
	{
		HashSet<DataLakeFile> fileSet = new HashSet<DataLakeFile>();
		for(TaskFileResult res : fileResults)
		{
			fileSet.add(res.getFile(dataLakeFolder));
		}
		return fileSet;
	}
	
	/**
	 * Add the participant to the project with the given ID.
	 * @param projectInstanceID The id of the project to which to add the participant.
	 * @param participant The id of the participant to add to the project.
	 * @Return a Successfull future if the participant was added
	 */
	public Future<Void> addParticipant(String projectInstanceID, Participant participant)
	{
		
		Promise<Void> addPromise = Promise.<Void>promise();
				
		// if no ID is provided, create a new Participant for the indicated project and add that participant to the list.		
		studies.getData(projectInstanceID).onSuccess(targetProject -> 
		{	
			
			targetProject.addParticipant(participant)
			.onSuccess(success -> {
				addPromise.complete();
			})
			.onFailure(err -> addPromise.fail(err));
		}).onFailure(fail -> {
				addPromise.fail(fail);	
		});		
		return addPromise.future();
	}
	
	/**
	 * Remove the participant with the given id from the project.
	 * @param projectInstanceID The id of the project to which to add the participant.
	 * @param participant The participant to remove from the project.
	 * @param ensureDeletionFromStudy Whether this Future should fail, if the participant was just not present in the study.
	 * @Return a Successfull future if the participant was removed
	 */
	public Future<Void> removeParticipant(String projectInstanceID, Participant participant, boolean ensureDeletionFromStudy)
	{
		
		Promise<Void> removePromise = Promise.<Void>promise();
				
		// if no ID is provided, create a new Participant for the indicated project and add that participant to the list.		
		studies.getData(projectInstanceID).onSuccess(targetProject -> 
		{				
			targetProject.deleteParticipant(participant)
			.onSuccess(deleted-> {
				if(deleted || !ensureDeletionFromStudy)
				{
					removePromise.complete();
				}
				else
				{
					removePromise.fail("Could not delete participant from study, didn't exist in study");
				}
			})
			.onFailure(err -> removePromise.fail(err));
		}).onFailure(fail -> {
			if( fail instanceof ObjectDoesNotExist && !ensureDeletionFromStudy)
			{
				// the project does not exist so it is fine if this is skipped.
				removePromise.complete();
			}
			else
			{
				removePromise.fail(fail);
			}
		});		
		return removePromise.future();
	}
	
	
	/**
	 * Start a project with the given Project Information.
	 * The information must contain:
	 * 1. "UUID" of the project from which this was started
	 * 2. "version" of the project from which this was started
	 * 3. "private" field wrt access for this 
	 * 4. "name" a name field.
	 * 5. "shortcut" (optional), that can be used as a shortcut to the project.
	 * @param projectInformation The information needed to start this project.
	 */
	public Future<Study> createStudy(JsonObject projectInformation)	
	{
		return manager.startProject(projectInformation);
	}
	
	/**
	 * Load a project with the given ID. 
	 * This can fail if the project does not exist.
	 * @param projectInstanceID the instance ID of the project to retrieve.
	 */
	public Future<Study> loadUpToDateStudy(String projectInstanceID)
	{
		return studies.getData(projectInstanceID);		
	}
	
	/**
	 * Load a project with the given ID and don't care whether it is current (only the ID is important here.. 
	 * This can fail if the project does not exist.
	 * @param projectInstanceID the instance ID of the project to retrieve.
	 */
	public Future<Study> loadPotentiallyOutdatedStudy(String projectInstanceID)
	{
		return studies.getDirtyData(projectInstanceID);		
	}
	
	/**
	 * Update a study 
	 * This can fail if the study does not exist OR if the requested update is not possible.
	 * @param studyID the instance ID of the project to retrieve.
	 */
	public Future<Void> updateStudy(String studyID, JsonObject newData)
	{
		Promise<Void> updatePromise = Promise.promise();		
		studies.getData(studyID)
		.onSuccess(currentStudy -> {			
			manager.updateStudy(currentStudy, newData)
			.onSuccess(updated -> {				
				updatePromise.complete();
			})
			.onFailure(err -> updatePromise.fail(err));
		})
		.onFailure(err -> updatePromise.fail(err));
		return updatePromise.future();
	}
	
	/**
	 * Get a list of project instances. based on the given Permissions
	 * This will return non-private projects and all projects listed in the permissions.
	 * @param Permissions  the permissions for the projects 
	 * @param permissionsOnly whether to only list those projects indicated by the permissions
	 * @return A {@link Future} of the {@link JsonArray} containing the projects (id and name)
	 */
	public Future<JsonArray> getStudyList(JsonArray Permissions, boolean permissionsOnly)
	{
		return manager.getProjectInstanceStatus(Permissions, permissionsOnly);
	}
	
	/**
	 * Get the list of Studies
	 * This will return all studies available on the server
	 * @return A {@link Future} of the {@link JsonArray} containing the projects (id and name)
	 */
	public Future<JsonArray> getStudyList()
	{
		return manager.getStudies();
	}
	
	
	/**
	 * Get the project ID for a given Path-ID (i.e. translate potential shortcuts).  
	 * @param pathID the {id} path parameter  
	 * @return A future of the actual path.
	 */
	public Future<String> getProjectIDForPath(String pathID)
	{
		return manager.getProjectIDForPathID(pathID);
	}
	
}
