import React from 'react';
import { Disabler, VSpacer, EmptyLine, HCenter, Fader, Icon, Caption, Table,Tr,Td} from './layout.jsx';
import { AppActions, Store as AppStore } from './store.jsx';

export class Btn extends React.Component {
  render() {
    const style= { display: 'inline-block', float: 'right', ...this.props.style };
    return (
      <div className={this.props.light ? "lightbtn":"defbtn"} style={style} onClick={this.props.onClick}>
        {this.props.children}
      </div>
    )
  }
}

function Error(props) {
  return (<HCenter><div style={{fontSize: 11, color: 'darkred', background: "rgba(255,255,255,.5)", padding: 2 }}>{props.children}</div></HCenter>);
}

export class Register extends React.Component {

  constructor(props) {
    super(props);
    this.state = {
      user: '', pwd: '', confirmPwd: '', error: '', text: '',
      valid: false
    }
  }

  handleUChange(event) {
    this.setState({user: event.target.value}, () => this.validate() );
  }

  handlePChange(event) {
    this.setState({pwd: event.target.value}, () => this.validate() );
  }

  handleTChange(event) {
    this.setState({text: event.target.value} );
  }

  handleCPChange(event) {
    this.setState({confirmPwd: event.target.value}, () => this.validate() );
  }

  validate() {
    const user = this.state.user;
    const pwd = this.state.pwd;
    const confirm = this.state.confirmPwd;
    const ascii = /^[\x21-\x7F]*$/;
    if ( !user || user.length < 6 || user.length >=20 || !ascii.test(user) ) {
      this.setState({valid: false, error: 'name requires 6-20 letters, numbers, common symbols'})
      return;
    }
    if ( !pwd || pwd.length < 6 || pwd.length >= 20 ) {
      this.setState({valid: false, error: 'pwd requires 6 to 20 chars'});
      return;
    }
    if ( pwd != confirm ) {
      this.setState({valid: false, error: 'confirmation password does not match'});
      return;
    }
    this.setState({valid:true,error:''})
  }

  handleSubmit() {
    AppActions.register(this.state.user, this.state.pwd, this.state.text).then( (r,e) => {
      if ( r ) {
        this.setState({ error: "Registered Successfully. Logging in .." });
        setTimeout( () => AppActions.login(this.state.user,this.state.pwd), 2000 );
      } else {
        this.setState({ error: e });
      }
    })
  }

  render() {
    return (
    <Fader>
      <VSpacer size="50px"/>
      <Table style={{padding: 32}}>
        <Tr>
          <Caption span="2">Register</Caption>
        </Tr>
        <EmptyLine/>
        <Tr>
          <Td>User:</Td><Td><input type="text" value={this.state.user} onChange={this.handleUChange.bind(this)}></input></Td>
        </Tr>
        <Tr>
          <Td>Pwd:</Td><Td><input type="password" value={this.state.pwd} onChange={this.handlePChange.bind(this)}></input></Td>
        </Tr>
        <Tr>
          <Td>Confirm Pwd:</Td><Td><input type="password" value={this.state.confirmPwd} onChange={this.handleCPChange.bind(this)}></input></Td>
        </Tr>
        <Tr>
          <Td>Freetext:</Td><Td><input type="text" cols="60" rows="3" value={this.state.text} onChange={this.handleTChange.bind(this)}></input></Td>
        </Tr>
        <EmptyLine/>
        <Tr>
          <Td></Td><Td><Disabler enabled={this.state.valid}><Btn onClick={this.handleSubmit.bind(this)}>Submit</Btn></Disabler></Td>
        </Tr>
      </Table>
      <br/>
      <Error>{this.state.error}</Error>
    </Fader>
    );
  }

}

export class Login extends React.Component {

  constructor(props) {
    super(props);
    this.state = {
      user: '', pwd: '', error: '',
      valid: false
    }
  }

  handleUChange(event) {
    this.setState({user: event.target.value}, () => this.validate() );
  }

  handlePChange(event) {
    this.setState({pwd: event.target.value}, () => this.validate() );
  }

  validate() {
    const user = this.state.user;
    const pwd = this.state.pwd;
    const ascii = /^[\x21-\x7F]*$/;
    if ( !user || user.length < 6 || user.length >=20 || !ascii.test(user) ) {
      this.setState({valid: false, error: 'name must consist of 6-20 chars'})
      return;
    }
    if ( !pwd || pwd.length < 6 || pwd.length >= 20 ) {
      this.setState({valid: false, error: 'pwd must consist 6 to 20 chars'});
      return;
    }
    this.setState({valid:true,error:''})
  }

  handleLogin(event) {
    AppActions.login( this.state.user, this.state.pwd ).then( (res,err) => err ? this.setState( { error: err }) : null);
  }

  handleRegister(event) {
    this.props.history.push('/register');
  }

  render() {
    return (
    <Fader>
      <VSpacer size="50px"/>
      <Table style={{padding: "32px 64px"}}>
        <Tr>
          <Caption>Login</Caption>
        </Tr>
        <EmptyLine/>
        <Tr>
          <Td><Icon icon="fa-user-circle-o fa-2xX" style={{float:'right'}}/></Td>
          <Td><input placeholder="name" type="text" value={this.state.user} onChange={this.handleUChange.bind(this)}></input></Td>
        </Tr>
        <Tr>
          <Td><Icon icon="fa-key fa-2xX" style={{float:'right'}}/></Td>
          <Td><input placeholder="password" type="password" value={this.state.pwd} onChange={this.handlePChange.bind(this)}></input></Td>
        </Tr>
        <EmptyLine/>
        <Tr>
          <Td>
          </Td>
          <Td>
            <Disabler enabled={this.state.valid}><Btn onClick={() => this.handleLogin()}><Icon icon="fa-unlock" small/>&nbsp;Login</Btn></Disabler>
            <Btn style={{float: 'left'}} light onClick={() => this.handleRegister()}>Register</Btn>
          </Td>
        </Tr>
      </Table>
      <br/>
      <Error>{this.state.error}</Error>

    </Fader>
    )
  }

}