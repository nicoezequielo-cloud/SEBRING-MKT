import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.math.BigDecimal;

public class Main {

    // Credenciales para servidor de postgres
    private static final String URL = "jdbc:postgresql://localhost:5432/sebring";
    private static final String USER = "postgres";
    private static final String PASSWORD = "admin";

    public static void main(String[] args) {

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD)) {
            System.out.println("Conectado ok");

            createUsersTable(conn);
            createVehiclesTable(conn);
            createTransactionsTable(conn);

            insertUser(conn, "Alice", "Smith", "alice@example.com", "secret123", "1122334455", "12345678", false, true);
            insertUser(conn, "Bob", "Jones", "bob@example.com", "secret456", "1199887766", "87654321", true, true);
            insertUser(conn, "Carla", "Gomez", "carla.gomez@example.com", "carla2024", "1155667788", "30111222", false, true);

            if (countRows(conn, "vehicles") == 0) {
                insertSampleVehicles(conn);
            } else {
                System.out.println("Vehicles table already has data, skipping sample insert.");
            }

            if (countRows(conn, "transactions") == 0) {
                insertSampleTransactions(conn);
            } else {
                System.out.println("Transactions table already has data, skipping sample insert.");
            }

            readUsers(conn);
            readVehicles(conn);
            readTransactions(conn);

        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static int countRows(Connection conn, String tableName) throws SQLException {
        String sql = "SELECT COUNT(*) FROM " + tableName + ";";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    // ================= USERS =================

    private static void createUsersTable(Connection conn) throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS users (
                id SERIAL PRIMARY KEY,
                first_name VARCHAR(100) NOT NULL,
                last_name VARCHAR(100) NOT NULL,
                email VARCHAR(100) UNIQUE NOT NULL,
                password VARCHAR(255) NOT NULL,
                phone VARCHAR(20),
                national_id VARCHAR(20),
                is_admin BOOLEAN NOT NULL DEFAULT FALSE,
                registration_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                active BOOLEAN NOT NULL DEFAULT TRUE
            );
        """;
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            System.out.println("Table 'users' verified/created.");
        }
    }

    private static void insertUser(Connection conn, String firstName, String lastName, String email,
                                   String password, String phone, String nationalId,
                                   boolean isAdmin, boolean active) throws SQLException {
        String sql = """
            INSERT INTO users (first_name, last_name, email, password, phone, national_id, is_admin, active)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (email) DO NOTHING;
        """;
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, firstName);
            pstmt.setString(2, lastName);
            pstmt.setString(3, email);
            pstmt.setString(4, password);
            pstmt.setString(5, phone);
            pstmt.setString(6, nationalId);
            pstmt.setBoolean(7, isAdmin);
            pstmt.setBoolean(8, active);

            int rowsInserted = pstmt.executeUpdate();
            if (rowsInserted > 0) {
                System.out.println("Inserted user: " + firstName + " " + lastName);
            } else {
                System.out.println("User with email '" + email + "' already exists.");
            }
        }
    }

    private static void readUsers(Connection conn) throws SQLException {
        String sql = """
            SELECT id, first_name, last_name, email, phone, national_id, is_admin, registration_date, active
            FROM users
            ORDER BY id ASC;
        """;

        System.out.println("\n--- Current Users in Database ---");
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                System.out.printf(
                        "[%d] %s %s | %s | phone: %s | ID: %s | admin: %b | registered: %s | active: %b%n",
                        rs.getInt("id"), rs.getString("first_name"), rs.getString("last_name"),
                        rs.getString("email"), rs.getString("phone"), rs.getString("national_id"),
                        rs.getBoolean("is_admin"), rs.getTimestamp("registration_date"), rs.getBoolean("active")
                );
            }
        }
        System.out.println("----------------------------------\n");
    }

    // ================= VEHICLES =================

    private static void createVehiclesTable(Connection conn) throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS vehicles (
                id SERIAL PRIMARY KEY,
                brand VARCHAR(50) NOT NULL,
                model VARCHAR(50) NOT NULL,
                version VARCHAR(100),
                year INTEGER NOT NULL,
                price DECIMAL(12, 2) NOT NULL,
                currency VARCHAR(10) NOT NULL DEFAULT 'ARS',
                stock INTEGER NOT NULL DEFAULT 0,
                transmission VARCHAR(20),
                fuel_type VARCHAR(20),
                engine VARCHAR(50),
                image_url VARCHAR(255),
                description TEXT,
                status VARCHAR(20) NOT NULL DEFAULT 'available'
            );
        """;
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            System.out.println("Table 'vehicles' verified/created.");
        }
    }

    private static void insertVehicle(Connection conn, String brand, String model, String version, int year,
                                      BigDecimal price, String currency, int stock, String transmission,
                                      String fuelType, String engine, String imageUrl, String description,
                                      String status) throws SQLException {
        String sql = """
            INSERT INTO vehicles (brand, model, version, year, price, currency, stock, transmission,
                                   fuel_type, engine, image_url, description, status)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
        """;
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, brand);
            pstmt.setString(2, model);
            pstmt.setString(3, version);
            pstmt.setInt(4, year);
            pstmt.setBigDecimal(5, price);
            pstmt.setString(6, currency);
            pstmt.setInt(7, stock);
            pstmt.setString(8, transmission);
            pstmt.setString(9, fuelType);
            pstmt.setString(10, engine);
            pstmt.setString(11, imageUrl);
            pstmt.setString(12, description);
            pstmt.setString(13, status);
            pstmt.executeUpdate();
        }
    }

    // Representative sample of the Argentine dealership market
    private static void insertSampleVehicles(Connection conn) throws SQLException {
        insertVehicle(conn, "Volkswagen", "Gol Trend", "Trendline 1.6", 2022, new BigDecimal("12500000"), "ARS", 3,
                "Manual", "Nafta", "1.6L", "https://example.com/img/vw-gol-trend.jpg",
                "Volkswagen Gol Trend, ideal para ciudad, bajo consumo y fácil mantenimiento.", "available");

        insertVehicle(conn, "Toyota", "Hilux", "SRX 4x4 2.8 TDI", 2023, new BigDecimal("48000"), "USD", 2,
                "Automática", "Diesel", "2.8L Turbo Diesel", "https://example.com/img/toyota-hilux.jpg",
                "Toyota Hilux SRX 4x4, la pickup más elegida de Argentina, ideal para trabajo y aventura.", "available");

        insertVehicle(conn, "Ford", "Ranger", "Limited 3.0 V6", 2023, new BigDecimal("52000"), "USD", 1,
                "Automática", "Diesel", "3.0L V6 Turbo Diesel", "https://example.com/img/ford-ranger.jpg",
                "Ford Ranger Limited, tope de gama, con todas las prestaciones para trabajo y familia.", "available");

        insertVehicle(conn, "Chevrolet", "Onix", "LT 1.0 Turbo", 2023, new BigDecimal("13800000"), "ARS", 4,
                "Manual", "Nafta", "1.0L Turbo", "https://example.com/img/chevrolet-onix.jpg",
                "Chevrolet Onix, moderno y eficiente, con motor turbo de bajo consumo.", "available");

        insertVehicle(conn, "Fiat", "Cronos", "Drive 1.3", 2022, new BigDecimal("11200000"), "ARS", 5,
                "Manual", "Nafta", "1.3L", "https://example.com/img/fiat-cronos.jpg",
                "Fiat Cronos, uno de los sedanes más vendidos del país, gran relación precio-calidad.", "available");

        insertVehicle(conn, "Renault", "Sandero", "Zen 1.6", 2021, new BigDecimal("9800000"), "ARS", 2,
                "Manual", "Nafta", "1.6L", "https://example.com/img/renault-sandero.jpg",
                "Renault Sandero Zen, práctico y económico, perfecto para uso diario.", "used");

        insertVehicle(conn, "Peugeot", "208", "Feline 1.6", 2023, new BigDecimal("15600000"), "ARS", 3,
                "Automática", "Nafta", "1.6L", "https://example.com/img/peugeot-208.jpg",
                "Peugeot 208 Feline, diseño moderno y tecnología de punta.", "available");

        insertVehicle(conn, "Citroën", "C3", "Feel Pack 1.6", 2022, new BigDecimal("12900000"), "ARS", 2,
                "Manual", "Nafta", "1.6L", "https://example.com/img/citroen-c3.jpg",
                "Citroën C3 Feel Pack, confortable y con buen equipamiento de serie.", "available");

        insertVehicle(conn, "Honda", "HR-V", "EX 1.8 CVT", 2023, new BigDecimal("38000"), "USD", 1,
                "Automática", "Nafta", "1.8L", "https://example.com/img/honda-hrv.jpg",
                "Honda HR-V EX, SUV compacta con gran espacio interior y confiabilidad Honda.", "available");

        insertVehicle(conn, "Nissan", "Kicks", "Advance CVT", 2022, new BigDecimal("34000"), "USD", 2,
                "Automática", "Nafta", "1.6L", "https://example.com/img/nissan-kicks.jpg",
                "Nissan Kicks Advance, SUV urbana con excelente equipamiento tecnológico.", "available");

        insertVehicle(conn, "Jeep", "Compass", "Longitude 1.3T", 2023, new BigDecimal("45000"), "USD", 1,
                "Automática", "Nafta", "1.3L Turbo", "https://example.com/img/jeep-compass.jpg",
                "Jeep Compass Longitude, SUV mediana con look robusto y buen andar en ruta.", "available");

        insertVehicle(conn, "Volkswagen", "Amarok", "V6 Extreme", 2023, new BigDecimal("58000"), "USD", 1,
                "Automática", "Diesel", "3.0L V6 TDI", "https://example.com/img/vw-amarok.jpg",
                "Volkswagen Amarok V6 Extreme, la pickup premium con motor V6 y máximo confort.", "available");

        insertVehicle(conn, "Toyota", "Corolla", "XEI 2.0 CVT", 2022, new BigDecimal("28000"), "USD", 2,
                "Automática", "Nafta", "2.0L", "https://example.com/img/toyota-corolla.jpg",
                "Toyota Corolla XEI, sedán confiable con bajo costo de mantenimiento.", "available");

        insertVehicle(conn, "Ford", "EcoSport", "Freestyle 1.5", 2020, new BigDecimal("10500000"), "ARS", 1,
                "Manual", "Nafta", "1.5L", "https://example.com/img/ford-ecosport.jpg",
                "Ford EcoSport Freestyle, SUV compacta ideal para ciudad y rutas cortas.", "used");

        insertVehicle(conn, "Chevrolet", "Tracker", "Premier 1.2 Turbo", 2023, new BigDecimal("30000"), "USD", 2,
                "Automática", "Nafta", "1.2L Turbo", "https://example.com/img/chevrolet-tracker.jpg",
                "Chevrolet Tracker Premier, SUV con diseño moderno y motor turbo eficiente.", "available");

        System.out.println("Sample vehicles inserted.");
    }

    private static void readVehicles(Connection conn) throws SQLException {
        String sql = """
            SELECT id, brand, model, version, year, price, currency, stock, transmission,
                   fuel_type, engine, status
            FROM vehicles
            ORDER BY id ASC;
        """;

        System.out.println("\n--- Vehicles in Stock ---");
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                System.out.printf(
                        "[%d] %s %s %s (%d) | %s %s | stock: %d | %s/%s | status: %s%n",
                        rs.getInt("id"), rs.getString("brand"), rs.getString("model"), rs.getString("version"),
                        rs.getInt("year"), rs.getBigDecimal("price"), rs.getString("currency"),
                        rs.getInt("stock"), rs.getString("transmission"), rs.getString("fuel_type"),
                        rs.getString("status")
                );
            }
        }
        System.out.println("----------------------------------\n");
    }

    // ================= TRANSACTIONS =================

    private static void createTransactionsTable(Connection conn) throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS transactions (
                id SERIAL PRIMARY KEY,
                user_id INTEGER NOT NULL REFERENCES users(id),
                vehicle_id INTEGER NOT NULL REFERENCES vehicles(id),
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                transaction_type VARCHAR(20) NOT NULL,
                total_amount DECIMAL(12, 2) NOT NULL,
                payment_method VARCHAR(30),
                status VARCHAR(20) NOT NULL DEFAULT 'pending',
                receipt_url VARCHAR(255)
            );
        """;
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            System.out.println("Table 'transactions' verified/created.");
        }
    }

    // Looks up a user's id by email (since ids are not guaranteed to be sequential/predictable
    // across multiple runs, thanks to ON CONFLICT DO NOTHING keeping existing rows)
    private static Integer getUserIdByEmail(Connection conn, String email) throws SQLException {
        String sql = "SELECT id FROM users WHERE email = ?;";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, email);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        }
        return null;
    }

    // Looks up a vehicle's id by brand + model + version (picks the first match)
    private static Integer getVehicleId(Connection conn, String brand, String model, String version) throws SQLException {
        String sql = "SELECT id FROM vehicles WHERE brand = ? AND model = ? AND version = ? ORDER BY id ASC LIMIT 1;";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, brand);
            pstmt.setString(2, model);
            pstmt.setString(3, version);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        }
        return null;
    }

    private static void insertTransaction(Connection conn, int userId, int vehicleId, String transactionType,
                                          BigDecimal totalAmount, String paymentMethod, String status,
                                          String receiptUrl) throws SQLException {
        String sql = """
            INSERT INTO transactions (user_id, vehicle_id, transaction_type, total_amount,
                                       payment_method, status, receipt_url)
            VALUES (?, ?, ?, ?, ?, ?, ?);
        """;
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, vehicleId);
            pstmt.setString(3, transactionType);
            pstmt.setBigDecimal(4, totalAmount);
            pstmt.setString(5, paymentMethod);
            pstmt.setString(6, status);
            pstmt.setString(7, receiptUrl);
            pstmt.executeUpdate();
        }
    }

    // A handful of transactions, resolved dynamically by email/brand/model so they still
    // work correctly no matter how many times this script has already been run before.
    private static void insertSampleTransactions(Connection conn) throws SQLException {
        Integer alice = getUserIdByEmail(conn, "alice@example.com");
        Integer bob = getUserIdByEmail(conn, "bob@example.com");
        Integer carla = getUserIdByEmail(conn, "carla.gomez@example.com");

        Integer onix = getVehicleId(conn, "Chevrolet", "Onix", "LT 1.0 Turbo");
        Integer hilux = getVehicleId(conn, "Toyota", "Hilux", "SRX 4x4 2.8 TDI");
        Integer hrv = getVehicleId(conn, "Honda", "HR-V", "EX 1.8 CVT");
        Integer amarok = getVehicleId(conn, "Volkswagen", "Amarok", "V6 Extreme");

        if (alice != null && onix != null) {
            insertTransaction(conn, alice, onix, "purchase", new BigDecimal("13800000"), "financiacion",
                    "completed", "https://example.com/receipts/rec-001.pdf");
        }
        if (bob != null && hilux != null) {
            insertTransaction(conn, bob, hilux, "reservation", new BigDecimal("480000"), "transferencia",
                    "pending", null);
        }
        if (carla != null && hrv != null) {
            insertTransaction(conn, carla, hrv, "purchase", new BigDecimal("38000"), "efectivo",
                    "completed", "https://example.com/receipts/rec-002.pdf");
        }
        if (alice != null && amarok != null) {
            insertTransaction(conn, alice, amarok, "test_drive", new BigDecimal("0"), null,
                    "completed", null);
        }

        System.out.println("Sample transactions inserted.");
    }

    private static void readTransactions(Connection conn) throws SQLException {
        String sql = """
            SELECT t.id, t.user_id, t.vehicle_id, t.created_at, t.transaction_type,
                   t.total_amount, t.payment_method, t.status
            FROM transactions t
            ORDER BY t.id ASC;
        """;

        System.out.println("\n--- Transactions ---");
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                System.out.printf(
                        "[%d] user %d | vehicle %d | %s | %s | amount: %s | method: %s | status: %s%n",
                        rs.getInt("id"), rs.getInt("user_id"), rs.getInt("vehicle_id"),
                        rs.getTimestamp("created_at"), rs.getString("transaction_type"),
                        rs.getBigDecimal("total_amount"), rs.getString("payment_method"), rs.getString("status")
                );
            }
        }
        System.out.println("----------------------------------\n");
    }
}