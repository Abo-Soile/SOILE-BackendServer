var vertx = require('vertx');
var container = require('vertx/container');
var console = require('vertx/console');

var templateManager = require('templateManager');
var sessionManager = require("sessionManager");

var logger = container.logger;
var config = container.config;

var router = new vertx.RouteMatcher();

var bindHost = config.bindhost;

var portToUse = config["port"]
if (config["externalport"]) {
  portToUse = config["externalport"]
}

function swapUrlPort(url, newPort) {
  var uriRegex = /([a-zA-Z+.\-]+):\/\/([^\/]+):([0-9]+)\//;

  return url.replace(uriRegex, "$1://$2:" + newPort + "/");
}

function logHttp(request) {
  var method = request.method();
  var url = request.absoluteURI();
  var remoteAddress = request.remoteAddress().getHostString();
  var userAgent = request.headers().get("User-Agent");

  if (request.headers().contains("X-Real-IP")) {
    remoteAddress = request.headers().get("X-Real-IP");
  }

  logger.info("HTTP " + method + "--" + remoteAddress + "  " + url + " Agent:" + userAgent);
}

//
// Adds some usefull functions to the request object
function extendRequest(request, func) {

  if (request.response) {
    request.response.json = function(data) {
      request.response.putHeader("Content-Type", "application/json; charset=UTF-8");
      request.response.end(JSON.stringify(data));
    }
  }
  /**
   * Returns full external URI using either absouluteURI,
   * or by building it using the externalURI configuration
   */
  request.absoluteExternalURI = function() {
    var URI = this.absoluteURI().toString();
    URI = swapUrlPort(URI, portToUse);

    if (!config["externalURI"]) {
      return URI;
    }

    if (bindHost) {
      URI = URI.replace(bindHost, config["externalURI"])
    } else {
      URI = URI.replace(config["host"], config["externalURI"])
    }

    return URI;
  }

  request.redirect = function(url) {
    console.log("Redirecting to " + url);
    console.log(this.remoteAddress());

    this.response.statusCode(302);
    this.response.putHeader('Location', url);
    this.response.end();
  };

  request.jsonRedirect = function(url) {
    console.log("JSON redirecting to " + url);

    var json = {};
    json.redirect = url;

    this.response.statusCode(200);
    this.response.putHeader("Content-Type", "application/json; charset=UTF-8");
    this.response.end(JSON.stringify(json));
  };

  request.unauthorized = function() {
    this.response.statusCode(401);

    var context = {};
    context.short = "Not authorized";
    context.long =  "You're not authorized to view this content. Try logging in";

    templateManager.render_template("error", context, this);
    //this.response.end("401, Unauthorized");
  };

  request.notfound = function() {
    this.response.statusCode(404);

    var context = {};
    context.short = "404, not found" ;
    context.long =  "The content you're looking for couldn't be found.";

    templateManager.render_template("error", context, this);
  };

  request.serverError = function() {
    this.response.statusCode(505);

    var context = {};
    context.short = "505, server error" ;
    context.long =  "Something went very wrong.";

    templateManager.render_template("error", context, this);
  };

  var session = sessionManager.loadManager(request);
  session.setPersonToken();
  request.session = session;

  //Check if a db session exists
  if((!session.loggedIn()) &&
      (session.getSessionCookie()) &&
      (request.method()==="GET" || request.method()==="POST")) {
    console.log("Checking session");

    /*
      If postrequest
      Run the handler, but defer the call to the endhandler until the session check has
      been completed.
    */
    if (request.method() === "POST") {
      console.log("DEFER END = TRUE")
      request.deferEndFunc = true;
      func(request);
    }

    // Timer used to debug issues with the session check
    //vertx.setTimer(3000, function(tid) {

    session.checkSession(function callback(user) {
      //Sending the session manager with the request
      console.log("user: " + user);
      console.log("user json: " + JSON.stringify(user));
      if(user) {
        console.log("Logging in from token");
        session.login(user);
        /*Reload manager when we have a user*/
        session = session.reloadUser();
      } else{
        console.log("No user found with toke: " + session.getSessionCookie());
      }

      if (request.method() === "POST") {
        console.log("Trying to call endhandler")
        request.deferEndFunc = false;
        request.callEndHandler();
      }
      else {
        func(request);
      }
    });
  }
  else {
    console.log("Skipping db session check");
    func(request);
  }

  logHttp(request);
}


function CustomMatcher() {
  this.routeMatcher = router;
  return;
}


// Handles arguments sent to the router to preserv backwards
// compatibility
CustomMatcher.prototype.handleArgs = function(arg) {
  var middleware = [];
  var handler = "";

  if(arg.length === 2) {
      handler = arg[1];
  }

  if(arg.length === 3) {
    middleware = arg[1];
    handler = arg[2];
  }

  return {h:handler, m:middleware};
};


//
// Runs middleware and
CustomMatcher.prototype.handleRequest = function(mCallback, middleware) {
  return function(request) {

    var callback = mCallback;

    if (request.method() == "POST") {
      request.origEndHandler = request.endHandler;
      request.deferEndFunc = false;

      console.log("POST Setting up new endhandler");

      /*
        Extending the endhandler so that we can defer the call to it, as well
        as calling it manually when something has been completed.
      */
      request.endHandler = function(func) {
        var that = this;

        console.log("setting up new endhandler")
        var endFunc = function() {

          console.log("CALLING ENFUNC -- deffer: " + that.deferEndFunc);
          if(!that.deferEndFunc) {
            console.log("ENDFUNC CALLED")
            callback = func;
            handleMiddleWare(that);
            //func();
          } else {
            console.log("DIDNT CALL ENDFUNC")
          }
        };
        that.origEndHandler(endFunc);

        that.callEndHandler = endFunc;
      };
    }

    function defMiddleware(req) {
      if(req.deferEndFunc) {
        console.log("deffering middleware");
        callback(req);
      } else {
        handleMiddleWare(req);
      }
    }

    function handleMiddleWare(req) {
            //One middleware
      if (typeof middleware === 'function') {
        middleware(req, function(req, err) {
          callback(req);
        });
      }

      //Multiple middleware
      else if (typeof middleware === 'object' && middleware.length > 0){
        var handleMiddlewareArray = function(n, request) {
          if(n < middleware.length) {

            console.log(JSON.stringify(middleware))
            console.log(typeof middleware[n] + " " + n + " type: " + typeof n);

            n = parseInt(n);

            middleware[n](req, function(newRequest) {
              handleMiddlewareArray(n+1, newRequest);
            });
          }else {
            callback(request);
          }
        };

        handleMiddlewareArray(0);
      }

      //No middleware
      else {
        callback(req);
      }
    }
    request = extendRequest(request, defMiddleware);

  };
};

//More methods from the routematcher should be implementd as needed.
CustomMatcher.prototype.get = function(pattern, handler) {
  var arg = this.handleArgs(arguments);
  this.routeMatcher.get(pattern, this.handleRequest(arg.h, arg.m));
  //routeMatcher.get(pattern, sessionTest(handler));
};

CustomMatcher.prototype.post = function(pattern, handler) {
  var arg = this.handleArgs(arguments);
  this.routeMatcher.post(pattern, this.handleRequest(arg.h, arg.m));

  //this.routeMatcher.post(pattern, sessionTest(handler));
};

CustomMatcher.prototype.delete = function(pattern, handler) {
  var arg = this.handleArgs(arguments);
  this.routeMatcher.delete(pattern, this.handleRequest(arg.h, arg.m));

  //this.routeMatcher.delete(pattern, sessionTest(handler));
};

CustomMatcher.prototype.allWithRegEx = function(pattern, handler) {
  var arg = this.handleArgs(arguments);
  this.routeMatcher.allWithRegEx(pattern, this.handleRequest(arg.h, arg.m));

  //this.routeMatcher.allWithRegEx(pattern, sessionTest(handler));
};

CustomMatcher.prototype.noMatch = function(handler) {
  //var arg = this.handleArgs(arguments);
  this.routeMatcher.noMatch(this.handleRequest(handler, []));
  //this.routeMatcher.noMatch(sessionTest(handler));
};

function a() {
  return new CustomMatcher()
}

//module.exports = CustomMatcher;
module.exports = function(){
  return new CustomMatcher();
};