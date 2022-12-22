package fi.abo.kogni.soile2.http_server.verticles;


import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fi.aalto.scicomp.gitFs.gitProviderVerticle;
import fi.abo.kogni.soile2.datamanagement.git.GitFile;
import fi.abo.kogni.soile2.datamanagement.git.GitManager;
import fi.abo.kogni.soile2.http_server.codeProvider.CodeProvider;
import fi.abo.kogni.soile2.http_server.codeProvider.CompiledCodeProvider;
import fi.abo.kogni.soile2.http_server.codeProvider.JSCodeProvider;
import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;

public class CodeRetrieverVerticle extends AbstractVerticle {

	static final Logger LOGGER = LogManager.getLogger(CodeRetrieverVerticle.class);

	public final static String JAVASCRIPT = "javascript";
	public final static String ELANG = "elang";
	public final static String QMARKUP = "qmarkup";
	private CodeProvider elangProvider;
	private CodeProvider qmarkupProvider;
	private CodeProvider psychoJsProvider;
	public CodeRetrieverVerticle(GitManager manager)
	{
		elangProvider = new CompiledCodeProvider(SoileConfigLoader.getVerticleProperty("elangAddress"),vertx.eventBus(),manager);
		qmarkupProvider = new CompiledCodeProvider(SoileConfigLoader.getVerticleProperty("questionnaireAddress"),vertx.eventBus(),manager);
		psychoJsProvider = new JSCodeProvider(manager);
	}
	
	@Override
	public void start(Promise<Void> startPromise)
	{
		
		LOGGER.debug("Deploying CodeRetriever with id : " + deploymentID());
		vertx.eventBus().consumer(SoileConfigLoader.getVerticleProperty("compilationAddress"), this::compileCode);
		vertx.eventBus().consumer(SoileConfigLoader.getVerticleProperty("gitCompilationAddress"), this::compileGitCode);				
	}
	
	
	@Override
	public void stop(Promise<Void> stopPromise)
	{
		List<Future> undeploymentFutures = new LinkedList<Future>();
		undeploymentFutures.add(vertx.eventBus().consumer(SoileConfigLoader.getVerticleProperty("compilationAddress"), this::compileCode).unregister());
		undeploymentFutures.add(vertx.eventBus().consumer(SoileConfigLoader.getVerticleProperty("gitCompilationAddress"), this::compileGitCode).unregister());
		CompositeFuture.all(undeploymentFutures).mapEmpty().
		onSuccess(v -> stopPromise.complete())
		.onFailure(err -> stopPromise.fail(err));			
	}
	
	private void compileCode(Message<JsonObject> sourceCode)
	{
		String type = sourceCode.body().getString("type");
		String code = sourceCode.body().getString("code");
		CodeProvider provider = getProviderForType(type);
		provider.compileCode(code)
		.onSuccess(compiledCode -> {			
			sourceCode.reply(new JsonObject().put("code", compiledCode));
		})
		.onFailure(err -> sourceCode.fail(200, err.getMessage()));
	}
	
	private void compileGitCode(Message<JsonObject> codeLocation)
	{
		String type = codeLocation.body().getString("type");
		String id = codeLocation.body().getString("id");
		String version = codeLocation.body().getString("version");
		CodeProvider provider = getProviderForType(type);
		// this is always a Task object (if not, there will not be code).
		GitFile f = new GitFile("Object.json", id, version);		
		provider.getCode(f)
		.onSuccess(compiledCode -> {			
			codeLocation.reply(new JsonObject().put("code", compiledCode));
		})
		.onFailure(err -> codeLocation.fail(200, err.getMessage()));
	}
	
	
	private CodeProvider getProviderForType(String type)
	{
		switch(type)
		{
			case JAVASCRIPT: return psychoJsProvider;
			case ELANG: return elangProvider;
			case QMARKUP: return qmarkupProvider;
			default: return null;
		}
	}
	
}
