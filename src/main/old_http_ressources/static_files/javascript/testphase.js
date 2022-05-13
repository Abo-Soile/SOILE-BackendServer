require([
  "dojo/request/xhr",
  "dojo/json",
  "levenshtein",
  "dojo/ready"
  ],
function(
  xhr,
  JSON,
  levenshtein,
  ready
  ) {
  ready(function() {

    window.Levenshtein = levenshtein;
    //Preventing scroll on arrowkeys 37-40 and navigation on backspace 8
    document.addEventListener("keydown", function (e) {
      if([37,38,39,40,8,32].indexOf(e.keyCode) > -1){
        //console.log(e);
        if(e.target.tagName == "INPUT" || e.target.type == "text" || e.target.tagName == "TEXTAREA") {
          return
        }

        e.preventDefault();

        // Do whatever else you want with the keydown event (i.e. your navigation).
      }
    }, false);
    //console.log(window.testJS);

    function startFunc() {
      console.log("Starting!!!");
    }

    function sendData(d) {

      //Send data xhr,
      xhr.post(document.URL, {timeout:10000,data:JSON.stringify(d)}).then(
        function(response) {

          if (typeof response !== 'undefined') {
            response = JSON.parse(response);
          }

          if(response.redirect) {
            console.log("JSON_REDIRECTING");
            window.location.replace(response.redirect);
          }

          else {
            //Navigate to next phase
            var url = document.URL;
            var currentPhase = parseInt(url.substr(url.lastIndexOf("/")+1));
            url = url.slice(0, url.lastIndexOf("/")+1);

            if(!isNaN(currentPhase)) {
              console.log("Redirecting " + isNaN(currentPhase));
              window.location.href = url+(currentPhase+1);
            }else {
              location.reload();
            }
          }
        },function(error) {
          console.log("Sending data failed, retrying...");
          setTimeout(function() {
            console.log("...resending");
            sendData(d);
          }, 1000);
        });
    }

    function end(expdata, duration, score, persistantData) {
      console.log("Test over");
      console.log(expdata);

      var d = {};
      d = {};
      d.exp = expdata;
      d.duration = duration;
      d.score = score;
      d.persistantData = persistantData;

      //Showing loadingscreen at the end when sending data.
      SOILE2.util.enableLoadScreen();

      sendData(d);
    }

    function startSoile(data) {
      console.log("Starting soile");
      SOILE2.util.enableLoadScreen();

      if(typeof window.persistantData !== "undefined") {
      	SOILE2.util.setPersistantData(window.persistantData);
      }

      if(typeof window.testConfig !== "undefined") {
        SOILE2.util.setPilotMode(window.testConfig.pilotMode);
      }

      SOILE2.util.setStartFunction(startFunc);
      SOILE2.util.eval(data);

      SOILE2.util.setEndFunction(end);

      SOILE2.start();
    }

    console.log(typeof window.testJs);

    if (window.testJs !== undefined) {
     // console.log("fdslkfdsklfjdsl");
      var data = window.testJs;
      startSoile(data);
    }
    else {
      //console.log("dklfjd");
      var jsonUrl = document.URL + "/json";
      xhr.get(jsonUrl).then(function(data) {

        startSoile(data);
        //setTimeout(function() {
        //  SOILE2.rt.exec_pi();
        //}, 1500);
      });
    }
  });
});