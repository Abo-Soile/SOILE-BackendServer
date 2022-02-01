var vertx = require("vertx");
var CustomMatcher = require('router');
var console = require('vertx/console');

var container = require('vertx/container');
var logger = container.logger;

var templateManager = require('templateManager');
var router = new CustomMatcher();

var trainingModel = require("models/Models").Training;
var trainingDAO = require("models/DAObjects").TrainingDAO;

var TrainingData = require("models/Models").TrainingData;

var formModel = require("models/Models").Form;

var trainingDataDAO = require("models/DAObjects").TrainingDataDAO;

var testDAO = require("models/DAObjects").TestDAO;
var formDAO = require("models/DAObjects").FormDAO;

var moment = require("libs/moment");

var requireAdmin = require('middleware').requireAdmin;
var requireEditor = require('middleware').requireEditor;

var m1 = require('middleware').m1;
var m2 = require('middleware').m2;

var utils = require("utils");

var babyparser = require("libs/babyparse");
var csvUtils = require("csvUtils");

var mailManager = require('mailManager');

var lodash = require("./node_modules/lodash/index");
var _ = lodash;

var Promise = require("mPromise");

// Promise demo
var p = new Promise(function(resolve, reject) {
  console.log("Setting up promise")
  vertx.setTimer(5000, function() {
    resolve("Success");
  });
});

p.then(function(res) {
  console.log("Promise " + res);
})

var A = function() {

    return new Promise(function(resolve, reject) {
        var result = 'A is done'

        console.log(result)
        resolve(result);
    })
}

var B = function() {

    return new Promise(function(resolve, reject) {
        var result = 'B is done'

        vertx.setTimer(2000,function() {
            console.log(result)
            resolve(result);
        })
    })
}

var C = function() {

    return new Promise(function(resolve, reject) {
        var result = 'C is done'
        console.log(result)
        resolve(result);
    })
}

var D = function() {

    return new Promise(function(resolve, reject) {
        var result = 'D is done'
        console.log(result)
        resolve(result);
    })
}

A().then(function(result) {
    return B();
}).then(C)
  .then(D)

/*
Architectural ideas.

Tränings experimentet sparar:
  pre komponenter
  post komponenter
  training komponenter
  dataumintervall

Per användare sparas:
  Var i testen en person er: pre - träning - post?
  Data för varje fas, samma som i ett vanligt experiment
  Datum intervall för nästa träning

Urlar:
  /training       -  allmän information samt status för var i testet användaren e.
  /training/pre   -  pre
  /training/post  -  post
  /training/task  -  träningsuppgift

  Skippa juttun med faser, istället visas bara rätt experiment/form
  kan ju ändu int navigera mellan olika juttun så, och soile sköter
  ändå om allt redirectande.

  såå flöde /training -> training/pre -> /training/task (repeat) -> training/post -> /training
*/

/*
Overwrites obj1's values with obj2's and adds obj2's if non existent in obj1
*/

function periodicReminder() {
  console.log("Running reminder");

  trainingDAO.getUsersToRemind()
  .then(function(res) {
    console.log("Found users to remind " + res.length)

    for (var i = res.length - 1; i >= 0; i--) {
      var usr = res[i]
      console.log("Sending mail to: " + usr.username);
      console.log("Delay " + JSON.stringify(usr.mailDelay));

      if(!usr.noMail) {
        var now = new Date();
        if(!usr.nextMail || now > usr.tData.nextMail) {
          //Send mail
          //Update trainingdata.nextmail
          console.log("Sending mail, updating nextmail");
          // console.log("Test add date " + t.setHours(t.getHours() + 24).toISOString())

          var t = new Date();
          t.setTime(t.getTime() + ((/*usr.mailDelay + */1)*60*60*1000*240000))
          //Large number so that we dont's send additional mail for this round

          var trainingId = usr.tData.trainingId;
          var linkToTraining = "http://soile.braintrain.fi/training/" + trainingId;
          // var d = now.setHours(now.getHours() + usr.mailDelay);

          console.log("Before mailmanager ", typeof mailManager.sendTrainingReminder)

          mailManager.sendTrainingReminder(
              usr.training.reminderSubject,
              usr.training.reminderEmailMessage,
              usr,
              linkToTraining
              // function(res) {
              //   console.log("MailManager:::Mail sent!")
              // }
            );
          console.log("After mailmanager")

          trainingDataDAO.update(usr.tData._id, {$set:{nextMail:t}}, function(){
            console.log("Tdata updated reminder done\n-------------------")
          });

        }
      }
    }
  });
  /*
    Select tra

    Select active trainings...

    Select active users.

    Find users who are near the dropout timelimit.

    Send email
  */
}

var twelveHours = 12*60*60*1000
// var timerID = vertx.setPeriodic(5000, function(timerID) {
var timerID = vertx.setPeriodic(twelveHours, function(timerID) {
    periodicReminder();
});


function merge_options(obj1,obj2){
    var obj3 = {};
    for (var attrname in obj1) { obj3[attrname] = obj1[attrname]; }
    for (var attrname in obj2) { obj3[attrname] = obj2[attrname]; }
    return obj3;
}

//Handles saving of posted data from tests
function handleResultData(data, datatype, callback) {

}

/**
 * Return all training experiments as a json
 * @return {[json]}   Json array containing all training experiments
 */
router.get("/training/json", requireEditor,function(request) {
  var user = request.session.currentUser;

  if (user.isTestLeader()) {
    var query = {};

    if (user.isEditor() && !user.isAdmin()) {
      query = {users:user.username};
    }

    trainingDAO.list(query ,function(trainings) {
      request.response.json(trainings);
    });
  }
});

//Admin view, shows a list of training experiments
router.get("/training", requireEditor,function(request) {
  var user = request.session.currentUser;

  if (user.isTestLeader()) {
    var query = {};

    if (user.isEditor() && !user.isAdmin()) {
      query = {users:user.username};
    }

    trainingDAO.list(query ,function(training) {
      templateManager.render_template("trainingList", {"trainings":training}, request);
    });
  }
});

//Create a new training task
router.post("/training", requireEditor,function(request) {

  var sDate = Date.now() + (1000*60*60*24*2); //Two days in the future
  var eDate = Date.now() + (1000*60*60*24*30);  //30 days in the future

  var newTraining = new trainingModel();

  newTraining.startDate = new Date(sDate);
  newTraining.endDate = new Date(eDate);
  newTraining.name = "";

  newTraining.users = [request.session.currentUser.username];

  newTraining.save(function(callback) {
      request.redirect("/training/"+newTraining._id+"/edit");
      request.response.end();

  });
});

//Shortned url
router.get("/t/:name",function(request){
  var name = request.params().get('name');

  trainingDAO.get({"shortname":name}, function(training) {
    if(training != "") {
      q = "?" + request.query();
      // logger.info("QUERT: " + q);
      return request.redirect("/training/" + training._id + q);
    }
    else {
      return request.notfound();
    }
  });
});


//View  training experiment
router.get("/training/:id",function(request) {
  var id = request.params().get('id');
  var userid = request.session.getUserId();

  // Get training
  trainingDAO.get(id, function(training) {
    console.log("##### FIRST TRAINING");
    console.log(JSON.stringify(training));

    if (request.session.isTestLeader()) {
      return trainingAdminView(request, training);
    }

    return trainingView(request, training);
  });
});

function trainingView(request, training) {
  var id = training._id;
  var userid = request.session.getUserId();

  trainingDataDAO.getGeneralData(userid, training, function(trainingData,redirect) {

    if (redirect) {
      console.log("Redirected to linked training");
      request.redirect("/training/"+redirect);
      return
    }

    if(trainingData === "") {
      templateManager.render_template('trainingLanding',{training:training},request);
      return;
    }

    var status = {};
    status.open = false;
    status.state = trainingData.getMode();
    status.nextRound = training.nextTask;

    trainingData.nextTask = new Date(trainingData.nextTask);

    var timeString = false;
    if(trainingData.nextTask - Date.now() > 0) {
      timeString = moment(trainingData.nextTask).fromNow();
      console.log("Timestring " + timeString);
    }

    status.timeLeft = timeString;

    var tasksLeft = parseInt(training.repeatcount) - parseInt(trainingData.position);
    var hoursLeft = tasksLeft * parseInt(training.repeatpause);

    console.log("HOURS LEFT " + hoursLeft  + " taskleft " + tasksLeft + " repeat " + training.repeatcount + " pause " + training.repeatpause);

    //status.totalTimeLeft = moment(trainingData.nextTask).add(hoursLeft, "hours").fromNow();
    status.totalTimeLeft = moment(Date.now()).add(hoursLeft, "hours").fromNow(true);
    if(timeString) {
      status.totalTimeLeft = moment(trainingData.nextTask).add(hoursLeft, "hours").fromNow(true);
    }

    status.deadline = moment(Date.now()).add(training.maxpause, "hours").fromNow(true);

    var totalRounds = parseInt(training.repeatcount) + 2;
    var roundsDone = 0;

    var mode = trainingData.getMode();

    if (mode === "training") {
      roundsDone = trainingData.trainingIteration + 1;
    }

    if (mode === "post") {
      roundsDone = totalRounds - 1;
    }

    if (mode === "done") {
      roundsDone = totalRounds;
    }

    if (training.components.pre.length === 0) {
      roundsDone -= 1;
      totalRounds -= 1;
    }

    if (training.components.post.length === 0) {
      //roundsDone -= 1;
      totalRounds -= 1;
      if (mode === "done") {
        roundsDone -= 1;
      }
    }

    status.roundsLeft = roundsDone + "/" + totalRounds;
    status.percentageDone = roundsDone/totalRounds * 100;

    status.roundType = mode;
    status.iteration = trainingData.trainingIteration;


    status.showscore = true;
    if (trainingData.trainingIteration == 0) {
      status.showscore = false;
    }

    if (!training.showScore) {
      status.showscore = false;
    }

    status.done = false;
    if (mode === "done") {
      /*trainingDataDAO.getPrePostScore(id, userid, function(pre, post) {
        status.preScore = pre;
        status.postScore = post;
      });*/
      status.done = true;
    }

    if (status.showscore) {

      trainingDataDAO.getScoreHistory(id, userid, function(score) {
        status.scoreHistory = score;
        templateManager.render_template('trainingUser', {training:training, status:status}, request);
      });
    } else {
      templateManager.render_template('trainingUser', {training:training, status:status}, request);
    }
  });
}

function trainingAdminView(request, training) {
  var id = training._id;
  var userid = request.session.getUserId();

  var cData = [["Pre", 123], ["1", 63],["2", 55],["3", 32],["Post", 19]];

  if (training.userHasAccess(request.session.currentUser)) {
    return templateManager.render_template("trainingAdmin", {training:training, chartData:cData}, request);
  } else {
    return request.unauthorized();
  }
}

/*
b3608da6-aef4-49aa-8e25-1210bc377254,f38699cf-d4e8-42da-8d7c-643b3952e067
*/
/*
  Manually enroll a user into a training experiment. Does the same thing as
  when a user clicks the participate button. The user will see that he is participating
  in the test at the user view after logging in.
*/
router.post("/training/:id/enrolluser", requireEditor,function (request) {
  var data = new vertx.Buffer();
  var trainingId = request.params().get('id');

  request.dataHandler(function(buffer) {
    data.appendBuffer(buffer);
  });

  request.endHandler(function() {

    var params = data.getString(0, data.length());
    params = utils.getUrlParams(params);


    console.log(JSON.stringify(params))

    params = params.userids;

    console.log(JSON.stringify(params))

    if (typeof params === "undefined" || params == "") {
      request.redirect(request.absoluteExternalURI());
      return;
    }

    var ids = params.split(",");
    var usersEnrolled = 0;

    function redirectToTraining() {
      request.redirect("/training/" + trainingId);
    }

    function enrollUsers(next) {
      var id = next.pop();

      var func = enrollUsers;

      if (id === undefined) {
        redirectToTraining();
      } else {
        trainingDAO.enrollUser(trainingId, id, function(res, enrollStatus) {
          if (enrollStatus) {
            usersEnrolled += 1;
          }
          func(next);
        });
      }
    }

    enrollUsers(ids);
  });

});

//Save data to the experiment
router.post("/training/:id/participate", function(request) {
  var id = request.params().get('id');
  var userid = request.session.getUserId();

  var params = request.params();
  var external_id = false;

  if (params.contains("external_id")) {
    external_id = params.get("external_id")
    logger.info("External id exists! " + external_id);
  }

  logger.info("Params: " + request.query());
  trainingDAO.get(id, function(training) {

    //trainingDataDAO.getOrGenerateGeneral(userid, id, training.controlgroup, function(trainingData) {
    trainingDataDAO.getOrGenerateGeneral(userid, training, function(trainingData) {

      if (external_id) {
        trainingData.externalId = external_id;
        trainingData.save(function(err) {
          logger.info("External id saved: " + external_id);
          request.redirect("/training/" + id);
        });
      } else {
        request.redirect("/training/" + id);
      }
    });
  });
});

function getTrainingAndUserData(trainingid, userid, callback) {

  trainingDAO.get(trainingid, function(training) {

    trainingDataDAO.get({userId:userid, trainingId:trainingid, type:"general"}, function(trainingData) {
      var data = {};
      data.training = training;
      data.trainingData = trainingData;

      console.log(JSON.stringify(data));

      callback(data.training, data.trainingData);
    });
  });
}

function renderTrainingPhase(components, position, translatedPhase, request, persistantData, training, userID) {

  var component = components[translatedPhase];
  var id = component.id;
  var template = "";
  var dao = {};
  var context = {};
  var contextObj = "";
  var childObj = 0;

  var phasesLeft = components.length - position;

  if(component.type === "form") {
    dao =formDAO;
    template = "formphase";
    contextObj = "form";
    childObj = "form";
    context.userID = userID;
  }

  if(component.type === "test") {
    dao = testDAO;
    template = "testphase";
    contextObj = "test";
    context.persistantData = persistantData;

    context.testConfig = {};

    if (training.pilotMode) {
      context.testConfig.pilotMode = true;
    }
  }

  context.completed = ((position+1)/components.length * 100);
  context.phasesLeft = (parseInt(position) + 1) + "/" + components.length;

  // Don't submit any form data if skiplastphase is enabled
  if (training.skipLastPhase) {

    console.log("RENDERING - SKIP LASTP PHASE")

    if (parseInt(position +1 ) == components.length ) {
      console.log("LAST POS SKIPING")

      context.noSubmit = true;
    }

  }

  dao.get(id, function(phase) {

    context[contextObj] = phase;

    if (training.submitbutton) {
      context.submitbutton = training.submitbutton;
    }

    if (childObj !== 0) {
      context[contextObj] = phase[childObj];
    }

    console.log(JSON.stringify(context));

    templateManager.render_template(template, context, request);
  });

}

//Execute current training phase
router.get("/training/:id/execute", function(request) {
  var id = request.params().get('id');
  var userid = request.session.getUserId();

  getTrainingAndUserData(id, userid, function(training, trainingData) {

    if(trainingData.mode === "done") {
      return request.redirect("/training/" + id);
    }

  /* if(trainingData.mode === "training" && trainingData.inControlGroup) {
      trainingData.mode = "control";
    }*/

    var modeComponents = training.components[trainingData.getMode()];
    var positionInMode = parseInt(trainingData.position);

    var modeComponents = training.getComponentsForRound(trainingData);

    var phasesLeft = modeComponents.length - (positionInMode);

    var phase = positionInMode;

    var isRandom = training.isRandom();
    console.log("Checking israndom "+ JSON.stringify(isRandom) + " - " + trainingData.checkRandom());
    if (isRandom) {
      if (isRandom[trainingData.getMode()] && trainingData.checkRandom()) {
        // console.log("Random translation " + positionInMode + " -> " +  trainingData.randomorder[trainingData.getMode()][positionInMode]);

        phase = trainingData.getRandomPhase(positionInMode, training);

        // phase = trainingData.randomorder[trainingData.getMode()][positionInMode];
      }
    }

    console.log("Executin training");
    console.log("mode = " + trainingData.getMode() + " position:" + positionInMode + " phase " + phase);
    console.log("Component:" + JSON.stringify(modeComponents[positionInMode]));

    var nextTaskTime = new Date(trainingData.nextTask);

    //if (phasesLeft == 0) {
    if(Date.now() - nextTaskTime < 0 || trainingData.getMode() === "done") {
      return request.redirect("/training/" + id);
    }
     else {
      renderTrainingPhase(modeComponents, positionInMode, phase, request, trainingData.persistantData, training, userid);

      if (training.skipLastPhase) {
        var isSecondLastPhase = trainingData.isLastPhase(training);
        console.log("Check skip last phase");

        if (isSecondLastPhase) {
          console.log("LASTPHASE COMPLETEING AHEAD OF TIME");

          trainingData.completePhase(training);
          trainingData.save(function f(res) {
            console.log("Skipped last phase!!")
          });
        }
      }

    }
  });

});

//Recieve and stora data from training phase
router.post("/training/:id/execute", function(request) {
  var id = request.params().get('id');
  var postData = new vertx.Buffer();

  request.dataHandler(function(buffer){
    postData.appendBuffer(buffer);
  });

  request.endHandler(function() {

    var userid = request.session.getUserId();
    //Figures out the current phase and creates a data object.
    getTrainingAndUserData(id, userid, function(training, generalData) {
      var jsonData = JSON.parse(postData.getString(0, postData.length()));

      //var modeComponents = training.components[generalData.mode];
      //var positionInMode = generalData.position;

      var tData = new TrainingData();
      var oldMode = generalData.getMode();
      tData.data = jsonData.exp;
      tData.mode = generalData.getMode();
      tData.phase = generalData.position;

      //Random order, save testdata with the right phase number
      // TODO fix this for the new randomize functionality
      var isRandom = training.isRandom();
      if (isRandom) {
        if (isRandom[generalData.getMode()] && generalData.checkRandom()) {
          // console.log("###SAVE### Random translation " + generalData.position + " -> " +  generalData.randomorder[generalData.getMode()][generalData.position]);
          // tData.phase = generalData.randomorder[generalData.getMode()][generalData.position];
          tData.phase = generalData.getRandomPhase(generalData.position, training);
        }
      }

      tData.trainingId = id;

      tData.userId = generalData.userId;

      tData.duration = jsonData.duration;
      tData.score = jsonData.score;

      if(generalData.mode === "training") {
        tData.trainingIteration = generalData.trainingIteration;
      }

      console.log(JSON.stringify(jsonData));

      tData.save(function(status) {
        console.log("SAVED TDATA" + JSON.stringify(status));

        var isLastPhase = generalData.isLastPhase(training);
        generalData.completePhase(training);

        generalData.persistantData = merge_options(generalData.persistantData,
                                                   jsonData.persistantData);

        generalData.save(function() {

          //TODO: Check if there is any stored score
          if (isLastPhase) {
            if(training.showScore && oldMode !== "pre") {
              request.jsonRedirect("/training/"+id+"/score");
            }
            else {
              request.jsonRedirect("/training/"+id);
            }
          }
          else {
            request.jsonRedirect("/training/"+id+"/execute");
          }

        });
      });

    });
  });
});

//Execute current training phase
/*
router.get("/training/:id/execute/json", function(request) {
  var id = request.params().get('id');
  var userid = request.session.getUserId();

  getTrainingAndUserData(id, userid, function(d) {
    var modeComponents = training.components[trainingData.mode];
    var positionInMode = trainingData.position;

    var phasesLeft = modeComponents.length - positionInMode;

    console.log("Executin training");
    console.log("mode = " + trainingData.mode + " position:" + positionInMode);
    console.log("Component:" + JSON.stringify(modeComponents[positionInMode]));
    if (phasesLeft = 0) {

    } else {
      renderTrainingPhase(modeComponents, positionInMode, request);
    }
  });

});
*/

router.get("/training/:id/score", function(request) {
  var id = request.params().get('id');
  var userid = request.session.getUserId();

  var context = {};
  trainingDAO.get(id, function(trainingObject) {

    context.showscore = trainingObject.showScoreEndTraining;

    console.log("message --- ",  trainingObject.completeSessionMessage)
    var messages = utils.messageGenerator(trainingObject.completeSessionMessage);


    context.title = messages.title
    context.sessionMessage = messages.message;

    console.log("ENDMESSAGE", messages.message)

    if (trainingObject.showScoreEndTraining) {
      //Fetch score and stuff
      trainingDataDAO.getScore(id, userid, function(totalScore, scores) {

        var scoreContext = {};

        scoreContext.totalScore = totalScore;
        scoreContext.scores = [];

        for (var i = 0; i < scores.length; i++) {
          var s = scores[i];
          scoreContext.scores[i] = {"score":s.score};
          scoreContext.scores[i].subscores = [];
          for(var key in s) {
            if (key !== "score") {
              var subScore = {};
              subScore.name = key;
              subScore.score = s[key];

              scoreContext.scores[i].subscores.push(subScore);
            }
          }
        }

        context.score = scoreContext;

        JSON.stringify(scoreContext);

        return templateManager.render_template("endoftrainingphase", context, request);
      });
    } else {
      return templateManager.render_template("endoftrainingphase", context, request);
    }
    //request.response.end("SCORE!!!")
  });
});

/*

Funderingar

Filter 1 pre/post/training/control/
          If Pre or Post
            Filter 2 all/raw
              if Raw
                filter 3 which test
          if Training or control
            Filter 2 phase
              filter 3 all/raw
                filter 4 which raw
            Filter 2 score
            filter 2 all + id

      Only select stuff from a single user if a id is specified


*/


function buildTranslationArray(training) {
  var translationArray = [];

  for (var i = 0; i < training.repeatcount; i++) {
    translationArray.push([]);

    for (var j = 0; j < training.components.training.length;j++) {
      var comp =  training.components.training[j];
      translationArray[i][j] = 0;
      if (j > 0) {
        translationArray[i][j] = translationArray[i][j-1];
      }
      if (comp.iterationcontrol) {
        if(!comp.iterationcontrolarray[i]) {
          translationArray[i][j] += 1;
        }
      }
    }
  }

  return translationArray;
}

/*
Translates phases to match over the whole training dataset
*/
function fixPhases(arr, training) {
  var translationArray = buildTranslationArray(training);
  console.log(JSON.stringify(translationArray));
  /*var shift = 0;

  for (var i = 0; i < training.repeatcount; i++) {
    translationArray.push([]);

    for (var j = 0; j < training.components.training.length;j++) {
      var comp =  training.components.training[j];
      translationArray[i][j] = 0;
      if (j > 0) {
        translationArray[i][j] = translationArray[i][j-1];
      }
      if (comp.iterationcontrol) {
        if(!comp.iterationcontrolarray[i]) {
          translationArray[i][j] += 1;
        }
      }
    }
  }
  */

  for (var i = 0; i < arr.length; i++) {
    var item = arr[i];
    if (translationArray[item.trainingIteration][item.phase]) {
      //console.log(arr[i].phase + " -> " + translationArray[item.trainingIteration][item.phase])
      arr[i].phase += translationArray[item.trainingIteration][item.phase];
    }
  }

  return arr;
}

function buildPhaseShiftQuery(training, phase) {
  var translationArray = buildTranslationArray(training);

 /* for (var i = 0; i < training.repeatcount; i++) {
    translationArray.push([]);

    for (var j = 0; j < training.components.training.length;j++) {
      var comp =  training.components.training[j];
      translationArray[i][j] = 0;
      if (j > 0) {
        translationArray[i][j] = translationArray[i][j-1];
      }
      if (comp.iterationcontrol) {
        if(!comp.iterationcontrolarray[i]) {
          translationArray[i][j] += 1;
        }
      }
    }
  }*/


  var orArr = [];

  for (var i = 0; i < translationArray.length; i++) {
    var currentTranslation = translationArray[i];
    var translationPhaseOffset = phase - currentTranslation[phase];

    if (training.components.training[phase].iterationcontrol) {
      if (training.components.training[phase].iterationcontrolarray[i] === false) {
        translationPhaseOffset = -1;
      }
    }

    orArr[i] = {$and:[{trainingIteration:i},{phase:translationPhaseOffset}]};
  }

  return orArr;
}

router.get("/training/:id/loaddata", requireEditor, function(request) {
  var id = request.params().get('id');
  var userid = request.params().get("userid");
  var filter1 = request.params().get("f1");
  var filter2 = request.params().get("f2");
  var filter3 = request.params().get("f3");
  var filter4 = request.params().get("f4");

  var startDate = request.params().get("startdate");
  var endDate = request.params().get("enddate");

  var offest = request.params().get("offset");
  var limit = request.params().get("limit");

  var matcher = {};
  var projection = {};

  console.log("filter1 " + filter1);
  console.log("filter2 " + filter2);
  console.log("filter3 " + filter3);
  console.log("filter4 " + filter4);

  console.log("startDate " + startDate);
  console.log("endDate " + endDate);

  matcher.trainingId = id;
  matcher.mode = filter1;

  var tempTimestamp= {}
  if (startDate) {
    startDate = new Date(startDate);
    tempTimestamp["$gte"] = startDate;
  }
  if (endDate) {
    endDate = new Date(endDate);
    tempTimestamp["$lte"] = endDate;
  }

  if(tempTimestamp["$lte"] || tempTimestamp["$gte"]) {
    matcher.timestamp = tempTimestamp;
  }

  var command = "single";
  var groupby = "userId";

  trainingDAO.get(id, function(training) {
    if (matcher.mode === "pre" || matcher.mode === "post") {
      /*
        Pre or post, fetch either raw or single data for all users.
      */
      if (filter2 === "raw") {
        matcher.phase = parseInt(filter3) - 1;
        projection['data.single'] = 0;
        command = "raw"
      }

      if (filter2 === "single") {
        projection['data.rows'] = 0;
      }

     // matcher.type = {$not:"generel"};
    }

    if (matcher.mode === "training") {

      matcher.userId = filter2;
      groupby = "trainingIteration";

      // Certain training phase
      if (filter3 === "single") {
        projection['data.rows'] = 0;
      }

      if (filter3 === "raw") {
        matcher.trainingIteration = parseInt(filter4) - 1;

        command = "raw"

        projection['data.single']= 0;
      }

      if (filter2 == "all") {
        command = "all";
        delete matcher.userId;

        var field = "data.single." + filter3;
        projection[field] = 1;
        projection["data."+filter3] = 1;
        projection.userId = 1;
        projection.trainingIteration = 1;

        // CHange this to take phase shifts into consideration
        /* Select test where
          $or($and(iteration:1, phase:x), (iteration:2, phase:x))

      {$or : [
          { $and : [ { trainingIteration : 1 }, { phase : 1 } ] },
          { $and : [ { trainingIteration : 2 }, { phase : 3 } ] }
      ]}

        */

        matcher.phase= parseInt(filter4)- 1;

        var orArr = buildPhaseShiftQuery(training, matcher.phase);

        delete matcher.phase;
        matcher.$or = orArr;

        groupby = filter3;
      }

      //matcher.type = "{$in:['form','test']}";
      //matcher.type = {$not:"generel"};

    }

    if (matcher.mode === "trainingiterations") {
      projection['data.rows'] = 0;
      matcher.mode = "training";
      matcher.trainingIteration = parseInt(filter2)-1;
      groupby = "userId";
    }

    matcher.type = {"$ne":"general"};

    console.log(JSON.stringify(matcher));

    trainingDataDAO.rawQuery(matcher, function(res) {

      var csv = "";

      if (command === "single") {
        if (matcher.mode == "training") {
          res = fixPhases(res, training);
        }
        csv = csvUtils.jsonRowDataToCsv(res, groupby);
      }
      if (command === "raw") {
        csv = csvUtils.jsonMatrixDataToCsv(res, groupby);
      }
      if (command === "all") {
        csv = csvUtils.jsonSingleTrainingVarToCsv(res, groupby);
      }

      request.response.putHeader("Content-Type", "text/csv; charset=utf-8");
      request.response.putHeader("Content-Disposition", "attachment; filename=data.csv");

      request.response.end(csv);
      //request.response.end("\ufeff " + phaseNames + "\n" + csv);
    }, {keys:projection});
  })

});


//Pre test
router.get("/training/:id/pre", function(request) {

});

//Post test
router.get("/training/:id/post", function(request) {

});

//Repeated training task
router.get("/training/:id/task", function(request) {

});

router.get("/training/:id/edit", requireEditor,function(request) {
  var id = request.params().get('id');
  trainingDAO.get(id, function(training) {
    if (training.userHasAccess(request.session.currentUser)) {
      return templateManager.render_template('trainingEdit', {}, request);
    } else {
      return request.unauthorized();
    }
  });
});

//JSON structure
/*
{
  pre:[],
  post[],
  training[]
}
*/

router.post("/training/:id/edit", requireEditor,function(request) {
  var id = request.params().get('id');
  var data = new vertx.Buffer();

  request.dataHandler(function(buffer){
    data.appendBuffer(buffer);
  });

  request.endHandler(function() {
    var dataObj = JSON.parse(data.getString(0, data.length()));
    trainingDAO.get(id, function(training) {

      var linksToRemove =  _.cloneDeep(training.links);
      var linksToAdd = dataObj.links;

      training = merge_options(training, dataObj);

      training.buildIsRandom();

      training.save(function() {
        request.response.end(200);
      });

      trainingDAO.saveLinks({name:training.name, _id:training._id}, linksToRemove, linksToAdd);
    });
  });
});

router.post("/training/:id/addform", requireEditor,function(request) {
  var id = request.params().get('id');

  var newForm = new formModel();

  newForm.saveAndRender(function(status) {

      trainingDAO.get(id, function(training) {

        training.components.training.push({"type":"form", "name": "Unamed Form", "id":newForm.id});

        training.save(function(stat) {
          request.response.putHeader("Content-Type", "application/json; charset=UTF-8");
          request.response.end(JSON.stringify({"id":newForm.id}));
        });
      });
  });
});

router.get("/training/:id/useroverview", requireEditor,function(request) {
  var id = request.params().get('id');

  trainingDAO.get(id, function(training) {

    trainingDataDAO.list({type:"general", trainingId:id}, function(data) {

      var response = {
        training:training.toJson(),
        participants:(data.map(function(obj){return obj.toJson();}))
      };

      console.log(JSON.stringify(response.participants))

      request.response.putHeader("Content-Type", "application/json; charset=UTF-8");
      request.response.end(JSON.stringify(response));
    });
  });
});


router.get("/training/:id/json", function(request) {
  var id = request.params().get('id');

  trainingDAO.get(id, function(training) {
    var js = training.toJson();

    request.response.putHeader("Content-Type", "application/json; charset=UTF-8");
    request.response.end(js);
  });
});

var formsToClone = [];


function generateDuplicateId(id) {
  // return id + "_d_" + java.util.UUID.randomUUID().toString();
  return "d_" + java.util.UUID.randomUUID().toString();
}

function updateComponents(item) {
  console.log("MAPFUNC")
  if (item.type == "form") {
    item.old_id = item.id;
    item.id = generateDuplicateId(item.id);
    formsToClone.push(item);
  }

  return item;
}

function getAndUpdateForm(fItem) {
  var tempP = formDAO.getP({ _id: fItem.old_id })
    .then(function (f) {
      f._id = fItem.id;
      return f.saveP();
    }).then(function (res) {
    }).catch(function (e) {
      console.log(e);
      console.log("Form update didn't work " + fItem.old_id)
    });

  return tempP
}

router.get("/training/:id/clone", function (request) {
  var id = request.params().get('id');
  console.log("Statring clone " + id);

  trainingDAO.get(id, function (train) {
    console.log("Cloning with model " + train.name);

    train.original_id = train._id;
    train._id = generateDuplicateId(train._id);
    train.name = train.name + "_copy";

    train.components.pre = train.components.pre.map(updateComponents);
    train.components.post = train.components.post.map(updateComponents);
    train.components.training = train.components.training.map(updateComponents);

    train.saveP().then(function (res) {
      console.log("Saved training with id " + train._id);

      var p = Promise.each(formsToClone, function (formItem) {
        return (getAndUpdateForm(formItem));
      });
      return p
    }).then(function (res) {
      return request.redirect("/training/" + train._id);
    }).catch(function (res) {
      console.log("Something didn't work")
      console.log(res);
    })
  });
});
