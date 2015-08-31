ICOS Carbon Portal metadata service
===================================

Metadata service for hosting, mantaining and querying information about things like ICOS stations, people, instruments, etc.
Not ready to be used yet.

Getting started with the front-end part
---------------------------------------
- Install `Node.js` as instructed [here](https://github.com/nodesource/distributions)
- Clone this repository: `git clone git@github.com:ICOS-Carbon-Portal/meta.git`
- `cd meta`
- Install Node.js dependencies: `npm install`
- Now you can run Gulp tasks: `npm run gulp <task>`

Getting started with the back-end part
--------------------------------------
- Add the jar from [here](https://github.com/ignazio1977/hermit-reasoner/tree/releases/1.3.8.5-SNAPSHOT) to lib folder in the project's root
- Set up a Docker container with PostgreSQL for RDF log (see the [infrastructure project](https://github.com/ICOS-Carbon-Portal/infrastructure/tree/master/rdflogdb))
- Make a copy of `example.application.conf` file in the project root named `application.conf` and edit it to suit your environment. For some default config values, see `application.conf` in `src/main/resources/`. For deployment, make sure there is a relevant `application.conf` in the JVM's working directory.
- Run sbt
- In the sbt console, run `~re-start` for continuous local rebuilds and server restarts

Using the webapp
----------------
To get the authentication cookie from Cpauth:
`curl --cookie-jar cookies.txt --data "mail=<user email>&password=<password>" https://cpauth.icos-cp.eu/password/login`
The resulting `cookies.txt` file must be edited if you want to use it for tests against localhost via HTTP.

To test the metadata upload (`upload.json` and `cookies.txt` must be in the current directory):
`curl --cookie cookies.txt -H "Content-Type: application/json" -X POST -d @upload.json 127.0.0.1:9094/upload`
