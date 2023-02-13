package fi.abo.kogni.soile2.projecthandling.apielements;

import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonProperty;

import fi.aalto.scicomp.gitFs.gitProviderVerticle;
import fi.abo.kogni.soile2.datamanagement.git.GitFile;
import fi.abo.kogni.soile2.projecthandling.exceptions.NoCodeTypeChangeException;
import fi.abo.kogni.soile2.projecthandling.projectElements.impl.Task;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.ReplyException;
import io.vertx.core.json.JsonObject;

/**
 * An API Task element
 * @author Thomas Pfau
 *
 */
public class APITask extends APIElementBase<Task> {
	private static final Logger LOGGER = LogManager.getLogger(APITask.class);

	private String[] gitFields = new String[] {"name", "codeType"};
	private Object[] gitDefaults = new Object[] {"", new JsonObject()};
	
	public APITask() {
		super(new JsonObject());
	}
	
	public APITask(JsonObject data) {
		super(data);
		loadGitJson(data);
	}

	@JsonProperty("codeType")
	public JsonObject getCodeType() {
		return data.getJsonObject("codeType", new JsonObject());
	}
	@JsonProperty("codeType")
	public void setCodetype(JsonObject codeType) {
		data.put("codeType", codeType);
	}

	public String getCodeVersion() {
		return getCodeType().getString("version","");
	}
	public void setCodeVersion(String codeVersion) {		
		JsonObject codeType = data.getJsonObject("codeType");
		if(codeType != null)
		{
			codeType.put("version", codeVersion);
		}
		else
		{
			data.put("codeType", new JsonObject().put("version", codeVersion));
		}
	}
	
	public String getCodeLanguage() {
		return getCodeType().getString("language", "");
	}
	public void setCodeLanguage(String language) {
		JsonObject codeType = data.getJsonObject("codeType");
		if(codeType != null)
		{
			codeType.put("language", language);
		}
		else
		{
			data.put("codeType", new JsonObject().put("language", language));
		}
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
		LOGGER.debug("Current Code Type: \n" + getCodeType().encodePrettily());
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
		LOGGER.debug("Data before addition: " + data.encodePrettily() );
		for(int i = 0; i < gitFields.length ; ++i)
		{
			this.data.put(gitFields[i], json.getValue(gitFields[i], gitDefaults[i]));	
		}
		LOGGER.debug("Data after addition: " + data.encodePrettily() );
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
			if(err instanceof ReplyException)
			{
				int errorCode = ((ReplyException)err).failureCode();
				if(errorCode == gitProviderVerticle.FILE_DOES_NOT_EXIST_FOR_VERSION)
				{
					// this is fine, could be that it wasn't generated yet, we will return an empty string then.
					setCode("");
					codePromise.complete(true);
				}
				else
				{
					codePromise.fail(err);		
				}
			}
			else
			{
				LOGGER.debug("Failed to load Code File");
				err.printStackTrace(System.out);
				codePromise.fail(err);
			}
		});			
		return CompositeFuture.all(loadedList).map(true);
	}	
}
