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
		// TODO Auto-generated method stub
		return TargetElementType.INSTANCE;
	}

	@Override
	public String getTargetCollection() {
		// TODO Auto-generated method stub
		return SoileConfigLoader.getCollectionName("projectInstanceCollection");
	}



}
