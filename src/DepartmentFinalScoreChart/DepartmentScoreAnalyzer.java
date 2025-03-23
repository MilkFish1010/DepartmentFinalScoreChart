package DepartmentFinalScoreChart;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// PDFBox imports – ensure these jars (pdfbox, fontbox, commons-logging, etc.) are in your lib folder
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;

public class DepartmentScoreAnalyzer extends Application {

    private List<Student> students = new ArrayList<>();
    private Map<String, Double> departmentAverages;
    private VBox legendBox; // Custom legend container
    private BorderPane appPane; // Main app layout pane
    private BarChart chart;   // Currently displayed chart
    private boolean isVerticalChart = true; // Toggle flag

    // Define department colors
    private static final String CS_COLOR = "#00008B"; // Dark Blue
    private static final String MATHEMATICS_COLOR = "#FF0000"; // Red
    private static final String ENGINEERING_COLOR = "#008000"; // Green
    private static final String BUSINESS_COLOR = "#FFD700"; // Yellow/Gold

    // Panes for switching between home screen and app screen
    private StackPane rootPane;
    private VBox homePane;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Department Average Final Score Comparison");

        // Create the root StackPane
        rootPane = new StackPane();
        // Create the homescreen and the app screen
        createHomePane();
        createAppPane(primaryStage);

        // Initially, show the home screen.
        rootPane.getChildren().addAll(appPane, homePane);
        appPane.setVisible(false);
        homePane.setVisible(true);

        Scene scene = new Scene(rootPane, 1000, 700);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    // Creates the homescreen pane.
    private void createHomePane() {
        homePane = new VBox(20);
        homePane.setAlignment(Pos.CENTER);
        homePane.setPadding(new Insets(20));
        homePane.setStyle("-fx-background-color: linear-gradient(from 0% 0% to 100% 100%, #ffd89b, #ff9d8e); " +
                          "-fx-font-family: 'Comic Neue', cursive;");

        Label title = new Label("Average Final Score Reaper");
        title.setFont(Font.font("Comic Neue", FontWeight.BOLD, 40));
        title.setTextFill(Color.web("#4a2c2a"));

        Label author = new Label("Author: Hoby Ace Jerico Josol");
        author.setFont(Font.font("Comic Neue", 20));
        author.setTextFill(Color.web("#4a2c2a"));

        Label description = new Label("Gathers average final score of students per department.");
        description.setFont(Font.font("Comic Neue", 16));
        description.setTextFill(Color.web("#4a2c2a"));

        Button startButton = new Button("🚀 Start");
        startButton.setFont(Font.font("Comic Neue", 14));
        startButton.setPrefWidth(200);
        startButton.setPrefHeight(40);
        startButton.setStyle("-fx-background-color: #4a2c2a; -fx-text-fill: white; " +
                             "-fx-background-radius: 30;");
        startButton.setOnAction(e -> {
            // Switch to appPane (main UI)
            homePane.setVisible(false);
            appPane.setVisible(true);
        });

        homePane.getChildren().addAll(title, author, description, startButton);
    }

    // Creates the main app pane (appPane) containing the chart, controls, and legend.
    private void createAppPane(Stage primaryStage) {
        appPane = new BorderPane();
        appPane.setStyle("-fx-background-color: linear-gradient(from 0% 0% to 100% 100%, #ffd89b, #ff9d8e); " +
                         "-fx-font-family: 'Comic Neue', cursive;");

        // Legend on the right
        legendBox = new VBox(10);
        legendBox.setPadding(new Insets(15));
        legendBox.setStyle("-fx-border-color: gray; -fx-border-width: 1; " +
                           "-fx-background-color: rgba(255,255,255,0.9);");
        appPane.setRight(legendBox);

        // Create control buttons with increased widths and emojis added
        Button loadButton = new Button("📂 Load CSV File");
        loadButton.setFont(Font.font("Comic Neue", 12));
        loadButton.setPrefWidth(200);
        loadButton.setPrefHeight(40);
        loadButton.setStyle("-fx-background-color: #4a2c2a; -fx-text-fill: white; " +
                            "-fx-padding: 15 30 15 30; -fx-font-size: 1.2em; -fx-background-radius: 30;");

        Button exportCSVButton = new Button("💾 Export CSV");
        exportCSVButton.setFont(Font.font("Comic Neue", 12));
        exportCSVButton.setPrefWidth(200);
        exportCSVButton.setPrefHeight(40);
        exportCSVButton.setStyle("-fx-background-color: #4a2c2a; -fx-text-fill: white; " +
                                 "-fx-padding: 15 30 15 30; -fx-font-size: 1.2em; -fx-background-radius: 30;");
        exportCSVButton.setDisable(true);

        Button exportPNGButton = new Button("🖼️ Export PNG");
        exportPNGButton.setFont(Font.font("Comic Neue", 12));
        exportPNGButton.setPrefWidth(200);
        exportPNGButton.setPrefHeight(40);
        exportPNGButton.setStyle("-fx-background-color: #4a2c2a; -fx-text-fill: white; " +
                                 "-fx-padding: 15 30 15 30; -fx-font-size: 1.2em; -fx-background-radius: 30;");
        exportPNGButton.setDisable(true);

        Button exportPDFButton = new Button("📄 Export PDF");
        exportPDFButton.setFont(Font.font("Comic Neue", 12));
        exportPDFButton.setPrefWidth(200);
        exportPDFButton.setPrefHeight(40);
        exportPDFButton.setStyle("-fx-background-color: #4a2c2a; -fx-text-fill: white; " +
                                 "-fx-padding: 15 30 15 30; -fx-font-size: 1.2em; -fx-background-radius: 30;");
        exportPDFButton.setDisable(true);

        Button resetButton = new Button("🔄 Reset");
        resetButton.setFont(Font.font("Comic Neue", 12));
        resetButton.setPrefWidth(200);
        resetButton.setPrefHeight(40);
        resetButton.setStyle("-fx-background-color: #4a2c2a; -fx-text-fill: white; " +
                             "-fx-background-radius: 30;");
        resetButton.setOnAction(e -> {
            // Reset: clear data and switch back to home screen
            students.clear();
            departmentAverages = null;
            chart = null;
            legendBox.getChildren().clear();
            appPane.setCenter(null);
            // Disable export and toggle buttons
            exportCSVButton.setDisable(true);
            exportPNGButton.setDisable(true);
            exportPDFButton.setDisable(true);
            // Show homescreen
            appPane.setVisible(false);
            homePane.setVisible(true);
        });

        Button toggleOrientationButton = new Button("↕️ Toggle Orientation");
        toggleOrientationButton.setFont(Font.font("Comic Neue", 12));
        toggleOrientationButton.setPrefWidth(200);
        toggleOrientationButton.setPrefHeight(40);
        toggleOrientationButton.setStyle("-fx-background-color: #4a2c2a; -fx-text-fill: white; " +
                                         "-fx-padding: 15 30 15 30; -fx-font-size: 1.2em; -fx-background-radius: 30;");
        toggleOrientationButton.setDisable(true);
        toggleOrientationButton.setOnAction(e -> {
            isVerticalChart = !isVerticalChart;
            chart = createChart(isVerticalChart, departmentAverages);
            appPane.setCenter(chart);
        });

        final Label summaryLabel = new Label("Load a CSV file to see results");
        summaryLabel.setFont(Font.font("Comic Neue", 14));
        summaryLabel.setStyle("-fx-text-fill: #4a2c2a;");

        // Layout for bottom controls (Load button at top, export buttons and summary below, then reset)
        VBox bottomBox = new VBox(15);
        bottomBox.setPadding(new Insets(15));
        bottomBox.setAlignment(Pos.CENTER);
        bottomBox.getChildren().add(loadButton);
        HBox exportButtonBox = new HBox(20, exportCSVButton, exportPNGButton, exportPDFButton);
        exportButtonBox.setAlignment(Pos.CENTER);
        bottomBox.getChildren().addAll(exportButtonBox, summaryLabel, resetButton);
        appPane.setBottom(bottomBox);

        // Place the toggle orientation button in a left VBox, center-aligned vertically
        VBox leftBox = new VBox(toggleOrientationButton);
        leftBox.setPadding(new Insets(15));
        leftBox.setAlignment(Pos.CENTER);
        appPane.setLeft(leftBox);

        // --- New: Load CSV File event handler ---
        loadButton.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Open CSV File");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
            File file = fileChooser.showOpenDialog(primaryStage);
            if (file != null) {
                try {
                    loadData(file.getAbsolutePath());
                    departmentAverages = calculateDepartmentAverages();
                    chart = createChart(isVerticalChart, departmentAverages);
                    appPane.setCenter(chart);
                    updateLegend(departmentAverages);
                    updateSummary(summaryLabel, departmentAverages);
                    exportCSVButton.setDisable(false);
                    exportPNGButton.setDisable(false);
                    exportPDFButton.setDisable(false);
                    toggleOrientationButton.setDisable(false);
                } catch (IOException ex) {
                    showAlert("Error", "Error loading CSV file: " + ex.getMessage());
                }
            }
        });

        // --- Export CSV event handler ---
        exportCSVButton.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save CSV File");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
            File file = fileChooser.showSaveDialog(primaryStage);
            if (file != null) {
                exportData(file, departmentAverages);
            }
        });

        // --- Export PNG event handler ---
        exportPNGButton.setOnAction(e -> {
            exportChartAsPNG(primaryStage);
        });

        // --- Export PDF event handler ---
        exportPDFButton.setOnAction(e -> {
            exportChartAsPDF(primaryStage);
        });
    }

    // Creates and returns a BarChart based on the orientation flag.
    private BarChart createChart(boolean isVertical, Map<String, Double> averages) {
        BarChart chart;
        double fixedBarWidth = 80;
        double fixedBarHeight = 80;
        double categoryGap = 50;
        int count = averages.size();

        if (isVertical) {
            double computedWidth = count * fixedBarWidth + (count + 1) * categoryGap;
            CategoryAxis xAxis = new CategoryAxis();
            NumberAxis yAxis = new NumberAxis();
            xAxis.setLabel("Department");
            yAxis.setLabel("Average Final Score");
            chart = new BarChart<>(xAxis, yAxis);
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            for (Map.Entry<String, Double> entry : averages.entrySet()) {
                series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
            }
            chart.getData().add(series);
            Platform.runLater(() -> {
                for (XYChart.Data<String, Number> data : series.getData()) {
                    String color = getDepartmentColor(data.getXValue());
                    if (data.getNode() != null) {
                        data.getNode().setStyle("-fx-bar-fill: " + color + ";");
                        if (data.getNode() instanceof javafx.scene.layout.StackPane) {
                            javafx.scene.layout.StackPane stackPane = (javafx.scene.layout.StackPane) data.getNode();
                            Label label = new Label(String.format("%.2f", data.getYValue()));
                            label.setFont(Font.font("Comic Neue", FontWeight.BOLD, 12));
                            label.setTextFill(Color.WHITE);
                            stackPane.getChildren().add(label);
                            javafx.scene.layout.StackPane.setAlignment(label, Pos.CENTER);
                        }
                    }
                }
            });
            chart.setMinWidth(computedWidth);
            chart.setPrefWidth(computedWidth);
            chart.setMaxWidth(computedWidth);
        } else {
            double computedHeight = count * fixedBarHeight + (count + 1) * categoryGap;
            NumberAxis xAxis = new NumberAxis();
            CategoryAxis yAxis = new CategoryAxis();
            xAxis.setLabel("Average Final Score");
            yAxis.setLabel("Department");
            chart = new BarChart<>(xAxis, yAxis);
            XYChart.Series<Number, String> series = new XYChart.Series<>();
            for (Map.Entry<String, Double> entry : averages.entrySet()) {
                series.getData().add(new XYChart.Data<>(entry.getValue(), entry.getKey()));
            }
            chart.getData().add(series);
            Platform.runLater(() -> {
                for (XYChart.Data<Number, String> data : series.getData()) {
                    String color = getDepartmentColor(data.getYValue());
                    if (data.getNode() != null) {
                        data.getNode().setStyle("-fx-bar-fill: " + color + ";");
                        if (data.getNode() instanceof javafx.scene.layout.StackPane) {
                            javafx.scene.layout.StackPane stackPane = (javafx.scene.layout.StackPane) data.getNode();
                            Label label = new Label(String.format("%.2f", data.getXValue()));
                            label.setFont(Font.font("Comic Neue", FontWeight.BOLD, 12));
                            label.setTextFill(Color.WHITE);
                            stackPane.getChildren().add(label);
                            javafx.scene.layout.StackPane.setAlignment(label, Pos.CENTER);
                        }
                    }
                }
            });
            chart.setMinHeight(computedHeight);
            chart.setPrefHeight(computedHeight);
            chart.setMaxHeight(computedHeight);
        }

        chart.setTitle("Average Final Scores by Department");
        chart.setAnimated(false);
        chart.setLegendVisible(false); // Custom legend is used
        chart.setCategoryGap(categoryGap);
        chart.setBarGap(0);
        BorderPane.setMargin(chart, new Insets(20));
        return chart;
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
        String[] parts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
        if (parts.length <= Math.max(departmentIndex, finalScoreIndex)) {
            System.err.println("Invalid line format (not enough fields): " + line);
            return null;
        }
        try {
            String studentId = parts[0].trim();
            String department = parts[departmentIndex].trim();
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
        for (Student student : students) {
            departmentScores.computeIfAbsent(student.getDepartment(), k -> new ArrayList<>())
                    .add(student.getFinalScore());
        }
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

    private void updateLegend(Map<String, Double> departmentAverages) {
        legendBox.getChildren().clear();
        Label legendTitle = new Label("Legend");
        legendTitle.setFont(Font.font("Comic Neue", FontWeight.BOLD, 14));
        legendBox.getChildren().add(legendTitle);
        for (String department : departmentAverages.keySet()) {
            HBox legendItem = new HBox(10);
            Region colorBox = new Region();
            colorBox.setPrefSize(15, 15);
            colorBox.setStyle("-fx-background-color: " + getDepartmentColor(department) + "; -fx-border-color: black;");
            Label deptLabel = new Label(department);
            deptLabel.setFont(Font.font("Comic Neue", 12));
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
                return "#808080";
        }
    }

    private void updateSummary(Label summaryLabel, Map<String, Double> departmentAverages) {
        StringBuilder summary = new StringBuilder("Summary of Average Final Scores by Department:\n\n");
        for (Map.Entry<String, Double> entry : departmentAverages.entrySet()) {
            summary.append(String.format("%s: %.2f\n", entry.getKey(), entry.getValue()));
        }
        summaryLabel.setText(summary.toString());
    }

    private void exportData(File file, Map<String, Double> departmentAverages) {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("Department,Average Final Score\n");
            for (Map.Entry<String, Double> entry : departmentAverages.entrySet()) {
                writer.write(String.format("%s,%.2f\n", entry.getKey(), entry.getValue()));
            }
            showAlert("Success", "Data exported successfully to " + file.getName());
        } catch (IOException e) {
            showAlert("Error", "Failed to export data: " + e.getMessage());
        }
    }

    // Export chart as PNG image
    private void exportChartAsPNG(Stage primaryStage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Chart as PNG");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG Files", "*.png"));
        File file = fileChooser.showSaveDialog(primaryStage);
        if (file != null) {
            WritableImage snapshot = chart.snapshot(new SnapshotParameters(), null);
            BufferedImage bufferedImage = SwingFXUtils.fromFXImage(snapshot, null);
            try {
                ImageIO.write(bufferedImage, "png", file);
                showAlert("Success", "Chart exported successfully as PNG.");
            } catch (IOException e) {
                showAlert("Error", "Failed to export chart as PNG: " + e.getMessage());
            }
        }
    }

    // Export chart as PDF document
    private void exportChartAsPDF(Stage primaryStage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Chart as PDF");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        File file = fileChooser.showSaveDialog(primaryStage);
        if (file != null) {
            WritableImage snapshot = chart.snapshot(new SnapshotParameters(), null);
            BufferedImage bufferedImage = SwingFXUtils.fromFXImage(snapshot, null);
            try (PDDocument doc = new PDDocument()) {
                PDPage page = new PDPage(new PDRectangle(bufferedImage.getWidth(), bufferedImage.getHeight()));
                doc.addPage(page);
                PDImageXObject pdImage = LosslessFactory.createFromImage(doc, bufferedImage);
                try (PDPageContentStream contentStream = new PDPageContentStream(doc, page)) {
                    contentStream.drawImage(pdImage, 0, 0, bufferedImage.getWidth(), bufferedImage.getHeight());
                }
                doc.save(file);
                showAlert("Success", "Chart exported successfully as PDF.");
            } catch (IOException e) {
                showAlert("Error", "Failed to export chart as PDF: " + e.getMessage());
            }
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
        public String getStudentId() { return studentId; }
        public String getDepartment() { return department; }
        public double getFinalScore() { return finalScore; }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
