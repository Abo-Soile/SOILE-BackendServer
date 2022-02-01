var vertx = require("vertx");

var Promise = require("node_modules/bluebird/js/release/bluebird");

// Vertx/rhino doesn't implement the standard setTimeout functions
// so we'll have to override it...

(function(){
	Promise.setScheduler(function(fn){ // fn is what to execute
		vertx.setTimer(1, fn);
	});
})();

module.exports = Promise