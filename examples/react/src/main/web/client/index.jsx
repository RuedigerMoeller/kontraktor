import React from 'react';
import ReactDOM from 'react-dom';
import { Game } from './fbreactexample.jsx';
import { Login } from './login.jsx';
import { BrowserRouter as Router, Route, Switch, Link, IndexRoute } from 'react-router-dom'
import createBrowserHistory from 'history/createBrowserHistory';
import AppStore from "./store.jsx";

class App extends React.Component {

  constructor(props) {
    super(props);
    this.state={ userData: null }
  }

  componentDidMount() {
    AppStore.addChangeListener('STORE_LOGIN_CHANGED', this.onLoginChange.bind(this));
  }
  onLoginChange() {
    this.setState({userData:AppStore.getUserData()});
  }
  render() {
    const style = { margin: "0 auto", width: "100%"};
    const loggedIn = this.state && this.state.userData;
      return (
        <Router history={createBrowserHistory()}>
          {loggedIn ? (
            <div style={style}>
              <h1>Boah ey !</h1>
              <div>You are {this.state.userData.userName}</div>
              <ul>
                <li><Link to="/">Home</Link></li>
                <li><Link to="/login">Login</Link></li>
              </ul>
              <hr/>
              <Switch>
                <Route exact path="/" component={Game}/>
                <Route path="/login" component={Login}/>
              </Switch>
            </div>
          )
            : <Login/>
          }
        </Router>
      );
  }
}

ReactDOM.render(<App/>,document.getElementById("root"));
// ReactDOM.render(<div><Game /><br/><Login /></div>, document.getElementById("root"));
