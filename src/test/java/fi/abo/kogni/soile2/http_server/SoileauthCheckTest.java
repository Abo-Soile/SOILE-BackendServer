package fi.abo.kogni.soile2.http_server;

import org.junit.Test;
import org.junit.runner.RunWith;

import fi.abo.kogni.soile2.http_server.utils.DebugCookieStore;
import fi.abo.kogni.soile2.utils.SoileCommUtils;
import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.netty.handler.codec.http.cookie.Cookie;
import io.vertx.core.MultiMap;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClientSession;


@RunWith(VertxUnitRunner.class)
public class SoileauthCheckTest extends SoileVerticleTest{	
	

	@Test
	public void testAuthWorks(TestContext context) {
		System.out.println("***************************************** Starting Authentication Check Test *************************************************");
		Async userCreationAsync = context.async();
		try {
			JsonObject userObject = new JsonObject()
					.put("username", "testUser")
					.put("password", "testpw")
					.put("email", "This@that.com")
					.put("type", "participant")
					.put("fullname","Test User")
					.put("remember","1");
			WebClientSession session = createSession();			
			vertx.eventBus().request(SoileCommUtils.getEventBusCommand(SoileConfigLoader.USERMGR_CFG,"addUser"), 
					userObject,res -> {
						if(res.succeeded())
						{
							//Now lets see if we can authenticate this.
							//This is a participant user, so we need to auth against the participant API
							MultiMap map = createFormFromJson(userObject);
							Async login = context.async();		
							//Lets build a new session that we will attach the cookie to and try to restore this.
							WebClientSession newCookieSession = createSession();
							WebClientSession newTokenSession = createSession();
							WebClientSession unregisteredSession = createSession();								
							session.post(port,"localhost","/login").sendForm(map).onSuccess(authed ->
							{
								boolean foundSessionCookie = false;
								for(Cookie current : session.cookieStore().get(true, SoileConfigLoader.getServerProperty("domain"), SoileConfigLoader.getSessionProperty("cookiePath")))
								{
									if(current.name().equals(SoileConfigLoader.getSessionProperty("sessionCookieID")))
									{		
										//add the cookie to the other session.
										newCookieSession.cookieStore().put(current);
										foundSessionCookie = true;
									}
								}
								context.assertTrue(foundSessionCookie);
								String token = new JsonObject(authed.body().toString()).getString("token");
								context.assertTrue(token != null);
								Async cookieTest = context.async();
								Async tokenTest = context.async();
								Async failedTest = context.async();
								// test that cookie is working
								newCookieSession.get(port,"localhost","/test/auth").send().onComplete(authTest ->									
								{
									if(authTest.succeeded())
									{
										HttpResponse authWorked = authTest.result();
										try
										{												
											context.assertTrue(new JsonObject(authWorked.body().toString()).getBoolean("authenticated"));
											context.assertEquals("testUser", new JsonObject(authWorked.body().toString()).getString("user"));
											System.out.println("Cookie Auth Worked");
										}
										catch(Exception e)
										{
											context.fail(e.getMessage());
										}
									}
									else
									{
										context.fail("Couldn't complete auth test request");
									}
									System.out.println("Completing cookieTest");
									cookieTest.complete();											
								});
								// test that token is working
								newTokenSession.addHeader("Authorization", "Bearer "+ token );
								newTokenSession.get(port,"localhost","/test/auth").send().onComplete(authTest ->									
								{
									if(authTest.succeeded())
									{
										HttpResponse authWorked = authTest.result();
										try
										{
											context.assertTrue(new JsonObject(authWorked.body().toString()).getBoolean("authenticated"));
											context.assertEquals("testUser", new JsonObject(authWorked.body().toString()).getString("user"));
											System.out.println("JWT Auth Worked");
										}
										catch(Exception e)
										{
											context.fail(e.getMessage());
										}
									}
									else
									{
										context.fail("Couldn't complete auth test request");
									}
									System.out.println("Completing tokenTest");
									tokenTest.complete();											
								});
								// test that no cookie and token fails.
								unregisteredSession.get(port,"localhost","/test/auth").send().onComplete(authTest ->									
								{
									if(authTest.succeeded())
									{
										HttpResponse authWorked = authTest.result();
										System.out.println("UnauthorizedTest yielded: " + authWorked.body().toString());
										try
										{
											context.assertEquals(401,authWorked.statusCode());												
										}
										catch(Exception e)
										{
											context.fail(e.getMessage());
										}
									}
									else
									{
										context.fail("Couldn't complete auth test request");
									}
									System.out.println("Completing failed Test");
									failedTest.complete();											
								});
								System.out.println("Completing login");
								login.complete();


							}).onFailure( fail -> {
								System.out.println("Completing login, failing test");
								login.complete();
								context.fail(fail.getCause());
							});								

						}
						else
						{
							context.fail("Couldn't create user");								
						}
						System.out.println("Completing user creation");
						userCreationAsync.complete();

					});
		}
		catch(Exception e)
		{
			context.fail(e.getMessage());
			System.out.println("Completing user creation failing");
			userCreationAsync.complete();
		}
	}  


}
