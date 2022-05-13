navigator.getUserMedia = navigator.getUserMedia || navigator.webkitGetUserMedia || navigator.mozGetUserMedia;

require([
  "dojo/request/xhr",
  "dojo/json",
  "dojo/ready"
  ],
function(
  xhr,
  JSON,
  ready
  ) {
  ready(function() {

    var config = window.testConfig

    var recordingOnStart = config.recordingOnStart
    var recordAfterVideo = config.recordingAfterVideo
    var recordVideo = true;
    var recordAudioOnly = config.recordAudioOnly;
    var showVideoPreview = config.showVideoPreview;

    var isRecordingAfter = false;

    if (!(recordingOnStart ||recordAfterVideo)) {
      recordVideo = false;
    }

    // const mediaRecorderOptions = { mimeType: 'video/webm;codecs=h264' };
    //var mediaRecorderOptions = { mimeType: 'audio/mpeg' };
    // var mediaRecorderOptions = { mimeType: 'video/mp4;codecs=h264' };
    var mediaRecorderOptions = { mimeType: 'video/mp4' };
    var recordingUploadName = ""
    var recordingUploadType = ""

    const mainVideo = document.querySelector('#main-video');
    const video = document.querySelector('#camera-video');
    const warning = document.querySelector('#warning');
    const message = document.querySelector('#videomessage');
    const startButton = document.querySelector('#start-button');

    startButton.innerHTML = config.button || "Start"

    const dataInput = [];

    var mediaContstraints = {
      audio: true,
    };

    if (recordAudioOnly) {
      recordingUploadName = "recording.mp3";
      recordingUploadType = "audio/mpeg";
      // mediaRecorderOptions = { mimeType: 'audio/mpeg' };
      mediaRecorderOptions = { mimeType: 'audio/webm' };

    }
    if (!recordAudioOnly && recordVideo) {
      recordVideo = true;
      recordingUploadName ="recording.mp4";
      recordingUploadType = "video/mp4";
      // mediaRecorderOptions = { mimeType: 'video/mp4;codecs=h264' };
      mediaRecorderOptions = { mimeType: 'video/webm;codecs=h264' };
      mediaContstraints.video = { width: { exact: 640 }, height: { exact: 480 } }
    }

    //Preventing scroll on arrowkeys 37-40 and navigation on backspace 8
    document.addEventListener("keydown", function (e) {
      if([37,38,39,40,8].indexOf(e.keyCode) > -1){
        if(e.target.tagName == "INPUT" || e.target.type == "text" || e.target.tagName == "TEXTAREA") {
          return
        }

        e.preventDefault();
      }
    }, false);


    function setPreviewDisplay() {
      if ( (!recordAudioOnly && recordVideo) && showVideoPreview) {
        video.style.display = "inherit";
      } else {
        video.style.display = "None";
      }
    }

    /**
     * Send captured video/audio data
     * @param {*} fd
     */
    function sendCapture(fd) {
      xhr.post(document.URL + "/video",
        {
          timeout: 50000,
          data: fd,
        }).then(
          function (response) {
            console.log("Upload Done")
            console.log(response)

            sendData(dataInput)
          })
    }

    /**
     * Send captured data to backend on phase end, doesn't include audioi/video tho
     * @param {*} d
     */
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
        }, function(error) {
          console.log("Sending data failed, retrying...", error);
          setTimeout(function() {
            console.log("...resending");
            sendData(d);
          }, 1000);
        });
    }

    let recorder = false;

    // Recording data stream
    const chunks = []
    let onData = (e) => {
      chunks.push(e.data)
    }

    function writeData(messageType, message) {
      dataInput.push({
        timestamp: new Date().toString(),
        messageType: messageType,
        message: message,
      })

      console.log(dataInput[dataInput.length - 1])
    }

    function onKeyInput(event) {
      let key = event.key;

      writeData("input", key);

      /* Stop recording on space*/
      if (recordAfterVideo) {
        if (key == " " || key == "Spacebar") {
          let r = recorder.requestData()
          recorder.stop()
        }
      }
    }

    /*Prevent backspace from going back*/
    document.addEventListener("keydown", function (e) {
      if ([8, 32].indexOf(e.keyCode) > -1) {
        e.preventDefault();
      }
    }, false);

    writeData("meta", "page loaded");

    video.width = 320;

    // navigator.mediaDevices.getUserMedia(constraints).
    //     then((stream) => { video.srcObject = stream });

    startButton.onclick = function () {
      writeData("meta", "start clicked");

      /**
       *  Request media access before starting playback this way we'll
       *  several chained video -> recording requests will be more similar.
       */
      navigator.mediaDevices.getUserMedia(mediaContstraints).then((s) => {
        startPlayback();
      }).catch(function(err ) {
        console.log("Media permission was denied!")
      })

      startButton.style.display = "None";

    }

    // mainVideo.addEventListener('canplaythrough', function () {
      // writeData("meta", "Video loaded");
      // startPlayback();
    // });


    function startPlayback() {
      writeData("meta", "Requesting camera access");

      if (recordAfterVideo || !(recordVideo)) {
        mainVideo.style.display = "inherit"
        warning.style.display = "none"

        mainVideo.play()

        document.addEventListener("keydown", onKeyInput);
        writeData("meta", "started playback")
      } else {
        navigator.mediaDevices.getUserMedia(mediaContstraints).
          then((stream) => {

            setPreviewDisplay()

            mainVideo.style.display = "inherit"
            warning.style.display = "none"

            writeData("meta", "Camera access ok");
            video.srcObject = stream;

            recorder = new MediaRecorder(stream, mediaRecorderOptions)
            recorder.ondataavailable = onData
            recorder.onstop = stopRecording

            document.addEventListener("keypress", onKeyInput);
            writeData("meta", "start input recording")

            recorder.start()
            mainVideo.play()
            writeData("meta", "started playback")
          });
      }
    }

    const stopRecording = function() {
      console.log("Stopping")
      // Upload
      // https://gist.github.com/rissem/d51b1997457a7f6dc4cf05064f5fe984
      // const bigVideoBlob = new Blob(chunks, { 'type': 'video/webm; codecs=webm' })
      // const bigVideoBlob = new Blob(chunks, { 'type': 'video/mp4;' })
      const bigVideoBlob = new Blob(chunks, { 'type': recordingUploadType })

      let fd = new FormData()
      fd.append('fname', recordingUploadName)
      fd.append('data', bigVideoBlob)

      writeData("meta", "starting upload")

      sendCapture(fd)

      writeData("meta", "upload done")

      console.log(dataInput)
    }

    mainVideo.onended = function () {
      writeData("meta", "Playback done")
      video.pause()

      if (!recordVideo) {
        sendData(dataInput)
        return;
      }

      if (recordAfterVideo) {
        mainVideo.style.display = "none"

        message.style.display = "inherit"
        message.innerHTML = config.description || "Your answer is being recorded, press spacebar when you'r done"

        navigator.mediaDevices.getUserMedia(mediaContstraints).
          then((stream) => {

            setPreviewDisplay()

            isRecordingAfter = true;

            mainVideo.style.display = "none"
            warning.style.display = "none"

            video.srcObject = stream;

            writeData("meta", "Camera access ok");

            recorder = new MediaRecorder(stream, mediaRecorderOptions)
            recorder.ondataavailable = onData
            recorder.onstop = stopRecording

            writeData("meta", "start input recording")

            recorder.start()
          });

      } else {
        let r = recorder.requestData()
        recorder.stop()
      }

    }
  });
});