import React from 'react'
import {Component}  from 'react'
import ReactDOM from 'react-dom';
import {HCenter,Fader} from './subtest/util';
import {Greeter} from './subtest/greeter';
import global from "./global"
import {SemanticPlay as MySemanticPlay,DummyFun,DummyFun3,BratkartoffelnWärenLecker} from './semantic/semanticplay';
import {Form,Modal} from 'semantic-ui-react';
import {KClientListener,KClient} from 'kontraktor-client';

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
