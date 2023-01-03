package fi.abo.kogni.soile2.http_server.auth;

import fi.abo.kogni.soile2.http_server.auth.SoileAuthorization.PermissionType;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.authorization.Authorization;
import io.vertx.ext.auth.authorization.PermissionBasedAuthorization;
import io.vertx.ext.auth.authorization.WildcardPermissionBasedAuthorization;

/**
 * This class provides static fsunctionality for Permission splitting and combining. No other place should manually handle permission settings.
 * @author Thomas Pfau
 *
 */
public class SoilePermissionProvider {

	
	public static JsonObject getAPIPermissionFromPermissionString(String permissionString)
	{
		int splitPos = permissionString.indexOf("$");
		String target = permissionString.substring(0,splitPos);
		String type = permissionString.substring(splitPos+1);	
		return new JsonObject().put("type", type).put("target", target);
	}
	
	public static String getTargetFromPermission(String permissionString)
	{		
		return permissionString.substring(0,permissionString.indexOf("$"));			
	}
	
	public static String getTypeFromPermission(String permissionString)
	{		
		return permissionString.substring(permissionString.indexOf("$")+1);			
	}
	
	public static String buildPermissionStringFromAPIPermission(JsonObject apiPermission)
	{
		return buildPermissionString(apiPermission.getString("target"), apiPermission.getString("type"));
	}
	
	public static String buildPermissionString(String targetID, PermissionType type)
	{
		return buildPermissionString(targetID, type.toString());
	}
	
	public static String buildPermissionString(String targetID, String type)
	{
		return targetID + "$" + type;
	}
	
	public static Authorization buildPermission(String targetID, PermissionType type)
	{
		return PermissionBasedAuthorization.create(buildPermissionString(targetID, type));
	}
	
	public static Authorization buildPermission(String targetID, String type)
	{
		return PermissionBasedAuthorization.create(buildPermissionString(targetID, type));
	}	
	public static Authorization getWildCardPermission(String targetID)
	{
		return WildcardPermissionBasedAuthorization.create(buildPermissionString(targetID, "*"));
	}	
}
