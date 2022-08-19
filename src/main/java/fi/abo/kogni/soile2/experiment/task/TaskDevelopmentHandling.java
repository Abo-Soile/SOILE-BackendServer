package fi.abo.kogni.soile2.experiment.task;


import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.RoutingContext;

/**
 * This Class provides methods to handle loading (for modification) and writing Tasks, along with access to task resources etc.  
 * 
 * @author Thomas Pfau
 *
 */
public class TaskDevelopmentHandling {
	
	MongoClient client;
	String taskCollection;
	
	/**
	 * Load the Task Data, a json of the following format:
	 * Task: 
	 *	{ 
	 *		UUID: String (UUID of the Task)
	 *		Version : String (Version of the code file),
	 *		Date: DD-MM-YYYY:HH-MM,
	 * 		Resources :  
	 *			[name1: "UUID1", name2: "UUID2", ...],  # The UUIDs obtained from uploading the target files. names are the names as named in the code.
	 *		Type: 	String (Can be e.g. js, elang, or qlang),
	 *		
	 *		
	 *		Outputs : [  # Only outputs that are necessary for Filters need to be defined here.
	 *				{name: String,
	 *				type : String (Either column, value or similar)},
	 *				...
	 *		]
	 *	}
	 * The used entry point for this handler is:
	 * GET: /task/:id/:version
	 * @param event
	 */
	public void loadTask(RoutingContext context) {
		
		String taskID = context.pathParam("id");
		String taskVersion = context.pathParam("version");
		
		//TODO: Try to retrieve the task from the in-memory task information. This should either collect the data from  
		
	}
	/**
	 * Save the code version, and get an updated version ID
	 * Task: 
	 *	{
	 *		
	 *	}
	 * The used entry point for this handler is:
	 * POST: /task/:id/:version
	 * @param event
	 */
	public void updateCode(RoutingContext context)
	{
		context.request().body().onSuccess( res -> {
			//TODO: Obtain the file, post it to the git repo, return the new commit ID.  
		});
	}
	
	/**
	 * Entrypoint: POST: /task/:id/:version/resource/add/:name
	 * Posting a file to upload. 
	 * Returns Updated ID of the Task.  
	 */
	public void addResource(RoutingContext context)
	{
		// Get Name, version and tsk ID
		// Create the respective the respective 
	}
	 

}
