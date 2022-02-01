var vertx = require('vertx');
var console = require('vertx/console');
var utils = require('utils');

var container = require('vertx/container');
var config = container.config;


// TODO: Load this from config
var mongoAddress = "vertx.mongo-persistor";

var dataCollection = "data";

//Password hashing using SHA-256
function _hashPassword(password) {

  var messageDigest = java.security.MessageDigest.getInstance("SHA-256");
  var jpass = new java.lang.String(password);

  var bytes = messageDigest.digest(jpass.getBytes());

  var hexString = java.math.BigInteger(1, bytes).toString(16);

  console.log(hexString);

  return hexString;
}

var currentDate = new Date();
var millisecondsPerDay = 1000*3600*24;

/*
Comparing start and end dates to calculate if an experiment 
should be active or not */
function _isActive(experiment) {
  var sDate = new Date(experiment.startDate);
  var eDate = new Date(experiment.endDate);

  if((sDate < currentDate)&&(currentDate<eDate)) {
    experiment.active = true;
    experiment.timedata = Math.ceil((eDate - currentDate)/millisecondsPerDay);
  }
  else{
    experiment.active = false;
    if(sDate > currentDate) {
      experiment.timedata = Math.ceil((sDate - currentDate)/millisecondsPerDay);
    }
  }

  //console.log("IS ACTIVE RUNNING");
  // Experiment is inactive if no components exits
  if(!experiment.hasOwnProperty("components")) {
    //console.log("components doesn't exist");
    experiment.active = false;
  }
  else {
    if (experiment.components.length == 0) {
      //console.log("components is empty");

      experiment.active = false;
    }
  }

  return experiment;
}

/*
Checks if at least two consequent componentes are set to random.
*/
function _isRandom(experiment) {
  var longestRandom = 0;
  var prevRandom = false;

  if (typeof experiment.components === 'undefined') {
    return false;
  } 

  for (var i = 0; i < experiment.components.length; i++) {
    if(experiment.components[i].random) {
      longestRandom +=1;
      if (longestRandom > 1) {
        return true;
      }  
    }
    else {
      longestRandom = 0;
    } 
  }

  return false;
}


var mongoHandler = {
  mongoAddress: "vertx.mongo-persistor",
  init: function(){
    this.setIndexes();
    this.ensureAdmin();
  },

  //Function that sets all indexes at startup
  setIndexes: function() {
    vertx.eventBus.send(mongoAddress, {"action": "command",
    "command":
      "{eval: 'function() {db.users.ensureIndex({username:1}, {unique: true});}', args: []}" }, 
      
      function(reply) {
        console.log("Setting user index");
        console.log(JSON.stringify(reply));
      });
  },

  ensureAdmin: function() {

    var username = config.adminuser;
    var pass = _hashPassword(config.adminpassword);
    console.log("Initializing admin:" + config.adminuser + " - " + config.adminpassword);
    vertx.eventBus.send(mongoAddress, {"action":"save",
    "collection":"users", "document":{"_id": 1,
                                      "username":username,
                                      "password": pass,
                                      "admin":true,
                                      "superuser":true,
                                      "org":"standard",
                                      "role":"admin"
                                       }},
    function(reply) {
      console.log("Generated admin");
    });
  }
};

var user = {
  new: function(username, password, response) {

    var pass = _hashPassword(password);

    vertx.eventBus.send(mongoAddress, {"action": "save",
    "collection":"users", "document":{"username":username, "password":pass, "admin":false}},
     function(reply) {
      response(reply);
     });
  },

  get: function(userid, response) {
    vertx.eventBus.send(mongoAddress, {"action":"findone",
      "collection":"users","matcher":{"_id":userid}},
      function(reply) {
        //console.log(JSON.stringify(reply));
        response(reply.result);
      });
  },

  delete: function(userid, callback) {
    vertx.eventBus.send(mongoAddress, {"action":"delete",
      "collection":"users","matcher":{"_id":userid}},
      function(reply) {
        //console.log(JSON.stringify(reply));
        callback(reply.result);
      });
  },

  list: function(isAdmin, response) {
      vertx.eventBus.send(mongoAddress, {"action":"find",
        "collection":"users","matcher":{"admin":isAdmin}},
        function listUserResponse (reply) {
          response(reply);
        });
  },

  getWithToken: function(token, response) {
    vertx.eventBus.send(mongoAddress, {"action":"findone",
      "collection":"users", "matcher":{"forgottenPasswordToken":token}},
      function(reply) {
        response(reply);
      });
  },

  update: function(userid, firstname, lastname, address1,
                      address2,postalcode, city, country, response) {
    vertx.eventBus.send(mongoAddress, {"action":"update",
        "collection":"users", "criteria": {"_id":userid},
        "objNew":{"$set":{
          "firstname": firstname,
          "lastname": lastname,
          "address1":address1,
          "address2": address2,
          "postalcode": postalcode,
          "city": city,
          "country": country
        }
      }
    },function(reply) {
      response(reply);
    });
  },

  auth: function(username, password, remember,response) {

    var pass = _hashPassword(password);
    var token = java.util.UUID.randomUUID().toString();

    vertx.eventBus.send(mongoAddress, {"action":"findone",
    "collection":"users", "matcher":{"username":username, "password":pass}},
    function(reply) {

      console.log("Finding user");

      //No user found, incorrect credentials
      if(reply.result == null) {
        reply.status = "notfound";
        reply.token = false;
        response(reply);
      } 

      //Found a user
      else {
        //Generating and storing a sessioncookie in the db
        if(remember){
          console.log("Logging in with remember me");
          reply.token = token;

          vertx.eventBus.send(mongoAddress, {"action":"update",
            "collection":"users",
            "criteria": {
              "_id":reply.result._id
            },
            "objNew": {
              "$set": {
                "sessiontoken":token
              }
            }
          }, function(replyNested) {
            //console.log(JSON.stringify(replyNested));
            response(reply);
          });
        } 
        else {
          response(reply);
        }
      }
    });
  },

  /*Generates a token that can be used to access the password reset page*/
  forgotPassword: function(username, response) {
    var token = java.util.UUID.randomUUID().toString();

    vertx.eventBus.send(mongoAddress, {"action":"update",
      "collection": "users",
      "criteria": {"username":username},
       "objNew":{"$set":{
        "forgottenPasswordToken":token
      }},
      "multi":false
    }, function(reply) {
      console.log("Setting forgott password token for user: " + username + "to: " + token);
      reply.token = token;
      response(reply);
    });
  },

  resetPassword: function(token, password,response) {
    vertx.eventBus.send(mongoAddress, {"action":"update",
      "collection":"users",
      "criteria":{"forgottenPasswordToken":token},
      "objNew":{"$set":{
        "forgottenPasswordToken":"",
        "password":_hashPassword(password)
      }},
      "multi":false
    }, function(reply) {
      console.log("Setting new password using reset token");
      response(reply);
    });
  },

  fromSession: function(session, response) {
    vertx.eventBus.send(mongoAddress, {"action":"findone",
      "collection":"users",
      "matcher": {"sessiontoken":session}},
      function(reply) {
        response(reply);
      }
    );
  },

  //Userid, completed:true/false
  _getCompleteOrIncompleteExperiments: function(userID, completed,response) {
    //TestData
    console.log("LOADING EXP STATES : " + completed);

    vertx.eventBus.send(mongoAddress, {"action":"find",
      "collection":dataCollection,
      "matcher": {"userid":userID,
                  "confirmed": completed,
                  "phase": "0" }},
      function(replyData) {
        //Formdata
        var expIDs = [];
        for(var i = 0; i<replyData.results.length; i++) {
          expIDs.push(replyData.results[i].expId);
          console.log("Pushing formid: " + replyData.results[i].expId + " i: " + i);
        }
        console.log(JSON.stringify(expIDs));

        vertx.eventBus.send(mongoAddress, {"action":"find",
          "collection":"experiment",
          "matcher": {"_id": {"$in":expIDs}}
          }, 
          function(exps) {
            exps.list = expIDs;
            response(exps);
          }
        );
      }
    );
  },

  //Returns a list of all completed and incompleted experiments for the current user. And a
  //array with their ID's
  _experimentStatus: function(userID, res) {
    //Incomplete experiments
    user._getCompleteOrIncompleteExperiments(userID, false,function (incompleted) {

      //Completed experiments
      user._getCompleteOrIncompleteExperiments(userID, true, function (completed) {
       
        var idList = incompleted.list.concat(completed.list);
        res({completed:   completed.results,
             incompleted: incompleted.results, 
             expList:     idList
           });
      });
    }) ;
  },

  status: function(userID, response) {

    this._experimentStatus(userID, function(r) {
      //console.log(JSON.stringify(r));
      Experiment.list(r.expList, function(r2) {

        response({newExps:r2.results, complete: r.completed, incomplete:r.incompleted});
      });
    }); 
  }

};


var Experiment = {

  get: function(id, response) {
    currentDate = new Date();
    vertx.eventBus.send("vertx.mongo-persistor",{"action":"findone", 
   "collection":"experiment","matcher":{"_id":id}},function(reply){

      if(reply.result) {
        reply.result = _isActive(reply.result);
        reply.result.israndom = _isRandom(reply.result); 

      }
      response(reply);

    });
  },

  delete: function(id, callback) {
    vertx.eventBus.send("vertx.mongo-persistor",
      {"action":"delete", 
       "collection":"experiment","matcher":{"_id":id}},
       function(reply){
      callback(reply);
    });
  },

  phaseCount: function(id,response) {
    //{ distinct: "orders", key: "item.sku" }
    var dataCommand = "{distinct:'"+dataCollection+"', key:'phase', query: {expId:'"+id+"'}}";

    vertx.eventBus.send(mongoAddress, {"action":"command",
      "command":dataCommand}, function(reply) {
        response(reply);
      }
    );

  },
  /*
    Saves data and updated the user's position in the test. 
  */
  saveData: function(phase, experimentid ,data, duration, score,userid, callback) {
    var doc = {};
    doc.phase = phase;
    doc.expId = experimentid;
    doc.userid = userid;
    doc.confirmed = false;
    doc.duration = duration;
    doc.data = data;
    doc.score = score;

    var timeStamp = new Date();
    doc.timestamp = timeStamp.toISOString();

    function save(document){

      if(typeof document.phase !== 'number') {
        document.phase =  parseInt(document.phase);
      }

      vertx.eventBus.send(mongoAddress, {"action":"save",
      "collection":dataCollection, "document":document}, function(reply) {

        vertx.eventBus.send(mongoAddress, {"action":"update", 
          "collection":dataCollection, "criteria":{
            "expId":document.expId,
            "userid":document.userid,
            "type":"general"
          },
          objNew : {
            $inc: {
              position:1
            } 
          },
        }, function(reply2) {
           callback(reply);
        });
      });
    }
    
    this.get(experimentid, function(r) {
      var exp = r.result;
      var type = r.result.components[phase].type;
      doc.type = type;

      Experiment.getUserData(userid, experimentid, function(userdata) {
        if (exp.israndom) { 
          doc.phase = userdata.randomorder[phase];
        }
        console.log("CURRENT PHASE: " + doc.phase);
        Experiment.userCompletedPhase(userid, experimentid, doc.phase, function(shouldProceed) {
          if(shouldProceed){
            save(doc);
          } else {
            callback(r);
          }
        });
      });
    });
  },

  setRandom: function(expId, component, value, response) {
    var qString = {};
    qString["components."+component+".random"] = value;
    var query = {
      "action":"update",
      "collection":"experiment",
      "criteria":{
        "_id":expId,
      },
      "objNew":{"$set":qString}
    };

    vertx.eventBus.send(mongoAddress, query, function(reply) {
        console.log(JSON.stringify(reply));
        response(reply);
    });
  },

  generateRandomOrder: function(exp) {
    var randomList = [];
    var randomMapping = [];
    var randomGroups = [0,0,0,0,0,0,0,0,0,0];

    for (var i = 0; i < exp.components.length; i++) {
      if (exp.components[i].random > 0) {
        randomList[i] = exp.components[i].random;
        randomGroups[exp.components[i].random] = 1;
      } else {
        randomList[i] = 0;
      }
      randomMapping[i] = i;
    }


   /* console.log(JSON.stringify(randomList))
    console.log(JSON.stringify(randomMapping))
    console.log(JSON.stringify(randomGroups))
*/
    randomGroups[0] = 0;
    var startRandomSequence = null;

    function randomizePart(arrSlice, index) {
      arrSlice = utils.shuffle(arrSlice);
      for (var i = 0; i < arrSlice.length; i++) {
        randomList[i + index] = arrSlice[i];
        randomMapping[i + index] = arrSlice[i];
      }
    }

    function randomizeGroup(array, groupMapping, groupNo) {
      var tempArr = [];
      for (var i = 0; i < array.length; i++) {
        if(groupMapping[i]===groupNo) {
          tempArr.push(array[i]);
          array[i] = null;
        }
      }

      //console.log(JSON.stringify(array))
      tempArr = utils.shuffle(tempArr);

      for (var j = 0; j < array.length; j++) {
        if(array[j] === null) {
          array[j] = tempArr.pop();
        }
      }

      return array;

    }
/*
    for(var i = 0; i < randomList.length; i++) {
      if ((randomList[i] || randomList[i] === 0) 
        && startRandomSequence === null) {
        startRandomSequence = i;
      }
      else if (startRandomSequence && (!(randomList[i]) || i==randomList.length-1)) {
        var slice = i
        if (i==randomList.length - 1) {
          slice += 1
        }
        randomizePart(randomList.slice(startRandomSequence, slice), startRandomSequence)
        startRandomSequence = null;
      }
    }
    */
    for (var i = 0; i < randomGroups.length; i++) {
      if(randomGroups[i]===1) {
        randomMapping = randomizeGroup(randomMapping, randomList, i);
      }
    }
    return randomMapping;
  },


  // Setting a confirmed flag on submitted data. 
  // This is run when an user successfully reaches the end
  // of an experiment.
  confirmData: function(expId, userid, response) {

    vertx.eventBus.send(mongoAddress, {"action":"update",
    "collection":dataCollection, "criteria":{"expId":expId, "userid":userid}, 
    "objNew":{"$set":{
        "confirmed":true
      }},
    "multi":true
    }, function(reply) {
      //console.log("confirming data");
      //console.log(JSON.stringify(reply));
      response(reply);   
    });
  },

  formData: function(id, response) {
    vertx.eventBus.send(mongoAddress, {"action":"find",
    "collection":dataCollection,
    "matcher":{"expId":id, "confirmed":true, "type":"form", "deleted": {$in: [null, false]}},
    "keys": {"confirmed":0},  // Projection
    "sort":{"phase":1, "timestamp":1},
    //TODO: Handle larger replies.
    "batch_size":10000
    }, 
     function(reply) {
      Experiment.phaseCount(id, function(phases) {
        reply.phases = phases.result.values;
        response(reply);
      });
    });
  },

  testData: function(id, response) {
    vertx.eventBus.send(mongoAddress, {"action":"find", 
      "collection":dataCollection,
      "matcher": {"expId":id, "confirmed":true, "type":"test", "deleted": {$in: [null, false]}},
      "keys": {"confirmed": 0, "data.rows":0}, // Projection
      "sort": {"phase":1, "timestamp":1}, 
      //TODO: Handle larger replies.
     "batch_size":10000
      },   
      function(reply) {
        response(reply);
      }
    );
  },

  rawTestData: function(expId, phase, response) {
    phase = parseInt(phase);
    vertx.eventBus.send(mongoAddress, {
        "action":"find",
        "collection":dataCollection,
        "matcher":{"expId":expId, "phase":phase, "confirmed":true, "deleted": {$in: [null, false]}},
        "keys": {"data.single":0},
        //TODO: Handle larger replies.
        "batch_size":10000
      },
      function(reply) {
        response(reply);
      }
    );
  },

  addForm: function(expid,formid, name,response) {
    vertx.eventBus.send(mongoAddress, {
      "action":"update",
      "collection":"experiment",
      "criteria":{
        "_id":expid
      },
      "objNew": {
        "$push":{
          "components":{
            "id":formid,
            "name":name, 
            "type":"form"
          }
        }
      }
    }, function(reply){
      //console.log(JSON.stringify(reply))
      response(reply);

    });
  },

  addTest: function(expid,testid, name,response) {
    vertx.eventBus.send(mongoAddress, {
      "action":"update",
      "collection":"experiment",
      "criteria":{
        "_id":expid
      },
      "objNew": {
        "$push":{
          "components":{
            "id":testid,
            "name":name, 
            "type":"test"
          }
        }
      }
    }, function(reply){
      //console.log(JSON.stringify(reply))
      response(reply);

    });
  },

/*
Mongo example that should work

db.experiment.update({_id:"c2aa8664-05b7-4870-a6bc-68450951b345",
"components.id":"59cecd81aca2c289942422d904ef495dfc21a6a3"},
{$set:{"components.$.name":"MY new name"}})
*/
  editFormName: function(expid, formid, name, response) {

    var query = {
      "action":"update",
      "collection":"experiment",
      "criteria":{
        "_id":expid,
        "components.id":formid
      },
      "objNew":{"$set":{"components.$.name":name}}
      };
    //var command = "db.experiment.update({'_id':'"+expid+"','components.id':'"+formid+"'},{$set:{'components.$.name':'"+name+"''}})";
    // console.log("\n"+command+"\n");
    vertx.eventBus.send(mongoAddress, query, function(reply){
      response(reply);
    });
  },

  // http://stackoverflow.com/questions/4588303/in-mongodb-how-do-you-remove-an-array-element-by-its-index
  // The above method could also be used 
  deleteComponent: function(expid, compid, response) {

    var query =  {
      "action":"update",
      "collection":"experiment",
      "criteria":{
        "_id":expid,
        "components.id":compid
      },
      "objNew":{"$pull":{"components":{"id":compid}}}
    };
    

    vertx.eventBus.send(mongoAddress, query, function(reply) {
      response(reply);
    });
  },

  deleteComponentByIndex: function(expid, index, response) {

    var comp = {};
    comp["components."+index] = 1;
    console.log(comp);

    //Set the component to null
    var query1 =  {
      "action":"update",
      "collection":"experiment",
      "criteria":{
        "_id":expid
      },
      "objNew":{"$unset":comp}
    };

    //Pull all null components
    var query2 = {
      "action":"update",
      "collection":"experiment",
      "criteria":{
        "_id":expid
      },
      "objNew":{"$pull":{"components": null}}
    };

    console.log("Deleting by component index");

    vertx.eventBus.send(mongoAddress, query1, function(reply1) {
      //console.log(JSON.stringify(reply1));
      vertx.eventBus.send(mongoAddress, query2, function(reply2) {
        //console.log(JSON.stringify(reply2));
        response(reply2);
       });
    });
  },

  //Returns all active experiments not in the ignore list
  list: function(ignore, response) {
    vertx.eventBus.send("vertx.mongo-persistor",{"action":"find",
    "collection":"experiment",
    "matcher": {"_id": {"$nin":ignore}}},
    function(reply){
      if(reply.results) {
        for(var i =0; i<reply.results.length;i++) {
          reply.results[i] = _isActive(reply.results[i]);
        }
      }
      response(reply);
    });
  },

  save: function(exp,response){
    exp.deleted = false;
    vertx.eventBus.send(mongoAddress, {"action":"save", 
      "collection":"experiment", "document":exp}, function(reply){
        response(reply);
      });
  },

  update: function(exp, id, response){
    vertx.eventBus.send(mongoAddress, {"action":"update", 
      "collection":"experiment", "criteria":{"_id":id},
      "objNew":{"$set":exp}}, function(reply){
        response(reply);
      });
  },

/*
  Returns the users current position in the experiment.
  Is done by selecting the latest stored data and checking its phase.
  So the phase to be displayed is latestdata.phase + 1.
*/
  userPosition: function(userid, experimentid, callback) {

    this.getUserData(userid, experimentid, function(userdata){
        /*
         Getting fetching the latest stored data fails when a
         random order goes from a bigger phase to a smaller on, eg 
         3 -> 2, since 3 will always be fetched even if 2 is 
         completed as well-
        */

        /*console.log("------Latest userData-----");
        console.log(JSON.stringify(userdata));*/
        var currentPhase = -1;
        var nextPhase = -1;
        var ran = 0;

        callback(userdata);
      }
    );
  },

  getUserData: function(userid, experimentid, callback) {
    vertx.eventBus.send(mongoAddress, {
      "action":"findone",
      "collection":dataCollection,
      "matcher":{
        "userid":userid,
        "expId":experimentid,
        "type":"general"
        /*"type":"$not('general')"*/
      }},
      function(data) {
        if (data.status === "error" ||
            typeof data.result === "undefined") {
          return callback(false);
        }else {
          callback(data.result);
        }
      }
    );
  },

  /*Returns true if user has completed this phase already*/
  userCompletedPhase: function(userid, experimentid, phase,callback) {
    console.log("SHOULD PROCEED: " + experimentid + "   p:" + phase + "  u: " + userid);
    vertx.eventBus.send(mongoAddress, {
      "action":"findone",
      "collection":dataCollection,
      "matcher":{
        "userid":userid.toString(),
        "expId":experimentid,
        "phase":parseInt(phase)
      }},
      function(data) {
        if (data.status === "error" ||
            typeof data.result === "undefined") {
          return callback(true);
        }else {
          callback(false);
        }
      }
    );
  },

  initUserData: function(data, userid, experimentid, callback) {
    data.userid = userid;
    data.expId = experimentid;
    data.type = "general";

    var timeStamp = new Date();
    data.starttime = timeStamp.toISOString();

    vertx.eventBus.send(mongoAddress, {
      "action":"save",
      "collection":dataCollection,
      "document":data
    },function(reply) {
      callback(reply);
    });
  },

  _randomToRealPhase: function(randomOrder, phase) {
    if (phase == -1) {
      return -1;
    }
    for (var i = 0; i < randomOrder.length; i++) {
      if(randomOrder[i] == phase) {
        return i;
      }
    }
    return 0;
  },

  //Refreshes the userid on submitted data, useable when
  //a user logs in or registers in the middle of an experiment
  updateDataIdentifier: function(userid, personToken,response) {

    vertx.eventBus.send(mongoAddress, {"action":"update",
      "collection":dataCollection,
      "criteria": {
        "userid":personToken,
        "confirmed":false
      },
      "objNew": {
        "$set":{
          "userid":userid
        }
      },
      "multi": true
    },
    function(reply) {
      //console.log(reply);
      console.log("Identifying persontoken: " + personToken + " as user " + userid);
      response(reply);
    });
  },


  countParticipants: function(experimentid, response) {

    var total = 0;
    var confirmed = 0;

    console.log("Counting participants");

    vertx.eventBus.send(mongoAddress, 
      {"action":"count",
       "collection":dataCollection,
       "matcher":{"expId":experimentid,
                  "confirmed":true,
                  "type":"general",
                  "deleted": {$in: [null, false]}
                }
      },
      function(reply) {
        console.log("Confirmed " + reply.count);
        confirmed = reply.count;

        //Found confirmed, first phase is test
         vertx.eventBus.send(mongoAddress, 
          {"action":"count",
           "collection":dataCollection,
           "matcher":{"expId":experimentid, "type":"general"}
          },function(reply2) {
            console.log("unconfirmed " + reply2.count);
            total = reply2.count;
            response({"confirmed":confirmed, 
                    "total": total});
          }
        );
      }
    );
  }
};
/*
  Tests as in visual tests
*/
var Test = {

  get: function(id, response){
    vertx.eventBus.send(mongoAddress, {"action":"findone",
    "collection":"tests","matcher":{"_id":id}}, function(reply) {
      response(reply);
    });
  },

  delete: function(id, callback){
    vertx.eventBus.send(mongoAddress, {"action":"delete",
    "collection":"tests","matcher":{"_id":id}}, function(reply) {
      callback(reply);
    });
  },

  list: function(response){
    vertx.eventBus.send(mongoAddress, {"action":"find",
    "collection":"tests"}, function(reply) {
      response(reply);
    });
  },

  save: function(test,response) {
    test.compiled = false;
    test.deleted = false;
    if(test.name==="") {
      test.name = "Unnamed";
    }
    vertx.eventBus.send(mongoAddress, {"action":"save",
    "collection":"tests","document":test}, function(reply) {
      response(reply);
    });
  },

  update: function(test,response) {
    vertx.eventBus.send(mongoAddress, {"action":"update",
    "collection":"tests", "criteria":{"_id":test._id},
    "objNew":{"$set":{
        "code": test.code,
        "js": test.js,
        "compiled":test.compiled
      }
    }}, function(reply) {
      response(reply);
    });
  },

  editName: function(test, name, response) {

    var query = {
      "action":"update",
      "collection":"tests",
      "criteria":{
        "_id":test
      },
      "objNew":{"$set":{"name":name}}
      };
    //var command = "db.experiment.update({'_id':'"+expid+"','components.id':'"+formid+"'},{$set:{'components.$.name':'"+name+"''}})";
    // console.log("\n"+command+"\n");
    vertx.eventBus.send(mongoAddress, query, function(reply){
      response(reply);
    });
  }

};

//Refactoring Done
var Form = {

  get: function(id, callback){
    vertx.eventBus.send("vertx.mongo-persistor", {"action":"findone",
    "collection":"forms","matcher":{"_id":id}}, function(reply) {
      callback(reply);
    });
  },

  delete: function(id, callback){
    vertx.eventBus.send("vertx.mongo-persistor", {"action":"delete",
    "collection":"forms","matcher":{"_id":id}}, function(reply) {
      callback(reply);
    });
  },

  //Saves a form, does 
  save: function(form, id, callback) {
    vertx.eventBus.send("vertx.mongo-persistor",{"action":"save",
      "collection":"forms","document":{"form":form}}, function(reply){
        callback(reply);
      });
  }
};

module.exports.mongoHandler = mongoHandler;
module.exports.user = user;
module.exports.test = Test;
module.exports.form = Form;
module.exports.experiment = Experiment;