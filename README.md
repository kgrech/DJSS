# DJSS - Demo of Jooq, Spark and Spock

## What is it?
It is a sample accounting and transfer application. It is possible to create accounts and perfrom transfers between them

## Technologies used
- DB: [JOOQ](https://www.jooq.org/) and [Liquibase](https://www.liquibase.org/)
- Rest API: [SparkJava](http://sparkjava.com/)
- Testing: [Spock](http://spockframework.org/) (Groovy)
- Build system: [Gradle](https://gradle.org/)
- [lombock](https://projectlombok.org/)

## Build proccess
- run ``gradle update`` command to initlize H2 DB in ./db folder
- run ``gradle build`` command to run jooq generation, code compilation and running of "processing" test suites
- run ``gradle restApiTest`` to run test suite for REST API
- run ``gradle end2EndTest`` to run E2E tests

## Database notes
- We are using SERIALIZABLE transaction isolation level. There might be enought to use REPEATABLE READ level, but H2 does not support it
- We are using liqubase to initilize db and to have a room for future migrations

## Test suites
There are 3 test suites in the project:
- ProcessingSuite tests main bussiness logic and concurrency
- RestApiSuite tests the rest API
- End2EndSuite tests the system end-to-end by creating and making transfer using rest API

The commands to run each suite are shown above. The test reports are produced to ``./build/reports/tests/<suiteName>/index.html``.
Line coverage is 86%

## Running
Execute ``gradle run`` command to run the application.
The following restfull API are availble:
- Accounts
  - Add a new account: POST ``http://{{host}}:{{port}}/accounts/``
  - Get page of accounts: GET ``http://{{host}}:{{port}}/accounts/?page={{page}}&pageSize={{pageSize}}``
  - Get account by id: PUT ``http://{{host}}:{{port}}/accounts/{{id}}``
  - Update an existing account: POST ``http://{{host}}:{{port}}/accounts/{{id}}``
  - Delete the account: DELETE ``http://{{host}}:{{port}}/accounts/{{id}}``

- Transfers
  - Add a new transfer: POST ``http://{{host}}:{{port}}/transfers/``
  - Get page of transfers: GET ``http://{{host}}:{{port}}/transfers/?page={{page}}&pageSize={{pageSize}}``
  - Get transfer by id: PUT ``http://{{host}}:{{port}}/transfers/{{id}}``
  - Update an existing transfer: POST ``http://{{host}}:{{port}}/transfers/{{id}}``
  - Delete the transfer: DELETE ``http://{{host}}:{{port}}/transfers/{{id}}``
The requests are availbile in the postman collection
