import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.sql.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
    private static final String JDBC_URL = "jdbc:mysql://localhost:3306/coursera";
    private static final String JDBC_USERNAME = "root";
    private static final String JDBC_PASSWORD = "root";

    private static final String QUERY = "SELECT s.pin, CONCAT(s.first_name, ' ', s.last_name) AS st_name " +
            "c.name, c.total_time, c.credit, " +
            "CONCAT(i.first_name, ' ', i.last_name) AS instructor " +
            "FROM students s " +
            "JOIN student_courses_xrf sc ON s.pin = sc.student_pin " +
            "JOIN courses c ON sc.course_id = c.id " +
            "JOIN instructors i ON c.instructor_id = i.id " +
            "WHERE (sc.completion_date IS NOT NULL AND sc.completion_date BETWEEN ? AND ?)" +
            "AND c.credit >= ?";


    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        String[] input = new String[5];
        int index = 0;
        int minCredit;
        // Find the minCredit variable (only integer in input)
        if (scanner.hasNextInt()) {
            minCredit = scanner.nextInt();
        } else {
            System.out.println("You did not specify a minimum amount for the credit [required]");
            return;
        }
        // Read input
        while (index < 5 && scanner.hasNext()) {
            input[index] = scanner.nextLine();
            index++;
        }
        scanner.close();

        List<String> includedStudents = null;
        String outputFormat = null;
        String directoryPath = null;
        // Datetime pattern in the 2004-05-23T14:25:10 format
        String pattern = "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}";

        Pattern r = Pattern.compile(pattern);
        String startDateTime = null;
        String endDateTime = null;

        // Distribute input under variables
        // Tried to make the input a way, where it does not matter which line of input comes after which
        for (String line : input) {
            Matcher m = r.matcher(line);
            if (line.contains(",")) {
                includedStudents = new ArrayList<>(Arrays.asList(line.split(",")));
            } else if (line.equals("csv") || line.equals("html")) {
                outputFormat = line;
            } else if (line.equals(String.valueOf(minCredit))) {
                continue;
            }
            else if (m.find()) {
                if (startDateTime == null) {
                    startDateTime = line;
                } else {
                    endDateTime = line;
                }
            }
            else {
                directoryPath = line;
            }
        }

        if (startDateTime == null || endDateTime == null) {
            System.out.println("You did not specify start or end DateTime for the query");
            return;
        }

        // Use default directory path if not provided
        if (directoryPath == null) {
            directoryPath = ".";
        }

        // Call methods based on what file should be created
        if (outputFormat != null && outputFormat.equals("csv")) {
            saveToCSV(includedStudents, minCredit, startDateTime, endDateTime,
                    directoryPath + File.separator + "report.csv");
        }
        else if (outputFormat != null && outputFormat.equals("html")) {
            saveToHTML(includedStudents, minCredit, startDateTime, endDateTime,
                    directoryPath + File.separator + "report.html");
        }
        else {
            saveToCSV(includedStudents, minCredit, startDateTime, endDateTime,
                    directoryPath + File.separator + "report.csv");
            saveToHTML(includedStudents, minCredit, startDateTime, endDateTime,
                    directoryPath + File.separator + "report.html");
        }
    }

    protected static Connection getConnection() {
        Connection connection = null;
        try {
            Class.forName("com.mysql.jdbc.Driver");
            connection = DriverManager.getConnection(JDBC_URL, JDBC_USERNAME, JDBC_PASSWORD);
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return connection;
    }

    protected static void saveToCSV(List<String> includedStudents, int minCredit, String startDateTime,
                                    String endDateTime, String filePath) {
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(QUERY)) {
            statement.setString(1, startDateTime);
            statement.setString(2, endDateTime);
            statement.setInt(3, minCredit);

            ResultSet resultSet = statement.executeQuery();

            try (PrintWriter writer = new PrintWriter(filePath)) {
                List<String> studentNames = new ArrayList<>();
                while (resultSet.next()) {
                    String studentPin = resultSet.getString("pin");
                    // Check if the student is included in the input if a set of pins was specified
                    if (includedStudents != null && !includedStudents.contains(studentPin)) {
                        continue;
                    }
                    String studentName = resultSet.getString("st_name");
                    String courseName = resultSet.getString("course_name");
                    int totalTime = resultSet.getInt("total_time");
                    int credit = resultSet.getInt("credit");
                    String instructor = resultSet.getString("instructor");

                    if (!studentNames.contains(studentName)) {
                        writer.println(studentName + "," + credit);
                        studentNames.add(studentName);
                    }
                    writer.println("\t" + courseName + "," + totalTime + "," + credit + "," + instructor);
                }
            }
        } catch (SQLException | FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    protected static void saveToHTML(List<String> includedStudents, int minCredit, String startDateTime,
                                     String endDateTime, String filePath) {
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(QUERY)) {
            statement.setString(1, startDateTime);
            statement.setString(2, endDateTime);
            statement.setInt(3, minCredit);

            ResultSet resultSet = statement.executeQuery();

            // Writing to HTML
            try (PrintWriter writer = new PrintWriter(filePath)) {
                writer.println("<!DOCTYPE html>");
                writer.println("<html>");
                writer.println("<head>");
                writer.println("<title>Course Data</title>");
                writer.println("</head>");
                writer.println("<body>");

                writer.println("<table border='1'>");
                writer.println("<tr><th>Student Name</th><th>Course Name</th><th>Total Time</th><th>Credit</th>" +
                        "<th>Instructor</th></tr>");

                List<String> studentNames = new ArrayList<>();
                while (resultSet.next()) {
                    String studentPin = resultSet.getString("pin");
                    // Check if the student is included in the input if a set of pins was specified
                    if (includedStudents != null && !includedStudents.contains(studentPin)) {
                        continue;
                    }
                    String studentName = resultSet.getString("st_name");
                    String courseName = resultSet.getString("course_name");
                    int totalTime = resultSet.getInt("total_time");
                    int credit = resultSet.getInt("credit");
                    String instructor = resultSet.getString("instructor");

                    if (!studentNames.contains(studentName)) {
                        writer.println("<tr><td rowspan='1'>" + studentName + "</td><td>" + courseName + "</td><td>" + totalTime
                                + "</td><td>" + credit + "</td><td>" + instructor + "</td></tr>");
                        studentNames.add(studentName);
                    } else {
                        writer.println("<tr><td>" + courseName + "</td><td>" + totalTime
                                + "</td><td>" + credit + "</td><td>" + instructor + "</td></tr>");
                    }
                }
                writer.println("</table>");
                writer.println("</body>");
                writer.println("</html>");
            }
        } catch (SQLException | FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}