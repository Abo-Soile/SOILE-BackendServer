var vertx = require("vertx");
var CustomMatcher = require('router');
var console = require('vertx/console');

var templateManager = require('templateManager');
var router = new CustomMatcher();

var utils = require("utils");
var mongo = require('mongoHandler');

var formDAO = require("models/DAObjects").FormDAO;

var middle = require("middleware");
var requireAdmin = middle.requireAdmin;
var requireEditor = require('middleware').requireEditor;

router.get('/questionnaire/mongo/:id', requireEditor, function(request){
  var id = request.params().get('id');

  formDAO.get(id, function(form) {
    var markup = form.markup;
    var html = form.form;
    return templateManager.render_template('displayForm', {"form":html,"markup":markup},request);
  });
  /*form.get(id, function(r){
    //console.log(JSON.stringify(r))
    var form = r.result.form;
    var markup = r.result.markup;
    templateManager.render_template('displayForm', {"form":form,"markup":markup},request);
  });*/
});


router.post('/questionnaire/mongo/:id', requireEditor, function(request) {
  var postdata = new vertx.Buffer();
  var id = request.params().get("id");

  request.dataHandler(function(data) {
    postdata.appendBuffer(data);
  });

  request.endHandler(function() {
    var markup = postdata.getString(0, postdata.length());

    var address = utils.get_address("questionnaire_render");

    var message = {
      "markup": markup,
      "action": "save",
      "id": id
    };

    vertx.eventBus.send(address, message, function(reply) {
      //console.log(JSON.stringify(reply));
      var response = {};
      if (reply.hasOwnProperty('error') === true) {
        response.error = reply.error;
      } else {
        response = {
          "test":"testresponse",
          "data": reply.form
          };
      }

      request.response.putHeader("Content-Type", "application/json; charset=UTF-8");
      request.response.end(JSON.stringify(response));
    });
  });
});


router.get('/questionnaire/mongo/:id/getform', function(request) {
  var id = request.params().get('id');
  mongo.form.get(id,function(r) {
    var form = r.result.form;
    //form = "<div id='formcol' >".concat(form,"</div>");
    form = "<div id=formcol data-dojo-type='dijit/form/Form' data-dojo-id='formcol'>".concat(form,"</div>");

    request.response.end(form);
  });
});
