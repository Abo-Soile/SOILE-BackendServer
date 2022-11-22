package fi.abo.kogni.soile2.projecthandling.projectElements;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonProperty;

import fi.abo.kogni.soile2.projecthandling.exceptions.ObjectDoesNotExist;
import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;

/**
 * This class represents a Project as stored in the mongoDB Project Database.
 * It essentially represents the Json object represented in the Database.
 * @author Thomas Pfau
 *
 */
public class Project extends ElementBase{
	private static SimpleDateFormat parser=new SimpleDateFormat("DD-MM-yyyy HH:mm:ss z");
	static final Logger LOGGER = LogManager.getLogger(Project.class);
	private List<String> elements;
	public Project()
	{
		super(new JsonObject(), SoileConfigLoader.getdbProperty("projectCollection"));
	}
			
		
	@JsonProperty("elements")
	public JsonArray getElements() {
		return new JsonArray(elements);
	}
	@JsonProperty("elements")
	public void setElements(JsonArray elements) {
		List<String> newElements = new LinkedList<String>();
		for(int i = 0; i < elements.size(); i++)
		{
			newElements.add(elements.getString(i));
		}
		this.elements = newElements;
	}
	
	/**
	 * Add an element to the list of elements used by this Project
	 * @param elementID
	 */
	public void addElement(String elementID)
	{
		elements.add(elementID);
	}
	
	@Override
	public JsonObject toJson(boolean provideUUID)
	{
		return super.toJson(provideUUID).put("elements", getElements());
	}	
	
	@Override
	public JsonObject getUpdates()
	{
			JsonObject updateVersions = new JsonObject().put("versions", new JsonObject().put("$each", getVersions()));
			JsonObject updateTags = new JsonObject().put("tags", new JsonObject().put("$each", getTags()));
			JsonObject updateElements = new JsonObject().put("elements", new JsonObject().put("$each", getElements()));
			JsonObject updates = new JsonObject().put("$addToSet", new JsonObject().mergeIn(updateVersions).mergeIn(updateElements).mergeIn(updateTags))
												 .put("$set", new JsonObject().put("private", getPrivate()).put("name", getName()));
			return updates;
	}
	
	
	@Override
	public void loadfromJson(JsonObject project)
	{
		super.loadfromJson(project);
		setElements(project.getJsonArray("elements", new JsonArray()));
	}
			
	public static Future<Project> createProject(MongoClient client)
	{
		Promise<Project> projectPromise = Promise.<Project>promise();

		Project newProject = new Project();
		newProject.save(client)						
		.onSuccess( id -> {
			System.out.println(" Generated ID is : " + id);
			newProject.setUUID(id);
			projectPromise.complete(newProject);
		})
		.onFailure(err -> {
			projectPromise.fail(err);
		});

		return projectPromise.future();
	}
	/**
	 * Load a project as specified by the UUID. This UUID is the mongoDB id. if no project could be loaded, the promise fails.
	 * @param client
	 * @param UUID
	 * @return
	 */
	public static Future<Project> loadProject(MongoClient client, String UUID)
	{
		Promise<Project> projectPromise = Promise.<Project>promise();

		client.findOne(SoileConfigLoader.getdbProperty("projectCollection"), new JsonObject().put("_id", UUID), null)
		.onSuccess(currentProject ->
				{					
					System.out.println(UUID);
					if(currentProject != null)
					{
						System.out.println("Found an existing project");
						
						Project p = new Project();
						p.loadfromJson(currentProject);
						projectPromise.complete(p);
					}
					else
					{
							projectPromise.fail(new ObjectDoesNotExist(UUID));
					}
				})
		.onFailure(err -> {
			projectPromise.fail(err);
		});		
		return projectPromise.future();

	}
		
	
}
