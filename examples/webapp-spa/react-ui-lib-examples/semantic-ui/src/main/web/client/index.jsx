import React from 'react'
import {Component}  from 'react'
import ReactDOM from 'react-dom';
import {HCenter,Fader} from './subtest/util';
import {Greeter} from './subtest/greeter';
import global from "./global"
import {
  SemanticPlay as MySemanticPlay, DummyFun, DummyFun3, BratkartoffelnWärenLecker,
  curry, currySameModule
} from './semantic/semanticplay';
import {Form,Modal} from 'semantic-ui-react';
import {KClientListener,KClient} from 'kontraktor-client';


const curryOtherModule = curry('curryOtherModule');

class App extends Component {

  constructor(props) {
    super(props);
    this.state = {
      user: '',
      loginEnabled: false,
      loggedIn: false,
      error: null,
      expired: false
    };
    const self = this;
    global.kclient.listener = new class extends KClientListener {
      onInvalidResponse(resp) {
        // session timeout (=401 or 'unknown actor')
        self.setState({expired: true});
      }
    };
  }

  reload() {
    document.location.href = "/";
  }

  handleUChange(ev) {
    this.setState( {user: ev.target.value}, () => this.validate() );
  }

  validate() {
    this.setState({
      loginEnabled: this.state.user.trim().length > 0
    });
  }

  login() {
    global.kclient
      .connect("/api")
        .then( (server,err) => {
          if ( err )
            this.setState( {error: ""+err} );
          else {
            global.server = server;
            server.login( this.state.user )
              .then( (session,err) => {
                if ( err )
                  this.setState( {error: ""+err} );
                else {
                  global.session = session;
                  console.log("logged in");
                  this.setState({loggedIn:true});
                }
              })
          }
      });
  }

  componentWillUpdate(nextProps,nextState) {
    if ( !this.state.loggedIn && nextState.loggedIn ) {
      console.log("will be logged in ..")
    }
  }

  testCall() {
    return <p>Testcase requires kontraktor-http >= 4.18.2</p>;
  }

  render() {
    const p = { a: 1, b: 2, c: 3 };
    const xx = { a: 77, ...p, c: 66 }; // test
    const curryLocal = curry("curry local");
    return (
      <div>
        <HCenter  { ...p , ...xx, c: 99 }></HCenter>
        <Modal
          open={this.state.expired}
          header='Session expired'
          content='you need to relogin'
          actions={[
            { key: 'ok', content: 'Ok', color: 'green' },
          ]}
          onClose={()=>this.reload()}
        />
        <HCenter>
          <div style={{fontWeight: 'bold', fontSize: 18}} >
            Hello World !
          </div>
        </HCenter>
        <br/>
        {/*{ this.state.loggedIn && this.testCall()}*/}
        { curryLocal() }
        { curryOtherModule() }
        { currySameModule() }
        { this.state.loggedIn && this.testCall()}
        { this.state.loggedIn ?
          <Fader><Greeter/></Fader>
          : (
            <Fader>
              <HCenter>
                <Form>
                  <Form.Input label='Sign In' placeholder='some name' onChange={ ev => this.handleUChange(ev) } />
                  <HCenter>
                    <Form.Button
                      disabled={!this.state.loginEnabled}
                      onClick={ ev => this.login(ev) }>
                      Login
                    </Form.Button>
                  </HCenter>
                </Form>
              </HCenter>
              <br/>
            </Fader>
          )
        }
        <div>
          {this.state.error ? <div><b>error</b></div> : ""}
        </div>
        <p>{DummyFun(1,2)}</p>
        <p>{DummyFun3(1)}</p>
        <p>{BratkartoffelnWärenLecker.text}</p>
        <div>
          <MySemanticPlay/>
        </div>
      </div>
    )}


}

if ( typeof _kHMR === 'undefined') { // indicates not running in a hot reload cycle
  global.app = <App/>; // avoid replacment of whole app
  // set app root for hot reloading
  window._kreactapprender = ReactDOM.render(global.app, document.getElementById("root"));

}

// for kontraktor internal development

if (typeof _kHMR === 'undefined') {
  if (typeof KClient === 'undefined') {
    console.error("hot module reloading requires 'import {KClient} from 'kontraktor-client''");
  }
  const hmrcl = new KClient().useProxies(false);
  let addr = "ws://" + window.location.host + "/hotreloading";

  window._kredefineModule = function(patch, prev, libname) {

    let impPatch = '';
    Object.getOwnPropertyNames(patch.__modimports).forEach(key=>{
        if (!prev.__initial_modimports[key]) {
          prev.__modimports[key] = patch.__modimports[key];
          impPatch += '\nvar ' + key + '= __modimports.' + key + ';';
          console.log("new import detected:", key);

        }
      }
    );
    Object.getOwnPropertyNames(patch).forEach(topleveldef=>{
        try {
          const istop = "__kdefault__" !== topleveldef && prev['__kdefault__'] === prev[topleveldef];
          if ("__kdefault__" === topleveldef) {// ignore
          } else if (!prev[topleveldef]) {
            prev[topleveldef] = patch[topleveldef];
            // new definition, FIXME: not locally visible, unsupported for now
            console.log('new definition detected', topleveldef);
          } else if (patch[topleveldef]._kNoHMR) {// unmarked for HMR
          } else if (typeof patch[topleveldef] === 'function') {
            let src = patch[topleveldef].toString();
            const isclass = src.indexOf("class") == 0;
            const isfun = src.indexOf("function") == 0;
            if (isfun || (!isclass)) // assume function or lambda
            {
              if (patch[topleveldef]._kwrapped && prev[topleveldef]._kwrapped) {
                let funsrc = patch[topleveldef]._kwrapped.toString();
                let evalSrc = impPatch + ";" + "" + topleveldef + " = " + funsrc + ";" + topleveldef;
                const newfun = __keval[libname](evalSrc);
                prev[topleveldef]._kwrapped = newfun;
              }
            } else if (isclass) {
              const newName = topleveldef;
              const newDef = __keval[libname](impPatch + ";" + newName + "=" + src + "; " + newName);
              Object.getOwnPropertyNames(newDef.prototype).forEach(key=>{
                  prev[topleveldef].prototype[key] = newDef.prototype[key];
                }
              );
            } else {
              // should not happen
              console.error("unknown function object", src);
            }
          } else {
            if (typeof patch[topleveldef] === 'object')
              Object.assign(prev[topleveldef], patch[topleveldef]);
            else {
              console.log('(possible hot rel failure) direct assignment on redefine:' + topleveldef + ',' + (typeof patch[topleveldef]), patch[topleveldef]);
              prev[topleveldef] = patch[topleveldef];
            }
          }
          if (istop)
            prev['__kdefault__'] = prev[topleveldef];
        } catch (e) {
          if (!(e instanceof TypeError))
            console.log(e);
        }
      }
    );
    window._kreactapprender.forceUpdate();
  }
  ;
  // subscribe to filewatcher
  hmrcl.connect(addr, "WS").then((conn,err)=>{
      if (err) {
        console.error("failed to connect to hot reloading actor on '" + addr + "'. Hot reloading won't work.");
        console.error('add to server builder:".hmrServer(true)"\n');
        return;
      }
      conn.ask("addListener", (libname,e)=>{
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
            fetch("_appsrc/" + libname + ".transpiled").then(response=>response.text()).then(text=>{
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
      ).then((r,e)=>{
          if (r)
            console.log('connected to hmr server');
          else
            console.log('could not subscribe to hmr server');
        }
      );
    }
  );

  // initially redefine all libs to avoid state loss on first redefine
  console.log("init hot reloading ..");
  Object.getOwnPropertyNames(kimports).forEach(prop=>{
      window._kredefineModule(kimports[prop], kimports[prop], prop);
    }
  );
  console.log("... done init hot reloading");
}
