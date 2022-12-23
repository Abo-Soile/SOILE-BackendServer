package fi.abo.kogni.soile2.http_server.verticles;

import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fi.abo.kogni.soile2.projecthandling.participant.ParticipantHandler;
import fi.abo.kogni.soile2.utils.SoileCommUtils;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonArray;


public class ParticipantVerticle extends AbstractVerticle {

	ParticipantHandler partHandler;
	static final Logger LOGGER = LogManager.getLogger(ParticipantVerticle.class);
	private List<MessageConsumer> consumers;
	
	public ParticipantVerticle(ParticipantHandler handler)
	{
		this.partHandler = handler;
		consumers = new LinkedList<>();
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
		onSuccess(v -> stopPromise.complete())
		.onFailure(err -> stopPromise.fail(err));			
	}
	
	public void deleteParticipants(Message<JsonArray> message)
	{
		JsonArray projectInfo = message.body();
		List<Future> deletionFutures = new LinkedList<>();
		for(int i = 0; i< projectInfo.size(); ++i)
		{
			deletionFutures.add(partHandler.deleteParticipant(projectInfo.getJsonObject(i).getString("participantID")));			
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
