package fi.abo.kogni.soile2.http_server.authentication;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.CookieSameSite;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.sstore.SessionStore;

public class SoileSessionHandler implements SessionHandler {

	
	public SoileSessionHandler(SessionStore store)
	{
		
	}
	@Override
	public void handle(RoutingContext event) {
		// TODO Auto-generated method stub

	}

	@Override
	public SessionHandler setSessionTimeout(long timeout) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SessionHandler setNagHttps(boolean nag) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SessionHandler setCookieSecureFlag(boolean secure) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SessionHandler setCookieHttpOnlyFlag(boolean httpOnly) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SessionHandler setSessionCookieName(String sessionCookieName) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SessionHandler setSessionCookiePath(String sessionCookiePath) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SessionHandler setMinLength(int minLength) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SessionHandler setCookieSameSite(CookieSameSite policy) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SessionHandler setLazySession(boolean lazySession) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SessionHandler setCookieMaxAge(long cookieMaxAge) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SessionHandler flush(RoutingContext ctx, Handler<AsyncResult<Void>> handler) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SessionHandler flush(RoutingContext ctx, boolean ignoreStatus, Handler<AsyncResult<Void>> handler) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SessionHandler setCookieless(boolean cookieless) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Session newSession(RoutingContext context) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Future<Void> setUser(RoutingContext context, User user) {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public Future<Void> flush(RoutingContext ctx, boolean ignoreStatus) {
		// TODO Auto-generated method stub
		return null;
	}

}
