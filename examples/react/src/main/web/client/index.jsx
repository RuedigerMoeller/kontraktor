import React from 'react';
import ReactDOM from 'react-dom';
import { Game } from './fbreactexample.jsx';
import { Login, Register } from './login.jsx';
import { HCenter } from './layout.jsx';
import { HashRouter as Router, Route, Switch, Link, NavLink } from 'react-router-dom'
import createBrowserHistory from 'history/createBrowserHistory';
import { Store as AppStore } from "./store.jsx";
import { UserTable } from "./usertable.jsx";

class OtherState extends React.Component {
  render() {
    return (<div>Some Other Component</div>)
  }
}

class App extends React.Component {

  constructor(props) {
    super(props);
    this.state={ userData: null }
  }

  componentDidMount() {
    AppStore.addChangeListener('login', this.onLoginChange.bind(this));
  }

  onLoginChange() {
    this.setState({userData: AppStore.getUserData()});
    this.forceUpdate();
  }

  renderLoggedInApp() {
    const style = { margin: "0 auto", width: "100%"};
    const actStyle = { fontWeight: 'bold', fontSize: 18, background: '#499bea', transition: "all .3s", margin: 1, padding: 6, _height: 25, width: 100 };
    const inactStyle = { transition: "all .3s", background: "#398bda", display: "inline-block", margin: 1, padding: 6, color: "white", _height: 20, width: 100 };
    return (
      <div style={style}>
        <NavLink style={inactStyle} exact={true} to="/" activeStyle={actStyle}>Home</NavLink>
        <NavLink style={inactStyle} to="/other" activeStyle={actStyle}>Misc</NavLink>
        <NavLink style={inactStyle} to="/game" activeStyle={actStyle}>Game</NavLink>
        <span style={{float: "right", margin: 4, color: 'white'}}>&nbsp;You are <b>'{this.state.userData.userKey}'</b></span>
        <br/><br/><br/><br/>
        <HCenter>
          <Switch>
            <Route exact path="/" component={UserTable}/>
            <Route path="/other" component={OtherState}/>
            <Route path="/game" component={Game}/>
          </Switch>
        </HCenter>
      </div>
    )
  }

  renderLogin() {
    return (
      <HCenter>
        <Switch>
          <Route exact path="/" component={Login}/>
          <Route path="/register" component={Register}/>
        </Switch>
      </HCenter>
    )
  }

  render() {
    const loggedIn = AppStore.isLoggedIn();
    return (
      <HCenter style={{ height: "100%" }} >
        <div style={{maxWidth: 1000, width: '100%', height: "100%" }} className="gradientbg">
          <Router history={createBrowserHistory()}>
            {loggedIn ? this.renderLoggedInApp() : this.renderLogin() }
          </Router>
        </div>
      </HCenter>
    );
  }
}

ReactDOM.render(<App/>,document.getElementById("root"));