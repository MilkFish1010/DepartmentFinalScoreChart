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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

public class DepartmentScoreAnalyzer extends Application {

    private StudentDataProcessor dataProcessor;
    private BarChart<String, Number> barChart;
    private Map<String, Double> departmentAverages;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Department Average Final Score Comparison");

        // Initialize the data processor
        dataProcessor = new StudentDataProcessor();

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
                    dataProcessor.loadData(selectedFile.getAbsolutePath());
                    departmentAverages = dataProcessor.calculateDepartmentAverages();
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
    
    private void updateChart(Map<String, Double> departmentAverages) {
        barChart.getData().clear();
        
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Average Final Score");
        
        for (Map.Entry<String, Double> entry : departmentAverages.entrySet()) {
            series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }
        
        barChart.getData().add(series);
    }
    
    private void updateSummary(Label summaryLabel, Map<String, Double> departmentAverages) {
        StringBuilder summary = new StringBuilder("Summary of Average Final Scores by Department:\n");
        
        for (Map.Entry<String, Double> entry : departmentAverages.entrySet()) {
            summary.append(String.format("%s: %.2f\n", entry.getKey(), entry.getValue()));
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

    public static void main(String[] args) {
        launch(args);
    }
}
