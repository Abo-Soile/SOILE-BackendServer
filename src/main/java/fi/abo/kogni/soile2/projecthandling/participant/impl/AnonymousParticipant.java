package fi.abo.kogni.soile2.projecthandling.participant.impl;

import fi.abo.kogni.soile2.projecthandling.participant.ParticipantManager;
import io.vertx.core.json.JsonObject;

//TODO Check if needed (probably not)
public class AnonymousParticipant extends TokenParticipant {

	public AnonymousParticipant(JsonObject data, ParticipantManager manager) {
		super(data, manager);
		// TODO Auto-generated constructor stub
	}

}
