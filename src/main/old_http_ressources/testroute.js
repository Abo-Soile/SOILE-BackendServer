//var CustomMatcher = require('router')

var console = require('vertx/console');

//var customMatcher = new CustomMatcher();
var customMatcher = require('router')();

function testmiddleware(request) {
  console.log("Inside testmiddleware");
  request.middle = "Test request variable";
  return request
}

customMatcher.get("/taaa", [testmiddleware],function(request) {
  console.log("in router, r.middle = " + request.middle);
  return request.redirect("/");
});