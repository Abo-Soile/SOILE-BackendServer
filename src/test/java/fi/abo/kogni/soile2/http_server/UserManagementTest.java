package fi.abo.kogni.soile2.http_server;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.vertx.core.AsyncResult;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

/**
 * Unit tests for the User Management code (i.e. permission setting/removing, user addition etc pp)
 */
@RunWith(VertxUnitRunner.class)
public class UserManagementTest extends MongoTestBase{

	String getCommand(String commandString)
	{
		return uconfig.getString("commandPrefix") + uconfig.getJsonObject("commands").getString(commandString);
	}
	/**
	 * Before executing our test, let's deploy our verticle.
	 * <p/>
	 * This method instantiates a new Vertx and deploy the verticle. Then, it waits in the verticle has successfully
	 * completed its start sequence (thanks to `context.asyncAssertSuccess`).
	 *
	 * @param context the test context.
	 */
	@Before
	public void setUp(TestContext context){
		super.setUp(context);
		// We pass the options as the second parameter of the deployVerticle method.
		vertx.deployVerticle(SoileServerVerticle.class.getName(), new DeploymentOptions(), context.asyncAssertSuccess());
	}

	@Test
	public void testUserAddition(TestContext context) {		
		Async async = context.async();
		try {
			createUser("testUser", res -> {					
						if (res.succeeded())
						{									
							JsonObject obj = (JsonObject)res.result().body();					
							context.assertEquals("Success",obj.getValue("Result"));
							createUser("testUser",invRes ->
								{
									if(invRes.succeeded())
									{
										JsonObject invobj = (JsonObject)invRes.result().body();
										context.assertEquals("Error", invobj.getString("Result"));
										context.assertEquals("User Exists", invobj.getString("Reason"));
										async.complete();
									}
									else
										{							
										context.fail("Invalid user errored unexpectedly: " + invRes.cause().getMessage());
										async.complete();
										}
								});													

						}					
						else
						{
							System.out.println(res.cause());
							context.fail("No response found");
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


	@Test
	public void testAuthentication(TestContext context) {
		Async async = context.async();
		try {
			JsonObject userObject = new JsonObject()
					.put("username", "testUser")
					.put("password", "testpw")
					.put("email", "This@that.com")
					.put("type", "participant")
					.put("fullname","Test User");
			vertx.eventBus().request(getCommand("addUser"), 
					userObject).onComplete(res -> {
							async.complete();
					});
		}
		catch(Exception e)
		{
			context.fail();
			async.complete();
		}
	} 

	@Test
	public void testInvalidAuthentication(TestContext context) {
		Async async = context.async();
		try {
			JsonObject userObject = new JsonObject()
					.put("username", "testUser")
					.put("password", "testpw")
					.put("email", "This@that.com")
					.put("type", "participant")
					.put("fullname","Test User");
			vertx.eventBus().request(getCommand("addUser"), 
					userObject).onComplete(res -> {
						async.complete();
					});
		}
		catch(Exception e)
		{
			context.fail();
			async.complete();
		}
	} 

	@Test
	public void testRemoveUser(TestContext context) {
		Async async = context.async();
		try {
			JsonObject userObject = new JsonObject()
					.put("username", "testUser")
					.put("password", "testpw")
					.put("email", "This@that.com")
					.put("type", "participant")
					.put("fullname","Test User");
			vertx.eventBus().request("umanager.adduser", 
					userObject).onComplete(res -> {
						if (res.succeeded())
						{
							JsonObject obj = (JsonObject)res.result().body();					
							context.assertEquals("Success",obj.getValue("Result"));
							System.out.println("User Creation Test Successfull");
							async.complete();
						}	
						else
						{
							context.fail();
							async.complete();
						}

					});
		}
		catch(Exception e)
		{
			context.fail();
			async.complete();
		}
	} 

	private Future<Message<Object>> createUser(String username, Handler<AsyncResult<Message<Object>>> handler)
	{
		JsonObject userObject = new JsonObject()
				.put("username", username)
				.put("password", "testpw")
				.put("email", "This@that.com")
				.put("type", "participant")
				.put("fullname","Test User");
		return vertx.eventBus().request("umanager.adduser", 
				userObject).onComplete(res -> {
					handler.handle(res);					
				});
	}
}
