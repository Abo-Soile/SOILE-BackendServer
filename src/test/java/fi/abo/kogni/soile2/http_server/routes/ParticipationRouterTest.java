package fi.abo.kogni.soile2.http_server.routes;

import org.junit.Test;

import fi.abo.kogni.soile2.http_server.SoileWebTest;
import fi.abo.kogni.soile2.http_server.auth.SoileAuthorization.Roles;
import fi.abo.kogni.soile2.projecthandling.utils.WebObjectCreator;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientSession;

public class ParticipationRouterTest extends SoileWebTest{

	private WebClient generatorSession;
	@Test
	public void runTaskTest(TestContext context)
	{
		Async creationAsync = context.async();
		createAndStartProject(true)
		.onSuccess(instanceID -> {
			createTokenAndSignupUser(generatorSession, instanceID)
			.onSuccess(authToken -> {
				Async codeTypeAsync = context.async();
				WebClientSession tempSession = createSession();
				tempSession.addHeader("Authorization", authToken);
				GET(tempSession, "/projectexec/" + instanceID + "/getTaskType", null, null)
				.onSuccess(response -> {
					JsonObject codeTypeInfo = response.bodyAsJsonObject();
					context.assertEquals("qmarkup", codeTypeInfo.getJsonObject("codeType", new JsonObject()).getString("language"));
					
					context.assertEquals(false, codeTypeInfo.getBoolean("finished") == null ? false : codeTypeInfo.getBoolean("finished"));
					codeTypeAsync.complete();
				})
				.onFailure(err -> context.fail(err));
				
				Async codeAsync = context.async();
				GET(tempSession, "/run/" + instanceID , null, null)
				.onSuccess(response -> {
					JsonObject compiledCode = response.bodyAsJsonObject();
					context.assertTrue(compiledCode.containsKey("elements"));
					context.assertEquals("html", compiledCode.getJsonArray("elements").getJsonArray(0).getJsonObject(0).getString("type"));
					context.assertEquals("",compiledCode.getString("title"));
					codeAsync.complete();
				})
				.onFailure(err -> context.fail(err));
				creationAsync.complete();
			})
			.onFailure(err -> context.fail(err));
		})
		.onFailure(err -> context.fail(err));
	}
	
	
	protected Future<String> signUpToProjectWithToken(WebClient client,String Token, String projectID)
	{
		Promise<String> tokenPromise = Promise.promise();
		POST(client,"/projectexec/" + projectID + "/signUp", new JsonObject().put("token", Token), null)
		.onSuccess(response -> {
			tokenPromise.complete(response.bodyAsJsonObject().getString("token"));
		})
		.onFailure(err -> tokenPromise.fail(err));
		return tokenPromise.future();
	}
	
	protected Future<Void> signUpToProject(WebClient client, String projectID)
	{
		Promise<Void> tokenPromise = Promise.promise();
		POST(client,"/projectexec/" + projectID + "/signUp", null, null)
		.onSuccess(response -> {
			tokenPromise.complete();
		})
		.onFailure(err -> tokenPromise.fail(err));
		return tokenPromise.future();
	}
	
	protected Future<JsonArray> createTokens(WebClient client, String projectID, int count, boolean unique)
	{
		Promise<JsonArray> resultPromise = Promise.promise();
		POST(client,"/projectexec/" + projectID + "/createtokens", new JsonObject().put("unique", unique).put("count", count), null )
		.onSuccess(response -> {
			if(unique)
			{
				resultPromise.complete(new JsonArray().add(response.bodyAsString()));
			}
			else
			{
				resultPromise.complete(response.bodyAsJsonArray());
			}
		})
		.onFailure(err -> resultPromise.fail(err));
		
		return resultPromise.future();
	}
	
	
	protected Future<String> createMasterToken(WebClient client, String projectID)
	{
		return createTokens(client,projectID,0,true).map(output -> {return output.getString(0);});
	}
	
	protected Future<String> createTokenAndSignupUser(WebClient authedSession, String projectID)
	{
		return createTokens(authedSession, projectID,1,false)
		.compose(tokenArray -> {
			String token = tokenArray.getString(0);
			return signUpToProjectWithToken(createSession(), token, projectID);
		});		
	}
	
	protected Future<String> createAndStartProject(boolean priv)
	{
		JsonObject projectExec = new JsonObject().put("private", priv).put("name", "New Project").put("shortcut","newShortcut"); 

		Promise<String> projectInstancePromise = Promise.promise();
		createUserAndAuthedSession("Researcher", "test", Roles.Researcher)
		.onSuccess(authedSession -> {
			generatorSession = authedSession;
			WebObjectCreator.createProject(authedSession, "Testproject")
			.onSuccess(projectData -> {				
				String projectID = projectData.getString("UUID");
				String projectVersion = projectData.getString("version");
				POST(authedSession, "/project/" + projectID + "/" + projectVersion + "/start", null,projectExec )
				.onSuccess(response -> {
					projectInstancePromise.complete(response.bodyAsJsonObject().getString("projectID"));
				})
				.onFailure(err -> projectInstancePromise.fail(err));

			})
			.onFailure(err -> projectInstancePromise.fail(err));
		})
		.onFailure(err -> projectInstancePromise.fail(err));
		return projectInstancePromise.future();
	}
	
}
