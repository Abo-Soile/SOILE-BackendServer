package fi.abo.kogni.soile2.projecthandling.projectElements.instance.impl;

import java.util.UUID;

import fi.abo.kogni.soile2.datamanagement.git.ResourceVersion;
import fi.abo.kogni.soile2.utils.SoileConfigLoader;

public class TaskSourceVersion extends ResourceVersion{

	public TaskSourceVersion(UUID elementID, String version, String filename) {
		super(elementID, version,SoileConfigLoader.getExperimentProperty("sourceCodeFileName"));
	}

}
