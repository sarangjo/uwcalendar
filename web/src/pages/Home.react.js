// @flow
import React from 'react';
import { FormControl, InputLabel, Select, MenuItem } from '@material-ui/core';
import { QUARTERS } from '../constants';

class Home extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      quarter: "au18"
    };
  }

  handleChange(event) {
    this.setState({
      quarter: event.target.value
    });
  }

  render() {
    let quarterMenuItems = QUARTERS.map((q) => (
      <MenuItem key={q} value={q}>{q}</MenuItem>
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
        {this.state.quarter}
      </div>
    );
  }
}

export default Home;
