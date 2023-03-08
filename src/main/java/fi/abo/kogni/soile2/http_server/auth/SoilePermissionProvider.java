package fi.abo.kogni.soile2.http_server.auth;

import fi.abo.kogni.soile2.http_server.auth.SoileAuthorization.PermissionType;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.authorization.Authorization;
import io.vertx.ext.auth.authorization.PermissionBasedAuthorization;

/**
 * This class provides static functionality for Permission splitting and combining. 
 * No other place should manually handle permission settings.
 * @author Thomas Pfau
 *
 */
public class SoilePermissionProvider {

	/**
	 * Get the API permission object for the given permission String
	 * @param permissionString The permission string (e.g. from the database)
	 * @return a {@link JsonObject} with "type" and "target" fields 
	 */
	public static JsonObject getAPIPermissionFromPermissionString(String permissionString)
	{		
		String target = getTargetFromPermission(permissionString);
		String type = getTypeFromPermission(permissionString);	
		return new JsonObject().put("type", type).put("target", target);
	}
	
	/**
	 * Get the target from the given permission. 
	 * @param permissionString The permision string to obain the target from.
	 * @return
	 */
	public static String getTargetFromPermission(String permissionString)
	{		
		return permissionString.substring(0,permissionString.indexOf("$"));			
	}
	
	/**
	 * Get the type of permission from the given permission. 
	 * @param permissionString The permission string to obtain the type from.
	 * @return
	 */
	public static String getTypeFromPermission(String permissionString)
	{		
		return permissionString.substring(permissionString.indexOf("$")+1);			
	}
	
	/**
	 * Build a permission string from the api Permission {@link JsonObject} containing "type" and "target";
	 * @param apiPermission The {@link JsonObject} for the api permission
	 * @return A JsonArray with all permissions this Api object provides. This is necessary since it can provide multiple permissions if the permission type is "ALL"
	 */
	public static JsonArray buildPermissionStringFromAPIPermission(JsonObject apiPermission)
	{
		PermissionType type = PermissionType.valueOf(apiPermission.getString("type"));
		if(type == PermissionType.ALL)
		{
			JsonArray result = new JsonArray();
			for(PermissionType p : PermissionType.values())
			{
				if(p.equals(PermissionType.ALL))
				{
					continue;
				}
				result.add(buildPermissionString(apiPermission.getString("target"), apiPermission.getString("type")));
			}
			return result;
		}
		else
		{
			return new JsonArray().add(buildPermissionString(apiPermission.getString("target"), type.toString()));
		}
	}
	
	/**
	 * Build a permission String from targetID and {@link PermissionType};
	 * @param targetID
	 * @param type
	 * @return
	 */
	public static String buildPermissionString(String targetID, PermissionType type)
	{
		return buildPermissionString(targetID, type.toString());
	}	
	
	/**
	 * Build a permission String from targetID and Type as String;
	 * @param targetID
	 * @param type
	 * @return
	 */
	public static String buildPermissionString(String targetID, String type)
	{
		return targetID + "$" + type;
	}
	
	/**
	 * Build an {@link Authorization} based on targetID and Type
	 * @param targetID
	 * @param type
	 * @return
	 */
	public static Authorization buildPermission(String targetID, PermissionType type)
	{
		return PermissionBasedAuthorization.create(buildPermissionString(targetID, type));
	}
	
	/**
	 * Build an {@link Authorization} based on targetID and Type
	 * @param targetID
	 * @param type
	 * @return
	 */
	public static Authorization buildPermission(String targetID, String type)
	{
		return PermissionBasedAuthorization.create(buildPermissionString(targetID, type));
	}	

}
