import React, {Component} from 'react';

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
    return <div>
      <div>
        User: <input type="text" value={this.state.user} onChange={this.handleUChange}></input>
      </div>
      <div>
        Pwd: <input type="password" value={this.state.pwd} onChange={this.handlePChange}></input>
      </div>
    </div>
  }
}