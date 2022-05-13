var vertx = require('vertx');
var container = require('vertx/container');
var console = require('vertx/console');

var Promise = require("mPromise");

var server = vertx.createHttpServer();

var config = container.config;
var shared_config = config.shared;
var port = config.port;
var host = config.host;
var bindHost = config.bindhost;

var testImages = config.directory + "/testimages";

var babyparser = require("libs/babyparse");
//var routeMatcher = new vertx.RouteMatcher();

var sessionMap = vertx.getMap("soile.session.map");

var logger = container.logger;

var messageDigest = java.security.MessageDigest.getInstance("SHA-256");

var userDAO = require("models/DAObjects").UserDAO;
var testDAO = require("models/DAObjects").TestDAO;
var experimentDAO = require("models/DAObjects").ExperimentDAO;
var trainingDataDAO = require("models/DAObjects").TrainingDataDAO;
var trainingDAO = require("models/DAObjects").TrainingDAO;

var userModel = require("models/Models").User;

var middle = require("middleware");

var a = new java.lang.String("sdfsdfs");
console.log(a.hashCode());
console.log(JSON.stringify(container.config));

var utils = require("utils");

// var lodash = require("./node_modules/lodash/index");

//var requireAdmin = utils.requireAdmin;
var requireAdmin = middle.requireAdmin;

function sendEmail(subject, body, address, func) {
  var mailAddress = "soile.my_mailer";

  var mail = {};
  mail.from = "kogni@abo.fi";
  mail.to = address;
  mail.subject = subject;
  mail.body = body;

  vertx.eventBus.send(mailAddress, mail, func);

}

/*
  Checking if a string seems to be an email address
*/
function looksLikeMail(str) {
    var lastAtPos = str.lastIndexOf('@');
    var lastDotPos = str.lastIndexOf('.');
    return (lastAtPos < lastDotPos &&  // @ before last .
      lastAtPos > 0 &&                 // Something before @
      str.indexOf('@@') === -1 &&       // No double @
      lastDotPos > 2 &&                // 3 chars before .com
      (str.length - lastDotPos) > 2);  // domain = min 2 chars
}

// Generates  a new customMatcher and sets it to routmatcher
// this matcher is then bound to de server object at the bottom
// of this file. The normal routematcher can also be called if
// needed.
//var routeMatcher = new CustomMatcher();

var customMatcher = require('router')();

// TODO: Load this from config
var DEBUG = true;   //This variable could stored in configs

var mongo = require('mongoHandler');
mongo.mongoHandler.init();

var templateManager = require('templateManager');
var mailManager = require('mailManager');

//Ugly hack to make sure that the template module is online before loading
//Base templates
var timerID = vertx.setTimer(3000, function() {
  console.log("\n ------Loading  templates------");
 // templateManager.load_template("header");
 // templateManager.load_template("footer");
  templateManager.loadAll();
});


var sessionManager = require("sessionManager");

//var sessionManager = require("sessionManager").sessionManager;

require('testroute.js');
require('training.js');

require('experiment.js');
require('experiment_old.js');
require('questionnaire.js');
require('test.js');
require('admin.js');

customMatcher.get("/login", function(request) {
  var previous = request.headers().get("Referer");

  templateManager.render_template('login', {"origin":previous},request);
});


customMatcher.post("/login", function(request) {
  var data = new vertx.Buffer();

  request.dataHandler(function(buffer) {
    data.appendBuffer(buffer);
  });

  request.endHandler(function() {

    var params = data.getString(0, data.length());
    params = utils.getUrlParams(params);

    var username = params.username;
    var password = params.password;
    var remember = params.remember;

    var origin = params.origin;
    var templateVars = {};

    login(username, password, remember).then(function(user) {
      console.log("Login success", user);
      request.session.login(user);
      if(origin){
        return request.redirect(decodeURIComponent(origin));
      }
      return request.redirect("/");
    }).catch(function(e) {
      // console.log("Error: " + e)
      templateVars.origin = decodeURIComponent(origin);
      templateVars.errors = e;

      templateManager.render_template('login', templateVars, request);
    });
  });
});

customMatcher.post("/login/json", function(request) {
  var data = new vertx.Buffer();

  request.dataHandler(function(buffer) {
    data.appendBuffer(buffer);
  });

  request.endHandler(function() {
    var params = JSON.parse(data.getString(0, data.length()));

    login(params.username, params.password, params.remember).then(function(user) {
      console.log("Login success", user);
      request.session.login(user);
      request.response.json({status:"ok"})
    }).catch(function(e) {
      request.response.json({status:"error", error:e})
    });
  });
});

function login(username, passwd, remember) {
  return new Promise(function(resolve, reject) {
    if (remember) {
      remember = true;
    }else {
      remember = false;
    }

    userDAO.auth(username, passwd, remember, function(user) {
      if (user) {
        resolve(user);
      } else { //No user was found, error
        reject("Wrong username or password")
      }
    });
  });
}

customMatcher.get("/login/forgotten", function(request) {
    templateManager.render_template("forgotten", {}, request);
});

customMatcher.get("/testeditor", function(request) {
    console.error("This is a test error");

    logger.trace("Trace Message!");
    logger.debug("Debug Message!");
    logger.info("Info Message!");
    logger.warn("Warn Message!");
    logger.error("Error Message!");
    logger.fatal("Fatal Message!");

    // Test some url related variables here
    logger.info("absoluteURI: " + request.absoluteURI());
    logger.info("uri: " + request.uri());
    logger.info("absoluteExternalUri: " + request.absoluteExternalURI());

    vertx.setTimer(300,function() {
      logger.info("Vertx timeout test");
    });
    request.response.end("Require editor");
});

customMatcher.post("/login/forgotten", function(request) {

  var data = new vertx.Buffer();

  request.dataHandler(function(buffer) {
    data.appendBuffer(buffer);
  });

  request.endHandler(function() {
    var params = data.getString(0, data.length());
    params = utils.getUrlParams(params);

    var username = params.username;

    mongo.user.forgotPassword(username, function(r) {
      console.log(JSON.stringify(r));

      var templateParams = {};
      templateParams.success = true;
      templateParams.email = username;

      var uri = request.absoluteURI() + "/" + r.token;

      //TODO: actually send the email
      mailManager.passwordReset(username, uri, function(r) {
        console.log("Reset mail sent to: " + username + " " + JSON.stringify(r));
        templateManager.render_template("forgotten", templateParams, request);

      });

    });
  });
});

customMatcher.get("/login/forgotten/:token", function(request) {
    var token = request.params().get('token');

    mongo.user.getWithToken(token, function(r) {
      console.log(JSON.stringify(r));
      if(!r.result) {
        request.notfound();
      }else {
        templateManager.render_template("resetpassword", {}, request);
      }
    });
});

customMatcher.post("/login/forgotten/:token", function(request) {
  var data = new vertx.Buffer();
  var token = request.params().get('token');

  request.dataHandler(function(buffer) {
    data.appendBuffer(buffer);
  });

  request.endHandler(function() {
    var params = data.getString(0, data.length());
    params = utils.getUrlParams(params);

    if (params.password === params.passwordAgain) {
      mongo.user.resetPassword(token, params.password, function(r) {
        console.log(r);
        templateManager.render_template("resetpassword",{"success":true}, request);
      });

    }
    else {
      templateManager.render_template("resetpassword",{"error":"The password didn't match, please try again"}, request);
    }
  });
});

customMatcher.get("/logout", function(request) {
  var uname = request.session.loggedIn();

  request.session.logout();

  //request.response.end("Logging user out " + JSON.stringify(uname));
  request.redirect("/");
});

customMatcher.get("/user", function(request) {
  var user = request.session.loggedIn();

  //request.response.end("Logging user out " + JSON.stringify(uname));
  templateManager.render_template("usersettings", {},request);
});

customMatcher.post("/user", function(request) {
  var data = new vertx.Buffer();

  request.dataHandler(function(buffer) {
    data.appendBuffer(buffer);
  });

  request.endHandler(function() {
    var params = data.getString(0, data.length());
    params = utils.getUrlParams(params);

    if (params.password === params.passwordAgain) {
      console.log(JSON.stringify(request.session.loggedIn()));
      userDAO.get(request.session.getUserId(), function(user) {
        user.setPassword(params.password);
        user.save(function() {
          templateManager.render_template("usersettings",{"success":true}, request);
        });
      });
    }
    else {
      templateManager.render_template("usersettings",{"error":"The password didn't match, please try again"}, request);
    }
  });
});

customMatcher.get('/signup', function(request) {
  templateManager.render_template('signup', {},request);
});

// function checkUsername(name) {
//   if (typeof name != "string") {
//     return false;
//   }

//   name = name.trim()
// }

function register(email, passwd, passwdAgain) {
  return new Promise(function(resolve, reject) {
    if(!(email && passwd && passwdAgain)) {
      return reject("All fields are required")
    }

    /*if(!looksLikeMail(email)) {
      templateVars.registererrors = "Enter a valid email address";
      templateManager.render_template('login', templateVars,request);
      return;
    }*/

    if((passwd !== passwdAgain)) {
      return reject("Password didn't match");
    }

    var newUser = new userModel();

    email =email.trim()
    if (email.indexOf(' ') !== -1) {
      return reject("Username contains invalid characters");
    }

    newUser.username = email;
    newUser.setPassword(passwd);

    userDAO.get({username:newUser.username}, function(existingUser) {

      /* Check for existing username */
      if (existingUser) {
        return reject("Username already exists!, try logging in")
      }

      newUser.save(function(result) {
        console.log("Trying to create new user");
        resolve(newUser)
      });
    });
  });
}

customMatcher.post("/signup", function(request) {
  var data = new vertx.Buffer();

  request.dataHandler(function(buffer) {
    data.appendBuffer(buffer);
  });

  request.endHandler(function() {

    var params = {};

    data = data.getString(0, data.length());
    params = utils.getUrlParams(data);

    var email = params.email;
    var passwd = params.passwd;
    var passwdAgain = params.passwdAgain;

    var origin = params.origin;

    var templateVars = {};
    templateVars.username = email;
    templateVars.origin = decodeURIComponent(origin);

    register(email, passwd, passwdAgain).then(function(user){
      request.session.login(user);
      if(origin){
        return request.redirect(decodeURIComponent(origin));
      }
      return request.redirect('/');
    }).catch(function(err){
      templateVars.registererrors = err;
      templateManager.render_template('login', templateVars,request);
    });

  });
});

customMatcher.post("/signup/json", function(request) {
  var data = new vertx.Buffer();

  request.dataHandler(function(buffer) {
    data.appendBuffer(buffer);
  });

  request.endHandler(function() {
    var params = JSON.parse(data.getString(0, data.length()));

    register(params.username, params.password, params.passwordAgain).then(function(user){
      console.log("Register promise resolve")
       request.session.login(user);
      return request.response.json({status:"ok"})
    }).catch(function(err){
      console.log("Register promise reject")
      console.log(err)
      return request.response.json({status:"error", error:err})
    });

  });
});


customMatcher.get('/users', requireAdmin, function(request){
  mongo.user.list(true, function(r) {
      var admins = r.results;
      console.log(JSON.stringify(r));
      templateManager.render_template("userList",{"users":admins}, request);
    });
});


/*
customMatcher.get('/questionnaire', function(req) {
  var file = config.directory.concat('/questionnaire.html');

  req.response.sendFile(file);
});



customMatcher.post('/questionnaire/render', function(request) {
  var body = new vertx.Buffer();

  request.dataHandler(function(buffer) {
    body.appendBuffer(buffer);
  });

  request.endHandler(function() {
    var address = utils.get_address('questionnaire_render');
    var eb = vertx.eventBus;

    // http://nelsonwells.net/2012/02/json-stringify-with-mapped-variables/#more-153
    var msg = {
      'markup': body.getString(0, body.length()),
      'action': "save"
    };

    eb.send(address, msg, function(reply) {
      var response = {};
      var id = '', link = '';

      if (reply.hasOwnProperty('error') === true) {
        response.error = reply.error;
      } else {
        id = reply.id;
        link = utils.build_url(host, port, '/questionnaire/generated/'.concat(id));
        response.id = id;
        response.link = link;
      }

      request.response.putHeader("Content-Type", "application/json; charset=UTF-8");
      request.response.end(JSON.stringify(response));
    });
  });
});


customMatcher.get('/questionnaire/generated/:id', function(request) {
  console.log(request.method);
  var id = request.params().get('id');
  var file = utils.build_path(utils.get_basedir(),
                              utils.get_directory('questionnaires'),
                              id);
  console.log(utils.build_path(utils.get_basedir(),utils.get_directory('questionnaires'),id));
  console.log(utils.get_basedir());
  vertx.fileSystem.readDir('', function(err, res){
    //console.log(err);
    var i;
    for (i = 0; i < res.length; i++) {
      console.log(res[i]);
    }
  });
  request.response.sendFile(file);
});


customMatcher.post('questionnaire/generated/:id', function(request) {
  console.log(request.method);
});
*/


customMatcher.post('/user', function(request) {
  var data = new vertx.Buffer();

  request.dataHandler(function(buffer) {
    data.appendBuffer(buffer);
  });

  request.endHandler(function() {

    var params = {};

    data = data.getString(0, data.length());
    params = utils.getUrlParams(data);

    var userid = request.session.loggedIn().id;
    var firstname  = params.firstname;
    var lastname   = params.lastname;
    var address1   = params.address1;
    var address2   = params.address2;
    var postalcode = params.postalcode;
    var city       = params.city;
    var country    = params.country;

    mongo.user.update(userid, firstname, lastname, address1, address2,
      postalcode, city, country, function(r) {
        console.log(r);
        return request.redirect("/");
      }
    );
  });
});


customMatcher.get('/', function(request) {
  // Admin showing admin controls
  // User logged in showing user controls
  if (request.session.loggedIn()) {
    var user = request.session.currentUser;
    var context = {};

    if (user.isTestLeader()) {

      var query = {};

      if (user.isEditor && !user.isAdmin()) {
        query = {users:user.username};
      }

      experimentDAO.list(query,function(experiments) {
        return templateManager.render_template('admin', {"experiments":experiments},request);
      });
    }
    else {
      trainingDataDAO.list({userId:user._id, type:"general"}, function(training) {


        var trainingIDs = [];
        for (var i = 0; i < training.length; i++) {
          trainingIDs.push(training[i].trainingId);
        }

        trainingDAO.list({_id:{$in:trainingIDs}}, function(tObjects) {
          for (var i = 0; i < training.length; i++) {
            for (var j = 0; j < tObjects.length; j++) {

              console.log("Compare id " + training[i].trainingId + "  " + tObjects[j]._id)

              if (training[i].trainingId == tObjects[j]._id) {
                training[i].name = tObjects[j].name;
              }
            }
          }

          context.training = training;
          templateManager.render_template('userV2', context, request);
        });
      });
    }
  }
  // Anonymous user, showing ladning page
  else {
    templateManager.render_template('landing', {} ,request);
  }

});


/*
  Matches static files. Uses the normal routmatcher so that session stuff is
  ignored when sending static files.
*/
customMatcher.routeMatcher.allWithRegEx('.*\.(html|htm|css|js|png|jpg|jpeg|gif|ico|md|wof|ttf|svg|woff|json|mp4|webm)$', function(request) {
  //logHttp(request);

  request.response.sendFile(utils.file_from_serverdir(request.path()));
});

/*
  Audio files
*/
customMatcher.routeMatcher.allWithRegEx('.*\.(mp3)$', function(request) {
  request.response.putHeader("Content-Type", "audio/mpeg");

//  var file = utils.file_from_serverdir(request.path());
//
//  console.log("Reading file: " + file);
//
//  vertx.fileSystem.readFile(file, function(err, res) {
//      if (!err) {
//        var len = res.length();
//        request.response.putHeader("Accept-Ranges","bytes");
//        request.response.putHeader("Content-Length", len);
//        request.response.putHeader("Content-Range","bytes 0-" + len +"/"+(len+1));
//        request.response.end(res);
//      }
//  });

  //Content-Range:bytes 0-1950826/1950827
  request.response.putHeader("Accept-Ranges","bytes");
  request.response.sendFile(utils.file_from_serverdir(request.path()));
});

customMatcher.allWithRegEx('.*/', function(req) {
  var url = req.uri().substring(0, req.uri().length - 1);

  console.log(url);

  req.response.statusCode(302);
  req.response.putHeader('Location', url);
  req.response.end()
;});

customMatcher.noMatch(function(request) {
  return request.notfound();
});

/* Let this be the last specified match. */
// customMatcher.allWithRegEx('.+', function(req) {
//   var file = http_directory.concat('/questionnaire.html');

//   req.response.sendFile(file);
// });

if (bindHost) {
  logger.info("STARTING WITH BINDHOST " + bindHost)
  server.requestHandler(customMatcher.routeMatcher).listen(port, bindHost);
} else {
  logger.info("STARTING WITH HOST" + host)
  server.requestHandler(customMatcher.routeMatcher).listen(port, host);
}
function vertxStop() {
  server.close();
}
