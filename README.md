# UW Calendar

A calendar optimizing application for UW students, using the Google Calendar API and more.

## Build Process

Run `npm install` from the root directory to install `bin/www` and the required `node_modules`.

### Notes

- When creating the `/details` folder, make sure that the quarter details start date is exactly the first date, whereas the end is buffered by one day. Kinda like the `lo` inclusive, `hi` exclusive.


usercollection [
  {
    email: "",
    password: "",
    googleauth: {},
    schedule: {
      "wi16": [
        {
          name: "",
          location: "",
          days: 0,
          start: "",
          end: "",
          googleEventId: ""
        }, {}, {}
      ],
      "sp16": [
        {}, {}, {}
      ]
    }
  }
]
