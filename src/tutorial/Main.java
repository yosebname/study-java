package tutorial;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        final String Full = "##########";
        final String EMPTY = "----------";
        final String DIVIDER   = "=".repeat(30);

        String BAR_Full = Full.repeat(2);
        System.out.println(BAR_Full);
        System.out.println(" 독서 진행률 ");
        System.out.println(BAR_Full);

        Scanner sc = new Scanner(System.in);
        System.out.print("책 제목  : ");
        String inputTitle = sc.nextLine();

        System.out.print("총 페이지 : ");
        int inputTotalPages = Integer.parseInt(sc.nextLine().trim());

        System.out.print("읽은 분량 : ");
        int inputCurrentPage = Integer.parseInt(sc.nextLine().trim());

        String trimmed = inputTitle.trim();
        String title = inputTitle.trim().isEmpty() ? "제목 없음" : trimmed;
        int total = inputTotalPages <= 0 ? 1 : inputTotalPages;
        int current = inputCurrentPage < 0 ? 0 : inputCurrentPage > total ? total : inputCurrentPage;

        int remaining = total - current;
        double rate = (double) current / total * 100;

        int filled = (int)(rate/10);
        String bar = "[" + Full.substring(0, filled) + EMPTY.substring(0, 10 - filled) + "]";

        String status = rate >= 100 ? "완독"
                : rate <= 0 ? "시작 전"
                : "읽는 중";
        String comment = rate >= 100 ? "완독 축하합니다!"
                : rate >= 50 ? "절반을 넘어가고 있어요!"
                : rate >= 10 ? "잘 나아가고 있어요" : "이제 시작이네요";

        System.out.printf("남은 분량 : %d%n", remaining);

        System.out.println(DIVIDER);
        System.out.printf(" 책 제목   : %s%n", title);
        System.out.printf(" 총 페이지 : %d p%n", total);
        System.out.printf(" 읽은 분량 : %d p%n", current);
        System.out.printf(" 남은 분량 : %d p%n", remaining);
        System.out.println();
        System.out.printf(" 진행률 : %.1f%%%n", rate);
        System.out.printf(" %s%n", bar);
        System.out.printf(" 상태   : %s%n", status);
        System.out.printf(" %s%n", comment);
        System.out.println(DIVIDER);

        sc.close();
    }
}
