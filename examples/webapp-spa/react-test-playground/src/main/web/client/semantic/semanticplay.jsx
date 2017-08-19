import {Component} from 'react';
import {Button,Dropdown,Label,Input,Step,Icon,Form} from 'semantic-ui/semantic-ui-react.min.js';

const steps = [
  { icon: 'truck', title: 'Shipping', description: 'Choose your shipping options' },
  { active: true, icon: 'payment', title: 'Billing', description: 'Enter billing information' },
  { disabled: true, icon: 'info', title: 'Confirm Order' },
];

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
  { key: 'ux', text: 'User Experience', value: 'ux' },
];


const options = [
  { key: 'm', text: 'Male', value: 'male' },
  { key: 'f', text: 'Female', value: 'female' },
];

class FormExampleSubcomponentControl extends Component {

  constructor(p) {
    super(p);
    this.state = {};
  }

  handleChange(e, { value }) {
    this.setState({ value });
  }

  render() {
    const { value } = this.state
    return (
      <Form>
        <Form.Group widths='equal'>
          <Form.Input label='First name' placeholder='First name' />
          <Form.Input label='Last name' placeholder='Last name' />
          <Form.Select label='Gender' options={options} placeholder='Gender' />
        </Form.Group>
        <Form.Group inline>
          <label>Size</label>
          <Form.Radio label='Small' value='sm' checked={value === 'sm'} onChange={this.handleChange.bind(this)} />
          <Form.Radio label='Medium' value='md' checked={value === 'md'} onChange={this.handleChange.bind(this)} />
          <Form.Radio label='Large' value='lg' checked={value === 'lg'} onChange={this.handleChange.bind(this)} />
        </Form.Group>
        <Form.TextArea label='About' placeholder='Tell us more about you...' />
        <Form.Checkbox label='I agree to the Terms and Conditions' />
        <Form.Button>Submit</Form.Button>
      </Form>
    )
  }
}

class SemanticPlay extends Component {

  render() {
    return (
      <div style={{marginTop: 48, marginBottom: 48}}>
        <h1>Semantic UI</h1>
        <div>
          <Button label={1048} icon='fork' labelPosition='left' />
          <Button label='1,048' icon='fork' labelPosition='left' />
          <Button label={{ content: '2,048' }} icon='heart' content='Like' labelPosition='left' />
          <Button label={<Label>2,048</Label>} icon='heart' content='Like' />
        </div>
        <br/><br/>
        <div>
          <Input loading placeholder='Search...' />
        </div>
        <br/><br/>
        <div>
          <Input label='http://' placeholder='mysite.com' />
        </div>
        <br/><br/>
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
          <Step.Group>
            <Step>
              <Icon name='truck' />
              <Step.Content>
                <Step.Title>Shipping</Step.Title>
                <Step.Description>Choose your shipping options</Step.Description>
              </Step.Content>
            </Step>

            <Step active>
              <Icon name='payment' />
              <Step.Content title='Billing' description='Enter billing information' />
            </Step>

            <Step disabled icon='info' title='Confirm Order' />
          </Step.Group>

          <br />

          <Step.Group items={steps} />
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