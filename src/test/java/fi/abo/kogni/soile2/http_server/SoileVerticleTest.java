package fi.abo.kogni.soile2.http_server;

import fi.abo.kogni.soile2.GitTest;
import fi.abo.kogni.soile2.MongoTest;
import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientSession;

public abstract class SoileVerticleTest extends GitTest {
	protected WebClient webclient;
	protected HttpClient httpClient;
	protected int port;
	
	@Override
	public void runBeforeTests(TestContext context){
		super.runBeforeTests(context);
		port = SoileConfigLoader.getServerIntProperty("port");
		setupWebClient();
		vertx.deployVerticle(SoileServerVerticle.class.getName(), new DeploymentOptions(), context.asyncAssertSuccess());
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
