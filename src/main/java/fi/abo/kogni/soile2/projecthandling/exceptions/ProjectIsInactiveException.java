package fi.abo.kogni.soile2.projecthandling.exceptions;

/**
 * The project that was requested is inactive and thus cannot be accessed
 * @author Thomas Pfau
 *
 */
public class ProjectIsInactiveException extends Exception {

	/**
	 * Default constructor
	 * @param name the name of the inactive project
	 */
	public ProjectIsInactiveException(String name) {
		super("Project \"" + name + "\" is not active" );
	}
}
