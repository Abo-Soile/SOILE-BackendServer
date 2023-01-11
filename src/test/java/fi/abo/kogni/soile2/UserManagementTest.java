package fi.abo.kogni.soile2;


import fi.abo.kogni.soile2.http_server.userManagement.SoileUserManager;
import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.unit.TestContext;

public interface UserManagementTest {

	
	public default Future<String> createUser(JsonObject userdata, TestContext context, SoileUserManager uManager)
	{			
		String username = userdata.getString(SoileConfigLoader.getUserdbField("usernameField"));
		String password = userdata.getString(SoileConfigLoader.getUserdbField("passwordField"));
		return uManager.createUser(username, password);
	}
	
	public default SoileUserManager createManager(Vertx vertx)
	{
		return new SoileUserManager(MongoClient.createShared(vertx, SoileConfigLoader.getDbCfg()));
	}
}
