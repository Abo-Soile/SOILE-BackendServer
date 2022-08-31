package fi.abo.kogni.soile2.http_server;

import org.junit.Before;

import fi.abo.kogni.soile2.VertxTest;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientSession;

public abstract class SoileVerticleTest extends VertxTest {
	protected WebClient webclient;
	protected HttpClient httpClient;
	protected int port;
	@Before
	public void setUp(TestContext context){
		super.setUp(context);
		// We pass the options as the second parameter of the deployVerticle method.
		 //PemKeyCertOptions keyOptions = new PemKeyCertOptions();
		 //   keyOptions.setKeyPath("keypk8.pem");
		  //  keyOptions.setCertPath("cert.pem");	
		//HttpClientOptions opts = new HttpClientOptions().setSsl(true).setVerifyHost(false).setPemKeyCertOptions(keyOptions);
		port = cfg.getJsonObject("http_server").getInteger("port");

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
