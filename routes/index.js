var express = require('express');
var router = express.Router();

var fs = require('fs');
var google = require('googleapis');
var googleAuth = require('google-auth-library');

// MD5 hashing for password
var md5 = require("blueimp-md5");

var SCOPES = ['https://www.googleapis.com/auth/calendar'];
var TOKEN_DIR = 'credentials/';
var TOKEN_PATH = TOKEN_DIR + 'uwgooglecal-cred.json';

// TODO: Make this DST-dependent
var TIMEZONE = "-07:00";

var QTR_DETAILS = 'details/qtr-details.json';
var quarterInfo = null;

// Authorization client
var oauth2Client = null;

// Number of classes
var N_OF_CLASSES = 4;

// Current user
var user = null;

//////////////// HELPERS ////////////////

// Standard String format: 2016-01-28T17:00:00-07:00
function dateToString(date) {
	var str = date.toISOString().substring(0, 19);
	return str + TIMEZONE;
}

/////////////////////////////////////////

/* GET home page. */
router.get('/', function (req, res) {
	res.render('pages/index', { title: 'UW Calendar', loggedout: true });
});

/* GET login page */
router.get('/login', function (req, res) {
	if (user) {
		res.redirect('/home');
		return;
	}
	res.render('pages/login', { title: 'Login', loggedout: true });
});

/* POST login form */
router.post('/login', function (req, res) {
	// TODO: add password as a parameter for the find operation
	req.db.get('usercollection').find( {email: req.body.email}, {}, function (err,results) {
  	if (err) {
  		console.log("Db error.");
  		console.log(err);
  		res.redirect('/login');
  		return;
  	}
  	// results should be 1 element long
  	if (results.length > 1) {
  		console.log("Internal db error: more than 1 user.");
  		res.redirect('/login');
  		return;
  	} else if (results.length < 1) {
  		console.log("Couldn't find user.");
  		res.redirect('/login');
  		return;
  	}
		var result = results[0];
		console.log("Logging in with: " + req.body.email);
  	finishLogin(req, res, result);
  });
});

// Finishes up logging in with the resulting user, saving it to the
// `user` variable
function finishLogin(req, res, result) {
	//req.session.user = result;
	user = result;
	res.redirect('/home');
}

/* GET signup page */
router.get('/signup', function (req, res) {
	if (user) {
		res.redirect('/home');
		return;
	}
	res.render('pages/signup', { title: 'Signup', loggedout: true });
});

/* POST login form */
router.post('/signup', function (req, res) {
	var collection = req.db.get('usercollection');
	
	// TODO: check existing email

	// TODO: secure password
	var passwordhash = md5(req.body.password);

  collection.insert({
  	email: req.body.email,
  	password: passwordhash
  }, function (err, doc) {
  	if (err) {
  		res.send("There was a problem.");
  	} else {
  		console.log("User signed up!");
  		console.log(doc);
  		finishLogin(req, res, doc);
  	}
  });
});

/* GET logout page. */
router.get('/logout', function (req, res) {
	// Disassociate the oauth2Client's token from the current user
	oauth2Client.token = null;
	
	// Remove user from session
	user = null;

	console.log("Logged out.");

	res.redirect('/');
});

/* GET home page. */
router.get('/home', function (req, res) {
	if (user == null) {
		// logged out
		res.redirect('/login');
		return;
	}

	if (oauth2Client == null) {
		console.log("App hasn't been setup yet.");
		setupAppForGoogle(req, res, addQtrInfo);
		return;
	}
	addQtrInfo(req, res);
});

/**
 * Setup our app to access the calendar, and continue the workflow.
 */
function setupAppForGoogle(req, res, callback) {
	// Load client secrets from a local file.
	fs.readFile('client_secret.json', function (err, content) {
		if (err) {
			console.log('Error loading client secret file: ' + err);
			return;
		}
		// Setup our client with the loaded credentials.
		var credentials = JSON.parse(content);

		var clientSecret = credentials.installed.client_secret;
		var clientId = credentials.installed.client_id;
		// 0: default page, 1: localhost
		var redirectUrl = credentials.installed.redirect_uris[1];
		var auth = new googleAuth();

		oauth2Client = new auth.OAuth2(clientId, clientSecret, redirectUrl);

		console.log("App has been set up.");
		callback(req, res);
	});
}

function addQtrInfo(req, res) {
	// Adding the quarter details object to the session
	if (quarterInfo == null) {
		fs.readFile(QTR_DETAILS, function (err, qtrDetailsFileContent) {
			if (err) {
				console.log('Error loading quarter info: ' + err);
				return;
			}
			quarterInfo = JSON.parse(qtrDetailsFileContent);
			renderHome(req, res);
		});
		return;
	}
	renderHome(req, res);
}

function renderHome(req, res) {
	res.render('pages/home', {
    "user": user,
    loggedout: false
  });
}

/* GET googleauth page */
router.get('/googleauth', function (req, res) {
	var authUrl = oauth2Client.generateAuthUrl({access_type: 'offline',scope: SCOPES});
	console.log("Authorization URL generated.");

	res.render('pages/googleauth', {
		title: 'Google Auth',
		auth: authUrl,
		loggedout: true
	});
});

/* GET authorized page. This is after retrieving the OAuth2 token. */
router.get('/authorized', function (req, res) {
	// The auth token is in the "code" GET param
	var token = req.query.code;

	oauth2Client.getToken(token, function (err, token) {
		if (err) {
			console.log("Access token error", err);
			renderFailedAuth(res, false);
			return;
		}
		oauth2Client.credentials = token;
		
		// Store in database
		var collection = req.db.get('usercollection');
		collection.update({email: user.email}, {$set:{googleauth:token}});

		// Store in local user object
		user.googleauth = token;

		res.redirect('classes');
	});
});

// Render what needs to be rendered if the user was not actually authorized
function renderFailedAuth(res) {
	var params = { result: "Failed!", loggedout: true };
	res.render('pages/authorize', params );
}

/* GET classes page. */
router.get('/classes', function (req, res) {
	if (user == null) {
		// logged out
		res.redirect('/login');
		return;
	}

	// Read in the JSON token from the user
	var token = user.googleauth;
	
	if (typeof token == 'undefined') {
		console.log("The user has not authorized with Google.");
		res.redirect('/googleauth');
		return;
	}
	oauth2Client.credentials = token;

	// Setting up parameters to send to the page
	var params = {};
	params.error = req.session.error;
	params.message = req.session.message;
	params.oldBody = oldBody;

	// Number of classes to allow
	params.nOfClasses = N_OF_CLASSES;

	// Add all of the classes to be shown to the params object
	req.db.get('usercollection').find( {email: user.email}, {}, function (err,results) {
		if (err || results.length != 1) {
			res.redirect('login');
			return;
		}
		user = results[0];

		params.schedule = user.schedule;

		renderClasses(req, res, params);
	});
});

/** 
 * Renders the classes given a params object.
 */
function renderClasses(req, res, params) {
	params.title = 'Classes';
	res.render('pages/classes', params);
}

var oldBody = {};

/* POST addclass page. Adds new event and redirects to /classes page. */
router.post('/addclass', function (req, res) {
	req.session.message = [];

	// Error-checking
	req.session.error = {};

	for (var i = 0; i < N_OF_CLASSES; i++) {
		// Do we care about this class?
		if (req.body.hasOwnProperty("classadd" + i)) {
			// Check for errors, in the process updating req
			checkErrors(req, i);
		} else {
			console.log("Skipping class #" + i);
		}
	}

	if (Object.keys(req.session.error).length > 0) {
		// Save old body, to be passed back again as a parameter later
		oldBody = req.body;
		res.redirect('/classes');
		return;
	}

	// We are error-free!
	// Update our local copy of the user
	req.db.get('usercollection').find( {email: user.email}, {}, function (err,results) {
  	if (err || results.length != 1) {
  		res.redirect('/login');
  		return;
  	}
		user = results[0];
		
		saveClass(req, res, 0);
	});
});

/**
 * Saves a new class to the calendar.
 * 
 * @param {Object} req
 *		the request object
 * @param {Object} res
 *		the result object
 * @param {Number} index
 *		index of the class
 */
function saveClass(req, res, index) {
	// Check to see if all classes have been added
	if (index >= N_OF_CLASSES) {
		console.log("Done adding classes.");
		console.log("Clearing request body.");
		oldBody = {};
		res.redirect('/classes');
		return;
	}

	// Classes to be skipped, i.e. as the kids call it these days "tampa'd"
	if (!(req.body.hasOwnProperty("classadd" + index))) {
		saveClass(req, res, index + 1);
		return;
	}

	// Insert the class into the Google calendar
	google.calendar('v3').events.insert({
		auth: oauth2Client,
		calendarId: 'primary',
		resource: createGoogleClass(req, index)
	}, function (err, event) {
		// Error and message addition to the req object
		if (err) {
			console.log('There was an error contacting the Calendar service: ' + err);
			req.session.error[index + ""] = err;
			// Abort!
			res.redirect('/classes');
		} else {
			console.log("Added to GCal");

			// Insert the class into the database
			if (!user.hasOwnProperty("schedule"))
				user.schedule = {};
			if (!user.schedule.hasOwnProperty(req.body.classqtr))
				user.schedule[req.body.classqtr] = [];
			var dbClass = createDbClass(req, index, event);
			user.schedule[req.body.classqtr].push(dbClass);

			req.db.get('usercollection').update({email: user.email}, {$set:{schedule:user.schedule}});
			console.log("Added to database");

			// Wrap up and move on
			var message = "Class #" + index + " added!";
			req.session.message.push(message);
			saveClass(req, res, index + 1);
		}
	});
}

// Efficient way of representing which days a class is repeated, 
// using bitwise representations for each day
var dbDaysMap = {monday: 16, tuesday: 8, wednesday: 4, thursday: 2, friday: 1};

/**
 * Creates a db class to be added to the database.
 */
function createDbClass(req, index, googleEvent) {
	var days = 0;

	// Go through and compile the bitwise vector
	for (var day in dbDaysMap) {
	  if (dbDaysMap.hasOwnProperty(day)) {
	  	if (req.body.hasOwnProperty(day + index)) {
				days = days | dbDaysMap[day];
			}
	  }
	}

	var dbClass = {
		name: req.body["classname" + index],
		location: req.body["classlocation" + index],
		days: days,
		start: req.body["classstart" + index],
		end: req.body["classend" + index],
		googleEventId: googleEvent.id
	};

	console.log(dbClass);

	return dbClass;
}

/**
 * Constructs a class from the request body.
 * 
 * @return {Object} the created class
 */
function createGoogleClass(req, index) {
	var addedClass = {
		summary: req.body["classname" + index],
		location: req.body["classlocation" + index],
	};

	// RECURRENCE DETAILS
	var qtrDetails = quarterInfo[req.body.classqtr];
	var enddate = (qtrDetails.end).replace(new RegExp("-", 'g'), "");
	var days = parseDays(req.body, index);
	addedClass.recurrence = ['RRULE:FREQ=WEEKLY;UNTIL=' + enddate + 'T115959Z;WKST=SU;BYDAY=' + days];

	// Expand start/end time to include full date
	start = qtrDetails.start + "T" + req.body["classstart" + index] + ":00" + TIMEZONE;
	end = start.substring(0, 11) + req.body["classend" + index] + start.substring(16);
	addedClass.start = { dateTime: start, timeZone: 'America/Los_Angeles' };
	addedClass.end =  { dateTime: end, timeZone: 'America/Los_Angeles' };

	return addedClass;
}

/**
 * Returns whether the request contents have an error.
 * 
 * @param {Number} index the index of the class being added, 0-based
 */
function checkErrors(req, index) {
	var myErrors = [];

	if (quarterInfo == null) {
		myErrors.push("Internal error. Quarter info invalid.");
	}

	if (req.body["classname" + index] == "") {
		myErrors.push("Enter a valid class name.");
	}
	
	if (req.body["classlocation" + index] == "") {
		myErrors.push("Enter a valid class location.");
	}
	
	var start = req.body["classstart" + index];
	var end = req.body["classend" + index];
	var timesOK = timesValid(start,end);
	if (!timesOK) {
			myErrors.push("Enter valid start/end times.");
	}	
	
	var days = parseDays(req.body, index);
	if (days == "") {
		myErrors.push("Select at least 1 day.");
	}

	if (myErrors.length != 0) {
		req.session.error[index + ""] = myErrors;
		return true;
	} else {
		return false;
	}
}

/**
 * Verifies that start is before end
 * Format 09:30, 21:30
 * @returns {boolean} true if the times are valid
 */
function timesValid(start, end) {
	if (start == '' || end == '') return false;
	var starthr = parseInt(start.substring(0,2));
	var endhr = parseInt(end.substring(0,2));
	if (starthr > endhr) return false;
	else if (starthr < endhr) return true;
	else return (parseInt(start.substring(3))) < (parseInt(end.substring(3)));
}

/**
 * Parses out the days string from the request body
 */
function parseDays(reqBody, index) {
	var start = true;
	var days = "";

	if (reqBody.hasOwnProperty("monday" + index)) {
		start = false;
		days += "MO";
	}
	if (reqBody.hasOwnProperty("tuesday" + index)) {
		if (!start) days += ",";
		else start = false;
		days += "TU";
	}
	if (reqBody.hasOwnProperty("wednesday" + index)) {
		if (!start)	days += ",";
		else start = false;
		days += "WE";
	}
	if (reqBody.hasOwnProperty("thursday" + index)) {
		if (!start) days += ",";
		else start = false;
		days += "TH";
	}
	if (reqBody.hasOwnProperty("friday" + index)) {
		if (!start) days += ",";
		else start = false;
		days += "FR";
	}

	return days;
}

/* GET deleteClass page. Deletes class and redirects to /classes. */
router.get('/deleteclass', function (req, res) {
	var i = req.query.index;
	var qtr = req.query.qtr;
	var classToDelete = user.schedule[qtr][i];

	console.log(classToDelete);

	// Delete from gcal
	google.calendar('v3').events.delete({
		auth: oauth2Client,
		calendarId: 'primary',
		eventId: classToDelete.googleEventId
	}, function (err, response) {
		if (err) {
			console.log("Could not delete Google event:");
			console.log(err);
		} else {
			console.log("Deleted Google event.");

			// Delete from db
			user.schedule[qtr].splice(i, 1);

			if (user.schedule[qtr].length == 0) {
				delete user.schedule[qtr];
			}

			req.db.get('usercollection').update({email: user.email},  {$set:{schedule:user.schedule}});
			console.log("Deleted from db.");
		}
		res.redirect('/classes');
	});
});

///////// OLD FUNCTIONS /////////

/**
 * Lists events in the console, and one on the page
 */
function listEvents(req, res, auth, callback) {
	var calendar = google.calendar('v3');
	var minDate = dateToString(new Date(2016, 1, 17, 0, 0, 0));
	var maxDate = dateToString(new Date(2016, 1, 17, 11, 59, 59));

	// List the next 10 events in the primary calendar.
	calendar.events.list({
		auth: auth,
		calendarId: 'primary',
		timeMin: minDate,
		timeMax: maxDate,
		maxResults: 10,
		singleEvents: true,
		orderBy: 'startTime'
	}, function (err, response) {
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

// Saves the user auth token locally
// TODO: save in database?
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

/////////////////////////////////

module.exports = router;
