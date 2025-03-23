package DepartmentFinalScoreChart;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DepartmentScoreAnalyzer extends Application {

    private List<Student> students = new ArrayList<>();
    private BarChart<String, Number> barChart;
    private Map<String, Double> departmentAverages;
    private VBox legendBox; // Custom legend container

    // Define department colors
    private static final String CS_COLOR = "#00008B"; // Dark Blue
    private static final String MATHEMATICS_COLOR = "#FF0000"; // Red
    private static final String ENGINEERING_COLOR = "#008000"; // Green
    private static final String BUSINESS_COLOR = "#FFD700"; // Yellow/Gold

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Department Average Final Score Comparison");

        // Create UI components
        BorderPane root = new BorderPane();
        
        // Create chart area with fixed width bars
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Department");
        yAxis.setLabel("Average Final Score");
        
        // Set font for better readability
        xAxis.setTickLabelFont(Font.font("Arial", 12));
        yAxis.setTickLabelFont(Font.font("Arial", 12));
        
        barChart = new BarChart<>(xAxis, yAxis);
        barChart.setTitle("Average Final Scores by Department");
        barChart.setAnimated(false);
        barChart.setLegendVisible(false); // We'll use a custom legend
        
        // Initial fixed width (can be adjusted later)
        barChart.setPrefWidth(800);
        barChart.setCategoryGap(100);
        barChart.setBarGap(0);
        
        // Create buttons with better styling
        Button loadButton = new Button("Load CSV File");
        loadButton.setFont(Font.font("Arial", 12));
        loadButton.setPrefWidth(120);
        loadButton.setPrefHeight(30);
        
        Button exportButton = new Button("Export Results");
        exportButton.setFont(Font.font("Arial", 12));
        exportButton.setPrefWidth(120);
        exportButton.setPrefHeight(30);
        exportButton.setDisable(true);
        
        Label summaryLabel = new Label("Load a CSV file to see results");
        summaryLabel.setFont(Font.font("Arial", 14));
        
        // Create a custom legend container on the right
        legendBox = new VBox(10);
        legendBox.setPadding(new Insets(15));
        legendBox.setStyle("-fx-border-color: gray; -fx-border-width: 1;");
        
        // Set up button actions
        loadButton.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Open Student Data CSV File");
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
            File selectedFile = fileChooser.showOpenDialog(primaryStage);
            
            if (selectedFile != null) {
                try {
                    loadData(selectedFile.getAbsolutePath());
                    departmentAverages = calculateDepartmentAverages();
                    updateChart(departmentAverages);
                    updateSummary(summaryLabel, departmentAverages);
                    updateLegend(departmentAverages);
                    exportButton.setDisable(false);
                } catch (Exception ex) {
                    showAlert("Error", "Failed to load or process data: " + ex.getMessage());
                }
            }
        });
        
        exportButton.setOnAction(e -> {
            if (departmentAverages != null && !departmentAverages.isEmpty()) {
                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("Save Results");
                fileChooser.getExtensionFilters().add(
                        new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
                File file = fileChooser.showSaveDialog(primaryStage);
                
                if (file != null) {
                    exportData(file, departmentAverages);
                }
            } else {
                showAlert("Error", "No data to export");
            }
        });
        
        // Layout with better spacing
        HBox buttonBox = new HBox(20, loadButton, exportButton);
        buttonBox.setPadding(new Insets(15));
        
        VBox bottomBox = new VBox(15, buttonBox, summaryLabel);
        bottomBox.setPadding(new Insets(15));
        
        root.setCenter(barChart);
        root.setBottom(bottomBox);
        root.setRight(legendBox); // Add custom legend to the right
        
        // Add some padding around the chart
        BorderPane.setMargin(barChart, new Insets(20));
        
        Scene scene = new Scene(root, 900, 700);
        primaryStage.setScene(scene);
        primaryStage.show();
    }
    
    private void loadData(String filePath) throws IOException {
        students.clear();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            boolean isHeader = true;
            int finalScoreIndex = -1;
            int departmentIndex = -1;
            
            while ((line = reader.readLine()) != null) {
                if (isHeader) {
                    // Process header to find the correct column indices
                    String[] headers = line.split(",");
                    for (int i = 0; i < headers.length; i++) {
                        String header = headers[i].trim();
                        if (header.equals("Final_Score")) {
                            finalScoreIndex = i;
                        } else if (header.equals("Department")) {
                            departmentIndex = i;
                        }
                    }
                    
                    if (finalScoreIndex == -1 || departmentIndex == -1) {
                        throw new IOException("Required columns 'Final_Score' or 'Department' not found in CSV header");
                    }
                    
                    isHeader = false;
                    continue;
                }
                
                try {
                    Student student = parseStudent(line, departmentIndex, finalScoreIndex);
                    if (student != null) {
                        students.add(student);
                    }
                } catch (Exception e) {
                    System.err.println("Error parsing line: " + line);
                    System.err.println("Error message: " + e.getMessage());
                }
            }
        }
        
        System.out.println("Loaded " + students.size() + " students");
    }
    
    private Student parseStudent(String line, int departmentIndex, int finalScoreIndex) {
        // Split the CSV line, handling potential commas within quoted fields
        String[] parts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
        
        if (parts.length <= Math.max(departmentIndex, finalScoreIndex)) {
            System.err.println("Invalid line format (not enough fields): " + line);
            return null;
        }
        
        try {
            String studentId = parts[0].trim();
            String department = parts[departmentIndex].trim();
            
            // Parse the final score
            double finalScore = 0.0;
            if (!parts[finalScoreIndex].trim().isEmpty()) {
                try {
                    finalScore = Double.parseDouble(parts[finalScoreIndex].trim());
                } catch (NumberFormatException e) {
                    System.err.println("Invalid final score: " + parts[finalScoreIndex]);
                }
            }
            
            return new Student(studentId, department, finalScore);
        } catch (Exception e) {
            System.err.println("Error parsing student data: " + e.getMessage());
            return null;
        }
    }
    
    private Map<String, Double> calculateDepartmentAverages() {
        Map<String, List<Double>> departmentScores = new HashMap<>();
        
        // Group scores by department
        for (Student student : students) {
            departmentScores.computeIfAbsent(student.getDepartment(), k -> new ArrayList<>())
                    .add(student.getFinalScore());
        }
        
        // Calculate averages
        Map<String, Double> departmentAverages = new HashMap<>();
        for (Map.Entry<String, List<Double>> entry : departmentScores.entrySet()) {
            List<Double> scores = entry.getValue();
            double sum = 0.0;
            for (Double score : scores) {
                sum += score;
            }
            double average = scores.isEmpty() ? 0.0 : sum / scores.size();
            departmentAverages.put(entry.getKey(), average);
        }
        
        return departmentAverages;
    }
    
    private void updateChart(Map<String, Double> departmentAverages) {
        barChart.getData().clear();

        // Calculate computed width based on number of departments to keep a fixed bar width
        double fixedBarWidth = 80;
        double categoryGap = 50;
        int count = departmentAverages.size();
        double computedWidth = count * fixedBarWidth + (count + 1) * categoryGap;
        
        // Force the chart to have the computed width
        barChart.setMinWidth(computedWidth);
        barChart.setPrefWidth(computedWidth);
        barChart.setMaxWidth(computedWidth);
        
        // Create a single series for all departments
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Average Final Score");

        // Add data for each department
        for (Map.Entry<String, Double> entry : departmentAverages.entrySet()) {
            series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }
        
        barChart.getData().add(series);
        
        // Set the category gap for consistency
        barChart.setCategoryGap(categoryGap);
        
        // After nodes are created, update each bar's color and add the value label inside the bar.
        Platform.runLater(() -> {
            for (XYChart.Data<String, Number> data : series.getData()) {
                String department = data.getXValue();
                String color = getDepartmentColor(department);
                if (data.getNode() != null) {
                    // Update bar color
                    data.getNode().setStyle("-fx-bar-fill: " + color + ";");
                    
                    // Assume the bar node is a StackPane, add a label centered within it
                    if (data.getNode() instanceof StackPane) {
                        StackPane stackPane = (StackPane) data.getNode();
                        Label label = new Label(String.format("%.2f", data.getYValue()));
                        label.setFont(Font.font("Arial", FontWeight.BOLD, 12));
                        label.setTextFill(Color.WHITE);
                        stackPane.getChildren().add(label);
                        StackPane.setAlignment(label, Pos.CENTER);
                    }
                }
            }
        });
    }
    
    private void updateLegend(Map<String, Double> departmentAverages) {
        legendBox.getChildren().clear();
        Label legendTitle = new Label("Legend");
        legendTitle.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        legendBox.getChildren().add(legendTitle);
        
        // Loop through departments to create legend items
        for (String department : departmentAverages.keySet()) {
            HBox legendItem = new HBox(10);
            Region colorBox = new Region();
            colorBox.setPrefSize(15, 15);
            colorBox.setStyle("-fx-background-color: " + getDepartmentColor(department) + "; -fx-border-color: black;");
            
            Label deptLabel = new Label(department);
            deptLabel.setFont(Font.font("Arial", 12));
            
            legendItem.getChildren().addAll(colorBox, deptLabel);
            legendBox.getChildren().add(legendItem);
        }
    }
    
    private String getDepartmentColor(String department) {
        switch (department.trim()) {
            case "CS":
                return CS_COLOR;
            case "Mathematics":
                return MATHEMATICS_COLOR;
            case "Engineering":
                return ENGINEERING_COLOR;
            case "Business":
                return BUSINESS_COLOR;
            default:
                return "#808080"; // Default gray for unknown departments
        }
    }
    
    private void updateSummary(Label summaryLabel, Map<String, Double> departmentAverages) {
        StringBuilder summary = new StringBuilder("Summary of Average Final Scores by Department:\n\n");
        
        // Create a formatted summary
        for (Map.Entry<String, Double> entry : departmentAverages.entrySet()) {
            String department = entry.getKey();
            summary.append(String.format("%s: %.2f\n", department, entry.getValue()));
        }
        
        summaryLabel.setText(summary.toString());
    }
    
    private void exportData(File file, Map<String, Double> departmentAverages) {
        try (FileWriter writer = new FileWriter(file)) {
            // Write header
            writer.write("Department,Average Final Score\n");
            
            // Write data
            for (Map.Entry<String, Double> entry : departmentAverages.entrySet()) {
                writer.write(String.format("%s,%.2f\n", entry.getKey(), entry.getValue()));
            }
            
            showAlert("Success", "Data exported successfully to " + file.getName());
        } catch (IOException e) {
            showAlert("Error", "Failed to export data: " + e.getMessage());
        }
    }
    
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    // Inner class to represent a student
    private static class Student {
        private String studentId;
        private String department;
        private double finalScore;
        
        public Student(String studentId, String department, double finalScore) {
            this.studentId = studentId;
            this.department = department;
            this.finalScore = finalScore;
        }
        
        public String getStudentId() {
            return studentId;
        }
        
        public String getDepartment() {
            return department;
        }
        
        public double getFinalScore() {
            return finalScore;
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
