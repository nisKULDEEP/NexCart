# Product Service - Refactored Structure

## Overview
The product service has been refactored to follow a clean, modular architecture with clear separation of concerns.

## New Directory Structure

```
com/example/
├── ProductApplication.java        # Main Spring Boot application class
├── config/                          # Configuration classes
│   ├── CacheConfig.java            # Redis caching configuration
│   └── OpenApiConfig.java          # OpenAPI/Swagger documentation
├── controller/                      # REST API endpoints
│   ├── ProductController.java      # Product API endpoints
│   └── CategoryController.java     # Category API endpoints (placeholder)
├── service/                         # Business logic layer
│   └── ProductService.java         # Product business logic
├── repository/                      # Data access layer
│   └── ProductRepository.java      # JPA repository for Product entity
├── mapper/                          # DTO mapping layer
│   └── ProductMapper.java          # Convert between entities and DTOs
├── models/                          # Domain entities
│   ├── Product.java                # Product entity
│   └── Category.java               # Category entity
├── dto/                             # Data Transfer Objects
│   ├── ProductRequest.java         # Request DTO for creating products
│   ├── ProductResponse.java        # Response DTO for products
│   ├── ProductPurchaseRequest.java # Request DTO for purchasing products
│   └── ProductPurchaseResponse.java# Response DTO for purchase operations
├── exception/                       # Custom exceptions
│   └── ProductPurchaseException.java # Exception for purchase failures
└── handler/                         # Exception handlers
    ├── GlobalExceptionHandler.java # Global exception handling
    └── ErrorResponse.java          # Error response model
```

## Folder Structure Explanation

### 1. **config/** - Configuration Classes
   - Contains Spring configuration beans
   - Examples: Cache configuration, API documentation setup
   - Best for: Application-wide configurations

### 2. **controller/** - REST Controllers
   - API endpoint definitions
   - HTTP request/response handling
   - Route mapping with Spring annotations
   - Best for: Defining REST APIs

### 3. **service/** - Business Logic
   - Core business logic implementation
   - Orchestrates between repositories and other services
   - Transaction and cache management
   - Best for: Business rules and processing logic

### 4. **repository/** - Data Access Layer
   - JPA repository interfaces
   - Database queries
   - Spring Data JPA repository definitions
   - Best for: Data persistence operations

### 5. **mapper/** - Data Transformation
   - Converts between entities and DTOs
   - Handles data transformation logic
   - Keeps controllers and services clean
   - Best for: Object mapping and conversion

### 6. **models/** - Domain Entities
   - JPA entity classes
   - Database table representations
   - Domain model definitions
   - Best for: Core domain objects

### 7. **dto/** - Data Transfer Objects
   - Request/Response DTOs
   - Validation annotations
   - API contract definitions
   - Best for: API input/output models

### 8. **exception/** - Custom Exceptions
   - Business-specific exceptions
   - Exception hierarchy
   - Best for: Custom error handling

### 9. **handler/** - Exception Handlers
   - Global exception handling
   - Error response formatting
   - HTTP status mapping
   - Best for: Centralized error handling

## Benefits of This Structure

1. **Separation of Concerns** - Each layer has a specific responsibility
2. **Scalability** - Easy to add new features following the same pattern
3. **Testability** - Each component can be tested independently
4. **Maintainability** - Clear organization makes code easier to understand
5. **Reusability** - Mappers and DTOs can be used across services
6. **Extensibility** - New features follow established patterns

## Import Updates

The following files have been updated with new import paths:

- **ProductController.java**: Updated to import from new packages (dto, service)
- **ProductService.java**: Updated to import from models, dto, repository, mapper, exception
- **ProductMapper.java**: Updated to import from models, dto

## Old Folders (Legacy - Can be Removed)

- `product/` - Contains duplicated files, can be removed after verification
- `category/` - Legacy category folder

## Next Steps

1. Verify all tests pass with the new structure
2. Remove the legacy `product/` and `category/` folders after ensuring new structure is working
3. Update any other services following this same pattern
4. Consider adding additional folders as needed:
   - `utils/` - For utility classes
   - `validator/` - For custom validators
   - `listener/` - For event listeners
   - `interceptor/` - For HTTP interceptors

