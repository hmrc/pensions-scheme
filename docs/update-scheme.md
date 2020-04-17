Update-scheme
-------------
Update a scheme which has already been registered.

* **URL**

  `/update-scheme`

* **Method**

  `POST`

*  **Request Header**
    
   `pstr`
   
   `psaId`

* **Example Payload**

```json

 {
    "benefits":"opt2",
    "establishers":[
       {
          "establisherDetails":{
             "firstName":"Robert",
             "lastName":"Smith"
          },
          "hasNino":true,
          "utr":{
             "value":"1234567892"
          },
          "address":{
             "country":"GB",
             "postcode":"NE3 5TR",
             "addressLine1":"10 Westgate Road",
             "addressLine2":"South Gosforth",
             "addressLine3":"Newcastle",
             "addressLine4":"Tyne and Wear"
          },
          "previousAddress":{
             "country":"GB",
             "postcode":"NE1 4AB",
             "addressLine1":"Flat 5a",
             "addressLine2":"Central Apartments",
             "addressLine3":"Waterloo Road",
             "addressLine4":"Newcastle"
          },
          "establisherKind":"individual",
          "addressYears":"under_a_year",
          "dateOfBirth":"1955-03-29",
          "establisherNino":{
             "value":"AA999999A"
          },
          "hasUtr":true,
          "contactDetails":{
             "emailAddress":"robert.smith@test.com",
             "phoneNumber":"0044-078665445"
          },
          "isEstablisherComplete":true
       }
    ],
    "schemeType":{
       "name":"single"
    },
    "isBeforeYouStartComplete":true,
    "isPsaSuspended":false,
    "membership":"opt3",
    "schemeStatus":"Open",
    "schemeEstablishedCountry":"GB",
    "isAboutBenefitsAndInsuranceComplete":true,
    "membershipFuture":"opt4",
    "insurancePolicyNumber":"123456",
    "moreThanTenTrustees":false,
    "isAboutMembersComplete":true,
    "psaDetails":[
       {
          "individual":{
             "firstName":"Nigel",
             "lastName":"Smith",
             "middleName":"Robert"
          },
          "relationshipDate":"2018-06-22",
          "id":"A2100005"
       },
       {
          "relationshipDate":"2018-07-01",
          "id":"A2100007",
          "organisationOrPartnershipName":"Acme Ltd"
       },
       {
          "individual":{
             "firstName":"Martha",
             "lastName":"Stewart"
          },
          "relationshipDate":"2018-07-13",
          "id":"A2100011"
       },
       {
          "individual":{
             "firstName":"Jennifer",
             "lastName":"Carter",
             "middleName":"Debra"
          },
          "relationshipDate":"2019-01-01",
          "id":"A2100021"
       },
       {
          "individual":{
             "firstName":"Matthew",
             "lastName":"Howe",
             "middleName":"Bryan"
          },
          "relationshipDate":"2019-01-01",
          "id":"A2100022"
       },
       {
          "relationshipDate":"2019-01-01",
          "id":"A2100031",
          "organisationOrPartnershipName":"Oxford Phones Ltd"
       },
       {
          "relationshipDate":"2019-01-01",
          "id":"A2100032",
          "organisationOrPartnershipName":"Hendon Electrics Ltd"
       },
       {
          "relationshipDate":"2019-01-01",
          "id":"A2100041",
          "organisationOrPartnershipName":"Riverside Partnerships"
       },
       {
          "relationshipDate":"2019-01-01",
          "id":"A2100042",
          "organisationOrPartnershipName":"Digital Solutions Partnership"
       }
    ],
    "occupationalPensionScheme":false,
    "schemeName":"Open Single Trust Scheme with Indiv Establisher and Trustees",
    "schemeSrnId":"S2400000001",
    "declaration":true,
    "insuranceCompanyName":"Acme Insurance",
    "pstr":"24000001IN",
    "changeOfEstablisherOrTrustDetails":true,
    "securedBenefits":true,
    "investmentRegulated":false,
    "trustees":[
       {
          "hasNino":true,
          "utr":{
             "value":"1234567892"
          },
          "trusteeNino":{
             "value":"AA999999A"
          },
          "trusteeAddressYears":"under_a_year",
          "dateOfBirth":"1955-03-29",
          "trusteeAddressId":{
             "country":"GB",
             "postcode":"WA8 6JT",
             "addressLine1":"44 Albert Road",
             "addressLine2":"Widnes",
             "addressLine3":"Cheshite"
          },
          "trusteePreviousAddress":{
             "country":"GB",
             "postcode":"NE1 4AB",
             "addressLine1":"Flat 5a",
             "addressLine2":"Central Apartments",
             "addressLine3":"Waterloo Road",
             "addressLine4":"Newcastle"
          },
          "hasUtr":true,
          "trusteeKind":"individual",
          "trusteeDetails":{
             "firstName":"Sandy",
             "lastName":"Graham"
          },
          "trusteeContactDetails":{
             "emailAddress":"robert.smith@test.com",
             "phoneNumber":"0044-09876542334"
          }
       },
       {
          "hasCrn":true,
          "companyDetails":{
             "companyName":"Croudace Trustees Ltd"
          },
          "companyRegistrationNumber":{
             "value":"AB123456"
          },
          "companyPaye":{
             "value":"123AB45678"
          },
          "hasVat":true,
          "trusteeKind":"company",
          "companyVat":{
             "value":"123456789"
          },
          "companyAddress":{
             "country":"GB",
             "postcode":"SG6 4ET",
             "addressLine1":"Spirella Building",
             "addressLine2":"Bridge Rd",
             "addressLine3":"Letchworth Garden City"
          },
          "noUtrReason":"No utr availiable",
          "companyContactDetails":{
             "emailAddress":"admin@croudace.com",
             "phoneNumber":"0044-09876542312"
          },
          "trusteesCompanyAddressYears":"under_a_year",
          "hasUtr":false,
          "companyPreviousAddress":{
             "country":"GB",
             "postcode":"NE1 4AB",
             "addressLine1":"Flat 5a",
             "addressLine2":"Central Apartments",
             "addressLine3":"Waterloo Road",
             "addressLine4":"Newcastle"
          },
          "hasPaye":true
       },
       {
          "hasCrn":true,
          "companyDetails":{
             "companyName":"Jerram Trustees Ltd"
          },
          "companyRegistrationNumber":{
             "value":"AB123456"
          },
          "companyPaye":{
             "value":"123AB45678"
          },
          "hasVat":true,
          "trusteeKind":"company",
          "utr":{
             "value":"0123456789"
          },
          "companyVat":{
             "value":"123456789"
          },
          "companyAddress":{
             "country":"GB",
             "postcode":"DT9 3LN",
             "addressLine1":"Half Moon St",
             "addressLine2":"Sherborne",
             "addressLine3":"Dorset"
          },
          "companyContactDetails":{
             "emailAddress":"admin@abc-hotels.com",
             "phoneNumber":"0044-09876542312"
          },
          "trusteesCompanyAddressYears":"over_a_year",
          "hasUtr":true,
          "hasPaye":true
       },
       {
          "partnershipPaye":{
             "value":"123987654"
          },
          "utr":{
             "value":"0123456789"
          },
          "partnershipAddress":{
             "country":"GB",
             "postcode":"NE15 9RT",
             "addressLine1":"Unit 4",
             "addressLine2":"Newburn Industrial Estate",
             "addressLine3":"Shelley Rd",
             "addressLine4":"Newcastle Upon Tyne"
          },
          "partnershipAddressYears":"over_a_year",
          "hasVat":false,
          "partnershipContactDetails":{
             "emailAddress":"admin@illingworth-partnerships.com",
             "phoneNumber":"0044-09876542312"
          },
          "partnershipDetails":{
             "name":"Illingworth Trustee Partnerships"
          },
          "hasUtr":true,
          "trusteeKind":"partnership",
          "hasPaye":true
       },
       {
          "partnershipPaye":{
             "value":"123987654"
          },
          "utr":{
             "value":"0123456789"
          },
          "partnershipAddress":{
             "country":"GB",
             "postcode":"WC1X 8HR",
             "addressLine1":"28 Gray Inn Road",
             "addressLine2":"London"
          },
          "partnershipAddressYears":"under_a_year",
          "hasVat":false,
          "partnershipContactDetails":{
             "emailAddress":"admin@holborn-partners.com",
             "phoneNumber":"0044-09876542312"
          },
          "partnershipDetails":{
             "name":"Holborn Trustee Partnerships"
          },
          "hasUtr":true,
          "partnershipPreviousAddress":{
             "country":"GB",
             "postcode":"SR2 4AB",
             "addressLine1":"Exchange Station",
             "addressLine2":"Grangetown",
             "addressLine3":"Sunderland",
             "addressLine4":"Tyne and Wear"
          },
          "trusteeKind":"partnership",
          "hasPaye":true
       }
    ],
    "insurerAddress":{
       "country":"GB",
       "postcode":"NE3 4ER",
       "addressLine1":"Flat 4",
       "addressLine2":"London Road",
       "addressLine3":"Newcastle",
       "addressLine4":"Tyne and Wear"
    }
 }

```

* **Success Response:**

  * **Code:** 200 <br />

* **Example Success Response**

```json
{
   "processingDate":"2020-04-17",
   "formBundleNumber":"000020000000"
}
```

* **Error Response:**

  * **Code:** 400 BAD_REQUEST <br />
    **Content:** `{"statusCode":400,"message":"Bad Request without PSAId or request body"}`

  * **Code:** 400 BAD_REQUEST <br />
    **Content:** `{"statusCode":400,"message":"INVALID_PAYLOAD"}`

  OR anything else

  * **Code:** 500 INTERNAL_SERVER_ERROR <br />
