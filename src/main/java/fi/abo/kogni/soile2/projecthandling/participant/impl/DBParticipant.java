package fi.abo.kogni.soile2.projecthandling.participant.impl;

import fi.abo.kogni.soile2.projecthandling.participant.Participant;
import fi.abo.kogni.soile2.projecthandling.participant.ParticipantManager;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.ProjectInstance;
import fi.abo.kogni.soile2.projecthandling.resultDB.ResultDBHandler;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * This class represents a participant stored in a database. It needs a participantManager to handle storing.
 * @author Thomas Pfau
 *
 */
public class DBParticipant extends Participant{

	private ParticipantManager pManager;	
	private ResultDBHandler resultHandler;
	public DBParticipant(JsonObject data, ProjectInstance p, ParticipantManager pManager)
	{
		super(data, p);
		this.pManager = pManager;
	}	
	
	
	
	@Override
	public Future<String> save()
	{		
		return pManager.save(this);
	}



	@Override
	public Future<String> saveJsonResults(JsonArray results) {
		// TODO Auto-generated method stub
		return resultHandler.createResults(results);
	}

}
