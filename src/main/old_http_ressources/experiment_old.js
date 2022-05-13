var vertx = require("vertx");
var CustomMatcher = require('router');
var console = require('vertx/console');

var templateManager = require('templateManager');
var router = new CustomMatcher();

var utils = require("utils");
var mongo = require('mongoHandler');

//var requireAdmin = utils.requireAdmin;
var middle = require("middleware");
var requireAdmin = middle.requireAdmin;


var testDAO = require("models/DAObjects").TestDAO;

var container = require('vertx/container');
var config = container.config;
var testImages = config.directory + "/testimages";

var babyparser = require("libs/babyparse");

router.get("/old_experiment", function(request){
  mongo.experiment.list([], function(r){

    templateManager.render_template("experimentList", {"experiments":r.results}, request);
  });
});


/*
Creates a new empty experiment and redirects the user to the new 
experiments' edit page
*/
router.get("/old_experiment/new", function(request){
  //templateManager.render_template("experimentform", {},request);
  var sDate = Date.now();
  var eDate = Date.now() + (1000*60*60*24*30);  //30 days in the future

  var expData = {};

  expData.name =  "";
  expData.startDate = new Date(sDate);
  expData.endDate = new Date(eDate);

  mongo.experiment.save(expData, function(r){
      console.log(JSON.stringify(r));
      var resp = {
        "status":"ok",
        "id":r._id
      };
      request.redirect("/experiment/"+r._id+"/edit");
      request.response.end();

    });
});


router.post("/old_experiment/new", function(request) {
  var data = new vertx.Buffer();

  request.dataHandler(function(buffer){
    data.appendBuffer(buffer);
  });

  request.endHandler(function() {

    var jsonData = JSON.parse(data.getString(0, data.length()));
    console.log(data.getString(0, data.length())); 

    var sDate = new Date(jsonData.startDate);
    var eDate = new Date(jsonData.endDate);

    if(jsonData.name === "") {
      jsonData.name = "Unnamed experiment";
    }


    console.log(sDate.toString());

    mongo.experiment.save(jsonData, function(r){
      console.log(JSON.stringify(r));
      var resp = {
        "status":"ok",
        "id":r._id
      };
      request.response.putHeader("Content-Type", "application/json; charset=UTF-8");
      request.response.end(JSON.stringify(resp));

    });


    //request.response.end({"status":"ok","id":id});
  });
});


router.get('/old_experiment/:id', function(request){
  var id = request.params().get('id');

  //Keeping stuff DRY
  function renderExp(r, admin) {
    var experiment = r.result;
    var hidelogin = false;

    if(typeof experiment.hidelogin !== undefined) {
      if(experiment.hidelogin){
        hidelogin = true;
      }
    }

    //Replacing newlines with html linebreaks when displaying the description
    if(typeof experiment.description !== 'undefined') {
      experiment.description = experiment.description.replace(/(?:\r\n|\r|\n)/g, '<br />');
    }
    //console.log(JSON.stringify(r));
    if (admin) {
      templateManager.render_template("experimentAdmin", {"exp":experiment, "hideLogin":hidelogin},request);
    } else{
      templateManager.render_template("experiment", {"exp":experiment, "hideLogin":hidelogin},request);
    }
  }

  mongo.experiment.get(id,function(r){
    //404 if experiment doesn't exist
    if(!r.result) {
      return request.notfound();
    }

    var exp = r.result;

    //If normal user, check if user has filled in something before
    if(!request.session.isAdmin()) {
      var userID = request.session.getPersonToken();

      if(request.session.loggedIn()) {
        userID = request.session.loggedIn().id;
      }

      /*
      Checking for userdata and generating it when needed.
      */
      mongo.experiment.getUserData(userID, id, function(userdata) {
        if (userdata) {
          console.log("Userdata exists " + JSON.stringify(userdata));
          //Redirect to right phase if available
          if(userdata.position > 0) {          
            request.redirect(request.absoluteURI() + "/phase/" + (userdata.position));
          }
          else{
            renderExp(r);
          }
        }
        else { 
          console.log("No userdata");

          var referer = "direct";

          if(typeof request.headers().get("Referer") != 'undefined'){
            referer  = request.headers().get("Referer");
          }

          var userAgent = request.headers().get("User-Agent");

          var userdata = {};
          userdata.position = 0;
          userdata.randomorder = false;

          userdata.userAgent = userAgent;

          userdata.referer = referer;

          if (exp.israndom) {
            var order = mongo.experiment.generateRandomOrder(exp);
            console.log("Generated random order " + JSON.stringify(order));
            userdata.randomorder = order; 
          }

          //Generating token if this is a turk experiment
          if (exp.mechanicalTurkEnabled) {
            userdata.mechanicalTurkToken = utils.randomAlphaNumeric(10);
          }

          mongo.experiment.initUserData(userdata, userID, exp._id, function(r2){
            renderExp(r, false);
          });
        }
      });
    } 
    //Admin, navigation controls dont apply here, just show the view
    else {
      mongo.experiment.countParticipants(id, function(r2) {
        r.result.participants = r2;
        console.log(JSON.stringify(r));
        renderExp(r, true); 
      });
    }
  });
});


router.get('/old_experiment/:id/edit', requireAdmin, function(request){

  var id = request.params().get('id');
  console.log(id);

  mongo.experiment.get(id,function(r){
    var experiment = r.result;
    console.log(JSON.stringify(r));
    templateManager.render_template("editexperiment", {"exp":experiment},request);
  });
 
});


router.post('/old_experiment/:id/edit', requireAdmin, function(request){
    var data = new vertx.Buffer();
    var id = request.params().get('id');

    request.dataHandler(function(buffer){
      data.appendBuffer(buffer);
    });

    request.endHandler(function() {

      var jsonData = JSON.parse(data.getString(0, data.length()));
      console.log(data.getString(0, data.length())); 

      var sDate = new Date(jsonData.startDate);
      var eDate = new Date(jsonData.endDate);

      var loginRequired = jsonData.loginrequired;
      var hidelogin = jsonData.hidelogin;

      console.log(sDate.toString());

      mongo.experiment.update(jsonData, id,function(r){
        console.log(JSON.stringify(r));
        var resp = r;
        request.response.putHeader("Content-Type", "application/json; charset=UTF-8");
        request.response.end(JSON.stringify(resp));

      });
      //request.response.end({"status":"ok","id":id});
    });
});

router.post('/old_experiment/:id/addform', requireAdmin, function(request){

  var address = utils.get_address('questionnaire_render');
  var expId = request.params().get('id');


  // http://nelsonwells.net/2012/02/json-stringify-with-mapped-variables/#more-153
  var msg = {
    'markup': "",
    'action': "save"
  };

  vertx.eventBus.send(address, msg, function(reply) {
    var response = {};
    var id = reply.id;
    mongo.experiment.addForm(expId,id,"Unnamed Form", function(r){
      response.id = id;

      request.response.putHeader("Content-Type", "application/json; charset=UTF-8");
      request.response.end(JSON.stringify(response));
    });
  });
});


router.post('/old_experiment/:id/editformname', requireAdmin, function(request){
  var expId = request.params().get('id');
  var data = new vertx.Buffer();

  request.dataHandler(function(buffer){
    data.appendBuffer(buffer);
  });

  request.endHandler(function() {
    var jsonData = (JSON.parse(data.getString(0, data.length())));
    console.log(JSON.stringify(jsonData));

    var name = jsonData.name;
    var formid = jsonData.id; 

    mongo.experiment.editFormName(expId, formid, name, function(r){
      console.log(JSON.stringify(r));
      request.response.end(JSON.stringify(r.result));
    });
  });
});


router.post("/old_experiment/:id/addtest", requireAdmin, function(request) {
  var expId = request.params().get('id');
  var data = new vertx.Buffer();

  request.dataHandler(function(buffer) {
    data.appendBuffer(buffer);
  });

  request.endHandler(function() {

    data = data.getString(0, data.length());
    data = JSON.parse(data);

    if (data.name === "" || data.testId === "") {
      return request.response.end(JSON.stringify({error:"No experiment specified"}));
    }

    mongo.experiment.addTest(expId, data.testId, data.name, function(r) {
      
      var resp = r;
      resp.name = data.name;
      resp.id = data.testId;
     
      console.log(JSON.stringify(resp));
      request.response.end(JSON.stringify(resp));
    });
  });
});


router.post("/old_experiment/:id/randomizeorder", requireAdmin, function(request) {
  var expId = request.params().get('id');
  var data = new vertx.Buffer();

  request.dataHandler(function(buffer) {
    data.appendBuffer(buffer);
  });

  request.endHandler(function() {
    var jsonData = (JSON.parse(data.getString(0, data.length())));

    console.log(JSON.stringify(jsonData));

    mongo.experiment.setRandom(expId, jsonData.index, jsonData.value, function(r) {
      console.log("Setting random, phase: " + jsonData.index + " v:" + jsonData.value);
      request.response.end("Ending");
    });
  });
});


router.post('/old_experiment/:id/deletecomponent', requireAdmin, function(request) {
  var expId = request.params().get('id');
  var data = new vertx.Buffer();

  request.dataHandler(function(buffer){
    data.appendBuffer(buffer);
  });

  request.endHandler(function() {
    var jsonData = (JSON.parse(data.getString(0, data.length())));
    console.log(JSON.stringify(jsonData));

    mongo.experiment.deleteComponentByIndex(expId, jsonData.index, function(r) {
      console.log(JSON.stringify(r));

      request.response.end(JSON.stringify(r.result));
    });

  });
});


router.get('/old_experiment/:id/json', function(request){
  var expId = request.params().get('id');

  mongo.experiment.get(expId, function(r){
    request.response.putHeader("Content-Type", "application/json; charset=UTF-8");
    request.response.end(JSON.stringify(r.result));
  });
});


//Shows a specific phase, if phase doesn't exist, assume last phase
//and redirect to some kind of final page
router.get('/old_experiment/:id/phase/:phase', function(request) {
  var expID = request.params().get('id');
  var phaseNo = request.params().get('phase');
  var phase;

  var userID = request.session.getPersonToken();

  mongo.experiment.userPosition(userID, expID, function(userdata) {
    var reg = /phase\/\d*/;

    //Checking if user has visited the landing page
    if(!userdata) {
      console.log("No userdata, redirecting ");
      return request.redirect(request.absoluteURI().toString().replace(reg,""));
    }

    //Checking if user is in the wrong phase
    if(userdata.position != phaseNo) {
      console.log("Wrong position, redirecting to phase " + userdata.position);
      if (userdata.position == 0) {
        return request.redirect(request.absoluteURI().toString().replace(reg,""));
      }
      return request.redirect(request.absoluteURI().toString().replace(reg, "phase/" + (userdata.position)));
    } 

    else {
      mongo.experiment.get(expID, function(r) {
        var exp  = r.result; 
        phase = exp.components[phaseNo];

        //Redirecting to experiment end
        if(phase===undefined) {
          var url = request.absoluteURI().toString();
          var cut = url.indexOf("/phase/");
          console.log(cut);
          url = url.substr(0,cut) + "/end";
          console.log(url);

          return request.redirect(url);
        }

        if(r.result.loginrequired && !request.session.loggedIn()) {
          var url = "/experiment/"+expID;
          return request.redirect(url);
        }

        //Calculating how much of the experiment is completed
        var noOfPhases = parseInt(r.result.components.length);
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

          mongo.form.get(phase.id, function(r2) {
            var form = r2.result.form;
            context.form = form;

            templateManager.render_template("formphase", context, request);
          });
        }
        //Testphases, rendering test template
        if(phase.type === "test") {
          console.log("test");


          mongo.test.get(phase.id, function(r2) {
            console.log(JSON.stringify(r2));
            var experiments = r2.result.js;
            context.experiment = experiments.replace(/(\r\n|\n|\r)/gm,"");

            templateManager.render_template("testphase", context, request);
          });
        }
        
        else {
          console.log(phase.type);
          console.log("Phase type is undefined");
        }
      });
    }
  });
});


router.get('/old_experiment/:id/phase/:phase/json', function(request) {
  var expID = request.params().get('id');
  var phaseNo = request.params().get('phase'); 
  var phase;

  var userID = request.session.getPersonToken();


  mongo.experiment.userPosition(userID, expID, function(userdata) {
    phaseNo = userdata.position;
    if (userdata.randomorder) {
      phaseNo = userdata.randomorder[userdata.position];
    }
    mongo.experiment.get(expID, function(r) {
      phase = r.result.components[phaseNo];

      mongo.test.get(phase.id, function(r2) {

        request.response.end(r2.result.js);
      });
    });
  });

});


//Records data from a certain phase,
router.post('/old_experiment/:id/phase/:phase', function(request) {
  var expID = request.params().get('id');
  var phase = request.params().get('phase');

  var userID = request.session.getPersonToken();
  if(request.session.loggedIn()) {
    userID = request.session.loggedIn().id;
  }

  var data = new vertx.Buffer();

  request.dataHandler(function(buffer) {
    data.appendBuffer(buffer);
  });

  request.endHandler(function() {
    var postData = data.getString(0, data.length());
    var postJson = JSON.parse(postData);

    var expData = postJson.exp;
    var duration = postJson.duration;
    var score = postJson.score;

    console.log(JSON.stringify(postJson));

    mongo.experiment.saveData(phase, expID, expData, duration, score,userID, function(r){
      //console.log(JSON.stringify(r));
      request.response.end(200);
    });
  });
});


router.get('/old_experiment/:id/end', function(request) {
  var expID = request.params().get('id');
  var userID = request.session.getPersonToken();
  if(request.session.loggedIn()) {
    userID = request.session.loggedIn().id;
  }


  mongo.experiment.confirmData(expID, userID, function(r) {
    console.log("confirmed submitted data");
    console.log(JSON.stringify(r));
    mongo.experiment.get(expID, function(exp) {

      exp = exp.result;
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
        mongo.experiment.userPosition(userID, expID, function(userData) {
          
          context.endmessage = context.endmessage.replace("{turkToken}", userData.mechanicalTurkToken);

          return templateManager.render_template('end', context, request);
        });
      }
      else {
        return templateManager.render_template('end', context, request);
      }
    });
  });

});


//Performs a custom crafted join on gathered data.
router.get('/experiment/:id/data', requireAdmin, function(request) {
  var expID = request.params().get('id');
  mongo.experiment.formData(expID, function(r) {
      
    var data = r.results;


    var fields = [];
    var userData = {};
  
    var semiColRegEx = RegExp(";","g");

    var headerSet = {};

    //finding max phase an
//    console.log(JSON.stringify(data));
    for(var i in data) {
      var item = data[i];
      phase = parseInt(item.phase);

      var phaseName = " phase_" + phase;

      if(!("userid" in item)) {
        item.userid = "Missing";
      }

      if(!userData[item.userid]) {
        userData[item.userid] = {};
      }

      var removeExtra = false;
      var checkForExtra = true;

      // Writing each users different phases to a single object.
      for(var j in item.data) {
          // checking for the useless quistionnaire-id at the beginning of keys, and removing it if present
          if(checkForExtra) {
            if(j.slice(0,17) === "questionnaire-id:") {
              removeExtra = true;
            }
          }
          checkForExtra = false;

          var headerName = null;
          if(removeExtra) {
            headerName = j.slice(17, j.length).replace(semiColRegEx,"_") + phaseName;
          } else {
            headerName = j.replace(semiColRegEx,"_") + phaseName;
          }
          if(typeof item.data[j] !== 'undefined' &&
             item.data[j] !== null) {
            userData[item.userid][headerName] = (item.data[j].toString().replace(semiColRegEx,"_"));
          }
          headerSet[headerName] = "";
       // }
      }
    }

    headerSet["userid"] = "";

    var userArr = [];
    for(var ud in userData) {
      userData[ud]["userid"] = ud;
      userArr.push(userData[ud]);
    }

    userArr.unshift(headerSet);

    console.log(JSON.stringify(userArr))

    var csv = babyparser.unparse(userArr, {"delimiter":";"});

    request.response.putHeader("Content-Type", "text/csv; charset=utf-8");
    request.response.putHeader("Content-Disposition", "attachment; filename=questioneerdata.csv");

    //request.response.end("\ufeff " + stringFields+"\n"+ userFields);
    request.response.end("\ufeff " + csv);
  });
});


// Does pretty much the same as the form data method, 
// Might generate empty fields when using phase no as array index
router.get('/experiment/:id/testdata', requireAdmin, function(request) {

  var expID = request.params().get('id');

  mongo.experiment.testData(expID, function(r) {
    mongo.experiment.get(expID, function(expRes) {

      var data = r.results;
      var sep = ";";

      var exp = expRes.result;
      var phaseNames = "";
      var phaseNameArray = [];

      var headerSet = {};


      //phaseNameArray.push("");

    //console.log(JSON.stringify(data));

      var fields = [];
      var userData = {};
      for(var i = 0; i < data.length; i++) {
        var item = data[i];
        console.log("\n" + i + " Index\n"  + JSON.stringify(item));
        item.single = item.data.single;
          
        var phase = parseInt(item.phase);

        if(!("userid" in item)) {
          item.userid = "Missing";
        }

        if(!userData[item.userid]) {
          userData[item.userid] = {};
        }

        //Writing headers
        if(!fields[phase]) {

          //indexes before phase will be set to null
          fields[phase] = [];

          phaseNameArray.push(exp.components[phase].name);
          var prop;
          for (prop in item.single) {
            fields[phase].push(prop);
            phaseNameArray.push("");
          }
          //Removing the last empty to account for the extra named field in the 
          //beginning
          phaseNameArray.pop();
        }

        if(!userData[item.userid]) {
          userData[item.userid] = [];
        }

        for(var j in item.single) {
            var headerName = j + "  " + exp.components[phase].name+phase;

            userData[item.userid][headerName] = item.single[j];
            headerSet[headerName] = "";
        }
      }

      headerSet["userid"] = "";

      var userArr = [];
      for(var ud in userData) {
        userData[ud]["userid"] = ud;
        userArr.push(userData[ud]);
      }    

      userArr.unshift(headerSet);

      var csv = babyparser.unparse(userArr, {"delimiter":";"});
      phaseNames = phaseNameArray.join(sep);

      request.response.putHeader("Content-Type", "text/csv; charset=utf-8");
      request.response.putHeader("Content-Disposition", "attachment; filename=testdata.csv");

      request.response.end("\ufeff " + phaseNames + "\n" + csv);
    });
  });
 
});
// /experiment/:id/phase/:phase/rawdata'
router.get('/experiment/:id/rawdata', requireAdmin, function(request) {
  var expID = request.params().get('id');
  mongo.experiment.testData(expID, function(r) {
    var data = r.results;

    request.response.end(JSON.stringify(data));
  });
});


// Returns raw tesdata from a testphase as a csv, data is formatted 
// Doesn't format correcly if some fields are missing from the data
router.get('/experiment/:id/phase/:phase/rawdata', requireAdmin, function(request) {
  var expId = request.params().get('id');
  var phase = request.params().get('phase');
  mongo.experiment.rawTestData(expId, phase, function(r) {
    var data = r.results;
    var sep =";";

    var csvData = "";
    

    for (var i = 0; i < data.length; i++) {
      var element = data[i];
      var keys = {};

      csvData += "userID: " + sep +  element.userid + sep + "\n";
      //console.log("RawData number of rows: " + element.data.rows.length);
      var rowCount = element.data.rows.length;
      for (var j = 0; j < element.data.rows.length; j++) {
        var row = element.data.rows[j];
        for (var rkey in row) {
          if (keys.hasOwnProperty(rkey)) {
            keys[rkey][j] = JSON.stringify(row[rkey]);
          }else {
            keys[rkey] = [];
            keys[rkey][j] = JSON.stringify(row[rkey]);
          }
        }
      }

      var csv = "";
      var lastK = "";
      
      //Building headers
      for(var k in keys) {
        csv += k + sep;
        lastK = k;
      }

      //console.log(csv + "\n");
      csv += "\n";
      
      /*
        Skriver ut resultatet till csv:n
      */
      for (var ij = 0; ij < rowCount; ij++) {
        for(var k in keys) {
          if ( keys[k][ij] !== undefined) {
            csv += keys[k][ij] + sep;
            
          } else{
            csv += sep;
          }
        }
        csv += "\n"; 
      }

      csvData += csv;

    }


    request.response.putHeader("Content-Type", "text/csv; charset=utf-8");
    request.response.putHeader("Content-Disposition", "attachment; filename=phase"+phase+"RawData.csv");

    request.response.end("\ufeff " + csvData);
  });


});

// Outputs 2d data where the useris is stored in every row, making it easier to perform various operation 
// on the data. 
router.get('/experiment/:id/phase/:phase/rawdata_pivot', requireAdmin, function(request) {
  var expId = request.params().get('id');
  var phase = request.params().get('phase');
  mongo.experiment.rawTestData(expId, phase, function(r) {
    var data = r.results;
    var sep =";";

    var csvData = "";

    var keys = {};

    keys.userid = [];

    var totalRows = 0;

    var rowcounter = 0;
    
    for (var i = 0; i < data.length; i++) {
      var element = data[i];

      //csvData += "userID: " + sep +  element.userid + sep + "\n";
      //console.log("RawData number of rows: " + element.data.rows.length);
      var rowCount = element.data.rows.length;
      for (var j = 0; j < element.data.rows.length; j++) {
        var row = element.data.rows[j];
        for (var rkey in row) {
          if (keys.hasOwnProperty(rkey)) {
            //keys[rkey][j] = JSON.stringify(row[rkey]);
            keys[rkey][rowcounter] = JSON.stringify(row[rkey]);
          }else {
            keys[rkey] = [];
            //keys[rkey][j] = JSON.stringify(row[rkey]);
            keys[rkey][rowcounter] = JSON.stringify(row[rkey]);
          }
          //keys.userid[j] = element.userid;
          keys.userid[rowcounter] = element.userid;

        }
          rowcounter += 1
      }
      totalRows += rowCount;
    }

    totalRows = rowcounter;

    var csv = "";
    var lastK = "";
    
    //Building headers
    for(var k in keys) {
      csv += k + sep;
      lastK = k;
    }

    //console.log(csv + "\n");
    csv += "\n";
    
    /*
      Skriver ut resultatet till csv:n
    */
    //for (var ij = 0; ij < keys[lastK].length; ij++) {
    for (var ij = 0; ij < totalRows; ij++) {
      for(var k in keys) {
        if ( keys[k][ij] !== undefined) {
          csv += keys[k][ij] + sep;
          
        } else{
          csv += sep;
        }
      }
      csv += "\n"; 
    }

    csvData += csv;

  
    request.response.putHeader("Content-Type", "text/csv; charset=utf-8");
    request.response.putHeader("Content-Disposition", "attachment; filename=phase"+phase+"RawDataPivot.csv");

    request.response.end("\ufeff " + csvData);
  });
});

/*
router.get('/test/demo', function(request) {
  var file = 'demo.html';
  request.response.sendFile(utils.file_from_serverdir(file));
});


router.post('/test/run', function(request) {
  var body = new vertx.Buffer();

  request.dataHandler(function(buffer) {
    body.appendBuffer(buffer);
  });
  request.endHandler(function() {
    var address = utils.get_address('experiment_language');
    var eb = vertx.eventBus;
    var msg = {
      'code': body.getString(0, body.length())
    };

    eb.send(address, msg, function(reply) {
      var response = {};

      if (reply.hasOwnProperty('errors') === true) {
        response.errors = reply.errors;
      } else {
        response.code = reply.code;
      }
      request.response.putHeader("Content-Type", "application/json; charset=UTF-8");
      request.response.end(JSON.stringify(response));
    });
  });
  *
});

*/