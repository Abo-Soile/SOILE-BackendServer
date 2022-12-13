package fi.abo.kogni.soile2.projecthandling.exceptions;

public class ProjectIsInactiveException extends Exception {

	public ProjectIsInactiveException(String name) {
		super("Project \"" + name + "\" is not active" );
	}
}
