package fi.abo.kogni.soile2.datamanagement.git;

import io.vertx.core.Future;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
public class ObjectManager extends GitDataRetriever<JsonObject> {
	
	public ObjectManager(EventBus bus)
	{
		super(bus,true);		
	}				
	/**
	 * Write an element to the git Repository and return the new Version.  
	 * @param target the target file containing the git file information (i.e. repo, filename and version to build on ) 
	 * @param targetUpload the Upload containing the information on where the target file (linked by the new github file) is stored. 
	 * @return a future of the new git revision after this change
	 */	 	
	public Future<String> writeElement(GitFile target, JsonObject content)	
	{							
		return manager.writeGitFile(target, content);		
	}

	@Override
	public JsonObject createElement(Object elementData, GitFile key) {		
		return (JsonObject) elementData;
	}
}
