package fi.abo.kogni.soile2.projecthandling.participant.impl;

import fi.abo.kogni.soile2.projecthandling.participant.Participant;
import fi.abo.kogni.soile2.projecthandling.participant.ParticipantManager;
import io.vertx.core.json.JsonObject;

/**
 * A factory for Toke based participants
 * @author Thomas Pfau
 *
 */
public class TokenParticipantFactory extends DBParticipantFactory {

	/**
	 * Default constructor
	 * @param client the participant manager to be used
	 */
	public TokenParticipantFactory(ParticipantManager client) {
		super(client);
	}

	@Override
	public Participant createParticipant(JsonObject data) {
		return new TokenParticipant(data, manager);
	}
}
