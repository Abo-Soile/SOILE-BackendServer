package fi.abo.kogni.soile2.project.experiment;

import java.util.UUID;

import fi.abo.kogni.soile2.datamanagement.git.ResourceVersion;
import fi.abo.kogni.soile2.utils.SoileConfigLoader;

public class ExperimentVersion extends ResourceVersion{

	public ExperimentVersion(UUID elementID, String version) {
		super(elementID, version, SoileConfigLoader.getExperimentProperty("experimentFileName"));
		// TODO Auto-generated constructor stub
	}	
}
