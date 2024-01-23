# Demo Application for hubpay

## Overview
This is a Spring Boot 2 JPA (with Hibernate) application that uses an embedded H2 database.

## Data setup
To initialise the embedded DB with the data or your choice, simply update **/src/main/resources/import.sql** file.
This file is executed automatically at application startup. Hibernate is configured to create the database schema
at startup and delete the schema when the session ends (i.e. 'create-drop' mode).

## Notes of testing
I chose to keep it simple and use @SpringBootTest for 'integration' testing of the service and controller. In a larger
project, I would likely mock the repository/service layers and use unit tests (faster and easier to test edge cases) in
addition to end-to-end tests (in a dedicated test environment).

## Notes on transaction isolation
The mutation methods in WalletService (i.e. addFunds & withdrawFunds) use transaction isolation level of
REPEATABLE_READ which is sufficient to prevent the Lost Update anomaly for SQL Server and PostgreSQL. If using Oracle
or MySQL, the isolation level will need to be SERIALIZABLE to prevent the Lost Update anomaly.

REPEATABLE_READ is, in effect, a row-level lock (akin to pessimistic locking) which prevents any other transaction
from mutating the row. For better performance, a less strict isolation level could be considered (e.g. READ COMMITTED)
but it is anticipated that, for a virtual wallet, integrity is far more important than high-performance updates to
the wallet (especially when double-submit prevention is a stated requirement).

## Notes on logging
For purposes of this demo, logging has been omitted but, of course, a Production-ready application would include
logging and other observability measures.

## Endpoints
The following endpoints are available:

### GET `/api/wallet/{id}`
Gets current  balance of wallet with id = {id}.

Response:
```json
{
  "balance": 200.00
}
```

### POST `/api/wallet/{id}/deposit`
Deposits funds into wallet with id = {id}.

Request:
```json
{
  "amount": 25.00
}
```

Response (balance after transaction):
```json
{
  "balance": 225.00
}
```

### POST `/api/wallet/{id}/withdraw`
Withdraws funds from wallet with id = {id}.

Request:
```json
{
  "amount": 50.00
}
```

Response (balance after transaction):
```json
{
  "balance": 175.00
}
```

### GET `/api/wallet/{id}/transactions`
Lists transactions for wallet with id = {id} (in descending order of transaction date).
Provides a paginated response and supports optional pagination query parameters.
By default will return first page (pageNumber=0) of up to 10 results (pageSize=10).

Response (default pagination):
```json
{
  "pageNumber": 0,
  "pageSize": 10,
  "transactions": [
    {
      "id": 2,
      "amount": -50.00,
      "timestamp": "2024-01-21T15:42:46.540023"
    },
    {
      "id": 1,
      "amount": 25.00,
      "timestamp": "2024-01-21T15:42:02.676016"
    }
  ]
}
```

Request (specific pagination): `http://localhost:8080/api/wallet/1/transactions?pageNumber=1&pageSize=1`

Response (specific pagination):
```json
{
  "pageNumber": 1,
  "pageSize": 1,
  "transactions": [
    {
      "id": 1,
      "amount": 25.00,
      "timestamp": "2024-01-21T15:42:02.676016"
    }
  ]
}
```

## How to execute
Open the project as a Maven project in an IDE that supports Java development and recognises Spring Boot (e.g. IntelliJ).
Create a run configuration to executes **com.example.demo.DemoApplication**.

Alternatively, ensure you have Maven installed, open a command prompt, change to folder where you unzipped the source
code and enter `mvn spring-boot:run`.

The API will be available at `http://localhost:8080` (e.g. `http://localhost:8080/api/wallet/1`).
