package fi.abo.kogni.soile2.http_server;

import java.net.HttpURLConnection;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fi.abo.kogni.soile2.http_server.auth.SoileAuthorization.Roles;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.impl.StudyHandler;
import fi.abo.kogni.soile2.projecthandling.utils.ObjectGenerator;
import fi.abo.kogni.soile2.utils.SoileCommUtils;
import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.ReplyException;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;

public class SetupServer extends SoileServerVerticle {

	static final Logger LOGGER = LogManager.getLogger(SetupServer.class);
	private String dataFolder;			

	public SetupServer(String dataFolder) {
		super();
		this.dataFolder = dataFolder;
	}

	@Override
	public void start(Promise<Void> startPromise) throws Exception {		
		soileRouter = new SoileRouteBuilding();
		deployedVerticles = new ConcurrentLinkedQueue<>();	

		setupConfig() // As the very first step we need to set up the config so that it is available for all later steps.			
		.compose(this::setupFolders)
		.compose(this::deployVerticles) // deploy all necessary verticles for setup.
		.compose(this::createAdminUser)
		.compose(this::makeExampleProject)
		.compose(this::startExampleProjects)
		.onComplete(res ->
		{
			if(res.succeeded())
			{
				LOGGER.info("Server successfully set up, you can now start it.");
				startPromise.complete();				
			}
			else
			{
				res.cause().printStackTrace(System.out);
				LOGGER.error("Error setting up the server " + res.cause().getMessage());
				startPromise.fail(res.cause().getMessage());
			}
		});
	}	

	Future<Void> createAdminUser(Void unused)
	{
		Promise<Void> setupPromise = Promise.promise();
		vertx.fileSystem().readFile(SetupServer.class.getClassLoader().getResource("setup.json").getPath())
		.onSuccess(fileContents -> {
			JsonObject def = new JsonObject(fileContents);
			createAdminUser(def)
			.compose(this::makeUserAdmin)
			.onSuccess( res -> {
				setupPromise.complete();	
			})
			.onFailure(err -> setupPromise.fail(err));
		})
		.onFailure(err -> setupPromise.fail(err));

		return setupPromise.future();
	}

	Future<JsonObject> createAdminUser(JsonObject config)
	{

		Promise<JsonObject> accountCreatedPromise = Promise.promise();
		JsonObject AdduserCommand = new JsonObject().put("username", config.getString("adminuser"))
				.put("password",config.getString("adminpassword"));
		LOGGER.info("Setting up user");
		vertx.eventBus().request(SoileCommUtils.getEventBusCommand(SoileConfigLoader.USERMGR_CFG,"addUser"), AdduserCommand )
		.onSuccess(done -> {
			accountCreatedPromise.complete(config);
		})
		.onFailure(err -> {
			if(err instanceof ReplyException)
			{
				if(((ReplyException)err).failureCode() == HttpURLConnection.HTTP_CONFLICT)
				{
					LOGGER.info("User existed. Resetting password to selected password.");
					vertx.eventBus().request(SoileCommUtils.getEventBusCommand(SoileConfigLoader.USERMGR_CFG,"setPassword"), AdduserCommand )
					.onSuccess(done -> {
						accountCreatedPromise.complete(config);
					})		
					.onFailure(err2 -> accountCreatedPromise.fail(err2));
				}
				else
				{
					accountCreatedPromise.fail(err);
				}
			}
			else
			{
				accountCreatedPromise.fail(err);
			}

		});

		return accountCreatedPromise.future();
	}

	Future<JsonObject> makeUserAdmin(JsonObject config)
	{
		JsonObject makeUserAdminCommand = new JsonObject().put("username", config.getString("adminuser"))
				.put("role",Roles.Admin.toString())
				.put("command", "setCommand");

		LOGGER.info("Making user Admin");
		return vertx.eventBus().request(SoileCommUtils.getEventBusCommand(SoileConfigLoader.USERMGR_CFG,"permissionOrRoleChange"), makeUserAdminCommand )
				.map(config);
	}


	Future<JsonObject> makeExampleProject(Void unused)
	{				
		LOGGER.info("Creating Example Project");
		Promise<JsonObject> projectInfoPromise = Promise.promise();
		MongoClient testClient = MongoClient.createShared(vertx, SoileConfigLoader.getMongoCfg());
		testClient.findOne(SoileConfigLoader.getCollectionName("projectCollection"), new JsonObject().put("name", "ExampleProject"), null)
		.onSuccess(result -> {
			if(result != null)
			{
				long time = 0;
				String version = "";
				for(int i = 0; i < result.getJsonArray("versions").size(); i++)
				{
					JsonObject cversion = result.getJsonArray("versions").getJsonObject(i); 
					if(cversion.getLong("timestamp") > time)
					{
						time = cversion.getLong("timestamp");
						version = cversion.getString("version");
					}
				}
				projectInfoPromise.complete(new JsonObject().put("UUID", result.getString("_id")).put("version", version));

			}
			else
			{
				ObjectGenerator.createProject(MongoClient.create(vertx, SoileConfigLoader.getMongoCfg()), vertx, "ExampleProject", dataFolder)
				.onSuccess(res -> projectInfoPromise.complete(res))
				.onFailure(err -> projectInfoPromise.fail(err));
			}
		})
		.onFailure(err -> projectInfoPromise.fail(err));

		return projectInfoPromise.future(); 				
	}

	Future<Void> startExampleProjects(JsonObject projectInformation)
	{	
		Promise<Void> projectInstanceSetupPromise = Promise.promise();
		MongoClient testClient = MongoClient.createShared(vertx, SoileConfigLoader.getMongoCfg());
		testClient.findOne(SoileConfigLoader.getCollectionName("studyCollection"), new JsonObject().put("name", "Example Private Project"), null)
		.onSuccess(existing -> 
		{
			if(existing != null)
			{
				projectInstanceSetupPromise.complete();
				return;
			}

			LOGGER.info("Starting private Project");
			StudyHandler studyHandler = new StudyHandler(MongoClient.createShared(vertx, SoileConfigLoader.getMongoCfg()), vertx);		
			JsonObject privateProject = new JsonObject().put("private", true).put("name", "Example Private Project").put("shortcut","newShortcut");
			JsonObject projectData = new JsonObject().put("UUID", projectInformation.getValue("UUID")).put("version", projectInformation.getValue("version"));
			studyHandler.createStudy(privateProject.put("sourceProject",projectData))
			.compose(instance -> instance.activate())
			.onSuccess(active -> {
				LOGGER.info("Starting public Project");
				JsonObject publicProject = new JsonObject().put("private", false).put("name", "Example Public Project").put("shortcut","newPublicShortcut");			
				studyHandler.createStudy(publicProject.mergeIn(projectData))
				.compose(cinstance -> cinstance.activate())
				.onSuccess(active2 -> 
				{
					projectInstanceSetupPromise.complete();
				})
				.onFailure(err -> projectInstanceSetupPromise.fail(err));
			})
			.onFailure(err -> projectInstanceSetupPromise.fail(err));
		})
		.onFailure(err -> projectInstanceSetupPromise.fail(err));

		return projectInstanceSetupPromise.future();


	}


	public static void main(String[] args)
	{
		Vertx instance = Vertx.vertx();
		String dataFolder;
		if(args.length > 0)
		{
			dataFolder = args[0];
		}
		else
		{
			dataFolder = null;
		}

		instance.deployVerticle(new SetupServer(dataFolder))
		.onSuccess(res -> {			
			instance.close()
			.onSuccess(closed -> {				
			})
			.onFailure(err -> {							
				LOGGER.error("Error while shutting down vertx: ");
				LOGGER.error(err, err);
				System.exit(1);
			});
		})
		.onFailure(err -> {
			instance.close();
			LOGGER.error("Error while setting up server: ");
			LOGGER.error(err, err);
			System.exit(1);
		});
	}



}
