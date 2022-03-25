package fi.abo.kogni.soile2.http_server;



import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;


public class ServerVerticle extends AbstractVerticle {

//		 vertx.createHttpServer().requestHandler(req -> {
  @Override
  public void start(Promise<Void> startPromise) throws Exception {
		DeploymentOptions opts = new DeploymentOptions();
		vertx.deployVerticle("js:dustHandler.js", opts);		

	Router router = Router.router(vertx);
	router.get("template").handler(this::handleTemplate);
    vertx.createHttpServer().requestHandler(router).requestHandler(req -> {
      req.response()
        .putHeader("content-type", "text/plain")
        .end("Hello from Vert.x!");
    }).listen(8888, http -> {
      if (http.succeeded()) {
        startPromise.complete();
        System.out.println("HTTP server started on port 8888");
      } else {
        startPromise.fail(http.cause());
      }
    });
  }
  
  public void handleTemplate(RoutingContext ctx)
  {
  	System.out.println("Handling Template request");
  	vertx.eventBus().request("dust.compile",new JsonObject().put("name","test").put("source","Hello {x}!"),reply ->
  	{
  		if(reply.succeeded())
  		{
  			vertx.eventBus().request("dust.render", new JsonObject().put("name","test").put("context",new JsonObject().put("x" , "world")), reply2 ->
  			{
  				if(reply2.succeeded())
  				{
  					ctx.request().response().end((String) reply2.result().body());
  				}
  				else
  				{
  				  	System.out.println("Couldn't get response from dust" + reply2.cause());
  				}
  			});
  		}
  		else
  		{
  		System.out.println("Couldn't get response from dust" + reply.cause());
  		
  		}  		
  	});
  	

  }
  
}
