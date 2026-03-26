# Database Schema Design - Order Processing Microservices System

**Version:** 1.0  
**Status:** Production Ready

---

## Overview

The system uses a **polyglot persistence** approach:
- **PostgreSQL:** Product, Order, Payment services (relational data)
- **MongoDB:** Customer, Notification services (document-oriented data)
- **Kafka:** Event streaming for inter-service communication

---

## 1. PRODUCT SERVICE DATABASE (PostgreSQL)

### Database Name: `product`
### Port: `5432`

#### Table: `category`

```sql
CREATE TABLE IF NOT EXISTS category (
    id          INTEGER NOT NULL PRIMARY KEY,
    description VARCHAR(255),
    name        VARCHAR(255)
);
```

**Columns:**
| Column | Type | Constraint | Description |
|--------|------|-----------|-------------|
| `id` | INTEGER | PK | Category identifier (auto-generated) |
| `name` | VARCHAR(255) | - | Category name (e.g., "Keyboards", "Monitors") |
| `description` | VARCHAR(255) | - | Category description |

**Relationships:**
- One-to-Many with `product` table (ONE category has MANY products)

**Indexes:**
- Primary key on `id`
- Optional: Index on `name` for fast lookups

---

#### Table: `product`

```sql
CREATE TABLE IF NOT EXISTS product (
    id                 INTEGER NOT NULL PRIMARY KEY,
    available_quantity DOUBLE PRECISION NOT NULL,
    description        VARCHAR(255),
    name               VARCHAR(255),
    price              NUMERIC(38, 2),
    category_id        INTEGER
        CONSTRAINT fk1mtsbur82frn64de7balymq9s
            REFERENCES category
);
```

**Columns:**
| Column | Type | Constraint | Description |
|--------|------|-----------|-------------|
| `id` | INTEGER | PK | Product identifier (auto-generated) |
| `name` | VARCHAR(255) | - | Product name |
| `description` | VARCHAR(255) | - | Product description |
| `price` | NUMERIC(38, 2) | - | Product price (e.g., 99.99) |
| `available_quantity` | DOUBLE PRECISION | NOT NULL | Stock quantity available |
| `category_id` | INTEGER | FK → category.id | Reference to category |

**Relationships:**
- Many-to-One with `category` table
- **Foreign Key:** `category_id` → `category.id`

**Indexes:**
- Primary key on `id`
- Foreign key on `category_id`
- Optional: Index on `name` for product search
- **Redis Cache:** Products cached by ID (key: `products::1`)

**Entity Class:**
```java
@Entity
public class Product {
    @Id
    @GeneratedValue
    private Integer id;
    
    private String name;
    private String description;
    private double availableQuantity;
    private BigDecimal price;
    
    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;
}
```

**Sample Data:**
- Mechanical Keyboard 1 | Price: $99.99 | Stock: 10 | Category: Keyboards
- 4K Monitor 1 | Price: $399.99 | Stock: 30 | Category: Monitors
- RGB Gaming Mouse 1 | Price: $59.99 | Stock: 30 | Category: Mice

---

#### Sequences

```sql
CREATE SEQUENCE IF NOT EXISTS category_seq INCREMENT BY 50;
CREATE SEQUENCE IF NOT EXISTS product_seq INCREMENT BY 50;
```

**Purpose:** Auto-increment IDs for `category` and `product` tables (incrementing by 50 for batch operations)

---

## 2. ORDER SERVICE DATABASE (PostgreSQL)

### Database Name: `order`
### Port: `5432`

#### Table: `customer_order` (Orders)

```sql
CREATE TABLE customer_order (
    id              INTEGER PRIMARY KEY,
    reference       VARCHAR(255) UNIQUE NOT NULL,
    total_amount    NUMERIC(38, 2),
    payment_method  VARCHAR(50),
    customer_id     VARCHAR(255),
    created_date    TIMESTAMP,
    last_modified_date TIMESTAMP
);
```

**Columns:**
| Column | Type | Constraint | Description |
|--------|------|-----------|-------------|
| `id` | INTEGER | PK | Order identifier (auto-generated) |
| `reference` | VARCHAR(255) | UNIQUE, NOT NULL | Order reference number (e.g., "ORD-2026-001") |
| `total_amount` | NUMERIC(38, 2) | - | Total order amount |
| `payment_method` | VARCHAR(50) | - | Payment method (CARD, PAYPAL, BANK_TRANSFER) |
| `customer_id` | VARCHAR(255) | - | Reference to Customer (from Customer Service) |
| `created_date` | TIMESTAMP | NOT NULL | Order creation timestamp |
| `last_modified_date` | TIMESTAMP | - | Last modification timestamp |

**Relationships:**
- One-to-Many with `customer_line` table (ONE order has MANY order lines)
- Many-to-One with Payment Service (order has ONE payment)

**Indexes:**
- Primary key on `id`
- Unique index on `reference` (for fast order lookup)
- Foreign key on `customer_id` (implicit)

**Entity Class:**
```java
@Entity
@Table(name = "customer_order")
@EntityListeners(AuditingEntityListener.class)
public class Order {
    @Id
    @GeneratedValue
    private Integer id;
    
    @Column(unique = true, nullable = false)
    private String reference;
    
    private BigDecimal totalAmount;
    
    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;
    
    private String customerId;
    
    @OneToMany(mappedBy = "order")
    private List<OrderLine> orderLines;
    
    @CreatedDate
    private LocalDateTime createdDate;
    
    @LastModifiedDate
    private LocalDateTime lastModifiedDate;
}
```

**Sample Data:**
- Reference: "ORD-2026-001" | Total: $199.98 | Payment: CARD | Customer: "cust-123"
- Reference: "ORD-2026-002" | Total: $599.99 | Payment: PAYPAL | Customer: "cust-456"

---

#### Table: `customer_line` (Order Line Items)

```sql
CREATE TABLE customer_line (
    id         INTEGER PRIMARY KEY,
    order_id   INTEGER,
    product_id INTEGER,
    quantity   DOUBLE PRECISION,
    CONSTRAINT fk_order
        FOREIGN KEY (order_id) REFERENCES customer_order(id)
);
```

**Columns:**
| Column | Type | Constraint | Description |
|--------|------|-----------|-------------|
| `id` | INTEGER | PK | Order line identifier |
| `order_id` | INTEGER | FK → customer_order.id | Reference to order |
| `product_id` | INTEGER | - | Reference to Product (from Product Service) |
| `quantity` | DOUBLE PRECISION | - | Quantity ordered |

**Relationships:**
- Many-to-One with `customer_order` table (MANY lines belong to ONE order)
- Many-to-One with Product Service (product reference)

**Indexes:**
- Primary key on `id`
- Foreign key on `order_id`
- Optional: Index on `product_id` for fast product lookups

**Entity Class:**
```java
@Entity
@Table(name = "customer_line")
public class OrderLine {
    @Id
    @GeneratedValue
    private Integer id;
    
    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order;
    
    private Integer productId;
    private double quantity;
}
```

**Sample Data:**
- Order ORD-2026-001, Product: 1, Quantity: 2
- Order ORD-2026-001, Product: 5, Quantity: 1
- Order ORD-2026-002, Product: 6, Quantity: 1

---

#### Payment Method Enum
```java
public enum PaymentMethod {
    CARD,
    PAYPAL,
    BANK_TRANSFER
}
```

---

## 3. PAYMENT SERVICE DATABASE (PostgreSQL)

### Database Name: `payment`
### Port: `5432`

#### Table: `payment`

```sql
CREATE TABLE payment (
    id                   INTEGER PRIMARY KEY,
    amount               NUMERIC(38, 2),
    payment_method       VARCHAR(50),
    order_id             INTEGER,
    created_date         TIMESTAMP NOT NULL,
    last_modified_date   TIMESTAMP
);
```

**Columns:**
| Column | Type | Constraint | Description |
|--------|------|-----------|-------------|
| `id` | INTEGER | PK | Payment identifier (auto-generated) |
| `amount` | NUMERIC(38, 2) | - | Payment amount |
| `payment_method` | VARCHAR(50) | - | Payment method (CARD, PAYPAL, BANK_TRANSFER) |
| `order_id` | INTEGER | - | Reference to Order (from Order Service) |
| `created_date` | TIMESTAMP | NOT NULL | Payment creation timestamp |
| `last_modified_date` | TIMESTAMP | - | Last modification timestamp |

**Relationships:**
- Many-to-One with Order Service (many payments can reference orders)

**Indexes:**
- Primary key on `id`
- Optional: Index on `order_id` for fast lookup
- Optional: Index on `created_date` for payment history

**Entity Class:**
```java
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "payment")
public class Payment {
    @Id
    @GeneratedValue
    private Integer id;
    
    private BigDecimal amount;
    
    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;
    
    private Integer orderId;
    
    @CreatedDate
    @Column(updatable = false, nullable = false)
    private LocalDateTime createdDate;
    
    @LastModifiedDate
    @Column(insertable = false)
    private LocalDateTime lastModifiedDate;
}
```

**Sample Data:**
- ID: 1, Amount: $199.98, Method: CARD, Order: 1
- ID: 2, Amount: $599.99, Method: PAYPAL, Order: 2

---

## 4. CUSTOMER SERVICE DATABASE (MongoDB)

### Database Name: `customer`
### Port: `27017`

#### Collection: `customer` (Document-oriented)

```javascript
{
    "_id": ObjectId("..."),
    "id": "cust-123",
    "firstname": "John",
    "lastname": "Doe",
    "email": "john.doe@example.com",
    "address": {
        "street": "123 Main St",
        "houseNumber": "Apt 4B",
        "zipCode": "10001"
    }
}
```

**Document Schema:**
| Field | Type | Description |
|-------|------|-------------|
| `_id` | ObjectId | MongoDB internal ID |
| `id` | String | Customer identifier (stored in Redis cache) |
| `firstname` | String | Customer first name |
| `lastname` | String | Customer last name |
| `email` | String | Customer email address |
| `address` | Object | Embedded address information |
| `address.street` | String | Street name |
| `address.houseNumber` | String | House or apartment number |
| `address.zipCode` | String | Postal code |

**Embedded Document:**
```javascript
"address": {
    "street": "123 Main St",
    "houseNumber": "Apt 4B",
    "zipCode": "10001"
}
```

**Indexes:**
- Automatic index on `_id`
- Optional: Unique index on `email` field
- Optional: Index on `firstname`, `lastname` for search

**Entity Class:**
```java
@Document
public class Customer {
    @Id
    private String id;
    
    private String firstname;
    private String lastname;
    private String email;
    
    private Address address;
}

public class Address {
    private String street;
    private String houseNumber;
    private String zipCode;
}
```

**Sample Data:**
```json
{
    "id": "cust-123",
    "firstname": "John",
    "lastname": "Doe",
    "email": "john.doe@example.com",
    "address": {
        "street": "123 Main St",
        "houseNumber": "Apt 4B",
        "zipCode": "10001"
    }
}

{
    "id": "cust-456",
    "firstname": "Jane",
    "lastname": "Smith",
    "email": "jane.smith@example.com",
    "address": {
        "street": "456 Oak Ave",
        "houseNumber": "Suite 200",
        "zipCode": "10002"
    }
}
```

---

## 5. NOTIFICATION SERVICE DATABASE (MongoDB)

### Database Name: `notification`
### Port: `27017`

#### Collection: `notification` (Document-oriented)

```javascript
{
    "_id": ObjectId("..."),
    "type": "ORDER_CONFIRMATION",
    "notificationDate": ISODate("2026-03-26T10:30:00.000Z"),
    "orderConfirmation": {
        "orderReference": "ORD-2026-001",
        "customerId": "cust-123",
        "totalAmount": 199.98,
        "products": [
            {
                "productId": 1,
                "name": "Mechanical Keyboard",
                "quantity": 2,
                "price": 99.99
            }
        ]
    },
    "paymentConfirmation": null
}
```

**Document Schema:**
| Field | Type | Description |
|-------|------|-------------|
| `_id` | ObjectId | MongoDB internal ID |
| `type` | String | Notification type (ORDER_CONFIRMATION, PAYMENT_CONFIRMATION) |
| `notificationDate` | DateTime | When notification was created |
| `orderConfirmation` | Object | Order details (if type = ORDER_CONFIRMATION) |
| `paymentConfirmation` | Object | Payment details (if type = PAYMENT_CONFIRMATION) |

**Nested: Order Confirmation**
```javascript
"orderConfirmation": {
    "orderReference": "ORD-2026-001",
    "customerId": "cust-123",
    "totalAmount": 199.98,
    "products": [
        {
            "productId": 1,
            "name": "Mechanical Keyboard",
            "quantity": 2,
            "price": 99.99
        }
    ]
}
```

**Nested: Payment Confirmation**
```javascript
"paymentConfirmation": {
    "orderId": 1,
    "amount": 199.98,
    "paymentMethod": "CARD",
    "status": "SUCCESS"
}
```

**Indexes:**
- Automatic index on `_id`
- Optional: Index on `type` for filtering notifications
- Optional: Index on `notificationDate` for timeline queries
- TTL Index: Auto-delete notifications older than 30 days (optional)

**Entity Class:**
```java
@Document
public class Notification {
    @Id
    private String id;
    
    private NotificationType type;
    private LocalDateTime notificationDate;
    
    private OrderConfirmation orderConfirmation;
    private PaymentConfirmation paymentConfirmation;
}

public enum NotificationType {
    ORDER_CONFIRMATION,
    PAYMENT_CONFIRMATION
}

public class OrderConfirmation {
    private String orderReference;
    private String customerId;
    private BigDecimal totalAmount;
    private List<Product> products;
}

public class PaymentConfirmation {
    private Integer orderId;
    private BigDecimal amount;
    private PaymentMethod paymentMethod;
    private String status;
}
```

**Sample Data:**
```json
{
    "type": "ORDER_CONFIRMATION",
    "notificationDate": "2026-03-26T10:30:00Z",
    "orderConfirmation": {
        "orderReference": "ORD-2026-001",
        "customerId": "cust-123",
        "totalAmount": 199.98,
        "products": [
            {
                "productId": 1,
                "name": "Mechanical Keyboard",
                "quantity": 2,
                "price": 99.99
            }
        ]
    }
}

{
    "type": "PAYMENT_CONFIRMATION",
    "notificationDate": "2026-03-26T10:31:00Z",
    "paymentConfirmation": {
        "orderId": 1,
        "amount": 199.98,
        "paymentMethod": "CARD",
        "status": "SUCCESS"
    }
}
```

---

## 6. CACHING LAYER (Redis)

### Redis Instance
- **Host:** localhost
- **Port:** 6379
- **Database:** 0

### Cache Keys Strategy

#### Product Cache (Product Service)
```
Key Pattern: products::{id}
Example: products::1
Value: Serialized ProductResponse
TTL: 600 seconds (10 minutes)
```

```
Key Pattern: productsList
Value: Serialized List<ProductResponse>
TTL: 600 seconds (10 minutes)
```

**Cache Operations:**
```
GET products::1          # Fetch product with ID 1
SET products::1 <value> EX 600  # Cache product for 10 min
DEL products::*          # Clear all product cache (on purchase)
```

---

## 7. COMPLETE DATABASE ARCHITECTURE DIAGRAM

```
┌─────────────────────────────────────────────────────────────────────┐
│                    MICROSERVICES ARCHITECTURE                        │
└─────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────┐
│   PRODUCT SERVICE (8050)         │
│   ┌──────────────────────────┐   │
│   │  PostgreSQL: product     │   │
│   ├──────────────────────────┤   │
│   │ ┌─────────────────────┐  │   │
│   │ │  category           │  │   │
│   │ │  ├─ id (PK)        │  │   │
│   │ │  ├─ name           │  │   │
│   │ │  └─ description    │  │   │
│   │ └─────────────────────┘  │   │
│   │        ↑                 │   │
│   │        │ (1:N)           │   │
│   │ ┌─────────────────────┐  │   │
│   │ │  product            │  │   │
│   │ │  ├─ id (PK)        │  │   │
│   │ │  ├─ name           │  │   │
│   │ │  ├─ price          │  │   │
│   │ │  ├─ available_qty  │  │   │
│   │ │  ├─ description    │  │   │
│   │ │  └─ category_id(FK)│  │   │
│   │ └─────────────────────┘  │   │
│   │         ↕                │   │
│   │    Redis Cache           │   │
│   │    products::{id}        │   │
│   │    (TTL: 10 min)         │   │
│   └──────────────────────────┘   │
└──────────────────────────────────┘

┌──────────────────────────────────┐
│   ORDER SERVICE (8070)           │
│   ┌──────────────────────────┐   │
│   │  PostgreSQL: order       │   │
│   ├──────────────────────────┤   │
│   │ ┌─────────────────────┐  │   │
│   │ │ customer_order      │  │   │
│   │ │ ├─ id (PK)         │  │   │
│   │ │ ├─ reference(UQ)   │  │   │
│   │ │ ├─ total_amount    │  │   │
│   │ │ ├─ payment_method  │  │   │
│   │ │ ├─ customer_id     │  │   │
│   │ │ ├─ created_date    │  │   │
│   │ │ └─ last_mod_date   │  │   │
│   │ └─────────────────────┘  │   │
│   │         ↓ (1:N)          │   │
│   │ ┌─────────────────────┐  │   │
│   │ │ customer_line       │  │   │
│   │ │ ├─ id (PK)         │  │   │
│   │ │ ├─ order_id (FK)   │  │   │
│   │ │ ├─ product_id      │  │   │
│   │ │ └─ quantity        │  │   │
│   │ └─────────────────────┘  │   │
│   └──────────────────────────┘   │
└──────────────────────────────────┘

┌──────────────────────────────────┐
│   PAYMENT SERVICE (8060)         │
│   ┌──────────────────────────┐   │
│   │  PostgreSQL: payment     │   │
│   ├──────────────────────────┤   │
│   │ ┌─────────────────────┐  │   │
│   │ │ payment             │  │   │
│   │ │ ├─ id (PK)         │  │   │
│   │ │ ├─ amount          │  │   │
│   │ │ ├─ payment_method  │  │   │
│   │ │ ├─ order_id (FK)   │  │   │
│   │ │ ├─ created_date    │  │   │
│   │ │ └─ last_mod_date   │  │   │
│   │ └─────────────────────┘  │   │
│   └──────────────────────────┘   │
└──────────────────────────────────┘

┌──────────────────────────────────┐
│   CUSTOMER SERVICE (8090)        │
│   ┌──────────────────────────┐   │
│   │  MongoDB: customer       │   │
│   ├──────────────────────────┤   │
│   │ customer (Collection)    │   │
│   │ ├─ _id (ObjectId)       │   │
│   │ ├─ id                   │   │
│   │ ├─ firstname            │   │
│   │ ├─ lastname             │   │
│   │ ├─ email                │   │
│   │ └─ address (embedded)   │   │
│   │    ├─ street            │   │
│   │    ├─ houseNumber       │   │
│   │    └─ zipCode           │   │
│   └──────────────────────────┘   │
└──────────────────────────────────┘

┌──────────────────────────────────┐
│   NOTIFICATION SERVICE (8040)    │
│   ┌──────────────────────────┐   │
│   │  MongoDB: notification   │   │
│   ├──────────────────────────┤   │
│   │ notification (Collection)│   │
│   │ ├─ _id (ObjectId)       │   │
│   │ ├─ type                 │   │
│   │ ├─ notificationDate     │   │
│   │ ├─ orderConfirmation    │   │
│   │ │  ├─ orderReference   │   │
│   │ │  ├─ customerId       │   │
│   │ │  ├─ totalAmount      │   │
│   │ │  └─ products[]       │   │
│   │ └─ paymentConfirmation │   │
│   │    ├─ orderId          │   │
│   │    ├─ amount           │   │
│   │    ├─ paymentMethod    │   │
│   │    └─ status           │   │
│   └──────────────────────────┘   │
└──────────────────────────────────┘
```

---

## 8. DATA FLOW DIAGRAMS

### Order Creation Flow

```
Client
  │
  ├─ POST /orders
  │
  ▼
OrderService (PostgreSQL)
  │
  ├─ INSERT INTO customer_order (reference, total_amount, ...)
  ├─ INSERT INTO customer_line (order_id, product_id, quantity) [multiple]
  │
  ├─ Publish: OrderConfirmation event to Kafka
  │
  ▼
NotificationService (MongoDB)
  │
  └─ INSERT INTO notification (type: ORDER_CONFIRMATION, ...)
      ▼
      Send Email to Customer
```

### Payment Processing Flow

```
Client
  │
  ├─ POST /payments
  │
  ▼
PaymentService (PostgreSQL)
  │
  ├─ INSERT INTO payment (amount, order_id, ...)
  │
  ├─ Publish: PaymentConfirmation event to Kafka
  │
  ▼
NotificationService (MongoDB)
  │
  └─ INSERT INTO notification (type: PAYMENT_CONFIRMATION, ...)
      ▼
      Send Email to Customer
```

### Product Lookup Flow

```
Client
  │
  ├─ GET /products/{id}
  │
  ▼
ProductService
  │
  ├─ Check Redis Cache (products::{id})
  │
  ├─ CACHE HIT → Return cached ProductResponse
  │
  ├─ CACHE MISS → Query PostgreSQL
  │              ├─ SELECT FROM product WHERE id = {id}
  │              ├─ JOIN category ON product.category_id = category.id
  │              ├─ Store in Redis (TTL: 10 min)
  │              └─ Return ProductResponse
```

---

## 9. KEY CONSTRAINTS & RELATIONSHIPS

### Foreign Key Relationships

| From Table | To Table | Relationship | Type |
|-----------|----------|-------------|------|
| product | category | product.category_id → category.id | Many-to-One |
| customer_line | customer_order | customer_line.order_id → customer_order.id | Many-to-One |
| payment | customer_order (implicit) | payment.order_id → customer_order.id | Many-to-One |

### Unique Constraints

| Table | Column | Reason |
|-------|--------|--------|
| customer_order | reference | Each order must have unique reference number |
| customer (MongoDB) | email | Each customer should have unique email |

---

## 10. INDEXING STRATEGY

### PostgreSQL Indexes

| Table | Index | Type | Purpose |
|-------|-------|------|---------|
| product | PK (id) | Primary | Fast lookup by ID |
| product | FK (category_id) | Foreign | Join operations |
| category | PK (id) | Primary | Fast category lookup |
| customer_order | PK (id) | Primary | Fast order lookup |
| customer_order | UNIQUE (reference) | Unique | Fast lookup by order reference |
| customer_line | FK (order_id) | Foreign | Join with customer_order |
| payment | PK (id) | Primary | Fast payment lookup |

### MongoDB Indexes

| Collection | Field | Index Type | Purpose |
|-----------|-------|-----------|---------|
| customer | _id | Automatic | Fast document lookup |
| customer | email | Unique (recommended) | Fast lookup by email |
| notification | type | Regular | Filter notifications |
| notification | notificationDate | Regular | Timeline queries |

### Redis Cache Indexes

| Key Pattern | Purpose |
|------------|---------|
| products::{id} | Cache individual products by ID |
| productsList | Cache all products list |

---

## 11. MIGRATION STRATEGY

### Flyway Migrations (Product Service)

**V1__init_database.sql**
- Creates `category` and `product` tables
- Creates sequences for ID generation

**V2__insert_data.sql**
- Inserts sample categories (5 categories)
- Inserts sample products (25 products across all categories)

### Schema Evolution

1. **Add Column:** Use ALTER TABLE ADD COLUMN
2. **Remove Column:** Use ALTER TABLE DROP COLUMN
3. **Rename Column:** Use ALTER TABLE RENAME COLUMN
4. **Modify Type:** Use ALTER TABLE ALTER COLUMN

---

## 12. PERFORMANCE CHARACTERISTICS

### Query Patterns

```sql
-- Product Lookup (Most Common - CACHED)
SELECT * FROM product WHERE id = ? 
  JOIN category ON product.category_id = category.id;
-- Time: <1ms (Redis) | 15-50ms (DB)

-- Order Lookup
SELECT * FROM customer_order WHERE reference = ?;
-- Time: 5-10ms (indexed on reference)

-- Order Details with Lines
SELECT * FROM customer_order WHERE id = ?;
SELECT * FROM customer_line WHERE order_id = ?;
-- Time: 5-10ms each query

-- Customer Lookup (MongoDB)
db.customer.findOne({ email: "..." })
-- Time: 5-10ms (indexed on email)
```

### Connection Pool Settings

**Product Service (PostgreSQL):**
```yaml
hikari:
  maximum-pool-size: 10
  minimum-idle: 5
  connection-timeout: 20000
```

**Redis (Jedis):**
```yaml
jedis:
  pool:
    max-active: 8
    max-idle: 8
    min-idle: 0
```

---

## 13. DATA BACKUP & RECOVERY

### PostgreSQL Backup

```bash
# Full backup
pg_dump -h localhost -U niskuldeep product > product_backup.sql

# Restore
psql -h localhost -U niskuldeep product < product_backup.sql
```

### MongoDB Backup

```bash
# Full backup
mongodump --uri "mongodb://niskuldeep:niskuldeep@localhost:27017" --out ./backup

# Restore
mongorestore --uri "mongodb://niskuldeep:niskuldeep@localhost:27017" ./backup
```

---

## Summary Table

| Component | Type | Database | Tables/Collections | Relationships |
|-----------|------|----------|------------------|---------------|
| **Product Service** | Relational | PostgreSQL | category, product | 1:N |
| **Order Service** | Relational | PostgreSQL | customer_order, customer_line | 1:N |
| **Payment Service** | Relational | PostgreSQL | payment | N:1 (to order) |
| **Customer Service** | Document | MongoDB | customer | Embedded address |
| **Notification Service** | Document | MongoDB | notification | Embedded confirmations |
| **Cache Layer** | Key-Value | Redis | (in-memory) | Cache keys |

---

## Conclusion

The database schema is designed with:
- ✅ **Polyglot Persistence** (PostgreSQL + MongoDB)
- ✅ **Proper Normalization** (PostgreSQL)
- ✅ **Denormalization** (MongoDB embedded documents)
- ✅ **Caching Strategy** (Redis for high-traffic products)
- ✅ **Audit Trails** (createdDate, lastModifiedDate)
- ✅ **Foreign Key Constraints** (referential integrity)
- ✅ **Unique Constraints** (data uniqueness)
- ✅ **Indexing** (performance optimization)

This schema supports millions of transactions while maintaining data consistency and query performance.

