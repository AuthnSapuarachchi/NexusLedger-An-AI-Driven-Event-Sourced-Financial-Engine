# 🏦 NEXUS LEDGER: AI-Powered Event-Driven Financial Engine

**Nexus Ledger** is a high-integrity FinTech platform built to demonstrate modern distributed systems architecture. It combines **Event Sourcing** via Kafka, **Real-Time Fraud Detection** using local AI, and a responsive **React** interface.



---

## 🚀 Key Architectural Features

* **Event-Driven Core**: Uses **Apache Kafka** to decouple the API layer from the ledger processing, ensuring high availability and system resilience.
* **AI Fraud Sentry**: Integrates a local **Llama 3 (via Ollama)** model to perform real-time risk assessment on transactions without compromising data privacy.
* **Atomic Ledger Logic**: Implements strict **Double-Entry Bookkeeping** with **Optimistic Locking** (`@Version`) to ensure zero data corruption during concurrent transfers.
* **Real-Time Sync**: Leverages **WebSockets (STOMP)** to push balance updates and AI audit results to the UI instantly.
* **Reliability**: Built with **Idempotent API** patterns using custom headers to prevent duplicate transactions during network retries.

---

## 🛠️ Technical Stack

| Layer | Technology |
| :--- | :--- |
| **Frontend** | React 18, Vite, Tailwind CSS, Axios, Lucide Icons |
| **Backend** | Spring Boot 3.4+, Spring Security, Spring AI, JPA/Hibernate |
| **Messaging** | Apache Kafka |
| **Database** | PostgreSQL 16 |
| **AI Engine** | Ollama (Running Llama 3) |
| **Security** | GitHub OAuth2 (JIT Provisioning) |

---

## 🏗️ System Flow

1.  **Request**: User initiates a transfer; the React client generates a unique `X-Idempotency-Key`.
2.  **Ingestion**: The REST Controller validates the user session and publishes the intent to the `financial-transactions` Kafka topic.
3.  **Analysis**: The `TransactionConsumer` picks up the message and sends the metadata to the **Llama 3 AI** for a "Safe/Fraud" verdict.
4.  **Execution**: If cleared, the `LedgerService` executes an atomic SQL transaction to update balances and record journal entries.
5.  **Feedback**: A WebSocket notification is pushed to the client, triggering a live UI state update.



---

## 🚥 Getting Started

### 1. Prerequisites
* Docker & Docker Compose
* Java 21 (JDK)
* Node.js & npm
* Ollama (run `ollama pull llama3`)

### 2. Infrastructure Setup
```bash
# Start the supporting services
docker-compose up -d postgres-db kafka
