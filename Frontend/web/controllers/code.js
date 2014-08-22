CodeMirror.hack = {};
app.controller('code',["$scope", "$timeout", "LANGUAGE", "VERSION", "scalaEval", "katas", "insightRenderer", "errorsRenderer", "wrap",
				 function code( $scope ,  $timeout ,  LANGUAGE ,  VERSION ,  scalaEval ,  katas,   insightRenderer ,  errorsRenderer ,  wrap){

	var cmCode,
			cmPrelude,
			state = {},
			ctrl = CodeMirror.keyMap["default"] == CodeMirror.keyMap.pcDefault ? "Ctrl-" : "Cmd-";

	state.configEditing = false;

	// if(angular.isDefined(window.localStorage['codemirror_' + VERSION])) {
	// 	$scope.cmOptions = JSON.parse(window.localStorage['codemirror_' + VERSION]);
	// } else {

		var keys = {}
		keys[ctrl + "Space"] = "autocomplete";
		keys['.'] = "autocompleteDot";
		keys[ctrl + "Enter"] = "run";
		keys[ctrl + ","] = "config";
		keys[ctrl + "."] = "typeAt";
		keys["F11"] = "fullscreen";
		keys[ctrl + "Up"] = "focusPrelude";
		keys[ctrl + "Down"] = "focusCode";

		$scope.cmOptions = {
			"_to config codemirror see_": "http://codemirror.net/doc/manual.html#config",
			extraKeys: keys,
			coverGutterNextToScrollbar: true,
			firstLineNumber: 0,
			lineNumbers: true,
			theme: 'solarized dark',
			"_themes": [ "solarized dark", "solarized light", "monokai", "ambiance", "eclipse", "mdn-like"],
			smartIndent: false,
			multiLineStrings: true,
			matchTags: {bothTags: true},
			autoCloseBrackets: true,
			styleActiveLine: false,
			keyMap: "sublime",
			mode: 'text/x-' + LANGUAGE,
			highlightSelectionMatches: { showToken: false }
		}

	// }

	function clear(){
		insightRenderer.clear();
		errorsRenderer.clear();
	}

	function setMode(edit){
		function refresh(cm_, next){
			var bar, parent = cm_.display.wrapper.parentElement;
			if(next) bar = parent.nextElementSibling;
			else bar = parent.previousElementSibling;

			angular.element(bar).on('mousemove', function(){
				cm_.refresh();
			})
		}
		if(edit) {
			state.code = $scope.code;
			clear();
			$timeout(function(){
				$scope.cmOptions.mode = 'application/json';
				$scope.code = JSON.stringify($scope.cmOptions, null, '\t');
			});
		} else {
			$scope.cmOptions.onLoad = function(cm_) {
				refresh(cm_, false);

				cmCode = cm_;
				CodeMirror.hack.code = cm_;
				cmCode.focus();
				cmCode.on('changes', function(){
					clear();
				});
				cmCode.on('dblclick', function(){
					clear();
				});
			};

			$scope.cmOptions.mode = 'text/x-' + LANGUAGE;
			$scope.cmOptionsPrelude = angular.copy($scope.cmOptions);
			$scope.cmOptionsPrelude.onLoad = function(cm_){
				refresh(cm_, true);

				cmPrelude = cm_;
				CodeMirror.hack.prelude = cm_;
			};

			window.localStorage['codemirror_' + VERSION] = JSON.stringify($scope.cmOptions);

			$timeout(function(){
				$scope.code = state.code;
				run();
			});
		}
	}

	$scope.toogleEdit = function(){
		state.configEditing = !state.configEditing;
		setMode(state.configEditing);
	};

	CodeMirror.hack.wrap = wrap;

	function run(){
		if(state.configEditing) return;
		if(!angular.isDefined($scope.prelude) && !angular.isDefined($scope.code)) return;

		var w = wrap($scope.prelude, $scope.code);
		scalaEval.insight(w.full).then(function(r){
			var data = r.data;
			var code = $scope.code.split("\n");
			insightRenderer.render(cmCode, w, $scope.cmOptions.mode, data.insight, code);
			errorsRenderer.render(cmCode, cmPrelude, w, data.infos, data.runtimeError, code);
		});
	}

	CodeMirror.commands.run = run;
	CodeMirror.commands.save = run;
	CodeMirror.commands.config = $scope.toogleEdit;

	CodeMirror.commands.fullscreen = function(){
		if(screenfull.enabled) {
	    screenfull.toggle();
		}
	}
	CodeMirror.commands.focusPrelude = function(){
		cmPrelude.focus();
	};
	CodeMirror.commands.focusCode = function(){
		cmCode.focus();
	}

	$scope.theme = function(){
		return cmCode.options.theme.split(" ").map(function(v){
			return "cm-s-" + v;
		}).join(" ");
	}

	$scope.$watch('prelude', function(){
		clear();
		window.localStorage['prelude'] = $scope.prelude;
	});
	$scope.$watch('code', function(){
		if(state.configEditing) {
			try {
				$scope.cmOptions = JSON.parse($scope.code);
				$scope.cmOptionsPrelude = angular.copy($scope.cmOptions);
			} catch(e){}
		} else {
			clear();
			window.localStorage['code'] = $scope.code;
		}
	});

	$scope.run = run;

	if(window.location.pathname !== "/") {
		setMode(false);
		katas(window.location.pathname).then(function(r){
			var res = wrap("","").split(r.data);
			$scope.prelude = res[0];
			state.code = res[1];
			setMode(false);
		});
	} else {
		if(angular.isDefined(window.localStorage['code'])){
			state.code = window.localStorage['code'];
		}
		if(angular.isDefined(window.localStorage['prelude'])){
			$scope.prelude = window.localStorage['prelude'];
		}
		setMode(false);
	}
}]);
