package fi.abo.kogni.soile2.http_server.authentication.utils;

public class PermissionIDStrategy {
	
	public enum Type
	{
		Owner,
		Collaborator,
		Participant
	}
	
	public static String getPermissionID(String ExperimentID, Type t)
	{
		switch(t) {
			case Owner:
					return "O" + ExperimentID;
			case Collaborator:
				return "C" + ExperimentID;
			case Participant:
				return "P" + ExperimentID;
			default:
				return null;
					
		}
	}
	
	public static Type getIdFromPermission(String Permission)
	{
		
		char ID =  Permission.charAt(0);
		switch(ID)
		{
			case('O'): 	return Type.Owner;
			case('C'): 	return Type.Collaborator;
			case('P'): 	return Type.Participant;
			default: return null;
		}
	}
}
