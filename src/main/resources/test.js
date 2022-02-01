var vertx = require("vertx");
var CustomMatcher = require('router');
var console = require('vertx/console');

var templateManager = require('templateManager');
var router = new CustomMatcher();

var utils = require("utils");
var mongo = require('mongoHandler');

var testModel = require('models/Models').Test;
var testDAO = require("models/DAObjects").TestDAO;

var container = require('vertx/container');
var config = container.config;
var testImages = config.directory + "/testimages";

var requireAdmin = require('middleware').requireAdmin;
var requireEditor = require('middleware').requireEditor;

router.get('/test', function(request) {
 testDAO.rawQuery({}, function(tests) {
    templateManager.render_template('testlist', {"tests":r.results},request);
  },{keys:{js:0, code:0}});
});

router.get('/test/json', function(request) {
 /* mongo.test.list(function(r) {

    request.response.putHeader("Content-Type", "application/json; charset=UTF-8");
    request.response.end(JSON.stringify(r.results));
  });*/
  var query = {};
  if (request.session.currentUser.isEditor() && !request.session.currentUser.isAdmin()) {
    query = {users:request.session.currentUser.username}
  }

  testDAO.rawQuery(query, function(tests) {
    request.response.putHeader("Content-Type", "application/json; charset=UTF-8");
    request.response.end(JSON.stringify(tests));
  },{keys:{js:0, code:0}});
});

router.get("/test/folder/json", requireEditor, function(request) {
  testDAO.listFolders(request.session.currentUser, function(folders) {
    console.log(JSON.stringify(folders));

    request.response.putHeader("Content-Type", "application/json; charset=UTF-8");
    request.response.end(JSON.stringify(folders));
  });
});

router.get("/test/json/compiled", requireEditor, function(request) {
 /* testDAO.list({"compiled":true}, function(result) {
    request.response.putHeader("Content-Type", "application/json; charset=UTF-8");
    request.response.end(JSON.stringify(result));
  });*/
  var query =  {compiled:true};
  if(request.session.currentUser.isEditor()){
    query.users = request.session.currentUser.username;
  }

  testDAO.rawQuery(query, function(tests) {
    request.response.putHeader("Content-Type", "application/json; charset=UTF-8");
    request.response.end(JSON.stringify(tests));
  },{keys:{js:0, code:0}});

});

router.get("/test/folder/:foldername/json", requireEditor, function(request) {
  var folder = request.params().get('foldername');

  var query = {"folder":folder};

  if (folder == "unspecified") {
    query = {$or: [{"folder" : { "$exists" : false }}, {folder:""}] };
  }

 /* testDAO.list(query,function(result) {
    request.response.putHeader("Content-Type", "application/json; charset=UTF-8");
    request.response.end(JSON.stringify(result));
  });*/

  testDAO.rawQuery(query, function(tests) {
    request.response.putHeader("Content-Type", "application/json; charset=UTF-8");
    request.response.end(JSON.stringify(tests));
  },{keys:{js:0, code:0}});
});

router.post("/test", requireEditor,function(request) {
  var data = new vertx.Buffer();

  request.dataHandler(function(buffer) {
    data.appendBuffer(buffer);
  });

  request.endHandler(function() {

    data = data.getString(0, data.length());
    var params = utils.getUrlParams(data);
    var name = decodeURIComponent(params.name).split("+").join(" ");

    console.log("name1: " + name);

    var test = new testModel();

    test.name = name;

    // Add edit access to current user
    if(!request.session.currentUser.isAdmin()) {
      test.users = [request.session.currentUser.username];
    }

    test.init(function(err, result) {
      return request.redirect("/test/"+test._id);
    });
  });
});


router.get('/test/:id', requireEditor, function(request) {
  var id = request.params().get('id');
  var code = "sadas";
  var files = [];
  vertx.fileSystem.readDir(testImages + "/" + id, function(err, res) {
    if (!err) {
      //files = res;
       for (var i = 0; i < res.length; i++) {
          var img = res[i].toString();
          var file = {};
          file.url = img.substring(img.indexOf("testimages"));
          file.name = img.substring(img.lastIndexOf("/")+1);
          files.push(file);
        }
        console.log(JSON.stringify(files));
        console.log("\n\n\n");
    }
    testDAO.get(id, function(test) {

      templateManager.render_template('testEditor',
        {"code":test.code, "test":test, "files":files}, request);
      /*var user = request.session.currentUser;
      if (test.userHasAccess(user) || user.isAdmin()) {
      } else {
        return request.unauthorized();
      }*/
    });
  });
});

router.post("/test/:id", requireEditor, function(request) {
  var data = new vertx.Buffer();
  var id = request.params().get('id');

  request.dataHandler(function(buffer) {
    data.appendBuffer(buffer);
  });

  request.endHandler(function() {

    data = data.getString(0, data.length());
    data = JSON.parse(data);

    var name = data.name;
    var published = data.published;
    var allCanEdit = data.allCanEdit;
    var folder = data.folder;
    var users = data.users;

    if (typeof folder === "undefined" || folder === "") {
      folder = "Unspecified";
    }

    testDAO.get(id, function(test) {

      test.name = name;
      test.published = published;
      test.allCanEdit = allCanEdit;
      test.folder = folder;
      test.users = data.users;

      test.save(function(response) {
        request.response.putHeader("Content-Type", "application/json; charset=UTF-8");
        request.response.end(JSON.stringify({"status":"ok"}));
      });
    });
  });
});

router.get('/test/:id/copy', requireEditor, function(request) {
  var id = request.params().get('id');

  testDAO.get(id, function(test) {
    test.copy(request.session.getUserId(), function(newTest) {
      request.redirect("/test/" + newTest._id);
    });
  });
});

router.post("/test/:id/compile", requireEditor, function(request) {
  var data = new vertx.Buffer();
  var id = request.params().get('id');

  request.dataHandler(function(buffer) {
    data.appendBuffer(buffer);
  });

  request.endHandler(function() {

    data = data.getString(0, data.length());
    var code = JSON.parse(data).code;

    testDAO.get(id, function(test) {

      test.compile(code, function(response) {

        request.response.putHeader("Content-Type", "application/json; charset=UTF-8");
        request.response.end(JSON.stringify(response));
      });
    });
  });
});


router.post("/test/:id/imageupload", requireEditor,function(request) {

  request.expectMultiPart(true);
  var id = request.params().get('id');

  request.uploadHandler(function(upload) {
      //var path = testImages + id + "/" + upload.filename()
      var fixedFilename = upload.filename();

      //Replacing and removing unwanted characters from filename
      fixedFilename = fixedFilename.replace(/[å+ä]/gi, "a");
      fixedFilename = fixedFilename.replace("ö", "o");
      fixedFilename = fixedFilename.replace(/[^a-z0-9+.]/gi, '_').toLowerCase();

      var path = testImages + "/" + id +"/" + fixedFilename;
      //var path = testImages + "/" + id +"/" + upload.filename()
      console.log("Uploading image to "+ path);
      upload.streamToFileSystem(path);
  });

  request.endHandler(function() {

    console.log("Upload");

    request.response.end(200);
  });
});


router.delete("/test/:id/imageupload/:imageName", requireEditor,function(request) {
  var id = request.params().get('id');
  var imgName = request.params().get('imageName');

  console.log("DELETING IMAGE");

  request.endHandler(function() {
    var filename = testImages + "/" + id + "/" + imgName;
    vertx.fileSystem.delete(filename, function(err, res) {
      if(!err) {
            request.response.end(200);
          }
      else {
        request.response.end(400);
      }
    });
  });
});


router.get('/test/:id/imagelist', requireEditor,function(request) {
  var id = request.params().get('id');
  var files = [];

  function sortByName(a, b) {
    if (a.name < b.name)
        return -1;
    else if (a.name > b.name) {
      return 1;
    }else {
      return 0;
    }
  }

  vertx.fileSystem.readDir(testImages + "/" + id, function(err, res) {
    if (!err) {
      //files = res;
      for (var i = 0; i < res.length; i++) {
        var img = res[i].toString();
        var file = {};
        file.url = img.substring(img.indexOf("testimages"));
        file.name = img.substring(img.lastIndexOf("/")+1);
        files.push(file);
      }

      files = files.sort(sortByName);

      var fileJSON = JSON.stringify(files);
      request.response.end(fileJSON);
    }

    else {
      request.notfound();
    }

  });
});


router.post('/test/:id/editname', requireEditor,function(request) {
  var id = request.params().get('id');
  var data = new vertx.Buffer();

  request.dataHandler(function(buffer){
    data.appendBuffer(buffer);
  });

  request.endHandler(function() {
    var jsonData = (JSON.parse(data.getString(0, data.length())));

    var name = jsonData.name;

    mongo.test.editName(id, name, function(r){
      console.log(JSON.stringify(r));
      request.response.end(JSON.stringify(r.result));
    });
  });
});

router.get('/test/:id/json', requireEditor, function(request) {
  var id = request.params().get('id');

  testDAO.get(id, function(test) {
    var json = test.toJson();

    request.response.putHeader("Content-Type", "application/json; charset=UTF-8");
    request.response.end(json);
  });

});

router.get("/tools/mergetool", requireEditor, function(request) {

  templateManager.render_template("mergeToolTemplate",{},request);

});