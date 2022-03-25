package fi.abo.kogni.soile2.utils;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public class DebugRouter implements Handler<RoutingContext>{

	@Override
	public void handle(RoutingContext event) {
		System.out.println("Request URL: " + event.request().absoluteURI());
		System.out.println("Request is ssl: " + event.request().isSSL());
		System.out.println("Request Method is : " + event.request().method());
		event.next();
	}

}
