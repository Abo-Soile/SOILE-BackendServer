package fi.abo.kogni.soile2.projecthandling.participant;

import fi.abo.kogni.soile2.projecthandling.projectElements.instance.ProjectInstance;
import io.vertx.core.json.JsonObject;
/**
 * Interface to create Participants, to keep the ParticipantManager flexible.
 * @author Thomas Pfau
 *
 */
public interface ParticipantFactory {	
	
	public Participant createParticipant(JsonObject data, ProjectInstance p);
}
