Scheme
-----------------------
Returns scheme details.

* **URL**

  `/scheme`

* **Method**

  `GET`

*  **Request Header**
    
   `schemeIdType`
   
   `idNumber`
   
   `PSAId`

* **Success Response:**

  * **Code:** 200 <br />

* **Example Success Response**

```json
{
   "processingDate":"2017-12-17T09:30:47Z",
   "psaSchemeDetails":{
      "schemeDetails":{
         "srn":"S2400000007",
         "pstr":"24000007IN",
         "schemeStatus":"Wound-up",
         "schemeName":"Wound-up Master Trust Scheme",
         "isSchemeMasterTrust":true,
         "hasMoreThanTenTrustees":false,
         "currentSchemeMembers":"2 to 11",
         "futureSchemeMembers":"12 to 50",
         "isReguledSchemeInvestment":false,
         "isOccupationalPensionScheme":false,
         "schemeProvideBenefits":"Defined Benefits only",
         "schemeEstablishedCountry":"GB",
         "isSchemeBenefitsInsuranceCompany":true,
         "insuranceCompanyName":"Acme Insurance",
         "policyNumber":"123456",
         "insuranceCompanyAddressDetails":{
            "nonUKAddress":false,
            "line1":"Flat 4",
            "line2":"London Road",
            "line3":"Newcastle",
            "line4":"Tyne and Wear",
            "postalCode":"NE3 4ER",
            "countryCode":"GB"
         },
         "insuranceCompanyContactDetails":{
            "telephone":"0044-09876542312",
            "mobileNumber":"0044-09876542312",
            "fax":"0044-09876542312",
            "email":"acme@test.com"
         }
      },
      "establisherDetails":{
         "individualDetails":[
            {
               "personDetails":{
                  "firstName":"Robert",
                  "middleName":"James",
                  "lastName":"Smith",
                  "dateOfBirth":"1955-03-29"
               },
               "nino":"AA999999A",
               "utr":"1234567892",
               "correspondenceAddressDetails":{
                  "nonUKAddress":false,
                  "line1":"10 Westgate Road",
                  "line2":"South Gosforth",
                  "line3":"Newcastle",
                  "line4":"Tyne and Wear",
                  "postalCode":"NE3 5TR",
                  "countryCode":"GB"
               },
               "correspondenceContactDetails":{
                  "telephone":"0044-09876542334",
                  "mobileNumber":"0044-09876542312",
                  "fax":"0044-09876542312",
                  "email":"robert.smith@test.com"
               },
               "previousAddressDetails":{
                  "isPreviousAddressLast12Month":true,
                  "previousAddress":{
                     "nonUKAddress":false,
                     "line1":"Flat 5a",
                     "line2":"Central Apartments",
                     "line3":"Waterloo Road",
                     "line4":"Newcastle",
                     "postalCode":"NE1 4AB",
                     "countryCode":"GB"
                  }
               }
            }
         ]
      },
      "psaDetails":[
         {
            "psaid":"A2100005",
            "firstName":"Nigel",
            "middleName":"Robert",
            "lastName":"Smith",
            "relationshipType":"Primary",
            "relationshipDate":"2018-10-01"
         },
         {
            "psaid":"A2100021",
            "firstName":"Jennifer",
            "middleName":"Debra",
            "lastName":"Carter",
            "relationshipType":"Primary",
            "relationshipDate":"2019-01-01"
         },
         {
            "psaid":"A2100023",
            "firstName":"Alicia",
            "middleName":"Mary",
            "lastName":"Ryan",
            "relationshipType":"Primary",
            "relationshipDate":"2019-01-01"
         },
         {
            "psaid":"A2100025",
            "firstName":"Elizabeth",
            "lastName":"Bray",
            "relationshipType":"Primary",
            "relationshipDate":"2019-01-01"
         },
         {
            "psaid":"A2100031",
            "organizationOrPartnershipName":"Oxford Phones Ltd",
            "relationshipType":"Primary",
            "relationshipDate":"2019-01-01"
         },
         {
            "psaid":"A2100033",
            "organizationOrPartnershipName":"Amersham Building Supplies Ltd",
            "relationshipType":"Primary",
            "relationshipDate":"2019-01-01"
         },
         {
            "psaid":"A2100035",
            "organizationOrPartnershipName":"Warrington Business Services Ltd",
            "relationshipType":"Primary",
            "relationshipDate":"2019-01-01"
         },
         {
            "psaid":"A2100041",
            "organizationOrPartnershipName":"Riverside Partnerships",
            "relationshipType":"Primary",
            "relationshipDate":"2019-01-01"
         },
         {
            "psaid":"A2100043",
            "organizationOrPartnershipName":"Midwest Partnerships",
            "relationshipType":"Primary",
            "relationshipDate":"2019-01-01"
         },
         {
            "psaid":"A2100045",
            "organizationOrPartnershipName":"JDA Partnership",
            "relationshipType":"Primary",
            "relationshipDate":"2019-01-01"
         }
      ]
   }
}
```

