package fi.abo.kogni.soile2.http_server.userManagement;

import org.junit.Test;
import org.junit.runner.RunWith;

import fi.abo.kogni.soile2.http_server.MongoTestBase;
import fi.abo.kogni.soile2.http_server.userManagement.exceptions.UserAlreadyExistingException;
import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.vertx.ext.auth.mongo.MongoAuthenticationOptions;
import io.vertx.ext.auth.mongo.MongoAuthorizationOptions;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;


@RunWith(VertxUnitRunner.class)
public class SoileUserManagerTest extends MongoTestBase{
	
	@Test
	public void testUserAddition(TestContext context) {
		final Async async = context.async();
		SoileUserManager man = createManager(); 		
		String username =  "testUser";
		String password =  "testpw";
		
		man.createUser(username, password).onComplete(id -> {
			if (id.succeeded()){
				man.createUser(username, password).onComplete(user -> {
					if(user.succeeded())
					{
						context.fail("User Already existed but was still created!");
						async.complete();
					}
					else
					{
						context.assertTrue(user.cause() instanceof UserAlreadyExistingException);
						async.complete();
					}
				});
			}
				else
				{
					context.fail("Could not create user ( " + id.cause().getMessage() + ")");
					async.complete();
				}			
		});				
				
	}
	
	
	@Test
	public void testSetUserNameAndPassword(TestContext context) {
		final Async async = context.async();
		SoileUserManager man = createManager();		
		String username =  "testUser2";
		String password =  "testpw";
		String email = "test@test.blubb";
		String fullname = "Test User";
		System.out.println("Set up test");
		man.createUser(username, password).onComplete(id -> {
			if (id.succeeded()){
				System.out.println("Created user");
				man.setEmailAndFullName(username,email,fullname, res ->{
					if(res.succeeded())
					{
						System.out.println("Set Email");
						man.getUserData(username, uData -> {
							if(uData.succeeded())
							{
								System.out.println("Got Data");
								context.assertEquals(username,uData.result().getString("username"));
								context.assertEquals(email,uData.result().getString("email"));
								context.assertEquals(fullname,uData.result().getString("fullname"));
								async.complete();		
								}
							else
							{								
								context.fail("Could not retrieve data for user");
								async.complete();		
							}
							
						});
						
					}
					else
					{
						context.fail("Could not set email address and full name for user.(" + res.cause().getMessage() + ")");
						async.complete();
					}
				});
			}
				else
				{
					context.fail("Could not create user. (" + id.cause().getMessage() + ")");
					async.complete();
				}			
		});				
				
	}
	
	@Test
	public void testInvalidUserName(TestContext context) {
		final Async async = context.async();
		SoileUserManager man = createManager();		
		String username =  "test@User2";
		String password =  "testpw";
		man.createUser(username, password).onComplete(id -> {
			if (id.succeeded())
			{
					context.fail("Should not allow this username!");
					async.complete();		
			}
			else
			{
					async.complete();
			}			
		});				
				
	}
	
	@Test
	public void testSessionValidity(TestContext context) {
		final Async async = context.async();
		SoileUserManager man = createManager();		
		String username =  "testUser2";
		String password =  "testpw";
		String email = "test@test.blubb";
		String fullname = "Test User";
		man.createUser(username, password).onComplete(id -> {
			if (id.succeeded()){
				man.setEmailAndFullName(username,email,fullname, res ->{
					if(res.succeeded())
					{		
						man.getUserData(username, uData -> {
							if(uData.succeeded())
							{
								context.assertEquals(username,uData.result().getString("username"));
								context.assertEquals(email,uData.result().getString("email"));
								context.assertEquals(fullname,uData.result().getString("fullname"));
								async.complete();		
								}
							else
							{								
								context.fail("Could not retrieve data for user");
								async.complete();		
							}
							
						});
						
					}
					else
					{
						context.fail("Could not set email address and full name for user.(" + res.cause().getMessage() + ")");
						async.complete();
					}
				});
			}
				else
				{
					context.fail("Could not create user. (" + id.cause().getMessage() + ")");
					async.complete();
				}			
		});				
				
	}
		
}