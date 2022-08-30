package fi.abo.kogni.soile2.project.instance;

/**
 * This interface needs to be used to provide specific project instance implementations 
 * For use in the project handling part of soile. 
 * @author Thomas Pfau
 *
 */
public interface ProjectFactory {

	/**
	 * Create a {@link ProjectInstance} of the type provided by the factory.
	 * @return a {@link ProjectInstance} 
	 */
	public ProjectInstance createInstance();
}
