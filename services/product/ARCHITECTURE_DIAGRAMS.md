# Product Service - Architecture Visualization

## 🏗️ Layered Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                      CLIENT/EXTERNAL API                        │
│                   (REST API Consumers)                          │
└────────────────────────┬────────────────────────────────────────┘
                         │ HTTP Request/Response
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│                   PRESENTATION LAYER                            │
│                    (controller/)                                │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │ ProductController         CategoryController             │  │
│  │ ✓ POST /products         ✓ GET /categories              │  │
│  │ ✓ GET /products/{id}     ✓ POST /categories             │  │
│  │ ✓ GET /products          ✓ DELETE /categories/{id}      │  │
│  │ ✓ POST /products/purchase                               │  │
│  └──────────────────────────────────────────────────────────┘  │
└────────────────────────┬────────────────────────────────────────┘
                         │ Service calls
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│                   BUSINESS LOGIC LAYER                          │
│                     (service/)                                  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │ ProductService                                           │  │
│  │ ✓ createProduct()         ✓ Caching Logic              │  │
│  │ ✓ findById()              ✓ Transaction Management      │  │
│  │ ✓ findAll()               ✓ Business Rules             │  │
│  │ ✓ purchaseProducts()      ✓ Error Handling             │  │
│  └──────────────────────────────────────────────────────────┘  │
└────────────────────────┬────────────────────────────────────────┘
                         │ Repository & Mapper calls
                    ┌────┴────┐
                    ▼         ▼
    ┌──────────────────────┐  ┌──────────────────────┐
    │  DATA ACCESS LAYER   │  │ TRANSFORMATION LAYER │
    │   (repository/)      │  │    (mapper/)         │
    │ ┌────────────────┐   │  │ ┌────────────────┐   │
    │ │ProductRepository   │  │ │ProductMapper   │   │
    │ │✓ findById()    │   │  │ │✓ toProduct()  │   │
    │ │✓ findAll()     │   │  │ │✓ toResponse() │   │
    │ │✓ save()        │   │  │ │✓ toPurchase()│   │
    │ │✓ Custom query  │   │  │ │               │   │
    │ └────────────────┘   │  │ └────────────────┘   │
    └──────────────────────┘  └──────────────────────┘
                    │         │
                    └────┬────┘
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│                    DOMAIN MODEL LAYER                           │
│                      (models/)                                  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │ @Entity                                                  │  │
│  │ Product                     Category                     │  │
│  │ ├─ id                       ├─ id                        │  │
│  │ ├─ name                     ├─ name                      │  │
│  │ ├─ description              ├─ description              │  │
│  │ ├─ availableQuantity        └─ products (1-to-Many)     │  │
│  │ ├─ price                                                │  │
│  │ └─ category (Many-to-1)                                 │  │
│  └──────────────────────────────────────────────────────────┘  │
└────────────────────────┬────────────────────────────────────────┘
                         │ CRUD Operations
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│                      DATABASE LAYER                             │
│                      (MySQL/Maria)                              │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │ products table                categories table            │  │
│  │ ├─ id (PK)                   ├─ id (PK)                 │  │
│  │ ├─ name                       ├─ name                    │  │
│  │ ├─ description                ├─ description            │  │
│  │ ├─ available_quantity         └─ created_at             │  │
│  │ ├─ price                                                │  │
│  │ ├─ category_id (FK)                                     │  │
│  │ └─ timestamps                                           │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

---

## 🔄 Request Flow Diagram

```
User Request
    │
    ▼
┌──────────────────────┐
│  ProductController   │
│   @RestController    │
│   @RequestMapping    │
└─────────┬────────────┘
          │ 1. Receive Request
          │ 2. Validate Input (@Valid)
          ▼
┌──────────────────────┐
│  ProductService      │
│   @Service           │
│ @Transactional       │
└─────────┬────────────┘
          │ 3. Business Logic
          │ 4. Check Cache
          │ 5. Call Repository
          ▼
┌──────────────────────┐
│ProductRepository     │
│ extends JpaRepository│
└─────────┬────────────┘
          │ 6. Query Database
          ▼
┌──────────────────────┐
│   MySQL Database     │
│   SELECT/INSERT      │
└─────────┬────────────┘
          │ 7. Return Data
          ▼
┌──────────────────────┐
│ProductMapper         │
│ @Service             │
│ @Component           │
└─────────┬────────────┘
          │ 8. Convert Entity to DTO
          ▼
┌──────────────────────┐
│ ProductResponse      │
│ (DTO)                │
└─────────┬────────────┘
          │ 9. Return Response
          ▼
┌──────────────────────┐
│  ProductController   │
│  ResponseEntity      │
└─────────┬────────────┘
          │ 10. HTTP Response
          ▼
      Client
```

---

## 🎯 Component Interaction Diagram

```
                    ┌─────────────────┐
                    │ ProductController
                    └────────┬─────────┘
                             │
          ┌──────────────────┼──────────────────┐
          │                  │                  │
          ▼                  ▼                  ▼
    ┌──────────┐      ┌──────────┐      ┌──────────┐
    │ Validate │      │  Log     │      │ Cache    │
    │ Input    │      │ Request  │      │ Check    │
    └────┬─────┘      └────┬─────┘      └────┬─────┘
         │                 │                 │
         └─────────────────┼─────────────────┘
                           │
                           ▼
                    ┌─────────────────┐
                    │ProductService   │
                    └────────┬─────────┘
                             │
          ┌──────────────────┼──────────────────┐
          │                  │                  │
          ▼                  ▼                  ▼
    ┌──────────┐      ┌──────────┐      ┌──────────┐
    │ Apply    │      │ Call     │      │ Handle   │
    │ Business │      │ Repository
    │ Rules    │      │          │      │ Exceptions
    │          │      │          │      │          │
    └────┬─────┘      └────┬─────┘      └────┬─────┘
         │                 │                 │
         └─────────────────┼─────────────────┘
                           │
                           ▼
                    ┌─────────────────┐
                    │ProductMapper    │
                    └────────┬─────────┘
                             │
          ┌──────────────────┼──────────────────┐
          │                  │                  │
          ▼                  ▼                  ▼
   ┌────────────┐    ┌────────────┐    ┌────────────┐
   │Entity to   │    │Format Data │    │Set Headers │
   │DTO        │    │           │    │           │
   │Conversion  │    │           │    │           │
   └────┬───────┘    └────┬───────┘    └────┬───────┘
        │                 │                 │
        └─────────────────┼─────────────────┘
                          │
                          ▼
                   ┌──────────────────┐
                   │ProductResponse   │
                   │(JSON)            │
                   └──────────────────┘
```

---

## 💾 Data Model Relationship

```
┌─────────────────────────┐
│     CATEGORY            │
├─────────────────────────┤
│ PK: id                  │
│ name: String            │
│ description: String     │
│ timestamps: Datetime    │
└────────────┬────────────┘
             │ 1
             │
             │ has many
             │
             │ Many
             │
┌────────────▼────────────┐
│     PRODUCT             │
├─────────────────────────┤
│ PK: id                  │
│ name: String            │
│ description: String     │
│ availableQuantity: Double
│ price: BigDecimal       │
│ FK: category_id         │
│ timestamps: Datetime    │
└─────────────────────────┘
```

---

## 🔌 Integration Points

```
┌─────────────────────────────────────────────────────┐
│            External Systems/APIs                    │
└────────────┬──────────────────────────┬─────────────┘
             │                          │
      ┌──────▼────────────┐    ┌────────▼──────────┐
      │ Order Service     │    │ Payment Service   │
      │ (calls us)        │    │ (calls us)        │
      └──────┬────────────┘    └────────┬──────────┘
             │                          │
             │ GET /products            │ POST /products/purchase
             │ POST /products/purchase  │
             │                          │
             └──────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────┐
│    Product Service (this service)   │
├─────────────────────────────────────┤
│ ✓ HTTP Server (Spring Boot)         │
│ ✓ Cache Server (Redis)              │
│ ✓ Database Connection (MySQL)       │
│ ✓ Logging (SLF4J)                   │
└─────────────────────────────────────┘
                        │
        ┌───────────────┼───────────────┐
        │               │               │
        ▼               ▼               ▼
   ┌────────┐    ┌────────────┐   ┌────────┐
   │ MySQL  │    │   Redis    │   │ Logs   │
   │Database│    │   Cache    │   │ ELK    │
   └────────┘    └────────────┘   └────────┘
```

---

## 📊 Caching Strategy

```
Request for Product by ID
        │
        ▼
    ┌────────────┐
    │ Check     │
    │ Redis     │  @Cacheable(value = "products", key = "#id")
    └────┬───────┘
         │
    ┌────▼─────────────────┐
    │ Cache Hit?           │
    └─┬──────────────────┬─┘
      │                  │
   Yes│                  │No
      │                  │
      ▼                  ▼
   ┌─────────┐    ┌──────────────┐
   │ Return  │    │ Query DB     │
   │ Cached  │    │ Get Product  │
   │ Value   │    │              │
   └─────────┘    └──────┬───────┘
                         │
                         ▼
                  ┌─────────────┐
                  │ Cache Value │ @CacheEvict on update
                  │ in Redis    │
                  └──────┬──────┘
                         │
                         ▼
                    ┌─────────────┐
                    │   Return    │
                    │   Product   │
                    └─────────────┘
```

---

## 🔐 Exception Handling Flow

```
Exception Occurs
        │
        ▼
┌──────────────────────────┐
│ Catch in Service Layer   │
└──┬───────────────────────┘
   │ 1. Log Error
   │ 2. Throw Custom Exception
   ▼
┌──────────────────────────┐
│ ProductPurchaseException │
│ EntityNotFoundException   │
│ MethodArgumentNotValid... │
└──┬───────────────────────┘
   │ 3. Bubble Up
   ▼
┌──────────────────────────────┐
│ GlobalExceptionHandler       │
│ @RestControllerAdvice        │
└──┬───────────────────────────┘
   │ 4. Handle & Format
   ▼
┌──────────────────────────────┐
│ ErrorResponse (JSON)         │
│ HTTP Status Code             │
│ Error Message                │
└──┬───────────────────────────┘
   │ 5. Send to Client
   ▼
Client (with proper error info)
```

---

## 📈 Performance Characteristics

```
Operation                    Time      Cache    DB Query
─────────────────────────────────────────────────────────
GET /products/{id}           <5ms      ✓        Skip if cached
GET /products                <50ms     ✓        Skip if cached
POST /products               ~100ms    ✗        INSERT
POST /products/purchase      ~200ms    ✗        Multiple INSERTs
                                                (Transaction)
```

---

This refactored architecture provides excellent separation of concerns, making the code maintainable, testable, and scalable!

