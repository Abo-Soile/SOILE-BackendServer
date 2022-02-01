var app = angular.module('experimentEdit',
       ['ui.tree',
        'ui.bootstrap',
        'ui.select',
        'ngSanitize',
        'angular-ladda',
        'monospaced.elastic',
        'useraccess',
        'angularFileUpload'
        ]);

app.config(function($interpolateProvider){
    $interpolateProvider.startSymbol('[([').endSymbol('])]');
});

/*Marks html as safe*/
app.filter('unsafe', function($sce) { return $sce.trustAsHtml; });

//Reverses the order of an array
app.filter("reverse", function(){
    return function(items){
        return items.slice().reverse();
    };
});

app.filter('propsFilter', function() {
  return function(items, props) {
    console.log(items);
    console.log(props);
  };
});

app.filter('range', function() {
  return function(input, min, max) {
    min = parseInt(min); //Make string input int
    max = parseInt(max);
    for (var i=min; i<max; i++){
          input.push(i);
        }
    return input;
  };
});

app.controller('componentController', function($scope, $http, $location) {
    var baseUrl = $location.absUrl();

    $scope.delComponent = function(type,index) {
        console.log("Deleting " + type + " : " + index);
        $scope.experiment.components.splice(index, 1);
    };
});

app.controller('experimentController', function ($scope, $http, $location, FileUploader) {
    var baseUrl = $location.absUrl();

    //$scope.formats = ['dd-MMMM-yyyy', 'yyyy/MM/dd', 'dd.MM.yyyy', 'shortDate'];
    $scope.format = 'yyyy/MM/dd';
    $scope.test = {};

    $scope.uploader = new FileUploader({ url: "addvideo" });

    $scope.uploader.autoUpload = true;
    $scope.uploader.removeAfterUpload = true;
    $scope.uploader.onCompleteItem = function(res) {
      console.log("UPLOADD")
      console.log(res)

      $scope.addVideo(res.video)
    };


    function loadData() {
      $http.get("json").success(function(data,status) {
          console.log(data);
          $scope.experiment = data;

          $scope.startdate = new Date($scope.experiment.startDate);
          $scope.enddate = new Date($scope.experiment.endDate);

          for (var i = 0; i < $scope.experiment.components.length; i++) {
            if($scope.experiment.components[i].random) {
              $scope.experiment.components[i].randomgroup = $scope.experiment.components[i].random;
              $scope.experiment.components[i].random = true;
            }
          }
      });
    }

    $scope.open = function($event) {
      $event.preventDefault();
      $event.stopPropagation();

      console.log("OPEN");

      $scope.opened = true;
    };

    $scope.dateOptions = {
      formatYear: 'yy',
      startingDay: 1
    };

    $scope.save = function save() {
      var data = JSON.parse(JSON.stringify($scope.experiment));

      data.startDate = $scope.startdate.toISOString();
      data.endDate = $scope.enddate.toISOString();

      //if (data.shortname == "") {
        //delete data.shortname;
      //}

      $scope.loading = true;

      for (var i = 0; i < data.components.length; i++) {
        if(data.components[i].random) {
          data.components[i].random = data.components[i].randomgroup;
        }
        delete data.components[i].randomgroup;
      }

      $http.post(baseUrl, data).then(function(status) {
        $scope.loading = false;
      });
    };

    $scope.refreshTests = function(search) {
      return $http.get('/test/json')
      .then(function(response) {
        $scope.tests = response.data;
        console.log($scope.tests);
      });
    };

    var loadTests = function() {
      return $http.get('/test/json/compiled')
      .then(function(response) {
        $scope.tests = response.data;

        for (var i = 0; i < $scope.tests.length; i++) {
          $scope.tests[i].findName = $scope.tests[i].folder + "/" + $scope.tests[i].name;
        };
      });
    };

    $scope.addTest = function() {
      console.log($scope);
      var compObject = {};
      compObject.name = $scope.test.selected.name;
      compObject.id = $scope.test.selected._id;
      compObject.type = "test";

      $scope.experiment.components.push(compObject);

      $scope.test.selected = null;

    };

    $scope.addForm = function() {
      $http.post('addform')
      .then(function(response) {
        var data = response.data;

        var compObject = {};
        compObject.name = "";
        compObject.id = data._id;
        compObject.type = "form";

        $scope.experiment.components.push(compObject);

        $scope.save();
      });
    };

  $scope.addVideo = function (filename) {

    var compObject = {};
    compObject.name = "VideoPhase";
    compObject.type = "video";
    compObject.videofile = filename;
    compObject.record = "true";

    $scope.experiment.components.push(compObject);

    $scope.save();

  };

  $scope.uploadFile = function (files, videoId) {
    var fd = new FormData();
    //Take the first selected file
    fd.append("file", files[0]);

    var uploadUrl = "addvideo"

    $http.post(uploadUrl, fd, {
      headers: { 'Content-Type': undefined },
      transformRequest: angular.identity
    }).success(function(res) {
      $scope.addVideo(res.video)
      // $scope.experiment.components[index].videofile = res;
    }).error(function(err) {
      console.log("Something didnt work")
    });

  };

    //$scope.startdate = new Date();
    //$scope.enddate = new Date();

    loadTests();
    loadData();
});