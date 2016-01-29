var express = require('express');
var router = express.Router();

var fs = require('fs');
var google = require('googleapis');
var googleAuth = require('google-auth-library');

var SCOPES = ['https://www.googleapis.com/auth/calendar'];
var TOKEN_DIR = 'credentials/';
var TOKEN_PATH = TOKEN_DIR + 'uwgooglecal-cred.json';

// Auth variables
var clientSecret;
var clientId;
var redirectUrl;
var auth = new googleAuth();
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
  clientSecret = credentials.installed.client_secret;
  clientId = credentials.installed.client_id;
  redirectUrl = credentials.installed.redirect_uris[1];
  
  oauth2Client = new auth.OAuth2(clientId, clientSecret, redirectUrl);

  // Check if we have previously stored a token.
  fs.readFile(TOKEN_PATH, function(err, token) {
    if (err) {
      var authUrl = oauth2Client.generateAuthUrl({
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

/* GET userauthorized page. */
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

// Saves the token
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

router.get('/events', function(req, res) {
  if (oauth2Client == null) {
    res.redirect('/login');
  }
  listEvents(req, res, oauth2Client, renderEvents);
});

function listEvents(req, res, auth, callback) {
  var calendar = google.calendar('v3');
  var result = {};
  calendar.events.list({
    auth: auth,
    calendarId: 'primary',
    timeMin: (new Date()).toISOString(),
    maxResults: 1,
    singleEvents: true
  }, function(err, response) {
    if (err) {
      console.log('The API returned an error: ' + err);
      return;
    }
    var events = response.items;
    if (events.length == 0) {
      console.log('No upcoming events found.');
    } else {
      /*console.log('Upcoming 10 events:');
      for (var i = 0; i < events.length; i++) {
        var event = events[i];
        var start = event.start.dateTime || event.start.date;
        console.log('%s - %s', start, event.summary);
      }*/
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

router.post('/addevent', function(req, res) {
  var name = req.body.eventname;
  var start = new Date(req.body.eventstart);
  var end = new Date();

  // one hour away
  end.setTime(start.getTime() + (60*60*1000));

  console.log("Start: " + start);

  /*var end = start.substring(0, 11);
  end += "23";
  end += start.substring(13);*/

  console.log("End: " + end);

  var event = {
    'summary': name,
    'start': {
      'dateTime': start.toISOString(),
      'timeZone': 'America/Los_Angeles',
    },
    'end': {
      'dateTime': '2016-01-28T17:00:00-07:00',
      'timeZone': 'America/Los_Angeles',
    }
  };

  var calendar = google.calendar('v3');

  calendar.events.insert({
    auth: oauth2Client,
    calendarId: 'primary',
    resource: event
  }, function(err, event) {
    if (err) {
      res.send('There was an error contacting the Calendar service: ' + err);
      return;
    }
    //res.redirect('events');
  });
});

router.get('/logout', function(req, res) {
  oauth2Client = null;

  res.redirect('/');
});

module.exports = router;
