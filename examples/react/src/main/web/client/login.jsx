import React from 'react';
import {Table,Tr,Td} from './layout.jsx';
import AppActions from './actions.jsx';
import AppStore from './store.jsx';
import Link from "react-router-dom";

class Btn extends React.Component {
  render() {
    const style= { display: 'inline-block', float: 'right' };
    return (
      <div className="lgbtn" style={style} onClick={this.props.onClick}>
        {this.props.children}
      </div>
    )
  }
}

export class Register extends React.Component {

  render() {
    return (<div>Register</div>)
  }

}

export class Login extends React.Component {

  constructor(props) {
    super(props);
    this.state = {
      user: '', pwd: ''
    }
  }

  componentDidMount() {
    AppStore.addChangeListener('STORE_TRY_LOGIN', this.onTryLogin);
    AppStore.addChangeListener('STORE_LOGIN_CHANGED', this.onLoginChange);
  }

  onTryLogin() {
    console.log("try login");
  }

  onLoginChange(ev) {
    console.log(ev);
  }

  handleUChange(event) {
    this.setState({user: event.target.value});
  }

  handlePChange(event) {
    this.setState({pwd: event.target.value});
  }

  handleLogin(event) {
    console.log("hello", event);
    AppActions.login( this.state.user, this.state.pwd );
  }

  render() {
    return (
      <Table>
        <Tr>
          <Td>User:</Td><Td><input type="text" value={this.state.user} onChange={this.handleUChange.bind(this)}></input></Td>
        </Tr>
        <Tr>
          <Td>Pwd:</Td><Td><input type="password" value={this.state.pwd} onChange={this.handlePChange.bind(this)}></input></Td>
        </Tr>
        <Tr>
          <Td>&nbsp;</Td><Td><Btn onClick={this.handleLogin.bind(this)}>Login</Btn></Td>
        </Tr>
      </Table>
    )
  }

}