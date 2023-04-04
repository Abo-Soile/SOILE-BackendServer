package fi.abo.kogni.soile2.http_server.routes;

import org.junit.Test;

import fi.abo.kogni.soile2.http_server.SoileWebTest;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.handler.HttpException;

public class BasicWebTests extends SoileWebTest {

	
	/**
	 * This test tests both starting and getting the list of running projects.
	 * @param context
	 */
	@Test
	public void testSessionUserCreation(TestContext context)
	{
		System.out.println("--------------------  Running Tests for sessions  ----------------------");    

		WebClient unAuthedSession = createSession();

		Async unAuthAsync = context.async();
		System.out.println("------------------------Using an unauthed Session to retrieve information -----------------------------------");
		POST(unAuthedSession, "/projectexec/list", null,null)
		.onSuccess(listresponse -> {							
			POST(unAuthedSession, "/projectexec/list", null,null)
			.onSuccess(listresponse2 -> {
				POST(unAuthedSession, "/test/auth", null,null)
				.onSuccess(authResponse -> {
					context.fail("Should be unauthorized");					
				})
				.onFailure(err -> {
					context.assertEquals(401, ((HttpException)err).getStatusCode());
					unAuthAsync.complete();	
				});				
			})
			.onFailure(err -> context.fail(err));

		})
		.onFailure(err -> context.fail(err));
	}
	
}
