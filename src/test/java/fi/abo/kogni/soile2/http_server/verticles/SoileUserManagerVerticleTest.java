package fi.abo.kogni.soile2.http_server.verticles;

import java.net.HttpURLConnection;

import org.junit.Test;

import fi.abo.kogni.soile2.http_server.SoileVerticleTest;
import fi.abo.kogni.soile2.http_server.UserVerticleTest;
import fi.abo.kogni.soile2.http_server.auth.SoileAuthorization.PermissionType;
import fi.abo.kogni.soile2.http_server.auth.SoileAuthorization.Roles;
import fi.abo.kogni.soile2.http_server.auth.SoilePermissionProvider;
import fi.abo.kogni.soile2.http_server.userManagement.SoileHashing;
import fi.abo.kogni.soile2.utils.SoileCommUtils;
import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.vertx.core.eventbus.ReplyException;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.HashingStrategy;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;

public class SoileUserManagerVerticleTest extends SoileVerticleTest implements UserVerticleTest {

	@Test
	public void createUser(TestContext context)
	{
		System.out.println("----------- Testing user creation --------------");
		Async userAsync = context.async();
		String username = "TestUser";		
		createUser(vertx, username, "testPassword", "User Name", "email@there.fi", Roles.Admin)
		.onSuccess(created -> {
			Async checkUserDetailsAsync = context.async();
			getUserDetailsFromDB(mongo_client, username)
			.onSuccess(userData -> {
				System.out.println(userData.encodePrettily());
				context.assertEquals(username, userData.getValue(SoileConfigLoader.getUserdbField("usernameField")));
				context.assertEquals("email@there.fi", userData.getValue(SoileConfigLoader.getUserdbField("userEmailField")));
				context.assertEquals(Roles.Admin.toString(), userData.getValue(SoileConfigLoader.getUserdbField("userRolesField")));
				context.assertEquals("User Name", userData.getValue(SoileConfigLoader.getUserdbField("userFullNameField")));
				HashingStrategy strategy = new SoileHashing(SoileConfigLoader.getUserProperty("serverSalt"));
				
				context.assertTrue(strategy.verify(userData.getString(SoileConfigLoader.getUserdbField("passwordField")),"testPassword"));				
				checkUserDetailsAsync.complete();
				})
			.onFailure(err -> context.fail(err));
			Async checkDuplicateUserName = context.async();
			System.out.println("----------- Testing username exists --------------");
			createUser(vertx, username, "otherPassword", "User Name", "email@here.fi", Roles.Admin)
			.onSuccess(err -> {
				context.fail("Should not be sucessfull. user already exists");
			})
			.onFailure(err -> {
				if(err instanceof ReplyException)
				{
					ReplyException ex = (ReplyException) err;
					context.assertEquals(HttpURLConnection.HTTP_CONFLICT, ex.failureCode());
					context.assertEquals("User already exists", ex.getMessage());
					checkDuplicateUserName.complete();
				}
				else
				{
					System.out.println("Failing due to:");
					err.printStackTrace(System.out);
					context.fail(err);
				}
			});
			Async checkDuplicateUserEmail = context.async();
			System.out.println("----------- Testing email exists --------------");
			createUser(vertx, "AnotherName", "otherPassword", "User Name", "email@there.fi", Roles.Admin)
			.onSuccess(err -> {
				context.fail("Should not be sucessfull. email already exists");
			})
			.onFailure(err -> {
				if(err instanceof ReplyException)
				{
					
					ReplyException ex = (ReplyException) err;
					context.assertEquals(HttpURLConnection.HTTP_CONFLICT, ex.failureCode());
					context.assertEquals("Email already in use", ex.getMessage());
					getUserDetailsFromDB(mongo_client, "AnotherName")
					.onSuccess(res -> {
						context.assertNull(res);
						checkDuplicateUserEmail.complete();
					})
					.onFailure(err2 -> context.fail(err2));					
				}
				else
				{
					System.out.println("Failing due to:");
					err.printStackTrace(System.out);
					context.fail(err);
				}
			});					
			userAsync.complete();
		})
		.onFailure(err -> context.fail(err));
		Async defaultRoleAsync = context.async();
		createUser(vertx, "SecondUser", "testPassword")
		.onSuccess(created -> {
			getUserDetailsFromDB(mongo_client, "SecondUser")
			.onSuccess(userData -> {
				System.out.println(userData.encodePrettily());
				context.assertEquals("SecondUser", userData.getValue(SoileConfigLoader.getUserdbField("usernameField")));				
				context.assertEquals(Roles.Participant.toString(), userData.getValue(SoileConfigLoader.getUserdbField("userRolesField")));				
				HashingStrategy strategy = new SoileHashing(SoileConfigLoader.getUserProperty("serverSalt"));				
				context.assertTrue(strategy.verify(userData.getString(SoileConfigLoader.getUserdbField("passwordField")),"testPassword"));				
				defaultRoleAsync.complete();
				})
			.onFailure(err -> context.fail(err));
		})
		.onFailure(err -> context.fail(err));
	}
	
	@Test
	public void deleteUser(TestContext context)
	{
		System.out.println("----------- Testing use deletion --------------");
		Async userAsync = context.async();
		String username = "TestUser";		
		createUser(vertx, username, "testPassword")
		.onSuccess(created -> {
			createUser(vertx, "TestUser2", "testPassword")
			.onSuccess(user2Created -> {
				getUserDetailsFromDB(mongo_client, username).onSuccess(userDetails -> {
					context.assertEquals(username, userDetails.getValue(SoileConfigLoader.getUserdbField("usernameField")));
					HashingStrategy strategy = new SoileHashing(SoileConfigLoader.getUserProperty("serverSalt"));
					context.assertTrue(strategy.verify(userDetails.getString(SoileConfigLoader.getUserdbField("passwordField")),"testPassword"));
					vertx.eventBus().request(SoileCommUtils.getEventBusCommand(SoileConfigLoader.USERMGR_CFG, "removeUser"), 
											 new JsonObject().put("username", username))
					.onSuccess( deleted -> {
						Async lookupOrigUser = context.async();
						getUserDetailsFromDB(mongo_client, username)
						.onSuccess(userData -> {
							context.assertNull(userData);
							lookupOrigUser.complete();
						})
						.onFailure(err -> context.fail(err));
						Async lookupSecondUser = context.async();
						getUserDetailsFromDB(mongo_client, "TestUser2")
						.onSuccess(userData2 -> {
							context.assertEquals("TestUser2", userData2.getValue(SoileConfigLoader.getUserdbField("usernameField")));							
							context.assertTrue(strategy.verify(userData2.getString(SoileConfigLoader.getUserdbField("passwordField")),"testPassword"));
							lookupSecondUser.complete();
						})
						.onFailure(err -> context.fail(err));
						userAsync.complete();
					});
				})
				.onFailure(err -> context.fail(err));
			})
			.onFailure(err -> context.fail(err));			
		})
		.onFailure(err -> context.fail(err));		
	}
	
	
	@Test
	public void addUserWithEmail(TestContext context)
	{
		System.out.println("----------- Testing User creation with email --------------");
		Async userAsync = context.async();
		String blockedEmail = "this@that.fi";
		String blockedUser = "Blocking";
		JsonObject WorkingTestUser = new JsonObject().put("username", "TestUser").put("password", "newPass").put("email", "some@other.rp");
		JsonObject EmailNotWorkingTestUser = new JsonObject().put("username", "AnotherUser").put("password", "newPass").put("email", blockedEmail);
		JsonObject NameNotWorkingTestUser = new JsonObject().put("username", blockedUser).put("password", "newPass").put("email", "something@else.fi");
		createUser(vertx, blockedUser, "testPassword", "Fullname", blockedEmail, Roles.Participant )
		.onSuccess(created -> {
			Async nameNotWorking = context.async();
			System.out.println("Trying to create user with blocking name");
			eb.request(getUsermanagerEventBusAddress("addUserWithEmail"), NameNotWorkingTestUser)
			.onSuccess(err -> {
				context.fail("Should fail due to duplicate Name");
			})
			.onFailure(success -> {
				System.out.println("Could not add user with blocking name because of: " + success.getMessage());
				nameNotWorking.complete();
			});
			System.out.println("Trying to create user with blocking email");
			Async emailNotWorking = context.async();
			eb.request(getUsermanagerEventBusAddress("addUserWithEmail"), EmailNotWorkingTestUser)
			.onSuccess(err -> {
				context.fail("Should fail due to duplicate email");
			})
			.onFailure(success -> {
				System.out.println("Could not add user with blocking email because of: " + success.getMessage());
				getUserDetailsFromDB(mongo_client, "AnotherUser")
				.onSuccess(res -> {
					context.assertNull(res);
					emailNotWorking.complete();
				})
				.onFailure(err -> context.fail(err));				
			});
			Async additionWorking = context.async();
			System.out.println("Trying to create working user");
			eb.request(getUsermanagerEventBusAddress("addUserWithEmail"), WorkingTestUser)
			.onSuccess(reply -> {
				JsonObject response = (JsonObject) reply.body();
				context.assertEquals(SoileCommUtils.SUCCESS,  response.getString(SoileCommUtils.RESULTFIELD));
				getUserDetailsFromDB(mongo_client, "TestUser")
				.onSuccess(userData -> {
					context.assertNotNull(userData);
					context.assertEquals("TestUser", userData.getValue(SoileConfigLoader.getUserdbField("usernameField")));				
					context.assertEquals(Roles.Participant.toString(), userData.getValue(SoileConfigLoader.getUserdbField("userRolesField")));
					context.assertEquals("some@other.rp", userData.getValue(SoileConfigLoader.getUserdbField("userEmailField")));
					HashingStrategy strategy = new SoileHashing(SoileConfigLoader.getUserProperty("serverSalt"));				
					context.assertTrue(strategy.verify(userData.getString(SoileConfigLoader.getUserdbField("passwordField")),"newPass"));	
					additionWorking.complete();
				})
				.onFailure(err -> context.fail(err));
				
			})			
			.onFailure(err -> context.fail(err));				
			userAsync.complete();
		})
		.onFailure(err -> context.fail(err));		
	}
	
	@Test
	public void testpermissionOrRoleChange(TestContext context)
	{
		System.out.println("----------- Testing Permission Setting --------------");
		JsonObject aPermission = new JsonObject().put("type", "READ")
				    							 .put("target", "a");
		String aPermissionTest =  SoilePermissionProvider.buildPermissionString("a", PermissionType.READ);
		JsonObject a2Permission = new JsonObject().put("type", "FULL")
				 .put("target", "a");
		String a2PermissionTest =  SoilePermissionProvider.buildPermissionString("a", PermissionType.FULL);
		JsonObject bPermission = new JsonObject().put("type", "FULL")
				 .put("target", "b");
		String bPermissionTest =  SoilePermissionProvider.buildPermissionString("b", PermissionType.FULL);
		Async testAsync = context.async();
		//create a few users
		createUser(vertx, "NewUser", "testPassword", "Fullname", "New@mail.fi", Roles.Participant )
		.compose(Void -> { return createUser(vertx, "NewUser2", "testPassword", "Fullname", "New@mail.fi", Roles.Participant);})
		.compose(Void -> { return createUser(vertx, "NewUser3", "testPassword", "Fullname", "New@mail.fi", Roles.Participant);})
		.onSuccess(created -> {
			JsonObject RoleChange = new JsonObject().put("username", "NewUser").put("role", Roles.Researcher);
			Async roleChangeAsync = context.async();
			eb.request(getUsermanagerEventBusAddress("permissionOrRoleChange"), RoleChange)
			.onSuccess(res -> {
				context.assertEquals(res.body().getClass(), JsonObject.class);
				getUserDetailsFromDB(mongo_client, "NewUser")
				.onSuccess(userData -> {
					context.assertEquals(Roles.Researcher.toString(), userData.getString(SoileConfigLoader.getUserdbField("userRolesField")));
					roleChangeAsync.complete();
				})
				.onFailure(err -> context.fail(err));
			})
			.onFailure(err -> context.fail(err));
			// test permission addition.
			Async PermissionTest = context.async();
			JsonObject addPermissionChange = new JsonObject().put("username", "NewUser")
					  										 .put("command", "add")
														     .put("permissionsProperties", new JsonObject().put("elementType", "task")
																  										.put("permissionsSettings", new JsonArray().add(aPermission).add(bPermission).add(a2Permission)));					
			eb.request(getUsermanagerEventBusAddress("permissionOrRoleChange"), addPermissionChange)
			.onSuccess(add -> {
				 context.assertEquals(SoileCommUtils.SUCCESS, ((JsonObject)add.body()).getValue(SoileCommUtils.RESULTFIELD));
				getUserDetailsFromDB(mongo_client, "NewUser")
				.onSuccess(userData-> {
					System.out.println(userData.encodePrettily());
					JsonArray taskPermissions = userData.getJsonArray(SoileConfigLoader.getMongoTaskAuthorizationOptions().getPermissionField());
					System.out.println("Checking if " + aPermissionTest +  " is in array");
					context.assertTrue(taskPermissions.contains(aPermissionTest));
					context.assertTrue(taskPermissions.contains(a2PermissionTest));
					context.assertTrue(taskPermissions.contains(bPermissionTest));
					JsonObject removePermissionChange = new JsonObject().put("username", "NewUser")
																		.put("command", "remove")
																	    .put("permissionsProperties", new JsonObject().put("elementType", "task")
																			  										.put("permissionsSettings", new JsonArray().add(aPermission)));					
					eb.request(getUsermanagerEventBusAddress("permissionOrRoleChange"), removePermissionChange)
					.onSuccess(removed -> {
						context.assertEquals(SoileCommUtils.SUCCESS, ((JsonObject)removed.body()).getValue(SoileCommUtils.RESULTFIELD));
						getUserDetailsFromDB(mongo_client, "NewUser")
						.onSuccess(userData2-> {
							System.out.println(userData2.encodePrettily());
							JsonArray taskPermissions2 = userData2.getJsonArray(SoileConfigLoader.getMongoTaskAuthorizationOptions().getPermissionField());
							context.assertFalse(taskPermissions2.contains(aPermissionTest));
							context.assertTrue(taskPermissions2.contains(a2PermissionTest));
							JsonObject setPermissionChange = new JsonObject().put("username", "NewUser")
									.put("command", "set")
								    .put("permissionsProperties", new JsonObject().put("elementType", "task")
										  										.put("permissionsSettings", new JsonArray().add(aPermission)));
							eb.request(getUsermanagerEventBusAddress("permissionOrRoleChange"), setPermissionChange)
							.onSuccess(set -> {
								context.assertEquals(SoileCommUtils.SUCCESS, ((JsonObject)set.body()).getValue(SoileCommUtils.RESULTFIELD));
								getUserDetailsFromDB(mongo_client, "NewUser")
								.onSuccess(userData3-> {
									JsonArray taskPermissions3 = userData3.getJsonArray(SoileConfigLoader.getMongoTaskAuthorizationOptions().getPermissionField());
									context.assertTrue(taskPermissions3.contains(aPermissionTest));
									context.assertFalse(taskPermissions3.contains(a2PermissionTest));
									context.assertFalse(taskPermissions3.contains(bPermissionTest));
									PermissionTest.complete();
								})
								.onFailure(err ->context.fail(err));
							})
							.onFailure(err ->context.fail(err));

						})
						.onFailure(err ->context.fail(err));
					})
					.onFailure(err ->context.fail(err));

				})
				.onFailure(err ->context.fail(err));
			})
			.onFailure(success -> {				
			});
			JsonObject failObject = new JsonObject().put("username", "NewUser2").put("role", Roles.Researcher).put("permissionsProperties", new JsonObject().put("elementType", "task")
						.put("permissionsSettings", new JsonArray().add(aPermission)));
			Async failRequestForDuplicate = context.async();  
			// Fail dual request
			eb.request(getUsermanagerEventBusAddress("permissionOrRoleChange"), failObject)
			.onSuccess(add -> {
				context.fail("Should fail due to because of both role and permissions");				
			})
			.onFailure(err -> {
				ReplyException e = (ReplyException) err;
				context.assertEquals(400,e.failureCode());
				context.assertEquals("Cannot change permission and role settings at the same time", e.getMessage());
				getUserDetailsFromDB(mongo_client, "NewUser2")
				.onSuccess(res -> {
					context.assertEquals(Roles.Participant.toString(), res.getString(SoileConfigLoader.getUserdbField("userRolesField")));
					failRequestForDuplicate.complete();
				})
				.onFailure(err2 -> context.fail(err2));				
			});
			testAsync.complete();

		})
		.onFailure(err -> context.fail(err));		
	}

	
	@Test
	public void testAddAndRemoveSession(TestContext context)
	{		
		//create a few users
		Async testAsync = context.async();
		createUser(vertx, "NewUser", "testPassword", "Fullname", "New@mail.fi", Roles.Participant )
		.onSuccess(created -> {						 
			// Fail dual request
			eb.request(getUsermanagerEventBusAddress("addSession"), new JsonObject().put("sessionID", "NewSession").put("username", "NewUser"))
			.onSuccess(added -> {
				getUserDetailsFromDB(mongo_client, "NewUser")
				.onSuccess(data -> {
					HashingStrategy strategy = new SoileHashing(SoileConfigLoader.getUserProperty("serverSalt"));
					String sessionHash = strategy.hash(SoileConfigLoader.getUserProperty("hashingAlgorithm"), null, SoileConfigLoader.getSessionProperty("sessionStoreSecret"), "NewSession");
					context.assertTrue(data.getJsonObject(SoileConfigLoader.getUserdbField("storedSessions")).containsKey(sessionHash));
					Async removeSession = context.async();
					eb.request(getUsermanagerEventBusAddress("removeSession"), new JsonObject().put("sessionID", "OldSession").put("username", "NewUser"))
					.onSuccess(nothing -> {
						getUserDetailsFromDB(mongo_client, "NewUser")
						.onSuccess(data2 -> {
							context.assertTrue(data2.getJsonObject(SoileConfigLoader.getUserdbField("storedSessions")).containsKey(sessionHash));
							eb.request(getUsermanagerEventBusAddress("removeSession"), new JsonObject().put("sessionID", "NewSession").put("username", "NewUser"))
							.onSuccess(nothing2 -> {
								getUserDetailsFromDB(mongo_client, "NewUser")
								.onSuccess(data3 -> {
									context.assertFalse(data3.getJsonObject(SoileConfigLoader.getUserdbField("storedSessions")).containsKey(sessionHash));
									removeSession.complete();
								})
								.onFailure(err -> context.fail(err));
							})
							.onFailure(err -> context.fail(err));
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
	
	@Test
	public void testcheckUserSessionValid(TestContext context)
	{		
		Async testAsync = context.async();
		//create a few users
		JsonObject userSession = new JsonObject().put("sessionID", "NewSession").put("username", "NewUser");
		JsonObject invalidSession = new JsonObject().put("sessionID", "OldSession").put("username", "NewUser");
		JsonObject userNotFoundSession = new JsonObject().put("sessionID", "OldSession").put("username", "NewUser");
		createUser(vertx, "NewUser", "testPassword", "Fullname", "New@mail.fi", Roles.Participant )
		.onSuccess(created -> {			
			eb.request(getUsermanagerEventBusAddress("addSession"), userSession)
			.onSuccess(added -> {
				Async validAsync = context.async();
				eb.request(getUsermanagerEventBusAddress("checkUserSessionValid"), userSession)
				.onSuccess(reply -> {
					JsonObject response = (JsonObject) reply.body();
					context.assertEquals(SoileCommUtils.SUCCESS,  response.getString(SoileCommUtils.RESULTFIELD));
					validAsync.complete();
				})
				.onFailure(err -> context.fail(err));
				Async invalidAsync = context.async();

				eb.request(getUsermanagerEventBusAddress("checkUserSessionValid"), invalidSession)
				.onSuccess(reply -> {
					context.fail("This should have failed");
				})
				.onFailure(err -> {										
					invalidAsync.complete();
				});	
				Async invalidUserAsync = context.async();
				eb.request(getUsermanagerEventBusAddress("checkUserSessionValid"), userNotFoundSession)
				.onSuccess(reply -> {
					context.fail("This should have failed");
				})
				.onFailure(err -> {										
					invalidUserAsync.complete();
				});	
				testAsync.complete();
			})
			.onFailure(err -> context.fail(err));
		})
		.onFailure(err -> context.fail(err));		
	}
	
	@Test
	public void testParticipantCommands(TestContext context)
	{		
		Async testAsync = context.async();
		//create a few users
		JsonObject addToProject = new JsonObject().put("projectInstanceID", "projectID").put("participantID", "NewID").put("username", "NewUser");
		JsonObject addTo2ndProject = new JsonObject().put("projectInstanceID", "projectID2").put("participantID", "NewID2").put("username", "NewUser");
		JsonObject invalidProject = new JsonObject().put("projectInstanceID", "invalidProject").put("participantID", "NewID3").put("username", "NewUser");
		createUser(vertx, "NewUser", "testPassword", "Fullname", "New@mail.fi", Roles.Participant )
		.onSuccess(created -> {			
			eb.request(getUsermanagerEventBusAddress("makeUserParticpantInProject"), addToProject)
			.compose(Void -> eb.request(getUsermanagerEventBusAddress("makeUserParticpantInProject"), addTo2ndProject))
			.onSuccess(added -> {
				Async validAsync = context.async();
				eb.request(getUsermanagerEventBusAddress("getParticipantForUserInProject"), addToProject)
				.onSuccess(reply -> {
					JsonObject response = (JsonObject) reply.body();
					context.assertEquals(SoileCommUtils.SUCCESS,  response.getString(SoileCommUtils.RESULTFIELD));
					context.assertEquals("NewID",  response.getString("participantID"));
					validAsync.complete();
				})
				.onFailure(err -> context.fail(err));
				Async invalidAsync = context.async();
				eb.request(getUsermanagerEventBusAddress("getParticipantForUserInProject"), invalidProject)
				.onSuccess(reply -> {
					JsonObject response = (JsonObject) reply.body();
					context.assertEquals(SoileCommUtils.SUCCESS,  response.getString(SoileCommUtils.RESULTFIELD));
					context.assertEquals(null,  response.getString("participantID"));
					invalidAsync.complete();
				})
				.onFailure(err -> context.fail(err));
				Async participantListAsync = context.async();
				eb.request(getUsermanagerEventBusAddress("getParticipantsForUser"), addToProject)
				.onSuccess(reply -> {
					JsonObject response = (JsonObject) reply.body();
					context.assertEquals(SoileCommUtils.SUCCESS,  response.getString(SoileCommUtils.RESULTFIELD));
					JsonArray participants =  response.getJsonArray("participantIDs");
					System.out.println(participants.encodePrettily());
					boolean id1 = false;
					boolean id2 = false;
					boolean id3 = false;
					for(int i = 0; i < participants.size(); ++i)
					{
						if(participants.getJsonObject(i).getString("participantID").equals("NewID"))
						{
							context.assertEquals("projectID", participants.getJsonObject(i).getString("uuid"));
							id1 = true;
						}
						if(participants.getJsonObject(i).getString("participantID").equals("NewID2"))
						{
							context.assertEquals("projectID2", participants.getJsonObject(i).getString("uuid"));
							id2 = true;
						}
						if(participants.getJsonObject(i).getString("participantID").equals("NewID3"))
						{
							context.assertEquals("invalidProject", participants.getJsonObject(i).getString("uuid"));
							id3 = true;
						}						
					}
					context.assertTrue(id1 && id2 && !id3);
					participantListAsync.complete();
				})
				.onFailure(err -> context.fail(err));
				testAsync.complete();
			})
			.onFailure(err -> context.fail(err));
		})
		.onFailure(err -> context.fail(err));		
	}
	
	@Test
	public void testUserInfo(TestContext context)
	{		
		Async testAsync = context.async();
		//create a few users
		createUser(vertx, "NewUser", "testPassword", "Fullname", "New@mail.fi", Roles.Researcher )
		.compose(Void -> createUser(vertx, "NewUser2", "testPassword2"))
		.onSuccess(created -> {			
			Async user1Async = context.async(); 
			eb.request(getUsermanagerEventBusAddress("getUserInfo"), new JsonObject().put("username","NewUser"))
			.onSuccess(reply -> {
				JsonObject response = (JsonObject) reply.body();
				System.out.println(response.encodePrettily());
				context.assertEquals(SoileCommUtils.SUCCESS,  response.getString(SoileCommUtils.RESULTFIELD));
				JsonObject userData = response.getJsonObject(SoileCommUtils.DATAFIELD);
				context.assertFalse(userData.containsKey("password"));
				context.assertEquals("Fullname", userData.getString("fullname"));
				context.assertEquals("New@mail.fi", userData.getString("email"));
				context.assertEquals(Roles.Researcher.toString(), userData.getString("role"));
				user1Async.complete();
				})
				.onFailure(err -> context.fail(err));
			Async user2Async = context.async(); 

			eb.request(getUsermanagerEventBusAddress("getUserInfo"), new JsonObject().put("username","NewUser2"))
			.onSuccess(reply -> {
				JsonObject response = (JsonObject) reply.body();
				context.assertEquals(SoileCommUtils.SUCCESS,  response.getString(SoileCommUtils.RESULTFIELD));
				JsonObject userData = response.getJsonObject(SoileCommUtils.DATAFIELD);

				context.assertFalse(userData.containsKey("password"));
				context.assertEquals("", userData.getString("fullname"));
				context.assertEquals("", userData.getString("email"));
				context.assertEquals(Roles.Participant.toString(), userData.getString("role"));
				user2Async.complete();
			})
			.onFailure(err -> context.fail(err));
			Async failAsync = context.async();
			eb.request(getUsermanagerEventBusAddress("getUserInfo"), new JsonObject().put("username","NewUser3"))
			.onSuccess(reply -> {
				context.fail("Should fail, since user does nto exist");
			})
			.onFailure(err -> {
				failAsync.complete();
			});
			
			testAsync.complete();
			
		})
		.onFailure(err -> context.fail(err));
		
	}
	@Test
	public void testlistUsers(TestContext context)
	{		
		Async testAsync = context.async();
		//create a few users
		createUser(vertx, "NewUser", "testPassword", "Fullname", "New@mail.fi", Roles.Researcher )
		.compose(Void -> createUser(vertx, "NewUser2", "testPassword2"))
		.compose(Void -> createUser(vertx, "NewUser3", "testPassword3"))
		.onSuccess(created -> {			
			Async list1Async = context.async(); 
			eb.request(getUsermanagerEventBusAddress("listUsers"), new JsonObject().put("namesOnly",true))
			.onSuccess(reply -> {
				JsonObject response = (JsonObject) reply.body();
				context.assertEquals(SoileCommUtils.SUCCESS,  response.getString(SoileCommUtils.RESULTFIELD));
				context.assertEquals(3,response.getJsonArray(SoileCommUtils.DATAFIELD).size());
				context.assertFalse(response.getJsonArray(SoileCommUtils.DATAFIELD).getJsonObject(0).containsKey("fullname"));
				context.assertTrue(response.getJsonArray(SoileCommUtils.DATAFIELD).getJsonObject(0).containsKey("username"));
				list1Async.complete();
				})
				.onFailure(err -> context.fail(err));
			Async list2Async = context.async(); 
			eb.request(getUsermanagerEventBusAddress("listUsers"), new JsonObject().put("namesOnly",false))
			.onSuccess(reply -> {
				JsonObject response = (JsonObject) reply.body();
				context.assertEquals(SoileCommUtils.SUCCESS,  response.getString(SoileCommUtils.RESULTFIELD));
				context.assertEquals(3,response.getJsonArray(SoileCommUtils.DATAFIELD).size());
				context.assertTrue(response.getJsonArray(SoileCommUtils.DATAFIELD).getJsonObject(0).containsKey("fullname"));
				context.assertTrue(response.getJsonArray(SoileCommUtils.DATAFIELD).getJsonObject(0).containsKey("username"));
				list2Async.complete();
				})
				.onFailure(err -> context.fail(err));
			
			testAsync.complete();
			
		})
		.onFailure(err -> context.fail(err));
		
	}
	
	@Test
	public void testgetAccessRequest(TestContext context)
	{		
		Async testAsync = context.async();
		JsonObject taskPermission = new JsonObject().put("type", "READ")
				 .put("target", "task");
		JsonObject expPermission = new JsonObject().put("type", "FULL")
				.put("target", "exp");
		JsonObject projPermission = new JsonObject().put("type", "FULL")
				.put("target", "proj");
		JsonObject instPermission = new JsonObject().put("type", "FULL")
				.put("target", "inst");
		JsonObject inst2Permission = new JsonObject().put("type", "FULL")
				.put("target", "inst2");
		JsonObject taskPermissionChange = new JsonObject().put("username", "NewUser")
				 .put("command", "add")
		     .put("permissionsProperties", new JsonObject().put("elementType", "task")
				  										.put("permissionsSettings", new JsonArray().add(taskPermission)));					

		JsonObject projectPermissionChange = new JsonObject().put("username", "NewUser")
				 .put("command", "add")
		     .put("permissionsProperties", new JsonObject().put("elementType", "project")
				  										.put("permissionsSettings", new JsonArray().add(projPermission)));
		
		JsonObject experimentPermissionChange = new JsonObject().put("username", "NewUser")
				 .put("command", "add")
		     .put("permissionsProperties", new JsonObject().put("elementType", "experiment")
				  										.put("permissionsSettings", new JsonArray().add(expPermission)));
		JsonObject instancePermissionChange = new JsonObject().put("username", "NewUser")
				 .put("command", "add")
		     .put("permissionsProperties", new JsonObject().put("elementType", "instance")
				  										.put("permissionsSettings", new JsonArray().add(instPermission).add(inst2Permission)));
		
		//create a few users
		createUser(vertx, "NewUser", "testPassword", "Fullname", "New@mail.fi", Roles.Researcher )
		.compose(Void -> eb.request(getUsermanagerEventBusAddress("permissionOrRoleChange"), taskPermissionChange))
		.compose(res -> eb.request(getUsermanagerEventBusAddress("permissionOrRoleChange"), projectPermissionChange))
		.compose(res -> eb.request(getUsermanagerEventBusAddress("permissionOrRoleChange"), experimentPermissionChange))
		.compose(res -> eb.request(getUsermanagerEventBusAddress("permissionOrRoleChange"), instancePermissionChange))		
		.onSuccess(created -> {
			eb.request(getUsermanagerEventBusAddress("getAccessRequest"), new JsonObject().put("username","NewUser") )
			.onSuccess(reply -> {
				JsonObject response = (JsonObject) reply.body();
				System.out.println(response.encodePrettily());
				context.assertEquals(SoileCommUtils.SUCCESS,  response.getString(SoileCommUtils.RESULTFIELD));
				JsonObject accessData = response.getJsonObject(SoileCommUtils.DATAFIELD);
				context.assertEquals("NewUser", accessData.getString("username"));
				context.assertEquals(Roles.Researcher.toString(), accessData.getString("role"));
				JsonObject permissions = accessData.getJsonObject("permissions");
				JsonArray taskPermissions = permissions.getJsonArray("tasks");
				JsonArray expPermissions = permissions.getJsonArray("experiments");
				JsonArray projPermissions = permissions.getJsonArray("projects");
				JsonArray instPermissions = permissions.getJsonArray("instances");
				context.assertEquals(2,instPermissions.size());
				context.assertEquals(1,taskPermissions.size());
				context.assertEquals("exp",expPermissions.getJsonObject(0).getString("target"));
				context.assertEquals("FULL",expPermissions.getJsonObject(0).getString("type"));							
				context.assertEquals("task",taskPermissions.getJsonObject(0).getString("target"));
				context.assertEquals("READ",taskPermissions.getJsonObject(0).getString("type"));
				testAsync.complete();					
			})
			.onFailure(err -> context.fail(err));
			
		})
		.onFailure(err -> context.fail(err));
	}
	/*			
	consumers.add(vertx.eventBus().consumer(getEventbusCommandString("listUsers"), this::listUsers));
	consumers.add(vertx.eventBus().consumer(getEventbusCommandString("getAccessRequest"), this::getUserAccessInfo));
	*/
	
	JsonObject createPermissionElement(String permission, PermissionType type)
	{
		return new JsonObject().put("type", type.toString()).put("target", permission);
	}
}
