MathJax.Hub.Config({
    skipStartupTypeset: true,
    messageStyle: "none",
    "HTML-CSS": {
        showMathMenu: false
    }
});
MathJax.Hub.Configured();

app.factory('insightRenderer', function() {
	var widgets = [];

	function apply(cm, cmOptions, insight, code){
		var nl = "\n",
        elem,
			  start = cm.getDoc().posFromIndex(insight.start),
			  end = cm.getDoc().posFromIndex(insight.end),
        clearF;

    function addClass(two){
      angular.element(elem)
            .addClass("insight")
            .addClass(two);
    }

    function fold(){
      addClass("fold");
      var widget = cm.foldCode(start, {
        widget: elem,
        rangeFinder: function(){
          return {
            // from: start,
            from: {ch: 0, line: start.line},
            to: end
          };
        }
      });
      clearF = function(){};
    }

    function inline(){
      addClass("inline");
      var widget = cm.addLineWidget(end.line, elem);
      clearF = function(){ widget.clear() };
    }

		switch (insight.renderType) {
			case "html":
				elem = document.createElement("div");
				elem.innerHTML = insight.result;
        fold();
				break;
			case "latex":

        var $script = angular.element("<script type='math/tex'>")
            .html(insight.result);
        var $element = angular.element("<div>");

        $element.append($script);
        elem = $element[0];
        MathJax.Hub.Queue(["Reprocess", MathJax.Hub, elem]);
        fold();

				break;
			case "markdown":
				elem = document.createElement("div");
				elem.innerHTML = marked.parse(insight.result, {ghf: true});
        fold();
				break;
			case "string":
				elem = document.createElement("pre");
				elem.innerText = insight.result;
        inline();
				break;
			case "other":
				elem = document.createElement("span");
        elem.className = "code";
				CodeMirror.runMode(insight.result, cmOptions, elem);
        inline();
				break;
		}
    return clearF;
	}
	function clearFun(){
		widgets.forEach(function(w){
			w();
		});
		widgets = [];
	}
	return {
		clear: clearFun,
		render: function(cm, cmOptions, insights, code){
			widgets = insights.map(function(insight){
				return apply(cm, cmOptions, insight, code);
			});
		}
	}
});
