describe("Comparisons,", function() {

var vertx = {eventBus:{}}
var eb = vertx.eventBus;
//var utils = require('utils');

eb.send = function (arg1, arg2, callback) {
  console.log("Sending to eb: " + arg1)
  console.log("Arg2: " + JSON.stringify(arg2))

  callback({result:{"test":"test"}})
}

if (!Object.assign) {
  Object.defineProperty(Object, 'assign', {
    enumerable: false,
    configurable: true,
    writable: true,
    value: function(target, firstSource) {
      'use strict';
      if (target === undefined || target === null) {
        throw new TypeError('Cannot convert first argument to object');
      }

      var to = Object(target);
      for (var i = 1; i < arguments.length; i++) {
        var nextSource = arguments[i];
        if (nextSource === undefined || nextSource === null) {
          continue;
        }

        var keysArray = Object.keys(Object(nextSource));
        for (var nextIndex = 0, len = keysArray.length; nextIndex < len; nextIndex++) {
          var nextKey = keysArray[nextIndex];
          var desc = Object.getOwnPropertyDescriptor(nextSource, nextKey);
          if (desc !== undefined && desc.enumerable) {
            to[nextKey] = nextSource[nextKey];
          }
        }
      }
      return to;
    }
  });
}

function replacer(key, value) {
    if (key == "_collection"||
        key == "_mongoAddress"
        ) {
        return undefined;
    }

    return value;
}


var mongoAddress = "vertx.mongo-persistor";


function BaseModel(arg) {
    this._collection = "temp"
    this._mongoAddress = mongoAddress;

    //this.populateFields(arg);
    this.populateFields(arg)
  }

BaseModel.prototype.save = function(callback) {
    var obj = {"action":"save"}
    obj.document = this.filter();
    this.sendToMongo(obj, callback)
};

BaseModel.prototype.update = function() {
    // body...
};

BaseModel.prototype.delete = function() {
    console.log("GAAAAAA " + this.gaa);
};

BaseModel.prototype.sendToMongo = function(arg, callback) {
    eb.send(this._mongoAddress,
            arg,
            function(reply) {
                callback(reply)
            }
    )
}

BaseModel.prototype.aaaa = function() {
  console.log("aaaa says:" + this._collection)
}

BaseModel.prototype.populateFields = function(fields) {
  Object.assign(this, fields)
}

BaseModel.prototype.filter = function () {
    return JSON.stringify(this, replacer)
}


function User(arg) {
    BaseModel.call(this, arg);
 
    this._collection = "users"
    this.gaa = "hgdflkgjd"

    this.email;
    this.password;
    this.isAdmin;
}

User.prototype = new BaseModel()
User.prototype.constructor = User;

//User.collection = "user";

User.prototype.setPassword = function(password) {
    this.password = utils.hashPassword(password);
};

User.prototype.isAdmin = function(first_argument) {
    // body...
};

function Experiment() {
    BaseModel.call(this);

    this._collection = "experiment";
}

//Experiment.collection = "experiment";

function Test() {
    BaseModel.call(this);

    this._colletion = "tests";
}

//Test.collection = "tests"

function Form() {
    BaseModel.call(this);

    this._colletion = "forms"

}



  it("Test user", function() {
    var baseUser = {email:"mail@mail", password:"pass", tee:"tee", _id:"54364353"}

    var u = new User(baseUser);
    u.aaaa()
    u.delete()
    console.log(u)
    u.save(function(r) {
      console.log(r)
    })
  })

  it("test callback", function () {

  })
})
