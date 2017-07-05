import React from 'react';
import {Table,Tr,Td} from './tablelayout.jsx';


class Btn extends React.Component {
  render() {
    const style= { display: 'inline-block', padding: "4px", background: "#afa", border: 'solid 1px #aaa', cursor: "pointer", float: 'right' };
    return <div style={style} onClick={this.props.onClick}>
      {this.props.children}
    </div>
  }
}

export class Login extends React.Component {

  constructor(props) {
    super(props);
    this.state = {
      user: '', pwd: ''
    }
  }
  handleUChange(event) {
    this.setState({user: event.target.value});
  }
  handlePChange(event) {
    this.setState({pwd: event.target.value});
  }
  handleLogin(event) {
    console.log("hello", event);
  }
  render() {
    return <Table>
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
  }

}