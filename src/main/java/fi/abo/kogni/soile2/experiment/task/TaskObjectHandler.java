package fi.abo.kogni.soile2.experiment.task;
import fi.aalto.scicomp.gitFs.gitProviderVerticle;
import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;

public class TaskObjectHandler {

	private String targetRepo;
	private EventBus eb;
	private String eventBusAddress;
	
	public TaskObjectHandler(String Targetrepo, Vertx vertx)
	{
		this.targetRepo = Targetrepo;
		eb = vertx.eventBus();
		eventBusAddress = SoileConfigLoader.getServerProperty("gitVerticleAddress");	
	}
	
	/**
	 * Save a file meta-information to the git repository.
	 * @param FileName The used filename of the given file in the Task
	 * @param PathToFile The Path to the file
	 * @param BaseVersion The version to base the save command on. 
	 * @param resultHandler Some handler communicating with the frontend to provide the new commit hash. 
	 */
	public void saveFile(String FileName, String PathToFile, String BaseVersion, Handler<JsonObject> resultHandler)
	{
		JsonObject metaData = new JsonObject();
		JsonObject fileInformation = new JsonObject();		
		metaData.put("FileName", PathToFile);		
		eb.request(eventBusAddress,gitProviderVerticle.createWriteCommand(targetRepo, BaseVersion, metaData.encodePrettily(), "resources/ "+ FileName),reply ->  
		{
			if(reply.succeeded())
			{
				JsonObject result = new JsonObject();
				// this contains the Commit hash, which needs to be reported back to the User.
				// in order to know which version to work from.
				resultHandler.handle(result);
			}
			else
			{
				resultHandler.handle(new JsonObject().clear().put("Error", reply.cause().getMessage()));
			}
		});	
	}

	/**
	 * Save a file meta-information to the git repository.
	 * @param FileName The used filename of the given file in the Task
	 * @param PathToFile The Path to the file
	 * @param BaseVersion The version to base the save command on. 
	 * @param resultHandler Some handler communicating with the frontend to provide the new commit hash. 
	 */
	public void saveVersion(String Code, String VersionName, String BaseVersion, Handler<JsonObject> resultHandler)
	{		 	
		eb.request(eventBusAddress,gitProviderVerticle.createWriteCommand(targetRepo, BaseVersion, Code, "source", VersionName),reply ->  
		{
			if(reply.succeeded())
			{
				JsonObject result = new JsonObject();
				// this contains the Commit hash, which needs to be reported back to the User.
				// in order to know which version to work from.
				resultHandler.handle(result);
			}
			else
			{
				resultHandler.handle(new JsonObject().clear().put("Error", reply.cause().getMessage()));
			}
		});	
	}
	
	/**
	 * Save a file meta-information to the git repository.
	 * @param FileName The used filename of the given file in the Task
	 * @param PathToFile The Path to the file
	 * @param BaseVersion The version to base the save command on. 
	 * @param resultHandler Some handler communicating with the frontend to provide the new commit hash. 
	 */
	public void getVersion(String Code, String VersionName, String BaseVersion, Handler<JsonObject> resultHandler)
	{		 	
		eb.request(eventBusAddress,gitProviderVerticle.createWriteCommand(targetRepo, BaseVersion, Code, "source", VersionName),reply ->  
		{
			if(reply.succeeded())
			{
				JsonObject result = new JsonObject();
				// this contains the Commit hash, which needs to be reported back to the User.
				// in order to know which version to work from.
				resultHandler.handle(result);
			}
			else
			{
				resultHandler.handle(new JsonObject().clear().put("Error", reply.cause().getMessage()));
			}
		});	
	}
	
	
	
}
