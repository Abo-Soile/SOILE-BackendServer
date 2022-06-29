package fi.abo.kogni.soile2.http_server.userManagement;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Participant {

	private HashMap<UUID, Set<UUID>> finishedTasks;
	private HashMap<UUID, UUID> currentPositions;
	
	public boolean finished(UUID taskID, UUID projectID)
	{
		return finishedTasks.get(projectID).contains(taskID);
	}
	
	public UUID getProjectPosition(UUID projectID)
	{
		return currentPositions.get(projectID);
	}
	
	public void finishElement(UUID taskID, UUID projectID)
	{
		if(!finishedTasks.containsKey(projectID))
		{
			finishedTasks.put(projectID, new HashSet<UUID>());
		}
		finishedTasks.get(projectID).add(taskID);
	}
	public Set<UUID> getFinisheTasks(UUID projectID)
	{
		return new HashSet<UUID>(finishedTasks.get(projectID));
	}
	public void setProjectPosition(UUID taskID, UUID projectID)
	{		
		currentPositions.put(projectID,taskID);	
	}
	
}
