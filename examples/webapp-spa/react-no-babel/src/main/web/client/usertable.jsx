import { VSpacer, EmptyLine, HCenter, Caption, Table, Tr, Td } from './layout.jsx';
import React from 'react';
import {AppActions, Store as AppStore} from './store.jsx';
import {Btn} from "./login.jsx";

function Unverified() {
  return (
    <div style={{ width: 16, background: "tomato", color: 'white', padding: 4, border: '1px solid #eee'}}><HCenter>!</HCenter></div>
  )
}

function Verified() {
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
        let jsmap = decoder.jsmap(r.map);
        jsmap["key"] = r.key;
        const copy = this.state.records.slice();
        copy.push(jsmap);
        this.setState({ records: copy });
      } else {
        console.log("query finished")
      }
    });
  }

  onDel(key,index) {
    AppActions.deleteUser(key);
    const copy = this.state.records.slice();
    copy.splice(index,1);
    this.setState({ records: copy });
  }

  render() {
    let count = 0;
    const grey = { background: "#eee" };
    const none = {};
    return (
      <Table bg='#fff' style={{ minWidth: "50%"}}>
        <Tr><Caption>User</Caption></Tr>
        <EmptyLine/>
        {this.state.records.map( (rec,index) =>
          <Tr key={rec.key}>
            <Td style={ ((++count%2) == 1) ? grey : none }>{rec.key}</Td>
            <Td style={ ((count%2) == 1) ? grey : none }>{rec.pwd}</Td>
            <Td style={ ((count%2) == 1) ? grey : none }>{rec.verified ? <Verified/>: <Unverified/>}</Td>
            <Td style={ ((count%2) == 1) ? grey : none }>{rec.text}</Td>
            <Td style={ ((count%2) == 1) ? grey : none }>{rec.count}</Td>
            <Td style={ ((count%2) == 1) ? grey : none }><Btn name={rec.key} onClick={() => this.onDel(rec.key,index)}>x</Btn></Td>
          </Tr>)}
      </Table>
    )
  }

}