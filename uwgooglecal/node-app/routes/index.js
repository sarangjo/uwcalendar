var express = require('express');
var router = express.Router();

/* GET home page. */
router.get('/', function(req, res, next) {
  res.render('index', { title: 'UW Google Cal', link: 'http://google.com' });
});

/* GET hello world page. */
router.get('/helloworld', function(req, res) {
  res.render('helloworld', { title: 'Hello World!' });
})

/* GET Userlist page. */
router.get('/userlist', function(req, res) {
  var db = req.db;
  var collection = db.get('usercollection');
  collection.find({}, {}, function(e, docs) {
    res.render('userlist', {
      "userlist" : docs
    });
  });
});

/* GET New User page. */
router.get('/newuser', function(req, res) {
  res.render('newuser', { title: "New User" });
});

/* POST to Add User service. */
router.post('/adduser', function(req, res) {
  var db = req.db;

  var user = req.body.username;
  var email = req.body.useremail;

  var collection = db.get('usercollection');

  collection.insert({
    "username": user,
    "email": email
  }, function(err, doc) {
    if (err) {
      res.send("Error. Lel.");
    } else {
      res.redirect('userlist');
    }
  });
});

module.exports = router;
