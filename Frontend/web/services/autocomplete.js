app.run(["scalaEval", function(scalaEval){
	// remove select line
	delete CodeMirror.keyMap.sublime["Ctrl-L"];

	function hint(cm, sf, cf, single){
		var offset;
		var wrap = CodeMirror.hack.wrap(
			CodeMirror.hack.prelude.getDoc().getValue(),
			CodeMirror.hack.code.getDoc().getValue()
		);

		if(cm == CodeMirror.hack.prelude) offset = 0;
		else offset = wrap.codeOffset();

		sf(
			wrap.full,
			cm.getDoc().indexFromPos(cm.getCursor()) + offset
		).then(function(r){
			var data = r.data;

			CodeMirror.showHint(cm, function(cm, options){
				var i;
				var cur= cm.getCursor();
				var curTo = {"ch" : cur.ch, "line" : cur.line};
				var curFrom = {"ch" : cur.ch, "line" : cur.line};

				var currentLine = cm.getDoc().getValue().split("\n")[cur.line];

				function delimiter(c){
					return  /^[a-zA-Z0-9\_]$/.test(c);
				}

				for (i = cur.ch-1; i >= 0 && delimiter(currentLine[i]); i--){
					curFrom.ch = i;
				}
				for (i = cur.ch; i < currentLine.length && delimiter(currentLine[i]); i++){
					curTo.ch = i+1;
				}

				var term = currentLine.substr(curFrom.ch, curTo.ch - curFrom.ch);

				options.completeSingle = single;

				if(single){
       		return {from: curFrom, to: curTo, list: cf(data, term)};
				} else {
       		curFrom.ch = Math.Infinity;
       		curTo.ch = Math.Infinity;
       		return {from: curFrom, to: curTo, list: cf(data)};
				}

			});
		});
	}

	CodeMirror.commands.typeAt = function(cm) {
		hint(cm, scalaEval.typeAt, function(data){
			return [
				{
					className: "typeAt",
					text: " // " + data.tpe,
					render: function(el, _, _1){
						var elem = document.createElement("pre");
						elem.innerText = data.tpe;
						el.appendChild(elem);
					}
				}
			];
		}, false);
	}
	CodeMirror.commands.autocomplete = function(cm) {
		hint(cm, scalaEval.autocomplete, function(data, term){
			return data.filter(function(c){
					return c.name.toLowerCase().indexOf(term.toLowerCase()) != -1;
			}).map(function(c){
				return {
					className: "autocomplete",
					text: c.name,
					completion: c,
					alignWithWord: true,
					render: function(el, _, _1){
						el.innerHTML = "<span class=\"autocomplete-result-name\">" + c.name + "</span> <span class=\"autocomplete-result-signature\">" + c.signature +"</span>";
					}
				}
			});
		}, true);
	};
	CodeMirror.commands.autocompleteDot = function (cm){
		cm.replaceSelection(".");
		cm.execCommand("autocomplete");
	};
}]);
