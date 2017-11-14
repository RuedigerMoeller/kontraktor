import React from 'react'
import {Component}  from 'react'
import ReactDOM from 'react-dom';
import {HCenter,Fader} from './subtest/util';
import {Greeter} from './subtest/greeter';
import global from "./global"
import {SemanticPlay as MySemanticPlay} from './semantic/semanticplay';
import {Form,Modal} from 'semantic-ui-react';
import {KClientListener} from 'kontraktor-client';

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
    return (
      <div>
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
          <div style={{fontWeight: 'bold', fontSize: 18}}>
            Hello World !
          </div>
        </HCenter>
        <br/>
        { this.state.loggedIn && this.testCall()}
        { this.state.loggedIn ?
          <Fader><Greeter/></Fader>
          : (
            <Fader>
              <HCenter>
                <Form>
                  <Form.Input label='Log In' placeholder='nick name' onChange={ ev => this.handleUChange(ev) } />
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
        <div>
          <MySemanticPlay/>
        </div>
      </div>
    )}


}

global.app = <App/>;

ReactDOM.render(global.app,document.getElementById("root"));
