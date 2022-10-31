
# Posts Fetcher

Implementation of stream-based client for fetching an array of JSON objects.

## Goal

Goal of this application is to present example of 
memory efficient and functional solution
for fetching arrays of JSON objects in Scala.

### Functional Requirements
1. Read all posts from the following API: https://jsonplaceholder.typicode.com
2. Write all JSON objects to the separate files on the local machine.

## Usage
### Run application
```
sbt run
```
### Build standalone application
```
sbt assembly
```
### Run Jar with external config
```
java -Dconfig.file=./my-external-application.properties -jar ./project-foo-1.jar
```
External config has to comply with typesafe config specification.

Default config:
```
# HTTP GET endpoint to fetch array of JSON objects
input.url=https://jsonplaceholder.typicode.com/posts

# Output directory to store fetched json objects
output.dir=./files/

# If true - overrides files on conflict (otherwise skips json)
processing.override-files=true
```

## Stack
http4s - HTTP client (Ember) and integration with circle.

fs2 - Scala streaming library.

Circle - JSON serialisation.

Cats/Cats Effect - Scala FP libraries.

Typesafe-config - config util library.

Typesafe logging - Scala logging (with Logback as logging library).

Scalatest - Scala testing library.

ScalaMock - Scala test mocking library.

## TODO

- Improve tests:
  - abstract file operations in PostsService so they can be mocked.
    - Also - cleanup created files (they are making tests - execution order dependent)
  - more test cases:
    - Related to I/O failures (network, filesystem).
    - Related to configuration validation.
    - Testing IOApp as a whole.
