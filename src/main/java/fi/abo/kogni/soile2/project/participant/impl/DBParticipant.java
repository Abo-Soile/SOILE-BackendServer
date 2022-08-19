package fi.abo.kogni.soile2.project.participant.impl;

import fi.abo.kogni.soile2.project.instance.ProjectInstance;
import fi.abo.kogni.soile2.project.participant.Participant;
import fi.abo.kogni.soile2.project.participant.ParticipantManager;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

/**
 * This class represents a participant stored in a database. It needs a participantManager to handle storing.
 * @author Thomas Pfau
 *
 */
public class DBParticipant extends Participant{

	private ParticipantManager pManager;

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

}
