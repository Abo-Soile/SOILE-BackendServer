var mApp = angular.module('useraccess', []);

/*
Directive for managing user access to a certain component. A component is viewable
by all admins and all editors who have been granted permission.
*/
mApp.directive('useraccess',  ['$http', function($http) {
    return {
        restrict: 'AE',
        scope: {
          users:"=",
          savefunction:"="
        },
        templateUrl: '/javascript/directives/useraccess.html',
        link:function (scope, element, attrs) {

          //Array of already added users
          scope.addedUsers = [];
          scope.showUserAccess = false;
          scope.userAccessSymbol = '<i class="fa fa-angle-double-down ng-scope"></i>'

          scope.currentUserName = null;

          $http.get("/admin/user/current").success(function(data) {
            scope.currentUserName = data.username;
            //console.log(scope.currentUserName)
          })

          scope.showhideuseraccess = function() {
            scope.showUserAccess = !scope.showUserAccess;

            if (scope.showUserAccess) {
              scope.userAccessSymbol = '<i class="fa fa-angle-double-down ng-scope"></i>'
            } else {
              scope.userAccessSymbol = '<i class="fa fa-angle-double-up ng-scope"></i>'
            }
          }

          scope.loadUsers = function() {

            var url = scope.address;
          };

          /*Load list of editors*/
          scope.loadUserList = function() {
            var userUrl = "/admin/user/json/editor/filter";

            $http.get(userUrl).success(function(data) {
              scope.userList = data;

              /*Removing already added users from the list*/

              if (!scope.users) {
                scope.users = [];
              }

              for (var i = 0; i < scope.users.length; i++) {
                var usr = scope.users[i];
                for (var j = 0; j < scope.userList.length; j++) {
                  if (scope.userList[j].username === usr) {
                    scope.addedUsers.push(scope.userList.splice(j, 1)[0]);
                  }
                }
              }

            });
          };

          /*
            Add user to component and calls the save function
          */
          scope.addUser = function(userIndex) {
            if (typeof scope.users === "undefined") {
              scope.users = [];
            }

            var user = scope.userList[userIndex];

            var index = userIndex;
            if (index > -1) {
                scope.addedUsers.push(scope.userList.splice(index, 1)[0]);
            }

            scope.users.push(user.username);
            if(scope.savefunction) {
              scope.savefunction(scope.users);
            }
          };

          /*
            Removes user from the user list and re-adds him to the available users list
          */
          scope.removeUser = function(user) {
            console.log("Removing useraccess " + user);

            var removeUser = scope.users.indexOf(user);
            scope.users.splice(removeUser, 1);

            for (var i = 0; i < scope.addedUsers.length; i++) {
              console.log(scope.addedUsers[i]);
              if (scope.addedUsers[i].username == user) {
                var u = scope.addedUsers.splice(i, 1)[0];
                scope.userList.push(u);
                break;
              }
            }

            console.log(scope.savefunction);
            console.log(typeof savefunction);
            if(scope.savefunction) {
              scope.savefunction(scope.users);
            }

          };
          scope.loadUserList();
        }
    };
}]);
