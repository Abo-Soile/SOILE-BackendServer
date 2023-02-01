package fi.abo.kogni.soile2.projecthandling.projectElements.impl;

import fi.abo.kogni.soile2.datamanagement.git.GitResourceManager;
import fi.abo.kogni.soile2.projecthandling.projectElements.ElementDataHandler;

public class TaskDataHandler extends ElementDataHandler<Task>{
	
	public TaskDataHandler(GitResourceManager manager)
	{
		super(manager, Task::new);		
	}	
}
