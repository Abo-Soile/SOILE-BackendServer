package fi.abo.kogni.soile2.http_server.routes;

import java.util.LinkedList;
import java.util.List;

import org.junit.Test;

import fi.abo.kogni.soile2.http_server.SoileWebTest;
import fi.abo.kogni.soile2.http_server.auth.SoileAuthorization.Roles;
import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import fi.abo.kogni.soile2.utils.WebObjectCreator;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientSession;
import io.vertx.ext.web.handler.HttpException;

//TODO: Test Project deletion and Project Stop.
public class StudyRouterTest extends SoileWebTest {

	/**
	 * This test tests both starting and getting the list of running projects.
	 * @param context
	 */
	@Test
	public void testStartProject(TestContext context)
	{
		System.out.println("--------------------  Running Start Project test  ----------------------");    

		JsonObject projectExec = new JsonObject().put("private", true).put("name", "New Project").put("shortcut","newShortcut"); 
		Async setupAsync = context.async();
		createUserAndAuthedSession("Researcher", "pw", Roles.Researcher)
		.onSuccess(authedSession -> {
			createUserAndAuthedSession("Researcher2", "pw", Roles.Researcher)
			.onSuccess(wrongSession -> {
				WebObjectCreator.createProject(authedSession, "Testproject")
				.onSuccess(projectData -> {
					String projectID = projectData.getString("UUID");
					String projectVersion = projectData.getString("version");
					Async startAsync = context.async();

					POST(authedSession, "/project/" + projectID + "/" + projectVersion + "/start", null,projectExec )
					.onSuccess(response -> {
						Async listAsync = context.async();
						String id = response.bodyAsJsonObject().getString("projectID");
						POST(authedSession, "/projectexec/list", null,null)
						.onSuccess(listresponse -> {
							context.assertEquals(1, listresponse.bodyAsJsonArray().size());
							context.assertEquals(id, listresponse.bodyAsJsonArray().getJsonObject(0).getString("uuid"));
							listAsync.complete();
						})
						.onFailure(err -> context.fail(err));
						Async emptyListAsync = context.async();

						POST(wrongSession, "/projectexec/list", null,null)
						.onSuccess(listresponse -> {
							context.assertEquals(0, listresponse.bodyAsJsonArray().size());
							emptyListAsync.complete();
						})
						.onFailure(err -> context.fail(err));
						startAsync.complete();
					})
					.onFailure(err -> context.fail(err));	

					setupAsync.complete();
				})
				.onFailure(err -> context.fail(err));

			})
			.onFailure(err -> context.fail(err));
		})
		.onFailure(err -> context.fail(err));
	}




	/**
	 * This test tests both starting and getting the list of running projects.
	 * @param context
	 */
	@Test
	public void testlistProjects(TestContext context)
	{
		System.out.println("--------------------  Running Start Project test  ----------------------");    

		JsonObject projectExec1 = new JsonObject().put("private", true).put("name", "New Project").put("shortcut","newShortcut");
		JsonObject projectExec2 = new JsonObject().put("private", false).put("name", "New Project2").put("shortcut","newShortcut2");
		Async setupAsync = context.async();
		createUserAndAuthedSession("Researcher", "pw", Roles.Researcher)
		.onSuccess(authedSession -> {
			createUserAndAuthedSession("Researcher2", "pw", Roles.Researcher)
			.onSuccess(wrongSession -> {
				WebClient unAuthedSession = createSession();

				WebObjectCreator.createProject(authedSession, "Testproject")
				.onSuccess(projectData -> {
					String projectID = projectData.getString("UUID");
					String projectVersion = projectData.getString("version");
					Async startAsync = context.async();

					POST(authedSession, "/project/" + projectID + "/" + projectVersion + "/start", null,projectExec1 )
					.onSuccess(response1 -> {
						String id1 = response1.bodyAsJsonObject().getString("projectID");

						POST(authedSession, "/project/" + projectID + "/" + projectVersion + "/start", null,projectExec2 )
						.onSuccess(response2 -> {
							String id2 = response2.bodyAsJsonObject().getString("projectID");
							Async listAsync = context.async();
							POST(authedSession, "/projectexec/list", null,null)
							.onSuccess(listresponse -> {
								context.assertEquals(2, listresponse.bodyAsJsonArray().size());
								List<String> uuids = new LinkedList<>();
								for(int i = 0 ; i < listresponse.bodyAsJsonArray().size(); i++)
								{
									uuids.add(listresponse.bodyAsJsonArray().getJsonObject(i).getString("uuid"));								
								}
								context.assertTrue(uuids.contains(id1));
								context.assertTrue(uuids.contains(id2));
								listAsync.complete();
							})
							.onFailure(err -> context.fail(err));
							Async readlistasync = context.async();
							POST(authedSession, "/projectexec/list", new JsonObject().put("access",  "read"),null)
							.onSuccess(listresponse -> {
								context.assertEquals(2, listresponse.bodyAsJsonArray().size());
								List<String> uuids = new LinkedList<>();
								for(int i = 0 ; i < listresponse.bodyAsJsonArray().size(); i++)
								{
									uuids.add(listresponse.bodyAsJsonArray().getJsonObject(i).getString("uuid"));								
								}
								context.assertTrue(uuids.contains(id1));
								context.assertTrue(uuids.contains(id2));
								readlistasync.complete();
							})
							.onFailure(err -> context.fail(err));
							Async emptyListAsync = context.async();

							POST(wrongSession, "/projectexec/list", null,null)
							.onSuccess(listresponse -> {
								context.assertEquals(1, listresponse.bodyAsJsonArray().size());
								context.assertEquals(id2, listresponse.bodyAsJsonArray().getJsonObject(0).getString("uuid"));
								emptyListAsync.complete();
							})
							.onFailure(err -> context.fail(err));

							Async accessAsync = context.async();
							POST(wrongSession, "/projectexec/list", new JsonObject().put("access",  "read"),null)
							.onSuccess(listresponse -> {
								context.assertEquals(0, listresponse.bodyAsJsonArray().size());							
								accessAsync.complete();
							})
							.onFailure(err -> context.fail(err));
							Async unAuthAsync = context.async();

							POST(unAuthedSession, "/projectexec/list", null,null)
							.onSuccess(listresponse -> {
								context.assertEquals(1, listresponse.bodyAsJsonArray().size());
								context.assertEquals(id2, listresponse.bodyAsJsonArray().getJsonObject(0).getString("uuid"));

								unAuthAsync.complete();
							})
							.onFailure(err -> context.fail(err));

							startAsync.complete();
						})
						.onFailure(err -> context.fail(err));	
					})
					.onFailure(err -> context.fail(err));	
					setupAsync.complete();
				})
				.onFailure(err -> context.fail(err));

			})
			.onFailure(err -> context.fail(err));
		})
		.onFailure(err -> context.fail(err));
	}


	/**
	 * This test tests both starting and getting the list of running projects.
	 * @param context
	 */
	@Test
	public void testStopRestart(TestContext context)
	{
		System.out.println("--------------------  Testing Start/Stop project  ----------------------");    

		JsonObject projectExec = new JsonObject().put("private", true).put("name", "New Project").put("shortcut","newShortcut"); 
		Async setupAsync = context.async();
		createUserAndAuthedSession("Researcher", "pw", Roles.Researcher)
		.onSuccess(authedSession -> {
			createUserAndAuthedSession("Researcher2", "pw", Roles.Researcher)
			.onSuccess(wrongSession -> {
				WebObjectCreator.createProject(authedSession, "Testproject")
				.onSuccess(projectData -> {
					String projectID = projectData.getString("UUID");
					String projectVersion = projectData.getString("version");
					Async startAsync = context.async();

					POST(authedSession, "/project/" + projectID + "/" + projectVersion + "/start", null,projectExec )
					.onSuccess(response -> {
						Async listAsync = context.async();
						Async emptyListAsync = context.async();
						String id = response.bodyAsJsonObject().getString("projectID");
						POST(authedSession, "/projectexec/list", null,null)
						.onSuccess(listresponse -> {
							context.assertEquals(1, listresponse.bodyAsJsonArray().size());
							context.assertEquals(id, listresponse.bodyAsJsonArray().getJsonObject(0).getString("uuid"));
							POST(authedSession, "/projectexec/" + id + "/stop", null,null )
							.onSuccess( stopped -> {
								POST(authedSession, "/projectexec/" + id + "/signup", null,null)
								.onSuccess(res -> context.fail("This should fail because the project is inactive"))
								.onFailure(rejected -> {
									context.assertEquals(410, ((HttpException)rejected).getStatusCode());
									POST(authedSession, "/projectexec/" + id + "/restart", null,null )
									.onSuccess( restarted -> {
										POST(authedSession, "/projectexec/" + id + "/signup", null,null)
										.onSuccess(res -> {											
											listAsync.complete();																		
										})
										.onFailure(err -> context.fail(err));
									})
									.onFailure(err -> context.fail(err));


								});
							})
							.onFailure(err -> context.fail(err));

						})
						.onFailure(err -> context.fail(err));

						POST(wrongSession, "/projectexec/list", null,null)
						.onSuccess(listresponse -> {
							context.assertEquals(0, listresponse.bodyAsJsonArray().size());
							emptyListAsync.complete();
						})
						.onFailure(err -> context.fail(err));
						startAsync.complete();
					})
					.onFailure(err -> context.fail(err));	

					setupAsync.complete();
				})
				.onFailure(err -> context.fail(err));

			})
			.onFailure(err -> context.fail(err));
		})
		.onFailure(err -> context.fail(err));
	}


	/**
	 * This test tests both starting and getting the list of running projects.
	 * @param context
	 */
	@Test
	public void testParticipantBlocksMod(TestContext context)
	{
		System.out.println("--------------------  Testing POST/GET study properties  ----------------------");    

		JsonObject projectExec = new JsonObject().put("private", true).put("name", "New Project").put("shortcut","newShortcut"); 
		Async setupAsync = context.async();
		createUserAndAuthedSession("Researcher", "pw", Roles.Researcher)
		.onSuccess(authedSession -> {
			createUserAndAuthedSession("Researcher2", "pw", Roles.Researcher)
			.onSuccess(wrongSession -> {
				WebObjectCreator.createProject(authedSession, "Testproject")
				.onSuccess(projectData -> {
					String projectID = projectData.getString("UUID");					
						String projectVersion = projectData.getString("version");
						WebObjectCreator.createProject(authedSession, "ExampleProject")
						.onSuccess(projectData2 -> {
							String projectID2 = projectData2.getString("UUID");						
							String projectVersion2 = projectData2.getString("version");
							Async startAsync = context.async();
							POST(authedSession, "/project/" + projectID + "/" + projectVersion + "/start", null,projectExec )					
							.onSuccess(response -> {
								// we have a project set up. lets try to get the information.
								String studyId = response.bodyAsJsonObject().getString("projectID");					
								Async getsetasync = context.async();
								POST(authedSession, "/projectexec/" + studyId + "/restart", null, null)
								.onSuccess(active2 -> {
									POST(authedSession, "/projectexec/" + studyId, null, null)
									.onSuccess(studyDataResponse -> {
										POST(authedSession, "/projectexec/" + studyId + "/signup", null,null)
										.onSuccess(res -> {
											JsonObject studyData = studyDataResponse.bodyAsJsonObject();
											JsonObject studyData2 = studyDataResponse.bodyAsJsonObject();
											context.assertEquals("newShortcut", studyData.getString("shortcut"));
											context.assertEquals(projectID, studyData.getString("sourceUUID"));
											context.assertEquals(projectVersion, studyData.getString("version"));
											context.assertEquals(true, studyData.getBoolean("private"));
											studyData.put("private", false);
											studyData.put("sourceUUID", projectID2);
											studyData.put("version", projectVersion2);
											Async failAsync = context.async();
											POST(authedSession, "/projectexec/" + studyId +"/update", null, studyData)
											.onSuccess(updateResponse -> {
												context.fail("This should not be possible");
											})
											.onFailure(err -> failAsync.complete());	
											studyData2.put("private",false);
											studyData2.put("shortDescription","Fancy");		
											POST(authedSession, "/projectexec/" + studyId +"/update", null, studyData2)
											.onSuccess(updateResponse -> {
												POST(authedSession, "/projectexec/" + studyId, null, null)
												.onSuccess(updatedstudyDataResponse -> {
													JsonObject studyDatanew = updatedstudyDataResponse.bodyAsJsonObject();
													context.assertEquals("Fancy", studyDatanew.getString("shortDescription"));
													context.assertEquals(projectID, studyDatanew.getString("sourceUUID"));
													context.assertEquals(projectVersion, studyDatanew.getString("version"));
													context.assertEquals(false, studyDatanew.getBoolean("private"));
													getsetasync.complete();
												})
												.onFailure(err -> context.fail(err));

											})									
											.onFailure(err -> context.fail(err));									
										})
										.onFailure(err -> context.fail(err));

									})
									.onFailure(err -> context.fail(err));
									Async failedAsync = context.async();
									POST(wrongSession, "/projectexec/" + studyId, null,null)
									.onSuccess(listresponse -> {
										context.fail("Does not have accesss");
									})
									.onFailure(err -> {
										context.assertEquals(403, ((HttpException)err).getStatusCode());
										failedAsync.complete();
									});
									startAsync.complete();
								})
								.onFailure(err -> context.fail(err));	

								setupAsync.complete();
							})
							.onFailure(err -> context.fail(err));
						})
						.onFailure(err -> context.fail(err));
					})
					.onFailure(err -> context.fail(err));

			})
			.onFailure(err -> context.fail(err));
		})
		.onFailure(err -> context.fail(err));
	}


	@Test
	public void testModify(TestContext context)
	{
		System.out.println("--------------------  Testing POST/GET study properties  ----------------------");    

		JsonObject projectExec = new JsonObject().put("private", true).put("name", "New Project").put("shortcut","newShortcut"); 
		Async setupAsync = context.async();
		createUserAndAuthedSession("Researcher", "pw", Roles.Researcher)
		.onSuccess(authedSession -> {
			createUserAndAuthedSession("Researcher2", "pw", Roles.Researcher)
			.onSuccess(wrongSession -> {
				WebObjectCreator.createProject(authedSession, "Testproject")
				.onSuccess(projectData -> {
					String projectID = projectData.getString("UUID");
					String projectVersion = projectData.getString("version");
					WebObjectCreator.createProject(authedSession, "ExampleProject")
					.onSuccess(projectData2 -> {
						String projectID2 = projectData2.getString("UUID");
						String projectVersion2 = projectData2.getString("version");
						Async startAsync = context.async();
						POST(authedSession, "/project/" + projectID + "/" + projectVersion + "/start", null,projectExec )					
						.onSuccess(response -> {
							// we have a project set up. lets try to get the information.
							String studyId = response.bodyAsJsonObject().getString("projectID");					
							Async getsetasync = context.async();
							POST(authedSession, "/projectexec/" + studyId, null, null)
							.onSuccess(studyDataResponse -> {

								JsonObject studyData = studyDataResponse.bodyAsJsonObject();
								context.assertEquals("newShortcut", studyData.getString("shortcut"));
								context.assertEquals(projectID, studyData.getString("sourceUUID"));
								context.assertEquals(projectVersion, studyData.getString("version"));
								context.assertEquals(true, studyData.getBoolean("private"));
								studyData.put("private", false);
								studyData.put("sourceUUID", projectID2);
								studyData.put("version", projectVersion2);
								POST(authedSession, "/projectexec/" + studyId +"/update", null, studyData)
								.onSuccess(updateResponse -> {
									POST(authedSession, "/projectexec/" + studyId, null, null)
									.onSuccess(updatedstudyDataResponse -> {
										JsonObject studyData2 = updatedstudyDataResponse.bodyAsJsonObject();
										System.out.println(studyData2.encodePrettily());
										context.assertEquals("newShortcut", studyData2.getString("shortcut"));
										context.assertEquals(projectID2, studyData2.getString("sourceUUID"));
										context.assertEquals(projectVersion2, studyData2.getString("version"));
										context.assertEquals(false, studyData2.getBoolean("private"));
										getsetasync.complete();				
									})
									.onFailure(err -> context.fail(err));

								})
								.onFailure(err -> context.fail(err));

							})
							.onFailure(err -> context.fail(err));
							Async failedAsync = context.async();
							POST(wrongSession, "/projectexec/" + studyId, null,null)
							.onSuccess(listresponse -> {
								context.fail("Does not have accesss");
							})
							.onFailure(err -> {
								context.assertEquals(403, ((HttpException)err).getStatusCode());
								failedAsync.complete();
							});
							startAsync.complete();
						})
						.onFailure(err -> context.fail(err));	

						setupAsync.complete();
					})
					.onFailure(err -> context.fail(err));
				})
				.onFailure(err -> context.fail(err));

			})
			.onFailure(err -> context.fail(err));
		})
		.onFailure(err -> context.fail(err));
	}
	@Test
	public void testTokens(TestContext context)
	{
		System.out.println("--------------------  Testing Creation and retrieval of tokens ----------------------");
		Async setupAsync = context.async();
		createAndStartTestProject(false)
		.onSuccess(projectID -> {
			createTokens(generatorSession, projectID, 10, false)
			.onSuccess(createdTokens -> {
				createTokens(generatorSession, projectID, 10, true)
				.onSuccess(masterToken -> {
					WebClientSession unauthed = createSession();
					signUpToProjectWithToken(unauthed, createdTokens.getString(0), projectID)
					.onSuccess(accessToken -> {
						Async requestTokensAsync = context.async();
						Async mongoAsync = context.async();
						mongo_client.find(SoileConfigLoader.getdbProperty("projectInstanceCollection"), new JsonObject())
						.onSuccess(results -> {
							for(JsonObject o : results)
							{
								System.out.println(o.encodePrettily());
							}
							mongoAsync.complete();
						})
						.onFailure(err -> context.fail(err));
						POST(generatorSession, "/projectexec/" + projectID + "/tokeninfo", null, null)
						.onSuccess(Response -> {
							JsonObject tokeninfo = Response.bodyAsJsonObject();
							System.out.println(tokeninfo.encodePrettily());

							context.assertEquals(1, tokeninfo.getJsonArray("usedTokens").size());
							context.assertEquals(createdTokens.getString(0), tokeninfo.getJsonArray("usedTokens").getString(0));
							context.assertFalse(tokeninfo.getJsonArray("signupTokens").contains(createdTokens.getString(0)));
							context.assertEquals(createdTokens.getString(0), tokeninfo.getJsonArray("usedTokens").getString(0));
							context.assertEquals(9,tokeninfo.getJsonArray("signupTokens").size());
							for(int i = 1; i < createdTokens.size(); ++i)
							{
								context.assertTrue(tokeninfo.getJsonArray("signupTokens").contains(createdTokens.getString(i)));
							}
							// The masterToken is also a JsonArray.
							context.assertEquals(masterToken.getString(0),tokeninfo.getString("permanentAccessToken"));
							requestTokensAsync.complete();
						})
						.onFailure(err -> context.fail(err));	

						setupAsync.complete();	
					})
					.onFailure(err -> context.fail(err));										
				})
				.onFailure(err -> context.fail(err));
			})
			.onFailure(err -> context.fail(err));
		})
		.onFailure(err -> context.fail(err));

	}
}
