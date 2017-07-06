import React from 'react';
import ReactDOM from 'react-dom';
import { Game } from './fbreactexample.jsx';
import { Login } from './login.jsx';
import { BrowserRouter as Router, Route, Switch, Link, IndexRoute } from 'react-router-dom'
import createBrowserHistory from 'history/createBrowserHistory';

class App extends React.Component {
  render() {
    const style = { margin: "0 auto", width: "100%"};
    return (
      <Router history={createBrowserHistory()}>
        <div style={style}>
          <h1>Boah ey !</h1>
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
      </Router>
    )
  }
}

ReactDOM.render(<App/>,document.getElementById("root"));
// ReactDOM.render(<div><Game /><br/><Login /></div>, document.getElementById("root"));
