package fi.abo.kogni.soile2.projecthandling.projectElements;

import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Test;

import fi.abo.kogni.soile2.MongoTest;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;

public class TaskTest extends MongoTest {

	@Test
	public void testTaskSaveLoad(TestContext context)
	{
		ElementFactory<Task> TaskFactory = new ElementFactory<Task>(Task::new);
		Async testAsync = context.async();
		buildTestTask(context).onSuccess(p -> {
			System.out.println("Initial Task set up");
			p.addVersion("abcdefg");
			p.addTag("NewVersion", "abcdefg");
			p.save(mongo_client)
			.onSuccess(ID -> {
				p.setUUID(ID);
				System.out.println(p.getUUID());
				TaskFactory.loadElement(mongo_client, p.getUUID())
				.onSuccess(Task -> {
					context.assertEquals(p.getPrivate(),Task.getPrivate());
					context.assertEquals(p.getName(),Task.getName());
					context.assertEquals(p.getVersionForTag("NewVersion"), Task.getVersionForTag("NewVersion"));
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
	public void testTaskUpdate(TestContext context)
	{
		ElementFactory<Task> TaskFactory = new ElementFactory<Task>(Task::new);
		Async testAsync = context.async();	
		buildTestTask(context).onSuccess(p -> {
			p.addVersion("abcdefg");
			p.addTag("NewVersion", "abcdefg");
			p.setPrivate(true);
			p.save(mongo_client)
			.onSuccess(ID -> {
				p.setUUID(ID);
				p.addVersion("12345");
				p.addTag("Another Tag", p.getCurrentVersion());				
				p.setPrivate(false);
				p.save(mongo_client)
				.onSuccess(Void2 -> {						
					TaskFactory.loadElement(mongo_client, p.getUUID())
					.onSuccess(Task -> {
						System.out.println("This is the final task:");
						System.out.println(Task.toJson().encodePrettily());
						context.assertEquals(p.getPrivate(),Task.getPrivate());
						context.assertEquals(2, Task.getTags().size());
						context.assertEquals(2, Task.getVersions().size());
						context.assertEquals(p.getVersionForTag("Another Tag"), Task.getVersionForTag("Another Tag"));
						context.assertEquals(p.getVersionDate("12345"),Task.getVersionDate("12345"));
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

	public Future<Task> buildTestTask(TestContext context)
	{			
		ElementFactory<Task> taskFactory = new ElementFactory<Task>(Task::new);
		Promise<Task> taskPromise = Promise.<Task>promise();
		try
		{
			JsonObject taskDef = new JsonObject(Files.readString(Paths.get(TaskTest.class.getClassLoader().getResource("DBTestData/DBTask.json").getPath())));
			Task tempTask = new Task();
			tempTask.loadfromJson(taskDef);
			System.out.println(tempTask.toJson().encodePrettily());
			taskFactory.createElement(mongo_client, "TestTask")
			.onSuccess(task -> 
			{
				tempTask.setUUID(task.getUUID());
				System.out.println("The generated Task has the id: " + task.getUUID());
				taskPromise.complete(tempTask);
								
			})
			.onFailure(err -> {
				taskPromise.fail(err);
				context.fail(err);
			});
		}
		catch(Exception e)
		{
			e.printStackTrace(System.out);
			context.fail(e);
			taskPromise.fail(e);			
		}
		return taskPromise.future();

	}

}

