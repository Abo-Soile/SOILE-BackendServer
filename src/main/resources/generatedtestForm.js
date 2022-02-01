
    require(["dijit/form/HorizontalSlider", 
             "dijit/form/HorizontalRuleLabels", 
             "dijit/form/HorizontalRule", 
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
             "dojo/aspect", 
             "dijit/registry", 
             "dojo/on",
             "dojo/parser",
             "dojo/query",
             "dojo/cookie",
             "dojo/json",
             "dojo/ready"],
      function(HorizontalSlider, 
               HorizontalRuleLabels, 
               HorizontalRule, 
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
               aspect, 
               registry, 
               on,
               parser,
               query,
               cookie,
               JSON,
               ready) {

        ready(function() {
          parser.parse();

          on(dom.byId('submitButton'), "click", function() {
            var qdata = {};
            var is_checked = function(id) {
              var widget = registry.byId(id);
              return (widget.get('checked') === true ? true : false);
            };
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
            var text_widget_value = function(id, maxlen) {
              var text = widget_value(id);
              if (maxlen > text.length) {
                return text;
              }
              return text.substring(0, maxlen);
            };

            var save_value = function(data, params, column, get_value){
              data[column] = get_value.apply({}, params);
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
              data[column_name] = value;
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
                data[column] = value;
                i += 1;
              }
            };

            var show_saved_data = function(data){
              for (var key in data){
                if (data.hasOwnProperty(key)){
                  domConstruct.create('dt', {innerHTML: key}, 'formdata');
                  domConstruct.create('dd', {innerHTML: data[key]}, 'formdata');
                }
              }
            };  

            var send_questionnaire_data = function(data){
              show_saved_data(data);
              console.log(JSON.stringify(data));
            }

            domConstruct.empty('formdata');
              // UNUSED: id_7fffff5b 
              save_value(qdata, ['id_7fffff5b', 50], 'questionnaire-id:comments', text_widget_value);
              save_value(qdata, ['id_7fffff5a'], 'questionnaire-id:jagbori', widget_value);
              save_first_checked(qdata,
                                 ['id_7fffff58','id_7fffff57','id_7fffff56','id_7fffff55','id_7fffff54'],
                                 ['0','1','2','4','3'],
                                 'questionnaire-id:singleselect', 'Valitse koulutustausta');
              save_all_checked(qdata,
                               ['id_7fffff52','id_7fffff51','id_7fffff50','id_7fffff4f','id_7fffff4e'],
                               ['0','1','2','4','3'],
                               ['questionnaire-id:edubg1','questionnaire-id:edubg2','questionnaire-id:edubg3','questionnaire-id:edubg4','questionnaire-id:edubg5'],
                               'Empty');
              save_value(qdata, ['id_7fffff4d', 20], 'questionnaire-id:TextArea', text_widget_value);
              save_value(qdata, ['id_7fffff4c'], 'questionnaire-id:Slider', widget_value);

            send_questionnaire_data(qdata);

          });
        });
      });
  