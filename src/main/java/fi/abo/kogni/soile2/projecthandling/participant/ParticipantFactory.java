package fi.abo.kogni.soile2.projecthandling.participant;

import io.vertx.core.json.JsonObject;
/**
 * Interface to create Participants, to keep the ParticipantManager flexible.
 * @author Thomas Pfau
 *
 */
public interface ParticipantFactory {	
	
	/**
	 * Create a Participant based on the provided data
	 * @param data the JsonData to use to create a Participant
	 * @return the created {@link Participant}
	 */
	public Participant createParticipant(JsonObject data);
}
