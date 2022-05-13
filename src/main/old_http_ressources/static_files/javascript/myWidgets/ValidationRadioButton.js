define(["dojo/_base/declare", "dojo/_base/lang", "dojo/on", "dijit/Tooltip",
        "dijit/form/RadioButton", "dojo/dom-class", "dijit/registry", 
        "dijit/_Widget"
        ],
  function(declare, lang, on, Tooltip,RadioButton, domClass, registry, _WidgetBase) {

    return declare('dijit.form.ValidationRadioButton',[_WidgetBase], {

      message:"",
      tooltipPosition: ["below"],
      tooltip:false,
      optional:true,
      value:"test",
      state:"valid",

      constructor: function(params){
        this.constraints = {};
        this.baseClass += ' dijitValidationRadioButton';

        this.optional = params.optional;

        //domClass.add(this.domNode, "radioSelect");

      },

      startup: function() {
        var that = this;
        this.inherited(arguments);
        console.log("startup");

        var d = this.displayTooltip;

        on(this.domNode, "mouseenter", function(e) {
          //console.log(e)
          that.displayTooltip();
        });

      },

      validate: function(test) {
        //this.focusNode.setAttribute("aria-invalid", this.state == "Error" ? "true" : "false");
        var isValid = this.isValid();

        if (!isValid) {
          this.displayMessage("This field is required");
          this.state = "invalid";
        } else {
          this.displayMessage("");
          this.state = "valid";
        }
        return isValid;
      },

      isValid: function() {
        //console.log("isValid");
//        var selectedRadio = registry.findWidgets(this.domNode.parentNode.parentNode);
        var selectedRadio = registry.findWidgets(this.domNode);
        var that = this;

        var checked = false;
        for (var i = 0; i < selectedRadio.length; i++) {
          if(selectedRadio[i].get('checked')) {
            checked = true;
            break;
          }
        }
        if (checked || this.optional) {
          return true;
        } else {

          for (var i = 0; i < selectedRadio.length; i++) {
            selectedRadio[i].on("click", function() {that.validate();});
          }

          return false;
        }
      },

      displayMessage: function(message){
        // summary:
        //    Overridable method to display validation errors/hints.
        //    By default uses a tooltip.
        // tags:
        //    extension

        var node = this.domNode;

        if(message){
          domClass.add(node, "selectError");
        }else{
          Tooltip.hide(node);
          domClass.remove(node, "selectError");

          if (this.tooltip) {
            console.log("Removing tooltip")
          }
        }
      },

      displayTooltip: function() {
        var message = "This question is required";
        var node = this.domNode;
        if (this.state == "valid") {
          Tooltip.hide(node);
          domClass.remove(node, "selectError");

        } else {
          Tooltip.show(message, node, this.tooltipPosition, false);
          domClass.add(node, "selectError");
         }
      },

      _onInput: function(event) {
        var isvalid = this.validate();

        if(isvalid) {
          this.displayMessage('');
          this.inherited(arguments);
        }
      },

      onMouseUp: function(){
        // the message still exists but for back-compat, and to erase the tooltip
        // (if the message is being displayed as a tooltip), call displayMessage('')
        var isvalid = this.validate();

        if(isvalid) {
          this.displayMessage('');
          this.inherited(arguments);
        }
      },

      focus: function() {
      }
    });
  });