# ICOS Carbon Portal metadata service


Metadata service for hosting, mantaining and querying information about things like ICOS stations, people, instruments, archived data objects, etc.
It is deployed to **https://meta.icos-cp.eu/** with different services accessible via different paths:

- [/uploadgui/](https://meta.icos-cp.eu/uploadgui/): web application for data/document object upload and collection creation (see instructions for manual upload below). 
- [/labeling/](https://meta.icos-cp.eu/labeling/): ICOS Station Labeling Step 1 web application
- [/edit/stationentry/](https://meta.icos-cp.eu/edit/stationentry/): provisional station information entry app for ICOS Head Office
- [/edit/labeling/](https://meta.icos-cp.eu/edit/labeling/): administrative interface for station labeling metadata
- [/edit/cpmeta/](https://meta.icos-cp.eu/edit/cpmeta/): editing app for Carbon Portal's metadata
- [/edit/otcentry/](https://meta.icos-cp.eu/edit/otcentry/): editing app for OTC's metadata (used for ICOS metadata flow, testing mode for now)
- [/edit/icosmeta/](https://meta.icos-cp.eu/edit/icosmeta/): viewing app for the test ICOS metadata produced by the metadata flow machinery
- [/sparqlclient/](https://meta.icos-cp.eu/sparqlclient/): GUI for running SPARQL queries against Carbon Portal's metadata database (RDF store)
- (example) [/objects/OPun_V09Pcat5jomRRF-5o0H](https://meta.icos-cp.eu/objects/OPun_V09Pcat5jomRRF-5o0H): landing pages for data objects registered with Carbon Portal
- (example) [/ontologies/cpmeta/DataObjectSpec](https://meta.icos-cp.eu/ontologies/cpmeta/DataObjectSpec): "landing pages" for metadata-schema concepts from Carbon Portal's ontologies
- **/upload**: the HTTP API to upload metadata packages for data object registration (see below)

Additionally, this repository contains code for the following visualization web apps (deployed as static pages):

- [Map of the provisional ICOS stations](https://static.icos-cp.eu/share/stationsproj/)
- [Table with the provisional station info](https://static.icos-cp.eu/share/stations/table.html)

---
## Upload instructions (manual)
Manual uploads of data/document objects and collection creation can be performed using [UploadGUI](https://meta.icos-cp.eu/uploadgui/) web app. Users need permissions and prior design of data object specifications in collaboration with the CP. Metadata of existing objects and collections can be updated later, using the same app.

---
## Upload instructions (scripting)

This section describes the complete, general 2-step workflow for registering and uploading a data object to the Carbon Portal for archival, PID minting and possibly for being served by various data services.

### Authentication

Before you begin, make sure with the Carbon Portal's (CP) technical staff that the service is configured to accept your kind of data objects, and that there is a user account associated with the uploads you are going to make. Log in to [CPauth](https://cpauth.icos-cp.eu/) with this account using the username/password provided to you. You will be redirected to a page showing, among other things, your API token. This token is what your software must use to authenticate itself against CP services. It has validity period of 100000 seconds (about 27.8 hours).

Alternatively, the authentication token can be fetched in an automation-friendly way by HTTP-POSTing the username and password as HTML form fields `mail` and `password` to **https://cpauth.icos-cp.eu/password/login**. For example, using a popular command-line tool `curl`, it can be done as follows:

`$ curl --cookie-jar cookies.txt --data "mail=<user email>&password=<password>" https://cpauth.icos-cp.eu/password/login`

The resulting `cookies.txt` file will then contain the authentication cookie token, which can be automatically resent during later requests. (Note for developers: the file must be edited if you want to use it for tests against `localhost`).

Naturally, instead of `curl`, one can automate this process (as well as all the next steps) using any other HTTP-capable tool or programming language.

### Registering the metadata package

The first step of the 2-step upload workflow is preparing and uploading a metadata package for your data object. The package is a JSON document whose exact content depends on the data object's ICOS data level. For example, for L0 and L1 the metadata has the following format:

```json
{
	"submitterId": "ATC",
	"hashSum": "7e14552660931a5bf16f86ad6984f15df9b13efb5b3663afc48c47a07e7739c6",
	"fileName": "L0test.csv",
	"specificInfo": {
		"station": "http://meta.icos-cp.eu/resources/stations/AS_SMR",
		"acquisitionInterval": {
			"start": "2008-09-01T00:00:00.000Z",
			"stop": "2008-12-31T23:59:59.999Z"
		},
		"instrument": "http://meta.icos-cp.eu/resources/instruments/ATC_181",
		"samplingHeight": 54.8,
		"production": {
			"creator": "http://meta.icos-cp.eu/resources/people/Lynn_Hazan",
			"contributors": [],
			"hostOrganization": "http://meta.icos-cp.eu/resources/organizations/ATC",
			"comment": "free text",
			"creationDate": "2017-12-01T12:00:00.000Z",
			"sources": ["utw3ah9Fo7_Sp7BN5i8z2vbK"]
		}
	},
	"objectSpecification": "http://meta.icos-cp.eu/resources/cpmeta/atcCo2NrtDataObject",
	"isNextVersionOf": "MAp1ftC4mItuNXH3xmAe7jZk",
	"preExistingDoi": "10.1594/PANGAEA.865618",
	"references": {
		"keywords": []
	}
}
```

Clarifications:

- `submitterId` will be provided by the CP's technical people. This is not the same as username for logging in with CPauth.
- `hashSum` is so-called SHA256 hashsum. It can be easily computed from command line using `sha256sum` tool on most Unix-based systems.
- `fileName` is required but can be freely chosen by you. Every data object is stored and distributed as a single file.
- `specificInfo` for level 0, 1 or 2
	- `station` is CP's URL representing the station that acquired the data. The lists of stations can be found for example here: [ATC](https://meta.icos-cp.eu/ontologies/cpmeta/AS), [ETC](https://meta.icos-cp.eu/ontologies/cpmeta/ES), [OTC](https://meta.icos-cp.eu/ontologies/cpmeta/OS).
	- `acquisitionInterval` (optional) is the temporal interval during which the actual measurement was performed. Required for data objects that do not get ingested completely by CP (i.e. with parsing and internal binary representation to support previews).
	- `instrument` (optional) is the URL of the metadata entity representing the instrument used to perform the measurement resulting in this data object.
	- `samplingHeight` (optional) is the height of the sampling (e.g. height of inlets for gas collection) in meters.
	- `production` (optional) is production provenance object. It is desirable for data levels 1 and higher.
		- `creator` can be an organization or a person URL.
		- `contributors` must be present but can be empty. Can contain organization or people URLs.
		- `hostOrganization` is optional.
		- `comment` is an optional free text.
		- `creationDate` is an ISO 8601 time stamp.
		- `sources` (optional) is an array of source data objects, that the current one was produced from, referred to as hashsums. Both hex- and base64url representations are accepted, in either complete (32-byte) or shortened (18-byte) versions.
	- `nRows` is the number of data rows (the total number of rows minus the number of header rows) and is required for some specifications where the files will be parsed and ingested for preview.
- `specificInfo` for level 3
	- `title` is a required string.
	- `description` is an optional string.
	- `spatial` is the spacial coverage or a url to another spacial coverage.
		- `min` containing numeric `lat` and `lon` (WGS84).
		- `max` containing numeric `lat` and `lon` (WGS84).
		- `label` is a optional string to describe the spacial coverage.
	- `temporal` is the time coverage.
		- `interval` containing `start` and `stop` timestamps.
		- `resolution` is a string indicating the resolution of the dataset.
	- `production` is similar to `production` for levels 1 and 2.
	- `customLandingPage` is an optional url linking to the data hosted somewhere else.
- `objectSpecification` has to be prepared and provided by CP, but with your help. It must be specific to every kind of data object that you want to upload. Please get in touch with CP about it.
- `isNextVersionOf` is optional. It should be used if you are uploading a new version of a data object(s) that is(are) already present. The value is the SHA256 hashsum of the older data object (or an array of the hashsums, if they are more than one). Both hex- and base64url representations are accepted, in either complete (32-byte) or shortened (18-byte) versions.
- `preExistingDoi` (optional) allows specifying a DOI for the data object, for example if it is also hosted elsewhere and already has a preferred DOI, or if a dedicated DOI has been minted for the object before uploading it to CP.
- `references` (optional) JSON object with additional "library-like" information; the list of its properties is planned to grow in the future.
- `keywords` (optional) an array of strings to be used as keywords specific to this particular object. Please note that CP metadata allows specifying keywords also on the data object specification (data type) level, and on the project level. Keywords common to all data objects of a certain data type should be associated directly with the corresponding specification (this is done by CP staff on request from the data uploaders).

In HTTP protocol terms, the metadata package upload is performed by HTTP-POSTing its contents to `https://meta.icos-cp.eu/upload` with `application/json` content type and the authentication cookie. For example, using `curl` (`metaPackage.json` and `cookies.txt` must be in the current directory), it can be done as follows:

`$ curl --cookie cookies.txt -H "Content-Type: application/json" -X POST -d @metaPackage.json https://meta.icos-cp.eu/upload`

Alternatively, the CPauth cookie can be supplied explicitly:

`$ curl -H "Cookie: <cookie-assignment>" -H "Content-Type: application/json" -X POST -d @metaPackage.json https://meta.icos-cp.eu/upload`

### Uploading the data object
Uploading the data object itself is a simple step performed against the CP's Data service **https://data.icos-cp.eu/**.
Proceed with the upload as instructed [here](https://github.com/ICOS-Carbon-Portal/data#instruction-for-uploading-icos-data-objects)

### Uploading document objects
In addition to data objects who have properties as data level, data object specification, acquisition and production provenance, there is a use case for uploading supplementary materials like pdf documents with hardware specifications, methodology descriptions, policies and other reference information.
To provide for this, CP supports upload of document objects.
The upload procedure is completely analogous to data object uploads, the only difference being the absence of `specificInfo` and `objectSpecification` properties in the metadata package.

### Creating a static collection
Carbon Portal supports creation of static collections with constant lists of immutable data objects or other static collections. The process of creating a static collection is similar to step 1 of data object upload. Here are the expected contents of the metadata package for it:
```json
{
	"submitterId": "ATC",
	"title": "Test collection",
	"description": "Optional collection description",
	"members": ["https://meta.icos-cp.eu/objects/G6PjIjYC6Ka_nummSJ5lO8SV", "https://meta.icos-cp.eu/objects/sdfRNhhI5EN_BckuQQfGpdvE"],
	"isNextVersionOf": "CkSE78VzQ3bmHBtkMLt4ogJy",
	"preExistingDoi": "10.18160/VG28-H2QA"
}
```
The fields are either self-explanatory, or have the same meaning as for the data object upload.

As with data object uploads, this metadata package must be HTTP-POSTed to `https://meta.icos-cp.eu/upload` with `application/json` content type and the CP authentication cookie. The server will reply with landing page of the collection. The last segment of the landing page's URL is collections ID that is obtained by SHA-256-hashsumming of the alphabetically sorted list of members' hashsums (it is base64url representations of the hashsums that are sorted, but it is binary values that contribute to the collections' hashsum).

### Reconstructing upload-metadata packages of existing objects/collections
When scripting uploads of multiple objects, it can be convenient to use an upload-metadata package of an existing object as an example or a template. The reconstructed package can be fetched using the following request:

`curl https://meta.icos-cp.eu/dtodownload?uri=<langing page URL>`

In bash shell, one can also format the JSON after fetching, as in this example:

`curl https://meta.icos-cp.eu/dtodownload?uri=https://meta.icos-cp.eu/objects/n7cB5kS4U1E5A3mXKtEUCF9s | python3 -m json.tool`

## Metadata flow (for ATC only)
The CSV tables with ATC metadata are to be pushed as payloads of HTTP POST requests to URLs of the form

`https://meta.icos-cp.eu/upload/atcmeta/<tableName>`

where `<tableName>` is a name used to distinguish different tables, for example "roles", "stations", "instruments", "instrumentsLifecycle", etc.

Authentication with a pre-configured CP account is required. The authentication mechanism is the same as for data object upload.

## Administrative API for RDF updates
Intended for internal use at Carbon Portal.
All the updates need to go through the RDF logs, therefore SPARQL UPDATE protocol could not be used directly. Instead, one needs to HTTP POST a SPARQL CONSTRUCT query, that will produce the triples that need to be inserted/retracted, to a URL of the form:

`https://meta.icos-cp.eu/admin/<insert | delete>/<instance-server id>` ,

where `instance-server id` is the id of the instance server that will be affected by the change, as specified in `meta`'s config file.

To be allowed to perform the operation, one needs to be a on the `adminUsers` list in the config (`cpmeta.sparql.adminUsers`). Here is a `curl` example of the API usage:

`curl --upload-file sparql.rq -H "Cookie: cpauthToken=<the token>" https://meta.icos-cp.eu/admin/delete/sitescsv?dryRun=true`

The output will show the resulting changes. If `dryRun` is `true`, no actual changes are performed, only the outcome is shown.

---

## Information for developers

### Getting started with the front-end part

- Install `Node.js` as instructed [here](https://github.com/nodesource/distributions)
- Clone this repository: `git clone git@github.com:ICOS-Carbon-Portal/meta.git`
- `cd meta`
- Install Node.js dependencies: `npm install`
- Now you can run Gulp tasks: `npm run <task>` (see `package.json` for the list of defined tasks)

### Getting started with the back-end part

- Set up a Docker container with PostgreSQL for RDF log (see the [infrastructure project](https://github.com/ICOS-Carbon-Portal/infrastructure/tree/master/rdflogdb))
- Make a copy of `example.application.conf` file in the project root named `application.conf` and edit it to suit your environment. For some default config values, see `application.conf` in `src/main/resources/`. For deployment, make sure there is a relevant `application.conf` in the JVM's working directory.
- Run sbt
- In the sbt console, run `~reStart` for continuous local rebuilds and server restarts. Alternatively, if the development is done only in the front end part, running `~copyResources` is sufficient but much faster.


### Setting up authentication/authorization for the Handle.net client HandleNetClient
Handle.net servers use two-way TLS.

#### Client side
- Generate a public/private key pair:

`$ openssl genpkey -algorithm RSA -out private_key.pem -pkeyopt rsa_keygen_bits:4096`

- Convert the private key to PKCS8 binary format:

`$ openssl pkcs8 -topk8 -outform DER -in private_key.pem -out private_key.der -nocrypt`

- Extract the public key from the key pair (output to X.509 binary format):

`$ openssl rsa -pubout -in private_key.pem -outform DER -out public_key.der`

- Convert the public key from X.509 format to the format used by Handle.net server software. This can be accomplished with the help of `HandleNetClient.getHandleNetKeyBytes` method, from Scala REPL. The obtained byte array should simply be written to a file, for example `handleClientPubKey.bin`.

- Make sure the contents of `handleClientPubKey.bin` file are published as the value of `HS_PUBKEY` type at an index that is claimed to describe an administrator of your Handle prefix. For example, it could be record 300 of [0.NA/11676](https://hdl.handle.net/0.NA/11676) or record 300 of [11676/ADMIN](https://hdl.handle.net/11676/ADMIN). This operation must be done by someone who already has the admin rights for the prefix.
- Generate a self-signed certificate using the private key from the previous steps. Only `CN` value should be provided, and it must identify the `HS_PUBKEY` record in the Handle system, for example as `300:11676/ADMIN`:

`$ openssl req -keyform DER -key private_key.der -new -x509 -days 15000 -out handleClientCert.pem`

#### Server side
By default, Handle.net server software comes with self-signed SSL certificates with `CN=anonymous`.
This does not work for Java, therefore it is necessary to get the administrators of the Handle server (which you are going to use) to replace the default with a self-signed certificate with a `CN` equal to the actual domain name of the server.
After that the server certificate needs to be fetched (to be used later as a trusted cert), for example:

`$ openssl s_client -showcerts -connect epic.pdc.kth.se:8000 < /dev/null 2> /dev/null | openssl x509 -outform PEM > server_cert.pem`

#### Testing with curl
curl has the possibility of disabling server certificate validation with `-k` command-line option. The following example should create/overwrite handle with suffix `<suffix>` (use actual desired suffix) by `HTTP-PUT`ing JSON file `payload.json` into a handle:

`$ curl -v -k --cert handleClientCert.pem --key private_key.pem -H 'Authorization: Handle clientCert="true"' -H "Content-Type: application/json" --upload-file payload.json https://epic.pdc.kth.se:8000/api/handles/11676/<suffix>?overwrite=true`

 `payload.json` is expected to contain a JSON object with array of handle values as `values` property. For more details on the HTTP API see [documentation](https://handle.net/hnr_documentation.html). To examine handle values, run, for example

`$ curl -k https://epic.pdc.kth.se:8000/api/handles/11676/<suffix> | python -m json.tool`

#### Deployment
- When deploying `meta`, make sure that the client private key, certificate, and the server certificate files are copied to the production environment, and that the config parameters for the Handle client provide correct paths to them.

### Miscellaneous recipes

#### Restoring RDFLog database from pg_dump

`cat dump.sqlc | docker exec -i rdflogdb_rdflogdb_1 pg_restore -c -U postgres -d postgres --format=c > /dev/null`

#### Autorendering README.md to HTML for preview on file change
Make sure that Python is available, and `python-markdown` and `inotify-tools` packages are installed on your Linux system.
Then you can run:

`$ while inotifywait -e close_write README.md; do python -m markdown README.md > README.html; done`

#### SHA-256 sum in base64
`$ sha256sum <filename> | awk '{print $1;}' | xxd -r -p | base64`
