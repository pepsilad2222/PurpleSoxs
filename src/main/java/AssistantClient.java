import java.util.ArrayList;
import java.util.List;

public class AssistantClient {

    private final String apiKey;
    private String currentAssistantId;
    private String currentThreadId;

    public AssistantClient(String apiKey) {
        this.apiKey = apiKey;
    }

    public void createAssistant(String name, String instructions) {
        // Simulate creating an assistant (would call OpenAI API in reality)
        this.currentAssistantId = "asst-" + System.currentTimeMillis();
    }

    public void deleteAssistant() {
        // Simulate deleting the assistant
        this.currentAssistantId = null;
    }

    public void startConversation() {
        if (this.currentAssistantId == null) {
            throw new IllegalStateException("Assistant must be created first.");
        }
        this.currentThreadId = "thread-" + System.currentTimeMillis();
    }

    public void endConversation() {
        this.currentThreadId = null;
    }

    public List<String> sendMessageAndWait(String message) {
        if (this.currentThreadId == null) {
            throw new IllegalStateException("Conversation not started.");
        }
        // Simulate response
        List<String> responses = new ArrayList<>();
        responses.add("This is a response to: " + message);
        return responses;
    }

    public String getCurrentAssistantId() {
        return this.currentAssistantId;
    }

    public String getCurrentThreadId() {
        return this.currentThreadId;
    }
}
