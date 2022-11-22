package fi.abo.kogni.soile2.projecthandling.apielements;

import com.fasterxml.jackson.annotation.JsonProperty;

import fi.abo.kogni.soile2.datamanagement.git.GitFile;
import fi.abo.kogni.soile2.datamanagement.git.GitManager;
import fi.abo.kogni.soile2.projecthandling.projectElements.Task;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class APITask extends APIElementBase<Task> {

	private static String[] gitFields = new String[] {"name", "codetype", "resources"};
	private static Object[] gitDefaults = new Object[] {"", "javascript", new JsonArray()};

	
	public APITask(JsonObject data) {
		super(data); 
	}

	@JsonProperty("codetype")
	public String getCodetype() {
		return data.getString("codetype");
	}
	@JsonProperty("codetype")
	public void setCodetype(String codetype) {
		data.put("codetype", codetype);
	}

	/**
	 * This represents ALL resources used in any version of the task. 
	 * @return
	 */
	@JsonProperty("resources")
	public JsonArray getResources() {
		return data.getJsonArray("resources");
	}
	@JsonProperty("resources")
	public void setResources(JsonArray resources) {
		data.put("resources", resources);
	}

	@JsonProperty("code")
	public String getCode() {
		return data.getString("code","");
	}
	@JsonProperty("code")
	public void setCode(String code) {
		data.put("codetype", code);
	}
	@Override
	public void setElementProperties(Task task)
	{
		task.setCodetype(getCodetype());
		// TODO: maybe this should be converted so that it in the end puts in IDs.
		task.setResources(getResources());
	}
	@Override
	public JsonObject getGitJson() {
		JsonObject gitData = new JsonObject();
		for(int i = 0; i < gitFields.length ; ++i)
		{
			gitData.put(gitFields[i], data.getValue(gitFields[i], gitDefaults[i]));	
		}
		return gitData;
	}
	@Override
	public boolean hasAdditionalContent()
	{
		return true;
	}
	
	public Future<String> storeAdditionalData(String currentVersion, GitManager gitManager)
	{
		// We need to store the code. Resources are stored individually.
		GitFile g = new GitFile("Code.obj", getUUID(), currentVersion);
		return gitManager.writeGitFile(g, getCode());
	}
}
