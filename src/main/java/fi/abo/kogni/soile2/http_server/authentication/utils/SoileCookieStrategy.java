package fi.abo.kogni.soile2.http_server.authentication.utils;

/**
 * The Soile Cookie building strategy, i.e. the strategy on how to extract data from the cookie and build it.
 * @author Thomas Pfau
 *
 */
public class SoileCookieStrategy {

	/**
	 * Build a Cookie String from Username and token.
	 * @param username
	 * @param token
	 * @return
	 */
	public static String buildCookieContent(String username, String token)
	{
		return username + ":" + token;
	}
	
	/**
	 * Extract a Username from the cookie string
	 * @param content
	 * @return
	 */
	public static String getUserNameFromCookieContent(String content)
	{
		return content.split(":",2)[0];
	}
	
	/**
	 * Extract a Token from the cookie string
	 * @param content
	 * @return
	 */
	public static String getTokenFromCookieContent(String content)
	{
		return content.split(":",2)[1];
	}
}
