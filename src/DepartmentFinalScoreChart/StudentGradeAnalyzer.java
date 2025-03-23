import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StudentGradeAnalyzer extends Application {

    // Model class for holding a student record
    public static class Student {
        String department;
        Double finalScore; // can be null if missing

        public Student(String department, Double finalScore) {
            this.department = department;
            this.finalScore = finalScore;
        }
    }

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        String filePath = "Students_Grading_Dataset.csv"; // adjust the path as needed

        List<Student> students = new ArrayList<>();
        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
            String[] header = reader.readNext(); // read header

            int deptIndex = getColumnIndex(header, "Department");
            int finalScoreIndex = getColumnIndex(header, "Final_Score");

            if (deptIndex == -1 || finalScoreIndex == -1) {
                System.err.println("Required columns not found in CSV file.");
                return;
            }

            String[] line;
            while ((line = reader.readNext()) != null) {
                String department = line[deptIndex].trim();
                String finalScoreStr = line[finalScoreIndex].trim();
                Double finalScore = null;
                if (!finalScoreStr.isEmpty()) {
                    try {
                        finalScore = Double.parseDouble(finalScoreStr);
                    } catch (NumberFormatException e) {
                        // If parsing fails, leave as null to impute later
                    }
                }
                students.add(new Student(department, finalScore));
            }
        } catch (IOException | CsvValidationException e) {
            e.printStackTrace();
            return;
        }

        // First, calculate overall average final score from valid records
        double sum = 0;
        int count = 0;
        for (Student s : students) {
            if (s.finalScore != null) {
                sum += s.finalScore;
                count++;
            }
        }
        double overallAverage = (count > 0) ? sum / count : 0;

        // Impute missing final scores with overall average
        for (Student s : students) {
            if (s.finalScore == null) {
                s.finalScore = overallAverage;
            }
        }

        // Calculate average final score per department
        Map<String, List<Double>> deptScores = new HashMap<>();
        for (Student s : students) {
            deptScores.computeIfAbsent(s.department, k -> new ArrayList<>()).add(s.finalScore);
        }

        Map<String, Double> deptAverages = new HashMap<>();
        StringBuilder summaryBuilder = new StringBuilder("Department Average Final Scores:\n");
        for (Map.Entry<String, List<Double>> entry : deptScores.entrySet()) {
            List<Double> scores = entry.getValue();
            double total = scores.stream().mapToDouble(Double::doubleValue).sum();
            double avg = total / scores.size();
            deptAverages.put(entry.getKey(), avg);
            summaryBuilder.append(String.format("%s: %.2f\n", entry.getKey(), avg));
        }

        // Create a BarChart for visualization
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Department");
        yAxis.setLabel("Average Final Score");

        BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);
        barChart.setTitle("Average Final Scores by Department");

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Avg Final Score");

        for (Map.Entry<String, Double> entry : deptAverages.entrySet()) {
            series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }

        barChart.getData().add(series);

        // Create a TextArea to display the summary
        TextArea summaryArea = new TextArea(summaryBuilder.toString());
        summaryArea.setEditable(false);
        summaryArea.setPrefHeight(150);

        // Layout
        BorderPane root = new BorderPane();
        root.setCenter(barChart);
        root.setBottom(summaryArea);
        BorderPane.setMargin(summaryArea, new Insets(10));

        Scene scene = new Scene(root, 800, 600);
        primaryStage.setTitle("Student Grade Analyzer");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    // Helper method to get column index from header array
    private int getColumnIndex(String[] header, String columnName) {
        for (int i = 0; i < header.length; i++) {
            if (header[i].trim().equalsIgnoreCase(columnName)) {
                return i;
            }
        }
        return -1;
    }
}
