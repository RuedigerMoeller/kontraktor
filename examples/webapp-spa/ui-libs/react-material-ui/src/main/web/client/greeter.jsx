import React,{Component} from 'react';
import global from "./global";
import {Fader} from "./util";
import {HCenter} from "./util";
import RaisedButton from 'material-ui/RaisedButton';

class Greeter extends Component {

  constructor(props) {
    super(props);
    this.state = {
      greeting: '...'
    };
  }

  componentDidMount() {
    global.session.greet('World')
    .then( (res,err) => this.setState({greeting: err ? err : res}) );
  }

  anotherGreet() {
    global.session.greet("Another World "+new Date())
    .then( (res,err) => this.setState({greeting: err ? err : res}) );
  }

  render() {
    /*enforce component creation to trigger fading*/
    return (
      <div>
        <HCenter>
          <Fader>
            { this.state.greeting == '...' ?
              <div>{this.state.greeting}</div>
              :
              <div>{this.state.greeting}</div>
            }
          </Fader>
        </HCenter>
        <br/><br/>
        <HCenter>
          <RaisedButton onClick={() => this.anotherGreet()}>
            Greet
          </RaisedButton>
        </HCenter>
      </div>
    );
  }

}

export default Greeter;