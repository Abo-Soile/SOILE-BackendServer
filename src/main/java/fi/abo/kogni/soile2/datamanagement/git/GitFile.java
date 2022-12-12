package fi.abo.kogni.soile2.datamanagement.git;

/**
 * This represents all data necessary to retrieve the file for a given repository (the repository id can e.g. be a task/project or similar). 
 * @author Thomas Pfau
 *
 */
public class GitFile {

	private String fileName;
	private String repoID;
	private String repoVersion;	
	public GitFile(String fileName, String repoID, String repoVersion) {
		super();
		this.fileName = fileName;
		this.repoID = repoID;
		this.repoVersion = repoVersion;
	}
	public String getFileName() {
		return fileName;
	}
	public String getRepoID() {
		return repoID;
	}
	public String getRepoVersion() {
		return repoVersion;
	}
	
	@Override
	public boolean equals(Object o)
	{
		if(o instanceof GitFile)
		{
			return ((GitFile) o).getRepoID().equals(repoID) && ((GitFile) o).getRepoVersion().equals(repoVersion) && ((GitFile) o).getFileName().equals(fileName);
		}
		return false;
	}
	
	@Override
	public int hashCode()
	{			
		return (fileName + repoID + repoVersion).hashCode();
	}
	
	@Override
	public String toString()
	{		
		return "GitFile{ filename: " + fileName + " ; repoID: " + repoID + " ; version: " + repoVersion + " }";
	}
	
	
}
