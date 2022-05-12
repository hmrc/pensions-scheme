Pensions Scheme
===============

Microservice to support the registration and post-registration update (variations) of pension schemes.
// TODO: Table isn't exhaustive?
API
---
  
| *Task*                                                      | *Supported Methods* | *Description*                                                                                                        |
|-------------------------------------------------------------|---------------------|----------------------------------------------------------------------------------------------------------------------|
| ```/register-scheme                                     ``` | POST                | Register scheme [More...](docs/register-scheme.md)                                                                   |
| ```/update-scheme                                       ``` | POST                | Update scheme [More...](docs/update-scheme.md)                                                                       |
| ```/list-of-schemes                                     ``` | GET                 | Retrieves list of schemes successfully submitted to date [More...](docs/list-of-schemes.md)                          |
| ```/scheme                                              ``` | GET                 | Returns scheme details [More...](docs/scheme.md)                                                                     |
| ```/journey-cache/scheme-subscription/:id               ``` | GET                 | Returns the value of a key from the Mongo scheme registration cache                                                  
| ```/journey-cache/scheme-subscription/:id               ``` | POST                | Saves a value to a key in the Mongo scheme registration cache                                                        
| ```/journey-cache/scheme-subscription/:id               ``` | DELETE              | Removes the value of a key from the Mongo scheme registration cache                                                  
| ```/journey-cache/scheme-subscription/:id/lastUpdated   ``` | GET                 | Returns the date and time when the Mongo scheme registration cache was last updated                                  
| ```/journey-cache/update-scheme/:id                     ``` | GET                 | Returns the value of a key from the Mongo scheme variations update cache                                             
| ```/journey-cache/update-scheme/:id                     ``` | POST                | Saves a value to a key in the Mongo scheme variations update cache                                                   
| ```/journey-cache/update-scheme/:id                     ``` | DELETE              | Removes the value of a key from the Mongo scheme variations update cache                                             
| ```/journey-cache/update-scheme/:id/lastUpdated         ``` | GET                 | Returns the date and time when the Mongo scheme variations update cache was last updated                             
| ```/journey-cache/scheme-details/:id                    ``` | GET                 | Returns the value of a key from the Mongo scheme variations read-only cache                                          
| ```/journey-cache/scheme-details/:id                    ``` | POST                | Saves a value to a key in the Mongo scheme variations read-only cache                                                
| ```/journey-cache/scheme-details/:id                    ``` | DELETE              | Removes the value of a key from the Mongo scheme variations read-only cache                                          
| ```/update-scheme/get-lock                              ``` | GET                 | Returns a lock record for a scheme that is locked for variations or 403 if not found                                 
| ```/update-scheme/get-lock-by-psa                       ``` | GET                 | Returns a lock record for a PSA having a locked scheme for variations or 403 if not found                            
| ```/update-scheme/get-lock-by-scheme                    ``` | GET                 | Returns a lock record for an SRN for a scheme which is locked for variations or 403 if not found                     
| ```/update-scheme/isLockByPsaOrScheme                   ``` | GET                 | Returns a lock record for a scheme that is locked for variations by PSA ID or SRN or 403 if not found                
| ```/update-scheme/lock                                  ``` | POST                | Locks a scheme for variations                                                                                        
| ```/update-scheme/release-lock                          ``` | DELETE              | Releases the lock on a scheme locked for variations                                                                  
| ```/email-response/*id                                  ``` | POST                | Sends an audit event indicating the response returned by the email service in response to a request to send an email 
| ```/is-psa-associated                                   ``` | GET                 | Returns boolean indicating whether a PSA is associated with any schemes [More...](docs/is-psa-associated.md)         |
| ```/psp-scheme ```                                          | GET                 | Description for GET                                                                                                  |
| ``` /journey-cache/racdac-scheme-subscription/:id ```       | GET                 | Description for GET                                                                                                  |
| ``` /journey-cache/racdac-scheme-subscription/:id```        | POST                | Description for POST                                                                                                 |
| ``` /journey-cache/racdac-scheme-subscription/:id```        | DELETE              | Description for DELETE                                                                                               |
| ``` /journey-cache/racdac-scheme-subscription/:id/lastUpdated ```       | GET                 | Description for GET                                                                                                  |
| ``` /email-response-racdac/:id ```          | POST                | Description for POST                                                                                                 |





## Note on terminology
The terms scheme reference number and submission reference number (SRN) are interchangeable within the PODS codebase; some downstream APIs use scheme reference number, some use submission reference number, probably because of oversight on part of the technical teams who developed these APIs. This detail means the same thing, the reference number that was returned from ETMP when the scheme details were submitted.
