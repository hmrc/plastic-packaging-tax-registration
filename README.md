
# Plastic Packaging Tax Registration (PPT)


This is the Scala microservice responsible for the transient storage of PPT registration information, which is part of the PPT tax regime, as discussed in this [GovUk Guidance](https://www.gov.uk/government/publications/introduction-of-plastic-packaging-tax/plastic-packaging-tax)

Other related PPT services:
- Frontend service: [plastic-packaging-tax-registration-frontend](https://github.com/hmrc/plastic-packaging-tax-registration-frontend)
- Stubs: [plastic-packaging-tax-stub](https://github.com/hmrc/plastic-packaging-tax-stub)
- Returns service: [plastic-packaging-tax-returns-frontend](https://github.com/hmrc/plastic-packaging-tax-returns-frontend)
- Returns service: [plastic-packaging-tax-returns](https://github.com/hmrc/plastic-packaging-tax-returns)

### How to run the service

These are the steps to the Plastic Packaging Tax Registrations digital service, of which this microservice is part of.

* Start a MongoDB instance

* Start the microservices
 
```
# Start the plastic packaging services and dependencies 
sm2 --start INCORPORATED_ENTITY_IDENTIFICATION_ALL PLASTIC_PACKAGING_TAX_ALL EMAIL_VERIFICATION_ALL

# confirm all services are running
sm2 -s 
```

* Visit http://localhost:9949/auth-login-stub/gg-sign-in
* Enter the redirect url: http://localhost:8503/plastic-packaging-tax/start and press **Submit**.
  

### Precheck

Before submitting a commit or pushing code remotely, please run  
```
./precheck.sh
```
This will execute unit and integration tests, check the Scala style and code coverage

### Scalastyle

Project contains `scalafmt` plugin.

Commands for code formatting:

```
sbt scalafmt        # format compile sources
sbt test:scalafmt   # format test sources
sbt sbt:scalafmt    # format .sbt source
```

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").

