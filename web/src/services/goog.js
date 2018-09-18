// @flow
import moment from 'moment';

import uw from './uw';

const config = require('../config/config.json');

// If modifying these scopes, delete token.json.
const SCOPES = ['https://www.googleapis.com/auth/calendar'];
const DISCOVERY_DOCS = ["https://www.googleapis.com/discovery/v1/apis/calendar/v3/rest"];
const CALENDAR_ID = 'primary';

// RFC5545
const RECURRENCE_DAYS = ["MO", "TU", "WE", "TH", "FR"];

let gapi;

function loadApi(user) {
  return new Promise(function(resolve, reject) {
    if (user) {
      let script = document.createElement('script');
      script.type = 'text/javascript';
      script.src = 'https://apis.google.com/js/api.js';
      script.id = 'gapi-script-container';
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
            resolve(window.gapi);
          });
        }); // end onload
      }
      document.getElementsByTagName('head')[0].appendChild(script);
    } else {
      reject();
    }
  });
}

const RECURRENCE_FORMAT = "YYYYMMDD";

function toGoogleEvent(c, d = uw.getQuarterDetails("au18")) {
  let recurrence = [
  ];

  // Calculate start date
  let offset = RECURRENCE_DAYS.indexOf(c.days[0]);
  // TODO this is slightly complicated, since d.start can be anything, not just Monday
  let startDate = moment(d.start).day(offset + 1);

  let toString = (t) => moment(startDate).set('hour', t.substring(0, 2)).set('minute' t.substring(3)).toISOString();
  let startTime = toString(c.start);
  let endTime = toString(c.end);

  return {
    summary: c.name,
    location: c.location,
    start: {
      dateTime: startTime,
      timeZone: 'America/Los_Angeles',
    },
    end: {
      dateTime: endTime,
      timeZone: 'America/Los_Angeles',
    },
    recurrence: [
      `RRULE:FREQ=WEEKLY;UNTIL=${d.end.format(RECURRENCE_FORMAT)}T115959Z;WKST=SU;BYDAY=${c.days.join()}`
    ],
  };
}

// Google related fields:

// - [x] Google Event ID
// - [ ] Color
// - [ ] Calendar name --> should this be per quarter, or per user?

// "Update" because we don't care if the user made any changes in the Google
// event, they get overwritten when this function is run
function updateGoogle(schedule) {
  // Go through each of the classes in this quarter, and do the following.
  console.log(schedule);
  let promises = schedule.map(c =>
    new Promise(function(resolve, reject) {
      console.log(toGoogleEvent(c));
      return;


      (
        (c.googleEventId) ?
        // - if the class already has an associated Google Event ID, update the google
        // event with the information in the Firebase object.
        gapi.client.calendar.events.update({
          calendarId: CALENDAR_ID,
          eventId: c.googleEventId,
          resource: toGoogleEvent(c),
        })
        :
        // - if not, then create a brand new Google event and update it with this
        // information, saving the Google event ID in the class's Firebase object.
        gapi.client.calendar.events.insert({
          calendarId: CALENDAR_ID,
          resource: toGoogleEvent(c),
        })
      ).execute((event) => {
        // FIXME error handling
        resolve(event);
      });
    })
  );

  return Promise.all(promises);
}

export default {
  loadApi,
  updateGoogle,
  // ourGapi,

  config,
  SCOPES,
  DISCOVERY_DOCS,
  RECURRENCE_DAYS,
};
