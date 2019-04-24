# won-paypal-bot
A won bot to prepare and evaluate PayPal payments.

# Build instructions

Make sure you have the webofneeds artifacts in your local repository (by locally building the Webofneeds project) and then use maven to build this project
```
mvn install
```

# Skills
 - Creates a new Atom for every hint it receives and opens a Connection to the hint Atom
 - Denies all incomming Connections
 - Payment-Evaluation-Scheduler which checks open payments in a fixed interval
 - Status controlled Action Handler, which act different for each state to secure a correct and safe process
 - Clean data management: Only holds payments and corresponding data which are active

# TODO
 - Implement Status: EXPIRED, ERROR
 - ProposeToCancel from the Merchant
 - PayPal Credentials from the conf-properties file
 - Correct handling from Acepts-Events
 - Storage/Serialization of PaymentBridges
 - Discuss the idea of creating a new atom for the buyer instead of using the same atom as the merchant