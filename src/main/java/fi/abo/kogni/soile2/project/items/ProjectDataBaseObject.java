package fi.abo.kogni.soile2.project.items;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.vertx.core.json.JsonObject;

public abstract class ProjectDataBaseObject {

	protected JsonObject data; 	
	
	public ProjectDataBaseObject(JsonObject data)
	{
		this.data = data;
	}
	@JsonProperty("UUID")
	public UUID getUUID()
	{
		return UUID.fromString(data.getString("UUID"));
	}	
	public void setUUID(UUID uuid)
	{
		data.put("UUID", uuid.toString());
	}
	public void setUUID(String uuid)
	{
		data.put("UUID", uuid);
	}


	/**
	 * Can be either be a commit ID or a Tag
	 * @return
	 */
	@JsonProperty("version")
	public String getVersion()
	{
		return data.getString("version");
	}

	/**
	 * Can be either be a commit ID or a Tag
	 */
	public void setVersion(String version)
	{
		data.put("version", version);
	}


	@JsonProperty("name")
	public String getName()
	{
		return data.getString("name");
	}
	/**
	 * A human readable name for a 
	 * @param name
	 */
	public void setName(String name)
	{
		data.put("name", name);
	}

	public String toString()
	{
		return data.encodePrettily();
	}

	public String toString(List<String> fields)
	{
		JsonObject restricted = new JsonObject();
		for(String field : fields)
		{
			restricted.put(field,data.getValue(field));
		}
		return restricted.encodePrettily();
	}


	@JsonProperty("private")
	public Boolean getPrivate() {
		return data.getBoolean("private");
	}
	public void setPrivate(Boolean _private) {
		data.put("private", _private);
	}

}
