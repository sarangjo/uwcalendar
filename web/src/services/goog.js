// @flow
import moment from 'moment';

const config = require('../config/config.json');

// If modifying these scopes, delete token.json.
const SCOPES = ['https://www.googleapis.com/auth/calendar'];
const DISCOVERY_DOCS = ["https://www.googleapis.com/discovery/v1/apis/calendar/v3/rest"];

const TOKEN_PATH = '../config/token.json';

let gapi;

function apiLoaded(user) {
  if (user) {
    let script = document.createElement('script');
    script.type = 'text/javascript';
    script.src = 'https://apis.google.com/js/api.js';
    script.onload = (e) => {
      gapi = window.gapi;

      window.gapi.load('client', () => {
        window.gapi.client.init({
          apiKey: config.apiKey,
          clientId: config.clientId,
          discoveryDocs: DISCOVERY_DOCS,
          scope: SCOPES.join(' '),
        })
        .then(() => user.getIdToken())
        .then(token => {
          window.gapi.client.setToken({
            access_token: localStorage.getItem('accessToken')
          });
        });
      }); // end onload
    }
    document.getElementsByTagName('head')[0].appendChild(script);
  }
}

function createGoogleEvent(c) {
  return {
    summary: c.name,
    location: c.location,
    start: {
      dateTime: moment(c.start, "HH:mm").toISOString(),
      timeZone: 'America/Los_Angeles',
    },
    end: {
      dateTime: moment(c.end, "HH:mm").toISOString(),
      timeZone: 'America/Los_Angeles',
    }
  };
}

// Google related fields:

// - Google Event ID
// - Color
// - Calendar name --> should this be per quarter, or per user?

// "Update" because we don't care if the user made any changes in the Google
// event, they get overwritten when this function is run
function updateGoogle(schedule) {
  // Go through each of the classes in this quarter, and do the following.
  let promises = schedule.map(c =>
    (c.googleEventId) ?
    // - if the class already has an associated Google Event ID, update the google
    // event with the information in the Firebase object.
    new Promise(function(resolve, reject) {
      resolve();
    })
    :
    // - if not, then create a brand new Google event and update it with this
    // information, saving the Google event ID in the class's Firebase object.
    new Promise(function(resolve, reject) {
      let request = gapi.client.calendar.events.insert({
        calendarId: 'primary',
        resource: createGoogleEvent(c),
      });
      request.execute((event) => {
        resolve(event);
      });
    })
  );

  return Promise.all(promises);
}

export default {
  apiLoaded,
  updateGoogle,
  // ourGapi,

  config,
  SCOPES,
  DISCOVERY_DOCS
};
