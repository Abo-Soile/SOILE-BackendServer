package fi.abo.kogni.soile2.http_server.verticles;

import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fi.abo.kogni.soile2.projecthandling.participant.ParticipantHandler;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.impl.StudyHandler;
import fi.abo.kogni.soile2.utils.SoileCommUtils;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonArray;


/**
 * Verticle for parts of participant handling
 * @author Thomas Pfau
 *
 */
public class ParticipantVerticle extends AbstractVerticle {

	ParticipantHandler partHandler;
	StudyHandler projHandler;
	static final Logger LOGGER = LogManager.getLogger(ParticipantVerticle.class);
	private List<MessageConsumer> consumers;
	
	public ParticipantVerticle(ParticipantHandler partHandler, StudyHandler projHandler)
	{
		this.partHandler = partHandler;
		this.projHandler = projHandler;
	}
	
	@Override
	public void start()
	{
		consumers = new LinkedList<>();
		consumers.add(vertx.eventBus().consumer(("soile.participant.delete"), this::deleteParticipants));
	}	

	@Override
	public void stop(Promise<Void> stopPromise)
	{
		List<Future> undeploymentFutures = new LinkedList<Future>();
		for(MessageConsumer consumer : consumers)
		{
			undeploymentFutures.add(consumer.unregister());
		}				
		CompositeFuture.all(undeploymentFutures).mapEmpty().
		onSuccess(v -> {
			LOGGER.debug("Successfully undeployed DataBundleGenerator with id : " + deploymentID());
			stopPromise.complete();
		})
		.onFailure(err -> stopPromise.fail(err));			
	}
	/**
	 * Delete a participant and all associated files/data.
	 * @param message
	 */
	public void deleteParticipants(Message<JsonArray> message)
	{
		JsonArray participantInformation = message.body();
		List<Future> deletionFutures = new LinkedList<>();
		for(int i = 0; i< participantInformation.size(); ++i)
		{
			deletionFutures.add(partHandler.deleteParticipant(participantInformation.getJsonObject(i).getString("participantID"), true));			
		}
		CompositeFuture.all(deletionFutures)
		.onSuccess(succeeded -> 
		{
			message.reply(SoileCommUtils.successObject());
		})
		.onFailure(err -> {
			LOGGER.error("Problems while deleting participant: " );
			LOGGER.error(err);
			message.fail(500, "Problems while deleting Participants");
		});
	}
	
}
