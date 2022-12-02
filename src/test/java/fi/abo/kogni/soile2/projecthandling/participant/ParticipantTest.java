package fi.abo.kogni.soile2.projecthandling.participant;
import org.junit.Test;
import org.junit.runner.RunWith;

import fi.abo.kogni.soile2.projecthandling.ProjectBaseTest;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.ProjectInstance;
import fi.abo.kogni.soile2.projecthandling.utils.ParticipantImplForTesting;
import fi.abo.kogni.soile2.projecthandling.utils.ProjectFactoryImplForTesting;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class ParticipantTest extends ProjectBaseTest{

	
	@Test
	public void testComparison(TestContext context) {
		Async p1Async = context.async();
		ParticipantImplForTesting.getTestParticipant(context,0,getPos(0)).onSuccess(participant1 -> {
			Async p2Async = context.async();	
			ParticipantImplForTesting.getTestParticipant(context,3,getPos(0)).onSuccess(participantOther -> {
				context.assertFalse(participant1.equals(participantOther));
				p2Async.complete();
			});
			Async p3Async = context.async();
			ParticipantImplForTesting.getTestParticipant(context,0,getPos(0)).onSuccess(participantSame -> {
				context.assertTrue(participant1.equals(participantSame));
				p3Async.complete();
			});					// 
			Async p4Async = context.async();
			ProjectFactoryImplForTesting fac = new ProjectFactoryImplForTesting();
			ProjectInstance.instantiateProject(getPos(0), fac).onSuccess(project -> {
				Participant copy = new ParticipantImplForTesting(participant1.toJson(), project);
				context.assertTrue(participant1.equals(copy));
				p4Async.complete();
			});			
			p1Async.complete();
			
		});		
	}
	
	@Test
	public void testJSONIO(TestContext context)
	{
		Async p1Async = context.async();
		ParticipantImplForTesting.getTestParticipant(context,0,getPos(0)).onSuccess(participant1 -> {
			Async p2Async = context.async();				
			ProjectFactoryImplForTesting fac = new ProjectFactoryImplForTesting();
			ProjectInstance.instantiateProject(getPos(0), fac).onSuccess(project -> {
				Participant copy = new ParticipantImplForTesting(participant1.toJson(), project);
				context.assertTrue(participant1.equals(copy));
				p2Async.complete();
			});			
			p1Async.complete();
			
		});
	}
	
	
		
}