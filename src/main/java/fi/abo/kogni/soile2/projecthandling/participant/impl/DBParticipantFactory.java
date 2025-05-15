package fi.abo.kogni.soile2.projecthandling.participant.impl;

import fi.abo.kogni.soile2.projecthandling.participant.Participant;
import fi.abo.kogni.soile2.projecthandling.participant.ParticipantFactory;
import fi.abo.kogni.soile2.projecthandling.participant.ParticipantManager;
import io.vertx.core.json.JsonObject;
/**
 * A Class to generate Participants with a link to the Participant Manager that stores them.
 * @author Thomas Pfau
 *
 */
public class DBParticipantFactory implements ParticipantFactory{
	
	ParticipantManager manager;
	/**
	 * Default constructor
	 * @param client the Participant Manager for handling and building Participants
	 */
	public DBParticipantFactory(ParticipantManager client)
	{
		this.manager = client;
	}
	
	@Override
	public Participant createParticipant(JsonObject data) {
		return new DBParticipant(data, manager);
	}
	
}
