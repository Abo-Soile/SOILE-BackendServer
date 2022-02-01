/*
Masterplan, laga ett bas object för enskilda object sparade i databasen.
Olika object kan sedan extenda detta och implementera sina egna metoder.

Samma sak med DAO-object, basobject med vanligaste functionerna, typ get, list
osv. Sedan extendas den med mera specifika, t.ex User-dao.

funktioner i dao:n ska alltså då initiera och returnera object som extendar
data objecten

typ User-dao.getUserWithPass() returnerar ett user object.

*/

var vertx = require('vertx');
var console = require('vertx/console');

var container = require('vertx/container');
var config = container.config;
var testImages = config.directory + "/testimages";

var experimentVideos = config.directory + "/testvideos";
var experimentUploads = config.directory + "/exp_video_upload";

var utils = require('utils');
var BaseModel = require('models/baseModel');

var lodash = require("../node_modules/lodash/index");
var _ = lodash;


function userHasAccess(userObject) {
  var users = this.users;
  var username = userObject.username;

  if (userObject.isAdmin()) {
    return true;
  }

  if (typeof users === "undefined") {
    return false;
  }

  var idx = users.indexOf(username);
  if (idx >= 0) {
    return true;
  }

  return false;
}

/*
  Main user class;
*/
function User(arg) {
    this.role = "user";
    this.org = "standard";

    BaseModel.call(this, arg);

    //this._collection = "users"
    this._collection = User.collection;

    if(this.isTestLeader()) {
      this.admin = true;
    }
}

User.prototype = new BaseModel();
User.prototype.constructor = User;
User.collection = "users";

//User.collection = "user";

User.prototype.setPassword = function(password) {
    this.password = utils.hashPassword(password);
};

User.prototype.setOrganisation = function(org) {
  this.organisation = org;
};

User.prototype.isAdmin = function() {
  if (this.role === "admin") {
    return true;
  } else {
    return false;
  }
};

User.prototype.isEditor = function() {
  if (this.role === "editor") {
    return true;
  } else {
    return false;
  }
};

User.prototype.isTestLeader = function() {
  if (this.role === "editor" || this.role === "admin") {
    return true;
  } else {
    return false;
  }
};

User.prototype.generatePasswordResetToken = function() {
  this.forgottenPasswordToken = java.util.UUID.randomUUID().toString();
};

User.prototype.deletePasswordResetToken = function() {
  delete this.forgottenPasswordToken;
}


function Experiment(arg) {

    this.components = [];

    BaseModel.call(this, arg);

    this._collection = Experiment.collection;

    this.isActive();
    this.isRandom();
}

Experiment.prototype = new BaseModel();
Experiment.prototype.constructor = Experiment;
Experiment.collection = "experiment";

Experiment.prototype.save = function(callback) {
  this.lastupdate = new Date();

  return BaseModel.prototype.save.call(this, callback);
};

Experiment.prototype.init = function (callback) {
  var that = this;

  that.save(function (r) {
    var dirName = experimentVideos + "/" + that._id;
    var dirName2 = experimentUploads + "/" + that._id;

    vertx.fileSystem.mkDir(dirName, true, function (err, res) {
      if (!err) {
        console.log('Experiment video directory created successfully');
      }

      vertx.fileSystem.mkDir(dirName2, true, function (err, res) {
        if (!err) {
          console.log('Experiment user video upload directory created successfully');
        }
        callback(err, res);
      });
    });

  });
};


Experiment.prototype.isActive = function() {
  var currentDate = new Date();
  var millisecondsPerDay = 1000*3600*24;

  var sDate = new Date(this.startDate);
  var eDate = new Date(this.endDate);

  if((sDate < currentDate)&&(currentDate<eDate)) {
    this.active = true;
    this.timedata = Math.ceil((eDate - currentDate)/millisecondsPerDay);
  }
  else{
    this.active = false;
    if(sDate > currentDate) {
      this.timedata = Math.ceil((sDate - currentDate)/millisecondsPerDay);
    }
  }

  //console.log("IS ACTIVE RUNNING");
  // Experiment is inactive if no components exits
  if(!this.hasOwnProperty("components")) {
    //console.log("components doesn't exist");
    this.active = false;
  }
  else {
    if (this.components.length == 0) {
      //console.log("components is empty");

      this.active = false;
    }
  }
};

Experiment.prototype.isRandom = function() {
  var longestRandom = 0;
  var prevRandom = false;

  if (typeof this.components === 'undefined') {
    this.israndom = false;
    return;
  }

  var randomCount = 0;

  for (var i = 0; i < this.components.length; i++) {
    if(this.components[i].random) {
      longestRandom +=1;
      if (longestRandom > 1) {
        this.israndom = true;
        return;
      }
      randomCount += 1;
    }
    else {
      longestRandom = 0;
    }
  }

  if(randomCount > 1) {
    this.israndom = true;
    return;
  }

  this.israndom = false;
  return;
};

Experiment.prototype.addComponent = function(id, type, name) {
  var comp = {};
  comp.id = id;
  comp.type = type;
  comp.name = name;
  this.components.push(comp);
};


Experiment.prototype.getPhaseType = function(phaseNo, randomOrder) {
  var type = this.components[this.getPhase(phaseNo, randomOrder)].type;
  return type;
};


Experiment.prototype.getPhase = function(phaseNo, randomOrder) {
  if (this.israndom) {
    //console.log("IS RANDOM, converting " + phaseNo + "  -> " + randomOrder[phaseNo]);
    return randomOrder[phaseNo];
  }
  return phaseNo;
};

Experiment.prototype.shouldProceedWithSave = function(generalData, location) {
  var lastSavedPhase = parseInt(generalData.position);

  if (lastSavedPhase === location) {
    return true;
  } else{
    return false;
  }

};

Experiment.prototype.removeComponent = function(index) {
  this.components.splice(index, 1);
};

Experiment.prototype.renameComponent = function(index, name) {
  this.components[index].name = name;
};

Experiment.prototype.userHasAccess = userHasAccess;

/*
##########
TRAINING
*/
function Training(arg) {

    this.components = {};

    this.components.pre = [];
    this.components.training = [];
    this.components.post = [];

    this.repeatpause = 0;
    this.repeatcount = 1;
    this.maxpause = 1;

    this._isRandom = false;

    BaseModel.call(this, arg);

    this._collection = Training.collection;
}

Training.prototype = new BaseModel();
Training.prototype.constructor = Training;
Training.collection = "training";

Training.prototype.isRandom = function(mode) {
//  var isRandom = {};
//
//  isRandom.pre = utils.isRandom(this.components["pre"]);
//  isRandom.post = utils.isRandom(this.components["post"]);
//  isRandom.training = utils.isRandom(this.components["training"]);
//  isRandom.control = utils.isRandom(this.components["control"]);
//
//  if (!(isRandom.pre || isRandom.post || isRandom.training || isRandom.control)) {
//    return false;
//  }
//  return isRandom
  return this._isRandom;
};

Training.prototype.buildIsRandom = function() {
  var isRandom = {};

  isRandom.pre = utils.isRandom(this.components["pre"]);
  isRandom.post = utils.isRandom(this.components["post"]);
  isRandom.training = utils.isRandom(this.components["training"]);
  isRandom.control = utils.isRandom(this.components["control"]);

  if (!(isRandom.pre || isRandom.post || isRandom.training || isRandom.control)) {
    return false;
  }

  console.log(JSON.stringify(isRandom));

  this._isRandom = isRandom;
  //return isRandom;
};

/*
Returns the components for the current training round, takes care
*/
Training.prototype.getComponentsForRound = function(trainingData) {

    var modeComponents = this.components[trainingData.getMode()];
    var positionInMode = parseInt(trainingData.position);

    var prunedModeComponents = [];

    for (var i = 0; i < modeComponents.length; i++) {
      if(modeComponents[i].iterationcontrol) {

        if (modeComponents[i].iterationcontrolarray[trainingData.trainingIteration]) {
          prunedModeComponents.push(modeComponents[i]);
        }
      } else {
        prunedModeComponents.push(modeComponents[i]);
      }
    }

    modeComponents = prunedModeComponents;

    return modeComponents;
};

/**
 * Check if the training experiment is linked to another one
 * @return {Boolean}
 */
Training.prototype.isLinked = function() {
  if(this.enableLink && this.links) {
    if(this.links.length > 0) {
      return true
    }
  }

  if(this.linkParent) {
    return true
  }

  return false
};

Training.prototype.userHasAccess = userHasAccess;

/*
####
TEST
*/
function Test(arg) {
    this.published = false;
    this.name = "Unnamed";
    this.folder = "Unspecified";

    this.users = [];

    BaseModel.call(this, arg);

    this._collection = Test.collection;
}

Test.prototype = new BaseModel();
Test.prototype.constructor = Test;
Test.collection = "tests";

Test.prototype.save = function(callback) {
  this.lastupdate = new Date();

  return BaseModel.prototype.save.call(this, callback);
};

/*
  Creates a copy of the experiment with the given userid as owner
*/
Test.prototype.copy = function(userid, callback) {
  var test = new Test(this);
  var testImages = config.directory + "/testimages";


  test.owner = userid;

  test.code = this.code;
  test.js = this.js;
  test.name = this.name + "_copy";

  if (typeof test.code === 'undefined') {
    test.code = "";
  }

  delete test.id;
  delete test._id;

  var that = this;
  test.save(function() {

    console.log("Replacing id:s " + that._id + " -> " + test._id);
    console.log(JSON.stringify(that.code.indexOf(that._id)));

    //test.code = test.code.replace(that._id, test._id);

    test.code = test.code.split(that._id).join(test._id);

    test.save(function(){
      var oldDir = testImages + "/" + that._id;
      var dirName = testImages + "/" + test._id;

      vertx.fileSystem.copy(oldDir, dirName,true, function(err, res) {
        callback(test);
      });
    });
  });
};

Test.prototype.init = function(callback) {
  var that = this;

  that.save(function(r) {
    var dirName = testImages + "/" + that._id;

      vertx.fileSystem.mkDir(dirName, true, function(err, res) {
        console.log(err + "  " + res);
        if (!err) {
          console.log('Directory created ok');
        }
        callback(err, res);
      });
  });
};


/*
  Compiles this test
*/
Test.prototype.compile = function(code, callback) {
  var address = utils.get_address('experiment_language');
  var eb = vertx.eventBus;

  this.code = code;

  var msg = {
    'code': this.code
  };

  var that = this;
  eb.send(address, msg, function(reply) {
    var response = {};

    if (reply.hasOwnProperty('errors') === true) {
      response.errors = reply.errors.split("\n");
      console.log(reply.errors);

      that.js = "";
      that.compiled = false;
    } else {
      response.code = reply.code;
      that.js = response.code;
      that.compiled = true;
    }

    that.save(function() {
      callback(response);
    });
  });
};

Test.prototype.userHasAccess = userHasAccess;

/*
####
#FORM
*/
function Form(arg) {
  this.markup = "";
  BaseModel.call(this, arg);

  this._collection = Form.collection;
}

Form.prototype = new BaseModel();
Form.prototype.constructor = Form;
Form.collection = "forms";


Form.prototype.saveAndRender = function(callback) {
    var address = utils.get_address("questionnaire_render");

    var msg = {
      'markup':this.markup,
      "action":"save"
    };

    var newForm = false;

    if (typeof this.id !== undefined) {
      msg.id = this.id;
      newForm = true;
    }

    var that = this;
    vertx.eventBus.send(address, msg, function(reply) {
      var id = reply.id;
      if (newForm) {
        that.id = id;
      }
      callback(reply);
    });
};


/*
####
DATA
*/
function Data(arg) {
  this.confirmed = false;
  this.timestamp = new Date().toISOString();

  BaseModel.call(this, arg);
  this._collection = Data.collection;
}

Data.prototype = new BaseModel();
Data.prototype.constructor = Data;
Data.collection = "data";

Data.prototype.initGeneral = function(exp) {
  this.type = "general";
  this.expId = exp._id;
  this.position = 0;
  this.randomorder = false;

  var timeStamp = new Date();
  this.starttime = timeStamp.toISOString();

  if (exp.mechanicalTurkEnabled) {
    this.mechanicalTurkToken = utils.randomAlphaNumeric(10);
  }

  if(exp.israndom) {
    this.generateRandomOrder(exp);
  }
};


Data.prototype.generateRandomOrder = function(exp) {
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

  for (var i = 0; i < randomGroups.length; i++) {
    if(randomGroups[i]===1) {
      randomMapping = randomizeGroup(randomMapping, randomList, i);
    }
  }
  this.randomorder = randomMapping;
};

//Form.collection = "forms"


/*
TRAINING DATA
*/
function TrainingData(arg) {
  this.confirmed = false;
  this.timestamp = new Date().toISOString();

  BaseModel.call(this, arg);
  this._collection = TrainingData.collection;
}

TrainingData.prototype = new BaseModel();
TrainingData.prototype.constructor = TrainingData;
TrainingData.collection = "trainingdata";


/*
  Sets the general datafiled to the initial state.
*/
TrainingData.prototype.initGeneral = function(training) {
  this.type = "general";

  this.mode = "pre";
  this.position = 0;
  this.trainingIteration = 0;

  this.inControlGroup = false;
  this.randomorder = false;

  if (training.controlGroup) {
    this.inControlGroup = false;
    // 50/50 chance to be put in control group
    if (Math.floor(Math.random() * 2 + 1) === 1) {
        this.inControlGroup = true;
    }
  }

  if (training.components.pre.length === 0) {
    this.mode = "training";
  }

//  var isRandom = training.isRandom();
//  if(isRandom) {
//    this.buildRandomOrder(isRandom);
//  }

  this.buildRandomOrder(training);

  //When the next session is opened// DATE
  this.nextTask = new Date();

  this.trainingId = training._id;
};


/**
 * Filter out components that arent in that trainingiteration
 * @param  {[type]} components [description]
 * @param  {[type]} iteration  [description]
 * @return {[type]}            [description]
 */
function filterTrainingRandomness(components, iteration){
  var comps = _.cloneDeep(components);

  var newComps = [];

  _.each(comps, function(component) {
    var r = "";
    if (component.iterationcontrol) {
      if (!component.iterationcontrolarray[iteration]) {
        component.random = false;
        r = "| random -> false "
      } else {
        newComps.push(component);
      }
      console.log("Iterationcontrol " + iteration + " - " + component.iterationcontrolarray[iteration] + " --random:"+component.random + r)
    } else {
      // if (component.random) {
        r = "| random -> " + component.random;
        newComps.push(component);
        console.log("Iterationcontrol " + iteration + " - no iterationcontrol --random:"+component.random + r)
      // }
    }
  });

  return newComps
  // return comps
}
/**
 * Get phase number for the randomized phase
 * @return {[integer]} Translated phase
 */
TrainingData.prototype.getRandomPhase = function(positionInMode) {
  if(this.type=="general") {
    var result = positionInMode;
    var mode = this.getMode();

    if (mode == "control" || mode == "training") {
      console.log("Translating random training");
      var tempRes = this.randomorder[mode];
      result = tempRes[this.trainingIteration][positionInMode];
    }else {
      result = this.randomorder[mode][positionInMode];
    }

    console.log("Random translation " + positionInMode + " -> " +  result);
    return result;
  }
};

TrainingData.prototype.buildRandomOrder = function(training) {
  var isRandom = training.isRandom();
  if (!isRandom) {
    this.randomorder = false;
    return;
  }

  this.randomorder = {};

  var orders = ["pre", "post", "training", "control"];

  for (var i = 0; i < orders.length; i++) {
    var ord = orders[i];
    if (isRandom[ord]) {
      if(ord=="training"||ord=="control") {
        var rand = [];
        for(var j = 0; j<training.repeatcount; j++) {
          console.log("\nBuilding "+ ord +" random order: "+ j);
          var randComponents = filterTrainingRandomness(training.components[ord], j);
          // console.log(JSON.stringify(randComponents||"No random components"))
          if (randComponents.length > 0) {
            rand.push(utils.generateRandomOrder(randComponents));
          } else {
            rand.push(false);
          }
        }
        this.randomorder[ord] = rand;
      }else {
        this.randomorder[ord] = utils.generateRandomOrder(training.components[ord]);
      }
    } else {
      this.randomorder[ord] = false;
    }
  }

  /*console.log("Created random order");
  console.log(JSON.stringify(this.randomorder));
*/
};

/**
 * Check if a phase actually is random
 * @return {[type]} [description]
 */
TrainingData.prototype.checkRandom = function() {
  if (!this.randomorder) {
    return false;
  } else {

    if (typeof this.randomorder[this.getMode()] !== "undefined"){
      if (typeof this.randomorder[this.getMode()][this.position] === "number" ||
          _.isArray(this.randomorder[this.getMode()][this.trainingIteration])
          ) {
        return true;
      }
    }

    return false;
  }

  return false;
};

/*
  Generates a date x hours in the future,
  Returns current date if if hours isn't an integer
*/
function hoursFromNow(hours) {
  var date = new Date();

  hours = parseInt(hours);

  if(_.isNaN(hours)) {
    return date
  }

  date.setHours(hours + date.getHours());

  return date;
}

/**
 * Check if we're in the last phase
 */
TrainingData.prototype.isLastPhase = function(training) {
  var isLast = false;
  var components = training.components[this.getMode()];

  components = training.getComponentsForRound(this);

  if(components.length === (this.position + 1) || components.length === 1) {
    isLast = true;
  }
  return isLast;
};


TrainingData.prototype.isSecondLastPhase = function() {
  var isSecondLast = false;

  var components = training.components[this.getMode()];

  components = training.getComponentsForRound(this);

  if (components.length === (this.position + 2) || components.length === 2) {
    isLast = true;
  }
}

/*Increment use position when a phase is completed*/
TrainingData.prototype.completePhase = function(training) {
  //If last phase, complete the whole set.
  //  If pre -> go to training + waittime
  //           -> or to control
  //  If training -> training if iterations left
  //              -> post if no iterations left
  //  if post -> finish experiment
  //If not last phase -> phase += 1
  var mode = this.getMode();
  var components = training.components[mode];

  components = training.getComponentsForRound(this);

  var lastPhase = false;

  if(components.length === (this.position + 1)) {
    lastPhase = true;
  }

  //LastPhase
  if (lastPhase) {

    console.log("IN LAST PHASE");

    if (mode === "pre") {

      if (training.repeatpauseadvance && !_.isNaN(parseInt(training.repeatpausepre))) {
        this.nextTask = hoursFromNow(training.repeatpausepre);
      } else {
        this.nextTask = hoursFromNow(training.repeatpause);
      }

      if (this.inControlGroup) {
        //this.mode = "control";
        this.mode = "training";
      } else {
        this.mode = "training";
      }
    }


    if (mode === "training" || mode === "control") {
      this.nextTask = hoursFromNow(training.repeatpause);

      if(training.reminderEmail && training.maxpause) {
        this.nextMail = hoursFromNow(training.maxpause || 1000000);
      }

      // Last training phase completed, go to post
      if (training.repeatcount == (this.trainingIteration + 1)) {
        this.mode = "post";

        if(training.repeatpauseadvance && !_.isNaN(parseInt(training.repeatpausepost))) {
          this.nextTask = hoursFromNow(training.repeatpausepost);
        }

        /*Set mode to done if the posttest is empty*/
        if (training.components.post.length === 0) {
          this.mode = "done";
        }

      } else {
        this.trainingIteration += 1;
      }
    }

    if (mode === "post") {
      this.mode = "done";
    }

    //Resetting task position
    this.position = 0;

  } else {
    this.position += 1;
  }
};

//Returns the current mode
TrainingData.prototype.getMode = function() {
  var mode = this.mode;
  if (this.inControlGroup && this.mode==="training") {
    mode = "control";
  }
  return mode;

};


module.exports.User = User;
module.exports.Test = Test;
module.exports.Form = Form;

module.exports.Experiment = Experiment;
module.exports.Data = Data;

module.exports.Training = Training;
module.exports.TrainingData = TrainingData;