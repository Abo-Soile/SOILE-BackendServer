var vertx = require("vertx");
var console = require('vertx/console');
var mongo = require("mongoHandler");
var utils = require("utils");

var userDAO = require("models/DAObjects").UserDAO;
var User = require("models/Models").User;

var sessionMap = vertx.getMap("soile.session.map");

var sessionManager =  {

  cookies: null,
  request: null,
  sessionTime: 1000*60*60*24,
  //sessionTime: 20000,

  loadManager: function(request) {
    this.cookies = request.headers().get("Cookie");
    this.request = request;
    this.currentUser = false;

    if (typeof this.cookies == "undefined") {
      this.cookies = "";
    }

    if(this.loggedIn()) {
      this.currentUser = new User(this.loggedIn());
    }

    return this;
  },

  reloadUser: function(request) {
    if(this.loggedIn()) {
      this.currentUser = new User(this.loggedIn());
    }
  },

  createCookie: function(name, value, days) {
    var expires = "";

    if(days) {
      var date = new Date();
      date.setTime(date.getTime()+(days*24*60*60*1000));
      expires = "; expires="+date.toGMTString();
    }

    //Adding cookie to memory so that it can be used instantly
    if (this.cookies != "") {
      this.cookies += ";";
    }
    this.cookies += name+"="+value+";";
    return name+"="+value+expires+"; path=/";
  },

  readCookie: function(name) {
    var nameEQ = name + "=";
    var i;

    //Dont do anything if no cookies exist
    if(!this.cookies) {
      return 0;
    }

    var ca = this.cookies.split(';');

    for(i=0;i < ca.length;i++) {
      var c = ca[i];
      while (c.charAt(0)==' ') {c = c.substring(1,c.length);}
      if (c.indexOf(nameEQ) == 0) {return c.substring(nameEQ.length,c.length);}
    }
    return 0;
  },

  eraseCookie: function(name) {
    var c = this.createCookie(name,"", -1);
    this.request.response.headers().add("Set-Cookie", c);
    //this.request.response.headers().set("Set-Cookie", c);
  },

  setPersonToken: function() {
    //console.log(this.readCookie("PersonToken"));
    if(!this.readCookie("PersonToken")) {
      var c = this.createCookie("PersonToken", utils.getRandomInt(0, 10000000000000000)+"a", 900);

      this.request.response.headers().add("Set-Cookie",c);
    }
  },

  getPersonToken: function() {
    return this.readCookie("PersonToken");
  },

  setSessionCookie: function(key) {
    var c = this.createCookie("Session", key, 1);
    this.request.response.headers().add("Set-Cookie", c);
    //this.request.response.headers().set("Set-Cookie", c);
  },

  getSessionCookie: function() {
    return this.readCookie("Session");
  },


  login: function(user) {
      console.log("----Logging in-----");
      //console.log(JSON.stringify(r));
      var sessionKey;

      if(user.sessiontoken) {
        sessionKey = user.sessiontoken;
      }
      else {
        sessionKey = java.util.UUID.randomUUID().toString();
      }

      console.log(this.getPersonToken());
      this.setSessionCookie(sessionKey);

      //Setting a new persontoken to avoid problems with clashing
      //tokens, doesn't work yet, only one cookie is set for some
      //reason
      this.eraseCookie("PersonToken");
      this.setPersonToken();

      var timerID = vertx.setTimer(this.sessionTime, function(timerID) {
        sessionMap.put(sessionKey, "");
      });

      var userObj = user.filter();

      userObj.timerID = timerID;

      delete userObj.password;

      sessionMap.put(sessionKey, JSON.stringify(userObj));
  },

  loggedIn: function() {
    var sessionData = sessionMap.get(this.getSessionCookie());
    if(!sessionData) {
      //Not logging in
      return false;
    }
    else {
      var data = JSON.parse(sessionData);
      this.renewSessionTimer(this.getSessionCookie(), data);
      return data;
    }

  },

  isAdmin: function() {
    var sessionData = sessionMap.get(this.getSessionCookie());
    // console.log("----Checking for admin----\n with session: " + this.getSessionCookie());
    // console.log(JSON.stringify(sessionData));
    if(!sessionData) {
      return false;
    }
    else {
      return this.currentUser.isAdmin();
      return JSON.parse(sessionData).admin;
    }
  },

  isEditor: function() {
    if (this.currentUser) {
      return this.currentUser.isEditor();
    } else {
      return false;
    }
  },

  isTestLeader: function() {
    if (this.currentUser) {
      return this.currentUser.isTestLeader();
    } else {
      return false;
    }
  },

  logout: function() {
    var data = this.loggedIn();
    if(data) {
      console.log("Logging out");
      console.log(JSON.stringify(data));
      vertx.cancelTimer(data.timerID);
      sessionMap.put(this.getSessionCookie(), "");

      this.eraseCookie("PersonToken");
      this.setSessionCookie("");

      //console.log(JSON.stringify(this.request.response.headers()));

    }
   },

  checkSession: function(callback) {
    var session = this.getSessionCookie();

    userDAO.fromSession(session, function(user) {
      console.log("---------Checking session--------");
      console.log(JSON.stringify(user));

      if (user == "") {
        callback(false);
      }
      else {
        callback(user);
      }
    });
  },

  renewSessionTimer: function (sessionID, sessionData) {
    vertx.cancelTimer(sessionData.timerID);

    sessionData.timerID = vertx.setTimer(this.sessionTime, function(timerID) {
        console.log("Killing session " + sessionData.username);
        sessionMap.put(sessionID, "");
    });

    sessionMap.put(sessionID, JSON.stringify(sessionData));

  },
  /*
    Returns userid, or usertoken, depending if the user is logged in or not
  */
  getUserId: function() {
    var userID = this.getPersonToken();
    if(this.loggedIn()) {
      userID = this.loggedIn()._id;
    }
    return userID;
  }

};

module.exports = sessionManager;