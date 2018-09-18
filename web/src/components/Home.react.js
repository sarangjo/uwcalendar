// @flow
import React from 'react';
import { FormControl, InputLabel, Select, MenuItem, Button } from '@material-ui/core';

import Class from './Class.react';

import goog from '../services/goog';
import fb from '../services/fb';
import uw from '../services/uw';
import { QUARTERS } from '../constants';

import './Home.css';

class Home extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      quarter: "au18",
      quarterSchedule: [],
    };
  }

  componentDidMount = this.updateSubscriber;

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

  handleSaveClass(id, o, event) {
    fb.updateClass(this.props.user.uid, this.state.quarter, id, o)
    .then((s) => {
      alert("Updated!");
    });
    // TODO add error catching
  }

  handleDeleteClass(id, o, event) {
    // TODO deletion requires deleting Google event first
    console.log(o, id);
    fb.deleteClass(this.props.user.uid, this.state.quarter, id, o).then((s) => {
      alert("Deleted!");
    });
    // TODO add error catching
  }

  handleAddClass(o, event) {
    fb.addClass(this.props.user.uid, this.state.quarter, o).then((s) => {
      // TODO Do something with state, but not setting state because we already
      // do that with our subscriber
      console.log(s);
    });
  }

  handleUw(event) {
    uw.getQuarterDetails();
  }

  handleGoogleSync(event) {
    // TODO add loading thingy

    let x = goog.loadApi(this.props.user)
    .then(() => goog.updateGoogle(this.state.quarterSchedule));

    // IDEA don't always update the google event id?
    x.then(gevs => fb.updateGoogleEventIds(this.props.user.uid, this.state.quarter, this.state.quarterSchedule, gevs))
    // FIXME error handling
    .then(() => {
      console.log("Synced with Google!");
    })
    .catch(() => {});
  }

  render() {
    let quarterMenuItems = QUARTERS.map((q) => (
      <MenuItem key={q} value={q}>{q}</MenuItem>
    ));

    let classItems = this.state.quarterSchedule.map((c, i) => (
      <Class key={i} data={c} onClick={this.handleSaveClass.bind(this, c.id)} text="Update"
        onDelete={this.handleDeleteClass.bind(this, c.id)}/>
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
        <Button onClick={this.handleUw.bind(this)}>UW Stuff</Button>
        <Button onClick={this.handleGoogleSync.bind(this)}>Sync with Google</Button>
        {classItems}
        <div className='add-class'>
          New Class:
          <Class onClick={this.handleAddClass.bind(this)}/>
        </div>
      </div>
    );
  }
}

export default Home;
