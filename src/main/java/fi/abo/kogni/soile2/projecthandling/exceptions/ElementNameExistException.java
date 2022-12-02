package fi.abo.kogni.soile2.projecthandling.exceptions;

public class ElementNameExistException extends Exception {

	private static final long serialVersionUID = 1L;
	private String name;	
	private String uuid = "";
	
	public ElementNameExistException(String id, String uuid)
	{		
		super("An element with name "+ id + " already exists");
		this.name = id;
		this.uuid = uuid;
	}
	
	public String getExistingElementUUID()
	{
		return uuid;
	}
	
	public String getName()
	{
		return name;
	}	
}
