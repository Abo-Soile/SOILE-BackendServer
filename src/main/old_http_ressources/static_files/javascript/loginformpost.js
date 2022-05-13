function sArrayToJson(sArr) {
    var res = {};
    $.each(sArr, function(i,v) {
        res[sArr[i].name] = sArr[i].value;
    })

    return res;
}

$("#showRegister").click(function() {

    $("#showRegister").hide(0,function() {
        $("#registerContainer").show("slow");
    });
});
var rForm = $("#registerform");
var lForm = $("#loginform");

var lAlert = $("#login-alert");
var rAlert = $("#register-alert");

console.log("Init");
console.log($("#btn-register"));

$("#btn-register").click(function(e) {
    var data =sArrayToJson(rForm.serializeArray());

    data.username = data.username.trim();

    var req = {
        "username": data.username,
        "password":data.password,
        "passwordAgain":data.passwordAgain
    }

    $.post("/signup/json", JSON.stringify(data), function(resp) {
        console.log(resp)
        if(resp.status=="ok") {
           location.reload();
        } else {
            rAlert.css("display","block").html(resp.error);

        }
    })
})

$("#btn-login").click(function(e) {
    var data =sArrayToJson(lForm.serializeArray());

    var req = {
        "username": data.username,
        "password":data.password
    }

    $.post("/login/json", JSON.stringify(req), function(resp) {
        console.log(resp)
        if(resp.status=="ok") {
            location.reload();
        } else {
            lAlert.css("display","block").html(resp.error);
        }
    })
})