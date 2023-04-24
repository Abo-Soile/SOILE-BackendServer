package fi.abo.kogni.soile2.datamanagement.git;

import java.util.LinkedList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;

import fi.abo.kogni.soile2.GitTest;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class GitManagerTest extends GitTest{

	@Test
	public void testGitRepoExists(TestContext context)
	{
		System.out.println("-------------------- Testing Repo exists ----------------------");
		Async initAsync = context.async();
		String targetElement = "TestElement";		
		gitManager.initRepo(targetElement)
		.onSuccess(res -> {
			Async existAsync = context.async();
			gitManager.doesRepoExist(targetElement)
			.onSuccess(exist -> {
				context.assertTrue(exist);
				existAsync.complete();			
			})
			.onFailure(err -> context.fail(err));
			Async notexistAsync = context.async();
			gitManager.doesRepoExist("NotExistent")
			.onSuccess(exist -> {
				context.assertFalse(exist);
				notexistAsync.complete();			
			})
			.onFailure(err -> context.fail(err));
			initAsync.complete();		
		})
		.onFailure(err -> context.fail(err));		
	}

	@Test
	public void testListFiles(TestContext context)
	{
		System.out.println("-------------------- Testing Repo exists ----------------------");
		Async testAsync = context.async();
		String targetElement = "TestElement";		
		gitManager.initRepo(targetElement)
		.onSuccess(initVersion -> {
			GitFile nonResourceFile = new GitFile("NewNonResourceFile",targetElement,initVersion);
			GitFile resource1 = new GitFile("NewFile1",targetElement,initVersion);
			gitManager.writeGitFile(nonResourceFile, "Some Data", null)
			.onSuccess(version1 -> {
				GitFile resource2 = new GitFile("NewFile2",targetElement,version1);			
				gitManager.writeGitResourceFile(resource1, "New Data")
				.onSuccess(version2 -> {
					Async firstTest = context.async();
					gitManager.writeGitResourceFile(resource2, "New Data")
					.onSuccess(version3 -> {
						gitManager.getResourceList(new GitElement(targetElement,version3))
						.onSuccess(list -> {
							// this list should only contain NewFile2 
							List<String> expectedList = new LinkedList<>();
							expectedList.add("NewFile2");							
							context.assertEquals(1, list.size());
							for(int i = 0; i < list.size(); i++)
							{
								expectedList.remove(list.getString(i));
							}
							context.assertEquals(0, expectedList.size());
							firstTest.complete();
						})
						.onFailure(err -> context.fail(err));
					})
					.onFailure(err -> context.fail(err));
					GitFile resource3 = new GitFile("NewFile3",targetElement,version2);
					Async secondTest = context.async();
					gitManager.writeGitResourceFile(resource3, "New Data")
					.onSuccess(versionNew -> {
						gitManager.getResourceList(new GitElement(targetElement,versionNew))
						.onSuccess(list -> {
							// this list should contain NewFile1 and NewFile3, but neither NewFile nor NewFile2
							List<String> expectedList = new LinkedList<>();
							expectedList.add("NewFile1");
							expectedList.add("NewFile3");
							context.assertEquals(2, list.size());
							for(int i = 0; i < list.size(); i++)
							{
								expectedList.remove(list.getString(i));
							}
							context.assertEquals(0, expectedList.size());
							secondTest.complete();
						})
						.onFailure(err -> context.fail(err));
					})
					.onFailure(err -> context.fail(err));	
					testAsync.complete();
				})
				.onFailure(err -> context.fail(err));
				
			})
			.onFailure(err -> context.fail(err));
			
		})
		.onFailure(err -> context.fail(err));


	}
}
