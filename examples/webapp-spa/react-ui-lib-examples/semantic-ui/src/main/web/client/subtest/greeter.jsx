import React,{Component} from 'react';
import {global} from "../global";
import {Fader, HCenter} from "./util";
import {Button} from 'semantic-ui-react';

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
    global.session.greet("Another World 1 POKPOK"+new Date())
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
        <HCenter>
          <Button onClick={() => this.anotherGreet()}>
            Another Greet
          </Button>
        </HCenter>
      </div>
    );
  }

}