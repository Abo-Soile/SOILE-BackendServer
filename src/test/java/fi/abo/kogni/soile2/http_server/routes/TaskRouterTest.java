package fi.abo.kogni.soile2.http_server.routes;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

import fi.abo.kogni.soile2.datamanagement.DataLakeManagerTest;
import fi.abo.kogni.soile2.http_server.SoileWebTest;
import fi.abo.kogni.soile2.http_server.auth.SoileAuthorization.Roles;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.web.handler.HttpException;

public class TaskRouterTest extends SoileWebTest{

	 @Test
	 public void testGetResource(TestContext context)
	 {		 
		System.out.println("--------------------  Testing Web get Task Resources  ----------------------");

		 Async testAsync = context.async();
		 createUserAndAuthedSession("TestUser", "testpw", Roles.Researcher)
		 .onSuccess(authedSession -> {
			 createUserAndAuthedSession("TestUser2", "testpw", Roles.Researcher)
			 .onSuccess(wrongSession -> {				 
				 WebObjectCreator.createOrRetrieveTask(authedSession, "FirstTask")
				 .onSuccess(taskData -> {
					 Async testResource = context.async();
					 String resourceAddress = "/task/" + taskData.getString("UUID") + "/" + taskData.getString("version") + "/resource/ImageData.jpg";
					 GET(authedSession,resourceAddress,null,null )
					 .onSuccess(result -> {
						 String targetFileName = tmpDir + File.separator + "taskRouter.out"; 
						 vertx.fileSystem().writeFile(targetFileName , result.bodyAsBuffer())
						 .onSuccess(
								 res -> 
								 {
									 try {
										 context.assertTrue( 
												 areFilesEqual(
														 new File(targetFileName),
														 new File(DataLakeManagerTest.class.getClassLoader().getResource("FileTestData/ImageData.jpg").getPath())
														 )
												 );
										 testResource.complete();
										 
									 }
									 catch(IOException e)
									 {
										 context.fail(e);
									 }
								 }
								 // compare that the file is the same as the original one.
								 )
						 .onFailure(err -> context.fail(err));
					 })
					 .onFailure(err -> context.fail(err));
					 Async invalidAccessAsync = context.async();
					 GET(wrongSession,resourceAddress,null,null )
					 .onSuccess(err -> {
						 context.fail("Should not be allowed");
					 })
					 .onFailure(err -> {
						 context.assertEquals(403, ((HttpException)err).getStatusCode());
						 invalidAccessAsync.complete();
					 });
					 
					 Async invalidFileAsync = context.async();					 
					 GET(authedSession,"/task/" + taskData.getString("UUID") + "/" + taskData.getString("version") + "/resource/Something.jpg",null,null )
					 .onSuccess(err -> {
						 context.fail("Should not be possible");
					 })
					 .onFailure(err -> {
						 context.assertEquals(404, ((HttpException)err).getStatusCode());
						 invalidFileAsync.complete();
					 });
					  
					 
					 testAsync.complete();
				 })
				 .onFailure(err -> context.fail(err));
				 
			 })
			 .onFailure(err -> context.fail(err));
			 
		 })
		 .onFailure(err -> context.fail(err));
	 }
}
