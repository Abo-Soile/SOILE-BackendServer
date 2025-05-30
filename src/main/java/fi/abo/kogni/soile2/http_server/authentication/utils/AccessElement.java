package fi.abo.kogni.soile2.http_server.authentication.utils;

import fi.abo.kogni.soile2.http_server.auth.SoileAuthorization.TargetElementType;

/**
 * An Access Element is a Type of element used in Handling. Experiments/Projects/Task and Studies should implement this 
 * Interface
 * @author Thomas Pfau
 *
 */
public interface AccessElement {

	/**
	 * Get the Type of the element
	 * @return The {@link TargetElementType} of the Access Element
	 */
	public TargetElementType getElementType();

	
	/**
	 * Get the database collection to use for this Element
	 * @return The name of the Collection associated with the AccessElement
	 */
	public String getTargetCollection();
}
