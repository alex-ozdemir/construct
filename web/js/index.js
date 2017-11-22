window.addEventListener("DOMContentLoaded", function () {
    var geval = eval;

    //var lib_editor = CodeMirror.fromTextArea(document.getElementById("lib-editor"), {
    //    lineNumbers: true,
    //});

    /* Example definition of a simple mode that understands a subset of
     * JavaScript:
     */

    CodeMirror.defineSimpleMode("simplemode", {
      // The start state contains the rules that are intially used
      start: [
        // The regex matches the token, the token property contains the type
        {regex: /"(?:[^\\]|\\.)*?(?:"|$)/, token: "string"},
        // You can match multiple tokens at once. Note that the captured
        // groups must span the whole string in this case
        {regex: /(function)(\s+)([a-z$][\w$]*)/,
         token: ["keyword", null, "variable-2"]},
        // Rules are matched in the order in which they appear, so there is
        // no ambiguity between this one and the one above
        {regex: /(?:function|var|return|if|for|while|else|do|this)\b/,
         token: "keyword"},
        {regex: /true|false|null|undefined/, token: "atom"},
        {regex: /0x[a-f\d]+|[-+]?(?:\.\d+|\d+\.?\d*)(?:e[-+]?\d+)?/i,
         token: "number"},
        {regex: /\/\/.*/, token: "comment"},
        {regex: /\/(?:[^\\]|\\.)*?\//, token: "variable-3"},
        // A next property will cause the mode to move to a different state
        {regex: /\/\*/, token: "comment", next: "comment"},
        {regex: /[-+\/*=<>!]+/, token: "operator"},
        // indent and dedent properties guide autoindentation
        {regex: /[\{\[\(]/, indent: true},
        {regex: /[\}\]\)]/, dedent: true},
        {regex: /[a-z$][\w$]*/, token: "variable"},
        // You can embed other modes with the mode property. This rule
        // causes all code between << and >> to be highlighted with the XML
        // mode.
        {regex: /<</, token: "meta", mode: {spec: "xml", end: />>/}}
      ],
      // The multi-line comment state.
      comment: [
        {regex: /.*?\*\//, token: "comment", next: "start"},
        {regex: /.*/, token: "comment"}
      ],
      // The meta property contains global information about the mode. It
      // can contain properties like lineComment, which are supported by
      // all modes, and also directives like dontIndentStates, which are
      // specific to simple modes.
      meta: {
        dontIndentStates: ["comment"],
        lineComment: "//"
      }
    });

    var repl = new CodeMirrorREPL("repl", {
        mode: "simplemode",
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
