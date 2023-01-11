package fi.abo.kogni.soile2.projecthandling.projectElements.instance;

import fi.abo.kogni.soile2.http_server.auth.SoileAuthorization.TargetElementType;
import fi.abo.kogni.soile2.http_server.authentication.utils.AccessElement;
import fi.abo.kogni.soile2.utils.SoileConfigLoader;
/**
 * This class is NOT to be used except as a representative of a project instance
 * @author Thomas Pfau
 *
 */
public class AccessProjectInstance implements AccessElement{

	@Override
	public TargetElementType getElementType() {
		return TargetElementType.INSTANCE;
	}

	@Override
	public String getTargetCollection() {
		return SoileConfigLoader.getCollectionName("projectInstanceCollection");
	}



}
