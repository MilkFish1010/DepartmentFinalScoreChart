package DepartmentFinalScoreChart;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StudentDataProcessor {
    
    private List<Student> students;
    
    public StudentDataProcessor() {
        students = new ArrayList<>();
    }
    
    public void loadData(String filePath) throws IOException {
        students.clear();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            boolean isHeader = true;
            
            while ((line = reader.readLine()) != null) {
                if (isHeader) {
                    isHeader = false;
                    continue; // Skip the header row
                }
                
                try {
                    Student student = parseStudent(line);
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
    
    private Student parseStudent(String line) {
        // Split the CSV line, handling potential commas within quoted fields
        String[] parts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
        
        if (parts.length < 8) {
            System.err.println("Invalid line format: " + line);
            return null;
        }
        
        try {
            String studentId = parts[0].trim();
            String department = parts[6].trim();
            
            // Parse the final score (index 8)
            double finalScore = 0.0;
            if (parts.length > 8 && !parts[8].trim().isEmpty()) {
                try {
                    finalScore = Double.parseDouble(parts[8].trim());
                } catch (NumberFormatException e) {
                    System.err.println("Invalid final score: " + parts[8]);
                    // Use 0 as default
                }
            }
            
            return new Student(studentId, department, finalScore);
        } catch (Exception e) {
            System.err.println("Error parsing student data: " + e.getMessage());
            return null;
        }
    }
    
    public Map<String, Double> calculateDepartmentAverages() {
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
    
    public List<Student> getStudents() {
        return students;
    }
}
