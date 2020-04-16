Register Scheme
-----------------------
Register a scheme.

* **URL**

  `/register-scheme`

* **Method**

  `POST`

*  **Request Header**
    
   `psaId`

* **Example Payload**

```json

 {
    "benefits":"opt3",
    "establishers":[
       {
          "establisherDetails":{
             "firstName":"Coffee",
             "lastName":"McCoffee",
             "isDeleted":false
          
 },
          "hasNino":false,
          "noNinoReason":"w",
          "address":{
             "addressLine1":"10 Other Place",
             "addressLine2":"Some District",
             "addressLine3":"Anytown",
             "addressLine4":"Somerset",
             "postcode":"ZZ1 1ZZ",
             "country":"GB"
          
 },
          "addressResults":{
             "addressLine1":"10 Other Place",
             "addressLine2":"Some District",
             "addressLine3":"Anytown",
             "addressLine4":"Somerset",
             "postalCode":"ZZ1 1ZZ",
             "countryCode":"GB"
          
 },
          "establisherKind":"individual",
          "addressYears":"over_a_year",
          "noUtrReason":"q",
          "dateOfBirth":"1991-01-01",
          "isEstablisherNew":true,
          "hasUtr":false,
          "contactDetails":{
             "emailAddress":"qwert@qwerty.com",
             "phoneNumber":"1234567890"
          
 }
       
 }
    
 ],
    "schemeType":{
       "name":"corp"
    
 },
    "haveAnyTrustees":false,
    "occupationalPensionScheme":false,
    "schemeName":"Starbucks Coffee",
    "membership":"opt2",
    "declaration":true,
    "schemeEstablishedCountry":"FR",
    "uKBankAccount":true,
    "membershipFuture":"opt3",
    "securedBenefits":false,
    "declarationDuties":true,
    "investmentRegulated":false,
    "uKBankDetails":{
       "sortCode":{
          "first":"12",
          "second":"13",
          "third":"14"
       
 },
       "accountNumber":"12345678"
    
 }
 }

```

* **Success Response:**

  * **Code:** 200 <br />
    **Content:**

```json
{
   "schemeReferenceNumber":"S0123456789"
}
```

* **Error Response:**

  * **Code:** 400 BAD_REQUEST <br />
    **Content:** `{"statusCode":400,"message":"Bad Request without PSAId or request body"}`

  * **Code:** 400 BAD_REQUEST <br />
    **Content:** `{"statusCode":400,"message":"Invalid pension scheme"}`
    
  * **Code:** 400 BAD_REQUEST <br />
    **Content:** `{"statusCode":400,"message":"Invalid bank account details"}`
    
  * **Code:** 404 NOT_FOUND <br />

  OR anything else

  * **Code:** 500 INTERNAL_SERVER_ERROR <br />
