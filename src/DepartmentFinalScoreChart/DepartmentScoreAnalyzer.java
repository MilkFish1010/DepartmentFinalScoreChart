package DepartmentFinalScoreChart;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
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
import javafx.util.Duration;
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

    // Data structures for student records and computed averages
    private List<Student> students = new ArrayList<>();
    private Map<String, Double> departmentAverages;
    private VBox legendBox; // Custom legend container
    private BorderPane appPane; // Main app layout pane
    private BarChart chart;   // Currently displayed chart
    private boolean isVerticalChart = true; // Toggle flag for chart orientation

    // Define department colors
    private static final String CS_COLOR = "#ADD8E6"; // Dark Blue
    private static final String MATHEMATICS_COLOR = "#FF0000"; // Red
    private static final String ENGINEERING_COLOR = "#008000"; // Green
    private static final String BUSINESS_COLOR = "#FFD700"; // Yellow/Gold

    // Panes for switching between home screen and app screen
    private StackPane rootPane;
    private VBox homePane;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Department Average Final Score Comparison");

        // Create the root StackPane and initialize home and app panes
        rootPane = new StackPane();
        createHomePane();
        createAppPane(primaryStage);

        // Show home screen initially
        rootPane.getChildren().addAll(appPane, homePane);
        appPane.setVisible(false);
        homePane.setVisible(true);

        Scene scene = new Scene(rootPane, 1000, 700);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    // Creates the home screen pane with a title, author info, and a start button.
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
        startButton.setStyle("-fx-background-color: #4a2c2a; -fx-text-fill: white; -fx-background-radius: 30;");
        
        // Scale animation for visual feedback
        startButton.setOnAction(e -> {
            ScaleTransition scale = new ScaleTransition(Duration.millis(150), startButton);
            scale.setFromX(1.0);
            scale.setFromY(1.0);
            scale.setToX(1.1);
            scale.setToY(1.1);
            scale.setAutoReverse(true);
            scale.setCycleCount(2);
            scale.play();

            // Transition from homePane to appPane using fade effects
            FadeTransition fadeOut = new FadeTransition(Duration.millis(500), homePane);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            fadeOut.setOnFinished(ev -> {
                homePane.setVisible(false);
                appPane.setOpacity(0.0);
                appPane.setVisible(true);
                FadeTransition fadeIn = new FadeTransition(Duration.millis(500), appPane);
                fadeIn.setFromValue(0.0);
                fadeIn.setToValue(1.0);
                fadeIn.play();
            });
            fadeOut.play();
        });

        homePane.getChildren().addAll(title, author, description, startButton);
    }

    // Creates the main application pane containing controls, the chart, and the legend.
    private void createAppPane(Stage primaryStage) {
        appPane = new BorderPane();
        appPane.setStyle("-fx-background-color: linear-gradient(from 0% 0% to 100% 100%, #ffd89b, #ff9d8e); " +
                         "-fx-font-family: 'Comic Neue', cursive;");

        // Legend container on the right side
        legendBox = new VBox(10);
        legendBox.setPadding(new Insets(15));
        legendBox.setStyle("-fx-border-color: gray; -fx-border-width: 1; " +
                           "-fx-background-color: rgba(255,255,255,0.9);");
        appPane.setRight(legendBox);

        // Create control buttons with animations
        Button loadButton = createAnimatedButton("📂 Load CSV File");
        Button exportCSVButton = createAnimatedButton("💾 Export CSV");
        exportCSVButton.setDisable(true);
        Button exportPNGButton = createAnimatedButton("🖼️ Export PNG");
        exportPNGButton.setDisable(true);
        Button exportPDFButton = createAnimatedButton("📄 Export PDF");
        exportPDFButton.setDisable(true);

        // Labels for file status and summary information
        final Label fileLabel = new Label("No file loaded yet.");
        fileLabel.setFont(Font.font("Comic Neue", 14));
        fileLabel.setStyle("-fx-text-fill: #4a2c2a;");
        final Label summaryLabel = new Label("Load a CSV file to see results");
        summaryLabel.setFont(Font.font("Comic Neue", 14));
        summaryLabel.setStyle("-fx-text-fill: #4a2c2a;");

        // Reset button clears data and transitions back to the home screen
        Button resetButton = createAnimatedButton("🔄 Reset");
        resetButton.setOnAction(e -> {
            students.clear();
            departmentAverages = null;
            chart = null;
            legendBox.getChildren().clear();
            appPane.setCenter(null);
            exportCSVButton.setDisable(true);
            exportPNGButton.setDisable(true);
            exportPDFButton.setDisable(true);
            fileLabel.setText("No file loaded yet.");
            summaryLabel.setText("Load a CSV file to see results");
            FadeTransition fadeOut = new FadeTransition(Duration.millis(500), appPane);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            fadeOut.setOnFinished(ev -> {
                appPane.setVisible(false);
                homePane.setOpacity(0.0);
                homePane.setVisible(true);
                FadeTransition fadeIn = new FadeTransition(Duration.millis(500), homePane);
                fadeIn.setFromValue(0.0);
                fadeIn.setToValue(1.0);
                fadeIn.play();
            });
            fadeOut.play();
        });

        // Button to toggle between vertical and horizontal chart orientations
        Button toggleOrientationButton = createAnimatedButton("↕️ Toggle Orientation");
        toggleOrientationButton.setDisable(true);
        toggleOrientationButton.setOnAction(e -> {
            isVerticalChart = !isVerticalChart;
            chart = createChart(isVerticalChart, departmentAverages);
            appPane.setCenter(chart);
        });

        VBox bottomBox = new VBox(15);
        bottomBox.setPadding(new Insets(15));
        bottomBox.setAlignment(Pos.CENTER);
        bottomBox.getChildren().add(loadButton);
        HBox exportButtonBox = new HBox(20, exportCSVButton, exportPNGButton, exportPDFButton);
        exportButtonBox.setAlignment(Pos.CENTER);
        bottomBox.getChildren().addAll(exportButtonBox, fileLabel, summaryLabel, resetButton);
        appPane.setBottom(bottomBox);

        VBox leftBox = new VBox(toggleOrientationButton);
        leftBox.setPadding(new Insets(15));
        leftBox.setAlignment(Pos.CENTER);
        appPane.setLeft(leftBox);

        // Load CSV action - opens a file chooser and processes the file
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
                    fileLabel.setText("Loaded file: " + file.getName());
                    exportCSVButton.setDisable(false);
                    exportPNGButton.setDisable(false);
                    exportPDFButton.setDisable(false);
                    toggleOrientationButton.setDisable(false);
                } catch (IOException ex) {
                    // Alert the user with a descriptive message if CSV loading fails
                    showAlert("Error", "Error loading CSV file: " + ex.getMessage());
                }
            }
        });

        // Export CSV action
        exportCSVButton.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save CSV File");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
            File file = fileChooser.showSaveDialog(primaryStage);
            if (file != null) {
                exportData(file, departmentAverages);
            }
        });

        // Export PNG and PDF actions
        exportPNGButton.setOnAction(e -> exportChartAsPNG(primaryStage));
        exportPDFButton.setOnAction(e -> exportChartAsPDF(primaryStage));
    }

    // Creates an animated button with a scale effect on click
    private Button createAnimatedButton(String text) {
        Button btn = new Button(text);
        btn.setFont(Font.font("Comic Neue", 12));
        btn.setPrefWidth(200);
        btn.setPrefHeight(40);
        btn.setStyle("-fx-background-color: #4a2c2a; -fx-text-fill: white; " +
                     "-fx-padding: 15 30 15 30; -fx-font-size: 1.2em; -fx-background-radius: 30;");
        btn.setOnAction(e -> {
            ScaleTransition scale = new ScaleTransition(Duration.millis(150), btn);
            scale.setFromX(1.0);
            scale.setFromY(1.0);
            scale.setToX(1.1);
            scale.setToY(1.1);
            scale.setAutoReverse(true);
            scale.setCycleCount(2);
            scale.play();
        });
        return btn;
    }

    // Creates and returns a BarChart with fade-in animations for both the chart and its bars.
    private BarChart createChart(boolean isVertical, Map<String, Double> averages) {
        BarChart chart;
        double fixedBarWidth = 80;
        double fixedBarHeight = 80;
        double categoryGap = 50;
        int count = averages.size();

        if (isVertical) {
            // For vertical charts, if only one department exists, use a natural width; otherwise, calculate normally.
            double computedWidth = (count == 1) 
                ? fixedBarWidth + 2 * categoryGap  // e.g. 80 + 100 = 180 pixels
                : count * fixedBarWidth + (count + 1) * categoryGap;
            
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

            FadeTransition ft = new FadeTransition(Duration.millis(800), chart);
            ft.setFromValue(0.0);
            ft.setToValue(1.0);
            ft.play();

            // Run styling and animations on the JavaFX thread
            Platform.runLater(() -> {
                chart.lookupAll(".axis-line").forEach(n -> n.setStyle("-fx-stroke: black;"));
                chart.lookupAll(".chart-horizontal-grid-lines").forEach(n -> n.setStyle("-fx-stroke: black;"));
                chart.lookupAll(".chart-vertical-grid-lines").forEach(n -> n.setStyle("-fx-stroke: black;"));
                chart.lookupAll(".axis-label").forEach(n -> n.setStyle("-fx-text-fill: black; -fx-font-size: 16px;"));
                chart.lookupAll(".tick-label").forEach(n -> n.setStyle("-fx-text-fill: black; -fx-font-size: 16px;"));

                // Animate each bar and add data labels with modifications for vertical charts
                for (XYChart.Data<String, Number> data : series.getData()) {
                    String color = getDepartmentColor(data.getXValue());
                    if (data.getNode() != null) {
                        data.getNode().setStyle("-fx-bar-fill: " + color + ";");
                        FadeTransition barFade = new FadeTransition(Duration.millis(600), data.getNode());
                        barFade.setFromValue(0.0);
                        barFade.setToValue(1.0);
                        barFade.play();
                        if (data.getNode() instanceof StackPane) {
                            StackPane stackPane = (StackPane) data.getNode();
                            // Use a smaller font for vertical labels
                            Label label = new Label(String.format("%.2f", data.getYValue()));
                            label.setFont(Font.font("Comic Neue", FontWeight.BOLD, 12));
                            label.setTextFill(Color.BLACK);
                            // Position the label at the bottom center of the bar
                            stackPane.getChildren().add(label);
                            StackPane.setAlignment(label, Pos.BOTTOM_CENTER);
                        }
                    }
                }
            });
            chart.setMinWidth(computedWidth);
            chart.setPrefWidth(computedWidth);
            chart.setMaxWidth(computedWidth);
        } else {
            // For horizontal charts, if only one department exists, increase the height for better visibility.
            double computedHeight = (count == 1)
                ? fixedBarHeight + 4 * categoryGap  // e.g. 80 + 200 = 280 pixels
                : count * fixedBarHeight + (count + 1) * categoryGap;
            
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

            FadeTransition ft = new FadeTransition(Duration.millis(800), chart);
            ft.setFromValue(0.0);
            ft.setToValue(1.0);
            ft.play();

            Platform.runLater(() -> {
                chart.lookupAll(".axis-line").forEach(n -> n.setStyle("-fx-stroke: black;"));
                chart.lookupAll(".chart-horizontal-grid-lines").forEach(n -> n.setStyle("-fx-stroke: black;"));
                chart.lookupAll(".chart-vertical-grid-lines").forEach(n -> n.setStyle("-fx-stroke: black;"));
                chart.lookupAll(".axis-label").forEach(n -> n.setStyle("-fx-text-fill: black; -fx-font-size: 16px;"));
                chart.lookupAll(".tick-label").forEach(n -> n.setStyle("-fx-text-fill: black; -fx-font-size: 16px;"));

                // Animate each bar and add data labels with padding to prevent clipping
                for (XYChart.Data<Number, String> data : series.getData()) {
                    String color = getDepartmentColor(data.getYValue());
                    if (data.getNode() != null) {
                        data.getNode().setStyle("-fx-bar-fill: " + color + ";");
                        FadeTransition barFade = new FadeTransition(Duration.millis(600), data.getNode());
                        barFade.setFromValue(0.0);
                        barFade.setToValue(1.0);
                        barFade.play();
                        if (data.getNode() instanceof StackPane) {
                            StackPane stackPane = (StackPane) data.getNode();
                            Label label = new Label(String.format("%.2f", data.getXValue()));
                            label.setFont(Font.font("Comic Neue", FontWeight.BOLD, 16));
                            label.setTextFill(Color.BLACK);
                            label.setPadding(new Insets(0, 5, 0, 5));
                            stackPane.getChildren().add(label);
                            StackPane.setAlignment(label, Pos.CENTER);
                        }
                    }
                }
            });
            chart.setMinHeight(computedHeight);
            chart.setPrefHeight(computedHeight);
            chart.setMaxHeight(computedHeight);
        }

        // Set common chart properties and style the title
        chart.setTitle("Average Final Scores by Department");
        Platform.runLater(() -> {
            chart.lookupAll(".chart-title").forEach(n -> n.setStyle("-fx-text-fill: black; -fx-font-size: 20px;"));
        });
        chart.setAnimated(false);
        chart.setLegendVisible(false);
        chart.setCategoryGap(categoryGap);
        chart.setBarGap(0);
        BorderPane.setMargin(chart, new Insets(20));
        return chart;
    }

    // Load and parse the CSV file line by line
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
                        throw new IOException("CSV header missing 'Final_Score' or 'Department' columns.");
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
                    // Log parsing error; consider alerting if many errors occur
                    System.err.println("Error parsing line: " + line);
                    System.err.println("Error message: " + e.getMessage());
                }
            }
        }
        System.out.println("Loaded " + students.size() + " students.");
    }

    // Parse a CSV line into a Student object. Uses regex to handle commas in quotes.
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

    // Calculate department averages by grouping student scores
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

    // Update the legend panel with colored boxes for each department
    private void updateLegend(Map<String, Double> departmentAverages) {
        legendBox.getChildren().clear();
        Label legendTitle = new Label("Legend");
        legendTitle.setFont(Font.font("Comic Neue", FontWeight.BOLD, 14));
        legendTitle.setStyle("-fx-text-fill: black;");
        legendBox.getChildren().add(legendTitle);
        for (String department : departmentAverages.keySet()) {
            HBox legendItem = new HBox(10);
            Region colorBox = new Region();
            colorBox.setPrefSize(15, 15);
            colorBox.setStyle("-fx-background-color: " + getDepartmentColor(department) + "; -fx-border-color: black;");
            Label deptLabel = new Label(department);
            deptLabel.setFont(Font.font("Comic Neue", 12));
            deptLabel.setStyle("-fx-text-fill: black;");
            legendItem.getChildren().addAll(colorBox, deptLabel);
            FadeTransition ft = new FadeTransition(Duration.millis(500), legendItem);
            ft.setFromValue(0.0);
            ft.setToValue(1.0);
            ft.play();
            legendBox.getChildren().add(legendItem);
        }
    }

    // Returns the color code for a given department
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

    // Updates the summary label with department averages and animates the fade-in
    private void updateSummary(Label summaryLabel, Map<String, Double> departmentAverages) {
        StringBuilder summary = new StringBuilder("Summary of Average Final Scores by Department:\n\n");
        for (Map.Entry<String, Double> entry : departmentAverages.entrySet()) {
            summary.append(String.format("%s: %.2f\n", entry.getKey(), entry.getValue()));
        }
        summaryLabel.setText(summary.toString());
        FadeTransition ft = new FadeTransition(Duration.millis(500), summaryLabel);
        ft.setFromValue(0.0);
        ft.setToValue(1.0);
        ft.play();
    }

    // Exports the department averages as a CSV file.
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

    // Exports the chart as a PNG image.
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

    // Exports the chart as a PDF document.
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

    // Displays an alert dialog with a title and message.
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Inner class representing a student record.
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
