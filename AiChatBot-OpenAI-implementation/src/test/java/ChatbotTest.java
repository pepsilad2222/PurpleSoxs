import org.junit.jupiter.api.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class ChatbotTest {

    private static final File CHAT_HISTORY_FILE = new File("chat_history.txt");
    private static final File PERSONAL_FAQ_FILE = new File("personal_faq.txt");

    @BeforeEach
    public void cleanFiles() throws IOException {
        Files.write(CHAT_HISTORY_FILE.toPath(), new byte[0]);
        Files.write(PERSONAL_FAQ_FILE.toPath(), new byte[0]);
        Chatbot.questionCount.clear();
    }

    @Test
    public void testSaveQuestionAndFAQTrigger() throws IOException {
        String question = "What classes do I take next semester?";

        // Save the same question 3 times to trigger FAQ logic
        for (int i = 0; i < 3; i++) {
            Chatbot.saveQuestion(question);
        }

        List<String> historyLines = Files.readAllLines(CHAT_HISTORY_FILE.toPath());
        long count = historyLines.stream().filter(line -> line.equals(question)).count();
        assertEquals(3, count, "Question should be recorded 3 times in chat history");

        List<String> faqLines = Files.readAllLines(PERSONAL_FAQ_FILE.toPath());
        assertTrue(faqLines.contains("- " + question), "Question should be added to personal FAQ after 3 saves");
    }

    @Test
    public void testLoadQuestionHistory() throws IOException {
        String question = "How do I check my GPA?";
        Files.write(CHAT_HISTORY_FILE.toPath(), List.of(question, question, question));

        Chatbot.loadQuestionHistory();
        int count = Chatbot.questionCount.getOrDefault(question, 0);

        assertEquals(3, count, "Question count should be 3 after loading history");
    }

    @AfterAll
    public static void cleanup() {
        CHAT_HISTORY_FILE.delete();
        PERSONAL_FAQ_FILE.delete();
    }
}
// This test class is designed to test the functionality of the Chatbot class, specifically focusing on the question history and FAQ trigger logic.
// It includes setup and cleanup methods to ensure a clean state before and after tests, and it verifies that questions are saved correctly and trigger the FAQ logic as expected.