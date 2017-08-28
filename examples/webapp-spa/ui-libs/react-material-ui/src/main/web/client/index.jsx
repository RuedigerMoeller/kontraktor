import React from 'react'
import {Component}  from 'react'
import ReactDOM from 'react-dom';
import MuiThemeProvider from 'material-ui/styles/MuiThemeProvider';
import {KClientListener} from 'kontraktor-client';
import {HCenter,Fader} from './util';
import Greeter from './greeter';
import global from "./global"
import MaterialPlay from './materialui/materialplay';
import TextField from 'material-ui/TextField';
import RaisedButton from 'material-ui/RaisedButton';
import Dialog from 'material-ui/Dialog';
import Static from './static';
import {RadioButtonGroup,RadioButton} from 'material-ui/RadioButton';
import Snackbar from 'material-ui/Snackbar';

class App extends Component {

  constructor(props) {
    super(props);
    this.state = {
      user: '',
      loginEnabled: false,
      loggedIn: false,
      relogin: false,
      connectionType: 'HTLP',
      error: null,
      snackText: "",
      snackOpen: false
    };
    const self = this;
    global.kclient.listener = new class extends KClientListener {
      // session timeout or resurrection fail
      onInvalidResponse(response) {
        console.error("invalid response",response);
        self.setState({relogin: true}); // session expired
      }
      onResurrection() {
        console.log("session resurrected. should update client data + resubscribe streams in case !")
        self.setState({snackText: "Session Resurrected !", snackOpen: true });
      }
    };
  }
  handleConnectionSelect(event, value) {
    this.setState( { connectionType:"http" == value ? "HTPL" : "WS" } )
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
    var url,connectionType;
    if ( this.state.connectionType == 'HTLP' ) {
      url = "/api";
      connectionType = "HTLP";
    } else {
      url = "ws://"+document.location.host+"/ws";
      connectionType = "WS";
    }
    global.kclient
      .connect(url,connectionType)
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
      <RaisedButton
        label="Ok"
        primary={true}
        onClick={ () => this.relogin() }
      />
    ];
    return (
      <MuiThemeProvider>
        <div>
          <Snackbar
            open={this.state.snackOpen}
            message={this.state.snackText}
            autoHideDuration={4000}
            onRequestClose={ () => this.setState({snackText: "", snackOpen: false }) }
          />
          <Dialog
            title="Session expired"
            actions={actions}
            modal={true}
            open={this.state.relogin}
            onRequestClose={() => this.relogin()}
          >
            Session timed out. Pls relogin.
          </Dialog>
          <br/><br/><br/>
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
                  <RadioButtonGroup name="connection" defaultSelected="http" onChange={(ev,val) => this.handleConnectionSelect(ev,val)}>
                    <RadioButton
                      value="http"
                      label="Http (adaptive long poll)"
                    />
                    <RadioButton
                      value="websockets"
                      label="WebSocket"
                    />
                  </RadioButtonGroup>
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
          { this.state.loggedIn ?
          <div>
            <MaterialPlay/>
          </div>
            : <Static/>
          }
        </div>
      </MuiThemeProvider>
  )}

}

global.app = <App/>;
ReactDOM.render(global.app,document.getElementById("root"));
