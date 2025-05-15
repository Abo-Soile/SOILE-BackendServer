package fi.abo.kogni.soile2.projecthandling.participant.impl;

import fi.abo.kogni.soile2.projecthandling.participant.ParticipantManager;
import io.vertx.core.json.JsonObject;

/**
 * A Participant that is based on a token (i.e. authenticated via a token and not associated with a user)
 * @author Thomas Pfau
 *
 */
public class TokenParticipant extends DBParticipant {

	private String token;
	/**
	 * Default constructor
	 * @param data data of the participant 
	 * @param manager the manager for the participant
	 */
	public TokenParticipant(JsonObject data, ParticipantManager manager) {
		super(data,manager);
		token = data.getString("token");
	}

	@Override
	public boolean hasToken()
	{
		return true;
	}
	
	@Override
	public String getToken()
	{
		return token;
	}
}
