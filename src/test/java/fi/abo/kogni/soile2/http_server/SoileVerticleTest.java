package fi.abo.kogni.soile2.http_server;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.apache.commons.io.FileUtils;
import org.junit.After;

import fi.abo.kogni.soile2.MongoTest;
import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientSession;

public abstract class SoileVerticleTest extends MongoTest {
	protected WebClient webclient;
	protected HttpClient httpClient;
	protected int port;
	protected String gitDataLakeDir;
	protected String resultDataLakeDir;
	protected String gitDir;
	@Override
	public void runBeforeTests(TestContext context){
		super.runBeforeTests(context);
		port = SoileConfigLoader.getServerIntProperty("port");
		setupWebClient();
		Async serverSetupAsync = context.async();
		vertx.deployVerticle(SoileServerVerticle.class.getName(), new DeploymentOptions())
		.onSuccess(Void -> 
		{
			serverSetupAsync.complete();
		})
		.onFailure(err -> context.fail(err));
	}
	
	
	@Override
	public void setupTestConfig(TestContext context)
	{
		super.setupTestConfig(context);
		try {
			gitDataLakeDir = Files.createTempDirectory("gitdataLakeDir").toFile().getAbsolutePath();
			resultDataLakeDir = Files.createTempDirectory("resultdataLakeDir").toFile().getAbsolutePath();
			gitDir = Files.createTempDirectory("gitDir").toFile().getAbsolutePath();
			// we need to Update the config for some verticles, which derive their pathes from the config.
			SoileConfigLoader.getConfig(SoileConfigLoader.HTTP_SERVER_CFG)
			.put("soileGitFolder", gitDir)
			.put("soileGitDataLakeFolder", gitDataLakeDir)
			.put("soileResultDirectory", resultDataLakeDir);
		}
		catch(IOException e)
		{
			context.fail(e);
		}
	}
	
	@Override
	public void tearDown(TestContext context)
	{
		super.tearDown(context);
		try
		{
			FileUtils.deleteDirectory(new File(gitDataLakeDir));
			FileUtils.deleteDirectory(new File(resultDataLakeDir));
			FileUtils.deleteDirectory(new File(gitDir));
		}
		catch(Exception e)
		{
			context.fail(e);
		}
	}
	
	
	protected void setupWebClient()
	{
		HttpClientOptions copts = new HttpClientOptions()
				.setDefaultHost("localhost")
				.setDefaultPort(port)
				.setSsl(true)
				.setTrustOptions(new JksOptions().setPath("server-keystore.jks").setPassword("secret"));
		httpClient = vertx.createHttpClient(copts);
		webclient = WebClient.wrap(httpClient);		
			
	}
	
	protected WebClientSession  createSession()
	{
		WebClientSession session = WebClientSession.create(webclient);
		return session;
	}
	
}
