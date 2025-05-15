package fi.abo.kogni.soile2.projecthandling.projectElements.instance.randomizers.impl;

import java.util.HashMap;

import fi.abo.kogni.soile2.projecthandling.participant.Participant;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.Study;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.randomizers.Randomizer;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * A Randomizer that makes sure, that Assignment happens in blocks.
 * @author Thomas Pfau
 *
 */
public class BlockRandomizer extends Randomizer {


	String instanceID;	
	String blockSpecification;
	HashMap<String,String> optionMap;
	/**
	 * Default constructor
	 * @param data specification data
	 * @param source the study the randomizer is in
	 */
	public BlockRandomizer(JsonObject data, Study source) {
		super(data, source);
		setSettings(this.data.getJsonArray("settings"));
		setOptions(this.data.getJsonArray("options"));
	}

	@Override
	public Future<String> nextTask(Participant p) {
		String instanceID = this.data.getString("instanceID");	
		Object id = p.getValueForRandomizationGroup(instanceID);
		if( id != null)
		{
			// we already got one. return it
			return Future.succeededFuture(optionMap.get(String.valueOf(blockSpecification.charAt((Integer)id))));
		}
		else
		{
			// first, we get the current Filter passes. 
			return sourceStudy.getFilterPassesAndAddOne(instanceID)
					.compose(count -> {				   
						// after that, we determine the current value
						int currentBlock = ((int)count)%blockSpecification.length();
						// and set it in the participant
						p.setValueForRandomizationGroup(instanceID, (Integer)currentBlock);
						// then we save the participant and return the option value.
						return p.save().map(partID -> optionMap.get(String.valueOf(blockSpecification.charAt((Integer)currentBlock))));
					});
		}

	}
	private void setOptions(JsonArray options) {
		optionMap = new HashMap<>();
		for(int i = 0; i < options.size(); i++) {
			optionMap.put(options.getJsonObject(i).getString("name"),options.getJsonObject(i).getString("next"));
		}
	}


	private void setSettings(JsonArray settings) {
		// TODO Auto-generated method stub
		for(int i = 0; i < settings.size(); ++i)
		{
			JsonObject currentSetting = settings.getJsonObject(i);
			String currentSettingName = currentSetting.getString("name");
			switch(currentSettingName)
			{
				case "blockSpecification" : this.blockSpecification = currentSetting.getString("value"); break;
				default: break; // do nothing				
			}
		}
	}

}
