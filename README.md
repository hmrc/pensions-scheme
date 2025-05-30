# Pensions Scheme

- [Overview](#overview)
- [Requirements](#requirements)
- [Running the Service](#running-the-service)
- [Enrolments](#enrolments)
- [Compile & Test](#compile--test)
- [Dependent Services](#dependent-services)
- [Terminology](#note-on-terminology)
- [Endpoints Used](#endpoints)
- [License](#license)

## Overview

This is the backend repository for the Pensions Scheme service. This service supports the registration and post-registration update (variations) of pension schemes.

This service has a corresponding front-end microservice, namely Pensions Scheme Frontend.

**Associated Frontend Link:** https://github.com/hmrc/pensions-scheme-frontend

**Stubs:** https://github.com/hmrc/pensions-scheme-stubs

## Requirements
This service is written in Scala and Play, so needs at least a [JRE] to run.

**Node version:** 16.20.2

**Java version:** 21

**Scala version:** 2.13.14


## Running the Service
**Service Manager Profile:** PODS_ALL

**Port:** 8203

In order to run the service, ensure Service Manager is installed (see [MDTP guidance](https://docs.tax.service.gov.uk/mdtp-handbook/documentation/developer-set-up/set-up-service-manager.html) if needed) and launch the relevant configuration by typing into the terminal:
`sm2 --start PODS_ALL`

To run the service locally, enter `sm2 --stop PENSIONS_SCHEME`.

In your terminal, navigate to the relevant directory and enter `sbt run`.

Access the Authority Wizard and login with the relevant enrolment details [here](http://localhost:9949/auth-login-stub/gg-sign-in)


## Enrolments
There are several different options for enrolling through the auth login stub. In order to enrol as a dummy user to access the platform for local development and testing purposes, the following details must be entered on the auth login page.


For access to the **Pension Administrator dashboard** for local development, enter the following information: 

**Redirect url -** http://localhost:8204/manage-pension-schemes/overview 

**GNAP Token -** NO 

**Affinity Group -** Organisation 

**Enrolment Key -** HMRC-PODS-ORG 

**Identifier Name -** PsaID 

**Identifier Value -** A2100005

---

In order to access the **Pension Practitioner dashboard** for local development, enter the following information: 

**Redirect URL -** http://localhost:8204/manage-pension-schemes/dashboard 

**GNAP Token -** NO 

**Affinity Group -** Organisation 

**Enrolment Key -** HMRC-PODSPP-ORG 

**Identifier Name -** PspID 

**Identifier Value -** 21000005

---

**Dual enrolment** as both a Pension Administrator and Practitioner is also possible and can be accessed by entering:

**Redirect url -** http://localhost:8204/manage-pension-schemes/overview 

**GNAP Token -** NO 

**Affinity Group -** Organisation 

**Enrolment Key 1 -** HMRC-PODSPP-ORG Identifier

**Name 1 -** PspID Identifier

**Value 1 -** 21000005

**Enrolment Key 2 -** HMRC-PODS-ORG 

**Identifier Name 2 -** PsaID 

**Identifier Value 2 -** A2100005

---

To access the **Scheme Registration journey**, enter the following information:

**Redirect URL -** http://localhost:8204/manage-pension-schemes/you-need-to-register

**GNAP Token -** NO 

**Affinity Group -** Organisation

---


## Compile & Test
**To compile:** Run `sbt compile`

**To test:** Use `sbt test`

**To view test results with coverage:** Run `sbt clean coverage test coverageReport`

For further information on the PODS Test Approach and wider testing including acceptance, accessibility, performance, security and E2E testing, visit the PODS Confluence page [here](https://confluence.tools.tax.service.gov.uk/pages/viewpage.action?spaceKey=PODSP&title=PODS+Test+Approach).

For Journey Tests, visit the [Journey Test Repository](| Journey tests(https://github.com/hmrc/pods-journey-tests).

View the prototype [here](https://pods-event-reporting-prototype.herokuapp.com/).


## Dependent Services
There are numerous APIs implemented throughout the MPS architecture, and the relevant endpoints are illustrated below. For an overview of all PODS APIs, refer to the [PODS API Documentation](https://confluence.tools.tax.service.gov.uk/display/PODSP/PODS+API+Latest+Version).

## Note on terminology
The terms scheme reference number and submission reference number (SRN) are interchangeable within the PODS codebase; some downstream APIs use scheme reference number, some use submission reference number, probably because of oversight on part of the technical teams who developed these APIs. This detail means the same thing, the reference number that was returned from ETMP when the scheme details were submitted.

## Endpoints


| *Task*                                                            | *Supported Methods* | *Description*                                                                                                        |
|-------------------------------------------------------------------|---------------------|----------------------------------------------------------------------------------------------------------------------|
| ```/register-scheme                                     ```       | POST                | Register scheme [More...](docs/register-scheme.md)                                                                   |
| ```/update-scheme                                       ```       | POST                | Update scheme [More...](docs/update-scheme.md)                                                                       |
| ```/list-of-schemes                                     ```       | GET                 | Retrieves list of schemes successfully submitted to date [More...](docs/list-of-schemes.md)                          |
| ```/scheme                                              ```       | GET                 | Returns PSA scheme details [More...](docs/scheme.md)                                                                 |
| ```/psp-scheme ```                                                | GET                 | Returns PSP scheme details by SRN and pspId                                                                          |
| ```/journey-cache/scheme-subscription/:id               ```       | GET                 | Returns the value of a key from the Mongo scheme registration cache                                                  |
| ```/journey-cache/scheme-subscription/:id               ```       | POST                | Saves a value to a key in the Mongo scheme registration cache                                                        |
| ```/journey-cache/scheme-subscription/:id               ```       | DELETE              | Removes the value of a key from the Mongo scheme registration cache                                                  |
| ```/journey-cache/scheme-subscription/:id/lastUpdated   ```       | GET                 | Returns the date and time when the Mongo scheme registration cache was last updated                                  |
| ```/journey-cache/update-scheme/:id                     ```       | GET                 | Returns the value of a key from the Mongo scheme variations update cache                                             |
| ```/journey-cache/update-scheme/:id                     ```       | POST                | Saves a value to a key in the Mongo scheme variations update cache                                                   |
| ```/journey-cache/update-scheme/:id                     ```       | DELETE              | Removes the value of a key from the Mongo scheme variations update cache                                             |
| ```/journey-cache/update-scheme/:id/lastUpdated         ```       | GET                 | Returns the date and time when the Mongo scheme variations update cache was last updated                             |
| ```/journey-cache/scheme-details/:id                    ```       | GET                 | Returns the value of a key from the Mongo scheme variations read-only cache                                          |
| ```/journey-cache/scheme-details/:id                    ```       | POST                | Saves a value to a key in the Mongo scheme variations read-only cache                                                |
| ```/journey-cache/scheme-details/:id                    ```       | DELETE              | Removes the value of a key from the Mongo scheme variations read-only cache                                          |
| ``` /journey-cache/racdac-scheme-subscription/:id ```             | GET                 | Returns the value of a key from the Mongo RACDAC scheme registration cache                                           |
| ``` /journey-cache/racdac-scheme-subscription/:id```              | POST                | Saves a value to a key in the Mongo RACDAC scheme registration cache                                                 |
| ``` /journey-cache/racdac-scheme-subscription/:id```              | DELETE              | Removes the value of a key from the Mongo RACDAC scheme registration cache                                           |
| ``` /journey-cache/racdac-scheme-subscription/:id/lastUpdated ``` | GET                 | Returns the date and time when the Mongo RACDAC scheme registration cache was last updated                           |
| ```/update-scheme/get-lock                              ```       | GET                 | Returns a lock record for a scheme that is locked for variations or 403 if not found                                 |
| ```/update-scheme/get-lock-by-psa                       ```       | GET                 | Returns a lock record for a PSA having a locked scheme for variations or 403 if not found                            |
| ```/update-scheme/get-lock-by-scheme                    ```       | GET                 | Returns a lock record for an SRN for a scheme which is locked for variations or 403 if not found                     |
| ```/update-scheme/isLockByPsaOrScheme                   ```       | GET                 | Returns a lock record for a scheme that is locked for variations by PSA ID or SRN or 403 if not found                |
| ```/update-scheme/lock                                  ```       | POST                | Locks a scheme for variations                                                                                        |
| ```/update-scheme/release-lock                          ```       | DELETE              | Releases the lock on a scheme locked for variations                                                                  |
| ```/email-response/*id                                  ```       | POST                | Sends an audit event indicating the response returned by the email service in response to a request to send an email |
| ```/is-psa-associated                                   ```       | GET                 | Returns boolean indicating whether a PSA is associated with any schemes [More...](docs/is-psa-associated.md)         |

---

## License
This code is open source software Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at:

[http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.

[↥ Back to Top](#pensions-scheme)

