package fi.abo.kogni.soile2.projecthandling.projectElements;

import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Test;

import fi.abo.kogni.soile2.MongoTest;
import fi.abo.kogni.soile2.projecthandling.exceptions.ElementNameExistException;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;

public class ExperimentTest extends MongoTest {

	@Test
	public void testExperimentSaveLoad(TestContext context)
	{
		System.out.println("----------------------------- Testing Experiment Save and Load -----------------------------");
		ElementFactory<Experiment> ExperimentFactory = new ElementFactory<Experiment>(Experiment::new);
		Async testAsync = context.async();
		buildTestExperiment().onSuccess(p -> {
			p.addVersion("abcdefg");
			p.addTag("NewVersion", "abcdefg");
			p.save(mongo_client)
			.onSuccess(ID -> {
				p.setUUID(ID);
				ExperimentFactory.loadElement(mongo_client, p.getUUID())
				.onSuccess(Experiment -> {
					context.assertEquals(p.getPrivate(),Experiment.getPrivate());
					context.assertEquals(p.getName(),Experiment.getName());
					context.assertEquals(p.getVersionForTag("NewVersion"), Experiment.getVersionForTag("NewVersion"));
					testAsync.complete();
				})
				.onFailure(err -> {
					context.fail(err);
				});
			})
			.onFailure(err -> {
				context.fail(err);
			});
		})
		.onFailure(err -> {
			context.fail(err);
		});
	}

	
	@Test
	public void testExperimentUpdate(TestContext context)
	{
		System.out.println("----------------------------- Testing Experiment Update -----------------------------");
		ElementFactory<Experiment> ExperimentFactory = new ElementFactory<Experiment>(Experiment::new);
		Async testAsync = context.async();	
		buildTestExperiment().onSuccess(exp -> {
			exp.addVersion("abcdefg");
			exp.addTag("NewVersion", "abcdefg");
			exp.setPrivate(true);
			exp.addElement("32145");
			exp.save(mongo_client)
			.onSuccess(ID -> {
				exp.setUUID(ID);
				exp.addVersion("12345");
				exp.addTag("Another Tag", "12345");				
				exp.setPrivate(false);
				exp.addElement("32145");
				exp.addElement("14555");
				exp.save(mongo_client)				
				.onSuccess(Void2 -> {						
					ExperimentFactory.loadElement(mongo_client, exp.getUUID())
					.onSuccess(Experiment -> {					
						context.assertEquals(exp.getPrivate(),Experiment.getPrivate());
						context.assertEquals(2, Experiment.getTags().size());
						context.assertEquals(2, Experiment.getVersions().size());
						context.assertEquals(2, Experiment.getElements().size());
						boolean found = false;
						for(int i = 0; i < 2; i++)
						{							
							if(Experiment.getElements().getString(i).equals("32145"))
							{
								found = true;
							}
						}						
						context.assertTrue(found);
						context.assertEquals(exp.getVersionForTag("Another Tag"), Experiment.getVersionForTag("Another Tag"));
						context.assertEquals(exp.getVersionDate("12345"),Experiment.getVersionDate("12345"));
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
	
	@Test
	public void testExperimentNameException(TestContext context)
	{
		System.out.println("----------------------------- Testing Experiment Name Exception -----------------------------");
		Async testAsync = context.async();	
		buildTestExperiment().onSuccess(exp -> {
			buildTestExperiment()
			.onFailure(err -> {
				context.assertEquals(err.getClass(), ElementNameExistException.class);
				testAsync.complete();
			})
			.onSuccess(exp2 -> {
				context.fail("The creation should have failed due to colliding names");
				testAsync.complete();
			});
			
		})
		.onFailure(err -> {
			context.fail(err);
			testAsync.complete();
		});

	}

	public Future<Experiment> buildTestExperiment()
	{			
		ElementFactory<Experiment> ExperimentFactory = new ElementFactory<Experiment>(Experiment::new);
		Promise<Experiment> ExperimentPromise = Promise.<Experiment>promise();
		try
		{
			JsonObject ExperimentDef = new JsonObject(Files.readString(Paths.get(ExperimentTest.class.getClassLoader().getResource("DBTestData/DBExperiment.json").getPath())));
			Experiment tempExperiment = new Experiment();
			tempExperiment.loadfromJson(ExperimentDef);
			ExperimentFactory.createElement(mongo_client,tempExperiment.getName())
			.onSuccess(Experiment -> 
			{
				Experiment.setPrivate(tempExperiment.getPrivate());
				ExperimentPromise.complete(Experiment);
			})
			.onFailure(err -> {
				ExperimentPromise.fail(err);
			});
		}
		catch(Exception e)
		{
			ExperimentPromise.fail(e);			
		}
		return ExperimentPromise.future();

	}
	
}

