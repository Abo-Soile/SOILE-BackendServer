package fi.abo.kogni.soile2.experiment.task;

public class VersionedTask {

	private String taskID;
	private String versionID;
	
	public VersionedTask(String TaskID, String VersionID)
	{
		this.taskID = TaskID;
		this.versionID = VersionID;		
	}
	
	@Override
	public int hashCode()
	{
		return (taskID + versionID).hashCode();		
	}
	
	@Override
	public boolean equals(Object o)
	{
		if(o instanceof VersionedTask)
		{
			return taskID.equals(((VersionedTask)o).taskID) && versionID.equals(((VersionedTask)o).versionID);  
		}
		return false;
	}
	
	
}
