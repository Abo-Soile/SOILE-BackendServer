package fi.abo.kogni.soile2.utils;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.SimpleAuthenticationHandler;
import io.vertx.ext.web.handler.impl.SimpleAuthenticationHandlerImpl;

public interface SimpleAuthWRapper extends SimpleAuthenticationHandler{

	static SimpleAuthenticationHandler create() {
	    return new SimpleAuthImplWrapper();
	 }
	
	public class SimpleAuthImplWrapper extends SimpleAuthenticationHandlerImpl
	{
		public SimpleAuthImplWrapper()
		{
			super();
		}
		
		@Override
		public void authenticate(RoutingContext ctx, Handler<AsyncResult<User>> handler) {
			super.authenticate(ctx, userResult -> {
				
				if(userResult.failed())
				{
					handler.handle(Future.succeededFuture(User.fromName("FailedUser")));
				}
				else
				{
					handler.handle(userResult);
				}
			});
		}
	}
}


