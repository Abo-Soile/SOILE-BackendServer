var app = angular.module('userList', []);

app.config(function($interpolateProvider){
    $interpolateProvider.startSymbol('[([').endSymbol('])]');
});


app.controller("UserController", function($scope,$http,$location) {

  $scope.loadData = function() {
    $scope.users = [];

    $http.get("user/json").success(function(data, status) {
        $scope.users = data;

        for (var i = 0; i < $scope.users.length; i++) {
          if($scope.users[i].forgottenPasswordToken) {
            $scope.users[i].hasToken = true;
          } else {
            $scope.users[i].hasToken = false;
          }
        }
    });
  };

  $scope.updateUser = function(userIndex) {

    var user = $scope.users[userIndex];

    $http.post("user/" + user._id, user).success(function(data, status) {
      //$scope.loadData();
      $scope.users[userIndex] = data;
    });
  };

  $scope.updateToken = function(userIndex) {
    if($scope.users[userIndex].hasToken) {
      $scope.users[userIndex].forgottenPasswordToken = true;
    }else {
      $scope.users[userIndex].forgottenPasswordToken = false;
    }
    $scope.updateUser(userIndex)
  }

  $scope.createUser = function(username){
    var user = {username:username};

    $http.post("user", user).success(function(data, status) {
      $scope.loadData();
    });
  }

  $scope.loadData();
});
