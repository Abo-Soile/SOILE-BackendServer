package fi.abo.kogni.soile2.project.elements.impl;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

import fi.abo.kogni.soile2.project.elements.ProjectDataBaseObject;
import io.vertx.core.json.JsonObject;

public abstract class ProjectDataBaseObjectImpl implements ProjectDataBaseObject {

	protected JsonObject data; 	
	
	public ProjectDataBaseObjectImpl(JsonObject data)
	{
		this.data = data;
	}
	@Override
	@JsonProperty("UUID")
	public UUID getUUID()
	{
		return UUID.fromString(data.getString("UUID"));
	}	
	@Override
	public void setUUID(UUID uuid)
	{
		data.put("UUID", uuid.toString());
	}
	@Override
	public void setUUID(String uuid)
	{
		data.put("UUID", uuid);
	}


	/**
	 * Can be either be a commit ID or a Tag
	 * @return
	 */
	@Override
	@JsonProperty("version")
	public String getVersion()
	{
		return data.getString("version");
	}

	/**
	 * Can be either be a commit ID or a Tag
	 */
	@Override
	public void setVersion(String version)
	{
		data.put("version", version);
	}


	@Override
	@JsonProperty("name")
	public String getName()
	{
		return data.getString("name");
	}
	/**
	 * A human readable name for a 
	 * @param name
	 */
	@Override
	public void setName(String name)
	{
		data.put("name", name);
	}

	@Override
	public String toString()
	{
		return data.encodePrettily();
	}

	@Override
	public String toString(List<String> fields)
	{
		JsonObject restricted = new JsonObject();
		for(String field : fields)
		{
			restricted.put(field,data.getValue(field));
		}
		return restricted.encodePrettily();
	}

	@Override
	@JsonProperty("private")
	public Boolean getPrivate() {
		return data.getBoolean("private");
	}
	@Override
	public void setPrivate(Boolean _private) {
		data.put("private", _private);
	}

}
