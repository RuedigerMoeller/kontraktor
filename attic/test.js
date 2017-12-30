const hmrcl = new KClient().useProxies(false);
let addr = "ws://" + window.location.host + "/hotreloading";
// subscribe to filewatcher
hmrcl.connect(addr, "WS").then((conn, err) => {
  if (err) {
    console.error("failed to connect to hot reloading actor on '" + addr + "'. Hot reloading won't work.");
    console.error('add to server builder:".hmrServer(true)"\n');
    return;
  }
  conn.ask("addListener", (libname, e) => {
      console.log("a file has changed _appsrc/" + libname);
      if (!window._kreactapprender) {
        console.error("hot module reloading requires window._kreactapprender to be set to rect root. E.g. 'window._kreactapprender = ReactDOM.render(global.app,document.getElementById(\"root\"));' ");
        return;
      }
      if (!libname) {
        console.error("failed to init hot reloading actor on '" + addr + "'. Hot reloading won't work.");
        console.error('add to server builder:".hmrServer(true)"\n');
      }
      const lib = kimports[libname];
      if (lib) {
        // fetch new source and patch
        fetch("_appsrc/" + libname + ".transpiled").then(response => response.text()).then(text => {
            const prev = kimports[libname];
            const prevEval = __keval[libname];
            const exp = eval("let _kHMR=true;" + text.toString());
            const patch = kimports[libname];
            kimports[libname] = prev;
            __keval[libname] = prevEval;
            window._kredefineModule(patch, prev, libname);
          }
        );
      }
    }
  ).then((r, e) => {
      if (r)
        console.log('connected to hmr server');
      else
        console.log('could not subscribe to hmr server');
    }
  );
  }
);
