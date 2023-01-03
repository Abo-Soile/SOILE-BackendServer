package fi.abo.kogni.soile2.projecthandling.participant.impl;

import fi.abo.kogni.soile2.projecthandling.participant.ParticipantManager;
import io.vertx.core.json.JsonObject;

public class TokenParticipant extends DBParticipant {

	private String token;
	
	public TokenParticipant(JsonObject data, ParticipantManager manager) {
		// TODO Auto-generated constructor stub
		super(data,manager);
		token = data.getString(token);
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
