package fi.abo.kogni.soile2.project.utils;

import fi.abo.kogni.soile2.project.instance.ProjectInstance;
import fi.abo.kogni.soile2.project.participant.Participant;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

public class TestParticipant extends Participant {

	public TestParticipant(JsonObject data, ProjectInstance p) {
		super(data, p);
		// TODO Auto-generated constructor stub
	}

	@Override
	public Future<String> save() {
				return Future.succeededFuture(this.uuid);
	}

}
