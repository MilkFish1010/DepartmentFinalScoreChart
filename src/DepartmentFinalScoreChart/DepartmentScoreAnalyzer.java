package DepartmentFinalScoreChart;

import javafx.application.Application;
import javafx.geometry.Insets;
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
import javafx.scene.layout.VBox;
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
        
        // Create chart area
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Department");
        yAxis.setLabel("Average Final Score");
        
        barChart = new BarChart<>(xAxis, yAxis);
        barChart.setTitle("Average Final Scores by Department");
        barChart.setAnimated(false);
        barChart.setLegendVisible(false); // Hide the legend since we're using the x-axis labels
        
        // Create buttons
        Button loadButton = new Button("Load CSV File");
        Button exportButton = new Button("Export Results");
        exportButton.setDisable(true);
        
        Label summaryLabel = new Label("Load a CSV file to see results");
        
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
        
        // Layout
        HBox buttonBox = new HBox(10, loadButton, exportButton);
        buttonBox.setPadding(new Insets(10));
        
        VBox bottomBox = new VBox(10, buttonBox, summaryLabel);
        bottomBox.setPadding(new Insets(10));
        
        root.setCenter(barChart);
        root.setBottom(bottomBox);
        
        Scene scene = new Scene(root, 800, 600);
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
                    // Continue processing other lines
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
                    // Use 0 as default
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
        
        // Create a single series for all departments
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Average Final Score");
        
        // Add data for each department
        for (Map.Entry<String, Double> entry : departmentAverages.entrySet()) {
            series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }
        
        barChart.getData().add(series);
        
        // Apply colors after the chart has been updated
        applyCssToChart();
    }
    
    private void applyCssToChart() {
        // Apply custom colors to each bar based on department name
        for (XYChart.Series<String, Number> series : barChart.getData()) {
            for (XYChart.Data<String, Number> data : series.getData()) {
                String department = data.getXValue();
                String color = getDepartmentColor(department);
                
                // Apply color to the bar
                if (data.getNode() != null) {
                    data.getNode().setStyle("-fx-bar-fill: " + color + ";");
                }
            }
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
        StringBuilder summary = new StringBuilder("Summary of Average Final Scores by Department:\n");
        
        // Create a formatted summary with department colors
        for (Map.Entry<String, Double> entry : departmentAverages.entrySet()) {
            String department = entry.getKey();
            String colorHex = getDepartmentColor(department);
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
        
        @Override
        public String toString() {
            return "Student{" +
                    "studentId='" + studentId + '\'' +
                    ", department='" + department + '\'' +
                    ", finalScore=" + finalScore +
                    '}';
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
