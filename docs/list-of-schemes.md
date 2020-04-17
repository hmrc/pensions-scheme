List-of-schemes
-----------------------
Retrieves list of schemes successfully submitted to date.

* **URL**

  `/list-of-schemes`

* **Method**

  `POST`

*  **Request Header**
    
   `psaId`

* **Success Response:**

  * **Code:** 200 

* **Example Success Response**

```json
{
   "processingDate":"2001-12-17T09:30:47Z",
   "totalSchemesRegistered":"15",
   "schemeDetail":[
      {
         "name":"Pending Single Trust Scheme with Company Individual",
         "referenceNumber":"S2400000002",
         "schemeStatus":"Pending",
         "openDate":"2017-12-17",
         "relationShip":"Primary PSA"
      },
      {
         "name":"Pending info required Single Trust Scheme with Partnership Establisher",
         "referenceNumber":"S2400000003",
         "schemeStatus":"Pending Info Required",
         "openDate":"2017-12-17",
         "relationShip":"Primary PSA"
      },
      {
         "name":"Pending information received Body Corporate Scheme",
         "referenceNumber":"S2400000004",
         "schemeStatus":"Pending Info Received",
         "openDate":"2017-12-17",
         "relationShip":"Primary PSA"
      },
      {
         "name":"Rejected Body Corporate Scheme",
         "referenceNumber":"S2400000005",
         "schemeStatus":"Rejected",
         "openDate":"2017-12-17",
         "relationShip":"Primary PSA"
      },
      {
         "name":"Open Single Trust Scheme with Indiv Establisher and Trustees",
         "referenceNumber":"S2400000001",
         "schemeStatus":"Open",
         "openDate":"2017-12-17",
         "pstr":"24000001IN",
         "relationShip":"Primary PSA"
      },
      {
         "name":"Deregistered Group life/death in service Scheme",
         "referenceNumber":"S2400000006",
         "schemeStatus":"Deregistered",
         "openDate":"2017-12-17",
         "pstr":"24000006IN",
         "relationShip":"Primary PSA"
      },
      {
         "name":"Wound-up Master Trust Scheme",
         "referenceNumber":"S2400000007",
         "schemeStatus":"Wound-up",
         "openDate":"2017-12-17",
         "pstr":"24000007IN",
         "relationShip":"Primary PSA"
      },
      {
         "name":"Rejected Under Appeal Single Trust Scheme",
         "referenceNumber":"S2400000008",
         "schemeStatus":"Rejected Under Appeal",
         "openDate":"2017-12-17",
         "pstr":"24000008IN",
         "relationShip":"Primary PSA"
      },
      {
         "name":"Open duplicate submission",
         "referenceNumber":"S2400000009",
         "schemeStatus":"Open",
         "openDate":"2017-12-17",
         "pstr":"24000009IN",
         "relationShip":"Primary PSA"
      },
      {
         "name":"Open invalid variation submission",
         "referenceNumber":"S2400000010",
         "schemeStatus":"Open",
         "openDate":"2017-12-17",
         "pstr":"24000010IN",
         "relationShip":"Primary PSA"
      },
      {
         "name":"Open Scheme Variations Test - Scheme D1.1",
         "referenceNumber":"S2400000015",
         "schemeStatus":"Open",
         "openDate":"2017-12-17",
         "pstr":"24000015IN",
         "relationShip":"Primary PSA"
      },
      {
         "name":"Open Scheme Variations Test - Scheme D1.2",
         "referenceNumber":"S2400000016",
         "schemeStatus":"Open",
         "openDate":"2017-12-17",
         "pstr":"24000016IN",
         "relationShip":"Primary PSA"
      },
      {
         "name":"Open Scheme Variations Test - Scheme D1.3",
         "referenceNumber":"S2400000017",
         "schemeStatus":"Open",
         "openDate":"2017-12-17",
         "pstr":"24000017IN",
         "relationShip":"Primary PSA"
      },
      {
         "name":"Scheme Variations Test - Scheme D1.4",
         "referenceNumber":"S2400000018",
         "schemeStatus":"Open",
         "openDate":"2017-12-17",
         "pstr":"24000018IN",
         "relationShip":"Primary PSA"
      },
      {
         "name":"Pickwick Parchments Ltd",
         "referenceNumber":"S2400000036",
         "schemeStatus":"Open",
         "openDate":"2017-03-01",
         "pstr":"24000036IN",
         "relationShip":"Primary PSA"
      },
      {
         "name":"Open Scheme Overview API Test",
         "referenceNumber":"S2400000040",
         "schemeStatus":"Open",
         "openDate":"2017-12-17",
         "pstr":"24000040IN",
         "relationShip":"Primary PSA"
      },
      {
         "name":"Open Scheme Overview API Test 2",
         "referenceNumber":"S2400000041",
         "schemeStatus":"Open",
         "openDate":"2017-12-17",
         "pstr":"24000041IN",
         "relationShip":"Primary PSA"
      }
   ]
}
```

* **Error Response:**

  * **Code:** 400 BAD_REQUEST <br />
    **Content:** `{"statusCode":400,"message":"Bad Request with no Psa Id"}`

  OR anything else

  * **Code:** 500 INTERNAL_SERVER_ERROR <br />
