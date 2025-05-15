package fi.abo.kogni.soile2.datamanagement.git;

import io.vertx.core.Future;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
/**
 * A Manager for Git Objects
 * @author Thomas Pfau
 *
 */
public class ObjectManager extends GitDataRetriever<JsonObject> {
	
	/**
	 * Default constructor s
	 * @param bus the {@link EventBus} for communication
	 */
	public ObjectManager(EventBus bus)
	{
		super(bus,true);		
	}				
	/**
	 * Write an element to the git Repository and return the new Version.  
	 * @param target the target file containing the git file information (i.e. repo, filename and version to build on ) 
	 * @param content the content to write to the file 
	 * @return a future of the new git revision after this change
	 */	 	
	public Future<String> writeElement(GitFile target, JsonObject content)	
	{							
		return eb.request("soile.git.writeGitFile", target.toJson().put("data", content)).map(reply -> {return (String) reply.body();});		
	}

	@Override
	public JsonObject createElement(Object elementData, GitFile key) {		
		return (JsonObject) elementData;
	}
}
