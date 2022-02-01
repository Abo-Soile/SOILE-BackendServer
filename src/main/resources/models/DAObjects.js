var vertx = require('vertx');
var eb = vertx.eventBus;
var utils = require('utils');

var console = require('vertx/console');

var models = require('models/Models');
var BaseDAO = require('models/baseDAO');

var Promise = require("mPromise");

var lodash = require("../node_modules/lodash/index");
var _ = lodash;

var User = models.User;
var Test = models.Test;
var Form = models.Form;
var Experiment = models.Experiment;
var Training = models.Training;
var TrainingData = models.TrainingData;
var ExpData = models.Data;

function UserDAO() {
    BaseDAO.call(this);
    this._baseObject = models.User;
    this._collection = this._baseObject.collection;
}

UserDAO.prototype = new BaseDAO();
UserDAO.prototype.constructor = UserDAO;

UserDAO.prototype.auth = function(username, password, remember, callback) {
    var pass = utils.hashPassword(password);

    this.get({username:username, password:pass}, function(user) {
        if (remember && user) {
            user.sessiontoken = java.util.UUID.randomUUID().toString();
            user.save(function() {
                callback(user);
            });
        } else {
            callback(user) ;
        }
    });
};

UserDAO.prototype.fromSession = function(sessionKey, callback) {
    this.get({sessiontoken:sessionKey}, function(user) {
        callback(user);
    });
};

function TestDAO() {
    BaseDAO.call(this);
    this._baseObject = models.Test;
    this._collection = this._baseObject.collection;
}

TestDAO.prototype = new BaseDAO();
TestDAO.prototype.constructor = TestDAO;

TestDAO.prototype.listFolders = function(user, callback) {
    var command = "{ distinct: 'tests', key: 'folder', query:{users:'" + user.username + "'}}";

    if(user.isAdmin()) {
        command = "{ distinct: 'tests', key: 'folder', query:{}}";
    }

    var mongo = {
        "action":"command",
        "command":command
    };

    this.sendToMongo(mongo, function(result) {
        callback(result.result.values);
    });
};

function ExperimentDAO() {
    BaseDAO.call(this);
    this._baseObject = models.Experiment;
    this._collection = this._baseObject.collection;

}

ExperimentDAO.prototype = new BaseDAO();
ExperimentDAO.prototype.constructor = ExperimentDAO;

ExperimentDAO.prototype.countParticipants = function(expId, callback) {
    var dataDAO = new DataDAO();

    var confMatcher = {"expId":expId,
                          "confirmed":true,
                          "type":"general"
                        };

    var totalMatcher = {
                        "expId":expId,
                        "type":"general"
                        };

    dataDAO.count(confMatcher, function(confirmed) {
        dataDAO.count(totalMatcher, function(total) {
            dataDAO.getPhaseCompletion(expId, function(completion) {
                callback({"confirmed":confirmed, "total":total, "completion":completion});
            });
        });
    });
};

/*
    Completes a phase in the current experiment and saves experiment data.
*/
ExperimentDAO.prototype.completePhase = function(dataObject, experimentId, callback) {
    var that = this;
    var dataDAO = new DataDAO();

    var dataObject = dataObject;
    var experimentId = experimentId;

    //Save object and increment general current position
    function save(dataObj) {
        dataObj.save(function() {
            var command = {
                "action":"update",
                "criteria":{
                    "expId":dataObject.expId,
                    "userid":dataObject.userid,
                    "type":"general"
                  },
                objNew : {
                    $inc: {
                        position:1
                    }
                }
            };

            dataDAO.sendToMongo(command, function() {
                callback();
            });
        });
    }

    that.get(experimentId, function(experiment) {
        dataDAO.getGeneral(dataObject.userid, experiment, function(userdata) {
            var location = dataObject.phase;
            dataObject.type = experiment.getPhaseType(location, userdata.randomorder);
            dataObject.phase = experiment.getPhase(location, userdata.randomorder);

            if (experiment.shouldProceedWithSave(userdata, location)) {
                save(dataObject);
            } else {
                //console.log("Skipping save");
                callback();
            }
        });
    });
};


function FormDAO() {
    BaseDAO.call(this);
    this._baseObject = models.Form;
    this._collection = this._baseObject.collection;
}

FormDAO.prototype = new BaseDAO();
FormDAO.prototype.constructor = FormDAO;


function DataDAO() {
    BaseDAO.call(this);
    this._baseObject = models.Data;
    this._collection = this._baseObject.collection;
}

DataDAO.prototype = new BaseDAO();
DataDAO.prototype.constructor = DataDAO;


/*
 * Fetches general data from the user
 * @param userid {string}
 * @param exp {string}
 * @param callback {function}
 */
DataDAO.prototype.getGeneral = function(userid, exp, callback) {
    var that = this;

   // console.log("Getting general " + userid + " " + exp._id);

    that.get({userid:userid, type:"general", expId:exp._id},
      function(data, message) {
        callback(data);
    });
};

DataDAO.prototype.getOrGenerateGeneral = function(userid, exp, request, callback) {
  var that = this;

  that.get({userid:userid, type:"general", expId:exp._id},
        function(data, message) {
    if (data === "") {
        //console.log("Generating new data object");
        data = new ExpData();

        data.userid = userid;
        data.initGeneral(exp);

        data.referer = "direct";
        data.ip = request.remoteAddress().getHostString();

        if (request.headers().contains("X-Real-IP")) {
            data.ip = request.headers().get("X-Real-IP");
        }

        if(typeof request.headers().get("Referer") != 'undefined'){
            data.referer  = request.headers().get("Referer");
        }

        data.userAgent = request.headers().get("User-Agent");

        if (exp.mechanicalTurkEnabled) {
            data.mechanicalTurkToken = utils.randomAlphaNumeric(10);
        }

        data.save(function(err) {
            return callback(data);
        });
    } else {
        console.log("ExpData exists");
        callback(data);
    }
  });
};

DataDAO.prototype.completeExperiment = function(expId, userid, callback) {
    var that = this;

    var matcher = {};
    matcher.expId = expId;
    matcher.userid = userid;

    var objnew = {"$set":{"confirmed":true}};

    that.update(matcher, objnew, function(status) {
        callback(status);
    });
};

DataDAO.prototype.getPhaseCompletion = function(expId, callback) {
    var pipe = [
        {$match:{expId:expId,
                 phase:{$gte:0},
                 deleted:{$in: [null, false]}}},
        {$group:{_id:"$phase", count:{$sum:1}}}
    ];

    this.aggregate(pipe, function(result){
        callback(result);
    });
};

DataDAO.prototype.getPhaseCompletionWithoutAggregate = function(expId, callback) {
    var that = this;
    var matcher = {
        expId:expId,
        type:"general"
    };

    that.rawQuery(matcher, function(res) {
        var positionCount = [];
        for (var i = 0; i < res.length; i++) {
            var pos = res[i].position;
            if (pos || pos == 0) {
                if(typeof positionCount[pos] === "undefined") {
                    positionCount[pos] = 1;
                } else {
                    positionCount[pos] += 1;
                }
            }
        }

        var posObj = {};
        for (var i = 0; i < positionCount.length; i++) {
            posObj[i] = positionCount[i];

            if(!posObj[i]) {
                posObj[i] = 0;
            }
        }

        callback([posObj]);
    },{keys:{"position":1}});
};

/*Aggregate completions per phase
db.data.aggregate([
    {$match:{expId:"8d4f15f3-d2a8-4001-a83c-6cd080b46911",deleted:{$in: [null, false]}}},
    {$group:{_id:"$phase", count:{$sum:1}}}
    ])


{
    "action": "aggregate",
    "collection": "testcities",
    pipelines: [
        {$match:{expId:"8d4f15f3-d2a8-4001-a83c-6cd080b46911",deleted:{$in: [null, false]}}},
        {$group:{_id:"$phase", count:{$sum:1}}}
    ]
}
*/
function TrainingDAO() {
    BaseDAO.call(this);
    this._baseObject = models.Training;
    this._collection = this._baseObject.collection;
}

TrainingDAO.prototype = new BaseDAO();
TrainingDAO.prototype.constructor = TrainingDAO;

TrainingDAO.prototype.addform = function(first_argument) {
    // body...
};

/*
Manually enrolls a user as a participant in a training experiment
Returns: True if user was enrolled
         False if the user couldnt be enrolled (i.e. he is already enrolled in this experiment)
*/
TrainingDAO.prototype.enrollUser = function(trainingID, userid, callback) {
    var that  = this;
    var tDataDao = new TrainingDataDAO();

    that.get(trainingID ,function(training) {
        tDataDao.getOrGenerateGeneral(userid, training, function(res, alreadyEnrolled) {
            if(alreadyEnrolled === true) {
                callback(false);
            } else {
                callback(true);
            }
        });
    });
};

TrainingDAO.prototype.getUsersToRemind = function(trainingID, callback) {
    that = this;

    // 1: Find all training experiments where emailing is activated
    // 2: Find general data for users who are past the timelimit type:general, trainingid, mode:training'
    // 3:

    // Promise.setScheduler(function(fn){ // fn is what to execute
    //     vertx.setTimer(1, fn);
    // });

    var tDataDao = new TrainingDataDAO();

    var trainingData = [];
    var trainingExpDealy = {};
    var trainingCollections = {}

    // Find training experiments with email enabled
    var getExps = function() {
        var p = new Promise(function(resolve, reject) {
            that.list({reminderEmail:true}, function(res) {
                resolve(res);
            })
        });
        return p;
    }

    // Find general trainingdata based on a set of ids
    // TODO: Check that the next mailing data is after the reminder
    var getTData = function(ids) {
        return new Promise(function(resolve, reject) {
            tDataDao.list({
                trainingId:{$in:ids},
                type:"general",
                mode:"training",
                nextMail:{$lt:new Date()}
            },function(res) {
                trainingData = res;
                resolve(res);
            });
        });
    }

    // Find a list of users
    // TODO: implement allow/forbid emailing switch
    // TODO: don't fetch users who have banne emailing
    var getUsers = function(userIDs) {
        return new Promise(function(resolve, reject) {
            module.exports.UserDAO.list({
                _id:{$in:userIDs}
            },function(res) {
                resolve(res);
            });
        });
    }

    var matcher = {
        type:"general",
        trainingId:trainingID,
    }

    var prom = new Promise(function(resolve, reject) {

        getExps().then(function(trainingObjects) {
            var ids =[];
            for (var i = trainingObjects.length - 1; i >= 0; i--) {
                trainingExpDealy[trainingObjects[i]._id] = trainingObjects[i].maxpause;
                trainingCollections[trainingObjects[i]._id] = trainingObjects[i];
                ids.push(trainingObjects[i]._id);
            }
            return getTData(ids);
        }).then(function(res) {
            var userIDs =[];
            for (var i = res.length - 1; i >= 0; i--) {
                userIDs.push(res[i].userId);
            }

            return getUsers(userIDs)
        }).then(function(users) {
            _.each(users, function(usr){
                usr.tData = _.chain(trainingData).where({userId:usr._id}).first().value();
                usr.mailDelay = trainingExpDealy[usr.tData.trainingId];
                usr.training = trainingCollections[usr.tData.trainingId];
            });
            resolve(users);
        }).catch(function(err) {
            console.log(err);
        })
    });


    return prom;
}

TrainingDAO.prototype.saveLinks = function(training,linksToRemove, linksToAdd) {
  var that = this;

  console.log("Saving links " + typeof linksToRemove + "  " + typeof linksToAdd)

  if (typeof linksToRemove == 'object') {
    if (linksToRemove.length > 0) {
        console.log("Linkstoremove: " + linksToRemove.length)
        var ids = [];
        linksToRemove.forEach(function(l) {
            ids.push(l._id)
        })
        that.update({_id:{$in:ids}}, {$set:{linkParent:false}}, function(res) {

        }, true);
    }
  }
  if (typeof linksToAdd == 'object') {
    if (linksToAdd.length > 0) {
        console.log("linksToAdd: " + linksToAdd.length)
        var ids = [];
        linksToAdd.forEach(function(l) {
            ids.push(l._id)
        })
        that.update({_id:{$in:ids}}, {$set:{linkParent:{name:training.name, _id:training._id, links:ids}}}, function(res) {

        }, true);
    }
  }
};
/*
var formsToClone = [];

function generateDuplicateId(id) {
    return id + "_d_" + java.util.UUID.randomUUID().toString();
}

function updateComponents(item) {
    console.log("MAPFUNC")
    if (item.type == "form") {
        item.old_id = item.id;
        item.id = generateDuplicateId(item.id);
        formsToClone.push(item);
    }
}

TrainingDAO.prototype.duplicate = function(train) {
    train._id = generateDuplicateId(train._id);
    train.name = train.name + "_copy";

    console.log("set new name and id " + train._id);

    train.components.pre = train.components.pre.map(updateComponents);
    train.components.post = train.components.post.map(updateComponents);
    train.components.training = train.components.training.map(updateComponents);

    console.log("Mapped new components");

    train.save(function(res) {
        console.log("Saved training with id " + train._id);
        formsToClone.forEach(function(formItem) {
            console.log("cloning form " + formItem.old_id);
            FormDAO.get({_id:formItem.old_id}, function(f) {
                console.log("Update form item")
                f._id = formItem.id;
                f.save();
            });
        });
    });
} */

function TrainingDataDAO() {
    BaseDAO.call(this);
    this._baseObject = models.TrainingData;
    this._collection = this._baseObject.collection;
}

TrainingDataDAO.prototype = new BaseDAO();
TrainingDataDAO.prototype.constructor = TrainingDataDAO;



//TrainingDataDAO.prototype.getOrGenerateGeneral = function(userid, trainingId, controlGroup,callback) {
TrainingDataDAO.prototype.getOrGenerateGeneral = function(userid, training, callback) {
  var that = this;
  that.get({userId:userid, type:"general", trainingId:training._id},
        function(trainingData, message) {
    if (trainingData === "") {
        //console.log("Generating new data object");
        trainingData = new TrainingData();

        trainingData.userId = userid;
        //trainingData.initGeneral(training._id, training.controlGroup);
        trainingData.initGeneral(training);

        trainingData.save(function(err) {
            return callback(trainingData);
        });
    } else {
        console.log("Training exists");
        callback(trainingData, true);
    }
  });
};

/*
TrainingDataDAO.prototype.getOrGenerateGeneral = function(userid, trainingId, controlGroup,callback) {
  var that = this;
  that.get({userId:userid, type:"general", trainingId:trainingId},
        function(trainingData, message) {
    if (trainingData === "") {
        //console.log("Generating new data object");
        trainingData = new TrainingData();

        trainingData.userId = userid;
        trainingData.initGeneral(trainingId, controlGroup);

        trainingData.save(function(err) {
            return callback(trainingData);
        });
    } else {
        callback(trainingData);
    }
  });
};
*/

TrainingDataDAO.prototype.handleLinks = function(userid, training) {
    var that = this;

    var idsToCheck = [training._id];

    if (training.links) {
        training.links.forEach(function(link) {
            idsToCheck.push(link._id)
        })
    } else if (training.linkParent) {
        training.linkParent.links.forEach(function(link) {
            idsToCheck.push(link);
        })

        idsToCheck.push(training.linkParent._id);
    }

    var p = new Promise(function(resolve, reject) {
        that.get({userId:userid, type:"general", trainingId:{$in:idsToCheck}}, function(res) {

            // No general data exists
            if (res == "") {
                console.log("No trainingdata, ignore link")
                return resolve("");
            }

            if (res.trainingId == training._id) {
                resolve(res);
            } else {
                resolve({_redirect:res.trainingId});
                // reject(res.trainingId)
            }
        });
    });
    return p
    // that.get({userId:userid, type:"general", trainingId:$in:training._id})
}

TrainingDataDAO.prototype.getGeneralData = function(userid, training,callback) {
  var that = this;
  if (!training.isLinked()) {
      that.get({userId:userid, type:"general", trainingId:training._id},
            function(trainingData, message) {
        if (trainingData === "") {
            callback("")
        } else {
            callback(trainingData);
        }
      });
  } else {
    that.handleLinks(userid, training).then(function(res) {
        if(res._redirect) {
            return callback("", res._redirect);
        }
        callback(res);
    })/*.catch(function(redir) {
        console.log("DAO REDIRECT: " + redir);
        if(redir) {
            callback({}, redir);
        }
    });  */
  }

};

/*
    Returns score from the previous training phase.
*/
TrainingDataDAO.prototype.getScore = function(trainingId, userid, callback) {
    var iteration = null;
    var mode = null;
    var that = this;

    that.get({userId:userid, type:"general", trainingId:trainingId}, function(general) {
        if(general.mode === "training" && general.trainingIteration > 0) {
            iteration = general.trainingIteration - 1;
            mode = "training";
        }
        else if (general.mode === "post") {
            mode = "training";
            iteration = general.trainingIteration;
        }

        else if (general.mode === "done") {
            mode = "post";
        } else {
            mode = "pre";
        }

        var matcher = {
            "trainingId":general.trainingId,
            "userId":general.userId
        };

        if(mode) {
            matcher.mode = mode;
        }

        if(iteration !== null) {
            matcher.trainingIteration = iteration;
        }

        that.list(matcher, function(scoreList) {
            var totalScore = 0;
            var scores = [];

            for (var i = 0; i < scoreList.length; i++) {
                console.log(scoreList[i].toString());
                console.log("Score value ", scoreList[i].score);
                if(typeof scoreList[i].score != "undefined" && scoreList[i].score != null && !scoreList[i].score) {
                    totalScore += scoreList[i].score.score;
                    scores.push(scoreList[i].score);
                }
            }

            callback(totalScore, scores);
        });
    });
};

/*
    Returns score statistics.
*/
TrainingDataDAO.prototype.getScoreHistory = function(trainingId, userid, callback) {
    var that = this;


    var matcher = {
        "trainingId":trainingId,
        "userId":userid,
        "mode":"training"
    };

    that.list(matcher, function(scoreList) {
        var iterationScores = [];

        for (var i = 0; i < scoreList.length; i++) {
            if(typeof scoreList[i].score !== "undefined") {
                if (typeof iterationScores[scoreList[i].trainingIteration] === "undefined") {
                    iterationScores[scoreList[i].trainingIteration] = 0;
                }
                if (scoreList[i].score != null) {

                    iterationScores[scoreList[i].trainingIteration] += scoreList[i].score.score;

                }
            }
        }

        callback(iterationScores);
    });
};

TrainingDataDAO.prototype.getPrePostScore = function(trainingId, userid, callback) {
    var that = this;

    var matcher = {
        "trainingId":trainingId,
        "userId":userid,
        "$or":[{"mode":"pre"}, {"mode":"post"}]
    };

    that.list(matcher, function(scoreList) {
        var preScore = 0;
        var postScore = 0;

        for (var i = 0; i < scoreList.length; i++) {
            if(typeof scoreList[i].score !== "undefined") {
                if (scoreList[i].mode === "pre") {
                   preScore += scoreList[i].score.score;
                }

                if(scoreList[i].mode === "post") {
                    postScore += scoreList[i].score.score;
                }
            }
        }

        callback(preScore, postScore);
    });

};

module.exports.BaseDAO = BaseDAO;

module.exports.UserDAO = new UserDAO();

module.exports.TestDAO = new TestDAO();
module.exports.FormDAO = new FormDAO();

module.exports.ExperimentDAO = new ExperimentDAO();
module.exports.DataDAO = new DataDAO();

module.exports.TrainingDAO = new TrainingDAO();
module.exports.TrainingDataDAO = new TrainingDataDAO();
