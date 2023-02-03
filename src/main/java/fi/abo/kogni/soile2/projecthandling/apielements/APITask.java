package fi.abo.kogni.soile2.projecthandling.apielements;

import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonProperty;

import fi.abo.kogni.soile2.datamanagement.git.GitElement;
import fi.abo.kogni.soile2.datamanagement.git.GitFile;
import fi.abo.kogni.soile2.projecthandling.exceptions.NoCodeTypeChangeException;
import fi.abo.kogni.soile2.projecthandling.projectElements.impl.Task;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * An API Task element
 * @author Thomas Pfau
 *
 */
public class APITask extends APIElementBase<Task> {
	private static final Logger LOGGER = LogManager.getLogger(APITask.class);

	private String[] gitFields = new String[] {"name", "codeType"};
	private Object[] gitDefaults = new Object[] {"", ""};
	
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
	public Future<String> storeAdditionalData(String currentVersion, EventBus eb, String targetRepository)
	{
		// We need to store the code. Resources are stored individually.
		
		GitFile g = new GitFile("Code.obj", targetRepository, currentVersion);
		LOGGER.debug("Writing Source Code");
		return eb.request("soile.git.writeGitFile", g.toJson().put("data",getCode())).map(message -> {return (String) message.body();});
	}
	@Override
	public Future<Boolean> loadAdditionalData(EventBus eb, String targetRepository)
	{
		Promise<Boolean> codePromise = Promise.promise();
		GitFile g = new GitFile("Code.obj", targetRepository, this.getVersion());
		LOGGER.debug("Loading Code Object");
		List<Future> loadedList = new LinkedList<>();
		loadedList.add(codePromise.future());
		eb.request("soile.git.getGitFileContents", g.toJson()).onSuccess(codeReply -> {
			setCode((String)codeReply.body());
			codePromise.complete(true);
		})
		.onFailure(err -> {
			codePromise.fail(err);
		});
		Promise<Boolean> resourcesPromise = Promise.promise();
		loadedList.add(resourcesPromise.future());
		GitElement targetRepo = new GitElement(targetRepository, this.getVersion());
		eb.request("soile.git.getResourceList", targetRepo.toJson()).onSuccess(resources -> {
			LOGGER.debug("Resources are: " + ((JsonArray) resources.body()).encodePrettily());
			setResources((JsonArray) resources.body());
			resourcesPromise.complete(true);
		})
		.onFailure(err -> {
			resourcesPromise.fail(err);
		});
		
		
		return CompositeFuture.all(loadedList).map(true);
	}	
}
