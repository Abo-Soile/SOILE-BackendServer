package fi.abo.kogni.soile2.http_server.routes;

import org.junit.Test;

import fi.abo.kogni.soile2.datamanagement.git.GitFile;
import fi.abo.kogni.soile2.http_server.SoileWebTest;
import fi.abo.kogni.soile2.http_server.auth.SoileAuthorization.Roles;
import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import fi.abo.kogni.soile2.utils.WebObjectCreator;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.web.client.WebClientSession;
import io.vertx.ext.web.handler.HttpException;

public class ElementRouterTest extends SoileWebTest {


	@Test
	public void testBuildWebTask(TestContext context)
	{	
		System.out.println("--------------------  Testing Task generation ----------------------");

		Async setupAsync = context.async();
		WebClientSession currentSession = createSession();
		createUser(vertx, "TestUser", "testPassword", Roles.Admin)
		.onSuccess(userCreated -> {
			authenticateSession(currentSession, "TestUser", "testPassword")
			.onSuccess(authed -> {
				WebObjectCreator.createTask(currentSession, "Test2")
				.onSuccess(taskData -> {
					Async taskInfoAsync = context.async();
					String taskID = taskData.getString("UUID");
					String taskVersion = taskData.getString("version");
					getElement(currentSession, "task", taskID, taskVersion)					
					.compose(newData -> {
						context.assertEquals(taskID, newData.getString("UUID"));
						context.assertEquals(taskVersion, newData.getString("version"));
						context.assertEquals(taskData.getString("code"), newData.getString("code"));
						context.assertTrue(taskData.getString("code").contains("intermezzo"));
						context.assertEquals("UNKNOWN", newData.getValue("author"));
						context.assertEquals(0, newData.getJsonArray("keywords").size());
						return mongo_client.findOne(SoileConfigLoader.getCollectionName("taskCollection"), new JsonObject().put("_id", taskID), null);												
					})
					.onSuccess(db -> {						
						System.out.println(db.encodePrettily());
						taskInfoAsync.complete();
					})
					.onFailure(err -> context.fail(err));

					Async taskListAsync = context.async();

					getElementList(currentSession, "task")
					.onSuccess(taskList-> {
						context.assertEquals(1, taskList.size());
						context.assertEquals(taskID, taskList.getJsonObject(0).getValue("UUID"));
						context.assertEquals(taskData.getString("name"), taskList.getJsonObject(0).getValue("name"));
						taskListAsync.complete();
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


	@Test
	public void testTagManagement(TestContext context)
	{	
		System.out.println("--------------------  Testing Task generation ----------------------");

		Async setupAsync = context.async();
		WebClientSession currentSession = createSession();
		createUser(vertx, "TestUser", "testPassword", Roles.Admin)
		.onSuccess(userCreated -> {
			authenticateSession(currentSession, "TestUser", "testPassword")
			.onSuccess(authed -> {
				WebObjectCreator.createTask(currentSession, "Test2")
				.onSuccess(taskData -> {
					POST(currentSession, "/task/" + taskData.getString("UUID") + "/list", null, null)
					.onSuccess(versionList -> {
						JsonObject possibleVersion = null;
						JsonObject otherVersion = new JsonObject();
						for(int i = 0; i < versionList.bodyAsJsonArray().size(); i++)
						{
							if(versionList.bodyAsJsonArray().getJsonObject(i).containsKey("tag"))
							{
								possibleVersion = versionList.bodyAsJsonArray().getJsonObject(i);								
							}
							else
							{
								otherVersion.put("version", versionList.bodyAsJsonArray().getJsonObject(i).getString("version"));
							}
						}
						context.assertNotNull(possibleVersion);
						context.assertTrue(otherVersion.containsKey("version"));
						JsonObject versionToRemove = possibleVersion;
						Async removeAndAddAsync = context.async();
						POST(currentSession, "/task/" + taskData.getString("UUID") + "/removetags", null, new JsonArray().add(versionToRemove.getString("tag")))
						.compose(Void -> {
							return POST(currentSession, "/task/" + taskData.getString("UUID") + "/list", null, null);
						})
						.onSuccess(newversionList -> {
							boolean hasTag= false;
							for(int i = 0; i < newversionList.bodyAsJsonArray().size(); i++)
							{
								if(newversionList.bodyAsJsonArray().getJsonObject(i).containsKey("tag"))
								{
									hasTag = true;
									break;
								}
							}
							context.assertFalse(hasTag);
							POST(currentSession, "/task/" + taskData.getString("UUID") + "/"+ versionToRemove.getString("version") +"/addtag", null, new JsonObject().put("name", versionToRemove.getString("tag")))
							.compose(Void -> {
								return POST(currentSession, "/task/" + taskData.getString("UUID") + "/list", null, null);
							})
							.onSuccess(reAddedversionList -> {								
								boolean hasTagAgain = false;

								for(int i = 0; i < reAddedversionList.bodyAsJsonArray().size(); i++)
								{
									if(reAddedversionList.bodyAsJsonArray().getJsonObject(i).containsKey("tag"))
									{
										if(reAddedversionList.bodyAsJsonArray().getJsonObject(i).getString("tag").equals(versionToRemove.getString("tag")))
										{
											hasTagAgain = true;
											break;
										}
									}
								}
								context.assertTrue(hasTagAgain);
								Async testInvalidCall1 = context.async();
								// now check, that we can't have duplicate tags, and no two tags for the same version.
								POST(currentSession, "/task/" + taskData.getString("UUID") + "/"+ otherVersion.getString("version") +"/addtag", null, new JsonObject().put("name", versionToRemove.getString("tag")))
								.onSuccess(res -> context.fail("This Tag should not be allowed"))
								.onFailure(err -> {
									HttpException e = (HttpException) err;
									context.assertEquals(409, e.getStatusCode());									
									testInvalidCall1.complete();
								});
								Async testInvalidCall2 = context.async();
								// now check, that we can't have duplicate tags, and no two tags for the same version.
								POST(currentSession, "/task/" + taskData.getString("UUID") + "/"+ versionToRemove.getString("version") +"/addtag", null,  new JsonObject().put("name", "YaY"))
								.onSuccess(res -> context.fail("This Tag should not be allowed"))
								.onFailure(err -> {
									HttpException e = (HttpException) err;
									context.assertEquals(409, e.getStatusCode());																	
									testInvalidCall2.complete();
								});

								removeAndAddAsync.complete();

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
		})
		.onFailure(err -> context.fail(err));
	}

	@Test
	public void testList(TestContext context)
	{	
		System.out.println("--------------------  Testing List Retrieval ----------------------");

		Async setupAsync = context.async();				
		createUser(vertx, "TestUser", "testPassword", Roles.Admin)
		.compose(userCreated -> createAuthedSession("TestUser", "testPassword"))
		.onSuccess(adminSession -> {
			createResearcher(vertx, "Researcher", "pw")
			.compose(researcherCreated -> createAuthedSession("Researcher", "pw"))
			.onSuccess(researcherSession -> {
				createUser(vertx, "TestParticipant", "testPassword", Roles.Participant)
				.compose(userCreated -> createAuthedSession("TestParticipant", "testPassword"))
				.onSuccess(participantSession -> {
					WebObjectCreator.createTask(researcherSession, "PrivateTask")
					.onSuccess(taskData -> {
						Async taskListAsync = context.async();
						getElementList(adminSession, "task")
						.onSuccess(taskList-> {
							context.assertEquals(0, taskList.size());
							taskListAsync.complete();
						})
						.onFailure(err -> context.fail(err));
						Async fullListAsync = context.async();
						POST(adminSession, "/task/list",new JsonObject().put("full", true), null)
						.onSuccess(response -> {
							context.assertEquals(1, response.bodyAsJsonArray().size());
							fullListAsync.complete();
						})
						.onFailure(err -> context.fail(err));

						Async fullListesearcherAsync = context.async();
						POST(researcherSession, "/task/list",new JsonObject().put("full", true), null)
						.onSuccess(response -> {
							context.assertEquals(1, response.bodyAsJsonArray().size());
							fullListesearcherAsync .complete();
						})
						.onFailure(err -> context.fail(err));

						// now this needs to fail, as its not an admin or researcher call
						Async fullListFailAsync = context.async();
						POST(participantSession, "/task/list",new JsonObject().put("full", true),null)
						.onSuccess(res -> {
							context.fail("Only admin should be allowed to use full");
						})
						.onFailure(err -> fullListFailAsync.complete());					

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
	public void testDeletion(TestContext context)
	{	
		System.out.println("--------------------  Testing Element deletion ----------------------");

		Async setupAsync = context.async();				
		createUser(vertx, "TestUser", "testPassword", Roles.Admin)
		.compose(userCreated -> createAuthedSession("TestUser", "testPassword"))
		.onSuccess(adminSession -> {
			createResearcher(vertx, "Researcher", "pw")
			.compose(researcherCreated -> createAuthedSession("Researcher", "pw"))
			.onSuccess(researcherSession -> {
				createUser(vertx, "TestParticipant", "testPassword", Roles.Participant)
				.compose(userCreated -> createAuthedSession("TestParticipant", "testPassword"))
				.onSuccess(participantSession -> {
					WebObjectCreator.createTask(researcherSession, "PrivateTask")
					.onSuccess(taskData -> {
						Async taskListAsync = context.async();
						String taskID = taskData.getString("UUID"); 
						getElementList(adminSession, "task")
						.compose(taskList-> {
							context.assertEquals(0, taskList.size());
							return POST(adminSession, "/task/list",new JsonObject().put("full", true), null);							
						})
						.onSuccess(response -> {
							context.assertEquals(1, response.bodyAsJsonArray().size());							
							Async adminTest = context.async();
							POST(adminSession, "/task/"+ taskID + "/delete",new JsonObject(), null)
							.onSuccess(res -> context.fail("Should not be possible"))
							.onFailure(err -> adminTest.complete());
							Async deletionTest = context.async();
							POST(researcherSession, "/task/"+ taskID + "/delete",new JsonObject(), null)
							.compose(res -> {									

								return POST(adminSession, "/task/list",new JsonObject().put("full", true), null);
							})
							.compose(res -> {
								context.assertEquals(0, res.bodyAsJsonArray().size());										
								return POST(researcherSession, "/task/list",new JsonObject().put("full", true), null);
							})																		
							.compose(res -> {
								context.assertEquals(0, res.bodyAsJsonArray().size());
								return mongo_client.find(SoileConfigLoader.getCollectionName("taskCollection"), new JsonObject());
							})
							.onSuccess(mongoresult -> {
								context.assertEquals(1, mongoresult.size());
								System.out.println(mongoresult.get(0));
								deletionTest.complete();
							})
							.onFailure(err -> adminTest.complete());
							taskListAsync.complete();
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
	public void testTaskExists(TestContext context)
	{	
		System.out.println("--------------------  Testing Task exists  ----------------------");

		Async setupAsync = context.async();
		WebClientSession currentSession = createSession();
		createUser(vertx, "TestUser", "testPassword", Roles.Researcher)
		.onSuccess(userCreated -> {
			authenticateSession(currentSession, "TestUser", "testPassword")
			.onSuccess(authed -> {
				WebObjectCreator.createTask(currentSession, "Test2")
				.onSuccess(taskData -> {
					Async invalidReacreateAsync = context.async();
					WebObjectCreator.createTask(currentSession, "Test2")
					.onSuccess(res -> {
						context.fail("This should have errored with a does exist exception");
					})
					.onFailure(err -> {
						context.assertEquals(HttpException.class, err.getClass());
						context.assertEquals(409, ((HttpException)err).getStatusCode());
						invalidReacreateAsync.complete();
					});
					Async validReacreateAsync = context.async();
					WebObjectCreator.createOrRetrieveTask(currentSession, "Test2")
					.onSuccess(res -> {
						context.assertEquals(taskData.getValue("UUID"), res.getValue("UUID"));
						context.assertEquals(taskData.getValue("code"), res.getValue("code"));
						context.assertEquals(taskData.getValue("codeType"), res.getValue("codeType"));
						validReacreateAsync.complete();
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


	@Test
	public void testListElementVersions(TestContext context)
	{	
		System.out.println("--------------------  Testing Task exists  ----------------------");

		Async setupAsync = context.async();
		WebClientSession currentSession = createSession();
		createUser(vertx, "TestUser", "testPassword", Roles.Researcher)
		.onSuccess(userCreated -> {
			authenticateSession(currentSession, "TestUser", "testPassword")
			.onSuccess(authed -> {
				WebObjectCreator.createTask(currentSession, "Test2")
				.onSuccess(taskData -> {							
					Async listVersionsAsync = context.async();
					POST(currentSession, "/task/" + taskData.getString("UUID") + "/list", null, null)
					.onSuccess(res -> {
						JsonArray versions = res.bodyAsJsonArray();
						context.assertEquals(2, versions.size());
						boolean init_version_found = false;
						for(int i = 0; i < versions.size(); ++i)
						{
							if(versions.getJsonObject(i).containsKey("tag"))
							{
								context.assertEquals("Initial_Version", versions.getJsonObject(i).getString("tag"));
								init_version_found = true;
							}

						}
						context.assertTrue(init_version_found);
						listVersionsAsync.complete();
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
	@Test
	public void testExperimentCreation(TestContext context)
	{	
		System.out.println("--------------------  Testing Experiment generation ----------------------");

		Async setupAsync = context.async();
		createUserAndAuthedSession("TestAdmin", "testPassword", Roles.Researcher)
		.onSuccess(currentSession -> {
			WebObjectCreator.createExperiment(currentSession, "TestExperiment1")
			.onSuccess(experimentObject -> {
				context.assertEquals(2, experimentObject.getJsonArray("elements",new JsonArray()).size());
				JsonArray items = experimentObject.getJsonArray("elements");
				for(int i = 0; i < items.size(); ++i)
				{
					JsonObject currentItem = items.getJsonObject(i).getJsonObject("data");
					String itemName = currentItem.getString("name"); 
					switch(itemName)
					{
					case "Test1" : 
						context.assertEquals("tabcdefg2", currentItem.getString("instanceID"));
						context.assertEquals(1, currentItem.getJsonArray("outputs").size());
						break;
					case "Test2" : 
						context.assertEquals("tabcdefg3", currentItem.getString("instanceID"));
						context.assertEquals("tabcdefg2.smoker = 0", currentItem.getString("filter"));
						break;
					}
				}
				setupAsync.complete();
			})
			.onFailure(err -> context.fail(err));
		})
		.onFailure(err -> context.fail(err));
	}

	@Test
	public void testGetTagForVersion(TestContext context)
	{		
		System.out.println("--------------------  Testing Project Generation ----------------------");
		Async projAsync = context.async();
		createUserAndAuthedSession("TestUser", "testPassword", Roles.Researcher)
		.onSuccess(session -> {

			WebObjectCreator.createProject(session, "Testproject")
			.onSuccess( projectData -> {
				GET(session, "/project/" + projectData.getString("UUID") +"/" +projectData.getString("version") + "/gettag", null, null)
				.onSuccess(response -> {
					context.assertEquals("Initial_Version", response.bodyAsJsonObject().getValue("tag"));
					projAsync.complete();
				})
				.onFailure(err -> context.fail(err));
			})
			.onFailure(err -> context.fail(err));
		})
		.onFailure(err -> context.fail(err));
	}

	@Test
	public void testBuildProject(TestContext context)
	{		
		System.out.println("--------------------  Testing Project Generation ----------------------");
		Async projAsync = context.async();
		createUserAndAuthedSession("TestUser", "testPassword", Roles.Researcher)
		.onSuccess(session -> {
			Async plistAsync = context.async();
			Async elistAsync = context.async();
			Async tlistAsync = context.async();
			Async plistAsync2 = context.async();
			Async elistAsync2 = context.async();
			Async tlistAsync2 = context.async();
			WebObjectCreator.createProject(session, "Testproject")
			.onSuccess( projectData -> {
				createUserAndAuthedSession("AnotherUser", "testPassword", Roles.Researcher)
				.onSuccess(otherUserSession -> {
					getElementList(otherUserSession, "experiment")
					.onSuccess(expList -> {					
						// check, that the created Elements actually exist.
						context.assertEquals(1, expList.size()); // one private experiment
						elistAsync2.complete(); 
					})
					.onFailure(err -> context.fail(err));

					getElementList(otherUserSession, "project")
					.onSuccess(list -> {
						context.assertEquals(1,list.size()); // no private data
						plistAsync2.complete();
					})
					.onFailure(err -> context.fail(err));

					getElementList(otherUserSession, "task")
					.onSuccess(list -> {
						context.assertEquals(5,list.size()); // one private task
						tlistAsync2.complete();
					})
					.onFailure(err -> context.fail(err));

				})
				.onFailure(err -> context.fail(err));				

				getElementList(session, "experiment")
				.onSuccess(expList -> {					
					// check, that the created Elements actually exist.
					context.assertEquals(2, expList.size()); // one private experiment
					elistAsync.complete(); 
				})
				.onFailure(err -> context.fail(err));

				getElementList(session, "project")
				.onSuccess(list -> {
					context.assertEquals(1,list.size()); // no private data
					plistAsync.complete();
				})
				.onFailure(err -> context.fail(err));

				getElementList(session, "task")
				.onSuccess(list -> {
					context.assertEquals(6,list.size()); // one private task
					tlistAsync.complete();
				})
				.onFailure(err -> context.fail(err));
				projAsync.complete();				
			})
			.onFailure(err -> context.fail(err));
		})
		.onFailure(err -> context.fail(err));
	}

	@Test
	public void testRemoveTags(TestContext context)
	{	
		System.out.println("--------------------  Testing Experiment generation ----------------------");

		Async setupAsync = context.async();
		createUserAndAuthedSession("TestAdmin", "testPassword", Roles.Researcher)
		.onSuccess(currentSession -> {
			createUserAndAuthedSession("Reasearcher2", "testPassword", Roles.Researcher)
			.onSuccess(wrongSession -> {
				WebObjectCreator.createExperiment(currentSession, "TestExperiment1")
				.onSuccess(experimentObject -> {
					String expVersion = experimentObject.getString("version");
					String expUUID = experimentObject.getString("UUID");
					Async testRemoveAsync = context.async();
					GET(currentSession, "/experiment/" + expUUID +"/" +expVersion + "/gettag", null, null)
					.onSuccess(response -> {

						context.assertEquals("Initial_Version", response.bodyAsJsonObject().getValue("tag"));
						// we only have the one.context
						Async failedAccess = context.async();
						// only allowed for READ_WRITE / FULL access. 
						POST(wrongSession, "/experiment/" + expUUID +"/removetags", null, new JsonArray().add("Initial_Version"))
						.onSuccess(res -> context.fail("Should not be allowed"))
						.onFailure(fail -> failedAccess.complete());

						POST(currentSession, "/experiment/" + expUUID +"/removetags", null, new JsonArray().add("Initial_Version"))
						.onSuccess(res -> 
						{
							POST(currentSession, "/experiment/" + expUUID + "/list", null, null)
							.onSuccess(listrespsone -> {
								// this should list the removed Version as canbetagged.
								JsonArray versionList = listrespsone.bodyAsJsonArray();
								boolean taggingFound = false;
								for(int i = 0; i < versionList.size(); i++)
								{									
									if(versionList.getJsonObject(i).getString("version").equals(expVersion))
									{
										context.assertTrue(versionList.getJsonObject(i).getBoolean("canbetagged"));
										taggingFound = true;
									}
								}
								context.assertTrue(taggingFound);
								GET(currentSession, "/experiment/" + expUUID +"/" +expVersion + "/gettag", null, null)
								.onSuccess(tagResponse -> context.fail("There should be no tag and thus the request should fail."))
								.onFailure(err -> {
									HttpException e = (HttpException) err;
									context.assertEquals(404, e.getStatusCode());
									// lets try to reinstate the version. 
									experimentObject.put("tag", "Initial_Version");
									Async remakeTagAsync = context.async();
									POST(currentSession, "/experiment/" + expUUID +"/" +expVersion + "/post",null,experimentObject)
									.onSuccess(versionResponse -> {
										remakeTagAsync.complete();
									})
									.onFailure(newerr -> context.fail(newerr));
									testRemoveAsync.complete();
								});	
							})
							.onFailure(err -> context.fail(err));
						})
						.onFailure(err -> context.fail(err));

					}).onFailure(err -> context.fail(err));
					setupAsync.complete();
				})
				.onFailure(err -> context.fail(err));
			})
			.onFailure(err -> context.fail(err));
		})
		.onFailure(err -> context.fail(err));
	}

	@Test
	public void testProjectsParseOK(TestContext context)
	{		
		System.out.println("--------------------  Testing Project Generation ----------------------");
		Async projAsync = context.async();
		createUserAndAuthedSession("TestUser", "testPassword", Roles.Researcher)
		.onSuccess(session -> {
			WebObjectCreator.createProject(session, "ExampleProject")
			.onSuccess( projectData -> {		
				Async gitRepoAsync = context.async();
				vertx.eventBus().request("soile.git.getGitFileContentsAsJson",new GitFile("Object.json", "P" + projectData.getString("UUID"), projectData.getString("version")).toJson())
				.onSuccess(response -> {
					// Check, that it is correct in git.
					JsonObject gitData = (JsonObject) response.body();
					JsonArray tasks = gitData.getJsonArray("tasks");
					context.assertEquals(2, tasks.size());
					for(int i = 0; i < tasks.size(); ++i)
					{
						if(tasks.getJsonObject(i).getString("name").equals("JSExp"))
						{
							context.assertTrue(tasks.getJsonObject(i).containsKey("position"));
							context.assertEquals(950, tasks.getJsonObject(i).getJsonObject("position").getNumber("x"));
						}
						else
						{
							context.assertTrue(tasks.getJsonObject(i).containsKey("position"));
							context.assertEquals(100, tasks.getJsonObject(i).getJsonObject("position").getNumber("x"));
						}
					}

					JsonArray experiments = gitData.getJsonArray("experiments");
					context.assertTrue(experiments.getJsonObject(0).containsKey("position"));
					context.assertEquals(650, experiments.getJsonObject(0).getJsonObject("position").getNumber("x"));
					JsonArray expElements = experiments.getJsonObject(0).getJsonArray("elements");
					context.assertEquals(2, expElements.size());
					for(int i = 0; i < expElements.size(); ++i)
					{
						context.assertTrue(expElements.getJsonObject(i).containsKey("data"));
						JsonObject elementData = expElements.getJsonObject(i).getJsonObject("data"); 
						if(elementData.getString("name").equals("ElangExp"))
						{
							context.assertTrue(elementData.containsKey("position"));
							context.assertEquals(100, elementData.getJsonObject("position").getNumber("x"));
						}
						else
						{
							context.assertTrue(elementData.containsKey("position"));
							context.assertEquals(350, elementData.getJsonObject("position").getNumber("x"));
						}
					}
					JsonArray filters = gitData.getJsonArray("filters");
					context.assertTrue(filters.getJsonObject(0).containsKey("position"));
					context.assertEquals(350, filters.getJsonObject(0).getJsonObject("position").getNumber("x"));
					gitRepoAsync.complete();
				})
				.onFailure(err -> context.fail(err));
				JsonArray tasks = projectData.getJsonArray("tasks");
				context.assertEquals(2, tasks.size());
				for(int i = 0; i < tasks.size(); ++i)
				{
					if(tasks.getJsonObject(i).getString("name").equals("JSExp"))
					{
						context.assertTrue(tasks.getJsonObject(i).containsKey("position"));
						context.assertEquals(950, tasks.getJsonObject(i).getJsonObject("position").getNumber("x"));
					}
					else
					{
						context.assertTrue(tasks.getJsonObject(i).containsKey("position"));
						context.assertEquals(100, tasks.getJsonObject(i).getJsonObject("position").getNumber("x"));
					}
				}

				JsonArray experiments = projectData.getJsonArray("experiments");
				context.assertTrue(experiments.getJsonObject(0).containsKey("position"));
				context.assertEquals(650, experiments.getJsonObject(0).getJsonObject("position").getNumber("x"));
				JsonArray expElements = experiments.getJsonObject(0).getJsonArray("elements");
				context.assertEquals(2, expElements.size());
				for(int i = 0; i < expElements.size(); ++i)
				{
					context.assertTrue(expElements.getJsonObject(i).containsKey("data"));
					JsonObject elementData = expElements.getJsonObject(i).getJsonObject("data"); 
					if(elementData.getString("name").equals("ElangExp"))
					{
						context.assertTrue(elementData.containsKey("position"));
						context.assertEquals(100, elementData.getJsonObject("position").getNumber("x"));
					}
					else
					{
						context.assertTrue(elementData.containsKey("position"));
						context.assertEquals(350, elementData.getJsonObject("position").getNumber("x"));
					}
				}
				JsonArray filters = projectData.getJsonArray("filters");
				context.assertTrue(filters.getJsonObject(0).containsKey("position"));
				context.assertEquals(350, filters.getJsonObject(0).getJsonObject("position").getNumber("x"));
				projAsync.complete();		
			})
			.onFailure(err -> context.fail(err));
		})
		.onFailure(err -> context.fail(err));
	}


	@Test
	public void testUpdateWebTask(TestContext context)
	{	
		System.out.println("--------------------  Testing Task update ----------------------");

		Async setupAsync = context.async();
		WebClientSession currentSession = createSession();
		createUser(vertx, "TestUser", "testPassword", Roles.Admin)
		.onSuccess(userCreated -> {
			authenticateSession(currentSession, "TestUser", "testPassword")
			.onSuccess(authed -> {
				WebObjectCreator.createTask(currentSession, "Test2")
				.onSuccess(taskData -> {
					Async taskInfoAsync = context.async();
					String taskID = taskData.getString("UUID");
					String taskVersion = taskData.getString("version");
					getElement(currentSession, "task", taskID, taskVersion)					
					.compose(newData -> {
						context.assertEquals(taskID, newData.getString("UUID"));
						context.assertEquals(taskVersion, newData.getString("version"));
						context.assertEquals(taskData.getString("code"), newData.getString("code"));
						context.assertTrue(taskData.getString("code").contains("intermezzo"));
						context.assertEquals("UNKNOWN", newData.getValue("author"));
						context.assertEquals(0, newData.getJsonArray("keywords").size());						
						return mongo_client.findOne(SoileConfigLoader.getCollectionName("taskCollection"), new JsonObject().put("_id", taskID), null);												
					})
					.onSuccess(db -> {						
						context.assertEquals("UNKNOWN", db.getValue("author"));
						Async updateAsync = context.async();
						JsonObject TaskUpdate = new JsonObject().put("UUID", taskID)
								.put("version", taskVersion)
								.put("author", "New Author")
								.put("description", "New description")
								.put("keywords", new JsonArray().add("keyword1"));

						POST(currentSession, "/task/" + taskID +"/" +taskVersion + "/post" , null, TaskUpdate)
						.onSuccess(response -> {

							String newVersion = response.bodyAsJsonObject().getString("version");
							getElement(currentSession, "task", taskID, newVersion)					
							.compose(newData -> {
								context.assertEquals(taskID, newData.getString("UUID"));
								context.assertEquals(newVersion, newData.getString("version"));
								context.assertEquals(taskData.getString("code"), newData.getString("code"));
								context.assertTrue(taskData.getString("code").contains("intermezzo"));
								context.assertEquals("New Author", newData.getValue("author"));
								context.assertEquals(TaskUpdate.getValue("description"), newData.getValue("description"));
								context.assertEquals(1, newData.getJsonArray("keywords").size());						
								return mongo_client.findOne(SoileConfigLoader.getCollectionName("taskCollection"), new JsonObject().put("_id", taskID), null);												
							})							
							.onSuccess(dbData -> {
								context.assertEquals("New Author", dbData.getValue("author"));	
								updateAsync.complete();
							})
							.onFailure(err -> context.fail(err));	
						})
						.onFailure(err -> context.fail(err));	
						taskInfoAsync.complete();


					})
					.onFailure(err -> context.fail(err));

					Async taskListAsync = context.async();

					getElementList(currentSession, "task")
					.onSuccess(taskList-> {
						context.assertEquals(1, taskList.size());
						context.assertEquals(taskID, taskList.getJsonObject(0).getValue("UUID"));
						context.assertEquals(taskData.getString("name"), taskList.getJsonObject(0).getValue("name"));
						taskListAsync.complete();
					})
					.onFailure(err -> context.fail(err));
					// now we run an update and see what happens

					setupAsync.complete();

				})
				.onFailure(err -> context.fail(err));			 
			})
			.onFailure(err -> context.fail(err));
		})
		.onFailure(err -> context.fail(err));
	}

}
