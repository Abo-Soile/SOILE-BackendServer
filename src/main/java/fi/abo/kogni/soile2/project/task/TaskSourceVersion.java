package fi.abo.kogni.soile2.project.task;

import java.util.UUID;

import fi.abo.kogni.soile2.project.itemManagement.ResourceVersion;
import fi.abo.kogni.soile2.utils.SoileConfigLoader;

public class TaskSourceVersion extends ResourceVersion{

	public TaskSourceVersion(UUID elementID, String version, String filename) {
		super(elementID, version,SoileConfigLoader.getExperimentProperty("sourceCodeFileName"));
	}

}
