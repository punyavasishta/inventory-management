# Cosmetic Store Database Management System - Project Submission

**Project Title:** Cosmetic Store Database Management System  
**Student Name:** [Your Name]  
**Student ID:** [Your ID]  
**Date:** October 22, 2026  
**Course:** Database Management Systems / Java Programming  

---

## 1. Project Overview

### 1.1 Objective
Develop a comprehensive inventory management system for a cosmetic store that demonstrates:
- Database design and normalization
- Java application development with JDBC
- RESTful API development
- Modern web interface design
- Real-time data synchronization

### 1.2 Scope
The system manages cosmetic product inventory with features for:
- Product catalog management (CRUD operations)
- Product type management with dynamic attributes
- Stock transaction processing (buy/sell operations)
- Low stock alerts and reporting
- Both console-based and web-based user interfaces

---

## 2. System Architecture

### 2.1 Technology Stack

| Component | Technology | Version |
|-----------|-----------|---------|
| Programming Language | Java | 11+ |
| Database | MySQL | 8.0+ |
| JDBC Driver | MySQL Connector/J | 26.7.0 |
| Web Server | Java HttpServer | Built-in |
| Frontend | HTML5, CSS3, JavaScript | ES6+ |

### 2.2 Database Design

#### Schema Overview
```
cosmetic_store
├── product_types (type_id, type_name, attribute_label, gst_rate)
├── categories (category_id, category_name)
├── products (product_id, name, brand, base_price, stock_quantity, attribute_value, category_id, type_id)
└── stock_transactions (txn_id, product_id, quantity_change, txn_time)
```

#### Key Features
- **Foreign Key Constraints**: Referential integrity between products, types, and categories
- **Views**: `product_details` (calculated final price), `low_stock_report` (stock < 20)
- **Stored Procedure**: `process_transaction` for atomic stock updates
- **Indexes**: Primary keys on all tables for performance

#### Sample Data
- 8 product types with varying GST rates (12-18%)
- 18 categories across cosmetic categories
- 50 products from major brands (Lakme, Maybelline, Garnier, etc.)
- 20 sample stock transactions

### 2.3 Application Architecture

#### Component 1: Console Application (InteractiveCosmeticStoreDB.java)
**Purpose:** Interactive command-line interface for inventory management

**Key Classes:**
- `Database`: Connection management with environment variable support
- `ProductType`: Encapsulates type metadata (name, attribute label, GST rate)
- `ProductTypeManager`: CRUD operations for product types
- `Product` (Abstract): Base class with common product properties
- `CosmeticProduct`: Concrete implementation with type-specific attributes
- `Inventory`: Product management with database persistence
- Custom Exceptions: `InsufficientStockException`, `NotFoundException`, `InvalidInputException`, `DatabaseException`

**Features:**
- Menu-driven interface (7 options)
- Input validation and error handling
- Transaction processing with stock validation
- Low stock reporting with custom thresholds

#### Component 2: Web Server (StoreServerDB.java)
**Purpose:** HTTP server providing REST API and web interface

**Architecture:**
- Built-in Java HttpServer (no external dependencies)
- CORS support for cross-origin requests
- RESTful API endpoints:
  - `GET/POST /api/products` - Retrieve/add products
  - `POST /api/products/delete` - Delete product
  - `GET/POST /api/types` - Retrieve/create types
  - `POST /api/types/edit` - Update type
  - `POST /api/types/delete` - Delete type
  - `POST /api/transaction` - Process stock transaction
  - `GET /api/lowstock` - Low stock report
- Automatic browser launch
- LAN access for multi-device support

#### Component 3: Web Interface (index.html)
**Purpose:** Modern, responsive frontend for inventory management

**Features:**
- **Dashboard**: Live statistics with auto-refresh (8s interval)
  - Total products count
  - Product types count
  - Low stock items (below 20)
  - Total inventory value (at final prices)
- **Products Page**: 
  - Grid view with product cards
  - Filtering by type, category, brand, stock level
  - Search functionality (Ctrl/Cmd+K)
  - Real-time stock adjustment modal
  - Delete with confirmation
- **Low Stock Page**:
  - Configurable threshold
  - Card and table views
  - One-click restock
- **Types Page**:
  - Table view of all types
  - Edit modal for GST and attribute label
  - Delete with constraint checking
- **Add Product/Type Pages**: Form-based data entry
- **Live Activity Feed**: Real-time event logging
- **Mobile Responsive**: Adaptive layout for different screen sizes

---

## 3. Implementation Details

### 3.1 Database Operations

#### Connection Management
```java
// Environment variable support for credentials
private static String env(String key, String def) {
    String v = System.getenv(key);
    return (v == null) ? def : v;
}
```

#### Transaction Processing
Uses MySQL stored procedure for atomic operations:
```sql
CREATE PROCEDURE process_transaction(IN p_id INT, IN qty INT)
BEGIN
    DECLARE current_stock INT;
    SELECT stock_quantity INTO current_stock FROM products WHERE product_id = p_id FOR UPDATE;
    
    IF current_stock IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Product ID not found.';
    ELSEIF qty < 0 AND ABS(qty) > current_stock THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Insufficient stock.';
    ELSE
        UPDATE products SET stock_quantity = stock_quantity + qty WHERE product_id = p_id;
        INSERT INTO stock_transactions (product_id, quantity_change) VALUES (p_id, qty);
    END IF;
END
```

### 3.2 Object-Oriented Design

#### Abstraction & Inheritance
- Abstract `Product` class defines common properties
- `CosmeticProduct` extends with type-specific behavior
- `Transaction` interface for polymorphic transaction processing

#### Encapsulation
- Private fields with public getters
- Validation in constructors
- Database operations encapsulated in manager classes

#### Exception Handling
- Custom exceptions for business logic errors
- SQLException handling with user-friendly messages
- Input validation before database operations

### 3.3 Web Interface Design

#### REST API Design
- JSON request/response format
- URL-encoded form data for POST requests
- CORS headers for cross-origin support
- Consistent error responses: `{"error": "message"}`

#### Frontend Architecture
- Vanilla JavaScript (no frameworks)
- Async/await for API calls
- Event-driven UI updates
- Local state management with server sync
- CSS variables for theming

#### Performance Optimizations
- Debounced search input
- Threshhold change debouncing
- Efficient DOM updates (bulk rendering)
- Local filtering before API calls when possible

---

## 4. Testing & Validation

### 4.1 Database Testing
- Sample data insertion verified (50 products)
- Foreign key constraints tested
- Stored procedure tested with various scenarios
- Views validated for correct calculations

### 4.2 Console Application Testing
- All menu options tested
- Input validation verified
- Exception handling tested
- Transaction processing with edge cases (insufficient stock, invalid ID)

### 4.3 Web Application Testing
- All API endpoints tested
- UI responsiveness verified
- Real-time updates tested
- Multi-device access tested (LAN)
- Browser compatibility tested

---

## 5. Features Demonstration

### 5.1 Console Application Workflow
1. **Manage Product Types**: Create "Organic Skincare" with GST 12%, attribute "Skin Concern"
2. **Add Product**: Add new organic face cream with stock 25, price ₹299
3. **View Inventory**: Display all 51 products with calculated final prices
4. **Process Transaction**: Sell 5 units of the new product
5. **Low Stock Report**: Generate report for items below 20 units
6. **Delete Product**: Remove product with confirmation

### 5.2 Web Application Workflow
1. **Dashboard**: View live statistics (51 products, 9 types, 8 low stock items)
2. **Filter Products**: Filter by type "Skincare", brand "Lakme"
3. **Search**: Use Ctrl+K to search for "serum"
4. **Stock Adjustment**: Open modal, restock 10 units
5. **Add Type**: Create "Hair Accessories" with GST 18%
6. **Low Stock**: Set threshold to 15, view updated report
7. **Live Feed**: Observe real-time activity log

---

## 6. Code Quality

### 6.1 Best Practices Implemented
- **Naming Conventions**: CamelCase for Java, snake_case for database
- **Code Organization**: Logical class separation
- **Comments**: Minimal, self-documenting code
- **Error Handling**: Comprehensive exception handling
- **Security**: Prepared statements to prevent SQL injection
- **Validation**: Input validation at multiple levels

### 6.2 Code Statistics
- **InteractiveCosmeticStoreDB.java**: 511 lines
- **StoreServerDB.java**: 368 lines
- **index.html**: 1,617 lines (including CSS and JavaScript)
- **cosmetic_store.sql**: 192 lines
- **Total**: ~2,688 lines of code

---

## 7. Challenges & Solutions

### 7.1 Challenge: Dynamic Product Attributes
**Problem:** Different product types need different attributes (e.g., Shade for makeup, Skin Type for skincare)

**Solution:** 
- Product types table stores attribute_label
- Products table stores attribute_value
- Runtime display based on type metadata

### 7.2 Challenge: Real-time Web Updates
**Problem:** Multiple users need to see consistent data

**Solution:**
- Auto-refresh every 8 seconds
- Live activity feed for events
- Optimistic UI updates with server sync
- Silent background refresh

### 7.3 Challenge: Transaction Atomicity
**Problem:** Prevent race conditions in stock updates

**Solution:**
- MySQL stored procedure with SELECT FOR UPDATE
- Database-level validation
- Transaction history logging

---

## 8. Future Enhancements

### 8.1 Potential Improvements
- User authentication and authorization
- Multi-store support
- Advanced analytics and reporting
- Export functionality (CSV, PDF)
- Barcode/QR code scanning
- Supplier management
- Purchase order system
- Mobile app (React Native)

### 8.2 Technical Improvements
- Migration to Spring Boot framework
- WebSocket for real-time updates
- Redis caching for performance
- Docker containerization
- Automated testing suite
- CI/CD pipeline

---

## 9. Conclusion

This project successfully demonstrates:
- **Database Design**: Normalized schema with relationships, views, and stored procedures
- **Java Development**: OOP principles, exception handling, JDBC integration
- **Web Development**: REST API, modern UI, real-time updates
- **Full-Stack Integration**: Seamless database-to-frontend connectivity

The system provides a complete inventory management solution with both traditional console interface and modern web interface, showcasing versatility in application design and implementation.

---

## 10. References

- MySQL Documentation: https://dev.mysql.com/doc/
- Java JDBC Tutorial: https://docs.oracle.com/javase/tutorial/jdbc/
- Java HttpServer API: https://docs.oracle.com/en/java/javase/11/docs/api/com/sun/net/httpserver/package-summary.html
- MySQL Connector/J: https://dev.mysql.com/doc/connector-j/

---

## Appendix: File Structure

```
cosmetic_store_project/
├── cosmetic_store.sql              # Database schema and sample data
├── InteractiveCosmeticStoreDB.java # Console application
├── StoreServerDB.java              # Web server
├── index.html                      # Web interface
├── mysql-connector-j-26.7.0.jar    # JDBC driver
├── README.md                       # Setup instructions
└── SUBMISSION.md                   # This document
```

---

**End of Submission Document**
