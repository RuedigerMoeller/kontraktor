import { VSpacer, EmptyLine, HCenter, Caption, Table,Tr,Td} from './layout.jsx';
import React from 'react';
import {AppActions, Store as AppStore} from './store.jsx';

let idCount = 0;
export class UserTable extends React.Component {

  constructor(props) {
    super(props);
    this.state = {
      records: [ { key: "hello", pwd:"1234"} ]
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

  render() {
    return (
      <Table>
        <Tr><Caption>Users</Caption></Tr>
        <EmptyLine/>
        {this.state.records.map( rec => <Tr key={rec.key}><Td>{rec.key}</Td><Td>{rec.pwd}</Td></Tr>)}
      </Table>
    )
  }

}