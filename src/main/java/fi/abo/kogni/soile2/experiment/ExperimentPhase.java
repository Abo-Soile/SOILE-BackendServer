package fi.abo.kogni.soile2.experiment;

import java.util.HashMap;

public class ExperimentPhase {

	private String ID;
	private boolean randomized;
	HashMap<Integer, ExperimentElement> experimentElements;
	
	
	public ExperimentPhase(String ID)
	{
		
	}
	
	public ExperimentElement next()
	{
		return null;
	}
	
	
}
