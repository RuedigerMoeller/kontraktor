import React from 'react'
import {Component}  from 'react'
import ReactDOM from 'react-dom';
import MuiThemeProvider from 'material-ui/styles/MuiThemeProvider';

import {HCenter,Fader} from './util';
import Greeter from './greeter';
import global from "./global"
import MaterialPlay from './materialui/materialplay';
import TextField from 'material-ui/TextField';
import RaisedButton from 'material-ui/RaisedButton';

class App extends Component {

  constructor(props) {
    super(props);
    this.state = {
      user: '',
      loginEnabled: false,
      loggedIn: false,
      relogin: false,
      error: null
    };
    const self = this;
    global.kclient.listener = new class extends KClientListener {
      // session timeout or resurrection fail
      onInvalidResponse(response) {
        console.error("invalid response");
        self.setState({relogin: true});
      }
      onError(obj) {
        console.error("connectionError",obj)
      }
      onClosed() {
        console.warn("connection closed")
      }
      onResurrection() {
        console.log("session resurrected")
      }
    };
  }

  handleUChange(ev) {
    this.setState( {user: ev.target.value}, () => this.validate() );
  }

  validate() {
    this.setState({
      loginEnabled: this.state.user.trim().length > 0
    });
  }

  relogin() {
    // forcereload
    document.location.href = "/";
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

  render() {
    const actions = [
      <FlatButton
        label="Cancel"
        primary={true}
        onClick={this.handleClose}
      />
    ];
    return (
      <MuiThemeProvider>
        <div>
          <Dialog
            title="Session expired"
            actions={actions}
            modal={true}
            open={this.state.relogin}
            onRequestClose={() => this.relogin()}
          >
            Session timed out. Pls relogin.
          </Dialog>

          <HCenter>
            <div style={{fontWeight: 'bold', fontSize: 18}}>
              Hello World !
            </div>
          </HCenter>
          <br/>
          { this.state.loggedIn ?
            <Fader><Greeter/></Fader>
            : (
              <Fader>
                <HCenter>
                  <TextField
                    onChange={ ev => this.handleUChange(ev) }
                    hintText="nickname"
                    floatingLabelText="Login"
                  />
                </HCenter>
                <br/>
                <HCenter>
                  <RaisedButton
                    disabled={!this.state.loginEnabled}
                    onClick={ ev => this.login(ev) }>
                    Login
                  </RaisedButton>
                </HCenter>
              </Fader>
            )
          }
          <div>
            {this.state.error ? <div><b>error</b></div> : ""}
          </div>
          <div>
            <MaterialPlay/>
          </div>
        </div>
      </MuiThemeProvider>
  )}

}

global.app = <App/>;

ReactDOM.render(global.app,document.getElementById("root"));
