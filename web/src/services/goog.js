// @flow
const config = require('../config/config.json');

// If modifying these scopes, delete token.json.
const SCOPES = ['https://www.googleapis.com/auth/calendar'];
const DISCOVERY_DOCS = ["https://www.googleapis.com/discovery/v1/apis/calendar/v3/rest"];

const TOKEN_PATH = '../config/token.json';

// Google related fields:

// - Google Event ID
// - Color
// - Calendar name --> should this be per quarter, or per user?

// "Update" because we don't care if the user made any changes in the Google
// event, they get overwritten when this function is run
function updateGoogle(schedule) {
  // Go through each of the classes in this quarter, and do the following.
  schedule.forEach(c => {
    if (c.googleEventId) {
      // - if the class already has an associated Google Event ID, update the google
      // event with the information in the Firebase object.
      return window.gapi.client.calendar.events
      .list({
        calendarId: "primary",
        timeMin: new Date().toISOString(),
        showDeleted: false,
        singleEvents: true,
        maxResults: 10,
        orderBy: "startTime"
      });
    } else {
      // - if not, then create a brand new Google event and update it with this
      // information, saving the Google event ID in the class's Firebase object.
    }
  })
}

export default {
  // apiLoaded,
  updateGoogle,
  // ourGapi,

  config,
  SCOPES,
  DISCOVERY_DOCS
};
