package fi.abo.kogni.soile2.datamanagement.cleanup;



import java.util.LinkedList;
import java.util.List;

import fi.abo.kogni.soile2.projecthandling.projectElements.impl.ElementManager;
import fi.abo.kogni.soile2.projecthandling.projectElements.impl.Experiment;
import fi.abo.kogni.soile2.projecthandling.projectElements.impl.Project;
import fi.abo.kogni.soile2.projecthandling.projectElements.impl.Task;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.impl.StudyManager;
import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.FindOptions;
import io.vertx.ext.mongo.MongoClient;

/**
 * Using the dependencies of each element, this cleaner checks, whether non visible elements are parts of the dependencies of visible ones
 * and delete them if they are not.
 * @author Thomas Pfau
 *
 */
public class ElementCleaner {

	MongoClient client;
	ElementManager<Project> projManager;
	ElementManager<Task> taskManager;
	ElementManager<Experiment> expManager;
	StudyManager studyManager;
	private Future<Void> cleanProjects()
	{
		Promise<Void> cleanupDone = Promise.promise();
		
		client.findWithOptions(SoileConfigLoader.getdbProperty("studyCollection"), new JsonObject(),new FindOptions().setFields(new JsonObject().put("sourceUUID",1)))
		.compose(sourceUUIDS -> {				
				JsonObject deleted = new JsonObject().put("visible", false);
				JsonArray existingUses = new JsonArray();
				for(JsonObject study : sourceUUIDS)
				{
					existingUses.add(study.getString("sourceUUID"));
				}
				JsonObject search = new JsonObject().put("$and", new JsonArray().add(new JsonObject().put("_id", new JsonObject().put( "$nin", existingUses )))
												   .add(deleted));
				return client.find(projManager.getElementSupplier().get().getTargetCollection(),search);
		})
		.onSuccess(deleteAbleProjects-> {
			List<Future> deletionFutures = new LinkedList<>();
			for(JsonObject o : deleteAbleProjects)
			{
				deletionFutures.add(projManager.removeElementAndCleanUpGit(o.getString("_id")));
			}				
			
		})
		.onFailure(err -> cleanupDone.fail(err));
		
		return cleanupDone.future();
		
	}
	
}

