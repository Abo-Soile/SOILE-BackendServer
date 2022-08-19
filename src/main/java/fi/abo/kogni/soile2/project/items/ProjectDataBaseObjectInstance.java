package fi.abo.kogni.soile2.project.items;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

import fi.abo.kogni.soile2.project.instance.ProjectInstance;
import fi.abo.kogni.soile2.project.participant.Participant;
import io.vertx.core.json.JsonObject;

public abstract class ProjectDataBaseObjectInstance extends ProjectDataBaseObject {

	ProjectInstance sourceProject;
	public ProjectDataBaseObjectInstance(JsonObject data, ProjectInstance source)
	{
		super(data);
		this.sourceProject = source; 
	}
	
	@JsonProperty("next")
	public String getNext() {
		return data.getString("next");
	}
	public void setNext(String next) {
		data.put("next", next);
	}
	@JsonProperty("instanceID")
	public String getInstanceID() {
		return data.getString("instanceID");
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
	public abstract String nextTask(Participant user);
	
}
