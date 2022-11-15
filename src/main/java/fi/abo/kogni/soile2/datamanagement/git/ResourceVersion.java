package fi.abo.kogni.soile2.datamanagement.git;

import java.util.UUID;
/**
 * Placeholder class for a version/filename/elementID combination that creates hases based on these factors
 * and allows comparison between Resources.  
 * @author Thomas Pfau
 *
 */
public class ResourceVersion extends GitFile{

	private UUID elementID;
	
	public ResourceVersion(UUID elementID, String version, String filename)
	{
		super(elementID.toString(),version,filename);
		this.elementID = elementID;
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
		return super.getRepoVersion();
	}

	@Override
	public int hashCode()
	{
		return this.elementID.hashCode() + getVersion().hashCode()+ getFileName().hashCode();
	}
	
}
