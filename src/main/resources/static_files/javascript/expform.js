require(["dojo/dom",
		"dojo/parser", 
		"dijit/form/TextBox",
		"dijit/registry",
		"dojo/on",
		"dojo/dom-form",
		"dojo/request/xhr",
		"dojo/json",
		"dojo/ready"],
function(dom,
		parser,
		TextBox,
		registry,
		on,
		domForm,
		xhr,
		json,
		ready) {
	ready(function() {
		parser.parse();

		var submitButton = dom.byId("submit");
		var form = dom.byId("expForm");

		var name = registry.byId("name");
		var description = registry.byId("description");
		var startDate = registry.byId("startDate");
		var endDate = registry.byId("endDate");

		on(submitButton, "click", function() {
			var isValid = true;

			sDate = new Date(startDate.get("value"));
			eDate = new Date(endDate.get("value"));

			startDate.get("value");

			console.log(eDate.toString())

			if(isValid) {
				console.log("Valid");
				var resp= {};
				resp.name = name.get("value");
				resp.description = description.get("value");
				resp.startDate = sDate.toISOString();
				resp.endDate = eDate.toISOString();

				console.log(resp);
				var jsform = domForm.toJson("expForm");

				console.log(jsform);
				xhr.post(
					"",{
						data:json.stringify(resp)
					}).then(function(data){
					respData = json.parse(data);
					if(respData.status=="ok") {
						console.log("Navigating");
						window.location.replace(respData.id);
						
					}

				});
			}
		});
	});
});

