# ShopFlow - Enterprise E-Commerce & Order Lifecycle Platform

ShopFlow is an enterprise-grade e-commerce backend platform built with Java 17 and Spring Boot 3. The platform is designed to manage order state transitions, two-phase inventory reservations, dynamic product search and filtering, Redis catalog caching, and asynchronous event streaming via RabbitMQ.

---

## Order Lifecycle & Stock Reservation Flow

ShopFlow addresses common e-commerce concurrency challenges by ensuring transactional stock reservations during order placement and proper compensation if payments fail:

```text
  Customer Places Order
            |
            v
    [Check Stock]
            |
            v
   [Reserve Stock]  -----------------> If Insufficient: 422 Unprocessable Entity
            |
            v
  Order Status: PENDING
            |
            +-----------------------------------------+
            | Payment Succeeds                        | Payment Fails / Cancelled
            v                                         v
   [Commit Reserved Stock]                  [Release Reserved Stock]
            |                                         |
            v                                         v
  Order Status: CONFIRMED                   Order Status: CANCELLED
            |
            v
  Order Status: PROCESSING
            |
            v
  Order Status: SHIPPED ----> DELIVERED
```

---

## Technical Highlights

### 1. Order State Machine
- Strict state transition validation rules enforced by `OrderStateMachine` ensure orders proceed only through permitted lifecycles (`PENDING` -> `CONFIRMED` -> `PROCESSING` -> `SHIPPED` -> `DELIVERED`).
- Prevents invalid operations such as fulfilling or updating cancelled orders.

### 2. Two-Phase Stock Reservation
- When an order is placed, required quantities are moved to `reservedQuantity` using JPA Optimistic Locking (`@Version`).
- Upon payment confirmation, the reserved quantities are permanently committed.
- When an order is cancelled or times out, reserved quantities are released back to available inventory.

### 3. Composable Search & Filtering
- Dynamic criteria queries using Spring Data JPA `Specification` and `CriteriaBuilder`.
- Supports composable filtering by product title/description keyword, category slug, price range, and multi-field sorting.

### 4. Event-Driven Messaging
- Dispatches domain events (`OrderCreatedEvent`, `PaymentCompletedEvent`, `OrderCancelledEvent`) to RabbitMQ.
- Decoupled worker listeners process invoice records and customer notification delivery asynchronously.

---

## Technology Stack

- Language: Java 17
- Framework: Spring Boot 3.3.3
- Security: Spring Security 6, JWT (jjwt 0.12.6), BCrypt
- Persistence: Spring Data JPA, Hibernate, PostgreSQL 16
- Caching: Redis 7, Spring Cache Abstraction
- Messaging: RabbitMQ 3.13, Spring AMQP
- Documentation: SpringDoc OpenAPI 3 (Swagger UI)
- Containerization: Docker, Docker Compose
- Testing: JUnit 5, Mockito, Spring Boot Test
- CI/CD: GitHub Actions

---

## Project Structure

```text
shopflow/
├── src/
│   ├── main/
│   │   ├── java/com/shopflow/
│   │   │   ├── ShopflowApplication.java
│   │   │   ├── auth/             # JWT provider, filter, authentication endpoints
│   │   │   ├── common/           # Unified API responses, global error handlers
│   │   │   ├── config/           # Security, Redis, AMQP, OpenAPI configs
│   │   │   ├── event/            # AMQP events and publishers
│   │   │   ├── inventory/        # Stock management & reservation logic
│   │   │   ├── order/            # Order state machine, repository, service
│   │   │   ├── product/          # Product catalog, dynamic specifications, caching
│   │   │   ├── user/             # User domain & roles
│   │   │   └── worker/           # RabbitMQ async event consumers
│   │   └── resources/
│   │       └── application.yml
│   └── test/                     # Unit and integration tests
├── .github/workflows/ci.yml
├── docker-compose.yml
├── Dockerfile
├── render.yaml
└── pom.xml
```

---

## Local Deployment & Execution

### Run with Docker Compose
```bash
docker compose up --build
```

Access points:
- Application API: `http://localhost:8081`
- Swagger UI Documentation: `http://localhost:8081/swagger-ui.html`
- PostgreSQL: `localhost:5433`
- Redis: `localhost:6380`
- RabbitMQ Management Console: `http://localhost:15673` (guest / guest)

---

## REST API Endpoints

### Authentication & Account
- `POST /api/auth/register` - Register customer account.
- `POST /api/auth/login` - Authenticate and obtain JWT access token.

### Product Catalog & Search
- `GET /api/products` - Filter catalog with `search`, `category`, `minPrice`, `maxPrice`, pagination, and sorting.
- `GET /api/products/{id}` - Retrieve product details (Redis cached).
- `POST /api/products` - Create product with initial inventory (Admin/Manager role required).
- `GET /api/categories` - List product categories.

### Order Processing & Stock Reservation
- `POST /api/orders` - Place order and reserve inventory stock.
- `GET /api/orders/{id}` - Retrieve order details.
- `GET /api/orders/my-orders` - Retrieve authenticated customer orders.
- `POST /api/orders/{id}/confirm-payment` - Confirm payment and commit reserved stock.
- `POST /api/orders/{id}/cancel` - Cancel order and release reserved stock.
- `PATCH /api/orders/{id}/status` - Advance order lifecycle state (Staff role required).

---

## Testing

Run automated tests via Maven:
```bash
mvn clean test
```
Tests cover:
- State machine transition validations.
- Two-phase stock reservation, commit, and release behaviors.
- Dynamic JPA product specification filter operations.
