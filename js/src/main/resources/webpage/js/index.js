var repl = null;
var libEdit = null;
window.addEventListener("DOMContentLoaded", function () {
  var geval = eval;

  //var lib_editor = CodeMirror.fromTextArea(document.getElementById("lib-editor"), {
  //  lineNumbers: true,
  //});

  /* Example definition of a simple mode that understands a subset of
   * JavaScript:
   */

  CodeMirror.defineSimpleMode("construct", {
    start: [
    {regex: /let|construction|shape|return/, token: "keyword"},
    {regex: /line|circle|ray|segment|intersection/, token: "atom"},
    {regex: /[-]/, token: "operator"},
    ],
  });

  repl = new CodeMirror2REPL("repl", {
    mode: 'construct',
    theme: "eclipse"
  });



  repl.print("Welcome to Construct. Type `:h` for help.");

  window.print = function (message) {
    repl.print(message, "message");
  };
  libEdit = CodeMirror.fromTextArea(document.getElementById('lib-editor'),{
    mode: 'construct',
    lineNumbers: true,
  });

  function print(s) {
    repl.print(s);
  }

  function write(s) {
    libEdit.setValue(libEdit.getValue() + s);
  }

  function read(s) {
    return libEdit.getValue();
  }

  var scalabit = ConstructWebGREPL(print, write, read);

  repl.eval = function (code) {
    scalabit.handleLine(code)
  };

  var c = document.getElementById('drawing');
  c.addEventListener("click", function(mouse_event) {
    var mouse_x = mouse_event.x;
    var mouse_y = mouse_event.y;
    var e_x = c.offsetLeft;
    var e_y = c.offsetTop;
    scalabit.handleClick(mouse_x - e_x, mouse_y - e_y);
  })

  document.getElementById('save-lib').addEventListener('click', function(event) {
    var file = new Blob([libEdit.getValue()], {type: 'txt'});
    const defaultfilename = "construction.con"
    if (window.navigator.msSaveOrOpenBlob)
        // IE10+
        window.navigator.msSaveOrOpenBlob(file, defaultfilename);
    else {
        // Other browers
        var url = URL.createObjectURL(file);

        var a = document.createElement("a");
        a.href = url;
        a.download = defaultfilename;
        document.body.appendChild(a);

        a.click();
        setTimeout(function() {
            document.body.removeChild(a);
            window.URL.revokeObjectURL(url);
        }, 0);
    }
  });

  window.onbeforeunload = function(){
    return "Are you sure you want to leave? Ay constructions will be lost.";
  };

  
}, false);

// Note: this function destroys what's in the current input box
function loadSession(file) {
  var fr = new FileReader();
  fr.onload = function(e) {
    libEdit.setValue(e.target.result);
  };
  fr.readAsText(file);
}

