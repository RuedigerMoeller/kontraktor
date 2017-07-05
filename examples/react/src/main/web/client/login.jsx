import React from 'react';
import {Table,Tr,Td} from './tablelayout.jsx';

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
  render() {
    return <Table>
        <Tr>
          <Td>User:</Td><Td><input type="text" value={this.state.user} onChange={this.handleUChange.bind(this)}></input></Td>
        </Tr>
        <Tr>
          <Td>Pwd:</Td><Td><input type="password" value={this.state.pwd} onChange={this.handlePChange.bind(this)}></input></Td>
        </Tr>
      </Table>

  }

}