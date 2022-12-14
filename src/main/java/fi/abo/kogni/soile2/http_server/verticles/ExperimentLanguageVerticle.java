package fi.abo.kogni.soile2.http_server.verticles;

import java.io.StringReader;
import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupFile;

import fi.abo.kogni.soile2.elang.CodeGenerator;
import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;

public class ExperimentLanguageVerticle extends SoileBaseVerticle {

	private STGroup template1;	
	private String address;
	static final Logger LOGGER = LogManager.getLogger(ExperimentLanguageVerticle.class);
	
    @Override
    public void start(Promise<Void> startPromise) {
        
		LOGGER.debug("Deploying ExperimentLanguageVerticle with id : " + deploymentID());
    	address = SoileConfigLoader.getVerticleProperty("elangAddress");
        String templateFile = SoileConfigLoader.getVerticleProperty("code-gen-template");
        String fullFileName = ExperimentLanguageVerticle.class.getClassLoader().getResource(templateFile).getFile();
	        template1 = new STGroupFile(fullFileName);        
        vertx.eventBus().consumer(address, this::getCodeTemplate);        
        startPromise.complete();
    }

	@Override
	public void stop(Promise<Void> stopPromise)
	{
		vertx.eventBus().consumer(address, this::getCodeTemplate).unregister().
		onSuccess(v -> stopPromise.complete())
		.onFailure(err -> stopPromise.fail(err));			
	}
	
    private void getCodeTemplate(Message<JsonObject> message) {
    	JsonObject json = message.body();
    	JsonObject reply = new JsonObject();
    	CodeGenerator codeGen = new CodeGenerator(template1);
    	StringBuilder code = new StringBuilder(); 
    	StringBuilder errors = new StringBuilder();
    	codeGen.codeOutputTo(code);
    	codeGen.errorMessagesTo(errors);
    	StringReader input = new StringReader(json.getString("code"));
    	codeGen.generate(input);
    	if (errors.length() > 0) {
    		reply.put("errors", errors.toString());
    	}
    	else {
    		reply.put("code", code.toString());
    	}
    	message.reply(reply);
    }
}

