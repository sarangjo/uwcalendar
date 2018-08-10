// @flow
import React from 'react';
import { TextField, Button, Select, MenuItem, Input } from '@material-ui/core';
import Icon from '@material-ui/core/Icon';

const NAMES = ['M', 'T', 'W', 'Th', 'F'];

class AddClass extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      name: 'Name',
      location: 'Location',
      start: '00:00',
      end: '01:00',
      days: ['M'],
    };
  }

  handleChange = field => event => {
    this.setState({
      [field]: event.target.value
    });
  }

  render() {
    return (
      <div>
        <TextField label="Class Name" value={this.state.name} onChange={this.handleChange('name')}/>
        <TextField label="Class Location" value={this.state.location} onChange={this.handleChange('location')}/>
        <TextField label="Start Time" type="time" value={this.state.start} onChange={this.handleChange('start')} inputProps={{ step: 1800 }} />
        <TextField label="End Time" type="time" value={this.state.end} onChange={this.handleChange('end')} inputProps={{ step: 1800 }} />
        <Select multiple value={this.state.days} onChange={this.handleChange('days')} input={<Input />}>
          {NAMES.map(name => (
            <MenuItem key={name} value={name}>{name}</MenuItem>
          ))}
        </Select>
        <Button variant="contained" onClick={this.props.onAdd.bind(null, Object.assign({}, this.state))}>
          Add Class
          <Icon>add_circle</Icon>
        </Button>
      </div>
    );
  }
}

export default AddClass;
