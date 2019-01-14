# pensions-scheme

[![Build Status](https://travis-ci.org/hmrc/pensions-scheme.svg)](https://travis-ci.org/hmrc/pensions-scheme) [ ![Download](https://api.bintray.com/packages/hmrc/releases/pensions-scheme/images/download.svg) ](https://bintray.com/hmrc/releases/pensions-scheme/_latestVersion)


pensions-scheme
=============

Microservice to support the registration of Pension Scheme Administrators and the registration of Pensions Schemes

### Registration Endpoints

| Method | Path                                                            | Description                                                                                                           |
|--------|-----------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------|
|  POST  | ```/register-scheme```                    | Register scheme.                                                          |
|  POST   | ```/register-psa```   | Register Pension Scheme Administrator.                                                   |


### Scheme List Endpoints

| Method | Path                                                            | Description                                                                                                           |
|--------|-----------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------|
|  GET   | ```/list-of-schemes```                  | Retrieves list of schemes successfully submitted to date.|


### Business Partner Matching Endpoints

| Method | Path                                                            | Description                                                                                                           |
|--------|-----------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------|
|  POST  | ```/register-with-no-id/organisation```                    | Retrieve company information for company without identifier (in most cases non-UK)                                                          |
|  POST   | ```/register-with-id/individual```   | Retrieve individual information given name and NINO                                                   |
|  POST   | ```/register-with-id/organisation```                  | Retrieve company information given Company Name and Unique Tax Reference.|


### Name Matching Endpoints

| Method | Path                                                            | Description                                                                                                           |
|--------|-----------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------|
|  GET   | ```/psa-name/:id```                  | Retrieve Pension Scheme Administrator name. |
|  POST   | ```/psa-name/:id```                  | Save Pension Scheme Administrator name. |

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html")

