package src;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.ArrayList;
import java.util.List;

public class ToDoListApp extends JFrame {
    // Modern Color Palette
    private static final Color PRIMARY_COLOR = new Color(100, 149, 237);  // Cornflower Blue
    private static final Color SECONDARY_COLOR = new Color(255, 215, 0);  // Gold
    private static final Color ACCENT_COLOR = new Color(255, 105, 97);    // Coral
    private static final Color BACKGROUND_COLOR = new Color(245, 245, 245); // Light Gray
    private static final Color CARD_COLOR = Color.WHITE;
    private static final Color TEXT_COLOR = new Color(51, 51, 51);        // Dark Gray
    private static final Color SUCCESS_COLOR = new Color(46, 204, 113);    // Emerald
    private static final Color WARNING_COLOR = new Color(230, 126, 34);    // Carrot Orange
    private static final Color DANGER_COLOR = new Color(231, 76, 60);     // Alizarin
    private static final Color DISABLED_COLOR = new Color(189, 195, 199);  // Silver
    
    // Database constants
    private static final String DB_URL = "jdbc:sqlite:tasks.db";
    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm";
    
    // UI Components
    private JTextField taskField;
    private JTextField dueDateField;
    private JComboBox<String> priorityBox;
    private JList<Task> taskList;
    private DefaultListModel<Task> listModel;
    private JButton addButton, deleteButton, completeButton;
    private JLabel statusLabel;
    
    // Database and utilities
    private Connection conn;
    private SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
    private Timer notificationTimer;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                new ToDoListApp().setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public ToDoListApp() {
        configureWindow();
        initializeDatabase();
        setupUIComponents();
        setupEventHandlers();
        startBackgroundServices();
        loadTasks();
    }

    private void configureWindow() {
        setTitle("Modern To-Do List");
        setSize(750, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(BACKGROUND_COLOR);
    }

    private void initializeDatabase() {
        try {
            Class.forName("org.sqlite.JDBC");
            conn = DriverManager.getConnection(DB_URL);
            createTables();
        } catch (Exception e) {
            showError("Database Error", "Failed to initialize database: " + e.getMessage());
        }
    }

    private void createTables() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS tasks (" +
                     "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                     "task TEXT NOT NULL, " +
                     "due TEXT NOT NULL, " +
                     "priority TEXT NOT NULL, " +
                     "status TEXT NOT NULL, " +
                     "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)";
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }

    private void setupUIComponents() {
        // Header Panel
        JPanel headerPanel = new JPanel();
        headerPanel.setBackground(PRIMARY_COLOR);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        JLabel titleLabel = new JLabel("My Tasks");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setForeground(Color.WHITE);
        headerPanel.add(titleLabel);
        add(headerPanel, BorderLayout.NORTH);

        // Input Panel
        JPanel inputPanel = createInputPanel();
        add(inputPanel, BorderLayout.NORTH);

        // Task List
        listModel = new DefaultListModel<>();
        taskList = new JList<>(listModel);
        taskList.setCellRenderer(new ModernTaskRenderer());
        taskList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        taskList.setFixedCellHeight(70);
        taskList.setBackground(CARD_COLOR);
        taskList.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JScrollPane scrollPane = new JScrollPane(taskList);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(CARD_COLOR);
        add(scrollPane, BorderLayout.CENTER);

        // Button Panel
        JPanel buttonPanel = createButtonPanel();
        
        // Status Bar
        statusLabel = new JLabel("Ready");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
        statusLabel.setForeground(TEXT_COLOR);
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        
        JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.setBackground(BACKGROUND_COLOR);
        southPanel.add(buttonPanel, BorderLayout.CENTER);
        southPanel.add(statusLabel, BorderLayout.SOUTH);
        add(southPanel, BorderLayout.SOUTH);
    }

   private JPanel createInputPanel() {
    JPanel panel = new JPanel(new GridBagLayout());
    panel.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(230, 230, 230)),
        BorderFactory.createEmptyBorder(15, 15, 15, 15)
    ));  // Fixed - added closing parenthesis for createCompoundBorder
    panel.setBackground(CARD_COLOR);
    
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(5, 5, 5, 5);
    gbc.fill = GridBagConstraints.HORIZONTAL;


        // Task Field
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        JLabel taskLabel = new JLabel("New Task:");
        taskLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        taskLabel.setForeground(TEXT_COLOR);
        panel.add(taskLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        taskField = new JTextField();
        taskField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        taskField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 220, 220)),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        panel.add(taskField, gbc);

        // Due Date Field
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        JLabel dueLabel = new JLabel("Due Date:");
        dueLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        dueLabel.setForeground(TEXT_COLOR);
        panel.add(dueLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        dueDateField = new JTextField(dateFormat.format(new Date()));
        dueDateField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        dueDateField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 220, 220)),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        panel.add(dueDateField, gbc);

        // Priority Combo Box
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0;
        JLabel priorityLabel = new JLabel("Priority:");
        priorityLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        priorityLabel.setForeground(TEXT_COLOR);
        panel.add(priorityLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        priorityBox = new JComboBox<>(new String[]{"Low", "Medium", "High"});
        priorityBox.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        priorityBox.setRenderer(new PriorityComboBoxRenderer());
        priorityBox.setBackground(Color.WHITE);
        priorityBox.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 220, 220)),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        panel.add(priorityBox, gbc);

        // Add Button
        gbc.gridx = 1;
        gbc.gridy = 3;
        gbc.anchor = GridBagConstraints.LINE_END;
        addButton = createModernButton("Add Task", SUCCESS_COLOR);
        panel.add(addButton, gbc);

        return panel;
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 15));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.setBackground(BACKGROUND_COLOR);

        completeButton = createModernButton("✓ Complete", SUCCESS_COLOR);
        deleteButton = createModernButton("✗ Delete", DANGER_COLOR);
        
        panel.add(completeButton);
        panel.add(deleteButton);

        return panel;
    }

    private JButton createModernButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(bgColor.darker(), 1),
            BorderFactory.createEmptyBorder(8, 20, 8, 20)
        ));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // Hover effect
        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                button.setBackground(bgColor.brighter());
            }
            public void mouseExited(MouseEvent e) {
                button.setBackground(bgColor);
            }
        });
        
        return button;
    }

    private void setupEventHandlers() {
        // Add Task
        addButton.addActionListener(e -> addTask());
        taskField.addActionListener(e -> addTask());

        // Mark Complete
        completeButton.addActionListener(e -> markSelectedTaskComplete());
        taskList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    markSelectedTaskComplete();
                }
            }
        });

        // Delete Task
        deleteButton.addActionListener(e -> deleteSelectedTask());
    }

    private void startBackgroundServices() {
        // Auto-refresh every minute
        new Timer(true).scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                SwingUtilities.invokeLater(() -> loadTasks());
            }
        }, 60_000, 60_000);

        // Notification checker
        notificationTimer = new Timer(true);
        notificationTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                checkForDueTasks();
            }
        }, 0, 60_000); // Check every minute
    }

    private void addTask() {
        String taskText = taskField.getText().trim();
        String dueDateText = dueDateField.getText().trim();
        String priority = (String) priorityBox.getSelectedItem();

        // Validate input
        if (taskText.isEmpty()) {
            showError("Validation Error", "Please enter a task description");
            return;
        }

        if (!isValidDate(dueDateText)) {
            showError("Date Error", "Please enter a valid date in format: " + DATE_FORMAT);
            return;
        }

        try (PreparedStatement pst = conn.prepareStatement(
                "INSERT INTO tasks(task, due, priority, status) VALUES (?, ?, ?, 'Pending')")) {
            pst.setString(1, taskText);
            pst.setString(2, dueDateText);
            pst.setString(3, priority);
            pst.executeUpdate();
            
            loadTasks();
            clearInputFields();
            updateStatus("Task added successfully", SUCCESS_COLOR);
        } catch (SQLException e) {
            showError("Database Error", "Failed to add task: " + e.getMessage());
        }
    }

    private void loadTasks() {
        listModel.clear();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM tasks ORDER BY due ASC")) {
            
            Date now = new Date();
            while (rs.next()) {
                Task task = new Task(
                    rs.getInt("id"),
                    rs.getString("task"),
                    rs.getString("due"),
                    rs.getString("priority"),
                    rs.getString("status")
                );
                
                // Check if task is overdue
                if (!"Done".equals(task.getStatus())) {
                    try {
                        Date dueDate = dateFormat.parse(task.getDue());
                        if (dueDate.before(now)) {
                            task.setStatus("Overdue");
                            updateTaskStatus(task);
                        }
                    } catch (Exception e) {
                        // Date parsing error - keep original status
                    }
                }
                
                listModel.addElement(task);
            }
            
            updateStatus("Loaded " + listModel.size() + " tasks", TEXT_COLOR);
        } catch (SQLException e) {
            showError("Database Error", "Failed to load tasks: " + e.getMessage());
        }
    }

    private void markSelectedTaskComplete() {
        Task selected = taskList.getSelectedValue();
        if (selected == null) {
            showError("Selection Error", "Please select a task to mark complete");
            return;
        }

        selected.setStatus("Done");
        updateTaskStatus(selected);
        loadTasks();
        updateStatus("Task marked as complete", SUCCESS_COLOR);
    }

    private void deleteSelectedTask() {
        Task selected = taskList.getSelectedValue();
        if (selected == null) {
            showError("Selection Error", "Please select a task to delete");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
            this, 
            "Delete task: " + selected.getDescription() + "?", 
            "Confirm Delete", 
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );
        
        if (confirm == JOptionPane.YES_OPTION) {
            try (PreparedStatement pst = conn.prepareStatement("DELETE FROM tasks WHERE id=?")) {
                pst.setInt(1, selected.getId());
                pst.executeUpdate();
                loadTasks();
                updateStatus("Task deleted successfully", SUCCESS_COLOR);
            } catch (SQLException e) {
                showError("Database Error", "Failed to delete task: " + e.getMessage());
            }
        }
    }

    private void checkForDueTasks() {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                 "SELECT * FROM tasks WHERE status NOT IN ('Done', 'Dismissed')")) {
            
            Date now = new Date();
            List<Task> dueTasks = new ArrayList<>();
            
            while (rs.next()) {
                try {
                    Date dueDate = dateFormat.parse(rs.getString("due"));
                    if (dueDate.before(now) || isDueSoon(dueDate, now)) {
                        dueTasks.add(new Task(
                            rs.getInt("id"),
                            rs.getString("task"),
                            rs.getString("due"),
                            rs.getString("priority"),
                            rs.getString("status")
                        ));
                    }
                } catch (Exception e) {
                    // Skip tasks with invalid dates
                }
            }
            
            if (!dueTasks.isEmpty()) {
                SwingUtilities.invokeLater(() -> showNotification(dueTasks));
            }
        } catch (SQLException e) {
            System.err.println("Error checking due tasks: " + e.getMessage());
        }
    }

    private boolean isDueSoon(Date dueDate, Date now) {
        long oneHour = 60 * 60 * 1000;
        return dueDate.getTime() - now.getTime() <= oneHour;
    }

    private void showNotification(List<Task> dueTasks) {
        // Play notification sound
        Toolkit.getDefaultToolkit().beep();
        
        StringBuilder message = new StringBuilder("<html><div style='font-size:14px'>");
        message.append("<b>The following tasks are due:</b><ul style='margin-top:5px'>");
        
        for (Task task : dueTasks) {
            String dueStatus = task.getStatus().equals("Overdue") ? " (OVERDUE!)" : "";
            message.append("<li style='margin-bottom:3px'>")
                  .append(task.getDescription())
                  .append(" - Due: ").append(task.getDue())
                  .append(dueStatus).append("</li>");
        }
        message.append("</ul></div></html>");
        
        Object[] options = {"Mark Complete", "Snooze (1 hour)", "Dismiss"};
        int choice = JOptionPane.showOptionDialog(
            this,
            message.toString(),
            "Task Due Notification",
            JOptionPane.YES_NO_CANCEL_OPTION,
            JOptionPane.WARNING_MESSAGE,
            null,
            options,
            options[0]
        );
        
        if (choice == 0) { // Mark Complete
            for (Task task : dueTasks) {
                task.setStatus("Done");
                updateTaskStatus(task);
            }
            updateStatus(dueTasks.size() + " tasks marked complete", SUCCESS_COLOR);
        } else if (choice == 1) { // Snooze
            snoozeTasks(dueTasks);
        } else if (choice == 2) { // Dismiss
            dismissTasks(dueTasks);
        }
        
        loadTasks();
    }

    private void updateTaskStatus(Task task) {
        try (PreparedStatement pst = conn.prepareStatement(
                "UPDATE tasks SET status=? WHERE id=?")) {
            pst.setString(1, task.getStatus());
            pst.setInt(2, task.getId());
            pst.executeUpdate();
        } catch (SQLException e) {
            showError("Database Error", "Failed to update task status: " + e.getMessage());
        }
    }

    private void snoozeTasks(List<Task> tasks) {
        try (PreparedStatement pst = conn.prepareStatement(
                "UPDATE tasks SET due=? WHERE id=?")) {
            String newDue = dateFormat.format(new Date(System.currentTimeMillis() + 60 * 60 * 1000));
            
            for (Task task : tasks) {
                pst.setString(1, newDue);
                pst.setInt(2, task.getId());
                pst.executeUpdate();
            }
            updateStatus(tasks.size() + " tasks snoozed for 1 hour", WARNING_COLOR);
        } catch (SQLException e) {
            showError("Database Error", "Failed to snooze tasks: " + e.getMessage());
        }
    }

    private void dismissTasks(List<Task> tasks) {
        try (PreparedStatement pst = conn.prepareStatement(
                "UPDATE tasks SET status='Dismissed' WHERE id=?")) {
            for (Task task : tasks) {
                pst.setInt(1, task.getId());
                pst.executeUpdate();
            }
            updateStatus(tasks.size() + " tasks dismissed", DISABLED_COLOR);
        } catch (SQLException e) {
            showError("Database Error", "Failed to dismiss tasks: " + e.getMessage());
        }
    }

    private void clearInputFields() {
        taskField.setText("");
        dueDateField.setText(dateFormat.format(new Date()));
        priorityBox.setSelectedIndex(0);
        taskField.requestFocus();
    }

    private boolean isValidDate(String dateStr) {
        try {
            dateFormat.parse(dateStr);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void updateStatus(String message, Color color) {
        statusLabel.setText(message);
        statusLabel.setForeground(color);
    }

    private void showError(String title, String message) {
        JOptionPane.showMessageDialog(
            this,
            message,
            title,
            JOptionPane.ERROR_MESSAGE
        );
    }

    @Override
    protected void processWindowEvent(WindowEvent e) {
        if (e.getID() == WindowEvent.WINDOW_CLOSING) {
            try {
                if (conn != null && !conn.isClosed()) {
                    conn.close();
                }
                if (notificationTimer != null) {
                    notificationTimer.cancel();
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
        super.processWindowEvent(e);
    }

    private static class Task {
        private int id;
        private String description;
        private String due;
        private String priority;
        private String status;

        public Task(int id, String description, String due, String priority, String status) {
            this.id = id;
            this.description = description;
            this.due = due;
            this.priority = priority;
            this.status = status;
        }

        // Getters and setters
        public int getId() { return id; }
        public String getDescription() { return description; }
        public String getDue() { return due; }
        public String getPriority() { return priority; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        @Override
        public String toString() {
            return description;
        }
    }

    private static class ModernTaskRenderer extends JPanel implements ListCellRenderer<Task> {
        private JLabel descriptionLabel;
        private JLabel dueLabel;
        private JLabel priorityLabel;
        private JCheckBox checkBox;

        public ModernTaskRenderer() {
            setLayout(new BorderLayout(10, 5));
            setOpaque(true);
            setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(240, 240, 240)),
                BorderFactory.createEmptyBorder(10, 15, 10, 15)
            ));

            // Checkbox
            checkBox = new JCheckBox();
            checkBox.setOpaque(false);

            // Description
            descriptionLabel = new JLabel();
            descriptionLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));

            // Details panel
            JPanel detailsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
            detailsPanel.setOpaque(false);
            
            dueLabel = new JLabel();
            dueLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            dueLabel.setIcon(new ImageIcon(createColoredCircleIcon(8, DISABLED_COLOR)));
            dueLabel.setIconTextGap(5);
            
            priorityLabel = new JLabel();
            priorityLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            priorityLabel.setIconTextGap(5);

            detailsPanel.add(dueLabel);
            detailsPanel.add(priorityLabel);

            JPanel textPanel = new JPanel(new BorderLayout(0, 5));
            textPanel.add(descriptionLabel, BorderLayout.NORTH);
            textPanel.add(detailsPanel, BorderLayout.SOUTH);
            textPanel.setOpaque(false);

            add(checkBox, BorderLayout.WEST);
            add(textPanel, BorderLayout.CENTER);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends Task> list, Task task, 
                int index, boolean isSelected, boolean cellHasFocus) {
            
            // Set background
            if (isSelected) {
                setBackground(new Color(240, 248, 255)); // Alice Blue
            } else {
                setBackground(CARD_COLOR);
            }

            // Set task information
            descriptionLabel.setText(task.getDescription());
            
            // Format due date
            dueLabel.setText("Due: " + task.getDue());
            
            // Set priority with colored circle
            priorityLabel.setText(task.getPriority());
            priorityLabel.setIcon(new ImageIcon(createPriorityIcon(task.getPriority())));
            
            // Set status
            checkBox.setSelected("Done".equals(task.getStatus()));
            
            // Style based on status
            if ("Done".equals(task.getStatus())) {
                descriptionLabel.setForeground(DISABLED_COLOR);
                descriptionLabel.setFont(descriptionLabel.getFont().deriveFont(Font.ITALIC));
                dueLabel.setForeground(DISABLED_COLOR);
                priorityLabel.setForeground(DISABLED_COLOR);
            } 
            else if ("Overdue".equals(task.getStatus())) {
                descriptionLabel.setForeground(DANGER_COLOR);
                descriptionLabel.setFont(descriptionLabel.getFont().deriveFont(Font.BOLD));
                dueLabel.setForeground(DANGER_COLOR);
                dueLabel.setIcon(new ImageIcon(createColoredCircleIcon(8, DANGER_COLOR)));
            }
            else {
                descriptionLabel.setForeground(TEXT_COLOR);
                dueLabel.setForeground(TEXT_COLOR);
                
                // Priority colors
                switch (task.getPriority()) {
                    case "High":
                        priorityLabel.setForeground(DANGER_COLOR);
                        break;
                    case "Medium":
                        priorityLabel.setForeground(WARNING_COLOR);
                        break;
                    case "Low":
                        priorityLabel.setForeground(PRIMARY_COLOR);
                        break;
                }
            }

            return this;
        }

        private Image createPriorityIcon(String priority) {
            Color color;
            switch (priority) {
                case "High": color = DANGER_COLOR; break;
                case "Medium": color = WARNING_COLOR; break;
                case "Low": color = PRIMARY_COLOR; break;
                default: color = DISABLED_COLOR;
            }
            return createColoredCircleIcon(8, color);
        }

        private Image createColoredCircleIcon(int diameter, Color color) {
            try {
                BufferedImage image = new BufferedImage(diameter, diameter, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2d = image.createGraphics();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(color);
                g2d.fillOval(0, 0, diameter, diameter);
                g2d.dispose();
                return image;
            } catch (Exception e) {
                return new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
            }
        }
    }

    private static class PriorityComboBoxRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, 
                int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            
            if (value != null) {
                setText(value.toString());
                
                // Set icon based on priority
                switch (value.toString()) {
                    case "High":
                        setForeground(DANGER_COLOR);
                        break;
                    case "Medium":
                        setForeground(WARNING_COLOR);
                        break;
                    case "Low":
                        setForeground(PRIMARY_COLOR);
                        break;
                }
            }
            
            return this;
        }
    }
}