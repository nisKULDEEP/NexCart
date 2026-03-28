# 🎯 Product Service Refactoring - Quick Reference Card

## 📍 Folder Navigation

```
├── controller/        → REST API endpoints & request handling
├── service/           → Business logic & transactions  
├── repository/        → Database queries & data access
├── mapper/            → Entity ↔ DTO conversion
├── models/            → JPA entities & domain objects
├── dto/               → Request/Response data models
├── exception/         → Custom exception classes
├── handler/           → Global exception handling
├── config/            → Spring configurations
└── utils/             → Helper & utility functions
```

---

## 🚀 Quick Commands

```bash
# Compile
./mvnw clean compile

# Run tests
./mvnw test

# Package
./mvnw clean package

# Run app
./mvnw spring-boot:run

# View API docs
http://localhost:8080/swagger-ui.html
```

---

## 💾 Adding New Features - Step by Step

### 1. Create DTO
**File:** `dto/YourFeatureRequest.java`
```java
public record YourFeatureRequest(
    @NotNull(message = "Field required")
    String field
) { }
```

### 2. Add Repository Method
**File:** `repository/ProductRepository.java`
```java
List<Product> findByYourCriteria(String param);
```

### 3. Add Service Method
**File:** `service/ProductService.java`
```java
@Service
public class ProductService {
    public ResponseData yourFeature(Request req) {
        // Business logic here
    }
}
```

### 4. Add Mapper Method
**File:** `mapper/ProductMapper.java`
```java
public ResponseData mapToResponse(Entity entity) {
    return new ResponseData(...);
}
```

### 5. Add Controller Endpoint
**File:** `controller/ProductController.java`
```java
@PostMapping("/your-endpoint")
public ResponseEntity<ResponseData> yourEndpoint(@RequestBody @Valid Request req) {
    return ResponseEntity.ok(service.yourFeature(req));
}
```

### 6. Add Exception Handler (if needed)
**File:** `handler/GlobalExceptionHandler.java`
```java
@ExceptionHandler(YourException.class)
public ResponseEntity<String> handle(YourException ex) {
    return ResponseEntity.status(BAD_REQUEST).body(ex.getMessage());
}
```

---

## 📦 Import Reference

### For Controllers
```java
import com.example.dto.*;
import com.example.service.*;
```

### For Services
```java
import com.example.models.*;
import com.example.repository.*;
import com.example.mapper.*;
import com.example.dto.*;
import com.example.exception.*;
```

### For Mappers
```java
import com.example.models.*;
import com.example.dto.*;
```

### For Exception Handlers
```java
import com.example.exception.*;
import com.example.handler.*;
```

---

## 🔍 Common Annotations

| Annotation | Package | Purpose |
|-----------|---------|---------|
| `@RestController` | spring.web | REST API |
| `@Service` | spring.stereotype | Business logic |
| `@Repository` | spring.stereotype | Data access |
| `@Entity` | jakarta.persistence | Database entity |
| `@Valid` | jakarta.validation | Input validation |
| `@NotNull` | jakarta.validation.constraints | Required field |
| `@Positive` | jakarta.validation.constraints | Positive number |
| `@Transactional` | spring.transaction | Transaction mgmt |
| `@Cacheable` | spring.cache.annotation | Caching |
| `@CacheEvict` | spring.cache.annotation | Cache clearing |

---

## 🧪 Testing Template

```java
// Unit Test Example
@SpringBootTest
class ProductServiceTest {
    
    @MockBean
    private ProductRepository repository;
    
    @InjectMocks
    private ProductService service;
    
    @Test
    void testCreateProduct() {
        // Arrange
        ProductRequest request = new ProductRequest(...);
        Product product = new Product(...);
        when(repository.save(any())).thenReturn(product);
        
        // Act
        Integer id = service.createProduct(request);
        
        // Assert
        assertEquals(product.getId(), id);
        verify(repository, times(1)).save(any());
    }
}
```

---

## 🚨 Exception Handling

```java
// In Service
throw new ProductPurchaseException("Custom error message");

// In Controller  
try {
    return ResponseEntity.ok(service.operation());
} catch (ProductPurchaseException e) {
    // Caught by GlobalExceptionHandler
    // Returns error response automatically
}
```

---

## 💾 Caching

```java
// Enable caching
@Cacheable(value = "products", key = "#id")
public ProductResponse findById(Integer id) { ... }

// Invalidate cache
@CacheEvict(value = {"products", "productsList"}, allEntries = true)
public void updateProduct(...) { ... }
```

---

## 📝 Validation

```java
// DTO with validation
public record ProductRequest(
    @NotNull(message = "Name required")
    String name,
    
    @Positive(message = "Price must be positive")
    BigDecimal price,
    
    @Positive(message = "Quantity must be positive")
    double quantity
) { }

// Use in controller
@PostMapping
public ResponseEntity<Integer> create(
    @RequestBody @Valid ProductRequest request
) { ... }
```

---

## 🔄 Request/Response Flow

```
1. HTTP Request (JSON)
        ↓
2. @RestController receives request
        ↓
3. @Valid validates DTO
        ↓
4. @Service processes business logic
        ↓
5. @Repository queries database
        ↓
6. @Mapper converts Entity → DTO
        ↓
7. HTTP Response (JSON)
        ↓
8. Exception → @RestControllerAdvice
```

---

## 📊 File Structure Summary

```
controllers/         → Accept HTTP requests
services/           → Implement business logic  
repositories/       → Access databases
mappers/            → Transform data
models/             → Define entities
dtos/               → Define API contracts
exceptions/         → Define error types
handlers/           → Handle errors
configs/            → Configure Spring
utils/              → Helper functions
```

---

## 🎯 Best Practices

✅ **DO:**
- Use DTOs for API contracts
- Use validation annotations
- Keep services transactional
- Cache frequently accessed data
- Use mappers for conversions
- Handle exceptions globally
- Keep controllers thin

❌ **DON'T:**
- Mix business logic in controllers
- Expose entities directly in APIs
- Use raw SQL queries
- Ignore exception handling
- Create god services
- Skip input validation
- Hardcode configuration

---

## 🔧 Troubleshooting

**Import errors?**
→ Check package paths match your module structure

**Compilation fails?**
→ Run `./mvnw clean compile` to clear cache

**API returns 404?**
→ Check `@RequestMapping` and endpoint path

**Cache not working?**
→ Verify Redis is running: `redis-cli ping`

**Database errors?**
→ Check `application.yml` database configuration

**Validation not working?**
→ Ensure `@Valid` is on DTO parameter

---

## 📚 Documentation Files

| File | Purpose | Read Time |
|------|---------|-----------|
| README_REFACTORING.md | Navigation & overview | 5 min |
| REFACTORING_COMPLETE.md | Complete structure | 10 min |
| MIGRATION_GUIDE.md | Developer guide | 15 min |
| ARCHITECTURE_OVERVIEW.md | Design details | 20 min |
| ARCHITECTURE_DIAGRAMS.md | Visual reference | 10 min |
| VERIFICATION_REPORT.md | Status report | 10 min |

---

## 🎊 You're Ready!

This quick reference card covers the essentials. For detailed information, refer to the full documentation files.

**Start:** README_REFACTORING.md  
**Learn:** MIGRATION_GUIDE.md  
**Explore:** ARCHITECTURE_DIAGRAMS.md

---

**Last Updated:** March 26, 2026  
**Version:** 1.0.0

