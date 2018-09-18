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
  // Calculate start date
  let startDate;
  let i = 0;
  // Go through all of the days of this class and find the first one that's actually
  // in the quarter
  while (true) {
    let dayOfWeek = RECURRENCE_DAYS.indexOf(c.days[i % c.days.length]) + 1; // +1 because Sunday is 0
    // Include potential of overflow if the start date is in the next week
    dayOfWeek += 7 * Math.floor(i / c.days.length);

    startDate = moment(d.start).day(dayOfWeek);
    if (startDate.isAfter(d.start)) {
      // We found the correct start date!
      break;
    }
    // If not, keep looking through this class's days
    i++;
  }

  // RFC3339 formatting
  let toString = (t) => moment(startDate).set('hour', t.substring(0, 2)).set('minute', t.substring(3))
    .format("YYYY-MM-DDTHH:mm:ss") + "Z";

  // TODO timezone
  return {
    summary: c.name,
    location: c.location,
    start: {
      dateTime: toString(c.start),
      timeZone: 'America/Los_Angeles',
    },
    end: {
      dateTime: toString(c.end),
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
      ).execute((o) => {
        if (o.error) {
          reject(o);
        } else {
          resolve(o);
        }
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
