import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

public class RetailStoreManagementSystem extends JFrame {
    private CardLayout cardLayout;
    private JPanel mainPanel;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JTextArea displayArea;
    private JLabel welcomeLabel;
    private Connection connection;
    private String currentUser;
    private boolean isAdmin;

    public RetailStoreManagementSystem() {
        setTitle("Retail Store Management System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        mainPanel.add(buildLoginPanel(), "Login");
        mainPanel.add(buildDashboardPanel(), "Dashboard");

        add(mainPanel);
        cardLayout.show(mainPanel, "Login");

        connectToDatabase();
        createTablesIfNotExist();
        setVisible(true);
    }

    private JPanel buildLoginPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        usernameField = new JTextField(15);
        passwordField = new JPasswordField(15);

        JButton loginBtn = new JButton("Login");
        JButton registerBtn = new JButton("Register");

        loginBtn.addActionListener(e -> login());
        registerBtn.addActionListener(e -> register());

        gbc.insets = new Insets(5, 5, 5, 5);

        gbc.gridx = 0; gbc.gridy = 0; panel.add(new JLabel("Username:"), gbc);
        gbc.gridx = 1; panel.add(usernameField, gbc);

        gbc.gridx = 0; gbc.gridy = 1; panel.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1; panel.add(passwordField, gbc);

        gbc.gridx = 0; gbc.gridy = 2; panel.add(loginBtn, gbc);
        gbc.gridx = 1; panel.add(registerBtn, gbc);

        return panel;
    }

    private JPanel buildDashboardPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        JPanel topPanel = new JPanel(new BorderLayout());
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JButton menuBtn = new JButton("Menu");
        JButton logoutBtn = new JButton("Logout");

        menuBtn.addActionListener(e -> openMenu());
        logoutBtn.addActionListener(e -> logout());

        buttonPanel.add(menuBtn);
        buttonPanel.add(logoutBtn);

        welcomeLabel = new JLabel("Welcome!");
        welcomeLabel.setFont(new Font("Arial", Font.BOLD, 18));
        welcomeLabel.setHorizontalAlignment(SwingConstants.CENTER);

        topPanel.add(welcomeLabel, BorderLayout.NORTH);
        topPanel.add(buttonPanel, BorderLayout.SOUTH);

        displayArea = new JTextArea();
        displayArea.setEditable(false);

        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(displayArea), BorderLayout.CENTER);

        return panel;
    }

    private void connectToDatabase() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver"); // Ensure this is the correct driver version
            // Try to connect to the database 'retailstorelog'
            connection = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/retailstorelog", "root", "");
            
            if (connection != null) {
                System.out.println("Connected to existing retailstorelog Database!");
            }
        } catch (SQLException e) {
            // If connection fails, create the database and tables
            System.out.println("Failed to connect to retailstorelog database. Creating the database...");
            try (Connection tempConnection = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/", "root", "")) {
                Statement stmt = tempConnection.createStatement();
                stmt.execute("CREATE DATABASE IF NOT EXISTS retailstorelog");
                System.out.println("Database created or already exists.");

                // Reconnect to the new database
                connection = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/retailstorelog", "root", "");
                System.out.println("Reconnected to retailstorelog Database!");

            } catch (SQLException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Failed to create database and connect!");
                System.exit(0);
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "MySQL JDBC Driver not found. Please include it in your project.");
            System.exit(0);
        }
    }

    private void createTablesIfNotExist() {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS users (id INT AUTO_INCREMENT PRIMARY KEY, username VARCHAR(50) UNIQUE, password VARCHAR(50), isAdmin BOOLEAN)");
            stmt.execute("CREATE TABLE IF NOT EXISTS inventory (id INT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(100), price DOUBLE, quantity INT)");
            stmt.execute("CREATE TABLE IF NOT EXISTS ledger (id INT AUTO_INCREMENT PRIMARY KEY, type VARCHAR(10), itemName VARCHAR(100), quantity INT)");

            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users WHERE username='AK21'");
            if (rs.next() && rs.getInt(1) == 0) {
                stmt.execute("INSERT INTO users (username, password, isAdmin) VALUES ('AK21', '123456', true)");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void login() {
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());

        try (PreparedStatement pst = connection.prepareStatement(
            "SELECT * FROM users WHERE username=? AND password=?")) {
            pst.setString(1, username);
            pst.setString(2, password);

            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                currentUser = username;
                isAdmin = rs.getBoolean("isAdmin");
                welcomeLabel.setText("Welcome, " + username + "!");
                cardLayout.show(mainPanel, "Dashboard");
                JOptionPane.showMessageDialog(this, "Login Successful!");
            } else {
                JOptionPane.showMessageDialog(this, "Invalid Credentials!");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void register() {
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());

        try (PreparedStatement check = connection.prepareStatement(
            "SELECT * FROM users WHERE username=?")) {
            check.setString(1, username);
            ResultSet rs = check.executeQuery();
            if (rs.next()) {
                JOptionPane.showMessageDialog(this, "Username already exists!");
                return;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        try (PreparedStatement pst = connection.prepareStatement(
            "INSERT INTO users (username, password, isAdmin) VALUES (?, ?, false)")) {
            pst.setString(1, username);
            pst.setString(2, password);
            pst.executeUpdate();
            JOptionPane.showMessageDialog(this, "Registered Successfully!");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void openMenu() {
        if (currentUser == null) return;

        String[] options = isAdmin
            ? new String[] {"View Users", "Add User", "Remove User", "View Inventory", "Add Item", "Remove Item", "View Transactions", "Logout"}
            : new String[] {"View Inventory", "Buy Item", "Sell Item", "View Transactions", "Logout"};

        String choice = (String) JOptionPane.showInputDialog(
            this, "Select an action", "Menu",
            JOptionPane.PLAIN_MESSAGE, null, options, options[0]);

        if (choice == null) return;

        switch (choice) {
            case "View Users": viewUsers(); break;
            case "Add User": addUser(); break;
            case "Remove User": removeUser(); break;
            case "View Inventory": viewInventory(); break;
            case "Add Item": addItem(); break;
            case "Remove Item": removeItem(); break;
            case "Buy Item": buyItem(); break;
            case "Sell Item": sellItem(); break;
            case "View Transactions": viewTransactions(); break;
            case "Logout": logout(); break;
        }
    }

    private void logout() {
        currentUser = null;
        cardLayout.show(mainPanel, "Login");
    }

    private void viewUsers() {
        displayArea.setText("--- Registered Users ---\n");
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT username, isAdmin FROM users")) {
            while (rs.next()) {
                displayArea.append(rs.getString("username") + (rs.getBoolean("isAdmin") ? " (Admin)" : "") + "\n");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void addUser() {
        String username = JOptionPane.showInputDialog(this, "Enter Username:");
        String password = JOptionPane.showInputDialog(this, "Enter Password:");
        String adminInput = JOptionPane.showInputDialog(this, "Is Admin? (yes/no)");

        if (username == null || password == null) return;

        boolean isAdmin = "yes".equalsIgnoreCase(adminInput);

        try (PreparedStatement pst = connection.prepareStatement(
            "INSERT INTO users (username, password, isAdmin) VALUES (?, ?, ?)")) {
            pst.setString(1, username);
            pst.setString(2, password);
            pst.setBoolean(3, isAdmin);
            pst.executeUpdate();
            JOptionPane.showMessageDialog(this, "User Added!");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void removeUser() {
        String username = JOptionPane.showInputDialog(this, "Enter Username to Remove:");

        try (PreparedStatement pst = connection.prepareStatement(
            "DELETE FROM users WHERE username=?")) {
            pst.setString(1, username);
            pst.executeUpdate();
            JOptionPane.showMessageDialog(this, "User Removed!");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void viewInventory() {
        displayArea.setText("--- Inventory ---\n");
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM inventory")) {
            while (rs.next()) {
                displayArea.append(rs.getString("name") + " | Price: " + rs.getDouble("price") + " | Qty: " + rs.getInt("quantity") + "\n");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void addItem() {
        String name = JOptionPane.showInputDialog(this, "Enter Item Name:");
        double price = Double.parseDouble(JOptionPane.showInputDialog(this, "Enter Price:"));
        int qty = Integer.parseInt(JOptionPane.showInputDialog(this, "Enter Quantity:"));

        try (PreparedStatement pst = connection.prepareStatement(
            "INSERT INTO inventory (name, price, quantity) VALUES (?, ?, ?)")) {
            pst.setString(1, name);
            pst.setDouble(2, price);
            pst.setInt(3, qty);
            pst.executeUpdate();
            JOptionPane.showMessageDialog(this, "Item Added!");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void removeItem() {
        String name = JOptionPane.showInputDialog(this, "Enter Item Name to Remove:");

        try (PreparedStatement pst = connection.prepareStatement(
            "DELETE FROM inventory WHERE name=?")) {
            pst.setString(1, name);
            pst.executeUpdate();
            JOptionPane.showMessageDialog(this, "Item Removed!");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void buyItem() {
        String name = JOptionPane.showInputDialog(this, "Enter Item Name:");
        int qty = Integer.parseInt(JOptionPane.showInputDialog(this, "Enter Quantity:"));

        try (PreparedStatement pst = connection.prepareStatement(
            "UPDATE inventory SET quantity = quantity - ? WHERE name = ?")) {
            pst.setInt(1, qty);
            pst.setString(2, name);
            pst.executeUpdate();

            try (PreparedStatement ledger = connection.prepareStatement(
                "INSERT INTO ledger (type, itemName, quantity) VALUES ('Buy', ?, ?)")) {
                ledger.setString(1, name);
                ledger.setInt(2, qty);
                ledger.executeUpdate();
            }

            JOptionPane.showMessageDialog(this, "Bought Successfully!");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void sellItem() {
        String name = JOptionPane.showInputDialog(this, "Enter Item Name:");
        int qty = Integer.parseInt(JOptionPane.showInputDialog(this, "Enter Quantity:"));

        try (PreparedStatement pst = connection.prepareStatement(
            "UPDATE inventory SET quantity = quantity + ? WHERE name = ?")) {
            pst.setInt(1, qty);
            pst.setString(2, name);
            pst.executeUpdate();

            try (PreparedStatement ledger = connection.prepareStatement(
                "INSERT INTO ledger (type, itemName, quantity) VALUES ('Sell', ?, ?)")) {
                ledger.setString(1, name);
                ledger.setInt(2, qty);
                ledger.executeUpdate();
            }

            JOptionPane.showMessageDialog(this, "Sold Successfully!");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void viewTransactions() {
        displayArea.setText("--- Transactions ---\n");
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM ledger")) {
            while (rs.next()) {
                displayArea.append(rs.getString("type") + " | " + rs.getString("itemName") + " | Qty: " + rs.getInt("quantity") + "\n");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(RetailStoreManagementSystem::new);
    }
}
