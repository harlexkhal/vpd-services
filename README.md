# VPD Microservice Finance MicroService Architecture Assessment

## Technologies
- **Java**: Used for implementing internal microservices.
- **JavaScript**: For building the API interface, enabling clients/partners to use transactional functionalities.
- **gRPC Server & Client**: Facilitates communication between internal microservices.
- **Spring Boot**: Simplifies the development of stand-alone, production-grade Spring-based Applications.
- **Maven (MVN)**: Used for managing project dependencies and building the application.

## Getting Started
This project covers basic functionalities of a finance application and is not a full-scale finance application. 

All microservices must be running concurrently for effective testing, as some microservices depend on one another. Reference this [High-Level Design Diagram](https://vpd-money-design-docs.vercel.app/#high-level-design) for more information.

### Guide
- In the parent root directory, run the following command:
  ```bash
  mvn clean install 
  ```

This command will install all dependencies, build target files for each microservice, and compile protobuf binaries.

## Microservices and Their Ports

- **Authentication Service**: Runs on Port 8080.
- **Authorization Service**: Runs on Port 8081.
- **Transaction Service**: Runs on Port 8082.
- **Node Service (API Interface)**: Runs on Port 3000.

Each of these services communicates with each other in some capacity, so they must all be active simultaneously.

Navigate to each microservice directory and follow the instructions in the `README.md` to start each service.

## Testing with Postman

Test the application using Postman with the pre-configured data collection. Click the button below to access the collection after you have all of the application started:

[![Run in Postman](https://run.pstmn.io/button.svg)](https://app.getpostman.com/run-collection/16211260-1163b7c1-63fe-4b55-81e2-4049fa0eee93?action=collection%2Ffork&source=rip_markdown&collection-url=entityId%3D16211260-1163b7c1-63fe-4b55-81e2-4049fa0eee93%26entityType%3Dcollection%26workspaceId%3De9f990a5-404d-45e8-91b4-9ad6d3c1933b)

visit [documentation](https://vpd-money-design-docs.vercel.app) page for more info about this project
