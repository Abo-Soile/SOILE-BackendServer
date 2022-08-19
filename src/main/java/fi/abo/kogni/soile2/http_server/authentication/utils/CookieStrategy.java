package fi.abo.kogni.soile2.http_server.authentication.utils;

public class CookieStrategy {

	public static String buildCookieContent(String username, String token)
	{
		return username + ":" + token;
	}
	
	public static String getUserNameFromCookieContent(String content)
	{
		return content.split(":",2)[0];
	}
	
	public static String getTokenFromCookieContent(String content)
	{
		return content.split(":",2)[1];
	}
}
