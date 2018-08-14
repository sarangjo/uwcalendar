// @flow
import React from 'react';
import { FormControl, InputLabel, Select, MenuItem, Button } from '@material-ui/core';

import Class from './Class.react';
import AddClass from './AddClass.react';

import fb from '../services/fb';
import { QUARTERS } from '../constants';

class Home extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      quarter: "au18",
      quarterSchedule: [],
    };
  }

  componentDidMount() {
    this.updateSubscriber();
  }

  updateSubscriber() {
    // instead of a one-time get why not just attach a hook to the schedule
    // and then unsub and resub when the quarter changes
    if (this.unsub) this.unsub();

    this.unsub = fb.getScheduleSubscriber(this.props.user.uid, this.state.quarter, (s) => {
      this.setState({
        quarterSchedule: s
      });
    })
  }

  handleChange(event) {
    this.setState({
      quarter: event.target.value
    }, this.updateSubscriber);
  }

  handleAddClass(o, event) {
    fb.addClass(this.props.user.uid, this.state.quarter, o).then((s) => {
      // TODO Do something with state, but not setting state because we already
      // do that with our subscriber
      console.log(s);
    });
  }

  handleGoogleSync(event) {

  }

  render() {
    let quarterMenuItems = QUARTERS.map((q) => (
      <MenuItem key={q} value={q}>{q}</MenuItem>
    ));

    let classItems = this.state.quarterSchedule.map((c, i) => (
      <Class key={i} data={c}/>
    ));

    return (
      <div>
        <FormControl>
          <InputLabel>Quarter</InputLabel>
          <Select value={this.state.quarter}
            onChange={this.handleChange.bind(this)}>
            {quarterMenuItems}
          </Select>
        </FormControl>
        <Button onClick={this.handleGoogleSync.bind(this)}>Sync with Google</Button>
        {classItems}
        <AddClass onAdd={this.handleAddClass.bind(this)}/>
      </div>
    );
  }
}

export default Home;
