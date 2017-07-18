import React from 'react';
import ReactDOM from 'react-dom';
import { Game } from './fbreactexample.jsx';
import { Login, Register } from './login.jsx';
import { HCenter } from './layout.jsx';
import { HashRouter as Router, Route, Switch, Link, IndexRoute } from 'react-router-dom'
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
    this.setState({userData:AppStore.getUserData()});
  }

  renderLoggedInApp() {
    const style = { margin: "0 auto", width: "100%"};
    return (
      <div style={style}>
        <h1>Boah ey !</h1>
        <div>You are {this.state.userData.userName}</div>
        <ul>
          <li><Link to="/">Home</Link></li>
          <li><Link to="/other">Misc</Link></li>
          <li><Link to="/game">Game</Link></li>
        </ul>
        <hr/>
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
    const loggedIn = this.state && this.state.userData;
      return (
        <Router history={createBrowserHistory()}>
          {loggedIn ? this.renderLoggedInApp() : this.renderLogin() }
        </Router>
      );
  }
}

ReactDOM.render(<App/>,document.getElementById("root"));
