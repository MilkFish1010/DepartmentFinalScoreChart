package DepartmentFinalScoreChart;
public class Student {
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
