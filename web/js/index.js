window.addEventListener("DOMContentLoaded", function () {
    var geval = eval;

    var repl = new CodeMirrorREPL("repl", {
        mode: "javascript",
        theme: "eclipse"
    });

    repl.print("Welcome to Construct.\nConstruct is written in Scala, and the web version is experimental.\nSend bug reports to aozdemir@hmc.edu");

    window.print = function (message) {
        repl.print(message, "message");
    };

    var scalabit = ConstructWebGREPL(function(s) {
        repl.print(s)
    });

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

    
}, false);
