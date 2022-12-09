package fi.abo.kogni.soile2.utils;

import java.io.IOException;
import java.nio.file.Files;

import org.apache.logging.log4j.Level;

import fi.aalto.scicomp.gitFs.gitProviderVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;

public class VerticleInitialiser {

	
	
	public static Future<String> startGitVerticle(Vertx vertx, Level loglevel)
	{
		Promise<String> dirPromise = Promise.promise();
		try
		{
			String gitDir = Files.createTempDirectory("soileGit").toFile().getAbsolutePath();		
			vertx.deployVerticle(new gitProviderVerticle(SoileConfigLoader.getServerProperty("gitVerticleAddress"),gitDir, loglevel))
			.onSuccess(Void -> {
				dirPromise.complete(gitDir);
			})
			.onFailure(err -> {
				dirPromise.fail(err);
			});		
		}
		catch(IOException e)
		{
			dirPromise.fail(e);		
		}
		return dirPromise.future();
	}
		
}
