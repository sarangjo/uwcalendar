var express = require('express');
var router = express.Router();

var fs = require('fs');
var google = require('googleapis');
var googleAuth = require('google-auth-library');

var SCOPES = ['https://www.googleapis.com/auth/calendar'];
var TOKEN_DIR = 'credentials/';
var TOKEN_PATH = TOKEN_DIR + 'uwgooglecal-cred.json';

var TIMEZONE = "-08:00";

// Authorization client
var oauth2Client = null;

/* GET home page. */
router.get('/', function(req, res, next) {
  res.render('index', { title: 'UW Google Cal', loggedout: true });
});

/* GET login page */
router.get('/login', function(req, res, next) {
  // Load client secrets from a local file.
  fs.readFile('client_secret.json', function(err, content) {
    if (err) {
      console.log('Error loading client secret file: ' + err);
      return;
    }
    // Authorize a client with the loaded credentials, and render
    // the login screen if the user isn't already authorized
    authorize(res, JSON.parse(content), renderLogin);
  }); 
});

// Authorize a client to access the calendar
// authCallback is where to come after the user has been authenticated
function authorize(res , credentials, authCallback) {
  var clientSecret = credentials.installed.client_secret;
  var clientId = credentials.installed.client_id;
  // 0: default page, 1: localhost
  var redirectUrl = credentials.installed.redirect_uris[1];
  var auth = new googleAuth();

  oauth2Client = new auth.OAuth2(clientId, clientSecret, redirectUrl);

  // Check if we have previously stored a token.
  fs.readFile(TOKEN_PATH, function(err, token) {
    if (err) {
      var authUrl = oauth2Client.generateAuthUrl({
        access_type: 'offline',
        scope: SCOPES
      });
      authCallback(res, authUrl);
    } else {
      oauth2Client.credentials = JSON.parse(token);
      // Jump to post-authorize
      renderAuthorized(res, true);
    }
  });
}

// Renders the login screen with the authorization URL.
function renderLogin(res, authUrl) {
  res.render('login', { title: 'Login', auth: authUrl, loggedout: true });
}

/* GET authorized page. This is after retrieving the OAuth2 token. */
router.get('/authorized', function(req, res) {
  // The auth token is in the "code" GET param
  var token = req.query.code;

  oauth2Client.getToken(token, function(err, token) {
    if (err) {
      console.log("Access token error", err);
      return;
    }

    oauth2Client.credentials = token;
    storeToken(token);
    renderAuthorized(res, true);
  });
});

// Render what needs to be rendered once the user has been authorized
function renderAuthorized(res, isAuthorized) {
  if (isAuthorized)
    res.redirect('events');
  else {
    var params = { result: "Failed!", loggedout: true };
    res.render('authorize', params );
  }
}

// Saves the token locally
function storeToken(token) {
  try {
    fs.mkdirSync(TOKEN_DIR);
  } catch (err) {
    if (err.code != 'EEXIST') {
      throw err;
    }
  }
  fs.writeFile(TOKEN_PATH, JSON.stringify(token));
  console.log('Token stored to ' + TOKEN_PATH);
}

// Standard String format: 2016-01-28T17:00:00-07:00
function dateToString(date) {
  var str = date.toISOString().substring(0, 19);
  return str + TIMEZONE;
}

/* GET events page. */
router.get('/events', function(req, res) {
  if (oauth2Client == null) {
    res.redirect('/login');
  }
  listCals(req, res, oauth2Client, renderEvents);
});

function listCals(req, res, auth, callback) {
  /*var calendar = google.calendar('v3');
  
  // Figure out which calendars there are
  calendar.calendarList.list({
    auth: auth
  }, function(err, response) {
    if (err)  {
      console.log('The API returned an error: ' + err);
      res.redirect('/');
    }

    var cals = response.list;

    for (var i = 0; i < cals.length; i++) {
      var cal = cals[i];
      console.log(cal.id);
    }*/

    listEvents(req, res, auth, callback);
  /*});*/
}

function listEvents(req, res, auth, callback) {
  var calendar = google.calendar('v3');
  var nowDate = dateToString(new Date());

  console.log(nowDate);

  // List the next 10 events in the primary calendar.
  calendar.events.list({
    auth: auth,
    calendarId: 'primary',
    timeMin: nowDate,
    maxResults: 10,
    singleEvents: true
  }, function(err, response) {
    if (err) {
      console.log('The API returned an error: ' + err);
      res.redirect('/');
    }
    var events = response.items;
    var result = {};
    if (events.length == 0) {
      console.log('No upcoming events found.');
    } else {
      console.log('Upcoming 10 events:');
      for (var i = 0; i < events.length; i++) {
        var event = events[i];
        var start = event.start.dateTime || event.start.date;
        console.log('%s - %s', start, event.summary);
      }
      var event = events[0];
      result.eventsummary = event.summary;
      result.eventdate = (event.start.dateTime || event.start.date);
    }
    callback(req, res, result);
  });
}

function renderEvents(req, res, params) {
  params.title = 'Events';
  res.render('events', params);
}

/* POST addevent page. Adds new event and redirects to /events page. */
router.post('/addevent', function(req, res) {
  var name = req.body.eventname;
  var start = req.body.eventstart + ":00" + TIMEZONE;
  var end = start;

  // one hour away
  var starthr = parseInt(start.substring(11, 13));

  console.log("Start hour" + starthr);

  var end = start.substring(0, 11);
  end += starthr+1;
  end += start.substring(13);

  var event = {
    'summary': name,
    'start': {
      'dateTime': start,
      'timeZone': 'America/Los_Angeles',
    },
    'end': {
      'dateTime': end,
      'timeZone': 'America/Los_Angeles',
    }
  };

  var calendar = google.calendar('v3');

  calendar.events.insert({
    auth: oauth2Client,
    calendarId: 'primary',
    resource: event
  }, function(err, event) {
    console.log("Event logged!");
    if (err) {
      console.log('There was an error contacting the Calendar service: ' + err);
    }
    res.redirect('events');
  });
});

/* GET logout page. Deletes OAuth2 token. */
router.get('/logout', function(req, res) {
  oauth2Client = null;

  // delete authorization token
  fs.unlinkSync(TOKEN_PATH);

  console.log("deleted auth token");

  res.redirect('/');
});

module.exports = router;
