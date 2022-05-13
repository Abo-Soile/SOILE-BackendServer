package fi.abo.kogni.soile2.experiment;

import java.util.HashMap;
import java.util.Set;
import java.util.UUID;

import com.mongodb.reactivestreams.client.MongoClient;
/**
 * An Experiment is associated directly with one user, and is the instance created for this user.
 * It contains pointers to its individual tasks (phases) or sub-experiments
 * @author Thomas Pfau
 *
 */
public abstract class Experiment implements ExperimentElement{
	
	//The Hashmap contains   the order of all phases of an experiment. If there is only one phase than 
	// there will be only one Hashmap within this Experiment.
	HashMap<Integer, ExperimentPhase> experimentPhases;
	Set<String> PhaseUUIDs;
	ExperimentPhase currentPhase;
	
	private boolean randomize;
	
	
	public ExperimentPhase addPhase(String name)
	{
		PhaseUUIDs.add(name);
		return new ExperimentPhase(name);
	}
	
	public ExperimentPhase addPhase()
	{
		return addPhase(UUID.randomUUID().toString());
	}
	
	public ExperimentElement next()
	{
		if(currentPhase != null)
		{
//			if(currentPhase.hasNext())
//			{
				
//			}
		}
		if(randomize)
		{
			
		}
		return null;
	}
	
	public void saveExperiment(MongoClient client)
	{
		
	}
}
