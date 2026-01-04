# NexusLedger: AI-Driven Event-Sourced Financial Engine

NexusLedger is a high-performance, distributed financial ledger built with **Spring Boot**, **Apache Kafka**, and **Local AI (Ollama/Llama 3)**.

## 🚀 Architecture Highlights
- **Event-Driven:** Uses Kafka to decouple transaction intake from processing.
- **AI Fraud Sentry:** Every transaction is analyzed by an integrated LLM before execution.
- **Double-Entry Integrity:** Ensures mathematical correctness and auditability.
- **Distributed Idempotency:** Prevents duplicate transactions across a distributed network.
- **Cloud-Native:** Fully containerized using Docker & Docker Compose.

## 🛠 Tech Stack
- **Backend:** Java 21, Spring Boot 3.5, Spring AI
- **Data:** PostgreSQL, Hibernate (JPA)
- **Messaging:** Apache Kafka (KRaft mode)
- **AI:** Ollama (Llama 3)
- **DevOps:** Docker, Micrometer (Actuator)

## 🚦 How to Run
1. Ensure **Ollama** is running with `llama3`.
2. Build the app: `./mvnw clean package -DskipTests`
3. Start the stack: `docker-compose up --build`
4. Send a transfer via Postman: `POST http://localhost:8080/api/ledger/transfer`
