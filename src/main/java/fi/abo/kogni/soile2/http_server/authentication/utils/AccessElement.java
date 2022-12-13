package fi.abo.kogni.soile2.http_server.authentication.utils;

import fi.abo.kogni.soile2.http_server.auth.SoileAuthorization.TargetElementType;

public interface AccessElement {

	/**
	 * Get the Type of the element
	 * @return
	 */
	public TargetElementType getElementType();

	
	/**
	 * Get the database collection to use for this Element
	 * @return
	 */
	public String getTargetCollection();
}
