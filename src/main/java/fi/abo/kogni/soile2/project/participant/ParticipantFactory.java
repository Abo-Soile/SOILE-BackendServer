package fi.abo.kogni.soile2.project.participant;

import fi.abo.kogni.soile2.project.instance.ProjectInstance;
import io.vertx.core.json.JsonObject;
/**
 * Interface to create Participants, to keep the ParticipantManager flexible.
 * @author Thomas Pfau
 *
 */
public interface ParticipantFactory {	
	
	public Participant createParticipant(JsonObject data, ProjectInstance p);
}
