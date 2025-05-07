import java.io.BufferedReader;
import java.io.IOException;

public class TextEngine {

    private static int delay = 30; // Default speed in milliseconds

    public static void clearScreen() {
        try {
            if (System.getProperty("os.name").contains("Windows")) {
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } else {
                System.out.print("\033[H\033[2J");
                System.out.flush();
            }
        } catch (IOException | InterruptedException e) {
            System.out.println("Failed to clear screen: " + e.getMessage());
        }
    }

    public static void printWithDelay(String message, boolean newLine) {
        for (char c : message.toCharArray()) {
            System.out.print(c);
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        if (newLine) {
            System.out.println();
        } else {
            System.out.flush(); // ensures it prints immediately
        }
    }

    public static void setDelay(int newDelay) {
        delay = Math.max(0, newDelay);
    }

    public static int getDelay() {
        return delay;
    }

    public static void openSettings(BufferedReader reader) {
        try {
            printWithDelay("Enter new text speed in milliseconds (current: " + getDelay() + "): ", false );
            String input = reader.readLine().trim();
            int newDelay = Integer.parseInt(input);
            setDelay(newDelay);
            printWithDelay("Text speed updated to " + newDelay + " ms.", true);
        } catch (Exception e) {
            printWithDelay("Invalid input. Returning to previous activity.", true);
        }
    }
    
    public static void printRainbowText(String text) {
        String redColor = "\033[1;31m";
        String yellowColor = "\033[1;33m";
        String greenColor = "\033[1;32m";
        String cyanColor = "\033[1;36m";
        String blueColor = "\033[1;34m";
        String purpleColor = "\033[35m";
        String resetColor = "\033[0m";
        String[] colors = {redColor, yellowColor, greenColor, cyanColor, blueColor, purpleColor};

        for (int i = 0; i < text.length(); i++) {
            System.out.print(colors[i % colors.length] + text.charAt(i));
            System.out.flush();
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        System.out.println(resetColor); // Reset to default color
    }
}
