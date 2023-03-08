package fi.abo.kogni.soile2.http_server;

import java.io.File;

import fi.abo.kogni.soile2.http_server.auth.SoileAuthorization.Roles;
import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientSession;
import io.vertx.ext.web.handler.HttpException;
import io.vertx.ext.web.multipart.MultipartForm;

public abstract class SoileWebTest extends SoileVerticleTest implements UserVerticleTest{

/*	public Future<HttpResponse<Buffer>> GET(String targetURL, JsonObject queryParameters, Object queryBody)
	{
		return GET(webclient, targetURL, queryParameters, queryBody);
	}

	public Future<HttpResponse<Buffer>> POST(String targetURL, JsonObject queryParameters, Object queryBody)
	{
		return POST(webclient, targetURL, queryParameters, queryBody);
	}*/


	public static Future<HttpResponse<Buffer>> GET(WebClient client, String targetURL, JsonObject queryParameters, Object queryBody)
	{
		String host = SoileConfigLoader.getServerProperty("host");
		int port = SoileConfigLoader.getServerIntProperty("port");
		HttpRequest<Buffer> request = client.get(port, host, targetURL);
		if(queryParameters != null)
		{
			for(String fieldname : queryParameters.fieldNames())
			{
				request.addQueryParam(fieldname, queryParameters.getString(fieldname));
			}
		}
		if(queryBody == null)
		{
			return request.send().compose(SoileWebTest::handleError);
		}
		else
		{

			if(queryBody instanceof JsonObject)
			{
				return request.sendJson((JsonObject)queryBody).compose(SoileWebTest::handleError);
					
			}
			else if(queryBody instanceof MultiMap)
			{
				return request.sendForm((MultiMap)queryBody).compose(SoileWebTest::handleError);
			}
			else
			{
				return request.sendBuffer(Buffer.buffer().appendString(queryBody.toString())).compose(SoileWebTest::handleError);
			}

		}
	}

	public static Future<JsonObject>  retrieveElementByName(WebClientSession webClient, String elementName, String elementType)
	{
		Promise<JsonObject> infoPromise = Promise.promise();
		// now, we need to get the Versions for the element and take the latest version.
		SoileWebTest.getElementList(webClient, elementType)
		.onSuccess(elementList -> {
			String uuid = null;
			for(int i = 0; i < elementList.size(); ++i)
			{
				if(elementList.getJsonObject(i).getString("name").equals(elementName))
				{
					uuid = elementList.getJsonObject(i).getString("uuid");
					break;
				}
			}
			if(uuid == null)
			{
				infoPromise.fail("Object supposedly exists but could not find an element in the list, possibly no access to the element");								
				return;
			}
			else
			{
				String elementID = uuid;
				SoileWebTest.getElementVersions(webClient, elementType, elementID)
				.onSuccess( res -> {
					// find the latest version.
					long newest = 0;
					String newestVersion = "";
					for(int i = 0; i < res.size(); i++)
					{
						if(res.getJsonObject(i).getLong("date") > newest)
						{
							newest = res.getJsonObject(i).getLong("date");
							newestVersion = res.getJsonObject(i).getString("version");
						}
					}
					if(newestVersion.equals(""))
					{
						infoPromise.fail("Couldn't find a version");
					}
					SoileWebTest.getElement(webClient, elementType, elementID, newestVersion)
					.onSuccess(latestJson -> {
						infoPromise.complete(latestJson);	
					})
					.onFailure(err2 -> infoPromise.fail(err2));									
				})
				.onFailure(err2 -> infoPromise.fail(err2));
			}

		})
		.onFailure(err2 -> infoPromise.fail(err2));

		return infoPromise.future();
	}

	public static Future<HttpResponse<Buffer>> POST(WebClient client, String targetURL, JsonObject queryParameters, Object queryBody)
	{
		String host = SoileConfigLoader.getServerProperty("host");
		int port = SoileConfigLoader.getServerIntProperty("port");
		HttpRequest<Buffer> request = client.post(port, host, targetURL);
		if(queryParameters != null)
		{
			for(String fieldname : queryParameters.fieldNames())
			{
				request.addQueryParam(fieldname, queryParameters.getString(fieldname));
			}
		}
		if(queryBody == null)
		{
			return request.send().compose(SoileWebTest::handleError);
		}
		else
		{
			if(queryBody instanceof JsonObject)
			{
				return request.sendJsonObject((JsonObject)queryBody).compose(SoileWebTest::handleError);
			}
			else if(queryBody instanceof MultiMap)
			{
				return request.sendForm((MultiMap)queryBody).compose(SoileWebTest::handleError);
			}
			else
			{
				return request.sendBuffer(Buffer.buffer().appendString(queryBody.toString())).compose(SoileWebTest::handleError);
			}

		}
	}

	/**
	 * Handle a erronous response;
	 * @param response
	 * @return
	 */
	public static Future<HttpResponse<Buffer>> handleError(HttpResponse<Buffer> response)
	{
		if(response.statusCode() >= 400)
		{
			return Future.failedFuture(new HttpException(response.statusCode(),response.statusMessage()));
		}
		else
		{
			return Future.succeededFuture(response);
		}
	}

	public static Future<JsonObject> createNewElement(WebClient webClient, String elementAPI, JsonObject queryParameters)
	{
		return GET(webClient, "/" + elementAPI + "/create", queryParameters, null).map(res -> { return res.bodyAsJsonObject(); });				
	}

	public static Future<JsonObject> getElement(WebClient webClient, String elementAPI, String elementID, String Version)
	{
		return GET(webClient, "/" + elementAPI + "/" + elementID + "/" + Version, null, null).map(res -> {return res.bodyAsJsonObject(); });				
	}

	public static Future<JsonArray> getElementList(WebClient webClient, String elementAPI)
	{
		return GET(webClient, "/" + elementAPI + "/list", null, null).map(res -> { return res.bodyAsJsonArray(); });				
	}

	public static Future<JsonArray> getElementVersions(WebClient webClient, String elementAPI, String elementID)
	{
		return GET(webClient, "/" + elementAPI + "/" + elementID + "/list", null, null).map(res -> { return res.bodyAsJsonArray(); });				
	}


	/** Get the new version after posting a file to update the specified filename 
	 * 
	 * @param webClient
	 * @param TaskID
	 * @param TaskVersion
	 * @param Filename
	 * @param target
	 * @return
	 */
	public static Future<String> postTaskRessource(WebClient webClient, String TaskID, String TaskVersion, String Filename, File target, String mimeType)
	{
		String URL = "/task/" + TaskID + "/" + TaskVersion  + "/resource/" + Filename;
		return upload(webClient, URL, Filename, target, mimeType, "version");		
	}		

	/** Upload a file for a executing project and receive the File ID for results. 
	 * 
	 * @param webClient
	 * @param instanceID
	 * @param target
	 * @param Filename
	 * @param mimeType
	 * @return
	 */
	public static Future<String> uploadResult(WebClient webClient, String instanceID, File target, String Filename, String mimeType)
	{
		String URL = "/projectexec/" + instanceID + "/uploaddata";
		return upload(webClient, URL, Filename, target, mimeType, "id");		
	}	
	
	public static Future<String> upload(WebClient client, String URL, String fileName, File uploadFile, String mimeType, String idField)
	{
		Promise<String> idPromise = Promise.promise();
		HttpRequest<Buffer> request = client.post(URL);				
		MultipartForm submissionForm = MultipartForm.create()
				.binaryFileUpload(fileName, fileName, uploadFile.getAbsolutePath(), mimeType);		
		request.sendMultipartForm(submissionForm)
		.onSuccess(response -> {
			try {
				idPromise.complete(response.bodyAsJsonObject().getString(idField));
			}
			catch(Exception e)
			{
				idPromise.fail(e);
			}
		})
		.onFailure(err -> idPromise.fail(err));		
		return idPromise.future();
	}

	public Future<Void> authenticateSession(WebClientSession session, String username, String password)
	{
		MultiMap map = createFormFromJson(new JsonObject().put("username", username).put("password", password).put("remember", "1"));
		return POST(session, "/login", null, map).mapEmpty();		
	}
	/**
	 * Will authenticate the user with a token.
	 */
	public Future<Void> authenticateRequest(HttpRequest<Buffer> request, String username, String password)
	{
		return authenticateRequest(request, username, password, false, null);
	}

	public Future<Void> authenticateRequest(HttpRequest<Buffer> request, String username, String password, boolean createUser, Roles role )
	{
		Promise<Void> authPromise = Promise.promise();	
		// create 
		createUserForRequest(username, password, role, createUser)
		.compose(done -> {
			Promise<Void> authFinishedPromise = Promise.promise();
			authUser(username, password)
			.onSuccess( response -> {
				// now we have to set the right authentication header.
				request.bearerTokenAuthentication(response.bodyAsJsonObject().getString("token"));
				authFinishedPromise.complete();

			})
			.onFailure(err -> authFinishedPromise.fail(err));
			return authFinishedPromise.future();
		}).onSuccess(authed ->  {
			authPromise.complete();

		})
		.onFailure(err -> authPromise.fail(err));

		return authPromise.future();
	}

	public Future<HttpResponse<Buffer>> authUser(String username, String password)
	{
		Promise<HttpResponse<Buffer>> responsePromise = Promise.<HttpResponse<Buffer>>promise();
		MultiMap map = createFormFromJson(new JsonObject().put("username", username).put("password", password).put("remember", "0"));
		POST(webclient,"/login", null , map)
		.onSuccess(response -> {
			responsePromise.complete(response);
		})
		.onFailure(err -> responsePromise.fail(err));
		return responsePromise.future();
	}

	private Future<Void> createUserForRequest(String username, String password, Roles role,  boolean create)
	{
		if(!create)
		{
			return Future.succeededFuture();
		}
		else
		{					
			return createUser(vertx, username, password, role);
		}
	}

	protected Future<WebClientSession> createUserAndAuthedSession(String username, String password, Roles role)
	{
		WebClientSession currentSession = createSession();
		return createUser(vertx, username, password, role)
				.compose(userCreated -> { return authenticateSession(currentSession, username, password);})
				.compose(authed -> {return Future.succeededFuture(currentSession);});
	}
	
	protected Future<WebClientSession> createAuthedSession(String username, String password)
	{
		WebClientSession currentSession = createSession();
		return authenticateSession(currentSession, username, password)
				.compose(authed -> {return Future.succeededFuture(currentSession);});
	}
}
