package fi.abo.kogni.soile2.projecthandling.projectElements;

import java.util.List;

import org.junit.Test;

import fi.abo.kogni.soile2.MongoTest;
import fi.abo.kogni.soile2.datamanagement.git.GitManager;
import fi.abo.kogni.soile2.projecthandling.apielements.APIProject;
import fi.abo.kogni.soile2.projecthandling.exceptions.ElementNameExistException;
import fi.abo.kogni.soile2.utils.VerticleInitialiser;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;

public class ElementManagerTest extends MongoTest{

	String gitDir;
	GitManager gitManager;
	ElementManager<Project> manager;
	@Override
	public void runBeforeTests(TestContext context)
	{		
		super.runBeforeTests(context);
		Async gitInit = context.async();
		VerticleInitialiser.startGitVerticle(vertx).onSuccess( dir -> {
			gitDir = dir;
			gitInit.complete();
		})
		.onFailure(err -> {
			context.fail(err);
			gitInit.complete();
		});
		gitManager = new GitManager(vertx.eventBus());
		manager = new ElementManager<Project>(Project::new, APIProject::new, mongo_client,gitManager);
	}
	
	 @Test
	 public void testGetList(TestContext context)
	 {		
		Async projectAsync = context.async();		
		manager.createElement("NewProject")
		.onSuccess(project -> {
			Async twoProjects = context.async();
			manager.createElement("NewProject2")
			.onSuccess(p2 -> {
				System.out.println("Second Project created");
				manager.getElementList()
				.onSuccess(list -> {
					System.out.println("List retrieved");
					context.assertEquals(2, list.size());
					Project[] projects = new Project[] {project, p2};
					for(Project p : projects)
					{
						for(int i = 0; i < list.size(); i++)
						{
							JsonObject current = list.getJsonObject(i); 
							if(current.getString("UUID").equals(p.getUUID()))
							{
								context.assertEquals(p.getName(), current.getString("name"));
								list.remove(i);
								break;
							}							
						}						
					}
					System.out.println("Items removed");
					// everything was removed
					context.assertEquals(0, list.size());
					twoProjects.complete();
				})
				.onFailure(err -> {					
					context.fail(err);
					twoProjects.complete();
				});	
			})
			.onFailure(err -> {
				context.fail(err);
				twoProjects.complete();
			});
			projectAsync.complete();
		})
		.onFailure(err -> {
			context.fail(err);
			projectAsync.complete();

		});
	 }
	 
	 
	 
	 @Test
	 public void testCreation(TestContext context)
	 {
		Async projectAsync = context.async();
		manager.createElement("NewProject").onSuccess(project -> {
			Async sameNameAsync = context.async();
			manager.createElement("NewProject").onSuccess(Void -> {
				context.fail("This should not be possible. name already exists");
				sameNameAsync.complete();
			})
			.onFailure(err -> {
				context.assertEquals(err.getClass(), ElementNameExistException.class);
				sameNameAsync.complete();				
			});
			context.assertEquals(project.getName(), "NewProject");
			projectAsync.complete();
		}).
		onFailure(err ->{
			context.fail(err);
		});		
	 }
/*
	 @Test
	 public void testUpdate(TestContext context)
	 {
		Async projectAsync = context.async();
		manager.createElement("NewProject").onSuccess(project -> {
			Async updateAsync = context.async();
			APIProject project = new APIProject(null)
			manager.updateElement(null)
			projectAsync.complete();
		}).
		onFailure(err ->{
			context.fail(err);
		});		
	 }*/
}
