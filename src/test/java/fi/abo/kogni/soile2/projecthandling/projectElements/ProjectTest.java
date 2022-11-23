package fi.abo.kogni.soile2.projecthandling.projectElements;

import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Test;

import fi.abo.kogni.soile2.MongoTest;
import fi.abo.kogni.soile2.projecthandling.projectElements.ElementFactory;
import fi.abo.kogni.soile2.projecthandling.projectElements.Project;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;

public class ProjectTest extends MongoTest {

	@Test
	public void testProjectSaveLoad(TestContext context)
	{
		ElementFactory<Project> projectFactory = new ElementFactory<Project>(Project::new);
		Async testAsync = context.async();
		buildTestProject(context).onSuccess(p -> {
			System.out.println("Initial Project set up");
			p.addVersion("abcdefg");
			p.addTag("NewVersion", "abcdefg");
			p.save(mongo_client)
			.onSuccess(ID -> {
				p.setUUID(ID);
				System.out.println(p.getUUID());
				projectFactory.loadElement(mongo_client, p.getUUID())
				.onSuccess(project -> {
					context.assertEquals(p.getPrivate(),project.getPrivate());
					context.assertEquals(p.getName(),project.getName());
					context.assertEquals(p.getVersionForTag("NewVersion"), project.getVersionForTag("NewVersion"));
					testAsync.complete();
				})
				.onFailure(err -> {
					context.fail(err);
					testAsync.complete();
				});
			})
			.onFailure(err -> {
				context.fail(err);
				testAsync.complete();
			});
		})
		.onFailure(err -> {
			context.fail(err);
			testAsync.complete();
		});
	}

	@Test
	public void testProjectUpdate(TestContext context)
	{
		ElementFactory<Project> projectFactory = new ElementFactory<Project>(Project::new);
		Async testAsync = context.async();	
		buildTestProject(context).onSuccess(p -> {
			p.addVersion("abcdefg");
			p.addTag("NewVersion", "abcdefg");
			p.setPrivate(true);
			p.save(mongo_client)
			.onSuccess(ID -> {
				p.setUUID(ID);
				p.addVersion("12345");
				p.addTag("Another Tag", "12345");
				p.setPrivate(false);
				p.save(mongo_client)
				.onSuccess(Void2 -> {						
					projectFactory.loadElement(mongo_client, p.getUUID())
					.onSuccess(project -> {		
						System.out.println("This is the final task:");
						System.out.println(project.toJson().encodePrettily());			
						context.assertEquals(p.getPrivate(),project.getPrivate());
						context.assertEquals(2, project.getTags().size());
						context.assertEquals(2, project.getVersions().size());
						context.assertEquals(p.getVersionForTag("Another Tag"), project.getVersionForTag("Another Tag"));
						context.assertEquals(p.getVersionDate("12345"),project.getVersionDate("12345"));
						testAsync.complete();
					}).onFailure(err -> {
						context.fail(err);
						testAsync.complete();
					});
				})
				.onFailure(err -> {
					context.fail(err);
					testAsync.complete();
				});
			})
			.onFailure(err -> {
				context.fail(err);
				testAsync.complete();
			});
		})
		.onFailure(err -> {
			context.fail(err);
			testAsync.complete();
		});

	}

	public Future<Project> buildTestProject(TestContext context)
	{			
		ElementFactory<Project> projectFactory = new ElementFactory<Project>(Project::new);
		Promise<Project> projectPromise = Promise.<Project>promise();
		try
		{
			JsonObject projectDef = new JsonObject(Files.readString(Paths.get(ProjectTest.class.getClassLoader().getResource("DBProject.json").getPath())));
			Project tempProject = new Project();
			tempProject.loadfromJson(projectDef);
			projectFactory.createElement(mongo_client)
			.onSuccess(project -> 
			{
				System.out.println("The generated project has the id: " + project.getUUID());
				project.setName(tempProject.getName());
				project.setPrivate(tempProject.getPrivate());
				projectPromise.complete(project);
			})
			.onFailure(err -> {
				projectPromise.fail(err);
				context.fail(err);
			});
		}
		catch(Exception e)
		{
			e.printStackTrace(System.out);
			context.fail(e);
			projectPromise.fail(e);			
		}
		return projectPromise.future();

	}

}

