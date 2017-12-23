import React, {Component} from 'react';
import {Button,Dropdown,Label,Input,Step,Icon,Form} from 'semantic-ui-react';

const BratkartoffelnWärenLecker = { text: 'mmh' };

const optionsDD = [
  { key: 'angular', text: 'Angular', value: 'angular' },
  { key: 'css', text: 'CSS', value: 'css' },
  { key: 'design', text: 'Graphic Design', value: 'design' },
  { key: 'ember', text: 'Ember', value: 'ember' },
  { key: 'html', text: 'HTML', value: 'html' },
  { key: 'ia', text: 'Information Architecture', value: 'ia' },
  { key: 'javascript', text: 'Javascript', value: 'javascript' },
  { key: 'mech', text: 'Mechanical Engineering', value: 'mech' },
  { key: 'meteor', text: 'Meteor', value: 'meteor' },
  { key: 'node', text: 'NodeJS', value: 'node' },
  { key: 'plumbing', text: 'Plumbing', value: 'plumbing' },
  { key: 'python', text: 'Python', value: 'python' },
  { key: 'rails', text: 'Rails', value: 'rails' },
  { key: 'react', text: 'React', value: 'react' },
  { key: 'repair', text: 'Kitchen Repair', value: 'repair' },
  { key: 'ruby', text: 'Ruby', value: 'ruby' },
  { key: 'ui', text: 'UI Design', value: 'ui' },
  { key: 'ux', text: 'User Experience', value: 'ux' }
];


export const options = [
  { key: 'm', text: 'Male', value: 'male' },
  { key: 'f', text: 'Female', value: 'female' },
];

export function DummyFun(x,y) {
  console.log("DummyFun");
  return "Honkytonk1";
}

export const DummyFun1 = (x,y) => {
  console.log("DummyFun1");
};

export const DummyFun2 = function(x,y) {
  console.log("DummyFun2");
};

export const DummyFun3 = x => {
  console.log("DummyFun3");
  return "DM3-XX--"+BratkartoffelnWärenLecker.text;
};

export class FormExampleSubcomponentControl extends Component {

  constructor(p) {
    super(p);
    this.state = {};
    console.log("FEX construcotr");
  }

  handleChange(e, { value }) {
    this.setState({ value });
    console.log("handleChange",value);
  }

  render() {
    console.log("FEX state",this.state);
    const { value } = this.state;
    return (
      <Form>
        <Form.Group widths='equal'>
          <Form.Input label='First name' placeholder='First name' />
          <Form.Input label='Last name' placeholder='Last name' />
          <Form.Select label='Gender' options={options} placeholder='Gender' />
        </Form.Group>
        <Form.Group inline>
          <label>Size</label>
          <Form.Radio label='Small' value='sm' checked={value === 'sm'} onChange={ (e,v) => this.handleChange(e,v) } />
          <Form.Radio label='Medium' value='md' checked={value === 'md'} onChange={ (e,v) => this.handleChange(e,v) } />
          <Form.Radio label='Large' value='lg' checked={value === 'lg'} onChange={ (e,v) => this.handleChange(e,v) } />
        </Form.Group>
        <Form.TextArea label='About' placeholder='Tell us more about you...' />
        <Form.Checkbox label='I might agree to the Terms and Conditions' />
        <Form.Button>Submit</Form.Button>
      </Form>
    )
  }
}
//
class SemanticPlay extends Component {

  render() {
    const h = 22;
    return (
      <div style={{marginTop: 48, marginBottom: 48}}>
        <h1>Semantic UI</h1>
        <div>
          <Button label={1048} icon='fork' labelPosition='left' />
          <Button label='1,048' icon='fork' labelPosition='left' />
          <Button label={{ content: '248' }} icon='heart' content='Likely' labelPosition='left' />
          <Button label={<Label>2,048</Label>} icon='heart' content='Like' />
        </div>
        <br/><br/>
        <div>
          <Input loading placeholder='Search me ..' />
        </div>
        <br/><br/>
        <div>
          <Input label='http://' placeholder='mysite.com' />
        </div>
        <div style={{height: h }}/>
        <div style={{height: h, backgroundColor: 'green'}}/>
        <div style={{height: h, backgroundColor: 'red'}}/>
        <div>
          <Input
            icon='tags'
            iconPosition='left'
            label={{ tag: true, content: 'Add Tag' }}
            labelPosition='right'
            placeholder='Enter tags'
          />
        </div>
        <br/><br/>
        <div>
          <Input
            action={{ color: 'teal', labelPosition: 'right', icon: 'copy', content: 'Copy' }}
            defaultValue='http://ww.short.url/c0opq'
          />
        </div>
        <br/><br/>

        <div>
          <Input list='languages' placeholder='Choose language...' />
          <datalist id='languages'>
            <option value='Djörmän' />
            <option value='English' />
            <option value='Chinese' />
            <option value='Dutch' />
          </datalist>
        </div>
        <br/><br/>

        <div>
          <FormExampleSubcomponentControl/>
        </div>
        <br/><br/>
        <div>
          <Dropdown placeholder='Skills' fluid multiple selection options={optionsDD} />
        </div>
        <br/><br/>

      </div>
    );
  }

}