package fi.abo.kogni.soile2.project.instance.impl;

import fi.abo.kogni.soile2.project.elementManagement.ItemRetriever;
import fi.abo.kogni.soile2.project.instance.ProjectInstance;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;

public class ProjectRetriever extends ItemRetriever<ProjectInstance> {

	public ProjectRetriever(EventBus eb) {
		super(eb);
	}

	@Override
	public ProjectInstance createElement(String elementData) {
		JsonObject projectData = new JsonObject(elementData);
		return null;		
	}
		
}
