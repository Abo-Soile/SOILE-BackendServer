package fi.abo.kogni.soile2.http_server;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import fi.abo.kogni.soile2.http_server.utils.DebugCookieStore;
import fi.abo.kogni.soile2.utils.SoileCommUtils;
import io.netty.handler.codec.http.cookie.Cookie;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientSession;


@RunWith(VertxUnitRunner.class)
public class SoileAuthenticationVerticleTest extends MongoTestBase{

	private WebClient webclient;
	private HttpClient httpClient;
	private int port;
	@Before
	public void setUp(TestContext context){
		super.setUp(context);
		// We pass the options as the second parameter of the deployVerticle method.
		 //PemKeyCertOptions keyOptions = new PemKeyCertOptions();
		 //   keyOptions.setKeyPath("keypk8.pem");
		  //  keyOptions.setCertPath("cert.pem");	
		//HttpClientOptions opts = new HttpClientOptions().setSsl(true).setVerifyHost(false).setPemKeyCertOptions(keyOptions);
		port = cfg.getJsonObject("http_server").getInteger("port");

		HttpClientOptions copts = new HttpClientOptions()
								      .setDefaultHost("localhost")
								      .setDefaultPort(port)
								      .setSsl(true)
								      .setTrustOptions(new JksOptions().setPath("server-keystore.jks").setPassword("secret"));
		httpClient = vertx.createHttpClient(copts);
		webclient = WebClient.wrap(httpClient);		
		vertx.deployVerticle(SoileServerVerticle.class.getName(), new DeploymentOptions(), context.asyncAssertSuccess());
	}
	
	
	
	@Test
	public void testAuthentication(TestContext context) {
		Async async = context.async();
		try {
			JsonObject userObject = new JsonObject()
					.put("username", "testUser")
					.put("password", "testpw")
					.put("email", "This@that.com")
					.put("type", "participant")
					.put("fullname","Test User")
					.put("remember","1");
			WebClientSession session = WebClientSession.create(webclient, new DebugCookieStore());			
			vertx.eventBus().request(SoileCommUtils.getEventBusCommand(uCfg,"addUser"), 
					userObject,res -> {
							if(res.succeeded())
							{
								//Now lets see if we can authenticate this.
								//This is a participant user, so we need to auth against the participant API
								MultiMap map = createFormFromJson(userObject);
								final Async sucAsync = context.async();		
								System.out.println("Posting to https://localhost/auth");
								//Lets build a new session that we will attach the cookie to and try to restore this.
								WebClientSession newsession = buildSession();								
								session.post(port,"localhost","/auth").sendForm(map).onSuccess(authed ->
								{
									boolean foundSessionCookie = false;
									for(Cookie current : session.cookieStore().get(true, serverCfg.getString("domain"), sessionCfg.getString("cookiePath")))
									{
										System.out.println("Found Cookie: " + current.toString());
										if(current.name().equals(sessionCfg.getString("sessionCookieID")))
										{		
											//add the cookie to the other session.
											newsession.cookieStore().put(current);
											foundSessionCookie = true;
										}
									}
									System.out.println("Valid Auth: " + authed.body().toString());
									context.assertTrue(foundSessionCookie);
									context.assertTrue(authed.body().toString().contains("Login successful"));																		
									
									sucAsync.complete();	
								});
								
								final Async unsucAsync = context.async();
								userObject.put("type", "user");
								map = createFormFromJson(userObject);
								webclient.post(port,"localhost","/auth").sendForm(map).onSuccess(authed ->
								{
									System.out.println("Invalid Auth: " +  authed.body().toString());
									context.assertTrue(authed.body().toString().contains("Redirecting to /login"));
									context.assertEquals(302,authed.statusCode());
									unsucAsync.complete();	
								});
								async.complete();
							}
							else
							{
								context.fail("Couldn't create user");
								async.complete();
							}
							
						});
		}
		catch(Exception e)
		{
			context.fail(e.getMessage());
			async.complete();
		}
	}  
	
	private WebClientSession buildSession()
	{
		HttpClientOptions copts = new HttpClientOptions()
				.setDefaultHost("localhost")
				.setDefaultPort(port)
				.setSsl(true)
				.setTrustOptions(new JksOptions().setPath("server-keystore.jks").setPassword("secret"));
		httpClient = vertx.createHttpClient(copts);
		webclient = WebClient.wrap(httpClient);
		WebClientSession session = WebClientSession.create(webclient, new DebugCookieStore());
		return session;

	}
}
