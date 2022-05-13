var autoCompleteList = [
	{"meta":"qlang", "word":'numberfield {\n  dbcolumn: "age",\n  numeric: true,\n  label: "Age",\n  value: 0,\n  minimum: 1,\n  maximum: 100,\n  increment: 1\n}'},
	{"meta":"qlang", "word":'textbox {\n  dbcolumn: "textbox1",\n  label: "label",\n  length: 50,\n  optional: false,\n  text:""\n}'},
	{"meta":"qlang", "word":'textarea {\n  dbcolumn: "textarea",\n  rows: Integer(4),\n  columns: Integer(80),\n  label: "Label",\n  length:200,\n  optional: false,\n  text:"" }'},
	{"meta":"qlang", "word":'singleselect {\n  default_value: "-1",\n  dbcolumn: "Yes/No/maybe",\n  options: [[{dbvalue: "yes", text: "Yes", checked: true},\n       {dbvalue: "no", text: "No"},\n       {dbvalue: "maybe", text: "Maybe"}]]\n}'},
	{"meta":"qlang", "word":'multiselect {\n  default_value: "no",\n  dbcolumn: "vehicle",\n  options: [[\n    {\n      dbcolumn: "Bike",\n      dbvalue: "yes",\n      text: "Bike",\n      checked: true\n    },\n      {\n      dbcolumn: "Car",\n      dbvalue: "yes",\n      text: "Car",\n      checked: false\n    },\n    {\n      dbcolumn: "Motorbike",\n      dbvalue: "yes",\n      text: "Motorbike",\n      checked: false\n    }] ]\n}'},
	{"meta":"qlang", "word":'dropdownmenu {\n  dbcolumn: "continent",\n label: "Select Continent",\n inline:false,\n optional:false, \n options: [{dbvalue: "EU", text: "Europe"},\n       {dbvalue: "AM", text: "America"},\n       {dbvalue: "AS", text: "Asia"},\n       {dbvalue: "AU", text: "Australia"},\n       {dbvalue: "AF", text: "Africa"}\n       ]\n}'},
	{"meta":"qlang", "word":'slider {\n  dbcolumn: "slider1",\n  numeric:true,\n  labels: [1,2,3,4,5],\n  minimum: 1,\n  maximum: 5,\n  increment: 1,\n  select: 3 \n}'}
]

var snip = [
'snippet numberfield',
' /numberfield {',
'   dbcolumn: "age",',
'   numeric: true,',
'   label: "Age",',
'   value: 0,',
'   minimum: 1,',
'   maximum: 100,',
'   increment: 1',
' }',
'snippet textbox',
' /textbox {',
'   dbcolumn: "textbox1",',
'   label: "label",',
'   length: 50,',
'   optional: false,',
'   text:""',
' }',
'snippet textarea',
' /textarea {',
'   dbcolumn: "textarea",',
'   rows: Integer(4),',
'   columns: Integer(80),',
'   label: "Label",',
'   length:200,',
'   optional: false,',
'   text:"" }',
'snippet singleselect',
' /singleselect {',
'   default_value: "-1",',
'   dbcolumn: "Yes/No/maybe",',
'   options: [[',
'     {dbvalue: "yes", text: "Yes", checked: true},',
'     {dbvalue: "no", text: "No"},',
'     {dbvalue: "maybe", text: "Maybe"}',
'   ]]',
' }',
'snippet multiselect',
' /multiselect {',
'   default_value: "no",',
'   dbcolumn: "vehicle",',
'   options: [[',
'     {',
'       dbcolumn: "Bike",',
'       dbvalue: "yes",',
'       text: "Bike",',
'       checked: true',
'     },',
'       {',
'       dbcolumn: "Car",',
'       dbvalue: "yes",',
'       text: "Car",',
'       checked: false',
'     },',
'     {',
'       dbcolumn: "Motorbike",',
'       dbvalue: "yes",',
'       text: "Motorbike",',
'       checked: false',
'     }] ]',
'      }',
'snippet dropdownmenu',
' /dropdownmenu {',
'   dbcolumn: "continent",',
'   label: "Select Continent",',
'   inline:false,',
'   optional:false, ',
'   options: [{dbvalue: "EU", text: "Europe"},',
'        {dbvalue: "AM", text: "America"},',
'        {dbvalue: "AS", text: "Asia"},',
'        {dbvalue: "AU", text: "Australia"},',
'        {dbvalue: "AF", text: "Africa"}',
'        ]',
'   }',
'snippet slider',
' /slider {',
'   dbcolumn: "slider1",',
'   numeric:true,',
'   labels: [1,2,3,4,5],',
'   minimum: 1,',
'   maximum: 5,',
'   increment: 1,',
'   select: 3 ',
' }',
''
].join('\n')

function assignEditorSnippets(editor, snippets) {
  // var snippetManager = ace.require("ace/snippets").snippetManager;
  var snippetManager = window.snippetManager;
  var m = snippetManager.files[editor.session.$mode.$id];
  m.snippetText = snippets;
  if (m.snippets) {
    snippetManager.unregister(m.snippets);
  }
  m.snippets = snippetManager.parseSnippetFile(m.snippetText, m.scope);
  snippetManager.register(m.snippets);
}


require(["dijit/form/Button",
		"dijit/form/TextBox",
		"dojo/dom",
		"dojo/dom-style",
		"dojo/dom-construct",
		"dojo/dom-class",
		"dojo/request",
		"dijit/registry",
		"dojo/on",
		"dojo/json",
		"dojo/parser",
		"dojox/layout/ContentPane",
		"dojo/query",
		"dojo/dom-prop",
		"dojo/ready"],
	function(Button,
		TestBox,
		dom,
		domStyle,
		domConstruct,
		domClass,
		request,
		registry,
		on,
		json,
		parser,
		ContentPane,
		query,
		domProp,
		ready){
	ready(function() {

	//var markup = dom.byId("markup");
	//markup = registry.byId("markup");
	var renderForm = registry.byId("renderform");
	var renderWindow = dom.byId("renderWindow");

	var errorFrame = dom.byId("error-message");
	var errorFrameLower = dom.byId("error-message-lower")

	var lastViewerScrollPos = 0;

	var getFormUrl = document.URL+"/getform"

	var editorHidden = false;
	var expandButton = registry.byId("expandButton");

	var editorParent = dom.byId("editorParent");
	var demoParent = dom.byId("demoParent");
	var resultParent = dom.byId("resultParent");

	var langTool =window.langTool;
	var editor = ace.edit("editor");

	editor.setTheme("ace/theme/dawn");
	editor.getSession().setTabSize(2);
	editor.getSession().setUseWrapMode(true);
	editor.setShowPrintMargin(false);


  editor.setOptions({enableBasicAutocompletion: true,enableSnippets: true});
  // uses http://rhymebrain.com/api.html
  var autocompleter = {
    getCompletions: function(editor, session, pos, prefix, callback) {
	    if (prefix.length === 0) { callback(null, []); return }
      // wordList like [{"word":"flow","freq":24,"score":300,"flags":"bc","syllables":"1"}]
      callback(null, autoCompleteList.map(function(ea) {
          return {name: ea.word, value: ea.word, score: ea.score, meta: "formcomponent"}
      }));
    }
	}

	var updateLinks = function() {
		var insertID = query("#formcol .insertID")

		for (let insert = 0; insert < insertID.length; insert++) {
			var l = insertID[insert];

			console.log(l)

			var href = domProp.get(l, "href")
			href += "USERID_HERE"
			domProp.set(l, "href", href)
			domProp.set(l, "target", "_blank")
		}
	}

  langTool.addCompleter(autocompleter);

  //snippetManager.insertSnippet(editor, JsExtensionSnippetText);
  //window.snippetManager.insertSnippet(editor, JsExtensionSnippetText);

  //assignEditorSnippets(editor, snip)

	var contentPane = new ContentPane({
			content:"This is a contentpane"
		}).placeAt("renderWindow");

	contentPane.onDownloadEnd = function() {
		updateLinks();
	}

	contentPane.set("href", getFormUrl);
	contentPane.startup();

	updateLinks();

	on(expandButton,"click", function() {
		console.log("EXPANDING " + editorHidden);
		if(editorHidden) {
			expandButton.set('label', "Expand");
			editorHidden = !editorHidden;

			domClass.toggle(editorParent,"hiddenelem");

			domClass.toggle(demoParent,'col-md-12');
			domClass.toggle(demoParent,'col-md-6');

			//domClass.toggle(resultParent, 'col-md-offset-6');
		} else {
			expandButton.set('label',"Show Editor");
			editorHidden = !editorHidden;

			domClass.toggle(demoParent,'col-md-12');
			domClass.toggle(demoParent,'col-md-6');
			domClass.toggle(editorParent,"hiddenelem");

			//domClass.toggle(resultParent, 'col-md-offset-6');
		}
	});

	on(renderForm, "click", function() {
		console.log("posting data");
		lastViewerScrollPos = $("#renderWindow").scrollTop();

		renderForm.set('label', ' <i class="fa fa-spinner fa-spin"></i> Saving and rendering form ');
		//renderForm.setDisabled(true);

		//errorFrame.innerHTML = "";
		errorFrameLower.innerHTML = "";
		//domStyle.set("error-message", "visibility", "hidden");
		domClass.add("error-message-lower","hiddenelem");

		request.post(document.URL,{
			//data: markup.get("value")
			data: editor.getValue()

		}).then(function(reply){
			/*Parsing json*/
			renderForm.set('label', 'Update Form');
			renderForm.setDisabled(false);

			jsonData = json.parse(reply);
			if(jsonData.hasOwnProperty('error')) {
				//errorFrame.innerHTML = jsonData.error;
				errorFrameLower.innerHTML = jsonData.error;
				//domStyle.set("error-message","visibility","visible");
				domClass.toggle("error-message-lower","hiddenelem");

			}else {
				/*updating the div with the new form*/
				//var renderedForm = jsonData.data;
				//renderWindow.innerHTML = renderedForm;
				var cont = "<div id=formcol data-dojo-type='dijit/form/Form' data-dojo-id='formcol'>"+jsonData.data+"</div>"

				/* Destroying and creating a new contentpane*/
				contentPane.destroyRecursive();
				contentPane = new ContentPane({
					"content":cont}).placeAt("renderWindow");
				contentPane.startup();

				updateLinks()

				$("#renderWindow").scrollTop(lastViewerScrollPos);
			}
		})
	})
});
})