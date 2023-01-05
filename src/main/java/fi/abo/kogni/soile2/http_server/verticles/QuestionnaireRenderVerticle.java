package fi.abo.kogni.soile2.http_server.verticles;

import java.nio.ByteBuffer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.vertx.core.AsyncResult;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import fi.abo.kogni.soile2.qmarkup.InputReader;
import fi.abo.kogni.soile2.qmarkup.QuestionnaireBuilder;
import fi.abo.kogni.soile2.qmarkup.typespec.MalformedCommandException;
import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import fi.abo.kogni.soile2.utils.generator.IdGenerator;



// This verticle handles generation, verification of questionnaires.

public final class QuestionnaireRenderVerticle extends SoileBaseVerticle {

	private QuestionnaireBuilder builder;
	private IdGenerator generator;
	private String template;
	static final Logger LOGGER = LogManager.getLogger(QuestionnaireRenderVerticle.class);

	@Override
	public void start() {
		LOGGER.debug("Deploying ExperimentLanguageVerticle with id : " + deploymentID());
		template = SoileConfigLoader.getVerticleProperty("questionaireTemplates");
		generator = IdGenerator.shortIdGenerator();
		builder = new QuestionnaireBuilder(ExperimentLanguageVerticle.class.getClassLoader().getResource(template).getFile());
		generator.seed(1024);
		generator.init();
		String address = SoileConfigLoader.getVerticleProperty("questionnaireAddress");
    	LOGGER.debug("Registering Questionaire Verticle at address " + address);
		vertx.eventBus().consumer(address, this::handle);		
	}
	// only one of this should ever be called simultaneously.
	// we might want to put in a few of these as worker verticles.
	private synchronized void handle(Message<JsonObject> message) {
		JsonObject json = message.body();
		JsonObject reply = new JsonObject();
		String markup = json.getString("code");            
		InputReader reader = new InputReader(markup);
		reader.addListener(builder);
		builder.questionnaireId("questionnaire-id");
		//This key is currently completely unused
		builder.encryptionKey("vr7DlZqAyY061Y9M");

		try {
			reader.processInput();
			builder.finish();
			String output = builder.output();

			reply.put("code", output);
		} catch (MalformedCommandException e) {
			reply.put("error", e.getMessage());                
		}
		finally
		{
			builder.reset();
			generator.reset();	
		}
		message.reply(reply);
	}

}


