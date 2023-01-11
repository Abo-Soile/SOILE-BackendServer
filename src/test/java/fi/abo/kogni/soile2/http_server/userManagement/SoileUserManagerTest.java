package fi.abo.kogni.soile2.http_server.userManagement;

import org.junit.Test;
import org.junit.runner.RunWith;

import fi.abo.kogni.soile2.MongoTest;
import fi.abo.kogni.soile2.UserManagementTest;
import fi.abo.kogni.soile2.http_server.userManagement.exceptions.UserAlreadyExistingException;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;


@RunWith(VertxUnitRunner.class)
public class SoileUserManagerTest extends MongoTest implements UserManagementTest{
	
	@Test
	public void testUserAddition(TestContext context) {
		System.out.println("----------------------------- Testing valid and duplicate user -----------------------------");
		Async async = context.async();
		SoileUserManager man = createManager(vertx); 		
		String username =  "testUser";
		String password =  "testpw";
		
		man.createUser(username, password).onComplete(id -> {
			if (id.succeeded()){
				man.createUser(username, password).onComplete(user -> {
					if(user.succeeded())
					{
						context.fail("User Already existed but was still created!");
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
				}			
		});				
				
	}
	
	
	
	@Test
	public void testInvalidUserName(TestContext context) {
		System.out.println("----------------------------- Testing invalid user -----------------------------");
		Async async = context.async();
		SoileUserManager man = createManager(vertx);		
		String username =  "test@User2";
		String password =  "testpw";
		man.createUser("Testuser", "testpw")
		.onSuccess( res -> {
		man.createUser(username, password).onSuccess(id -> {
					context.fail("Should not allow this username!");
		})
		.onFailure(err -> async.complete());
		})
		.onFailure(err -> context.fail(err));				
				
	}
		
}