package fi.abo.kogni.soile2.datamanagement.git;

import io.vertx.core.json.JsonObject;

/**
 * This represents all data necessary to retrieve the file for a given repository (the repository id can e.g. be a task/project or similar). 
 * @author Thomas Pfau
 *
 */
public class GitFile {

	private String fileName;
	private String repoID;
	private String repoVersion;	
	/**
	 * Default constructor using individual data
	 * @param fileName the Filename of the file
	 * @param repoID the id of the repo the file is in
	 * @param repoVersion the version of the file
	 */
	public GitFile(String fileName, String repoID, String repoVersion) {
		super();
		this.fileName = fileName;
		this.repoID = repoID;
		this.repoVersion = repoVersion;
	}
	/**
	 * Default constructor using data from a Json object
	 * @param json the Json containing the data
	 */
	public GitFile(JsonObject json) {
		super();
		this.fileName = json.getString("filename");
		this.repoID = json.getString("repoID");
		this.repoVersion = json.getString("version");
	}
	
	/**
	 * Get the fileName this File represents.
	 * @return the name of the file
	 */
	public String getFileName() {
		return fileName;
	}
	/**
	 * Get the RepoId this file should be stored in / retrieved from
	 * @return the id of the repo
	 */
	public String getRepoID() {
		return repoID;
	}
	/**
	 * Get the version of the file that will be looked up / stored to.
	 * @return the version of the file
	 */
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
	
	/**
	 * Ge a Json representation for this file
	 * @return a {@link JsonObject} with a "filename", a "repoID" and a "version" field.
	 */
	public JsonObject toJson()
	{		
		return new JsonObject().put("filename",fileName).put("repoID",repoID).put("version", repoVersion);
	}
}
