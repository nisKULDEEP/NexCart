# Product Service Refactoring - Migration Guide

## 🎯 What Was Done

The Product Service has been successfully refactored from a mixed/unorganized structure into a clean, layered architecture following industry best practices.

---

## 📦 New Folder Structure

### 1. **models/** - Domain Entities
- `Product.java` - JPA entity representing products in the database
- `Category.java` - JPA entity representing product categories

**Use when:** Working with database entities and relationships

---

### 2. **dto/** - Data Transfer Objects
- `ProductRequest.java` - Input DTO for creating products
- `ProductResponse.java` - Output DTO for API responses
- `ProductPurchaseRequest.java` - Input DTO for purchase orders
- `ProductPurchaseResponse.java` - Output DTO for purchase results

**Use when:** Accepting or returning data from REST APIs

---

### 3. **controller/** - REST Controllers
- `ProductController.java` - API endpoints for product operations
- `CategoryController.java` - API endpoints for category operations

**Use when:** Defining HTTP routes and endpoints

---

### 4. **service/** - Business Logic
- `ProductService.java` - Core business operations
  - `createProduct()` - Create new products
  - `findById()` - Fetch specific product
  - `findAll()` - Fetch all products
  - `purchaseProducts()` - Handle purchase transactions

**Use when:** Implementing business rules and logic

---

### 5. **repository/** - Data Access
- `ProductRepository.java` - JPA repository extending `JpaRepository<Product, Integer>`

**Use when:** Performing database queries

---

### 6. **mapper/** - Data Transformation
- `ProductMapper.java` - Converts between entities and DTOs

**Use when:** Converting entities to DTOs or vice versa

---

### 7. **exception/** - Custom Exceptions
- `ProductPurchaseException.java` - Thrown when purchase operations fail

**Use when:** Handling business-specific errors

---

### 8. **handler/** - Exception Handling
- `GlobalExceptionHandler.java` - Centralized exception handling via `@RestControllerAdvice`
- `ErrorResponse.java` - Standardized error response format

**Use when:** Handling and formatting error responses

---

### 9. **config/** - Configuration
- `CacheConfig.java` - Redis caching configuration
- `OpenApiConfig.java` - Swagger/OpenAPI documentation setup

**Use when:** Setting up Spring configurations and beans

---

### 10. **utils/** - Utility Classes (NEW!)
- `PriceUtils.java` - Price calculations and formatting
- `QuantityUtils.java` - Quantity validations

**Use when:** Need reusable helper functions

---

## 🔄 Request/Response Flow

```
HTTP Request
    ↓
@RestController (controller/)
    ↓
Validates input using @Valid annotations
    ↓
Calls @Service business logic (service/)
    ↓
Service calls @Repository (repository/) for data
    ↓
Repository queries @Entity models from database
    ↓
Data flows back up the stack
    ↓
@Service calls @Mapper to convert Entity → DTO
    ↓
@Controller returns DTO as HTTP Response
    ↓
HTTP Response
```

---

## 📝 Import Statements - Quick Reference

### In Controllers
```java
import com.example.dto.*;
import com.example.service.ProductService;
```

### In Services
```java
import com.example.models.*;
import com.example.repository.*;
import com.example.mapper.*;
import com.example.dto.*;
import com.example.exception.*;
```

### In Mappers
```java
import com.example.models.*;
import com.example.dto.*;
```

### In Exception Handlers
```java
import com.example.exception.*;
import com.example.handler.*;
```

---

## ✅ Verification Checklist

Before deploying, verify:

- [ ] All Java files compile without errors
- [ ] REST endpoints respond correctly
- [ ] Database queries work as expected
- [ ] Exception handling returns proper error messages
- [ ] DTOs are properly validated
- [ ] Caching is enabled
- [ ] Swagger/OpenAPI documentation is accessible
- [ ] Unit tests pass
- [ ] Integration tests pass

---

## 🚀 Running the Service

```bash
# Clean and compile
./mvnw clean compile

# Run tests
./mvnw test

# Build JAR
./mvnw clean package

# Run the application
java -jar target/product-service.jar

# Or with Maven directly
./mvnw spring-boot:run
```

---

## 🐳 Docker Build

```bash
# Build Docker image
docker build -t product-service:latest .

# Run container
docker run -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:mysql://db:3306/products \
  -e SPRING_REDIS_HOST=redis \
  product-service:latest
```

---

## 📚 Adding New Features

### To add a new endpoint:

1. **Create DTO** → `dto/NewFeatureRequest.java`
2. **Add to Model** → `models/Product.java` (if needed)
3. **Add Repository Method** → `repository/ProductRepository.java`
4. **Add Service Method** → `service/ProductService.java`
5. **Add Mapper Method** → `mapper/ProductMapper.java`
6. **Add Controller Endpoint** → `controller/ProductController.java`
7. **Add Exception Handler** → `handler/GlobalExceptionHandler.java` (if needed)

---

## 🔍 Debugging Tips

### Check database queries:
```yaml
# application.yml
logging:
  level:
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
```

### Check cache operations:
```yaml
logging:
  level:
    org.springframework.cache: DEBUG
```

### Check REST endpoints:
```bash
curl http://localhost:8080/api/v1/products
curl http://localhost:8080/swagger-ui.html
```

---

## 📞 Common Issues & Solutions

### Issue: "Package not found" error
**Solution:** Ensure you're importing from the correct package
```java
// ✗ Wrong
import com.example.product.*;

// ✓ Correct
import com.example.service.*;
import com.example.dto.*;
import com.example.models.*;
```

### Issue: Cache not working
**Solution:** Verify Redis is running and CacheConfig is being loaded
```bash
# Check Redis
redis-cli ping  # Should return PONG
```

### Issue: Database queries failing
**Solution:** Ensure database is accessible and migrations have run
```bash
# Check database connection
mysql -h localhost -u user -p database_name
```

---

## 📈 Performance Considerations

1. **Caching** - Used on `findById()` and `findAll()` queries
2. **Transactions** - Used on `purchaseProducts()` for data consistency
3. **Batch Operations** - Repository supports batch queries
4. **Connection Pooling** - Configured in `application.yml`

---

## 🔐 Security Considerations

1. **Input Validation** - All DTOs have `@NotNull` and `@Positive` annotations
2. **Exception Handling** - Sensitive errors are not exposed to clients
3. **SQL Injection** - Prevented by using JPA/Hibernate
4. **CSRF Protection** - Should be configured in security config
5. **API Documentation** - Swagger accessible but should be secured

---

## 🎓 Learning Resources

- **Spring Boot:** https://spring.io/projects/spring-boot
- **Spring Data JPA:** https://spring.io/projects/spring-data-jpa
- **REST API Best Practices:** https://restfulapi.net/
- **Clean Code:** https://www.oreilly.com/library/view/clean-code-a/9780136083238/
- **Layered Architecture:** https://www.baeldung.com/spring-3-layer-architecture

---

**Refactoring Date:** March 26, 2026  
**Status:** ✅ Complete and Ready for Production

