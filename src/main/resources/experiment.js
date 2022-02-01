var vertx = require("vertx");
var container = require('vertx/container');
var console = require('vertx/console');

var utils = require("utils");

var csvUtils = require("csvUtils");

var CustomMatcher = require('router');
var router = new CustomMatcher();

var templateManager = require('templateManager');

var experimentModel = require("models/Models").Experiment;
var experimentDAO = require("models/DAObjects").ExperimentDAO;

var dataDAO = require("models/DAObjects").DataDAO;

var formModel = require("models/Models").Form;
var dataModel = require("models/Models").Data;

var formDAO = require("models/DAObjects").FormDAO;
var testDAO = require("models/DAObjects").TestDAO;

var requireAdmin = require('middleware').requireAdmin;
var requireEditor = require('middleware').requireEditor;

var Promise = require("mPromise");

var bowser = require("node_modules/bowser/bowser");
//var lodash = require("node_modules/lodash");

var container = require('vertx/container');
var logger = container.logger

var config = container.config;
var externalPort = config.externalport;

var babyparser = require("libs/babyparse");

function swapUrlPort(url, newPort) {
  var uriRegex = /([a-zA-Z+.\-]+):\/\/([^\/]+):([0-9]+)\//;

  return url.replace(uriRegex, "$1://$2:" + newPort + "/");
}

function merge_options(obj1,obj2){
  var obj3 = {};
  for (var attrname in obj1) { obj3[attrname] = obj1[attrname]; }
  for (var attrname in obj2) { obj3[attrname] = obj2[attrname]; }
  return obj3;
}

router.get("/experiment", function(request){
  experimentDAO.list({users:request.session.currentUser.username},function(r){

    templateManager.render_template("experimentList", {"experiments":r}, request);
  });
});


router.get("/experiment/new", requireEditor,function(request){
  //templateManager.render_template("experimentform", {},request);
  var sDate = Date.now();
  var eDate = Date.now() + (1000*60*60*24*700);  //Two years into the future

  var expData = {};

  var newExp = new experimentModel();
  newExp.startDate = new Date(sDate);
  newExp.endDate = new Date(eDate);
  newExp.name = "";

  newExp.users = [request.session.currentUser.username];

  newExp.init(function(res, err) {
      console.log(JSON.stringify(res));

      request.redirect("/experiment/"+newExp._id+"/edit");
      request.response.end();

    });
});


//Shortned url
router.get("/e/:name",function(request){
  var name = request.params().get('name');

  experimentDAO.get({"shortname":name}, function(exp) {
    if(exp != "") {
      var q = "";
      if (request.query()) {
        q = "?" + request.query();
      }
      return request.redirect("/experiment/" + exp._id + q);
    }
    else {
      return request.notfound();
    }
  });
});


router.get('/experiment/:id', function(request){
  var id = request.params().get('id');
  var userAgent = request.headers().get("User-Agent");

  //Keeping stuff DRY
  function renderExp(exp, admin) {
    var experiment = exp;
    var hidelogin = false;

    //Replacing newlines with html linebreaks when displaying the description
    if(typeof experiment.description !== 'undefined') {
      experiment.description = experiment.description.replace(/(?:\r\n|\r|\n)/g, '<br />');
    }
    //console.log(JSON.stringify(r));
    if (admin) {
      experimentDAO.countParticipants(id, function(count) {
        experiment.participants = count;
        console.log(JSON.stringify(count));

        templateManager.render_template("experimentAdmin", {"exp":experiment, "hideLogin":hidelogin},request);
      });
    } else {

      if(typeof experiment.hidelogin !== undefined) {
        if(experiment.hidelogin){
          hidelogin = true;
        }
      }

      var res = bowser._detect(userAgent);
      var blockUa = res.tablet||res.mobile;

      if (experiment.allowMobile) {
        blockUa = false;
      }

      templateManager.render_template("experiment", {"exp":experiment, "hideLogin":hidelogin, "blockUa":blockUa},request);
    }
  }

  experimentDAO.get(id, function(exp) {
    //404 if experiment doesn't exist
    if(exp === "") {
      return request.notfound();
    }

    //If normal user, check if user has filled in something before
    if(!request.session.isTestLeader()) {
      var userID = request.session.getPersonToken();

      if(request.session.loggedIn()) {
        //userID = request.session.loggedIn().id;
        userID = request.session.getUserId();
      }

      /*
      Checking for userdata and generating it when needed.
      */
      dataDAO.getOrGenerateGeneral(userID, exp, request, function(userdata) {
        if(userdata.position > 0) {
            var expUrl = "/experiment/" + id + "/";

            request.redirect(expUrl + "phase/" + (userdata.position));
          }
        else {
          renderExp(exp, false);
        }
      });
    }
    //Admin, navigation controls dont apply here, just show the view
    else {

      if (exp.userHasAccess(request.session.currentUser)) {
        renderExp(exp, true);
      }
      else {
        return request.unauthorized();
      }
    }
  });
});

router.get('/experiment/:id/phase/:phase', function(request) {
  var expID = request.params().get('id');
  var phaseNo = request.params().get('phase');
  var phase;

  var userID = request.session.getPersonToken();

  if(request.session.loggedIn()) {
    userID = request.session.getUserId();
  }

  dataDAO.get({"userid":userID, "expId": expID, "type":"general"}, function(userdata) {
    var reg = /phase\/\d*/;

    var expUrl = "/experiment/" + expID + "/";

    //Checking if user has visited the landing page, if not redirect to first
    if(userdata === "") {
      console.log("No userdata, redirecting ");
      return request.redirect(expUrl);
    }

    //Checking if user is in the wrong phase, if  yes, redirect
    if(userdata.position != phaseNo) {
      console.log("Wrong position, redirecting to phase " + userdata.position);
      if (userdata.position == 0) {
        return request.redirect(expUrl);
      }
      return request.redirect(expUrl + "phase/" + (userdata.position));
    }

    else {
      experimentDAO.get(expID, function(exp) {
        phase = exp.components[phaseNo];

        //Redirecting to experiment end
        if(phase===undefined) {
          // var url = request.absoluteURI().toString();
          var url = request.absoluteExternalURI();

          // url = swapUrlPort(url, externalPort);

          var cut = url.indexOf("/phase/");
          url = url.substr(0,cut) + "/end";

          dataDAO.completeExperiment(expID, userID, function(status) {

            return request.redirect(url);
          });
        }
        else {
          if(exp.loginrequired && !request.session.loggedIn()) {
            var url = "/experiment/"+expID;
            return request.redirect(url);
          }

          //Calculating how much of the experiment is completed
          var noOfPhases = parseInt(exp.components.length);
          phaseNo = parseInt(phaseNo);
          var context = {"completed":(phaseNo+1)/noOfPhases*100, "phasesLeft":phaseNo+1+"/"+noOfPhases};


          if (typeof exp.hidelogin !== 'undefined') {
            context.hideLogin = exp.hidelogin;
          }

          if (exp.israndom) {
            console.log("------Translating phase number---------");
            console.log(phaseNo + " -> " + userdata.randomorder[phaseNo]);
            phase = exp.components[userdata.randomorder[phaseNo]];
          }

          //Formphase, rendering form template
          if(phase.type === "form") {
            console.log("Form ");

            if (exp.submitbutton) {
              context.submitbutton = exp.submitbutton;
            }

            formDAO.get(phase.id, function(form) {
              context.form = form.form;
              context.userID = userID;

              templateManager.render_template("formphase", context, request);
            });
          }
          //Testphases, rendering test template
          if(phase.type === "test") {
            console.log("test");

            testDAO.get(phase.id, function(experiment) {
              context.experiment = experiment.js.replace(/(\r\n|\n|\r)/gm,"");

              templateManager.render_template("testphase", context, request);
            });
          }

          if(phase.type === "video") {
            console.log("test");

            context.testConfig = {}
            context.testConfig.file = phase.videofile
            context.testConfig.recordingOnStart = phase.recordingOnStart
            context.testConfig.recordingAfterVideo = phase.recordingAfterVideo
            context.testConfig.recordAudioOnly = phase.recordAudioOnly
            context.testConfig.showVideoPreview = phase.showVideoPreview

            context.testConfig.description = phase.description
            context.testConfig.button = phase.button

            templateManager.render_template("videophase", context, request);
          }

          else {
            console.log(phase.type);
            console.log("Phase type is undefined");
          }
        }
      });
    }
  });
});


/*
/ Saves data from a certain phase, while also checking that the phase hasn't been
/
*/
router.post('/experiment/:id/phase/:phase', function(request) {
  var expID = request.params().get('id');
  var phase = request.params().get('phase');

  var data = new vertx.Buffer();

  request.dataHandler(function(buffer) {
    data.appendBuffer(buffer);
  });

  request.endHandler(function() {

    var userID = request.session.getPersonToken();
    if(request.session.loggedIn()) {
      userID = request.session.getUserId();
    }

    var postData = data.getString(0, data.length());
    var postJson = JSON.parse(postData);

    var expData = postJson.exp;
    var duration = postJson.duration;
    var score = postJson.score;

    var clientStartTime = postJson.clienttime;
    var timezone = postJson.timezone;

    var dataObj = new dataModel();

    dataObj.phase = parseInt(phase);
    dataObj.expId = expID;
    dataObj.userid = userID;
    dataObj.duration = duration;
    dataObj.score = score;

    dataObj.clientstarttime = clientStartTime;
    dataObj.timezone = timezone;

    dataObj.data = expData;

    experimentDAO.completePhase(dataObj, expID,function(r){
      //console.log(JSON.stringify(r));
      request.response.end(200);
    });
  });
});

router.post('/experiment/:id/phase/:phase/video', function (request) {

  request.expectMultiPart(true);

  var expID = request.params().get('id');
  var phase = request.params().get('phase');

  var data = new vertx.Buffer();

  request.uploadHandler(function (upload) {
    //var path = testImages + id + "/" + upload.filename()
    var userID = request.session.getPersonToken();
    if (request.session.loggedIn()) {
      userID = request.session.getUserId();
    }

    var fixedFilename = upload.filename();

    //Replacing and removing unwanted characters from filename
    fixedFilename = fixedFilename.replace(/[å+ä]/gi, "a");
    fixedFilename = fixedFilename.replace("ö", "o");
    fixedFilename = fixedFilename.replace(/[^a-z0-9+.]/gi, '_').toLowerCase();

    // var path = "upload/" + expID +"_video_"+ userID + "/" + fixedFilename;
    var videoRecordings = config.directory + "/exp_video_upload/";

    // vertx.fileSystem.mkDir(videoRecordings + expID, true, function (err, res) {

    var path = videoRecordings + expID +"/video_"+ userID + "_" + "p_" + phase + ".webm";
    //var path = testImages + "/" + id +"/" + upload.filename()
    console.log("Uploading image to " + path);
    upload.streamToFileSystem(path);
    // })

  });

  request.endHandler(function () {
    console.log("Upload done")
    request.response.end()
  })

})

router.get('/experiment/:id/end', function(request) {
  var expID = request.params().get('id');
  var userID = request.session.getPersonToken();

  if(request.session.loggedIn()) {
    userID = request.session.getUserId();
  }


  //dataDAO.completeExperiment(expID, userID, function(status) {
    console.log("confirmed submitted data");
    experimentDAO.get(expID, function(exp) {

      var endMessage = exp.endmessage;
      var endTitle = "";
      if(typeof endMessage !== 'undefined') {

        endTitle = endMessage.split('\n')[0];
        endMessage = endMessage.split("\n").slice(1).join("\n");

        endMessage = endMessage.replace(/(?:\r\n|\r|\n)/g, '<br />');
      }

      var context = {"endtitle":endTitle, "endmessage":endMessage};

      if (typeof exp.hidelogin !== 'undefined') {
        context.hideLogin = exp.hidelogin;
      }

      if (exp.mechanicalTurkEnabled) {
        dataDAO.getGeneral(userID, expID, function(userData) {
          context.endmessage = context.endmessage.replace("{turkToken}", userData.mechanicalTurkToken);

          return templateManager.render_template('end', context, request);
        });
      }
      else {
        return templateManager.render_template('end', context, request);
      }
    });

  //});
});

router.get('/experiment/:id/phase/:phase/json', function(request) {
  var expID = request.params().get('id');
  var phaseNo = request.params().get('phase');

  var userID = request.session.getPersonToken();

  if(request.session.loggedIn()) {
    userID = request.session.getUserId();
  }

  dataDAO.get({"userid":userID, "expId": expID, "type":"general"}, function(userdata) {
    phaseNo = userdata.position;

    if (userdata.randomorder) {
      phaseNo = userdata.randomorder[userdata.position];
    }

    experimentDAO.get(expID, function(exp) {
      var phase = exp.components[phaseNo];

      testDAO.get(phase.id, function(test) {

        request.response.end(test.js);
      });
    });
  });

});


router.get("/experiment/:id/edit", requireEditor, function(request) {
  var id = request.params().get('id');

  experimentDAO.get(id, function(exp) {
    if (exp.userHasAccess(request.session.currentUser)) {
      return templateManager.render_template('a_experimentEdit', {}, request);
    }
    return request.unauthorized();
  });
});

router.post("/experiment/:id/edit", requireEditor,function(request) {
  var id = request.params().get('id');
  var data = new vertx.Buffer();

  request.dataHandler(function(buffer){
    data.appendBuffer(buffer);
  });

  request.endHandler(function() {
    var dataObj = JSON.parse(data.getString(0, data.length()));
    experimentDAO.get(id, function(experiment) {

      console.log(JSON.stringify(experiment));

      experiment = merge_options(experiment, dataObj);

      experiment.save(function() {
        request.response.end(200);
      });
    });
  });
});


router.get("/experiment/:id/json",function(request) {
  var id = request.params().get('id');

  experimentDAO.get(id, function(experiment) {
    var js = experiment.toJson();

    request.response.putHeader("Content-Type", "application/json; charset=UTF-8");
    request.response.end(js);
  });
});

router.get("/experiment/:id/loaddata", requireEditor,function(request) {
  var id = request.params().get('id');

  //All/completed
  var filter1 = request.params().get("f1");

  //Raw/aggregate
  var filter2 = request.params().get("f2");
  //if raw -> phase
  var filter3 = request.params().get("f3");
  //if raw -> machine readable/huma readable
  var filter4 = request.params().get("f4");

  var offest = request.params().get("offset");
  var limit = request.params().get("limit");

  var startdate = request.params().get("startdate");
  var enddate = request.params().get("enddate");

  var matcher = {};
  var projection = {};

  var command = "single";

  if (filter1 === "videos") {
    var videoRecordings = config.directory + "/exp_video_upload/";

    return vertx.fileSystem.readDir(videoRecordings + id, function (err, res) {
      if (!err) {
        //files = res;
        var files = [];
        for (var i = 0; i < res.length; i++) {
          var img = res[i].toString();
          var file = {};
          file.url = img.substring(img.indexOf("exp_video_upload/"));
          file.name = img.substring(img.lastIndexOf("/") + 1);
          files.push(file);
        }

        console.log(JSON.stringify(files))
        request.response.putHeader("Content-Type", "text/csv; charset=utf-8");
        request.response.putHeader("Content-Disposition", "attachment; filename=data.csv");

        request.response.end(babyparser.unparse(files, { "delimiter": ";" }));
      }
    })
  }

  if (filter1 === "confirmed") {
    matcher.confirmed = true;
  }

  if (filter1 === "completions") {
    return dataDAO.getPhaseCompletionWithoutAggregate(id, function(res) {

      request.response.end(csvUtils.jsonArrayToCsv(res));
      //request.response.end(JSON.stringify(res));
    });
  }

  if (filter2 === "aggregate") {
    projection["data.rows"] = 0;
    //projection.data.raw = 0;

    if (filter3 === "test") {
      matcher.type = "test";
    }

    else if (filter3 === "all") {
      matcher.type = {$ne:"general"};
    }
    else if (filter3 === "form"){
      matcher.type = "form";
    } else {
      matcher.phase = parseInt(filter3)-1;
      matcher.type = {$ne:"general"};
    }
  }

  if (filter2 === "raw") {
    //projection.data = {};
    projection["data.single"] = 0;
    matcher.type === "test";

    matcher.phase = parseInt(filter3)-1;
    /*matcher.format = filter4;*/

    command = "raw";
  }

  if (filter2 === "metadata") {
    matcher.type = "general";

    command = "metadata";
  }

  var groupby = "userid";
  matcher.expId = id;

  // If start and enddate is set, add date filters to query
  if (startdate && enddate) {
    startdate = new Date(startdate);
    enddate = new Date(enddate);

    matcher.timestamp = {"$gte":startdate,"$lte":enddate};
  }


  dataDAO.rawQuery(matcher, function(res) {
    var csv = "";

    if (command === "single"){
      csv = csvUtils.jsonRowDataToCsv(res, groupby);
    }

    if (command === "raw") {
      if(filter4 === "standard") {
        csv = csvUtils.jsonMatrixDataToCsv(res, groupby);
      } else {
        csv = csvUtils.jsonMatrixToCsvSorted(res, groupby);
      }
    }

    if (command === "metadata") {
      csv = csvUtils.jsonArrayToCsv(res);
    }

    request.response.putHeader("Content-Type", "text/csv; charset=utf-8");
    request.response.putHeader("Content-Disposition", "attachment; filename=data.csv");

    request.response.end(csv);
  }, {keys:projection, batch_size:100, timeout:120000});
});


router.post("/experiment/:id/addform", requireEditor,function(request) {
  var id = request.params().get('id');
  var address = utils.get_address('questionnaire_render');

  var msg = {
    'markup': "",
    'action': "save"
  };

  vertx.eventBus.send(address, msg, function(reply) {
    var response = {};
    var id = reply.id;

    var form = new formModel();
    form._id = id;
    form.name = "";
    form.save(function() {
      request.response.putHeader("Content-Type", "application/json; charset=UTF-8");
      request.response.end(form.toJson());
    });
  });
});

var formsToClone = [];


function updateComponents(item) {
  console.log("MAPFUNC")
  if (item.type == "form") {
    item.old_id = item.id;
    item.id = utils.generateDuplicateId(item.id);
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

router.get("/experiment/:id/clone", requireEditor,function(request) {
  var id = request.params().get('id');
  logger.info("Trying to clone experiment");

  experimentDAO.get(id, function(exp) {
    logger.info("Cloning with model " + exp.name);

    exp.original_id = exp.id;
    exp._id = utils.generateDuplicateId(exp._id)
    exp.name = exp.name + "_copy"

    exp.components = exp.components.map(updateComponents)

    exp.init(function (re) {
      exp.saveP().then(function (res) {
        logger.info("Saved exp with id " + exp._id);

        var p = Promise.each(formsToClone, function (formItem) {
          return (getAndUpdateForm(formItem));
        });

        return p
      }).then(function (res) {
        return request.redirect("/experiment/" + exp._id);
      }).catch(function (res) {
        logger.error("Something didn't work")
        logger.error(res);
      })
    })
  })

});
router.post("/experiment/:id/addvideo", requireEditor,function(request) {
  var id = request.params().get('id');

  var uploadFilename = "";
  request.expectMultiPart(true);


  request.uploadHandler(function (upload) {
    //var path = testImages + id + "/" + upload.filename()
    console.log("UPLOADING VIDEO TO EXPERIMENT");
    var userID = request.session.getPersonToken();
    if (request.session.loggedIn()) {
      userID = request.session.getUserId();
    }

    var fixedFilename = upload.filename();

    //Replacing and removing unwanted characters from filename
    fixedFilename = fixedFilename.replace(/[å+ä]/gi, "a");
    fixedFilename = fixedFilename.replace("ö", "o");
    fixedFilename = fixedFilename.replace(/[^a-z0-9+.]/gi, '_').toLowerCase();

    // var path = "upload/" + expID +"_video_"+ userID + "/" + fixedFilename;
    var videoRecordings = config.directory + "/testvideos/"+ id + "";

    // vertx.fileSystem.mkDir(videoRecordings, true, function (err, res) {

    var path = videoRecordings + "/" + fixedFilename;
    console.log("Uploading Video to " + path);
    uploadFilename = "/testvideos/" + id + "/" + fixedFilename;

    upload.streamToFileSystem(path);
    // })

  });

  request.endHandler(function () {
    console.log("ADD VIDEO ENDHANDLER")
    request.response.putHeader("Content-Type", "application/json; charset=UTF-8");
    request.response.end(JSON.stringify({"video":uploadFilename}))
  })
});
