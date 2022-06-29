package fi.abo.kogni.soile2.project;

import java.util.HashMap;
import java.util.UUID;

import fi.abo.kogni.soile2.http_server.userManagement.Participant;
import fi.abo.kogni.soile2.project.items.ProjectDataBaseObjectInstance;

public class Project {

	UUID projectID;
	private HashMap<UUID, ProjectDataBaseObjectInstance> elements;
	public UUID getActive(Participant user)
	{
		UUID currentPosition = user.getProjectPosition(projectID);
		return currentPosition;
	}
	
	public UUID getID()
	{
		return projectID;
	}
	public void finishStep(Participant user)
	{
	 storeStepCompleted(user.getProjectPosition(projectID));
	 
	}	
	
	public ProjectDataBaseObjectInstance getElement(UUID elementID)
	{
		return elements.get(elementID);
	}
	private void setNextStep(Participant user)
	{
		ProjectDataBaseObjectInstance current = getElement(user.getProjectPosition(projectID));
		user.finishElement(user.getProjectPosition(projectID), projectID);
		ProjectDataBaseObjectInstance nextElement = getElement(current.getNext());
		user.setProjectPosition(nextElement.nextTask(user), projectID);
		
	}
	
	
}
