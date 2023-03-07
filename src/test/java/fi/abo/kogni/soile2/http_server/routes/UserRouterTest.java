package fi.abo.kogni.soile2.http_server.routes;

import org.junit.Test;

import fi.abo.kogni.soile2.http_server.SoileWebTest;
import fi.abo.kogni.soile2.http_server.UserVerticleTest;
import fi.abo.kogni.soile2.http_server.auth.SoileAuthorization.PermissionType;
import fi.abo.kogni.soile2.http_server.auth.SoileAuthorization.Roles;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.web.handler.HttpException;

public class UserRouterTest extends SoileWebTest implements UserVerticleTest{


	@Test
	public void permissionOrRoleRequestTest(TestContext context)
	{
		System.out.println("--------------------  Testing Web Role and Permission retrieval ----------------------");

		Async testAsync = context.async();
		createUserAndAuthedSession("Admin", "testpw", Roles.Admin)
		.onSuccess(adminsession -> {
			createUserAndAuthedSession("TestUser", "testpw", Roles.Researcher)
			.onSuccess(usersession -> {
				WebObjectCreator.createExperiment(usersession, "TestExperiment2")
				.onSuccess(experimentInfo -> {
					Async invalidAsync = context.async();
					GET(usersession, "/user/getaccess",new JsonObject().put("username", "Admin"),null)
					.onSuccess(res -> context.fail("Should not be allowed"))
					.onFailure(err -> {
						context.assertEquals(403, ((HttpException)err).getStatusCode());
						invalidAsync.complete();
					});
					Async adminCheck = context.async();
					GET(adminsession, "/user/getaccess",new JsonObject().put("username", "TestUser"),null)
					.onSuccess(response -> {
						JsonObject data =  response.bodyAsJsonObject();
						context.assertEquals(Roles.Researcher.toString(), data.getString("role"));
						JsonObject permissions = data.getJsonObject("permissions");
						context.assertEquals(0, permissions.getJsonArray("instances").size());
						context.assertEquals(0, permissions.getJsonArray("projects").size());
						context.assertEquals(experimentInfo.getString("UUID"), permissions.getJsonArray("experiments").getJsonObject(0).getString("target"));
						context.assertEquals("FULL", permissions.getJsonArray("experiments").getJsonObject(0).getString("type"));
						context.assertEquals(2, permissions.getJsonArray("tasks").size());
						adminCheck.complete();
					})
					.onFailure(err -> context.fail(err));
					Async userCheck = context.async();
					GET(usersession, "/user/getaccess",new JsonObject().put("username", "TestUser"),null)
					.onSuccess(response -> {
						JsonObject data =  response.bodyAsJsonObject();
						context.assertEquals(Roles.Researcher.toString(), data.getString("role"));
						JsonObject permissions = data.getJsonObject("permissions");
						context.assertEquals(0, permissions.getJsonArray("instances").size());
						context.assertEquals(0, permissions.getJsonArray("projects").size());
						context.assertEquals(experimentInfo.getString("UUID"), permissions.getJsonArray("experiments").getJsonObject(0).getString("target"));
						context.assertEquals("FULL", permissions.getJsonArray("experiments").getJsonObject(0).getString("type"));
						context.assertEquals(2, permissions.getJsonArray("tasks").size());
						userCheck.complete();
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
public void permissionChangeTest(TestContext context)
{
	System.out.println("--------------------  Testing Web Role and Permission Changes  ----------------------");
	JsonArray permissionSettings = new JsonArray();
	JsonObject permissionChange = new JsonObject().put("username","NonAdmin")
			.put("command", "add")
			.put("permissionsProperties", new JsonObject().put("elementType", "EXPERIMENT")
					.put("permissionSettings", permissionSettings));

	JsonObject permissionChangeOther = new JsonObject().put("username","OtherUser")
			.put("command", "add")
			.put("permissionsProperties", new JsonObject().put("elementType", "EXPERIMENT")
					.put("permissionSettings", permissionSettings));
	Async testAsync = context.async();
	createUser(vertx, "OtherUser", "otherpw")
	.onSuccess(created -> {
		createUserAndAuthedSession("Admin", "testpw", Roles.Admin)
		.onSuccess(adminsession -> {
			WebObjectCreator.createExperiment(adminsession, "TestExperiment2")
			.onSuccess(experimentInfo -> {
				createUserAndAuthedSession("NonAdmin", "testpw", Roles.Researcher)
				.onSuccess(userSession -> {
					Async failedAccessAsync = context.async();
					GET(userSession,"/experiment/" + experimentInfo.getString("UUID") + "/" + experimentInfo.getString("version"), null, null )
					.onSuccess(err -> context.fail("Is private not accessible"))
					.onFailure(rejected -> {						
						context.assertEquals(403, ((HttpException)rejected).getStatusCode());
						failedAccessAsync.complete();
						permissionSettings.add(new JsonObject().put("type", PermissionType.FULL.toString())
								.put("target", experimentInfo.getString("UUID")));

						POST(userSession, "/user/setpermissions",null,permissionChangeOther)
						.onSuccess(fail -> context.fail("Should not be possible"))
						.onFailure(cont -> {
							context.assertEquals(403, ((HttpException)cont).getStatusCode());
							POST(adminsession, "/user/setpermissions",null,permissionChange)
							.onSuccess(permissionsSet -> {
								// now we should have access
								GET(userSession,"/experiment/" + experimentInfo.getString("UUID") + "/" + experimentInfo.getString("version"), null, null )
								.onSuccess(objInfo -> {
									context.assertEquals(experimentInfo, objInfo.bodyAsJsonObject());
									POST(userSession, "/user/setpermissions",null,permissionChangeOther)
									.onSuccess(res -> {
										context.assertEquals(200, res.statusCode());
										testAsync.complete();
									})
									.onFailure(err -> context.fail(err));
								})
								.onFailure(err -> context.fail(err));

							})
							.onFailure(err -> context.fail(err));
						});							
					});
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
public void listUsersTest(TestContext context)
{		
	System.out.println("--------------------  Testing Web List Users  ----------------------");
	Async sessionAsync = context.async();
	createUserAndAuthedSession("Admin", "testPassword", Roles.Admin)
	.onSuccess(authedSession -> {
		createUser(vertx, "User1", "pw1", Roles.Researcher)
		.compose((Void) -> {return createUser(vertx, "User2", "pw2",Roles.Participant);})
		.onSuccess(usersCreated -> {
			Async defaultListAsync = context.async(); 
			GET(authedSession,"/user/list", null, null)
			.onSuccess(response -> {
				JsonArray users = response.bodyAsJsonArray();
				context.assertEquals(3, users.size());
				for(int i = 0; i < users.size(); ++i)
				{
					JsonObject currentUser = users.getJsonObject(i);
					String username = currentUser.getString("username");
					switch(username)
					{
					case "Admin" : 
						context.assertEquals(Roles.Admin.toString(), currentUser.getString("role"));
						break;
					case "User1" : 
						context.assertEquals(Roles.Researcher.toString(), currentUser.getString("role"));
						break;
					case "User2" : 
						context.assertEquals(Roles.Participant.toString(), currentUser.getString("role"));
						break;
					default:
						context.fail("User " + username + " should not exist");						
					}
				}
				defaultListAsync.complete();
			});
			Async searchList = context.async();
			GET(authedSession,"/user/list", new JsonObject().put("searchString", "User"), null)
			.onSuccess(response -> {
				JsonArray users = response.bodyAsJsonArray();
				context.assertEquals(2, users.size());
				searchList.complete();
			});
			Async skipAndLimit = context.async();
			GET(authedSession,"/user/list", new JsonObject().put("limit", 2), null)
			.onSuccess(response -> {
				JsonArray users = response.bodyAsJsonArray();
				context.assertEquals(2, users.size());
				GET(authedSession,"/user/list", new JsonObject().put("skip",2).put("limit", 2), null)
				.onSuccess(response2 -> {
					JsonArray users2 = response2.bodyAsJsonArray();
					context.assertEquals(1, users2.size());
					JsonArray expectedUsers = new JsonArray().add("Admin").add("User1").add("User2");
					for(Object o : users)
					{
						expectedUsers.remove(((JsonObject)o).getString("username"));
					}
					for(Object o : users2)
					{
						expectedUsers.remove(((JsonObject)o).getString("username"));
					}
					context.assertEquals(0, expectedUsers.size());
					skipAndLimit.complete();
				})
				.onFailure(err -> context.fail(err));	
			})
			.onFailure(err -> context.fail(err));	
			// and now, test for a non admin user, who will only get names.
			Async secondSessionAsync = context.async();
			createAuthedSession("User1", "pw1")
			.onSuccess(nonAdminSession -> {
				Async defaultListAsync2 = context.async(); 
				GET(nonAdminSession,"/user/list", null, null)
				.onSuccess(response -> {
					JsonArray users = response.bodyAsJsonArray();
					context.assertEquals(3, users.size());						
					JsonArray expectedUsers = new JsonArray().add("Admin").add("User1").add("User2");
					for(int i = 0; i < users.size(); ++i)
					{
						JsonObject currentUser = users.getJsonObject(i);
						String username = currentUser.getString("username");							
						context.assertEquals(1,currentUser.fieldNames().size());
						expectedUsers.remove(username);															
					}
					context.assertEquals(0, expectedUsers.size());
					defaultListAsync2.complete();
				});
				Async searchList2 = context.async();
				GET(nonAdminSession,"/user/list", new JsonObject().put("searchString", "User"), null)
				.onSuccess(response -> {
					JsonArray users = response.bodyAsJsonArray();
					context.assertEquals(2, users.size());
					searchList2.complete();
				});
				Async skipAndLimit2 = context.async();
				GET(nonAdminSession,"/user/list", new JsonObject().put("limit", 2), null)
				.onSuccess(response -> {
					JsonArray users = response.bodyAsJsonArray();
					context.assertEquals(2, users.size());
					GET(nonAdminSession,"/user/list", new JsonObject().put("skip",2).put("limit", 2), null)
					.onSuccess(response2 -> {
						JsonArray users2 = response2.bodyAsJsonArray();
						context.assertEquals(1, users2.size());
						JsonArray expectedUsers = new JsonArray().add("Admin").add("User1").add("User2");
						for(Object o : users)
						{
							expectedUsers.remove(((JsonObject)o).getString("username"));
						}
						for(Object o : users2)
						{
							expectedUsers.remove(((JsonObject)o).getString("username"));
						}
						context.assertEquals(0, expectedUsers.size());
						skipAndLimit2.complete();
					})
					.onFailure(err -> context.fail(err));	
				})
				.onFailure(err -> context.fail(err));
				secondSessionAsync.complete();
			})
			.onFailure(err -> context.fail(err));	
			sessionAsync.complete();
		})
		.onFailure(err -> context.fail(err));	

	})
	.onFailure(err -> context.fail(err));
}

@Test
public void registerUserTest(TestContext context)
{
	System.out.println("--------------------  Testing Web Register User  ----------------------");
	JsonObject userData = new JsonObject().put("username", "TestUser")
			.put("fullname", "Some Name")
			.put("email", "me@you.fi")
			.put("password", "test");
	Async testAsync = context.async();
	POST(webclient, "/register",null,userData )
	.onSuccess(response -> {
		context.assertEquals(201, response.statusCode());
		authenticateSession(createSession(), "TestUser", "test")
		.onSuccess(websession -> {
			POST(webclient, "/register",null,userData )
			.onSuccess(w00t -> {
				context.fail("This should not be allowed");
			})
			.onFailure(err -> {
				if(err instanceof HttpException)
				{
					context.assertEquals(409,((HttpException)err).getStatusCode());
					POST(webclient, "/register",null,userData.put("username", "AnotherUser") )
					.onSuccess(w00t -> {
						context.fail("This should not be allowed");
					})
					.onFailure(err2 -> {
						if(err2 instanceof HttpException)
						{
							context.assertEquals(409,((HttpException)err2).getStatusCode());
							testAsync.complete();
						}
						else
						{
							context.fail(err2);
						}
					});
				}
				else
				{
					context.fail(err);
				}
			});
		})
		.onFailure(err -> context.fail(err));
	})
	.onFailure(err -> context.fail(err));;
}

@Test
// TODO: Test that files also get deleted for a user.
public void removeUserTest(TestContext context)
{
	System.out.println("--------------------  Testing Web Remove User  ----------------------");
	JsonObject userData = new JsonObject().put("username", "TestUser")
			.put("fullname", "Some Name")
			.put("email", "me@you.fi")
			.put("password", "test");		
	JsonObject deletionCommand = new JsonObject().put("username", "TestUser");
	Async testAsync = context.async();
	POST(webclient, "/register",null,userData )
	.onSuccess(created -> {
		createUserAndAuthedSession("Admin", "password", Roles.Admin)
		.onSuccess(adminsession -> {
			createUserAndAuthedSession("NonAdmin", "password", Roles.Researcher)
			.onSuccess(nonAdminsession -> {
				POST(nonAdminsession,"/user/delete",null,deletionCommand)
				.onSuccess(err -> {
					context.fail("This should not be possible");
				})
				.onFailure(err -> {
					context.assertEquals(403, ((HttpException)err).getStatusCode());
					// test self deletion.
					createAuthedSession("TestUser", "test")
					.onSuccess(authedFitUser -> {
						POST(authedFitUser,"/user/delete",null,deletionCommand)
						.onSuccess(succ -> {
							context.assertEquals(202,succ.statusCode());
							POST(adminsession,"/user/delete",null,new JsonObject().put("username", "Admin"))
							.onSuccess(adminDel -> {
								context.assertEquals(202,adminDel.statusCode());
								GET(nonAdminsession,"/user/list",null,null)
								.onSuccess(response -> {
									context.assertEquals(1,response.bodyAsJsonArray().size());
									testAsync.complete();	
								})
								.onFailure(err2 -> context.fail(err));

							})
							.onFailure(err2 -> context.fail(err));							

						})
						.onFailure(err2 -> context.fail(err));							
					})
					.onFailure(err2 -> context.fail(err));
				});
			})
			.onFailure(err -> context.fail(err));

		})
		.onFailure(err -> context.fail(err));
	})
	.onFailure(err -> context.fail(err));
}


@Test
public void setUserInfoTest(TestContext context)
{
	System.out.println("--------------------  Testing Web Setting User Info  ----------------------");
	JsonObject invaliduserData = new JsonObject().put("username", "TestUser")
			.put("fullname", "Some Name")
			.put("email", "me@you.fi")
			.put("password", "test");
	JsonObject validuserData = new JsonObject().put("username", "NonAdmin")
			.put("fullname", "Some Other Name")
			.put("email", "me@you2.fi")
			.put("password", "test");

	JsonObject invalidAdminData = new JsonObject().put("username", "Admin")
			.put("fullname", "Some Admin Name")
			.put("email", "me@you2.fi")
			.put("password", "test");


	JsonObject validAdminData = new JsonObject().put("username", "Admin")
			.put("fullname", "Some Admin Name")
			.put("email", "admin@you2.fi")
			.put("password", "test");
	Async testAsync = context.async();
	createUserAndAuthedSession("Admin", "password", Roles.Admin)
	.onSuccess(adminsession -> {
		createUserAndAuthedSession("NonAdmin", "password", Roles.Researcher)
		.onSuccess(nonAdminsession -> {
			POST(nonAdminsession,"/user/info",null,invaliduserData)
			.onSuccess(err -> {
				context.fail("This should not be possible");
			})
			.onFailure(err -> {
				// non admin will just be denied this request.
				context.assertEquals(403, ((HttpException)err).getStatusCode());
				Async testNonExistentUser = context.async();

				POST(adminsession,"/user/info",null,invaliduserData)
				.onSuccess(invalid -> {
					context.fail("This should not be possible");

				})
				.onFailure(res -> {
					context.assertEquals(410, ((HttpException)res).getStatusCode());
					testNonExistentUser.complete();
				});
				Async firstChecks = context.async();
				// Self Setting test
				POST(nonAdminsession,"/user/info",null,validuserData)
				.onSuccess(succ -> {
					context.assertEquals(200, succ.statusCode());
					JsonObject updatedData = succ.bodyAsJsonObject();
					context.assertEquals(validuserData.getValue("fullname"), updatedData.getValue("fullname"));
					context.assertEquals(validuserData.getValue("email"), updatedData.getValue("email"));
					// now we test a reject for same email
					POST(adminsession,"/user/info",null,invalidAdminData)
					.onSuccess(oops -> {
						context.fail("This should not be possible, same email.");
					})
					.onFailure(failed -> {
						context.assertEquals(409, ((HttpException)failed).getStatusCode());		
						firstChecks.complete();
					});
				})
				.onFailure(err2 -> context.fail(err));
				Async otherChecks = context.async();
				// Test admin changing itself.
				POST(adminsession,"/user/info",null,validAdminData)
				.onSuccess(succ -> {
					context.assertEquals(200, succ.statusCode());
					JsonObject updatedData = succ.bodyAsJsonObject();
					context.assertEquals(validAdminData.getValue("fullname"), updatedData.getValue("fullname"));
					context.assertEquals(validAdminData.getValue("email"), updatedData.getValue("email"));
					// now we test a reject for same email
					otherChecks.complete();
				})
				.onFailure(err2 -> context.fail(err));
				testAsync.complete();
			});				
		})
		.onFailure(err2 -> context.fail(err2));
	})
	.onFailure(err -> context.fail(err));
}

@Test
public void setUserInfoWithRoleTest(TestContext context)
{
	System.out.println("--------------------  Testing Web Seting User Info and Role  ----------------------");
	JsonObject validUserData = new JsonObject().put("username", "NonAdmin")
			.put("fullname", "Some Other Name")
			.put("email", "me@you2.fi")
			.put("password", "test")
			.put("role", Roles.Participant.toString());


	JsonObject invalidUserData = new JsonObject().put("username", "NonAdmin")
			.put("fullname", "Some Admin Name")
			.put("email", "me@you2.fi")
			.put("password", "test")
			.put("role", "Nonsense");
	Async testAsync = context.async();
	createUserAndAuthedSession("Admin", "password", Roles.Admin)
	.onSuccess(adminsession -> {
		createUserAndAuthedSession("NonAdmin", "password", Roles.Researcher)
		.onSuccess(nonAdminsession -> {
			Async nonAdminTest = context.async();
			POST(nonAdminsession,"/user/info",null,validUserData)
			.onSuccess(err -> {
				context.fail("This should not be possible");
			})
			.onFailure(err -> {
				// non admin will just be denied this request.
				context.assertEquals(403, ((HttpException)err).getStatusCode());
				nonAdminTest.complete();
			});
			Async AdminTest = context.async();
			POST(adminsession,"/user/info",null,invalidUserData)
			.onSuccess(err -> {
				context.fail("This should not be possible");
			})
			.onFailure(failed -> {
				// non admin will just be denied this request.
				context.assertEquals(400, ((HttpException)failed).getStatusCode());
				POST(adminsession,"/user/info",null,validUserData)
				.onSuccess(posted -> {
					GET(adminsession,"/user/info",new JsonObject().put("username", "NonAdmin"),null)
					.onSuccess(res -> {
						context.assertEquals(res.bodyAsJsonObject().getString("role"), Roles.Participant.toString());
						AdminTest.complete();	
					})
					.onFailure(err -> context.fail(err));
				})
				.onFailure(err -> context.fail(err));

			});
			testAsync.complete();
		})
		.onFailure(err2 -> context.fail(err2));
	})
	.onFailure(err -> context.fail(err));
}


@Test
public void getUserInfoTest(TestContext context)
{
	System.out.println("--------------------  Testing Web Get User Info  ----------------------");
	JsonObject validuserData = new JsonObject().put("username", "NonAdmin")
			.put("fullname", "Some Other Name")
			.put("email", "me@you2.fi")
			.put("password", "test");

	JsonObject userrequest = new JsonObject().put("username", "NonAdmin");


	Async testAsync = context.async();
	createUserAndAuthedSession("Admin", "password", Roles.Admin)
	.onSuccess(adminsession -> {
		createUserAndAuthedSession("NonAdmin", "password", Roles.Researcher)
		.onSuccess(nonAdminsession -> {
			Async testOriginalData = context.async();
			Async testnewData = context.async();
			GET(adminsession,"/user/info",userrequest,null)
			.onSuccess(response -> {
				JsonObject res = response.bodyAsJsonObject();
				context.assertEquals("", res.getValue("email"));
				context.assertEquals("", res.getValue("fullname"));
				context.assertNull(res.getValue("password"));				
				GET(nonAdminsession,"/user/info",userrequest,null)
				.onSuccess(userRes -> {
					JsonObject res2 = userRes.bodyAsJsonObject();
					context.assertEquals("", res2.getValue("email"));
					context.assertEquals("", res2.getValue("fullname"));
					context.assertNull(res2.getValue("password"));
					testOriginalData.complete();
					POST(adminsession,"/user/info",null,validuserData)
					.onSuccess(success -> {
						GET(adminsession,"/user/info",userrequest,null)
						.onSuccess(adminUserData -> {
							JsonObject res3 = adminUserData.bodyAsJsonObject();
							context.assertEquals(validuserData.getValue("email"), res3.getValue("email"));
							context.assertEquals(validuserData.getValue("fullname"), res3.getValue("fullname"));
							context.assertNull(res3.getValue("password"));
							GET(nonAdminsession,"/user/info",userrequest,null)
							.onSuccess(userRes2 -> {
								JsonObject res4 = userRes2.bodyAsJsonObject();
								context.assertEquals(validuserData.getValue("email"), res4.getValue("email"));
								context.assertEquals(validuserData.getValue("fullname"), res4.getValue("fullname"));									
								context.assertNull(res4.getValue("password"));
								testnewData.complete();
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
			testAsync.complete();

		})
		.onFailure(err2 -> context.fail(err2));
	})
	.onFailure(err -> context.fail(err));
}



@Test
public void setPasswordTest(TestContext context)
{		
	System.out.println("--------------------  Testing Web Set Password  ----------------------");
	Async user1Async = context.async();
	createUserAndAuthedSession("Admin", "password", Roles.Admin)
	.onSuccess(adminsession -> {
		createUser(vertx, "TestUser", "testpw")
		.onSuccess(userCreated -> {
			Async changeSuccessfullAsync = context.async();
			POST(adminsession, "/user/setpassword", null, new JsonObject().put("username", "TestUser").put("password", "newPassword"))
			.onSuccess(passChanged -> {
				context.assertEquals(200, passChanged.statusCode());
				authenticateSession(createSession(), "TestUser", "testpw")
				.onSuccess(res -> context.fail("Should not work, PW wrong"))
				.onFailure(fail -> {
					authenticateSession(createSession(), "TestUser", "newPassword")
					.onSuccess(res -> 
					{							
						changeSuccessfullAsync.complete();
					})
					.onFailure(err -> context.fail(err));
				});					

			})
			.onFailure(err -> context.fail(err));
			Async userDoesntExistAsync = context.async();
			POST(adminsession, "/user/setpassword", null, new JsonObject().put("username", "NonExistentUser").put("password", "newPassword"))
			.onSuccess(passChanged -> {
				context.fail("Request should have failed.");					
			})
			.onFailure(err -> {
				context.assertEquals(410, ((HttpException)err).getStatusCode());
				userDoesntExistAsync.complete();
			});
			user1Async.complete();
		})
		.onFailure(err -> context.fail(err));
		Async userChangeAsync = context.async();
		createUserAndAuthedSession("TestUser2", "testpw", Roles.Participant)
		.onSuccess(userSession -> {
			Async invalidRequest = context.async();
			POST(userSession, "/user/setpassword", null, new JsonObject().put("username", "Admin").put("password", "newPassword"))
			.onSuccess(passChanged -> {
				context.fail("Should not be allowed");
			})
			.onFailure(err -> {
				context.assertEquals(403, ((HttpException)err).getStatusCode());
				invalidRequest.complete();
			});				
			Async validRequests = context.async(); 
			POST(userSession, "/user/setpassword", null, new JsonObject().put("username", "TestUser2").put("password", "newPassword"))
			.onSuccess(passChanged -> {
				Async invalidAuth = context.async();
				authenticateSession(createSession(), "TestUser2", "testpw")
				.onSuccess(err -> context.fail("Should not be possible"))
				.onFailure(err ->
				{
					context.assertEquals(401, ((HttpException)err).getStatusCode());
					invalidAuth .complete();
				});
				Async validAuth = context.async();
				authenticateSession(createSession(), "TestUser2", "newPassword")
				.onFailure(err -> context.fail(err))
				.onSuccess(authedSession ->
				{						
					validAuth.complete();
				});
				validRequests.complete();
			});	
			userChangeAsync.complete();
		})
		.onFailure(err -> context.fail(err));

	})
	.onFailure(err -> context.fail(err));
}


@Test
public void setRoleTest(TestContext context)
{		
	System.out.println("--------------------  Testing Web Set Role   ----------------------");
	JsonObject userrequest = new JsonObject().put("role", Roles.Participant.toString()).put("username", "TestUser");
	JsonObject invalidrolerequest = new JsonObject().put("username", "TestUser").put("role", "Joker");
	JsonObject invaliduserrequest = new JsonObject().put("username", "NonAdmin").put("role", "Researcher");

	Async user1Async = context.async();
	createUserAndAuthedSession("Admin", "password", Roles.Admin)
	.onSuccess(adminsession -> {
		createUser(vertx, "TestUser", "testpw", Roles.Researcher)			
		.onSuccess(userCreated -> {
			Async invalidTest = context.async();
			POST(adminsession, "/user/setrole",null,invaliduserrequest)
			.onSuccess(err -> context.fail("Should not be possible - user doesn't exist"))
			.onFailure(err -> {
				context.assertEquals(410, ((HttpException)err).getStatusCode());
				invalidTest.complete();
			});
			Async invalidTest2 = context.async();
			POST(adminsession, "/user/setrole",null,invalidrolerequest)
			.onSuccess(err -> context.fail("Should not be possible - user doesn't exist"))
			.onFailure(err -> {
				context.assertEquals(400, ((HttpException)err).getStatusCode());
				invalidTest2.complete();
			});
			Async validAsync = context.async();
			POST(adminsession, "/user/setrole",null,userrequest)
			.onSuccess(roleSet-> 
			{

				GET(adminsession,"/user/info", new JsonObject().put("username", "TestUser"), null)
				.onSuccess(res -> {
					JsonObject result = res.bodyAsJsonObject();
					context.assertEquals(Roles.Participant.toString(),result.getValue("role"));
					validAsync.complete();
				})
				.onFailure(err -> context.fail(err));	
			})
			.onFailure(err -> context.fail(err));
			Async userTestsAsync = context.async();
			createAuthedSession("TestUser", "testpw")
			.onSuccess(usersession -> {
				POST(usersession, "/user/setrole",null,userrequest)
				.onSuccess(roleSet-> 
				{
					context.fail("Should not be allowed");
				})
				.onFailure(err -> {						
					context.assertEquals(403, ((HttpException)err).getStatusCode());
					userTestsAsync.complete();
				});

			})
			.onFailure(err -> context.fail(err));	
			user1Async.complete();
		})
		.onFailure(err -> context.fail(err));			
	})
	.onFailure(err -> context.fail(err));
}



}
