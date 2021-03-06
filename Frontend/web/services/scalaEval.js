app.factory("scalaEval",["$http", function($http) {
	return {
		"insight": function(code){
			return $http.post("/eval", {"code": code});
		},
		"autocomplete": function(code, position){
			return $http.post("/completion", {"code": code, "position": position});
		},
		"typeAt": function(code, position){
			return $http.post("/typeAt", {"code": code, "position": position});
		}
	};
}]);
