package fi.abo.kogni.soile2.http_server.userManagement;

import org.junit.Test;
import org.junit.runner.RunWith;

import fi.abo.kogni.soile2.http_server.MongoTestBase;
import fi.abo.kogni.soile2.http_server.SoileUserManagementVerticle;
import fi.abo.kogni.soile2.http_server.userManagement.exceptions.UserAlreadyExistingException;
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
		SoileUserManager man = new SoileUserManager(MongoClient.create(vertx, config.getJsonObject("db")),
				  new MongoAuthenticationOptions(config.getJsonObject(SoileUserManagementVerticle.USER_CFG)),
				  new MongoAuthorizationOptions(config.getJsonObject(SoileUserManagementVerticle.USER_CFG)),
				  config,
				  SoileUserManagementVerticle.USER_CFG);		
		String username =  "testUser";
		String password =  "testpw";
		
		man.createUser(username, password).onComplete(id -> {
			if (id.succeeded()){
				man.createUser(username, password).onComplete(user -> {
					if(user.succeeded())
					{
						context.fail("Could not authenticate created user (" + user.cause().getMessage() + ")");
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
		SoileUserManager man = new SoileUserManager(MongoClient.create(vertx, config.getJsonObject("db")),
				  new MongoAuthenticationOptions(config.getJsonObject(SoileUserManagementVerticle.USER_CFG)),
				  new MongoAuthorizationOptions(config.getJsonObject(SoileUserManagementVerticle.USER_CFG)),
				  config,
				  SoileUserManagementVerticle.USER_CFG);		
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
	
	
	
	@Test
	public void testSessionValidity(TestContext context) {
		final Async async = context.async();
		SoileUserManager man = new SoileUserManager(MongoClient.create(vertx, config.getJsonObject("db")),
				  new MongoAuthenticationOptions(config.getJsonObject(SoileUserManagementVerticle.USER_CFG)),
				  new MongoAuthorizationOptions(config.getJsonObject(SoileUserManagementVerticle.USER_CFG)),
				  config,
				  SoileUserManagementVerticle.USER_CFG);		
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