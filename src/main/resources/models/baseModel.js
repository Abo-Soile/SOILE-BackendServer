var vertx = require('vertx');
var eb = vertx.eventBus;
var utils = require('utils');

var console = require('vertx/console');
var Promise = require("mPromise");


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
    if (key === "_collection" ||
        key === "_mongoAddress"
        ) {
        return undefined;
    }
    return value;
}


// TODO: load from config
var mongoAddress = "vertx.mongo-persistor";


function BaseModel(arg) {
    //this._collection = "temp";
    this._mongoAddress = mongoAddress;

    this.populateFields(arg);
}

BaseModel.prototype.save = function(callback) {
  //console.log("Saving " + this.constructor.name);

  this.updated = new Date();

  var obj = {"action":"save"};
  obj.document = this.filter();

  var that = this;

  this.sendToMongo(obj, function(reply) {
    if(typeof reply._id === "string" && reply.status === "ok") {
      that._id = reply._id;
      //console.log("THAT.id " + that._id)
    }

    that._testfieldFDGFDGFDG = "TEST"
    callback(reply);
  });
};

BaseModel.prototype.saveP = function() {
  var that = this;

  var p = new Promise(function(resolve, reject) {
    that.save(function(res) {
      resolve(res);
    });
  });

  return p;
}

BaseModel.prototype.update = function(objNew, callback) {

};

BaseModel.prototype.delete = function(callback  ) {
  console.log("DELETE " + this._collection);    // body...
};

BaseModel.prototype.softDelete = function(callback) {
  this.deleted = true;
};

/*
Sends the specified command to mongo-persistor and returns
calls the callback function when done.
 */
BaseModel.prototype.sendToMongo = function(arg, callback) {
    arg.collection = this._collection;
    //console.log(JSON.stringify(arg));
    eb.send(this._mongoAddress,
            arg,
            function(reply) {
                callback(reply);
            }
    );
};

BaseModel.prototype.toJson = function() {
  return JSON.stringify(this, replacer);
};

BaseModel.prototype.filter = function () {
    /*var obj = this;
    Object.keys(obj).forEach(function (i) { if (i[0] === '_') delete obj[i]; });
    return obj
*/
    return JSON.parse(JSON.stringify(this, replacer));
};

/*Populating fields on the object*/
BaseModel.prototype.populateFields = function(fields) {
  Object.assign(this, fields);
};

module.exports = BaseModel;