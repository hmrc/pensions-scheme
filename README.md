Pensions Scheme
===============

Microservice to support the registration of Pension Scheme Administrators and the registration of Pensions Schemes

Requirements
------------

This service is written in [Scala](http://www.scala-lang.org/) and [Play 2.5](http://playframework.com/), so needs at least a [JRE 1.8](http://www.oracle.com/technetwork/java/javase/downloads/index.html) to run.

API
---

| *Task* | *Supported Methods* | *Description* |
|--------|----|----|
| ```/register-scheme                                     ```  | POST   | Register scheme [More...](docs/register-scheme.md) |
| ```/update-scheme                                       ```  | POST   | Update scheme [More...](docs/update-scheme.md) |
| ```/list-of-schemes                                     ```  | GET    | Retrieves list of schemes successfully submitted to date [More...](docs/list-of-schemes.md) |
| ```/scheme                                              ```  | GET    | Returns scheme details [More...](docs/scheme.md) |
| ```/journey-cache/scheme-subscription/:id               ```  | GET    | Returns the value of a key from the Mongo subscription journey cache 
| ```/journey-cache/scheme-subscription/:id               ```  | POST   | Saves a value to a key in the Mongo subscription journey cache
| ```/journey-cache/scheme-subscription/:id               ```  | DELETE | Removes the value of a key from the Mongo subscription journey cache
| ```/journey-cache/scheme-subscription/:id/lastUpdated   ```  | GET    | Returns the date and time when the Mongo subscription journey cache was last updated
| ```/journey-cache/update-scheme/:id                     ```  | GET    | Returns the value of a key from the Mongo variations journey cache
| ```/journey-cache/update-scheme/:id                     ```  | POST   | Saves a value to a key in the Mongo variations journey cache
| ```/journey-cache/update-scheme/:id                     ```  | DELETE | Removes the value of a key from the Mongo variations journey cache
| ```/journey-cache/update-scheme/:id/lastUpdated         ```  | GET    | Returns the date and time when the Mongo variations journey cache was last updated
| ```/journey-cache/scheme-details/:id                    ```  | GET    | Returns the value of a key from the Mongo scheme details cache
| ```/journey-cache/scheme-details/:id                    ```  | POST   | Saves a value to a key in the Mongo scheme details cache
| ```/journey-cache/scheme-details/:id                    ```  | DELETE | Removes the value of a key from the Mongo scheme details cache
| ```/update-scheme/get-lock                              ```  | GET    | Returns a lock record for a scheme that is locked or 403 if not found
| ```/update-scheme/get-lock-by-psa                       ```  | GET    | Returns a lock record for a PSA having a locked scheme or 403 if not found
| ```/update-scheme/get-lock-by-scheme                    ```  | GET    | Returns a lock record for an SRN for a scheme which is locked or 403 if not found
| ```/update-scheme/isLockByPsaOrScheme                   ```  | GET    | Returns a lock record for a scheme that is locked by PSA ID or SRN or 403 if not found
| ```/update-scheme/lock                                  ```  | POST   | Locks a scheme
| ```/update-scheme/release-lock                          ```  | DELETE | Releases a locked scheme
| ```/email-response/*id                                  ```  | POST   | Sends an audit event indicating the response returned by the email service in response to a request to send an email
| ```/is-psa-associated                                   ```  | GET    | Returns boolean indicating whether a PSA is associated with any schemes [More...](docs/is-psa-associated.md) |




| ```/tai/:nino/employments/years/:year ```  | GET | Retrieves all employments for a given year with Annual Account information [More...](docs/employments/annual-account-employments.md) |
| ```/tai/:nino/employments/years/:year/update ```  | POST | The end point updates the incorrect employment details [More...](docs/employments/update-employments.md)|

Configuration
-------------

All configuration is namespaced by the `run.mode` key, which can be set to `Dev` or `Prod` - note this is independent of Play's concept of mode.

All the other microservices used by TAI require host and port settings, for example:

| *Key*                    | *Description* |
| ------------------------ | ----------- |
| `microservice.services.nps-hod.host` | The host of the NPS service |
| `microservice.services.nps-hod.port` | The port of the NPS service |
| `microservice.services.nps-hod.path` | The path of the NPS service |

Only nps microservice requires a path.

