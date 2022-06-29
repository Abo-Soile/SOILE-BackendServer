package fi.abo.kogni.soile2.project.items;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

import fi.abo.kogni.soile2.http_server.userManagement.Participant;
import fi.abo.kogni.soile2.project.Project;
import io.vertx.core.json.JsonObject;

public abstract class ProjectDataBaseObjectInstance extends ProjectDataBaseObject {

	Project sourceProject;
	public ProjectDataBaseObjectInstance(JsonObject data, Project source)
	{
		super(data);
		this.sourceProject = source; 
	}
	
	@JsonProperty("next")
	public UUID getNext() {
		return UUID.fromString(data.getString("next"));
	}
	public void setNext(UUID next) {
		data.put("next", next.toString());
	}
	public void setNext(String next) {
		data.put("next", next);
	}
	@JsonProperty("instanceID")
	public UUID getInstanceID() {
		return UUID.fromString(data.getString("instanceID"));

	}
	public void setInstanceID(UUID instanceID) {
		data.put("instanceID", instanceID.toString());
	}
	public void setInstanceID(String instanceID) {
		data.put("instanceID", instanceID);
	}

	@JsonProperty("project")
	public UUID getProject() {
		return UUID.fromString(data.getString("project"));
	}
	public void setProject(UUID project) {
		data.put("project", project.toString());
	}
	public void setProject(String project) {
		data.put("project", project);
	}
	public abstract UUID nextTask(Participant user);
	
}
