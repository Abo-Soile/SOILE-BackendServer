package fi.abo.kogni.soile2.http_server.userManagement;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class containing some utility methods for user management
 * @author Thomas Pfau
 *
 */
public class UserUtils {
	//TODO: Potentially fix this pattern. might need to make it more general 
	static final Pattern emailPattern = Pattern.compile("[A-Za-z0-9.$#_%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,4}");
	
	/**
	 * check if a provided email address is valid
	 * @param emailAddress the email addess to check
	 * @return whether its a valid email address
	 */
	public static boolean isValidEmail(String emailAddress)
	{
		if(emailAddress == null)
		{
			return false;
		}
		Matcher mat = emailPattern.matcher(emailAddress);
		return mat.matches();
	}
}
