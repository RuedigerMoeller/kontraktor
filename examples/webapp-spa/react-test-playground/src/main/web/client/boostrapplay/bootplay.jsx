import {
  OverlayTrigger, Tooltip, Button, ButtonGroup, ButtonToolbar,
  DropdownButton, MenuItem, Modal, Nav, NavItem, NavDropdown, Tabs, Tab,
  Pagination, Table, Accordion, Panel, Form, FormGroup, Col, ControlLabel, FormControl, Checkbox,
  Carousel, ProgressBar
} from 'react-bootstrap';

import React, { Component } from 'react';

const tooltip = (
  <Tooltip id="tooltip"><strong>Holy guacamole!</strong> Check this info.</Tooltip>
);

class PaginationAdvanced extends Component {

  constructor(props) {
    super(props);
    this.state = {
      activePage: 1
    };
  }

  handleSelect(eventKey) {
    this.setState({
      activePage: eventKey
    });
  }

  render() {
    return (
      <Pagination
        prev
        next
        first
        last
        ellipsis
        boundaryLinks
        items={20}
        maxButtons={5}
        activePage={this.state.activePage}
        onSelect={ ev => this.handleSelect(ev) } />
    );
  }

}

const now = 60;

class BootPlay extends Component {

  constructor(props) {
    super(props);
    this.state = { showModal: false };
  }

  showModal(ev) {
    this.setState({ showModal: !this.state.showModal });
  }

  render() {
    return (
      <div style={{margin:16, display: 'block'}}>
        <h1>react-bootstrap</h1>
        <p> BootStrap Buttons </p>
        <div style={{margin:16}}>
          <Button bsStyle="primary">Primary</Button> &nbsp;
          <Button bsStyle="info">Info</Button> &nbsp;
          <Button bsStyle="default">Default</Button>&nbsp;
          <Button bsStyle="info" disabled>Info Disabled</Button> &nbsp;
          <DropdownButton title="Dropdown" id="bg-nested-dropdown">
            <MenuItem eventKey="1">Dropdown link</MenuItem>
            <MenuItem eventKey="2">Dropdown link</MenuItem>
          </DropdownButton>
        </div>
        <div style={{margin:16}}>
          <ButtonToolbar>
            <ButtonGroup>
              <Button>Left</Button>
              <Button>Middle</Button>
              <Button>Right</Button>
            </ButtonGroup>
          </ButtonToolbar>
        </div>
        <p>Dialog</p>
        <Button onClick={ ev => this.showModal(ev) }>Click to show</Button>
        { this.state.showModal ?
          <div className="static-modal">
            <Modal.Dialog>
              <Modal.Header>
                <Modal.Title>Modal title</Modal.Title>
              </Modal.Header>

              <Modal.Body>
                One fine body...
              </Modal.Body>

              <Modal.Footer>
                <Button onClick={ ev => this.showModal(ev)}>Close</Button>
                <Button bsStyle="primary" onClick={ ev => this.showModal(ev)}>Save changes</Button>
              </Modal.Footer>
            </Modal.Dialog>
          </div>
          : ""
        }
        <br/><br/>
        <p>Tooltip</p>
        <ButtonToolbar>
          <OverlayTrigger placement="left" overlay={tooltip}>
            <Button bsStyle="default">Holy guacamole!</Button>
          </OverlayTrigger>

          <OverlayTrigger placement="top" overlay={tooltip}>
            <Button bsStyle="default">Holy guacamole!</Button>
          </OverlayTrigger>

          <OverlayTrigger placement="bottom" overlay={tooltip}>
            <Button bsStyle="default">Holy guacamole!</Button>
          </OverlayTrigger>

          <OverlayTrigger placement="right" overlay={tooltip}>
            <Button bsStyle="default">Holy guacamole!</Button>
          </OverlayTrigger>
        </ButtonToolbar>

        <br/><br/>
        <p>Tabs</p>
        <Tabs defaultActiveKey={2} id="uncontrolled-tab-example">
          <Tab eventKey={1} title="Tab 1">Tab 1 content</Tab>
          <Tab eventKey={2} title="Tab 2">Tab 2 content</Tab>
          <Tab eventKey={3} title="Tab 3" disabled>Tab 3 content</Tab>
        </Tabs>

        <br/><br/>
        <p>Accordion</p>
        <Accordion>
          <Panel header="Collapsible Group Item #1" eventKey="1">
            Anim pariatur cliche reprehenderit, enim eiusmod high life accusamus terry richardson ad squid. 3 wolf moon officia aute, non cupidatat skateboard dolor brunch. Food truck quinoa nesciunt laborum eiusmod. Brunch 3 wolf moon tempor, sunt aliqua put a bird on it squid single-origin coffee nulla assumenda shoreditch et. Nihil anim keffiyeh helvetica, craft beer labore wes anderson cred nesciunt sapiente ea proident. Ad vegan excepteur butcher vice lomo. Leggings occaecat craft beer farm-to-table, raw denim aesthetic synth nesciunt you probably haven't heard of them accusamus labore sustainable VHS.
          </Panel>
          <Panel header="Collapsible Group Item #2" eventKey="2">
            Anim pariatur cliche reprehenderit, enim eiusmod high life accusamus terry richardson ad squid. 3 wolf moon officia aute, non cupidatat skateboard dolor brunch. Food truck quinoa nesciunt laborum eiusmod. Brunch 3 wolf moon tempor, sunt aliqua put a bird on it squid single-origin coffee nulla assumenda shoreditch et. Nihil anim keffiyeh helvetica, craft beer labore wes anderson cred nesciunt sapiente ea proident. Ad vegan excepteur butcher vice lomo. Leggings occaecat craft beer farm-to-table, raw denim aesthetic synth nesciunt you probably haven't heard of them accusamus labore sustainable VHS.
          </Panel>
          <Panel header="Collapsible Group Item #3" eventKey="3">
            Anim pariatur cliche reprehenderit, enim eiusmod high life accusamus terry richardson ad squid. 3 wolf moon officia aute, non cupidatat skateboard dolor brunch. Food truck quinoa nesciunt laborum eiusmod. Brunch 3 wolf moon tempor, sunt aliqua put a bird on it squid single-origin coffee nulla assumenda shoreditch et. Nihil anim keffiyeh helvetica, craft beer labore wes anderson cred nesciunt sapiente ea proident. Ad vegan excepteur butcher vice lomo. Leggings occaecat craft beer farm-to-table, raw denim aesthetic synth nesciunt you probably haven't heard of them accusamus labore sustainable VHS.
          </Panel>
        </Accordion>

        <br/><br/>
        <p>Pagination</p>
        <PaginationAdvanced/>

        <br/><br/>
        <p>Table</p>
        <Table striped bordered condensed hover>
          <thead>
          <tr>
            <th>#</th>
            <th>First Name</th>
            <th>Last Name</th>
            <th>Username</th>
          </tr>
          </thead>
          <tbody>
          <tr>
            <td>1</td>
            <td>Mark</td>
            <td>Otto</td>
            <td>@mdo</td>
          </tr>
          <tr>
            <td>2</td>
            <td>Jacob</td>
            <td>Thornton</td>
            <td>@fat</td>
          </tr>
          <tr>
            <td>3</td>
            <td colSpan="2">Larry the Bird</td>
            <td>@twitter</td>
          </tr>
          </tbody>
        </Table>


        <br/><br/>
        <p>Form</p>
        <Form horizontal>
          <FormGroup controlId="formHorizontalEmail">
            <Col componentClass={ControlLabel} sm={2}>
              Email
            </Col>
            <Col sm={10}>
              <FormControl type="email" placeholder="Email" />
            </Col>
          </FormGroup>

          <FormGroup controlId="formHorizontalPassword">
            <Col componentClass={ControlLabel} sm={2}>
              Password
            </Col>
            <Col sm={10}>
              <FormControl type="password" placeholder="Password" />
            </Col>
          </FormGroup>

          <FormGroup>
            <Col smOffset={2} sm={10}>
              <Checkbox>Remember me</Checkbox>
            </Col>
          </FormGroup>

          <FormGroup>
            <Col smOffset={2} sm={10}>
              <Button type="submit">
                Sign in
              </Button>
            </Col>
          </FormGroup>
        </Form>

        <br/><br/>
        <p>Carousel</p>
        <div style={{maxWidth: 600}}>
          <Carousel>
            <Carousel.Item>
              <img width={900} height={500} alt="900x500" src="/assets/carousel.png"/>
              <Carousel.Caption>
                <h3>First slide label</h3>
                <p>Nulla vitae elit libero, a pharetra augue mollis interdum.</p>
              </Carousel.Caption>
            </Carousel.Item>
            <Carousel.Item>
              <img width={900} height={500} alt="900x500" src="/assets/carousel.png"/>
              <Carousel.Caption>
                <h3>Second slide label</h3>
                <p>Lorem ipsum dolor sit amet, consectetur adipiscing elit.</p>
              </Carousel.Caption>
            </Carousel.Item>
            <Carousel.Item>
              <img width={900} height={500} alt="900x500" src="/assets/carousel.png"/>
              <Carousel.Caption>
                <h3>Third slide label</h3>
                <p>Praesent commodo cursus magna, vel scelerisque nisl consectetur.</p>
              </Carousel.Caption>
            </Carousel.Item>
          </Carousel>
        </div>


        <br/><br/>
        <p>Misc</p>
        <ProgressBar now={now} label={`${now}%`} />

      </div>
    );
  }
}
