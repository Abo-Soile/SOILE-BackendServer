package fi.abo.kogni.soile2.project.data;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;

/**
 * This workerverticle will retrieve data for a project and, in response, provid ethe respective 
 * file lists, once generated so that those can be pumped through zip streams to a downloader. 
 * @author Thomas Pfau
 *
 */
public class DataVerticle extends AbstractVerticle{

	MongoClient client;
	DataBundleGenerator generator;
	String projectInstanceCollection;
	static final Logger LOGGER = LogManager.getLogger(DataVerticle.class);

	public void handleRequest(Message<Object> requestMessage)
	{
		JsonObject command = (JsonObject) requestMessage.body();
		
		String projectID = command.getString(SoileConfigLoader.getCommunicationField("projectID"));
		client.findOne(projectInstanceCollection, new JsonObject().put("_id", projectID),null).onSuccess(pInstanceData -> {
			generator.getDataBundleForProject(pInstanceData).onSuccess(ID -> {				
			requestMessage.reply(new JsonObject().put(SoileConfigLoader.getCommunicationField("downloadID"), ID.toString()));
			});
		}).onFailure(fail -> {
			LOGGER.error(fail);
			requestMessage.fail(1, "Problem retrieveing requested Project : " + fail.getMessage());		
		});
		
	}
	
	
	
}
