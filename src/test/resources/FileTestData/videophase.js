navigator.getUserMedia =
  navigator.getUserMedia ||
  navigator.webkitGetUserMedia ||
  navigator.mozGetUserMedia;

//starts video recording and returns stop-function inside a promise.
// When stop-function is called it stops recording and returns the video data inside a promise
async function startRecording(stream, startDelay) {
  let recorder = new MediaRecorder(stream);
  let data = [];

  recorder.ondataavailable = (event) => data.push(event.data);
  //wait the startdelay time
  await new Promise((resolve) =>
    setTimeout(() => {
      recorder.start();
      resolve();
    }, startDelay)
  );

  //returns a function that is used to stop recording
  return async function () {
    recorder.stop();
    await new Promise((resolve, reject) => {
      recorder.onstop = resolve;
      recorder.onerror = (event) => reject(event.name);
    });
    return data;
  };
}

async function openFullscreen() {
  var elem = document.documentElement;

  if (elem.requestFullscreen) {
    await elem.requestFullscreen();
  } else if (elem.webkitRequestFullscreen) {
    /* Safari */
    await elem.webkitRequestFullscreen();
  } else if (elem.msRequestFullscreen) {
    /* IE11 */
    await elem.msRequestFullscreen();
  }
}

async function videophase() {
  //Preventing scroll on arrowkeys 37-40 and navigation on backspace 8
  document.addEventListener(
    'keydown',
    function (e) {
      if ([37, 38, 39, 40, 8].indexOf(e.keyCode) > -1) {
        if (
          e.target.tagName == 'INPUT' ||
          e.target.type == 'text' ||
          e.target.tagName == 'TEXTAREA'
        ) {
          return;
        }

        e.preventDefault();
      }
    },
    false
  );
  const config = await (await fetch("config.json")).json();


  //var config = window.testConfig;
  var logMessages = [];
  var dataToStore = new FormData();

  //mediasettings
  var mediaContstraints = {
    audio: true
  };
  if (config.recordAudioOnly) {
    dataToStore.append('fname', 'recording.mp3');
    mediaRecorderOptions = { mimeType: 'audio/webm' };
  } else {
    dataToStore.append('fname', 'recording.mp4');
    mediaRecorderOptions = { mimeType: 'video/webm;codecs=vp8,opus' };
    mediaContstraints.video = {
      width: { exact: 1280 },
      height: { exact: 720 }
    };
  }

  //Startbutton
  const startButton = document.querySelector('#start-button');
  console.log("Activating start button")
  startButton.innerHTML = config.button || 'Start';
  startButton.style.display = 'initial';

  const preview = document.querySelector('#camera-preview');
  const previewInsructions = document.querySelector('#preview-instructions');
  const previewTitle = document.querySelector('#preview-title');
  const text = document.querySelector('#text')
  const title = document.querySelector('#title')
  const recordingText = document.querySelector('#recording-text')

  let stream = null;
  //gets camera stream.
  //this is a function so that camera stream is started only when its used first time
  async function getStream() {
    if (stream === null) {
      console.log("Trying to request user media")
      stream = await navigator.mediaDevices.getUserMedia(mediaContstraints);
    }
    return stream;
  }
  
    previewInsructions.innerHTML = config.previewInstructions || '';
    previewTitle.innerHTML = config.previewInstructionsTitle || '';
    if(config.previewInstructionsTitle === '-'){
      previewTitle.style.display = 'None';
    }

  //show preview
  if (config.showVideoPreview) {
    preview.srcObject = await getStream();
    preview.captureStream = preview.captureStream || preview.mozCaptureStream;

    await new Promise((resolve) => (preview.onplaying = resolve));
  } else {
    preview.style.display = 'None';
  }
  // Wait until the start button was clicked
  await new Promise((resolve, reject) => {
    startButton.addEventListener('click', (event) => resolve());
  });
  
  if (config.fullScreen) {
    await openFullscreen();
  }

  startButton.style.display = 'None';
  preview.style.display = 'None';
  previewInsructions.style.display = 'None';
  previewTitle.style.display = 'None';

  const mainVideo = document.querySelector('#main-video');
  console.log(config);
  if(config.file){
    //showVideo
    console.log("Trying to show video")
    mainVideo.style.display = 'initial';
    console.log("Starting video")
    mainVideo.play();
    
    //Record during video
    if (config.recordingOnStart) {
      console.log("Recording on start")
      const stop = await startRecording(await getStream(), 0);
      
      await new Promise((resolve, reject) => {
        mainVideo.addEventListener('ended', (event) => resolve());
      });
      
      const data = await stop();
      
      const recordedBlob = new Blob(data, {
        type: mediaRecorderOptions.mimeType
      });
      
      dataToStore.append('data', recordedBlob);
    }//if (config.recordingOnStart)
  }

    //Record after video
  if (config.recordingAfterVideo) {
    const recordButton = document.querySelector('#record-button');

    await new Promise((resolve, reject) => {
      if (mainVideo.ended || !config.file) {
        resolve();
      }
      mainVideo.addEventListener('ended', (event) => resolve());
    });
    mainVideo.style.display = 'none';
    if(config.textAfterVideo){
      text.style.display = 'inherit';
      text.innerHTML = config.textAfterVideo;
    }
    if(config.textAfterVideoTitle && config.textAfterVideoTitle !== '-'){
      title.style.display = 'inherit';
      title.innerHTML = config.textAfterVideoTitle;
    }

    recordButton.innerHTML = config.startRecordButton || 'Start recording';
    recordButton.style.display = 'initial';
    // Wait until the button was clicked
    await new Promise((resolve, reject) => {
      recordButton.addEventListener('click', (event) => resolve());
    });

    const stop = await startRecording(await getStream(), 0);
    // Remove text after recording is started
    text.innerHTML = '';
    recordingText.style.display = 'inherit';

    recordButton.innerHTML = config.stopRecordButton || 'Stop recording';

    //Stop recording eventhandler
    await new Promise((resolve, reject) => {
      recordButton.addEventListener('click', (event) => resolve());
    });

    const data = await stop();
    recordingText.style.display = 'none';
    recordButton.style.display = 'none';

    const recordedBlob = new Blob(data, {
      type: mediaRecorderOptions.mimeType
    });

    dataToStore.append('data', recordedBlob);
  }//  if (config.recordingAfterVideo) 
  //ends the video stream
  if (stream !== null) {
    stream.getTracks().forEach((track) => track.stop());
  }
  
  let timer = null;
  //set feedback timer
  if(config.minLoadingTime && !isNaN(config.minLoadingTime) && Number(config.minLoadingTime)>0){
    timer = new Promise((resolve) => {
      setTimeout(resolve, (Number(config.minLoadingTime)*1000));
    })
  }

  //send recorded data
  if (config.recordingAfterVideo || config.recordingOnStart) {
    //hide ended main video
    mainVideo.style.display = 'none';
    if(config.loadingText){
      text.style.display = 'inherit';
      text.innerHTML = config.loadingText;
    }
    if(config.loadingTextTitle && config.loadingTextTitle !== '-'){
      title.style.display = 'inherit';
      title.innerHTML = config.loadingTextTitle;
    } else if (config.loadingTextTitle === '-') {
      title.style.display = 'None';
    }
    try {
      const submissionFile = new File(dataToStore.getAll("data"), dataToStore.get("fname"), {type : mediaRecorderOptions.mimeType})      
      await window.submitFile(dataToStore.get("fname"),mediaRecorderOptions.mimeType, submissionFile);      
    } catch (e) {
      console.log(e);
    }
  } else {
    // if there is no recording, wait untill the video has finnished
    await new Promise((resolve, reject) => {
      if(!config.file) {
        resolve();
      }
      mainVideo.addEventListener('ended', (event) => resolve());
    });
    //after video has ended hide it
    mainVideo.style.display = 'none';
  }

  //Send meta-data
  async function sendMetaData() {
    window.reportResult({log: logMessages})    
  }
  await sendMetaData();

  await window.submitResult();

  if(timer !== null){
    await timer;
  }

  //writeData
  function log(messageType, message) {
    logMessages.push({
      timestamp: new Date().toString(),
      messageType: messageType,
      message: message
    });
    console.log(logMessages[logMessages.length - 1]);
  }
}
videophase();

