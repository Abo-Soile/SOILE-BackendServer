package fi.abo.kogni.soile2.http_server.auth;

import java.util.LinkedList;
import java.util.List;

import fi.abo.kogni.soile2.http_server.auth.SoileAuthorization.PermissionType;
import fi.abo.kogni.soile2.http_server.userManagement.SoileUserManager.PermissionChange;
import fi.abo.kogni.soile2.http_server.userManagement.exceptions.CannotUpdateMultipleException;
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
		return buildPermissionString(targetID, type, false);
	}

	
	
	/**
	 * Build a permission String from targetID and Type as String;
	 * @param targetID
	 * @param type
	 * @return
	 */
	public static String buildPermissionString(String targetID, String type, boolean regexp)
	{
		if(regexp)
		{
			return targetID + "\\$" + type;
		}
		else
		{
			return targetID + "$" + type;
		}
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

	/**
	 * Build a permission String from targetID and {@link PermissionType};
	 * @param targetID
	 * @param minimalPermission the minimal permission needed to match this query
	 * @return
	 */
	public static String buildPermissionQuery(String targetID, PermissionType minimalPermission)
	{
		List<String> permissions = new LinkedList<>();  		
		
		switch(minimalPermission)
		{
			case EXECUTE : permissions.add(PermissionType.EXECUTE.toString());
			case READ : permissions.add( PermissionType.READ.toString() );
			case READ_WRITE : permissions.add(PermissionType.READ_WRITE.toString() );
			case FULL : permissions.add(PermissionType.FULL.toString() ); break; // we only need to break here, as we want all "higher" level cases to be included.
			case ALL: permissions.add(".+");
		}
				
		return buildPermissionString(targetID, "(" + String.join("|", permissions) + ")", true);
	}
	
	/**
	 * Get the highest permission from a list of permission types, ONLY READ/WRITE/FULL are supported by this function,
	 * as execute is a special permission.
	 * @param permissions the list of permissions
	 * @return
	 */
	public static PermissionType getMaxPermission(List<PermissionType> permissions)
	{		  	
		if(permissions.contains(PermissionType.FULL ))
		{
			return PermissionType.FULL; 
		}
		if(permissions.contains(PermissionType.READ_WRITE ))
		{
			return PermissionType.READ_WRITE; 
		}
		if(permissions.contains(PermissionType.READ ))
		{
			return PermissionType.READ; 
		}			
		return null;
	}
	
	/**
	 * Get an update Object for the given permission change.
	 * @param targetID
	 * @param change
	 * @param newPermission
	 * @param targetField
	 * @return
	 */
	public static JsonObject getPermissionUpdate(String targetID, PermissionChange change, PermissionType newPermission, String targetField)
	{
		JsonObject update = new JsonObject();
		String alteredPermission = buildPermissionString(targetID, newPermission);		
		switch(change)
		{
			case Add: update.put("$addToSet", new JsonObject().put(targetField, alteredPermission));break;
			case Remove: update.put("$pull", new JsonObject().put(targetField, alteredPermission));break;
			case Set: update.put("$set", new JsonObject().put(targetField, new JsonArray().add(alteredPermission)));break; // needs to set an array as it is setting an individual permission.
			case Update: update.put("$pullAll", new JsonObject().put(targetField, getOtherAccess(targetID, newPermission)))
							   .put("$addToSet", new JsonObject().put(targetField, alteredPermission));
							   break;			
		}
		return update;
	}	
	
	
	/**
	 * Change multiple permissions at once, 
	 * @param change - the type of change, i.e. whether add/remove or replace. Change will cause an error here, as it is a more complex operation that isn't implemented for multiple changes.
	 * @param changedPermissions - All permissions (ID$Perm format) that are changing with this update. 
	 * @param targetField the target 
	 * @return
	 */
	public static JsonObject getPermissionUpdate(PermissionChange change, JsonArray changedPermissions, String targetField) throws CannotUpdateMultipleException
	{
		JsonObject update = new JsonObject();
				
		switch(change)
		{
			case Add: update.put("$addToSet", new JsonObject().put(targetField, new JsonObject().put("$each", changedPermissions)));break;
			case Remove: update.put("$pullAll", new JsonObject().put(targetField, changedPermissions));break;
			case Set: update.put("$set", new JsonObject().put(targetField, changedPermissions));break;
			case Update: throw new CannotUpdateMultipleException();							   		
		}
		return update;
	}
	
	
	
	public static JsonArray getOtherAccess(String targetID, PermissionType excludedPermission)
	{
		JsonArray result = new JsonArray();
		
		if(excludedPermission.equals(PermissionType.ALL) || excludedPermission.equals(PermissionType.EXECUTE))
		{
			return result;
		}
		if(!excludedPermission.equals(PermissionType.FULL ))
		{
			result.add(buildPermissionString(targetID, PermissionType.FULL)); 
		}
		if(!excludedPermission.equals(PermissionType.READ_WRITE ))
		{
			result.add(buildPermissionString(targetID, PermissionType.READ_WRITE)); 
		}
		if(!excludedPermission.equals(PermissionType.READ ))
		{
			result.add(buildPermissionString(targetID, PermissionType.READ)); 
		}			
		return result;
	}
	
}
