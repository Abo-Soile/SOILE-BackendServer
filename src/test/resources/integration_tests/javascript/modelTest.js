var vertx = require("vertx");
var container = require("vertx/container");
var vertxTests = require("vertx_tests");
var vassert = require("vertx_assert");

var console = require('vertx/console');

var mongo = require("mongoHandler");
var async = require("async");

var models = require("models/Models");
var dao = require('models/DAObjects');

var userDao = dao.UserDAO;

var dataDAO = dao.DataDAO;
var Data = models.Data;

var dataCount = 200;

var mongoConfig = {
  "address": "vertx.mongo-persistor",
  "host": "127.0.0.1",
  "port": 27017,
  "db_name": "soilemodeltest"
};

function resetMongo(callback) {
  vertx.eventBus.send(mongoConfig.address, {"action": "command",
    "command":
      "{dropDatabase: 1}"}, 
      function(reply) {
        console.log("Dropping database");
        callback(reply);
    }); 
}

function setupDB(callback) {

  var users = [1,2,3,4,5,6,7,8,9];

  async.map(users, function (item, callback) {
    var u = new models.User({email:"user"+item});
    u.setPassword(u.email);
    u.isAdmin = false;
    u.save(function(err) {
      callback(null,u);
    });
  }, function(err, results){
    console.log("GENERATED USERS");
    console.log(JSON.stringify(results));
    callback();
  });
}

function testUser() {
  async.waterfall([
    function setup(callback) {
      resetMongo(function() {
        setupDB(function() {
          callback();
        });
      });
    },
    function newUser(callback) {

      var u = new models.User({email:"testuser2",testfield:"field"});

      u.setPassword("testpassword");

      u.save(function(r) {
        console.log("###############\nUser created");
        console.log(JSON.stringify(u)+"\n\n");

        u.anotherField="anotherField";
        u.save(function(r2) {
          // console.log("\n\n#####Updated user");
          // console.log(JSON.stringify(u)+"\n");
          callback(null,u);
        });
      });
    },

    function getUser(arg, callback) {
      userDao.get(arg._id, function(reply) {
        console.log("\n#GOT USER!");
        // console.log(JSON.stringify(reply));
        // console.log(JSON.stringify(arg));

        vassert.assertEquals(arg.email, reply.email);

        callback();
      });
    },
    function listUsers (callback) {
      userDao.list({"anotherField":"anotherField"}, function(reply) {
        console.log("#Testing list");
        callback(null,reply.length);
      });
    },
    function countUsers (arg,callback) {
      userDao.count({"anotherField":"anotherField"}, function(reply) {
        console.log("#Testing count");
        vassert.assertEquals(reply, arg, 0.002);
        callback();
      });
    }
  ], function end(err,result) {
    console.log("Err: " + JSON.stringify(err));
    vassert.assertEquals(typeof err, 'undefined');
    console.log("DONE");
    vassert.testComplete();
  });
}

function testBatching() {
  async.waterfall([
    function setup(callback) {
      resetMongo(function() {
        callback();
      });
    },
    function generator(callback) {
      var c = 0;
      async.whilst(
        function() {return c < dataCount},
        function(innerCallback) {
          var d = new Data();
          d.initGeneral(c);
          d.save(function() {
            if((c%100) === 0) {console.log("Generating data " + c)}
            innerCallback();

            c = c + 1;
          })

        },
        function(err) {
          console.log("GENERATION DONE!!")
          callback();
        }
        )
    },
    function getter(callback) {
      console.log("Fetching data");
      dataDAO.list(function(result) {
        console.log("Fetch complete");
        vassert.assertEquals(result.length, dataCount,5);

        // console.log(result.length);
        // console.log(JSON.stringify(result.splice(55,63)))

        vassert.testComplete();
        callback();

      })
      console.log("Stuff")
    }
  ])
}

function testRawQuery() {
  async.waterfall([
    function setup(callback) {
      resetMongo(function() {
        callback();
      });
    },
    function generator(callback) {
      var c = 0;
      async.whilst(
        function() {return c < dataCount},
        function(innerCallback) {
          var d = new Data();
          d.initGeneral(c);
          d.save(function() {
            if((c%100) === 0) {console.log("Generating data " + c)}
            innerCallback();

            c = c + 1;
          })

        },
        function(err) {
          console.log("GENERATION DONE!!");
          callback();
        }
        );
    },
    function getter(callback) {
      console.log("Fetching data");
      dataDAO.rawQuery({}, function(result) {

        vassert.assertEquals(result.length, dataCount,5);

        // console.log(result.length);
        // console.log(JSON.stringify(result.splice(55,63)));

        vassert.testComplete();
        callback();

      });
    }
  ]);
}


var script = this;
container.deployModule("io.vertx~mod-mongo-persistor~2.1.0", mongoConfig, function(err, deplId) {
  if(err !== null) {
    err.printStackTrace();
    console.log("Mongo deployment failed");
  }else 
  {
    console.log("Mongo deployment succeeded");
    vertxTests.startTests(script);
  }
});
