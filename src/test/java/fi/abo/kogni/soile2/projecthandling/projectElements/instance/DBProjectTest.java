package fi.abo.kogni.soile2.projecthandling.projectElements.instance;

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

public class DBProjectTest extends MongoTest{

	String gitDir;
	GitManager gitManager;
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
	}
	
	 @Test
	 public void Test(TestContext context)
	 {		
		 
	 }
	 
	 
	 
	 @Test
	 public void testCreation(TestContext context)
	 {
		 
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
