var vertx = require("vertx");
var console = require('vertx/console');

var verticleAddress = "soile.my_mailer";

var defaultFrom = "noreply@kogni.abo.fi"

function looksLikeMail(str) {
    var lastAtPos = str.lastIndexOf('@');
    var lastDotPos = str.lastIndexOf('.');
    return (lastAtPos < lastDotPos &&  // @ before last .
      lastAtPos > 0 &&                 // Something before @
      str.indexOf('@@') === -1 &&       // No double @
      lastDotPos > 2 &&                // 3 chars before .com
      (str.length - lastDotPos) > 2);  // domain = min 2 chars
}

var mailManager = {
    sendMail:function(to, subject, body, response) {
        var mail = {
            "to": to,
            "from": defaultFrom,
            "subject": subject,
            "body": body
        };

        vertx.eventBus.send(verticleAddress, mail, response);
    },

    passwordReset:function(to, resetUrl,response) {
        var body = "\
Hello \n  \
You or someone specifying your email account, has requested to \
have their password reset for Soile.\n\n \
To reset the password, click the link below:: \n " + 
resetUrl + 
"\n\n Soile Team";

        var subject = "Soile Password reset";

        this.sendMail(to, subject, body, response);
    },

    sendTrainingReminder:function(header, body, user, url, callback) {
        console.log("Send reminder mail")

        var body = body.replace("{link}", url);

        console.log(body)

        if(!looksLikeMail(user.username)){
            console.log("username doesn't look like an email address")
            return
        }

        console.log("Sending reminder mail")
        this.sendMail(user.username, header, body, callback);
    }
}

module.exports = mailManager;