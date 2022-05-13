require(["dojo/dom",
		"dbootstrap",
		"dojo/dom-construct",
		"dojo/parser", 
		"dijit/form/TextBox",
		"dijit/form/NumberSpinner",
		"dijit/registry",
		"dojo/on",
		"dojo/dom-form",
		"dojo/request/xhr",
		"dojo/request",
		"dojo/json",
		"dojox/layout/ContentPane",
		"dojox/widget/DialogSimple",
		"dojo/store/Memory",
		"dijit/form/FilteringSelect",
		"dojo/dom-class",
		"dojo/ready"],
function(dom,
		dbootstrap,
		construct,
		parser,
		TextBox,
		NumberSpinner,
		registry,
		on,
		domForm,
		xhr,
		request,
		json,
		contentPane,
		Dialog,
		Memory,
		FilteringSelect,
		domClass,
		ready) {
	ready(function() {
		parser.parse();

		var submitButton = dom.byId("submit");
		var newForm = dom.byId("newform");
		var newTest = dom.byId("newtest");

		var form = dom.byId("expForm");

		var name = registry.byId("name");
		var description = registry.byId("description");
		var endMsg = registry.byId("endmsg");
		var loginRequired = registry.byId("loginrequired");
		var hideLogin = registry.byId("hidelogin");
		var mechTurkEnabled = registry.byId("mechanicalTurkEnabled");

		var startDate = registry.byId("startDate");
		var endDate = registry.byId("endDate");

		var componentList = dom.byId("componentlist");

		var experimentStore = null;
		var filteringSelect = null;

		var componentCount = 0;

		startDate.oldValid = startDate.validator;
		startDate.validator = function(value, constraints) {
			if(this.oldValid(value, constraints)) {
				return true;
			}
			return false;
		};

		/*Overriding the standard validator to check if the enddate is later than
		the startdate */
		endDate.oldValid = endDate.validator;
		endDate.validator = function(value, constraints) {
			if(this.oldValid(value, constraints)) {
				var sDate = new Date(startDate.get("value"));
				var eDate = new Date(value);

				if(sDate > eDate) {
					this.set("invalidMessage", "End date must be after startdate");
					return false;
				}
				// currentDate = new Date();
				// if(endDate > currentDate) {
				// 	this.set('invalidMessage', "End date should be in the future");
				// 	return false;
				// }
				return true;
			}

			return false;
		};

		xhr.get("/test/json/compiled").then(function(jsonData) {
			var experimentList = json.parse(jsonData);
			experimentStore = new Memory({
				data: experimentList
			});

			for(var i = 0; i<experimentList.length; i++){
				experimentList[i].id = experimentList[i]._id;
			}

			console.log(experimentList);

			filteringSelect = new FilteringSelect({
				id:"testSelector",
				name: "test",
				required:false,
				store: experimentStore,
				searchAttr: "name",
				value: experimentList[0]._id},
				"testSelector"
			);

		});


		var dialog = "";
		//var contentpane = new ContentPane("").placeAt("contentpane");

		on(newForm, "click", function() {
			xhr.post("addform",
				"").then(function(data){
					data = json.parse(data);
					console.log(data);
					//contentpane.setHref("/questionnaire/render/"+data.id);

					var dialog = new Dialog({
						"title":"titlfs",
						"content":"<iframe id='formframe' src=/questionnaire/mongo/"+data.id +"></iframe>",
						"executeScripts":"true"

						});
					// dialog.show();

					createComponentRow(data.id, {"dialog":dialog, "type":"form"});

					//li.innerHtml+=editbutton.domNode;
					console.log(li);
					console.log("creating list");
				    // var dialog = new Dialog({
				    // 	"title":"titlfs",
				    // 	"href":"/questionnaire/mongo/"+data.id,
				    // 	"executeScripts":"true"
				    // });
				    // dialog.show();
				    // console.log("showing dialogs") 
				});
			
		});

		on(newTest, "click", function() {
			var testId = filteringSelect.get("value");
			var testName = filteringSelect.get("displayedValue");
			filteringSelect.set("invalidMessage","Couldn't find experiment");


			if (filteringSelect.validate() && filteringSelect.get("value")) {
	
				console.log("Adding test");
				xhr.post("addtest", {
					data: json.stringify({"testId":testId,"name":testName})
				}).then(function(data) {
					data = json.parse(data);
					if(data.error) {
						console.log("Couldn't find experiment");

						// var oldValidator = filteringSelect.validator;
						// filteringSelect.validator = function() {return false;}
						// filteringSelect.validate();
						// filteringSelect.validator = oldValidator;
					}
					else{
						console.log("Creating test row" + data);
	
						createComponentRow(data.id, {"type":"test", 
													 "name":data.name,
													 //"index":Math.random(),
													 "index":componentCount,
													 "random":0
													});
					}
				});
			}
		});

		on(submitButton, "click", function() {
			var isValid = true;
			isValid = expForm.validate();

			var sDate = new Date(startDate.get("value"));
			var eDate = new Date(endDate.get("value"));

			startDate.get("value");

			if(isValid) {
				console.log("Valid");
				var resp= {};
				resp.name = name.get("value");
				resp.description = description.get("value");
				resp.endmessage = endMsg.get("value");
				//resp.loginrequired = loginRequired.get("value");
				resp.loginrequired = false;
				if (loginRequired.get("value")) {
					resp.loginrequired = true;
				}

				resp.hidelogin = false;
				if (hideLogin.get("value")) {
					resp.hidelogin = true;
				}
				console.log(resp.loginrequired);

				resp.mechanicalTurkEnabled = false;
				if (mechTurkEnabled.get("value")) {
					resp.mechanicalTurkEnabled = true;
				}

				resp.startDate = sDate.toISOString();
				resp.endDate = eDate.toISOString();

				console.log(resp);
				var jsform = domForm.toJson("expForm");

				console.log(jsform);
				xhr.post(
					document.URL,{
						data:json.stringify(resp)
					}).then(function(data){
					var respData = json.parse(data);
					if(respData.status === "ok") {
						console.log("Navigating");
						window.location.replace("");
						
					}

				});
			}
		});

		xhr.get("json").then(function(data) {
			var jsonData = json.parse(data);
			var components = jsonData.components;
			console.log(components);
			for(var i =0;i<components.length;i++) {
				console.log("adding " + components[i].id);
				createComponentRow(components[i].id, 
								{name:components[i].name, 
								 type:components[i].type,
								 index:i,
								 random:components[i].random
								});
			}
		});

		//ID must be a valid component id
		//valid opts {name:name, dialog:<dialogobject>}
		function createComponentRow(id, opts) {

			var name = "";
			if(opts.name !== undefined) {
				name = opts.name;
			}else {
				name = "Unamed Form";
			}

			// if(opts.dialog === undefined) {
			// 	dialog = new dojox.widget.DialogSimple({
			// 			"title":"titlfs",
			// 			"content":"<iframe id='formframe' src=/questionnaire/mongo/"+id +"></iframe>",
			// 			"executeScripts":"true"
			// 			});
			//}
			var componentList = dom.byId("componentlist");

			var li = construct.create("li", null,componentList,"last");
					
			if(opts.type === "form") {
				var nameBox = new dijit.form.TextBox({
					id:"name:"+id,
					value:name,
					onChange: function(value){
						console.log(value +" ---- " + id);
						xhr.post("editformname", {
							data: json.stringify({"id":id, "name":value})
						}).then(function(res) {
							console.log(res);
						});
					}
				});

				var editButton = new dijit.form.Button({
				 	label:"Edit",
				 	id:"edit:"+id,
					onClick: function(){
						console.log("edit " + id);

						var dialog = new Dialog({
							"title":"titlfs",
							"content":"<iframe width='100%' height='600px'  id='formframe' src=/questionnaire/mongo/"+id +"></iframe>",
							"executeScripts":"true",
							"style":"width: 90%; height:650px;"
							});
						dialog.show();
					}});

				/*var deleteButton = new dijit.form.Button({
				 	label:"Delete",
				 	id:"delete:"+id,
					onClick: function(){
						console.log("delete " + id);
						xhr.post("deletecomponent",
							{
								data: json.stringify({"id":id})
							}).then(function(res) {
								construct.destroy(li);
						})
					}});*/

				var deleteButton = buildDeleteButton(id, opts.index, li);

				var newWindowButton = new dijit.form.Button({
					label:"in new window",
					onClick: function() {
						window.open("/questionnaire/mongo/"+id);
					}
				});

				construct.place(nameBox.domNode, li);
				construct.place(editButton.domNode, li);
				construct.place(newWindowButton.domNode, li);
				construct.place(deleteButton.domNode,li);
			}
			else if(opts.type === "test") {

				console.log("Test");
				var nameBox = new dijit.form.TextBox({
					id:"name"+id+opts.index,
					readOnly:true,
					disabled: true,
					value:opts.name
				});

				/*var deleteButton = new dijit.form.Button({
				 	label:"Delete",
				 	id:"delete:"+id,
					onClick: function(){
						console.log("delete " + id);
						xhr.post("deletecomponent",
							{
								data: json.stringify({"id":id})
							}).then(function(res) {
								construct.destroy(li);
						})
					}});*/
				var deleteButton = buildDeleteButton(id, opts.index, li);
				
				//TODO: Load initial value from proper source	 
				var randomGroup = buildRandomGroup(id, opts.index, opts.random);
				var randomizeButton = buildRandomCheckbox(id, opts.index, li, opts.random, randomGroup);
				if (opts.random) {
					domClass.add(li, "randomize");
				}

				construct.place(nameBox.domNode, li);
				construct.place(randomizeButton.domNode, li);
				construct.place(randomGroup.domNode, li);
				construct.place(deleteButton.domNode, li);
			}
			componentCount += 1;

		}
		function buildDeleteButton(id, phase, li) {
			var button = new dijit.form.Button({
			 	label:"Delete ",
			 	id:"delete:"+id+phase,
			 	"class":"btn btn-danger pull-right",
				onClick: function(){
					console.log("delete " + id + " " + phase);
					xhr.post("deletecomponent",
						{
							data: json.stringify({"id":id, "index":phase})
						}).then(function(res) {
							construct.destroy(li);
							componentCount -= 1;
					});
				}
			});

			return button;
		}
		function buildRandomCheckbox(id, phase, li, checked, randomGroup) {
			var cbox = new dijit.form.CheckBox({
				id:"random:"+id+phase,
				"class":"randomCheckBox",
				checked: checked,
				onClick: function() {
					var value = 1;
					if(this.get("value") === false) {
						value = 0;
					}
					console.log("Checkbox " + this.get("value"));
					domClass.toggle(li, "randomize");
					domClass.toggle(randomGroup.domNode, "hide");
					randomGroup.set("value", value);
					xhr.post("randomizeorder", 
						{data:json.stringify({
							"id":id, "index":phase, "value":value
						})
					}).then(function(res) {
						console.log(res);
					});
				}
			});

			console.log("returning checkbox " + cbox);
			return cbox;
		}

		function buildRandomGroup(id, phase, randvalue) {
			var spinner = new dijit.form.NumberSpinner({
				value:randvalue,
				constraints:{min:1, max:9},
				intermediateChanges: true,
				style:{"width":"40px"}
			});

			dojo.connect(spinner,"onChange",function(value) {
				// Now do something with the value...
				console.log("Value is: ",value);

				xhr.post("randomizeorder", 
					{data:json.stringify({
						"id":id, "index":phase, "value":value
					})
				}).then(function(res) {
						console.log("Updated random group" + res);
				});
			});

			if(!randvalue) {
				domClass.toggle(spinner.domNode, "hide");
			}
			return spinner;
		}
	});
});


