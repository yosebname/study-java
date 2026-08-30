package tutorial;
import java.util.Scanner;

public class exchange {
    static void main() {
        Scanner sc = new Scanner(System.in);
        System.out.print("환전할 금액을 입력하시오: ");
        int money = sc.nextInt();

        Scanner sc1 = new Scanner(System.in);
        float start = sc1.nextFloat();

        Scanner sc2 = new Scanner(System.in);
        float target = sc2.nextFloat();

        float exchange = money / start * target;

        System.out.printf("환율은 %.1f", exchange);
    }
}
