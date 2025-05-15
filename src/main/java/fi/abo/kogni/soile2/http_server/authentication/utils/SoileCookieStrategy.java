package fi.abo.kogni.soile2.http_server.authentication.utils;

/**
 * The Soile Cookie building strategy, i.e. the strategy on how to extract data from the cookie and build it.
 * @author Thomas Pfau
 *
 */
public class SoileCookieStrategy {

	/**
	 * Build a Cookie String from Username and token.
	 * @param username the username
	 * @param token the token to use
	 * @return the combined token
	 */
	public static String buildCookieContent(String username, String token)
	{
		return username + ":" + token;
	}
	
	/**
	 * Extract a Username from the cookie string
	 * @param content the content of the cookie
	 * @return the user name
	 */
	public static String getUserNameFromCookieContent(String content)
	{
		return content.split(":",2)[0];
	}
	
	/**
	 * Extract a Token from the cookie string
	 * @param content the cookie string
	 * @return the token 
	 */
	public static String getTokenFromCookieContent(String content)
	{
		return content.split(":",2)[1];
	}
}
