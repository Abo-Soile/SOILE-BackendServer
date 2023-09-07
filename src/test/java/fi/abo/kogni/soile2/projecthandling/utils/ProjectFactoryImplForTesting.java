package fi.abo.kogni.soile2.projecthandling.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import fi.abo.kogni.soile2.projecthandling.participant.Participant;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.Study;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.StudyFactory;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.impl.FieldSpecifications;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * The Test study does NOT offer the possibility to handle data  on a study level, i.e. some randomizers wont work, and cannot be used with it.
 * @author Thomas Pfau
 *
 */
public class ProjectFactoryImplForTesting implements StudyFactory{

	@Override
	public Study createInstance() {
		return new TestStudy();
	}

	
	private class TestStudy extends Study
	{
		JsonArray participants = new JsonArray();
		private boolean active = true;
		@Override
		public Future<JsonObject> save(boolean updateVersion) {
			// do nothing;
			return Future.<JsonObject>succeededFuture(this.toDBJson());
		}

		@Override
		public Future<JsonObject> load(JsonObject id) {			
			Promise<JsonObject> studyPromise = Promise.promise();
			try
			{				
				studyPromise.complete(loadProjectData(id.getInteger("pos")));
			}
			catch(Exception e)
			{
				studyPromise.fail(e);
			}
			return studyPromise.future();
		}

		@Override
		public Future<JsonObject> delete() {			
			return Future.succeededFuture(toDBJson());
		}

		@Override
		public Future<Void> addParticipant(Participant p) {
			participants.add(p.getID());
			return Future.succeededFuture();
		}

		@Override
		public Future<Boolean> deleteParticipant(Participant p) {
			participants.remove(p.getID());
			return Future.succeededFuture(true);
		}

		@Override
		public Future<JsonArray> getParticipants() {
			return Future.succeededFuture(participants.copy());
		}

		@Override
		public Future<Void> deactivate() {
			active = false;
			return Future.succeededFuture();
		}

		@Override
		public Future<Void> activate() {
			active = true;
			return Future.succeededFuture(); 
		}
		public Future<Boolean> isActive() {			
			return Future.succeededFuture(active); 
		}
		@Override
		public Future<JsonArray> createSignupTokens(int count) {
			return null;
		}

		@Override
		public Future<String> createPermanentAccessToken() {
			return null;
		}

		@Override
		public Future<Void> useToken(String token) {
			return null;
		}

		@Override
		public Future<JsonArray> reset() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public FieldSpecifications getUpdateableDBFields() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		protected Future<Boolean> checkShortCutAvailable(String shortcut) {
			// TODO Auto-generated method stub
			return Future.succeededFuture(true);
		}

		@Override
		public Future<Long> getStoredModificationDate() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Future<JsonObject> getTokenInformation() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Future<Integer> getFilterPassesAndAddOne(String filterID) {
			// TODO Auto-generated method stub
			return null;
		}		
	}
	
	public static JsonObject loadProjectData() throws IOException
	{
		return loadProjectData(0);
	}
	
	public static JsonObject loadProjectData(int i) throws IOException
	{
		JsonArray ProjectInstanceDef = new JsonArray(Files.readString(Paths.get(ProjectFactoryImplForTesting.class.getClassLoader().getResource("ProjectDefinition.json").getPath())));
		JsonArray projectGitDef = new JsonArray(Files.readString(Paths.get(ProjectFactoryImplForTesting.class.getClassLoader().getResource("GitProjObj.json").getPath())));
		ProjectInstanceDef.getJsonObject(i).put("sourceProject", projectGitDef.getJsonObject(i));		
		return ProjectInstanceDef.getJsonObject(i);

	}
	
	public static Future<Study> loadProject(JsonObject id) throws IOException 
	{		
		ProjectFactoryImplForTesting fac = new ProjectFactoryImplForTesting();
		return Study.instantiateStudy(id, fac);
		
	}
}
