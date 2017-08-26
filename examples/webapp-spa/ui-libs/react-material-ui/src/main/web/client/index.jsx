import React from 'react'
import {Component}  from 'react'
import ReactDOM from 'react-dom';
import MuiThemeProvider from 'material-ui/styles/MuiThemeProvider';

import {HCenter,Fader} from './util';
import {Greeter} from './greeter';
import {global} from "./global"
import {MaterialPlay} from './materialui/materialplay';

class App extends Component {

  constructor(props) {
    super(props);
    this.state = {
      user: '',
      loginEnabled: false,
      loggedIn: false,
      error: null
    }
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

  render() {
    return (
      <MuiThemeProvider>
        <div>
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
                  <input type="text" value={this.state.user}
                         onChange={ ev => this.handleUChange(ev) }></input>
                </HCenter>
                <br/>
                <HCenter>
                  <button
                    disabled={!this.state.loginEnabled}
                    className='defbtn'
                    onClick={ ev => this.login(ev) }>
                    Login
                  </button>
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
