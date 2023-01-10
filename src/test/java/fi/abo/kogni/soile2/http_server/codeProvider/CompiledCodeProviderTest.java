package fi.abo.kogni.soile2.http_server.codeProvider;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Test;

import fi.abo.kogni.soile2.datamanagement.git.GitManager;
import fi.abo.kogni.soile2.http_server.SoileVerticleTest;
import fi.abo.kogni.soile2.http_server.codeProvider.CompiledCodeProvider;
import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;

public class CompiledCodeProviderTest extends SoileVerticleTest {

	@Test
	public void elangTest(TestContext context)
	{
		GitManager gm = new GitManager(vertx.eventBus()); 
		CompiledCodeProvider elangProvider = new CompiledCodeProvider(SoileConfigLoader.getVerticleProperty("elangAddress"), vertx.eventBus(), gm);
		try
		{			
			String originalCode2 = Files.readString(Paths.get(CompiledCodeProviderTest.class.getClassLoader().getResource("CodeTestData/FirstTask.elang").getPath()));
			Async compilation2Async = context.async();			
			elangProvider.compileCode(originalCode2)
			.onSuccess(Code -> {	
				// this could be made more explicit, testing actual contents.
				context.assertNotNull(Code);
				context.assertTrue(Code.contains("SOILE2"));
				compilation2Async.complete();				
			})
			.onFailure(err -> context.fail(err));
			String failingCode = Files.readString(Paths.get(CompiledCodeProviderTest.class.getClassLoader().getResource("CodeTestData/FirstTask_Error.elang").getPath()));			
			Async compilationAsync = context.async();			
			elangProvider.compileCode(failingCode)
			.onSuccess(Code -> {
				context.fail("Should have failed since code does nto compile");								
			})
			.onFailure(err -> {
				System.out.println(err.getMessage());
				compilationAsync.complete();
			});

		}
		catch(IOException e)
		{
			context.fail(e);
		}

	}
	
	@Test
	public void qmarkupTest(TestContext context)
	{
		GitManager gm = new GitManager(vertx.eventBus()); 
		CompiledCodeProvider qmarkupProvider = new CompiledCodeProvider(SoileConfigLoader.getVerticleProperty("questionnaireAddress"), vertx.eventBus(), gm);
		try
		{			
			String originalCode2 = Files.readString(Paths.get(CompiledCodeProviderTest.class.getClassLoader().getResource("CodeTestData/pilotform.qmarkup").getPath()));
			Async compilation2Async = context.async();			
			qmarkupProvider.compileCode(originalCode2)
			.onSuccess(Code -> {	
				// this could be made more explicit, testing actual contents.
				context.assertNotNull(Code);
				// this s a compiled code, original does not contain this.
				context.assertTrue(Code.contains("<p>"));
				compilation2Async.complete();				
			})
			.onFailure(err -> context.fail(err));
			String failingCode = Files.readString(Paths.get(CompiledCodeProviderTest.class.getClassLoader().getResource("CodeTestData/pilotform2_error.qmarkup").getPath()));			
			Async compilationAsync = context.async();			
			qmarkupProvider.compileCode(failingCode)
			.onSuccess(Code -> {
				context.fail("Should have failed since code does nto compile");								
			})
			.onFailure(err -> {
				context.assertTrue(err.getMessage().contains("numberfiel"));
				compilationAsync.complete();
			});

		}
		catch(IOException e)
		{
			context.fail(e);
		}

	}
	
}
