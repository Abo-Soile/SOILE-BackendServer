package fi.abo.kogni.soile2.project.itemManagement;

import java.util.UUID;

public class ResourceVersion{

	private UUID elementID;
	private String version;
	private String filename; 
	
	public ResourceVersion(UUID elementID, String version, String filename)
	{
		this.elementID = elementID;
		this.version = version;
		this.filename = filename;
	}
	/**
	 * Get the element ID 
	 * @return the UUID identifying this element
	 */
	public UUID getElementID() {
		return elementID;
	}
	/**
	 * Get the element version 
	 * @return the git-hash identifying the version of this element.
	 */
	public String getVersion() {		
		return version;
	}

	/**
	 * Get the element version 
	 * @return the git-hash identifying the version of this element.
	 */
	public String getFilename() {		
		return filename;
	}

	@Override
	public int hashCode()
	{
		return this.elementID.hashCode() + this.version.hashCode()+ filename.hashCode();
	}
	
	@Override
	public boolean equals(Object o)
	{
		if(o instanceof ResourceVersion)
		{
			return ((ResourceVersion) o).getElementID().equals(elementID) && ((ResourceVersion) o).getVersion().equals(version) && ((ResourceVersion) o).getFilename().equals(filename);
		}
		return false;
	}
	
}
