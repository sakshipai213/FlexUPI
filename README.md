# FlexUPI Credit Engine

FlexUPI Credit Engine is a high-performance, containerized Spring Boot application that simulates a "Credit on UPI" payment system. Designed to mirror real-world digital lending infrastructure, it enables users to execute transactions using a pre-approved digital credit line, process repayments in real time, and maintain complete double-entry financial auditability.

---

## Technical Highlights

* **Distributed Idempotency:** Integrates Redis-based atomic locks (`setIfAbsent`) tied to the `X-Idempotency-Key` header to block duplicate payment requests and prevent double-charging during network retries or concurrent clicks.
* **Double-Entry Ledger:** Enforces immutable `DEBIT` and `CREDIT` entries in PostgreSQL for every transaction and repayment, ensuring strict auditability and balance consistency.
* **Real-Time Balance & Overdraft Control:** Validates user credit limits dynamically and rejects overdraft attempts instantly using structured custom exceptions (`InsufficientCreditException`).
* **Pluggable Fee Strategy:** Implements the Strategy Pattern (`FeeCalculationStrategy`) to allow dynamic fee structures (e.g., promotional waivers, flat rates, or percentage fees) without altering core transaction logic.
* **Containerized Architecture:** Fully dockerized environment orchestrating Spring Boot, PostgreSQL, and Redis using Docker Compose for local development and cloud deployment.

---

## Tech Stack

* **Language & Framework:** Java 17, Spring Boot 3
* **Persistence & Caching:** PostgreSQL, Spring Data JPA, Redis
* **Documentation & Build:** Swagger / OpenAPI 3, Maven
* **Infrastructure:** Docker, Docker Compose, AWS EC2
* **Testing:** JUnit 5, Mockito

---

## System Architecture

```
[ Client / Mobile App ]
          |
          | 1. POST /api/v1/payments/credit-upi
          |    Header: X-Idempotency-Key: tx-1001
          v
[ FlexUPI Spring Boot Application ]
          |
          +---> 2. Acquire Redis Distributed Lock (tx-1001)
          |        - Blocks duplicate concurrent requests
          |
          +---> 3. Credit Limit & Fee Validation
          |        - Ensures availableCredit >= amount + fees
          |
          +---> 4. PostgreSQL Database Transaction
          |        - Atomically deducts available credit balance
          |        - Records Transaction record (SETTLED)
          |        - Writes balanced Ledger Entries (DEBIT & CREDIT)
          |
          v
[ HTTP 200 OK Response ]

```

---

## API Reference

### 1. Process UPI Credit Payment

* **HTTP Method:** `POST`
* **Path:** `/api/v1/payments/credit-upi`
* **Headers:** `X-Idempotency-Key: <unique-uuid>`
* **Request Body:**

```json
{
  "payerVpa": "john@upi",
  "payeeVpa": "merchant@upi",
  "amount": 1500.00
}

```

* **Response:** `200 OK`

```json
{
  "transactionId": "tx-8f92a10b",
  "status": "SETTLED",
  "amount": 1500.00,
  "remainingCredit": 48500.00
}

```

### 2. Process Credit Repayment

* **HTTP Method:** `POST`
* **Path:** `/api/v1/payments/repay`
* **Request Body:**

```json
{
  "vpa": "john@upi",
  "amount": 1500.00
}

```

* **Response:** `200 OK`

```json
{
  "vpa": "john@upi",
  "restoredCredit": 50000.00,
  "status": "SUCCESS"
}

```

### 3. Fetch Transaction History

* **HTTP Method:** `GET`
* **Path:** `/api/v1/payments/history/{vpa}`
* **Response:** `200 OK`

---

## Local Setup & Deployment

### Prerequisites

* JDK 17 or higher
* Maven 3.8+
* Docker & Docker Compose

### Step 1: Clone the Repository

```bash
git clone https://github.com/<YOUR_GITHUB_USERNAME>/flexupi.git
cd flexupi

```

### Step 2: Run Unit Tests

```bash
mvn clean test

```

### Step 3: Run with Docker Compose

To launch PostgreSQL, Redis, and the FlexUPI application together in detached mode:

```bash
docker compose up --build -d

```

### Step 4: Verify Deployment

Check the status of running containers:

```bash
docker compose ps

```

Access the live Swagger UI documentation in your browser:

```
http://localhost:8080/swagger-ui.html

```

---

## Deployment on AWS EC2

1. Launch an Ubuntu 24.04 LTS EC2 instance (`t2.micro` or `t3.micro`).
2. Configure Security Group inbound rules:
* Port 22 (SSH)
* Port 8080 (Custom TCP for Spring Boot)


3. SSH into the instance and install Docker:

```bash
sudo apt update && sudo apt install -y docker.io docker-compose-v2 git
sudo usermod -aG docker ubuntu
newgrp docker

```

4. Clone and launch the application:

```bash
git clone https://github.com/<YOUR_GITHUB_USERNAME>/flexupi.git
cd flexupi
docker compose up --build -d

```

5. Access Swagger UI on AWS: `
