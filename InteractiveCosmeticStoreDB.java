import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Scanner;

// CUSTOM EXCEPTIONS
class InsufficientStockException extends Exception { public InsufficientStockException(String msg) { super(msg); } }
class NotFoundException extends Exception { public NotFoundException(String msg) { super(msg); } }
class InvalidInputException extends Exception { public InvalidInputException(String msg) { super(msg); } }
class DatabaseException extends Exception { public DatabaseException(String msg) { super(msg); } }

// DATABASE CONNECTION (MySQL)
class Database {
    private static final String URL  = env("DB_URL",  "jdbc:mysql://localhost:3306/cosmetic_store");
    private static final String USER = env("DB_USER", "root");
    private static final String PASS = env("DB_PASS", "your_password");

    private static String env(String key, String def) {
        String v = System.getenv(key);
        return (v == null) ? def : v;
    }

    public static Connection connect() throws DatabaseException {
        try {
            return DriverManager.getConnection(URL, USER, PASS);
        } catch (SQLException e) {
            throw new DatabaseException("Could not connect to MySQL: " + e.getMessage());
        }
    }
}

// DYNAMIC PRODUCT TYPE TEMPLATE
class ProductType {
    private String typeName;
    private String customAttributeLabel;
    private double gstRate;

    public ProductType(String typeName, String customAttributeLabel, double gstRatePercent) {
        this.typeName = typeName;
        this.customAttributeLabel = customAttributeLabel;
        this.gstRate = gstRatePercent / 100.0;
    }

    public String getTypeName() { return typeName; }
    public String getCustomAttributeLabel() { return customAttributeLabel; }
    public double getGstRate() { return gstRate; }
    public double getGstPercent() { return Math.round(gstRate * 10000) / 100.0; }
}

// PRODUCT TYPE MANAGER (reads/writes the product_types table)
class ProductTypeManager {

    public void addType(String name, String attrLabel, double gst) throws InvalidInputException, DatabaseException {
        if (name.isEmpty() || attrLabel.isEmpty()) throw new InvalidInputException("Name and attribute label are required.");
        if (gst < 0) throw new InvalidInputException("GST cannot be negative.");
        String sql = "INSERT INTO product_types (type_name, attribute_label, gst_rate) VALUES (?,?,?)";
        try (Connection c = Database.connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, attrLabel);
            ps.setDouble(3, gst);
            ps.executeUpdate();
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new InvalidInputException("Type already exists.");
        } catch (SQLException e) {
            throw new DatabaseException("Database error: " + e.getMessage());
        }
    }

    public void editType(String name, String newLabel, double newGst) throws NotFoundException, InvalidInputException, DatabaseException {
        if (newLabel.isEmpty()) throw new InvalidInputException("Attribute label is required.");
        if (newGst < 0) throw new InvalidInputException("GST cannot be negative.");
        String sql = "UPDATE product_types SET attribute_label=?, gst_rate=? WHERE type_name=?";
        try (Connection c = Database.connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, newLabel);
            ps.setDouble(2, newGst);
            ps.setString(3, name);
            if (ps.executeUpdate() == 0) throw new NotFoundException("Type '" + name + "' not found.");
        } catch (SQLException e) {
            throw new DatabaseException("Database error: " + e.getMessage());
        }
        System.out.println("Product Type '" + name + "' updated successfully.");
    }

    public void deleteType(String name) throws NotFoundException, InvalidInputException, DatabaseException {
        String sql = "DELETE FROM product_types WHERE type_name=?";
        try (Connection c = Database.connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, name);
            if (ps.executeUpdate() == 0) throw new NotFoundException("Product type not found.");
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new InvalidInputException("Cannot delete: products still use this type.");
        } catch (SQLException e) {
            throw new DatabaseException("Database error: " + e.getMessage());
        }
        System.out.println("Product Type '" + name + "' deleted successfully.");
    }

    public ProductType findType(String name) throws NotFoundException, DatabaseException {
        String sql = "SELECT type_name, attribute_label, gst_rate FROM product_types WHERE type_name=?";
        try (Connection c = Database.connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return new ProductType(rs.getString(1), rs.getString(2), rs.getDouble(3));
        } catch (SQLException e) {
            throw new DatabaseException("Database error: " + e.getMessage());
        }
        throw new NotFoundException("Type '" + name + "' not found.");
    }

    public List<ProductType> getAllTypes() throws DatabaseException {
        List<ProductType> list = new ArrayList<>();
        String sql = "SELECT type_name, attribute_label, gst_rate FROM product_types ORDER BY type_id";
        try (Connection c = Database.connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(new ProductType(rs.getString(1), rs.getString(2), rs.getDouble(3)));
        } catch (SQLException e) {
            throw new DatabaseException("Database error: " + e.getMessage());
        }
        return list;
    }

    public void displayTypes() throws DatabaseException {
        List<ProductType> types = getAllTypes();
        if (types.isEmpty()) {
            System.out.println("No product types available.");
            return;
        }
        for (int i = 0; i < types.size(); i++) {
            ProductType t = types.get(i);
            System.out.println((i + 1) + ". " + t.getTypeName() + " (Attribute: " + t.getCustomAttributeLabel() + ", GST: " + t.getGstPercent() + "%)");
        }
    }

    public ProductType getTypeByIndex(int index) throws DatabaseException {
        List<ProductType> types = getAllTypes();
        if (index >= 0 && index < types.size()) return types.get(index);
        return null;
    }
}

// --- CATEGORY CLASS ---
class Category {
    private String name;
    public Category(String name) { this.name = name; }
    public String getName() { return name; }
}

// --- INTERFACES & ABSTRACT PRODUCT ---
interface Transaction {
    void processTransaction(int quantity) throws InsufficientStockException, DatabaseException;
}

abstract class Product {
    private int id;
    private String name;
    private String brand;
    private double basePrice;
    protected int stockQuantity;
    private Category category;

    public Product(int id, String name, String brand, double basePrice, int initialStock, Category category) throws InvalidInputException {
        if (basePrice < 0 || initialStock < 0) throw new InvalidInputException("Price and Stock cannot be negative.");
        this.id = id;
        this.name = name;
        this.brand = brand;
        this.basePrice = basePrice;
        this.stockQuantity = initialStock;
        this.category = category;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getBrand() { return brand; }
    public double getBasePrice() { return basePrice; }
    public int getStockQuantity() { return stockQuantity; }
    public Category getCategory() { return category; }

    public abstract double calculateFinalPrice();
    public void displayProductInfo() {
        System.out.println("ID: " + id + " | Category: " + category.getName() + " | " + brand + " " + name + " | Stock: " + stockQuantity + " | Base Price: ₹" + basePrice);
    }
}

// --- UNIVERSAL COSMETIC PRODUCT ---
class CosmeticProduct extends Product implements Transaction {
    private ProductType productType;
    private String customAttributeValue;

    public CosmeticProduct(int id, String name, String brand, double basePrice, int initialStock, Category category, ProductType type, String attrValue) throws InvalidInputException {
        super(id, name, brand, basePrice, initialStock, category);
        this.productType = type;
        this.customAttributeValue = attrValue;
    }

    public ProductType getProductType() { return productType; }
    public String getCustomAttributeValue() { return customAttributeValue; }

    @Override public double calculateFinalPrice() {
        return Math.round((getBasePrice() + (getBasePrice() * productType.getGstRate())) * 100) / 100.0;
    }

    @Override public void displayProductInfo() {
        super.displayProductInfo();
        System.out.println("   -> [" + productType.getTypeName() + "] " + productType.getCustomAttributeLabel() + ": " + customAttributeValue + " | Final Price: ₹" + calculateFinalPrice());
    }

    // Uses the MySQL stored procedure process_transaction(product_id, qty)
    @Override public void processTransaction(int quantity) throws InsufficientStockException, DatabaseException {
        if (quantity < 0 && Math.abs(quantity) > this.stockQuantity) {
            throw new InsufficientStockException("Cannot sell " + Math.abs(quantity) + ". Only " + this.stockQuantity + " in stock.");
        }
        try (Connection c = Database.connect()) {
            try (CallableStatement cs = c.prepareCall("{CALL process_transaction(?, ?)}")) {
                cs.setInt(1, getId());
                cs.setInt(2, quantity);
                cs.execute();
            }
            try (PreparedStatement ps = c.prepareStatement("SELECT stock_quantity FROM products WHERE product_id=?")) {
                ps.setInt(1, getId());
                ResultSet rs = ps.executeQuery();
                if (rs.next()) this.stockQuantity = rs.getInt(1);
            }
        } catch (SQLException e) {
            if ("45000".equals(e.getSQLState())) throw new InsufficientStockException(e.getMessage());
            throw new DatabaseException("Database error: " + e.getMessage());
        }
        System.out.println("Transaction applied. New Stock for " + getName() + ": " + this.stockQuantity);
    }
}

// --- INVENTORY MANAGEMENT (reads/writes products, categories) ---
class Inventory {
    private static final String SELECT_PRODUCTS =
        "SELECT p.product_id, p.name, p.brand, p.base_price, p.stock_quantity, p.attribute_value, "
      + "c.category_name, t.type_name, t.attribute_label, t.gst_rate "
      + "FROM products p "
      + "JOIN categories c ON c.category_id = p.category_id "
      + "JOIN product_types t ON t.type_id = p.type_id ";

    private CosmeticProduct mapRow(ResultSet rs) throws SQLException {
        ProductType type = new ProductType(rs.getString("type_name"), rs.getString("attribute_label"), rs.getDouble("gst_rate"));
        try {
            return new CosmeticProduct(rs.getInt("product_id"), rs.getString("name"), rs.getString("brand"),
                    rs.getDouble("base_price"), rs.getInt("stock_quantity"),
                    new Category(rs.getString("category_name")), type, rs.getString("attribute_value"));
        } catch (InvalidInputException e) {
            throw new SQLException(e.getMessage());
        }
    }

    private List<CosmeticProduct> query(String suffix, Object... args) throws DatabaseException {
        List<CosmeticProduct> list = new ArrayList<>();
        try (Connection c = Database.connect(); PreparedStatement ps = c.prepareStatement(SELECT_PRODUCTS + suffix)) {
            for (int i = 0; i < args.length; i++) ps.setObject(i + 1, args[i]);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            throw new DatabaseException("Database error: " + e.getMessage());
        }
        return list;
    }

    private int lookupId(Connection c, String sql, String value) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, value);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : -1;
        }
    }

    public void addProduct(CosmeticProduct p) throws InvalidInputException, NotFoundException, DatabaseException {
        try (Connection c = Database.connect()) {
            int typeId = lookupId(c, "SELECT type_id FROM product_types WHERE type_name=?", p.getProductType().getTypeName());
            if (typeId < 0) throw new NotFoundException("Type '" + p.getProductType().getTypeName() + "' not found.");

            String catName = p.getCategory().getName();
            int catId = lookupId(c, "SELECT category_id FROM categories WHERE category_name=?", catName);
            if (catId < 0) {   // create the category if it is new
                try (PreparedStatement ps = c.prepareStatement("INSERT INTO categories (category_name) VALUES (?)", Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, catName);
                    ps.executeUpdate();
                    ResultSet keys = ps.getGeneratedKeys();
                    keys.next();
                    catId = keys.getInt(1);
                }
            }

            String sql = "INSERT INTO products (product_id, name, brand, base_price, stock_quantity, attribute_value, category_id, type_id) VALUES (?,?,?,?,?,?,?,?)";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setInt(1, p.getId());
                ps.setString(2, p.getName());
                ps.setString(3, p.getBrand());
                ps.setDouble(4, p.getBasePrice());
                ps.setInt(5, p.getStockQuantity());
                ps.setString(6, p.getCustomAttributeValue());
                ps.setInt(7, catId);
                ps.setInt(8, typeId);
                ps.executeUpdate();
            }
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new InvalidInputException("Product ID already exists.");
        } catch (SQLException e) {
            throw new DatabaseException("Database error: " + e.getMessage());
        }
        System.out.println("Product added: " + p.getName());
    }

    public void deleteProduct(int id) throws NotFoundException, DatabaseException {
        try (Connection c = Database.connect(); PreparedStatement ps = c.prepareStatement("DELETE FROM products WHERE product_id=?")) {
            ps.setInt(1, id);
            if (ps.executeUpdate() == 0) throw new NotFoundException("Product ID not found.");
        } catch (SQLException e) {
            throw new DatabaseException("Database error: " + e.getMessage());
        }
        System.out.println("Product " + id + " deleted successfully.");
    }

    public void showAllProducts() throws DatabaseException {
        List<CosmeticProduct> products = query("ORDER BY p.product_id");
        if (products.isEmpty()) {
            System.out.println("Inventory is empty.");
            return;
        }
        System.out.println("\n--- STORE INVENTORY ---");
        for (CosmeticProduct p : products) {
            p.displayProductInfo();
            System.out.println("-----------------------");
        }
        System.out.println("Total products: " + products.size());
    }

    public CosmeticProduct findProductById(int id) throws NotFoundException, DatabaseException {
        List<CosmeticProduct> found = query("WHERE p.product_id = ?", id);
        if (found.isEmpty()) throw new NotFoundException("Product ID not found.");
        return found.get(0);
    }

    public void generateLowStockReport(int threshold) throws DatabaseException {
        System.out.println("\n--- LOW STOCK REPORT (Below " + threshold + " units) ---");
        List<CosmeticProduct> low = query("WHERE p.stock_quantity < ? ORDER BY p.stock_quantity", threshold);
        if (low.isEmpty()) {
            System.out.println("All products are well stocked.");
            return;
        }
        for (CosmeticProduct p : low) {
            System.out.println("ALERT: ID " + p.getId() + " - " + p.getName() + " has only " + p.getStockQuantity() + " units left.");
        }
    }
}

// --- MAIN CLASS ---
public class InteractiveCosmeticStoreDB {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        Inventory storeInventory = new Inventory();
        ProductTypeManager typeManager = new ProductTypeManager();

        System.out.println("Connecting to MySQL database...");
        try (Connection c = Database.connect()) {
            System.out.println("Connected. Store data is loaded from the database.");
        } catch (DatabaseException | SQLException e) {
            System.out.println(e.getMessage());
            scanner.close();
            return;
        }

        boolean running = true;

        while (running) {
            try {
                System.out.println("\n--- MAIN MENU ---");
                System.out.println("1. Manage Product Types (Create/Edit/Delete)");
                System.out.println("2. Add New Product to Inventory");
                System.out.println("3. View All Inventory Products");
                System.out.println("4. Process Transaction (Buy/Sell)");
                System.out.println("5. View Low Stock Report");
                System.out.println("6. Delete Product");
                System.out.println("7. Exit");
                System.out.print("Select an option: ");

                int choice = scanner.nextInt();
                scanner.nextLine();

                switch (choice) {
                    case 1:
                        System.out.println("\n--- MANAGE TYPES ---");
                        System.out.println("1. Create New Type\n2. Edit Existing Type\n3. Delete Type\n4. View Types");
                        System.out.print("Select action: ");
                        int typeChoice = scanner.nextInt();
                        scanner.nextLine();

                        if (typeChoice == 1) {
                            System.out.print("Enter New Type Name (e.g., Haircare): ");
                            String tName = scanner.nextLine().trim();
                            System.out.print("Enter Attribute Label (e.g., Hair Type): ");
                            String tLabel = scanner.nextLine().trim();
                            System.out.print("Enter GST Rate Percentage: ");
                            double tGst = scanner.nextDouble();
                            typeManager.addType(tName, tLabel, tGst);
                            System.out.println("Product Type '" + tName + "' created successfully.");
                        } else if (typeChoice == 2) {
                            System.out.print("Enter Name of Type to Edit: ");
                            String oldName = scanner.nextLine().trim();
                            System.out.print("Enter New Attribute Label: ");
                            String newLabel = scanner.nextLine().trim();
                            System.out.print("Enter New GST Rate Percentage: ");
                            double newGst = scanner.nextDouble();
                            typeManager.editType(oldName, newLabel, newGst);
                        } else if (typeChoice == 3) {
                            System.out.print("Enter Name of Type to Delete: ");
                            String delName = scanner.nextLine().trim();
                            typeManager.deleteType(delName);
                        } else if (typeChoice == 4) {
                            typeManager.displayTypes();
                        } else {
                            System.out.println("Invalid action.");
                        }
                        break;

                    case 2:
                        System.out.println("\n--- SELECT PRODUCT TYPE ---");
                        typeManager.displayTypes();
                        System.out.print("Select Type number: ");
                        int typeIndex = scanner.nextInt() - 1;
                        scanner.nextLine();

                        ProductType selectedType = typeManager.getTypeByIndex(typeIndex);
                        if (selectedType == null) {
                            System.out.println("Invalid type selection.");
                            break;
                        }

                        System.out.print("Enter Product ID: ");
                        int id = scanner.nextInt();
                        scanner.nextLine();
                        System.out.print("Enter Brand: ");
                        String brand = scanner.nextLine();
                        System.out.print("Enter Product Name: ");
                        String name = scanner.nextLine();
                        System.out.print("Enter Category (e.g., Face Wash): ");
                        String catName = scanner.nextLine();
                        System.out.print("Enter Base Price (₹): ");
                        double price = scanner.nextDouble();
                        System.out.print("Enter Initial Stock: ");
                        int stock = scanner.nextInt();
                        scanner.nextLine();

                        System.out.print("Enter " + selectedType.getCustomAttributeLabel() + ": ");
                        String attrVal = scanner.nextLine();

                        storeInventory.addProduct(new CosmeticProduct(id, name, brand, price, stock, new Category(catName), selectedType, attrVal));
                        break;

                    case 3:
                        storeInventory.showAllProducts();
                        break;

                    case 4:
                        System.out.print("Enter Product ID for transaction: ");
                        int transId = scanner.nextInt();
                        Product p = storeInventory.findProductById(transId);
                        System.out.print("Enter quantity (Positive to restock, Negative to sell): ");
                        int qty = scanner.nextInt();
                        ((Transaction) p).processTransaction(qty);
                        break;

                    case 5:
                        System.out.print("Enter stock threshold for report: ");
                        int threshold = scanner.nextInt();
                        storeInventory.generateLowStockReport(threshold);
                        break;

                    case 6:
                        System.out.print("Enter Product ID to delete: ");
                        int delId = scanner.nextInt();
                        scanner.nextLine();
                        CosmeticProduct delProduct = storeInventory.findProductById(delId);
                        System.out.print("Delete '" + delProduct.getBrand() + " " + delProduct.getName() + "'? This also removes its transaction history (y/n): ");
                        String confirm = scanner.nextLine().trim();
                        if (confirm.equalsIgnoreCase("y")) {
                            storeInventory.deleteProduct(delId);
                        } else {
                            System.out.println("Delete cancelled.");
                        }
                        break;

                    case 7:
                        System.out.println("Exiting the system. Goodbye!");
                        running = false;
                        break;

                    default:
                        System.out.println("Invalid option.");
                }
            } catch (InputMismatchException e) {
                System.out.println("Input Error: Invalid data type entered.");
                scanner.nextLine();
            } catch (InvalidInputException | NotFoundException | InsufficientStockException | DatabaseException e) {
                System.out.println("Operation Failed: " + e.getMessage());
            }
        }
        scanner.close();
    }
}
