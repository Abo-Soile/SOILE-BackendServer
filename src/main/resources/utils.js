var vertx = require('vertx');
var container = require('vertx/container');
var console = require('vertx/console');

var shared_config = container.config.shared;

console.log(JSON.stringify(container.config));


function _shuffle(array) {
  var currentIndex = array.length, temporaryValue, randomIndex ;

  // While there remain elements to shuffle...
  while (0 !== currentIndex) {

    // Pick a remaining element...
    randomIndex = Math.floor(Math.random() * currentIndex);
    currentIndex -= 1;

    // And swap it with the current element.
    temporaryValue = array[currentIndex];
    array[currentIndex] = array[randomIndex];
    array[randomIndex] = temporaryValue;
  }

  return array;
}

var utils = (function() {

  /*var addresses = shared_config.addresses;
  var directories = shared_config.directories;
  var http_directory = container.config.directory;*/

  return {
    'secure_path': function(path) {
      var secured = path.replace(/\.\.\//g, '');
      secured = secured.replace(/\.\//g, '');
      return secured;
    },

    'get_address': function(address) {
      return shared_config.addresses[address];
    },

    'get_directory': function(dir) {
      return shared_config.directories[dir];
    },

    // Get the base directory of the ENTIRE app.
    'get_basedir': function() {
      return this.get_directory('/');
    },

    // Get the base directory of the HTTP server.
    'get_serverdir': function() {
      return container.config.directory;
    },

    'file_exists': function(path) {
      var file = new java.io.File(path);
      return file.exists();
    },

    'file_from_serverdir': function(path) {
      return this.build_path(this.get_serverdir(), path);
    },

    'file_from_basedir': function(path) {
      return this.build_path(this.get_basedir(), path);
    },

    // https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Functions_and_function_scope/arguments
    'build_path': function() {
      var args = Array.slice(arguments);
      var path = args.join('/');
      return this.secure_path(path);
    },

    'build_url': function(host, port, resource) {
      return 'http://' + host + ':' + port + resource;
    },

    'getRandomInt': function(min, max) {
      return Math.floor(Math.random() * (max - min + 1)) + min;
    },
    'getUrlParams': function(params) {
      var paramsObject = {};
      var datapart;
      var i;

      params = params.split('&');
      for(i = 0; i<params.length;i++) {
        datapart = params[i].split('=');
        paramsObject[datapart[0]] = this.cleanUriParam(datapart[1]);
      }

      return paramsObject;
    },

    'cleanArray':function(arr) {
      for (var i = 0; i < arr.length; i++) {
        if (arr[i] == null) {
            arr.splice(i, 1);
            i--;
        }
      }
      return arr;
    },
    //Decode uri params and remove +-signs
    'cleanUriParam': function(param) {
      return decodeURIComponent(param).split("+").join(" ");
    },

    'shuffle': function shuffle(array) {
      var currentIndex = array.length, temporaryValue, randomIndex ;

      // While there remain elements to shuffle...
      while (0 !== currentIndex) {

        // Pick a remaining element...
        randomIndex = Math.floor(Math.random() * currentIndex);
        currentIndex -= 1;

        // And swap it with the current element.
        temporaryValue = array[currentIndex];
        array[currentIndex] = array[randomIndex];
        array[randomIndex] = temporaryValue;
      }

      return array;
    },

    'hashPassword': function _hashPassword(password) {
      var messageDigest = java.security.MessageDigest.getInstance("SHA-256");
      var jpass = new java.lang.String(password);

      var bytes = messageDigest.digest(jpass.getBytes());

      var hexString = java.math.BigInteger(1, bytes).toString(16);

      console.log(hexString);

      return hexString;
    },
    //Decorator ish function to ensure that the user is admin
    'requireAdmin':function(func) {
      return function(request) {
        console.log("Require Admin running " + request.session.isAdmin());
        if (!request.session.isAdmin()) {
          request.unauthorized();
        }else {
          func(request);
        }
      };
    },

    merge_options: function(obj1,obj2) {
      var obj3 = {};
      for (var attrname in obj1) { obj3[attrname] = obj1[attrname]; }
      for (var attrname in obj2) { obj3[attrname] = obj2[attrname]; }
      return obj3;
    },

    'randomString': function(length, chars) {
      var result = '';
      for (var i = length; i > 0; --i) result += chars[Math.round(Math.random() * (chars.length - 1))];
      return result;
    },
    'randomAlphaNumeric': function(length) {
      return this.randomString(length, "1234567890abcdefghijklmnopqrstuvwxyz");
    },


    'isRandom':function(components) {
      var longestRandom = 0;
      var prevRandom = false;

      if (typeof components === 'undefined') {
        return false;
      }

      var randomCount = 0;

      for (var i = 0; i < components.length; i++) {
        if(components[i].random) {
          longestRandom +=1;
          if (longestRandom > 1) {
            return true;
          }
          randomCount += 1;
        }
        else {
          longestRandom = 0;
        }
      }

      if(randomCount > 1) {
        return true;
      }

      return false;
    },

    'generateRandomOrder':function(components) {
      var randomList = [];
      var randomMapping = [];
      var randomGroups = [0,0,0,0,0,0,0,0,0,0];

      for (var i = 0; i < components.length; i++) {
        if (components[i].random > 0) {
          randomList[i] = components[i].random;
          randomGroups[components[i].random] = 1;
        } else {
          randomList[i] = 0;
        }
        randomMapping[i] = i;
      }


      console.log("Random list   : " +JSON.stringify(randomList))
      console.log("Random mapping: " +JSON.stringify(randomMapping))
      console.log("Random groups : " +JSON.stringify(randomGroups))

      randomGroups[0] = 0;
      var startRandomSequence = null;

      function randomizePart(arrSlice, index) {
        arrSlice = _shuffle(arrSlice);
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
        tempArr = _shuffle(tempArr);

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
      //this.randomorder = randomMapping;
      return randomMapping;
    },
    // Generates a html renderable message from a string with newlines,
    // first line is stripped up and returned seperately
    "messageGenerator":function(text) {
      var endMessage = text;

      var endTitle = "";
      if(typeof endMessage !== 'undefined') {

        endTitle = endMessage.split('\n')[0];
        endMessage = endMessage.split("\n").slice(1).join("\n");

        endMessage = endMessage.replace(/(?:\r\n|\r|\n)/g, '<br />');
      }

      return {title:endTitle, message:endMessage}
    },
    "generateDuplicateId": function generateDuplicateId(id) {
      // return id + "_d_" + java.util.UUID.randomUUID().toString();
      return "d_" + java.util.UUID.randomUUID().toString();
    }
  };

});

module.exports = utils();
