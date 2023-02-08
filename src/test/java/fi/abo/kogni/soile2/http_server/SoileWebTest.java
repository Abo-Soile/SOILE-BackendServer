package fi.abo.kogni.soile2.http_server;

import java.io.File;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.multipart.MultipartForm;

public abstract class SoileWebTest extends SoileVerticleTest{
		
	public Future<HttpResponse<Buffer>> GET(String targetURL, JsonObject queryParameters, JsonObject queryBody)
	{
		return GET(webclient, targetURL, queryParameters, queryBody);
	}
	
	public Future<HttpResponse<Buffer>> POST(String targetURL, JsonObject queryParameters, JsonObject queryBody)
	{
		return POST(webclient, targetURL, queryParameters, queryBody);
	}
	
	public static Future<HttpResponse<Buffer>> GET(WebClient client, String targetURL, JsonObject queryParameters, JsonObject queryBody)
	{
		HttpRequest<Buffer> request = client.get(targetURL);
		if(queryParameters != null)
		{
			for(String fieldname : queryParameters.fieldNames())
			{
				request.addQueryParam(fieldname, queryParameters.getString(fieldname));
			}
		}
		if(queryBody == null)
		{
			return request.send();
		}
		else
		{
			return request.sendJson(queryBody);
		}
	}
	
	public static Future<HttpResponse<Buffer>> POST(WebClient client, String targetURL, JsonObject queryParameters, JsonObject queryBody)
	{
		HttpRequest<Buffer> request = client.post(targetURL);
		if(queryParameters != null)
		{
			for(String fieldname : queryParameters.fieldNames())
			{
				request.addQueryParam(fieldname, queryParameters.getString(fieldname));
			}
		}
		if(queryBody == null)
		{
			return request.send();
		}
		else
		{
			return request.sendJson(queryBody);
		}
	}
	
	public static Future<JsonObject> createNewElement(WebClient webClient, String elementAPI, JsonObject queryParameters)
	{
		return SoileWebTest.GET(webClient, "/" + elementAPI + "/create", queryParameters, null).map(res -> { return res.bodyAsJsonObject(); });				
	}

	public static Future<String> updateElement(WebClient webClient, String elementAPI, JsonObject element, JsonObject queryParameters)
	{
		return SoileWebTest.POST(webClient, "/" + elementAPI + "/" + element.getString("UUID") + "/" + element.getString("version"), queryParameters, element).map(res -> { return res.bodyAsJsonObject().getString("version"); });				
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
		Promise<String> versionPromise = Promise.promise();
		HttpRequest<Buffer> request = webClient.post("/task/" + TaskID + "/" + TaskVersion  + "/" + Filename);				
		MultipartForm submissionForm = MultipartForm.create()
													.binaryFileUpload(Filename, Filename, target.getAbsolutePath(), mimeType);		
		request.sendMultipartForm(submissionForm)
		.onSuccess(response -> {
			try {
				versionPromise.complete(response.bodyAsJsonObject().getString("version"));
			}
			catch(Exception e)
			{
				versionPromise.fail(e);
			}
		})
		.onFailure(err -> versionPromise.fail(err));		
		return versionPromise.future();				
	}
}
