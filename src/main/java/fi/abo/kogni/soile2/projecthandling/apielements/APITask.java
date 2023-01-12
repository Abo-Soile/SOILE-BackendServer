package fi.abo.kogni.soile2.projecthandling.apielements;

import com.fasterxml.jackson.annotation.JsonProperty;

import fi.abo.kogni.soile2.datamanagement.git.GitFile;
import fi.abo.kogni.soile2.datamanagement.git.GitManager;
import fi.abo.kogni.soile2.projecthandling.exceptions.NoCodeTypeChangeException;
import fi.abo.kogni.soile2.projecthandling.projectElements.Task;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class APITask extends APIElementBase<Task> {

	private String[] gitFields = new String[] {"name", "codeType", "resources"};
	private Object[] gitDefaults = new Object[] {"", "", new JsonArray()};
	
	public APITask() {
		super(new JsonObject());
	}
	
	public APITask(JsonObject data) {
		super(data);
		loadGitJson(data);
	}

	@JsonProperty("codeType")
	public String getCodetype() {
		return data.getString("codeType", "");
	}
	@JsonProperty("codeType")
	public void setCodetype(String codeType) {
		data.put("codeType", codeType);
	}

	/**
	 * This represents ALL resources used in any version of the task. 
	 * @return
	 */
	@JsonProperty("resources")
	public JsonArray getResources() {
		return data.getJsonArray("resources", new JsonArray());
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
		data.put("code", code);
	}
	@Override
	public void setElementProperties(Task task) throws NoCodeTypeChangeException
	{
		if(!task.getCodetype().equals(getCodetype()) && !task.getCodetype().equals(""))
		{
			// the code type is set, and differs from what is supposed to be set now. 
			throw new NoCodeTypeChangeException();
		}
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
	public void loadGitJson(JsonObject json) {
		for(int i = 0; i < gitFields.length ; ++i)
		{
			this.data.put(gitFields[i], json.getValue(gitFields[i], gitDefaults[i]));	
		}
	}
	
	@Override
	public boolean hasAdditionalGitContent()
	{
		return true;
	}
	@Override
	public Future<String> storeAdditionalData(String currentVersion, GitManager gitManager, String targetRepository)
	{
		// We need to store the code. Resources are stored individually.
		GitFile g = new GitFile("Code.obj", targetRepository, currentVersion);
		return gitManager.writeGitFile(g, getCode());
	}
	@Override
	public Future<Boolean> loadAdditionalData(GitManager gitManager, String targetRepository)
	{
		Promise<Boolean> successPromise = Promise.promise();
		GitFile g = new GitFile("Code.obj", targetRepository, this.getVersion());
		gitManager.getGitFileContents(g).onSuccess(code -> {
			setCode(code);
			successPromise.complete(true);
		})
		.onFailure(err -> {
			successPromise.fail(err);
		});
		
		return successPromise.future();
	}	
}
