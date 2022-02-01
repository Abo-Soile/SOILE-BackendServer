var app = angular.module('trainingEdit', 
                      ['ui.tree',
                       'ui.select',
                       'ngSanitize',
                       'ui.bootstrap',
                       'useraccess'
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

app.service('trainingService', function($http, $location, $q) {
  var deferred = $q.defer();

  /**
   * Returns all available trainingexperiments
   * @return {promise} Resolves to the training list data
   */
  this.getTrainings = function() {
    var promise = $http.get("/training/json").success(function(data,status) {
      deferred.resolve(data);
    });

    return deferred.promise;
  }
});

app.controller('trainingController', function($scope, $http, $location, trainingService) {
  var baseUrl = $location.absUrl();

  $scope.format = 'yyyy/MM/dd';

  $scope.availableTrainings = [];

  $scope.training = {};

  trainingService.getTrainings().then(function(res){
    $scope.availableTrainings = [];
    res.forEach(function(r) {
      $scope.availableTrainings.push({name:r.name, _id:r._id});
    })
    $scope.availableTrainings = res;
  });

  $scope.getRepeatCount = function() {
    if (isNaN(parseInt($scope.training.repeatcount))) {
      return [];
    }else {
      return new Array(parseInt($scope.training.repeatcount));
    }
  };

  $scope.saveTraining = function save() {
      console.log("SSSSAAAVVE");
      var data = angular.copy($scope.training);

      for (var i in data.components) {
        var comp = data.components[i];

        for (var j = 0; j < comp.length; j++) {
          if (comp[j].random) {
            comp[j].random = comp[j].randomgroup;
            delete comp[j].randomgroup;
          }
        }
      }

      $http.post(baseUrl, data);
  };

  $scope.loadData = function() {
      $http.get("json").success(function(data,status) {
          $scope.training = data;

          for (var i in $scope.training.components) {
            var comp = $scope.training.components[i];
            //console.log(comp);

            for (var j = 0; j < comp.length; j++) {
              if (comp[j].random) {
                comp[j].randomgroup = comp[j].random;
                comp[j].random = true;
              }
            }
          }          
          console.log($scope);
      });
  };

  $scope.toggleControlGroup = function(event) {
      console.log(event);
      if ($scope.training.controlgroup) {
          $scope.training.components.control = [];
      } else {
          delete($scope.training.components.control);
      }
  };

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


  $scope.loadData();
});

app.controller('componentController', function($scope, $http, $location) {

  $scope.test = {};

  $scope.delComponent = function(type,index) {
      console.log("Deleting " + type + " : " + index);
      $scope.training.components[type].splice(index, 1);
  };


  $scope.addTest = function() {
    console.log($scope);
    var compObject = {};
    compObject.name = $scope.test.selected.name;
    compObject.id = $scope.test.selected._id;
    compObject.type = "test";

    $scope.training.components.training.push(compObject);

    $scope.test.selected = null;

  };

  $scope.addForm = function() {
    $http.post('addform')
    .then(function(response) {
      var data = response.data;

      $scope.loadData();
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

  $scope.refreshTests = function(search) {
    return $http.get('/test/json/compiled')
    .then(function(response) {
      $scope.tests = response.data;
      console.log($scope.tests);
    });
  };

/*
  var arr = [];
  arr.push({name:"aaa", type:"test"});
  arr.push({name:"bbb", type:"form"});
  arr.push({name:"ccc", type:"test"});

  $scope.training = {};
  $scope.training.name = "TESTNAME";

  $scope.components = {};
  $scope.components.pre = [];
  $scope.components.training = [];
  $scope.components.post = [];

  $scope.components.pre = arr;
  $scope.components.training = arr;
  $scope.components.post = arr;

  $scope.training.components = $scope.components;
*/
  loadTests();
});


app.directive('selectboxes', function () {
    return {
        restrict: 'AE',
        scope: {
          component:"=",
          rep:"="
        },
        template: "<div class='ng-selectboxes'>"+
                  "<span ng-click='toggleIterationElement($event,lk)'" + 
                  "ng-repeat='(lk,i) in getRepeatCount() track by $index' ng-click=[([lk])] " + 
                  "ng-class='{activePhase: getIterationStatusByIndex($index)}'>[([lk + 1])]</span></div>",
        link:function (scope, element, attrs) {
          var component = scope.component;
          //console.log(scope.training.components.training);

          if (!Array.isArray(component.iterationcontrolarray) && scope.rep > 0) {
            component.iterationcontrolarray = new Array(parseInt(scope.rep));
            for (var i = 0; i < component.iterationcontrolarray.length; i++) {
              component.iterationcontrolarray[i] = false;
            }
          }

          scope.toggleIterationElement = function(event, id) {
            component.iterationcontrolarray[id] = !component.iterationcontrolarray[id];
          };

          scope.getRepeatCount = function(){
            if (isNaN(parseInt(scope.rep))) {
                return [];
            } else {
                return new Array(parseInt(scope.rep));
            }
          };

          scope.getIterationStatusByIndex = function (index) {
            return component.iterationcontrolarray[index];
          };
        }

    };
});

