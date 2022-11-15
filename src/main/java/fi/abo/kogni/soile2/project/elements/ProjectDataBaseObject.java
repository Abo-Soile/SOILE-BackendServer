package fi.abo.kogni.soile2.project.elements;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

public interface ProjectDataBaseObject {

	@JsonProperty("UUID")
	UUID getUUID();

	@JsonProperty("UUID")
	void setUUID(UUID uuid);

	@JsonProperty("UUID")
	void setUUID(String uuid);

	/**
	 * Can be either be a commit ID or a Tag
	 * @return
	 */
	@JsonProperty("version")
	String getVersion();

	/**
	 * Can be either be a commit ID or a Tag
	 */
	@JsonProperty("version")
	void setVersion(String version);

	@JsonProperty("name")
	String getName();

	/**
	 * A human readable name for a 
	 * @param name
	 */
	@JsonProperty("name")
	void setName(String name);

	String toString();

	String toString(List<String> fields);
	@JsonProperty("private")
	Boolean getPrivate();
	@JsonProperty("private")
	void setPrivate(Boolean _private);

}