package fi.abo.kogni.soile2.projecthandling.participant;

import java.io.File;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import fi.abo.kogni.soile2.datamanagement.utils.TimeStampedMap;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.ProjectInstance;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.impl.ProjectInstanceHandler;
import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.ext.mongo.MongoClient;
/**
 * The participantHandler keeps track of all participants. 
 * It stores 
 * @author Thomas Pfau
 *
 */
public class ParticipantHandler {
	MongoClient client;
	ProjectInstanceHandler project;
	TimeStampedMap<String,Participant> participants;
	ParticipantManager manager;
	Vertx vertx;
	
	
	
	
	public ParticipantHandler(MongoClient client, ProjectInstanceHandler project, ParticipantManager manager, Vertx vertx) {
		super();		
		this.client = client;
		this.project = project;
		this.manager = manager;
		this.vertx = vertx;
		participants = new TimeStampedMap<String, Participant>(manager, 2*3600); //Keep for two hours
	}
	/**
	 * Create a participant in the database, store that  and let the handler handle it
	 * @param handler
	 */
	public void create(ProjectInstance p, Handler<AsyncResult<Participant>> handler)
	{
		handler.handle(create(p));			
	}
	
	/**
	 * Create a participant in the database, store that  and let the handler handle it
	 * @param handler
	 */
	public Future<Participant> create(ProjectInstance p)
	{
		Promise<Participant> participantPromise = Promise.<Participant>promise();
		manager.createParticipant(p).onComplete(participant ->
		{
			if(participant.succeeded())
			{
				participants.putData(participant.result().getID(), participant.result());
				participantPromise.complete(participant.result());
			}
			else
			{
				participantPromise.fail(participant.cause());	
			}			
		});
		return participantPromise.future();
	}
	
	
	/**
	 * Clean up the data currently stored by this Participant handler. 
	 * This is necessary to avoid excessive data in memory.
	 */
	public void cleanup()
	{
		Collection<Participant> partsToClean = participants.cleanup();
	}
	
	/**
	 * Retrieve a participant from the database (or memory) and return the participant
	 * based on the participants uID.
	 * @param id the uid of the participant
	 * @param handler the handler that requested the participant.
	 */
	public void getParticpant(String id, Handler<AsyncResult<Participant>> handler)
	{
		participants.getData(id, handler);
	}
	
	/**
	 * Retrieve a participant from the datbase (or memory) and return the participant
	 * based on the participants uID.
	 * @param id the uid of the participant
	 * @param handler the handler that requested the participant.
	 */
	public Future<Participant> getParticpant(String id)
	{
		return participants.getData(id);		
	}
	
	/**
	 * Delete a participant and all data associated with the participant from the project.
	 * @param id
	 */
	public Future<Void> deleteParticipant(String id)
	{
		Promise<Void> deletionPromise = Promise.<Void>promise();
		manager.deleteParticipant(id, res -> 
		{
			if(res.succeeded())
			{
				// the participant was successfully deleted from the database, so now, we only need to delete the files
				participants.getData(id).onSuccess(participant -> {
					List<Future> deletionFutures = new LinkedList<Future>();
					for(File f : project.getFilesForParticipant(participant))
					{
						deletionFutures.add(vertx.fileSystem().delete(f.getAbsolutePath()));
					}

					CompositeFuture.all(deletionFutures).onFailure(failure ->
					{
						deletionPromise.fail(failure.getCause());
					}).onSuccess(success ->
					{
						List<Future> deletedFolders = new LinkedList<Future>();
						for(File f : project.getFilesForParticipant(participant))
						{
							deletedFolders.add(vertx.fileSystem().delete(f.getAbsolutePath()));
						}
						CompositeFuture.all(deletedFolders).onFailure(failure ->
						{
							deletionPromise.fail(failure.getCause());
						}).onSuccess(deltionDone ->{
							deletionPromise.complete();						
						});

					});
				}).onFailure(fail -> {
					deletionPromise.fail("Could not retrieve participant, thus cannot delete files!");
				});
			}
		});
		return deletionPromise.future();
	}
	
	
}
