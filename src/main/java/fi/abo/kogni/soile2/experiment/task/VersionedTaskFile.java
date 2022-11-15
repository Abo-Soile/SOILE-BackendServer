package fi.abo.kogni.soile2.experiment.task;

public class VersionedTaskFile extends VersionedTask{

	private String filename;

	public VersionedTaskFile(String TaskID, String VersionID, String filename)
	{
		super(TaskID,VersionID);
		this.filename = filename;
	}		
	
	@Override
	public int hashCode()
	{
		return (getTaskID() + getVersionID() + filename).hashCode();		
	}
	
	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	@Override
	public boolean equals(Object o)
	{
		if(o instanceof VersionedTaskFile)
		{
			return super.equals(o) &&  filename.equals(((VersionedTaskFile)o).filename);  
		}
		return false;
	}
	
	
}
