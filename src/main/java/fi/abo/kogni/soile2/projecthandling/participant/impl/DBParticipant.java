package fi.abo.kogni.soile2.projecthandling.participant.impl;

import fi.abo.kogni.soile2.projecthandling.participant.ParticipantImpl;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fi.abo.kogni.soile2.datamanagement.utils.DatedDataMap;
import fi.abo.kogni.soile2.projecthandling.participant.DataParticipantImpl;
import fi.abo.kogni.soile2.projecthandling.participant.ParticipantManager;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.ProjectInstance;
import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * This class represents a participant stored in a database.
 * The DB Participant has the following spec:
 *   Participant:
      description: An Entry in this database represents one participant in one project.
                   Data relevant for this project is stored in each document e.g. the position in the project, data from the project etc. 
      type: object
      properties:
        _id: 
          type: string
          description: The UUID of this participant. Has to have the form of a uuid
        project:
          type: string
          description: The project this participant is in.
        position: 
          type: string
          description: the uuid of the element in the project this user is currently at. Has to have the form of a uuid
          default: the project uuid if the user just started.
        finishedExperimentTasks: 
          type: array
          items:
            type: object
            properties: 
              experimentID:
                type: string
                description: uuids of the experiment with finished tasks
              tasks:
                type: array
                items:
                  type: string
                  description: uuids of the experiment with finished tasks
        outputData: 
          type: array
          items:
            type: object
            required: 
              - task
              - outputs
            properties:
              task: 
                type: string
                description: The task for which data is being stored. Contains one entry for each defined output of a task.
              outputs:
                type: array
                items:
                  type: object
                  required: 
                    - name
                    - value
                  properties:
                    name: 
                      type: string
                      description: the name of the output, the format is "[0-9A-Za-z]+"
                      example: "smoker"
                    value:
                      type: number
                      description: The value of an output                
                    timestamp:
                      type: string
                      format: date                      

 * @author Thomas Pfau
 * 
 */
public class DBParticipant extends ParticipantImpl{
	
	private ParticipantManager manager;
	private String participantCollection = SoileConfigLoader.getdbProperty("participantCollection");
	static final Logger LOGGER = LogManager.getLogger(DBParticipant.class);
	public DBParticipant(JsonObject data, ParticipantManager manager)
	{
		super(data);
		this.manager = manager;
	}		
	
	@Override
	public Future<String> save()
	{		
		return manager.save(this);
	}

	@Override
	public Future<Void> addResult(String taskID, JsonObject result) {
		Promise<Void> resultPromise = Promise.promise(); 
		getCurrentStep()
		.onSuccess(cstep ->
		{
			manager.updateResults(this, cstep, taskID, result);
			resultPromise.complete();
		})
		.onFailure(err -> resultPromise.fail(err));
				
		return resultPromise.future();
	}

	@Override
	public Future<Integer> getCurrentStep() {
		return Future.succeededFuture(currentStep);
	}
	
	/**
	 * 
	 */
	@Override
	public Future<Void> setOutputDataForTask(String taskID, JsonArray taskOutputData)
	{		
		Promise<Void> outputsSetPromise = Promise.promise();
		if(taskOutputData != null && taskOutputData.size() > 0)
		{			
			manager.updateOutputsForTask(this, taskID, taskOutputData)
			.onSuccess(v-> {				
				LOGGER.debug("Updated the db outputs");
				for(Object output : taskOutputData)
				{
					JsonObject dataElement = (JsonObject) output;				
					super.addOutput(taskID, dataElement.getString("name"), dataElement.getNumber("value"));					
				}				
				manager.getElement(this.getID())
				.onSuccess(part -> {					
					outputsSetPromise.complete();	
				})
				.onFailure(err -> outputsSetPromise.fail(err));
				
			})
			.onFailure(err -> outputsSetPromise.fail(err));
		}
		else
		{
			outputsSetPromise.complete();
		}
		return outputsSetPromise.future();
		
	}

	
	@Override
	public Future<Void> resetOutputs()
	{		
		outputMap = new DatedDataMap<>();
		return manager.resetParticipant(this);
	}
	

}
