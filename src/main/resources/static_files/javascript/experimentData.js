var app = angular.module('experimentAdmin', ['ui.bootstrap']);

app.config(function($interpolateProvider){
    $interpolateProvider.startSymbol('[([').endSymbol('])]');
});

app.config(['$compileProvider', function ($compileProvider) {
    $compileProvider.aHrefSanitizationWhitelist(/^\s*(|blob|mp4|https|http):/);
}]);


/**
 * Generate buffer from excel workbook
 * @param  {[type]} s [description]
 * @return {[type]}   [description]
 */
function s2ab(s) {
  var buf = new ArrayBuffer(s.length);
  var view = new Uint8Array(buf);
  for (var i=0; i!=s.length; ++i) view[i] = s.charCodeAt(i) & 0xFF;
  return buf;
}

app.controller('experimentDataFilterController', function($scope, $http, $location, $window) {
  var baseUrl = $location.absUrl();
  $scope.components = [];
  $scope.testComponents = [];
  $scope.format = 'yyyy/MM/dd';

  $scope.startdate = 0;
  $scope.enddate = 0;

  $scope.dataLimit = 1000;
  $scope.warning = false;

  $scope.open = function($event) {
      $event.preventDefault();
      $event.stopPropagation();

      $scope.opened = true;
    };

    $scope.dateOptions = {
      formatYear: 'yy',
      startingDay: 1
   };

  $scope.jsonData = {};

  $http.get(baseUrl+"/json").success(function(data) {
    var comps = [];
    for (var i = 0; i < data.components.length; i++) {
      comps.push(i+1);
      if (data.components[i].type === "test") {
        $scope.testComponents.push(i+1);
      }
    }

    $scope.components = comps;
    $scope.f3Components = $scope.getF3Components();
  });

  $scope.getComponents = function() {
    return [1,2,3,4,5,6,7,8];
  };

  $scope.getF3Components = function() {
    return ["test", "form", "all"].concat($scope.components);
  };

  $scope.buildQuery = function() {
    var base = baseUrl + "/loaddata?";
    var query = base;

    if ($scope.filter1 === "single" || $scope.filter1 === "raw") {
      $scope.filter4 = undefined;

      if($scope.filter2 === "confirmed") {
        $scope.filter3 = undefined;
      }
    }

    query += "f1=" + ($scope.filter1 ? $scope.filter1 : "") + "&";
    query += "f2=" + ($scope.filter2 ? $scope.filter2 : "") + "&";
    query += "f3=" + ($scope.filter3 ? $scope.filter3 : "") + "&";
    query += "f4=" + ($scope.filter4 ? $scope.filter4 : "") + "&";

    var sDate = new Date($scope.startdate).toISOString();
    var eDate = new Date($scope.enddate).toISOString();

    query += "startdate=" + ($scope.startdate ?  sDate: "") + "&";
    query += "enddate=" + ($scope.enddate ?  eDate: "") + "&";

    $scope.warning = false;

    $http.get(query).success(function(data, status) {
      var anchor = angular.element( document.querySelector( '#dlLink' ) );

      if ($scope.fileUrl) {
        $window.URL.revokeObjectURL($scope.fileUrl);
      }
     /*  anchor.attr({
           href: 'data:attachment/csv;charset=utf-8,' + encodeURI(data),
           target: '_blank',
           download: 'data.csv'
       })*/
       //[0].click();

      CSV.RELAXED = true;
      CSV.COLUMN_SEPARATOR = ";";

      data = data.replace(/questionnaire-id:/g, "");

      var jsonData = CSV.parse(data);
      if (jsonData.length > 1000) {
        $scope.warning = true;
      }
      $scope.datarows = jsonData;
      $scope.downloadData = true;


      var excelWb = XLSX.utils.book_new();
      XLSX.utils.book_append_sheet(excelWb, XLSX.utils.json_to_sheet(jsonData), "Data");

      var wbout = XLSX.write(excelWb, {bookType:'xlsx', type:'binary'});

      var excelBolb =new Blob([s2ab(wbout)],{type:"application/octet-stream"});
      $scope.excelUrl = $window.URL || $window.webkitURL;
      $scope.fileUrlExcel = $scope.excelUrl.createObjectURL(excelBolb);

      var blob=new Blob([data]);
      $scope.url = $window.URL || $window.webkitURL;
      $scope.fileUrl = $scope.url.createObjectURL(blob);
    });
  };
});