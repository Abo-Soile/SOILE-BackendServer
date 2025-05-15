package fi.abo.kogni.soile2.projecthandling.exceptions;

/**
 * The name given to the element already exists. Names have to be unique.
 * @author Thomas Pfau
 *
 */
public class ElementNameExistException extends Exception {

	private static final long serialVersionUID = 1L;
	/**
	 * Name of the element
	 */
	private String name;	
	/**
	 * UUID of the existing element
	 */
	private String UUID = "";
	
	/**
	 * Default constructor
	 * @param id the id indicating the name that exists
	 * @param UUID the UUID of the existig element
	 */
	public ElementNameExistException(String id, String UUID)
	{		
		super("An element with name "+ id + " already exists");
		this.name = id;
		this.UUID = UUID;
	}
	/**
	 * Get the existing Elements UUID
	 * @return the UUID of the existing element
	 */
	public String getExistingElementUUID()
	{
		return UUID;
	}
	/**
	 * Get the name of the element that already exists
	 * @return the name of the element that caused this.
	 */
	public String getName()
	{
		return name;
	}	
}
