package fi.abo.kogni.soile2.experiment.task;

import fi.abo.kogni.soile2.datamanagement.git.GitFile;
import fi.abo.kogni.soile2.datamanagement.git.GitManager;
import fi.abo.kogni.soile2.datamanagement.utils.DataRetriever;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;

public class TaskCodeRetriever implements DataRetriever<VersionedTask, String>{

	GitManager git;		
	private String targetFile;		
	
	@Override
	public Future<String> getElement(VersionedTask ID) {
		GitFile file = new GitFile(targetFile, ID.getTaskID(), ID.getVersionID());		
		return git.getGitFileContents(file);
	}

	@Override
	public void getElement(VersionedTask ID, Handler<AsyncResult<String>> handler) {
		handler.handle(getElement(ID));
	}

}
