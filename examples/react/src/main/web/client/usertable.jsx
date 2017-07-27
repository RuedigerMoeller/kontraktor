import { VSpacer, EmptyLine, HCenter, Caption, Table, Tr, Td } from './layout.jsx';
import React from 'react';
import {AppActions, Store as AppStore} from './store.jsx';
import {Btn} from "./login.jsx";

function Verified() {
  return (
    <div style={{ width: 16, background: "tomato", color: 'white', padding: 4, border: '1px solid #eee'}}><HCenter>!</HCenter></div>
  )
}

function Unverified() {
  return (
    <div style={{ width: 16, color: "white", background: 'green', padding: 4, border: '1px solid #eee'}}><HCenter>Ok</HCenter></div>
  )
}

let idCount = 0;
export class UserTable extends React.Component {

  constructor(props) {
    super(props);
    this.state = {
      records: []
    };
    this.idCount = idCount++;
    console.log("created "+this.idCount)
  }

  componentDidMount() {
    const decoder = new DecodingHelper();
    AppActions.queryUsers( (r,e) => {
      console.log("reply ",this.idCount);
      if ( r ) {
        let jsmap = decoder.jsmap(r.mp);
        jsmap["key"] = r.key;
        const copy = this.state.records.slice();
        copy.push(jsmap);
        this.setState({ records: copy });
      } else {
        console.log("query finished")
      }
    });
  }

  onDel(ev) {
    console.log(ev)
  }
  render() {
    let count = 0;
    const grey = { background: "#eee" };
    const none = {};
    return (
      <Table bg='#fff'>
        <Tr><Caption>Users</Caption></Tr>
        <EmptyLine/>
        {this.state.records.map( rec =>
          <Tr key={rec.key}>
            <Td style={ ((++count%2) == 1) ? grey : none }>{rec.key}</Td>
            <Td style={ ((count%2) == 1) ? grey : none }>{rec.pwd}</Td>
            <Td style={ ((count%2) == 1) ? grey : none }>{rec.verified ? <Verified/>: <Unverified/>}</Td>
            <Td style={ ((count%2) == 1) ? grey : none }>{rec.text}</Td>
            <Td style={ ((count%2) == 1) ? grey : none }><Btn name={rec.key} onClick={this.onDel.bind(this)}>x</Btn></Td>
          </Tr>)}
      </Table>
    )
  }

}