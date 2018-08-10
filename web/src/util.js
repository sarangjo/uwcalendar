// @flow
import React from 'react';

// Attach user to the component
export function component(c, u) {
  return (props) => React.createElement(c, {
    ...props, user: u
  });
}
