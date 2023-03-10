package fi.abo.kogni.soile2.projecthandling.participant.impl;

import fi.abo.kogni.soile2.projecthandling.participant.Participant;
import fi.abo.kogni.soile2.projecthandling.participant.ParticipantManager;
import io.vertx.core.json.JsonObject;

public class TokenParticipantFactory extends DBParticipantFactory {

	public TokenParticipantFactory(ParticipantManager client) {
		super(client);
	}

	@Override
	public Participant createParticipant(JsonObject data) {
		return new TokenParticipant(data, manager);
	}
}
