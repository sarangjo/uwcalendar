// @flow
import React from 'react';
import { TextField, Button, Select, MenuItem, Input } from '@material-ui/core';
import Icon from '@material-ui/core/Icon';

import goog from '../services/goog';

class Class extends React.Component {
  constructor(props) {
    super(props);
    this.state = Object.assign({}, props.data);
  }

  handleChange = field => event => {
    this.setState({
      [field]: event.target.value
    });
  }

  render() {
    let deleteButton;
    if (this.props.onDelete) {
      deleteButton = (
        <Button variant="contained" onClick={this.props.onDelete.bind(null, Object.assign({}, this.state))}>
          Delete
          <Icon>delete</Icon>
        </Button>
      );
    }
    // TODO color it if it's not state.name != props.data.name
    return (
      <div className={this.props.className}>
        <TextField label="Class Name" value={this.state.name} onChange={this.handleChange('name')}/>
        <TextField label="Class Location" value={this.state.location} onChange={this.handleChange('location')}/>
        <TextField label="Start Time" type="time" value={this.state.start} onChange={this.handleChange('start')} inputProps={{ step: 1800 }} />
        <TextField label="End Time" type="time" value={this.state.end} onChange={this.handleChange('end')} inputProps={{ step: 1800 }} />
        <Select multiple value={this.state.days} onChange={this.handleChange('days')} input={<Input />}>
          {goog.RECURRENCE_DAYS.map(name => (
            <MenuItem key={name} value={name}>{name}</MenuItem>
          ))}
        </Select>
        <Button variant="contained" onClick={this.props.onClick.bind(null, Object.assign({}, this.state))}>
          {this.props.text}
          <Icon>save</Icon>
        </Button>
        {deleteButton}
      </div>
    );
  }
}

Class.defaultProps = {
  data: {
    name: "Name",
    location: "Location",
    start: "15:00",
    end: "16:00",
    days: [goog.RECURRENCE_DAYS[0]],
  },
  onClick: () => {},
  text: "Save",
  onDelete: null,
};

export default Class;
