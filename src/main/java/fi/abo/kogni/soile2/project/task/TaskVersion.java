package fi.abo.kogni.soile2.project.task;

import java.util.UUID;

import fi.abo.kogni.soile2.datamanagement.git.ResourceVersion;
import fi.abo.kogni.soile2.utils.SoileConfigLoader;

public class TaskVersion extends ResourceVersion {

	public TaskVersion(UUID elementID, String version, String filename) {
		super(elementID, version, SoileConfigLoader.getExperimentProperty("taskFileName")); 
	}

}
