package tutorial;
import java.util.Scanner;

public class bmi {
    static void main() {
        Scanner scanner = new Scanner(System.in);
        String ms = "bmi";

        System.out.print("키를 입력하시요: ");
        float cm = scanner.nextFloat();

        System.out.print("몸무게를 입력하시오: ");
        float kg = scanner.nextFloat();

        double m = cm / 100.0;

        double bmi = kg / (m*m);
        System.out.printf("%s는 %.2f", ms, bmi);
    }
}
