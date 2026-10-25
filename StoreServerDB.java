import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.awt.Desktop;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

public class StoreServerDB {

    // ---------- DATABASE CONFIG (override with environment variables) ----------
    static final String URL  = env("DB_URL",  "jdbc:mysql://localhost:3306/cosmetic_store");
    static final String USER = env("DB_USER", "root");
    static final String PASS = env("DB_PASS", "Apple23@_");

    static String env(String k, String d) { String v = System.getenv(k); return v == null ? d : v; }
    static Connection db() throws SQLException { return DriverManager.getConnection(URL, USER, PASS); }

    interface Action { String run(String method, Map<String, String> p) throws Exception; }

    public static void main(String[] args) throws Exception {
        try (Connection c = db()) {
            System.out.println("Connected to MySQL.");
        } catch (SQLException e) {
            System.out.println("Could not connect to MySQL: " + e.getMessage());
            System.out.println("Set DB_URL / DB_USER / DB_PASS, or edit PASS in this file.");
            return;
        }

        // Port: java StoreServerDB   or   java StoreServerDB 9090
        int port = 8080;
        if (args.length > 0) {
            try { port = Integer.parseInt(args[0]); } catch (NumberFormatException ignored) {}
        }

        // 0.0.0.0 = this laptop + other laptops on the same Wi-Fi/LAN
        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);

        // Frontend
        server.createContext("/", ex -> {
            String path = ex.getRequestURI().getPath();
            if (path.equals("/") || path.equals("/index.html")) {
                Path html = Path.of("index.html");
                if (!Files.exists(html)) {
                    send(ex, 404, "text/plain", "index.html not found next to StoreServerDB.class");
                    return;
                }
                send(ex, 200, "text/html", Files.readString(html));
            } else {
                send(ex, 404, "text/plain", "Not found");
            }
        });

        // GET all products / POST add product
        route(server, "/api/products", (m, p) -> {
            if (m.equals("GET"))
                return productsJson("SELECT * FROM product_details ORDER BY product_id");
            addProduct(p);
            return "{\"ok\":true}";
        });

        // POST delete product
        route(server, "/api/products/delete", (m, p) -> {
            int id = toInt(p, "id");
            try (Connection c = db();
                 PreparedStatement ps = c.prepareStatement("DELETE FROM products WHERE product_id=?")) {
                ps.setInt(1, id);
                if (ps.executeUpdate() == 0) throw new IllegalArgumentException("Product ID not found.");
            }
            return "{\"ok\":true}";
        });

        // GET all types / POST create type
        route(server, "/api/types", (m, p) -> {
            if (m.equals("GET")) return typesJson();
            try (Connection c = db();
                 PreparedStatement ps = c.prepareStatement(
                         "INSERT INTO product_types (type_name, attribute_label, gst_rate) VALUES (?,?,?)")) {
                ps.setString(1, req(p, "name"));
                ps.setString(2, req(p, "label"));
                ps.setDouble(3, nonNegative(toDouble(p, "gst"), "GST"));
                ps.executeUpdate();
            } catch (SQLIntegrityConstraintViolationException e) {
                throw new IllegalArgumentException("Type already exists.");
            }
            return "{\"ok\":true}";
        });

        route(server, "/api/types/edit", (m, p) -> {
            try (Connection c = db();
                 PreparedStatement ps = c.prepareStatement(
                         "UPDATE product_types SET attribute_label=?, gst_rate=? WHERE type_name=?")) {
                ps.setString(1, req(p, "label"));
                ps.setDouble(2, nonNegative(toDouble(p, "gst"), "GST"));
                ps.setString(3, req(p, "name"));
                if (ps.executeUpdate() == 0) throw new IllegalArgumentException("Product type not found.");
            }
            return "{\"ok\":true}";
        });

        route(server, "/api/types/delete", (m, p) -> {
            try (Connection c = db();
                 PreparedStatement ps = c.prepareStatement("DELETE FROM product_types WHERE type_name=?")) {
                ps.setString(1, req(p, "name"));
                if (ps.executeUpdate() == 0) throw new IllegalArgumentException("Product type not found.");
            } catch (SQLIntegrityConstraintViolationException e) {
                throw new IllegalArgumentException("Cannot delete: products still use this type.");
            }
            return "{\"ok\":true}";
        });

        // POST buy/sell (qty positive = restock, negative = sell)
        route(server, "/api/transaction", (m, p) -> {
            int id = toInt(p, "id");
            try (Connection c = db()) {
                try (CallableStatement cs = c.prepareCall("{CALL process_transaction(?, ?)}")) {
                    cs.setInt(1, id);
                    cs.setInt(2, toInt(p, "qty"));
                    cs.execute();
                }
                try (PreparedStatement ps = c.prepareStatement("SELECT stock_quantity FROM products WHERE product_id=?")) {
                    ps.setInt(1, id);
                    ResultSet rs = ps.executeQuery();
                    rs.next();
                    return "{\"stock\":" + rs.getInt(1) + "}";
                }
            }
        });

        // GET low stock report
        route(server, "/api/lowstock", (m, p) ->
            productsJson("SELECT * FROM product_details WHERE stock_quantity < ? ORDER BY stock_quantity",
                         toInt(p, "threshold")));

        server.start();
        printAccessInfo(port);
        openBrowser("http://localhost:" + port);
    }

    // ---------- NETWORK ACCESS INFO ----------
    static void printAccessInfo(int port) {
        System.out.println();
        System.out.println("========================================");
        System.out.println("  Cosmetic Store (MySQL) is running");
        System.out.println("========================================");
        System.out.println("  On THIS laptop:  http://localhost:" + port);
        System.out.println();
        System.out.println("  From OTHER laptops (same Wi-Fi / LAN):");
        boolean found = false;
        try {
            Enumeration<NetworkInterface> nics = NetworkInterface.getNetworkInterfaces();
            while (nics.hasMoreElements()) {
                NetworkInterface nic = nics.nextElement();
                if (!nic.isUp() || nic.isLoopback()) continue;
                Enumeration<InetAddress> addrs = nic.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress addr = addrs.nextElement();
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                        System.out.println("    http://" + addr.getHostAddress() + ":" + port);
                        found = true;
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("    (could not detect LAN IP - run ipconfig / ifconfig)");
        }
        if (!found) {
            System.out.println("    (no LAN IP found - connect Wi-Fi and restart server)");
        }
        System.out.println();
        System.out.println("  Share the http://YOUR-IP:" + port + " link with others.");
        System.out.println("  Keep this window open while people use the app.");
        System.out.println("========================================");
        System.out.println();
    }

    static void openBrowser(String url) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
                System.out.println("  Browser opened: " + url);
                return;
            }
        } catch (Exception ignored) {}
        try {
            String os = System.getProperty("os.name", "").toLowerCase();
            if (os.contains("win")) new ProcessBuilder("cmd", "/c", "start", url).start();
            else if (os.contains("mac")) new ProcessBuilder("open", url).start();
            else new ProcessBuilder("xdg-open", url).start();
            System.out.println("  Browser opened: " + url);
        } catch (Exception e) {
            System.out.println("  Open manually: " + url);
        }
    }

    // ---------- DATABASE OPERATIONS ----------
    static void addProduct(Map<String, String> p) throws Exception {
        int id = toInt(p, "id");
        double price = nonNegative(toDouble(p, "price"), "Price");
        int stock = toInt(p, "stock");
        if (stock < 0) throw new IllegalArgumentException("Stock cannot be negative.");

        try (Connection c = db()) {
            int typeId = lookup(c, "SELECT type_id FROM product_types WHERE type_name=?", req(p, "type"));
            if (typeId < 0) throw new IllegalArgumentException("Type '" + p.get("type") + "' not found.");

            String cat = req(p, "category");
            int catId = lookup(c, "SELECT category_id FROM categories WHERE category_name=?", cat);
            if (catId < 0) {   // create category on the fly
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO categories (category_name) VALUES (?)", Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, cat);
                    ps.executeUpdate();
                    ResultSet keys = ps.getGeneratedKeys();
                    keys.next();
                    catId = keys.getInt(1);
                }
            }

            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO products (product_id, name, brand, base_price, stock_quantity, attribute_value, category_id, type_id) "
                  + "VALUES (?,?,?,?,?,?,?,?)")) {
                ps.setInt(1, id);
                ps.setString(2, req(p, "name"));
                ps.setString(3, req(p, "brand"));
                ps.setDouble(4, price);
                ps.setInt(5, stock);
                ps.setString(6, p.getOrDefault("attr", ""));
                ps.setInt(7, catId);
                ps.setInt(8, typeId);
                ps.executeUpdate();
            } catch (SQLIntegrityConstraintViolationException e) {
                throw new IllegalArgumentException("Product ID already exists.");
            }
        }
    }

    static int lookup(Connection c, String sql, String value) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, value);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : -1;
        }
    }

    static String productsJson(String sql, Object... args) throws SQLException {
        try (Connection c = db(); PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < args.length; i++) ps.setObject(i + 1, args[i]);
            ResultSet rs = ps.executeQuery();
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            while (rs.next()) {
                if (!first) sb.append(",");
                first = false;
                sb.append("{\"id\":").append(rs.getInt("product_id"))
                  .append(",\"name\":\"").append(esc(rs.getString("name")))
                  .append("\",\"brand\":\"").append(esc(rs.getString("brand")))
                  .append("\",\"category\":\"").append(esc(rs.getString("category_name")))
                  .append("\",\"type\":\"").append(esc(rs.getString("type_name")))
                  .append("\",\"label\":\"").append(esc(rs.getString("attribute_label")))
                  .append("\",\"attr\":\"").append(esc(rs.getString("attribute_value")))
                  .append("\",\"stock\":").append(rs.getInt("stock_quantity"))
                  .append(",\"price\":").append(rs.getBigDecimal("base_price").toPlainString())
                  .append(",\"finalPrice\":").append(rs.getBigDecimal("final_price").toPlainString())
                  .append("}");
            }
            return sb.append("]").toString();
        }
    }

    static String typesJson() throws SQLException {
        try (Connection c = db();
             ResultSet rs = c.createStatement().executeQuery(
                     "SELECT type_name, attribute_label, gst_rate FROM product_types ORDER BY type_id")) {
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            while (rs.next()) {
                if (!first) sb.append(",");
                first = false;
                sb.append("{\"name\":\"").append(esc(rs.getString(1)))
                  .append("\",\"label\":\"").append(esc(rs.getString(2)))
                  .append("\",\"gst\":").append(rs.getBigDecimal(3).stripTrailingZeros().toPlainString()).append("}");
            }
            return sb.append("]").toString();
        }
    }

    // ---------- HTTP PLUMBING ----------
    static void route(HttpServer server, String path, Action action) {
        server.createContext(path, ex -> {
            try {
                if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) {
                    ex.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                    ex.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
                    ex.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
                    ex.sendResponseHeaders(204, -1);
                    ex.close();
                    return;
                }
                String method = ex.getRequestMethod();
                String raw = method.equals("GET")
                        ? ex.getRequestURI().getRawQuery()
                        : new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                send(ex, 200, "application/json", action.run(method, parse(raw)));
            } catch (Exception e) {
                send(ex, 400, "application/json", "{\"error\":\"" + esc(e.getMessage()) + "\"}");
            }
        });
    }

    static void send(HttpExchange ex, int code, String type, String body) throws IOException {
        byte[] b = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", type + "; charset=utf-8");
        ex.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        ex.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        ex.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
        ex.sendResponseHeaders(code, b.length);
        ex.getResponseBody().write(b);
        ex.close();
    }

    static Map<String, String> parse(String raw) {
        Map<String, String> m = new HashMap<>();
        if (raw == null || raw.isEmpty()) return m;
        for (String kv : raw.split("&")) {
            String[] p = kv.split("=", 2);
            m.put(URLDecoder.decode(p[0], StandardCharsets.UTF_8),
                  p.length > 1 ? URLDecoder.decode(p[1], StandardCharsets.UTF_8) : "");
        }
        return m;
    }

    static String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }

    static String req(Map<String, String> p, String key) {
        String v = p.get(key);
        if (v == null || v.trim().isEmpty()) throw new IllegalArgumentException("Missing field: " + key);
        return v.trim();
    }

    static int toInt(Map<String, String> p, String key) {
        try { return Integer.parseInt(req(p, key)); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("Input Error: " + key + " must be a whole number."); }
    }

    static double toDouble(Map<String, String> p, String key) {
        try { return Double.parseDouble(req(p, key)); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("Input Error: " + key + " must be a number."); }
    }

    static double nonNegative(double v, String label) {
        if (v < 0) throw new IllegalArgumentException(label + " cannot be negative.");
        return v;
    }
}
