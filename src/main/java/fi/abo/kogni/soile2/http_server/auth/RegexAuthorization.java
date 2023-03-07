package fi.abo.kogni.soile2.http_server.auth;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.vertx.ext.auth.User;
import io.vertx.ext.auth.authorization.Authorization;
import io.vertx.ext.auth.authorization.AuthorizationContext;
import io.vertx.ext.auth.authorization.Authorizations;
import io.vertx.ext.auth.authorization.PermissionBasedAuthorization;
import io.vertx.ext.auth.authorization.WildcardPermissionBasedAuthorization;


/**
 * A Regex Authorization is solely for verification purposes, i.e. to test whether a specific authorization 
 * is sufficient. 
 * @author Thomas Pfau
 *
 */
public class RegexAuthorization implements PermissionBasedAuthorization {

	  private final Pattern permission;	  
	  private String resource;

	  public RegexAuthorization(String permission) {
	    this.permission = Pattern.compile(Objects.requireNonNull(permission));	    
	  }

	  @Override
	  public boolean equals(Object obj) {
	    if (this == obj)
	      return true;
	    if (!(obj instanceof PermissionBasedAuthorization))
	      return false;
	    PermissionBasedAuthorization other = (PermissionBasedAuthorization) obj;
	    return permission.pattern().equals(other.getPermission()) && resource.equals(other.getResource());
	  }

	  @Override
	  public String getPermission() {
	    return permission.pattern();
	  }

	  @Override
	  public int hashCode() {
	    return Objects.hash(permission, resource);
	  }

	  @Override
	  public boolean match(AuthorizationContext context) {
	    Objects.requireNonNull(context);

	    User user = context.user();
	    if (user != null) {
	      final Authorization resolvedAuthorization = getResolvedAuthorization(context);
	      final Authorizations authorizations = user.authorizations();
	      for (String providerId: authorizations.getProviderIds()) {
	        for (Authorization authorization : authorizations.get(providerId)) {
	          if (authorization.verify(resolvedAuthorization)) {
	            return true;
	          }
	        }
	      }
	    }
	    return false;
	  }

	  private PermissionBasedAuthorization getResolvedAuthorization(AuthorizationContext context) {
	    if (resource == null) {
	      return this;
	    }
	    return PermissionBasedAuthorization.create(this.permission.pattern()).setResource(resource);
	  }

	  @Override
	  public boolean verify(Authorization otherAuthorization) {
	    Objects.requireNonNull(otherAuthorization);

	    if (otherAuthorization instanceof PermissionBasedAuthorization) {
	      PermissionBasedAuthorization otherPermissionBasedAuthorization = (PermissionBasedAuthorization) otherAuthorization;
	      Matcher match = permission.matcher(otherPermissionBasedAuthorization.getPermission());
	      if (match.find()) {
	        if (getResource() == null) {
	          return otherPermissionBasedAuthorization.getResource() == null;
	        }
	        return getResource().equals(otherPermissionBasedAuthorization.getResource());
	      }
	    }
	    else if (otherAuthorization instanceof WildcardPermissionBasedAuthorization) {
	      WildcardPermissionBasedAuthorization otherWildcardPermissionBasedAuthorization = (WildcardPermissionBasedAuthorization) otherAuthorization;
	      Matcher match = permission.matcher(otherWildcardPermissionBasedAuthorization.getPermission());
	      if (match.find()) {
	        if (getResource() == null) {
	          return otherWildcardPermissionBasedAuthorization.getResource() == null;
	        }
	        return getResource().equals(otherWildcardPermissionBasedAuthorization.getResource());
	      }
	    }
	    return false;
	  }

	  @Override
	  public String getResource() {
	    return resource;
	  }

	  @Override
	  public PermissionBasedAuthorization setResource(String resource) {
	    Objects.requireNonNull(resource);
	    this.resource = resource;
	    return this;
	  }
}
