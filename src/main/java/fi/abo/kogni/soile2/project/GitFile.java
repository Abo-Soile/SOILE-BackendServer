package fi.abo.kogni.soile2.project;

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
	
	
	
}
