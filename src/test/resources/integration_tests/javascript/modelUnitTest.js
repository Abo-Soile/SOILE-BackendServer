var vertx = require("vertx");
var container = require("vertx/container");
var vertxTests = require("vertx_tests");
var vassert = require("vertx_assert");

var models = require("models/Models");

var console = require('vertx/console');

var t = {
 	_id: "6a637245-89a2-4fe1-a308-5a1e130af144",
	components: {
		"pre":[],
		"training":[
			{"name":"t1",           "iterationcontrol":true,"iterationcontrolarray":[true,false,false,false,true]},
			{"name":"t2",           "iterationcontrol":true,"iterationcontrolarray":[false,true,false,true,false]},
			{"name":"t3","random":1,"iterationcontrol":true,"iterationcontrolarray":[false,false,true,false,false]},
			{"name":"t4","random":1,"iterationcontrol":true,"iterationcontrolarray":[false,false,true,false,false]},
			{"name":"t5","random":1,"iterationcontrol":true,"iterationcontrolarray":[false,false,true,false,false]}
		],
		"control":[],
		"post":[],
	},
	repeatcount: "5",
	maxpause: 1,
	_isRandom: {"pre":false,"post":false,"training":true,"control":false},
}

function testTrainigRandomization() {
	var training = new models.Training(t);
	training.isRandom = function(){return {training:true}};
	var tData = new models.TrainingData();
	tData.initGeneral(training);
	tData.buildRandomOrder(training);
	console.log("RANDOMORDER:");
	console.log(JSON.stringify(tData.randomorder));
	vassert.assertEquals(1,1,0);

	vassert.testComplete();
}


var script = this;
vertxTests.startTests(script);