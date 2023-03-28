package fi.abo.kogni.soile2.http_server;

import org.junit.Test;

import fi.abo.kogni.soile2.http_server.utils.DebugCookieStore;
import fi.abo.kogni.soile2.utils.SoileCommUtils;
import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.netty.handler.codec.http.cookie.Cookie;
import io.vertx.core.MultiMap;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.web.client.WebClientSession;

public class SoileLoginTest extends SoileVerticleTest {

	/**
	 * A Valid login attempt
	 * @param context
	 */
	@Test
	public void testValidAuthentication(TestContext context) {
		System.out.println("--------------------  Testing Valid Auth ----------------------");		 

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
			vertx.eventBus().request(SoileCommUtils.getEventBusCommand(SoileConfigLoader.USERMGR_CFG,"addUser"), 
					userObject,res -> {
							if(res.succeeded())
							{
								//Now lets see if we can authenticate this.
								//This is a participant user, so we need to auth against the participant API
								MultiMap map = createFormFromJson(userObject);
								final Async sucAsync = context.async();		
								//Lets build a new session that we will attach the cookie to and try to restore this.
								WebClientSession newsession = createSession();								
								session.post(port,"localhost","/login").sendForm(map).onSuccess(authed ->
								{
									boolean foundSessionCookie = false;
									for(Cookie current : session.cookieStore().get(true, SoileConfigLoader.getServerProperty("domain"), SoileConfigLoader.getSessionProperty("cookiePath")))
									{
										if(current.name().equals(SoileConfigLoader.getSessionProperty("sessionCookieID")))
										{		
											//add the cookie to the other session.
											newsession.cookieStore().put(current);
											foundSessionCookie = true;
										}
									}
									context.assertTrue(foundSessionCookie);
									context.assertEquals(200,authed.statusCode());
									context.assertTrue(new JsonObject(authed.body().toString()).fieldNames().contains("token"));																		
									
									sucAsync.complete();	
									async.complete();
								});								
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
	
	/**
	 * A Valid login attempt
	 * @param context
	 */
	@Test
	public void testLogOut(TestContext context) {
		System.out.println("--------------------  Testing Logout----------------------");		 

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
			vertx.eventBus().request(SoileCommUtils.getEventBusCommand(SoileConfigLoader.USERMGR_CFG,"addUser"), 
					userObject,res -> {
							if(res.succeeded())
							{
								//Now lets see if we can authenticate this.
								//This is a participant user, so we need to auth against the participant API
								MultiMap map = createFormFromJson(userObject);
								final Async sucAsync = context.async();		
								//Lets build a new session that we will attach the cookie to and try to restore this.
								WebClientSession newsession = createSession();								
								session.post(port,"localhost","/login").sendForm(map).onSuccess(authed ->
								{
									boolean foundSessionCookie = false;
									for(Cookie current : session.cookieStore().get(true, SoileConfigLoader.getServerProperty("domain"), SoileConfigLoader.getSessionProperty("cookiePath")))
									{
										if(current.name().equals(SoileConfigLoader.getSessionProperty("sessionCookieID")))
										{		
											//add the cookie to the other session.
											newsession.cookieStore().put(current);
											foundSessionCookie = true;
										}
									}
									context.assertTrue(foundSessionCookie);
									context.assertEquals(200,authed.statusCode());
									context.assertTrue(new JsonObject(authed.body().toString()).fieldNames().contains("token"));																		
									session.post(hashingAlgo);
									sucAsync.complete();	
									async.complete();
								});								
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
	
	
	/**
	 * An invalid login attempt
	 * @param context
	 */
	@Test
	public void testInValidAuthentication(TestContext context) {
		System.out.println("--------------------  Testing Invalid Auth ----------------------");		 

		Async async = context.async();
		try {
			JsonObject userObject = new JsonObject()
					.put("username", "testUser")
					.put("password", "testpw")
					.put("email", "This@that.com")
					.put("type", "participant")
					.put("fullname","Test User")
					.put("remember","1");					
			vertx.eventBus().request(SoileCommUtils.getEventBusCommand(SoileConfigLoader.USERMGR_CFG,"addUser"), 
					userObject,res -> {
							if(res.succeeded())
							{
								//Now lets see if we can authenticate this.
								//This is a participant user, so we need to auth against the participant API
								MultiMap map = createFormFromJson(userObject);																
								final Async unsucAsync = context.async();
								userObject.put("password", "user");
								map = createFormFromJson(userObject);
								webclient.post(port,"localhost","/login").sendForm(map).onSuccess(authed ->
								{
									context.assertTrue(authed.body().toString().contains("Unauthorized"));
									context.assertEquals(401,authed.statusCode());
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
}
