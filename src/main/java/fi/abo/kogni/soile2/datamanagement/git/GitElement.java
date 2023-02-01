package fi.abo.kogni.soile2.datamanagement.git;

import io.vertx.core.json.JsonObject;

public class GitElement {
		
		private String repoID;
		private String repoVersion;	
		public GitElement(String repoID, String repoVersion) {
			super();			
			this.repoID = repoID;
			this.repoVersion = repoVersion;
		}
		public GitElement(JsonObject json) {
			this.repoID = json.getString("repoID");
			this.repoVersion = json.getString("version");
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
			if(o instanceof GitElement)
			{
				return ((GitFile) o).getRepoID().equals(repoID) && ((GitFile) o).getRepoVersion().equals(repoVersion);
			}
			return false;
		}
		
		@Override
		public int hashCode()
		{			
			return (repoID + repoVersion).hashCode();
		}
		
		@Override
		public String toString()
		{		
			return "GitRepo{ repoID: " + repoID + " ; version: " + repoVersion + " }";
		}
		
		public boolean isValid()
		{		
			return repoID != null && repoVersion != null;
		}
		public JsonObject toJson()
		{		
			return new JsonObject().put("repoID",repoID).put("version", repoVersion);
		}
	}
