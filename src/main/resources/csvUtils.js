var vertx = require('vertx');
var console = require('vertx/console');

var babyparser = require("libs/babyparse"); 

function jsonRowDataToCsv(json, groupby) {

  var users = {};
  var command = "single";
  var headers = {};
  var idField = groupby;

  console.log("jsonlen = " + json.length);

  for (var i = 0; i < json.length; i++) {
    var item = json[i];
    var id = item[idField];

    if (typeof users[id] === "undefined") {
      users[id] = {};
      //console.log("new user");
    } 

    var data = null;
    var checkForExtra = false;

    if (typeof item.data !== "undefined") {
      if (command === "single") {
        data = item.data.single;
      }

      // No single or raw object -> form
      if (typeof item.data.single === "undefined") {
        data = item.data;
        checkForExtra = true;
      }
    }


    users[id]["timestamp_" + item.phase] = item.timestamp;
    headers["timestamp_" + item.phase] = "";

    for (var datapoint in data) {

      var dataheader = false;

      // Cheking for questionnair id in header and removing if it exists
      if(checkForExtra && datapoint.slice(0,17) === "questionnaire-id:") {
        dataheader = datapoint.slice(17, datapoint.length) + " phase" +item.phase;
      } else {
        dataheader = datapoint + " phase" +item.phase;
      }

      users[id][dataheader] = data[datapoint];
      headers[dataheader] = "";

      //console.log(datapoint + " --- " + data[datapoint]);
    }
  }

  headers[groupby] = "";

  //console.log(JSON.stringify(users));

  var resArr = [];

  for (var id in users) {
    users[id][groupby] = id;
    resArr.push(users[id]);
  } 

  resArr.unshift(headers);

  var csv = babyparser.unparse(resArr, {"delimiter":";"});

  return csv;
}


/*
  Writes raw 2d data in humanredable format, where groupby
  is set as a heading for each thing to groupby.
*/
function jsonMatrixDataToCsv(json, groupby) {

  var data = [];
  var sep =";";

  var csvData = "";

  for (var ik = 0; ik < json.length; ik++) {
    if (typeof json[ik].data.rows !== "undefined") {
      data.push(json[ik]);
    }
  }

  for (var i = 0; i < data.length; i++) {
    var element = data[i];
    var keys = {};

    csvData += groupby+ ": " + element[groupby] + sep + "\n";
    var rowCount = element.data.rows.length;
    for (var j = 0; j < element.data.rows.length; j++) {
      var row = element.data.rows[j];
      for (var rkey in row) {
        if (keys.hasOwnProperty(rkey)) {
          keys[rkey][j] = JSON.stringify(row[rkey]);
        }else {
          keys[rkey] = [];
          keys[rkey][j] = JSON.stringify(row[rkey]);
        }
      }
    }

    var csv = "";
    var lastK = "";
    
    //Building headers
    for(var k in keys) {
      csv += k + sep;
      lastK = k;
    }

    csv += "\n";
    
    /*
      Write results to the csv file
    */
    for (var ij = 0; ij < rowCount; ij++) {
      for(var k in keys) {
        if ( keys[k][ij] !== undefined) {
          csv += keys[k][ij] + sep;
          
        } else{
          csv += sep;
        }
      }
      csv += "\n"; 
    }

    csvData += csv;

  }
  return csvData;
}


/*
  Writes raw 2d data in a machinereadable format.
*/
function jsonMatrixToCsvSorted(json, groupby) {
  var data = json;
  var sep =";";

  var csvData = "";

  var keys = {};

  keys.userid = [];

  var totalRows = 0;

  var rowcounter = 0;
  
  for (var i = 0; i < data.length; i++) {
    var element = data[i];

    //csvData += "userID: " + sep +  element.userid + sep + "\n";
    //console.log("RawData number of rows: " + element.data.rows.length);
    var rowCount = element.data.rows.length;
    for (var j = 0; j < element.data.rows.length; j++) {
      var row = element.data.rows[j];
      for (var rkey in row) {
        if (keys.hasOwnProperty(rkey)) {
          //keys[rkey][j] = JSON.stringify(row[rkey]);
          keys[rkey][rowcounter] = JSON.stringify(row[rkey]);
        }else {
          keys[rkey] = [];
          //keys[rkey][j] = JSON.stringify(row[rkey]);
          keys[rkey][rowcounter] = JSON.stringify(row[rkey]);
        }
        //keys.userid[j] = element.userid;
        keys.userid[rowcounter] = element.userid;

      }
        rowcounter += 1;
    }
    totalRows += rowCount;
  }

  totalRows = rowcounter;

  var csv = "";
  var lastK = "";
  
  //Building headers
  for(var k in keys) {
    csv += k + sep;
    lastK = k;
  }

  //console.log(csv + "\n");
  csv += "\n";
  
  /*
    Skriver ut resultatet till csv:n
  */
  //for (var ij = 0; ij < keys[lastK].length; ij++) {
  for (var ij = 0; ij < totalRows; ij++) {
    for(var k in keys) {
      if ( keys[k][ij] !== undefined) {
        csv += keys[k][ij] + sep;
        
      } else{
        csv += sep;
      }
    }
    csv += "\n"; 
  }

  csvData += csv;

  return csvData;
}


function jsonSingleTrainingVarToCsv(data, variable) {
   var users = {};

   var maxHeader = 0;

   for (var i = 0; i < data.length; i++) {
     var item = data[i];

    if (typeof users[item.userId] === "undefined") {
      users[item.userId] = [];
      console.log("new user");
    } 

    var itemData = item.data.single;
    if (typeof item.data.single === "undefined") {
      itemData = item.data;
    }

    users[item.userId][item.trainingIteration] = itemData[variable];

    console.log(variable + "  " + itemData[variable] + " iteration: " + item.trainingIteration);
    console.log(JSON.stringify(item));

    if (item.trainingIteration > maxHeader) {
      maxHeader = item.trainingIteration;
      console.log("new maxheader: " + maxHeader);
    }
   }

  var headers = {};

  for (var j = 0; j <= maxHeader; j++) {
    headers[j] = "";
  }
  headers.userId = "";

  var resArr = [];

  for (var id in users) {
    users[id].userId = id;
    resArr.push(users[id]);
  } 
  resArr.unshift(headers);


  var csv = babyparser.unparse(resArr, {"delimiter":";"});

  return csv;
}

function jsonArrayToCsv(data) {
  var csv = babyparser.unparse(data, {"delimiter":";"});

  return csv;
}

var csvUtils = (function() {
  return {
    'jsonRowDataToCsv':jsonRowDataToCsv,
    'jsonMatrixDataToCsv':jsonMatrixDataToCsv,
    'jsonMatrixToCsvSorted':jsonMatrixToCsvSorted,
    'jsonSingleTrainingVarToCsv':jsonSingleTrainingVarToCsv,
    'jsonArrayToCsv':jsonArrayToCsv
  };
});

module.exports = csvUtils();