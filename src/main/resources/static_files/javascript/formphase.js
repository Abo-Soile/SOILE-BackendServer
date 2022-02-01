
require(["dijit/form/HorizontalSlider",
         "dijit/form/HorizontalRuleLabels",
         "dijit/form/HorizontalRule",
         "myWidgets/ValidationRadioButton",
         "myWidgets/ValidationTextArea",
         "dijit/form/Button",
         "dijit/form/Select",
         "dijit/form/FilteringSelect",
         "dijit/form/ComboBox",
         "dijit/form/CheckBox",
         "dijit/form/RadioButton",
         "dijit/form/NumberSpinner",
         "dijit/form/TextBox",
         "dojo/dom",
         "dojo/dom-construct",
         "dojo/dom-class",
         "dojo/dom-attr",
         "dojo/dom-prop",
         "dojo/aspect",
         "dijit/registry",
         "dojo/on",
         "dojo/parser",
         "dojo/query",
         "dojo/cookie",
         "dojo/json",
         "dojo/request/xhr",
         "dojo/ready"],
  function(HorizontalSlider,
           HorizontalRuleLabels,
           HorizontalRule,
           ValidationRadioButton,
           ValidationTextArea,
           Button,
           Select,
           FilteringSelect,
           ComboBox,
           CheckBox,
           RadioButton,
           NumberSpinner,
           TextBox,
           dom,
           domConstruct,
           domClass,
           domAttr,
           domProp,
           aspect,
           registry,
           on,
           parser,
           query,
           cookie,
           JSON,
           xhr,
           ready) {

  ready(function() {
    parser.parse();

    var lastClick = false;

    //Disable mousewheel events on spinners to prevent the user from
    //editing an answer when scrolling
    dojo.extend(dijit.form.NumberSpinner, {
      _mouseWheeled: function() {}
    });

    dojo.extend(dijit.form.HorizontalSlider, {
      _mouseWheeled: function() {}
    });

    //Disabling back navigation on backspace
    $(document).unbind('keydown').bind('keydown', function (event) {
      var doPrevent = false;
      if (event.keyCode === 8) {
          var d = event.srcElement || event.target;
          if ((d.tagName.toUpperCase() === 'INPUT' && (d.type.toUpperCase() === 'TEXT' || d.type.toUpperCase() === 'PASSWORD' || d.type.toUpperCase() === 'FILE' || d.type.toUpperCase() === 'EMAIL' ))
               || d.tagName.toUpperCase() === 'TEXTAREA') {
              doPrevent = d.readOnly || d.disabled;
          }
          else {
              doPrevent = true;
          }
      }

        if (doPrevent) {
            event.preventDefault();
      }
    });

    var qdata = {};
    var testdata = {};

    var startTime = new Date();

    var dojoForm = registry.byId("formcol");
    //console.log(dojoForm);

    var inBuilder = false;
    var clashingColNames = [];

    var insertID = query("#formcol .insertID")

    for (let insert = 0; insert < insertID.length; insert++) {
      var l = insertID[insert];

      console.log(l)

      if (window.userID) {
        var href = domProp.get(l, "href")
        href += userID
        domProp.set(l, "href", href)
        domProp.set(l, "target", "_blank")
      }
    }
    var links = query("#formcol a")
    for (let link = 0; link < links.length; link++) {
      var l = links[link];
      domProp.set(l, "target", "_blank");
    }

    function addToQdata(col, value) {
      if(inBuilder) {
        if(qdata.hasOwnProperty(col)) {
          clashingColNames.push(col);
        }
      }
      qdata[col] = value;
    }

    function uniqueArr(arr) {
      var result = [];
      arr.forEach(function(item) {
           if(result.indexOf(item) < 0) {
               result.push(item);
           }
      });

      return result;
    }

    var is_checked = function(id) {
      var widget = registry.byId(id);
      return (widget.get('checked') === true ? true : false);
    };

    //deprecated
    var widget_value = function(id) {
      return registry.byId(id).get('value');
    };
    var validate_widget = function(id) {
      return (registry.byId(id).validate() === true ? true : false);
    };
    var nonempty_text_widget = function(id) {
      var text = widget_value(id);
      return (text.length > 0);
    };
    var set_select_value = function(id, value, default_value) {
      return (is_checked(id) === true ? value : default_value);
    };

    //Deprecated
    var text_widget_value = function(id, maxlen) {
      var text = widget_value(id);
      if (maxlen > text.length) {
        return text;
      }
      return text.substring(0, maxlen);
    };

    var save_value = function(data, params, column){
      var id = params[0];
      var value = registry.byId(params[0]).get('value');

      addToQdata(column, value);
      //qdata[column] = value;
    };

    var save_textwidget_value = function(data, params, column) {
      var id = params[0];
      var maxlen = params[1];

      var text = registry.byId(id).get('value').replace(/\r?\n|\r/g, " ");
      //console.log(text);
      if (maxlen > text.length) {
        text = text;
      }
      else {
        text = text.substring(0, maxlen);
      }

      addToQdata(column, text);

      //qdata[column] = text;
    };

    var save_first_checked = function(data,
                                      ids,
                                      values,
                                      column_name,
                                      default_value){
      var len = Math.min(ids.length, values.length);
      var i = 0;
      var use_default = true;
      var id, value;

      while (i < len){
        id = ids[i];
        if (is_checked(id)){
          use_default = false;
          value = values[i];
          break;
        }
        i += 1;
      }
      if (use_default){
        value = default_value;
      }

      addToQdata(column_name, value);
      //qdata[column_name] = value;
    };

    var save_all_checked = function(data,
                                    ids,
                                    values,
                                    column_names,
                                    default_value){
      var len = Math.min(ids.length, values.length, column_names.length);
      var i = 0;
      var id, value, column;

      while (i < len){
        id = ids[i];
        column = column_names[i];
        if (is_checked(id)){
          value = values[i];
        } else {
          value = default_value;
        }
        //qdata[column] = value;

        addToQdata(column, value);
        i += 1;
      }
    };

    var show_saved_data = function(data){
      //Emptying div before rendering
      domConstruct.empty("formdata");

      for (var key in qdata){
        if (qdata.hasOwnProperty(key)){

          var splitKey = key.replace("questionnaire-id", "");
          domConstruct.create('tr', {innerHTML: "<td>" + splitKey +" </td><td> " + qdata[key] + "</td>"}, 'formdata');
          //domConstruct.create('dd', {innerHTML: qdata[key]}, 'formdata');
        }
      }
    };

    var send_questionnaire_data = function(data){
      console.log(JSON.stringify(data));

    };
    var loadData = function() {
      var funcArray = window.funcArray;
      qdata = {};

      for(var i = 0; i<funcArray.length; i++) {
        console.log(funcArray[i]);
        switch(funcArray[i].fun) {
          case "save_all_checked":
            // console.log("save_all_checked " + i);
            save_all_checked.apply(undefined, funcArray[i].params);
            break;
          case "save_first_checked":
            // console.log("save_first_checked " + i);
            save_first_checked.apply(undefined, funcArray[i].params);
            break;
          case "save_value":
            // console.log("save_value " + i);
            save_value.apply(undefined, funcArray[i].params);
            break;
          case "save_textwidget_value":
            // console.log("save_textwidget_value " + i);
            save_textwidget_value.apply(undefined, funcArray[i].params);
            break;
        }
      }
      return qdata;
    };

    if (dom.byId('showData')) {
      on(registry.byId('showData'), "click", function() {
        //var domContainer = dom.byId("formcol");
        //var widgets = registry.findWidgets(domContainer);

        var warningBox = dom.byId("warningbox");
        domClass.add(warningBox, "hiddenelem");

        inBuilder = true;
        clashingColNames = [];

        var form = registry.byId("formcol");

        var valid = form.validate();

        var d = loadData();

        formcol.validate();

        show_saved_data(d);

        if (clashingColNames.length > 0) {

          clashingColNames = uniqueArr(clashingColNames);

          var names = "name";
          var isare = "is";
          if (clashingColNames.length > 1) {names = "names"; isare="are"}

          var message = "WARING: The column " + names + " <strong>" + clashingColNames.toString() +
                        "</strong> " + isare + " used multiple times, is this intended?";

          warningBox.innerHTML = message;
          domClass.remove(warningBox, "hiddenelem");
        }

        inBuilder = false;
        //console.log("Widgets:");
        //console.log(widgets);

      });
    }

    if (dom.byId("submitButton")) {
      on(registry.byId("submitButton"), "click", function() {
        console.log("submitbutton");

        var valid = formcol.validate();
        var clickLimit = true;


        if (lastClick) {
          if ((Date.now() - lastClick) < 500) {
            console.log("Submit clicked in quick succession, ignore click " + (Date.now() - lastClick));
            clickLimit = false;
          } else {
            lastClick = Date.now();
          }
        }

        if(!lastClick) {
          lastClick = Date.now();
        }

        if(valid && clickLimit) {
          var formdata = loadData();
          send_questionnaire_data(formdata);

          var duration = Date.now() - startTime;

          var d = {}
          d.exp = formdata;
          d.duration = duration;

          xhr.post(document.URL,{data:JSON.stringify(d)}).then(function(response) {
            console.log(response);
            var url = document.URL;
            var response = JSON.parse(response);

            if(response.redirect) {
              console.log("JSON_REDIRECTING");
              window.location.replace(response.redirect);
            }

            else {
              //currentPhase = parseInt(document.URL.slice(76));
              var currentPhase = parseInt(url.substr(url.lastIndexOf("/")+1));

              url = url.slice(0, url.lastIndexOf("/")+1);
              if (!isNaN(currentPhase)) {
                window.location.href = url+(currentPhase+1);
              }
              else {
                location.reload();
              }
              //window.location.assign("../");
            }
          });
        }
      });
    }
  });
});