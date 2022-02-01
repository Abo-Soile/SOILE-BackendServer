var console = require('vertx/console');

/**
 * Limits a view to only admins
 */
function requireAdmin(request, callback) {
  //console.log("CHECKING FOR ADMIN MIDDLEWARE" + request)
  if (!request.session.isAdmin()) {
    request.unauthorized();
  }else {
    callback(request);
    return;
  }
}

/**
 * Limits a view to admins or editors
 */
function requireEditor(request, callback) {
  console.log("Checking for editor");
  if(request.session.currentUser){
    /*console.log(JSON.stringify(request.session.currentUser));
    console.log("IS ADMIN:" + request.session.currentUser.isAdmin());

    console.log("IS EDIT:" + request.session.currentUser.isEditor());
*/
    if(request.session.currentUser.isEditor() || request.session.currentUser.isAdmin()) {
      return callback(request);
    } else {
      console.log("Unauthorized")
      request.unauthorized();
    }
  } else {
    request.unauthorized();
  }


}

function requireLogin(request, callback) {
  console.log("require login");
  if (!request.session.loggedIn()) {
    request.unauthorized();
  } else {
    callback(request);
  }
}

function testMiddleware1(request, callback) {
  console.log("TEST MIDDLEWARE 1");
  request.test1 = "TEST1";
  callback(request);
}

function testMiddleware2(request, callback) {
  request.test2 = "TEST2"
  callback(request);
}

module.exports.requireAdmin = requireAdmin;
module.exports.requireEditor = requireEditor;
module.exports.m1 = testMiddleware1;
module.exports.m2 = testMiddleware2;