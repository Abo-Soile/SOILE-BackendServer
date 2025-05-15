package fi.abo.kogni.soile2.projecthandling.projectElements.instance.randomizers.impl;

import fi.abo.kogni.soile2.projecthandling.participant.Participant;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.Study;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.randomizers.Randomizer;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * A BAsic randomizer
 * @author Thomas Pfau
 *
 */
public class BasicRandomizer extends Randomizer {

	boolean assignGroupOnce = false;
	/**
	 * Basic Randomizer that just does a general randmization
	 * @param data Randomizer specification
	 * @param source source Study
	 */
	public BasicRandomizer(JsonObject data, Study source) {
		super(data,source);
		this.setSettings(this.data.getJsonArray("settings"));
	}

	@Override
	public Future<String> nextTask(Participant p) {
		String instanceID = this.data.getString("instanceID");
		if(assignGroupOnce)
		{
			Object id = p.getValueForRandomizationGroup(instanceID);
			if( id != null)
			{
				// we already got one. return it
				return Future.succeededFuture(this.data.getJsonArray("options").getJsonObject((Integer)id).getString("next"));
			}
			else
			{
				Double randomVar = Math.random();
				Double fraction = 1.0/this.data.getJsonArray("options").size();		
				Double position = randomVar/fraction;
				Integer randomPosition = position.intValue();
				// first, we get the current Filter passes. 
				return sourceStudy.getFilterPassesAndAddOne(instanceID)
						.compose(count -> {				   
							p.setValueForRandomizationGroup(instanceID, randomPosition);
							// then we save the participant and return the option value.
							return p.save().map(partID -> this.data.getJsonArray("options").getJsonObject(randomPosition).getString("next"));
						});
			}
		}
		else
		{
			Double randomVar = Math.random();
			Double fraction = 1.0/this.data.getJsonArray("options").size();		
			Double position = randomVar/fraction;
			// intValue rounds "toZero", i.e. it implicitly does a floor operation, which is what we want here.
			return Future.succeededFuture(this.data.getJsonArray("options").getJsonObject(position.intValue()).getString("next"));
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
			case "assignGroupOnce" : this.assignGroupOnce = currentSetting.getBoolean("value"); break;
			default: break; // do nothing				
			}
		}
	}

}
