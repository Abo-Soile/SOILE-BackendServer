define(["dojo/_base/declare", "dojo/_base/lang", "dijit/form/SimpleTextarea", "dijit/form/ValidationTextBox"],
  function (declare, lang, SimpleTextarea, ValidationTextBox) {

    // return declare('dijit.form.ValidationTextArea', [SimpleTextarea], {
    return declare('dijit.form.ValidationTextArea', [SimpleTextarea, ValidationTextBox], {
      constructor: function (params) {
        this.constraints = {};
        this.baseClass += ' dijitValidationTextArea';

        // this.optional = params.optional;
        this.required = params.required;

        if (params.required == undefined) {
          this.required = false;
        }
      },

      validate: function (test) {
        //this.focusNode.setAttribute("aria-invalid", this.state == "Error" ? "true" : "false");
        // var isValid = this.isValid();

        // var isValid = true;
        var isValid = this.isValid();


        // if (!isValid) {
        //   this.displayMessage("This field is required");
        //   this.state = "invalid";
        // } else {
        //   this.displayMessage("");
        //   this.state = "valid";
        // }
        // var required = this.params.required = "true" ? true : false

        //Backend doesnt reverse the required flag, so let's do that here instead for now
        var required = this.params.required ? false : true

        // var required = this.params.required ? true : false

        if (this.value.length <= 0 & required == true) {
          this.displayMessage("This field is required, length");
          this.state = "Error";

          isValid = false;

        } else {
          isValid = true;
          this.state = "";
        }

        return isValid;
      },
      templateString: "<textarea ${!nameAttrSetting} data-dojo-attach-point='focusNode,containerNode,textbox' autocomplete='off'></textarea>"
    })
  })