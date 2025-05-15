package fi.abo.kogni.soile2.datamanagement.git;

import fi.aalto.scicomp.gitFs.gitProviderVerticle;
import io.vertx.core.json.JsonObject;
/**
 * This Class represents a key to a given git repository at a given version.
 * @author Thomas Pfau
 *
 */
public class GitElement {
		
		private String repoID;
		private String repoVersion;
		/**
		 * Create from the repository ID and repository version.
		 * @param repoID the ID of the repository to use
		 * @param repoVersion the version of the element in the repo 
		 */
		public GitElement(String repoID, String repoVersion) {
			super();			
			this.repoID = repoID;
			this.repoVersion = repoVersion;
		}
		
		/**
		 * Extract info from Json
		 * @param json the Json representing the element
		 */
		public GitElement(JsonObject json) {
			this.repoID = json.getString("repoID");
			this.repoVersion = json.getString("version");
		}
		/**
		 * Get the Repository ID of the represented Repository (for use with a {@link gitProviderVerticle})
		 * @return the rpository id
		 */
		public String getRepoID() {
			return repoID;
		}
		/**
		 * Get the Repository version this Element represents
		 * @return the version of the repo element
		 */
		public String getRepoVersion() {
			return repoVersion;
		}
		
		@Override
		public boolean equals(Object o)
		{
			if(o instanceof GitElement)
			{
				return ((GitElement) o).getRepoID().equals(repoID) && ((GitElement) o).getRepoVersion().equals(repoVersion);
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
		
		/**
		 * If any of the parts of this element are <code>null</code> the element is inValid.
		 * @return whether the element is a (potentially) valid element - if the repo does not exist it is still invalid...
		 */
		public boolean isValid()
		{		
			return repoID != null && repoVersion != null;
		}
		/**
		 * Convert to Json
		 * @return a {@link JsonObject} with a "repoID" and a "version" field.
		 */
		public JsonObject toJson()
		{		
			return new JsonObject().put("repoID",repoID).put("version", repoVersion);
		}
	}
