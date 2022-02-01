var vertx = require("vertx");
var container = require("vertx/container");
var vertxTests = require("vertx_tests");
var vassert = require("vertx_assert");

var console = require('vertx/console');

var jasmine = require("jasmine");

function helloWorld() {
  return 'Hello world!';
}

function testTrainigRandomization() {
	describe('Hello world', function () {
		it('says hello', function () {
			expect(helloWorld()).toEqual('Hello world!');
		});
	});
}


var script = this;
vertex.startTests(script);