package fi.abo.kogni.soile2.http_server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
/**
 * This class contains all routing mechanisms i.e. handles access control 
 * @author thomas
 *
 */
public class SoileHTTPRoutes {
	static final Logger LOGGER = LogManager.getLogger(SoileHTTPRoutes.class);

	private Router router;
	/**
	 * Constructor for routes using a given router from the server.
	 * @param router
	 */
	public SoileHTTPRoutes(Router router)
	{		
		this.router = router;
	}
	
	public void setupAuthTestRoute()
	{
		router.get("/test/auth").handler(this::testAuth);	
	}
	
	public void testAuth(RoutingContext ctx)
	{
		LOGGER.debug("AuthTest got a request");
		if(ctx.user() != null)
		{
			ctx.request().response()
			.putHeader(HttpHeaders.CONTENT_TYPE, "text/html; charset=utf-8")
			.end("<html><body><h1>Authenticated as " + ctx.user().principal().getString("username") + "</h1></body></html>");
		}
		else
		{
			ctx.request().response()
			.putHeader(HttpHeaders.CONTENT_TYPE, "text/html; charset=utf-8")
			.end("<html><body><h1>Not Authenticated</h1></body></html>");	
		}
	}
}
