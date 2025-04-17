
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import org.json.JSONObject;
import org.json.JSONArray;


public class Chatbot {

    
    private static OpenAiAssistantEngine assistant;
    private static final String APIKEY = System.getenv("OPENAI_API_KEY");
    private static final File USER_INFO_FILE = new File("user_info.txt");
    private static final File ACU_DATABASE_FILE = new File("acu_database.txt");
    private static final File CHAT_HISTORY_FILE = new File("chat_history.txt");
    private static final File PERSONAL_FAQ_FILE = new File("personal_faq.txt");
    private static String vectorStoreId;
    private static final Map<String, Integer> questionCount = new HashMap<>();


    public static void main(String[] args) {
        loadQuestionHistory(); // ← load saved history before assistant setup

        assistant = new OpenAiAssistantEngine(APIKEY);
        System.out.println("-------------------------");
        System.out.println("Setting up AI Academic Advisor...");

        String assistantId = setupAssistant();
        if (assistantId == null) {
            return;
        }

        startInteractiveChat(assistantId);
    }

    private static void loadQuestionHistory() {
    if (!CHAT_HISTORY_FILE.exists()) return;
    try (BufferedReader reader = new BufferedReader(new FileReader(CHAT_HISTORY_FILE))) {
        String line;
        while ((line = reader.readLine()) != null) {
            questionCount.put(line, questionCount.getOrDefault(line, 0) + 1);
        }
    } catch (IOException e) {
        System.out.println("Failed to read chat history: " + e.getMessage());
    }
    }

    private static void saveQuestion(String question) {
    try (FileWriter writer = new FileWriter(CHAT_HISTORY_FILE, true)) {
        writer.write(question + "\n");
    } catch (IOException e) {
        System.out.println("Failed to save question: " + e.getMessage());
    }

    questionCount.put(question, questionCount.getOrDefault(question, 0) + 1);

    if (questionCount.get(question) == 3) {
        try (FileWriter writer = new FileWriter(PERSONAL_FAQ_FILE, true)) {
            writer.write("- " + question + "\n");
        } catch (IOException e) {
            System.out.println("Failed to update personal FAQ: " + e.getMessage());
        }
    }
}



    private static String setupAssistant() {
        String assistantId = assistant.createAssistant(
            "gpt-3.5-turbo",
            "Personal AI Academic Advisor",
            null,
            "You are a real-time chat AI Academic Advisor for Abilene Christian University. Always address the student by their first and last name using the information provided in the user_info.txt file. Provide clear and accurate information about the student's academic journey, current or upcoming courses, important deadlines, and school-related policies.\n\nAlways respond in clean, plain text. Do not include citations, sources, links, or reference material formatting. Keep your tone friendly, helpful, and professional.",
            null,
            List.of("file_search"),
            null,
            0.5,
            0.5,
            null
        );
        

        if (assistantId == null) {
            System.out.println("Failed to create assistant");
            return null;
        }

        String userInfoFileID = assistant.uploadFile(USER_INFO_FILE, "assistants");
        String acuDatabaseFileID = assistant.uploadFile(ACU_DATABASE_FILE, "assistants");

        if (userInfoFileID == null || acuDatabaseFileID == null) {
            System.out.println("Failed to upload one or more files");
            return null;
        }

        Map<String, String> fileMetadata = new HashMap<>();
        fileMetadata.put(userInfoFileID, "This fileID (user_info.txt) is associated with the user info");
        fileMetadata.put(acuDatabaseFileID, "This fileID (acu_database.txt) is associated with the ACU database");

        vectorStoreId = assistant.createVectorStore(
                "User Files",
                Arrays.asList(userInfoFileID, acuDatabaseFileID),
                null,
                null,
                fileMetadata
        );

        if (vectorStoreId == null) {
            System.out.println("Failed to create vector store");
            return null;
        }

        Map<String, Object> toolResources = new HashMap<>();
        Map<String, List<String>> fileSearch = new HashMap<>();
        fileSearch.put("vector_store_ids", List.of(vectorStoreId));
        toolResources.put("file_search", fileSearch);

        boolean updateSuccess = assistant.modifyAssistant(
                assistantId,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                toolResources,
                null,
                null
        );

        if (!updateSuccess) {
            System.out.println("Failed to update assistant with vector store");
            return null;
        }

        System.out.println("Assistant setup successfully with ID: " + assistantId);
        return assistantId;
    }

    private static void startInteractiveChat(String assistantId) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        String threadId = null;
        String runId = null;
    
        System.out.println("\n=== ACU AI Academic Advisor Chat ===");
        System.out.println("Here are some FAQ's based on different departments in the school:");
        System.out.println("Section 1. Academic Advisor");
        System.out.println("Section 2. Your Personal FAQ");
        System.out.println("Please type the department number you would like to view (e.g., '1'), or type 'skip' to go directly to the chatbot.");
    
        try {
            // === START FAQ SELECTION ===
            while (true) {
                System.out.print("\nSelection: ");
                String selection = reader.readLine().trim().toLowerCase();
            
                if (selection.equals("1") || selection.contains("academic")) {
                    System.out.println("\n--- FAQs for Academic Advisor ---");
                    System.out.println("1. How do I check my current/upcoming registrations?");
                    System.out.println("2. When can I access my upcoming courses in Canvas?");
                    System.out.println("3. When do the next courses start?");
                    System.out.println("4. What if I need to change or drop an upcoming course?");
                    System.out.println("5. When will final grades be posted?");
                    System.out.println("6. What happens if I fail my course(s)?");
                    System.out.println("7. What happens if I’m not financially clear for the next term?");
                    System.out.println("8. How can I reach my Financial Intake Specialist (FIS) about financial concerns?");
                    System.out.println("9. How do I reach out to technical support?");
                    System.out.println("\nPlease enter the question number (1–9), type 'chat' to begin chatting, or 'back' to return to departments.");
            
                    while (true) {
                        System.out.print("FAQ #: ");
                        String faqInput = reader.readLine().trim().toLowerCase();
            
                        switch (faqInput) {
                            case "1":
                                System.out.println("To check on upcoming/current registrations, please navigate to Degree Works and scroll to the bottom. It will show you your current and registered classes for the next term. To access Degree Works, please go to my.acu.edu, and in the search bar, please type in \"Degree Works\".");
                                break;
                            case "2":
                                System.out.println("You can only access your upcoming courses in Canvas when the professor decides to publish them. Most of the time, you will have access a day or two before the start day of the semester.");
                                break;
                            case "3":
                                System.out.println("The next courses start depending on when you registered for them. If you signed up for summer classes they will start during the summer term. If you signed up for classes during the fall or spring term they will start when you get there. You can check the specific day by going to my.acu.edu and searching for \"First day of classes\".");
                                break;
                            case "4":
                                System.out.println("If you need to drop or change a course, you will need to reach out to your academic advisor and ask them to either drop or change a course. If you drop a course within the first week of the start of the semester (Monday-Friday before 5:00 PM) then you will get a full refund for that course and your transcript will be unaffected. If you decide to change a class to another class during the first week, your transcript will also be unaffected.");
                                break;
                            case "5":
                                System.out.println("Final grades will be posted within 1-2 weeks of the semester concluding. If you go to my.acu.edu and in the search bar type banner, you will have access to see your unofficial transcript and see your grades. Once there in the search bar, type in \"transcript\" and you will see. The transcript will only show a letter grade, so if you wish to see your numerical grade then please navigate to Canvas. Once there, please select courses and on the top right select \"view all courses\". Here you can see all the previous courses you took and what numerical value you achieved in them.");
                                break;
                            case "6":
                                System.out.println("If you fail your course(s) they will count as an F on your transcript and carry a weight of 0 on the 4.0 scale. Regardless of it being a major class, university requirement, or an elective, you would not get credit for the class or classes and would have to retake them. ACU's policy is that you have 3 attempts to retake a class. Every time that you retake it whatever grade you make on the end will replace the current one. Also, ACU will keep the highest score automatically. So if you get a B in a class, decide to retake it for an A and get a C instead you will keep the B.");
                                break;
                            case "7":
                                System.out.println("If you are not financially clear for the next term, then you need to call Student Services at 325-674-2300. They will tell you the next steps and ultimately, if you aren't able to pay, you will be removed from the school.");
                                break;
                            case "8":
                                System.out.println("Along with an academic advisor, each student is assigned a financial advisor as well. To reach them, please call Wildcat Central at 325-674-6770 and ask them who it is.");
                                break;
                            case "9":
                                System.out.println("To reach ACU technical support, please call 325-674-5555. They will assist you with whatever technical problems you have.");
                                break;
                            case "chat":
                                System.out.println("\nEntering chatbot mode...");
                                break;
                            case "back":
                                return;
                            default:
                                System.out.println("Please enter a valid number (1–9), 'chat', or 'back'.");
                                continue;
                        }
            
                        if (faqInput.equals("chat")) break;
                        System.out.println("\nYou can type another FAQ number, 'chat' to begin chatting, or 'back' to return.");
                    }
            
                    break; // exit FAQ loop to continue to chatbot
                } 
                else if (selection.equals("2")) {
    System.out.println("\n--- Your Personal FAQ ---");
    List<String> faqList = new ArrayList<>();

    if (!PERSONAL_FAQ_FILE.exists()) {
        System.out.println("You have no personal FAQs yet.");
    } else {
        try (BufferedReader faqReader = new BufferedReader(new FileReader(PERSONAL_FAQ_FILE))) {
            String line;
            int i = 1;
            while ((line = faqReader.readLine()) != null) {
                faqList.add(line.substring(2)); // remove "- " prefix
                System.out.println(i + ". " + line.substring(2));
                i++;
            }
        } catch (IOException e) {
            System.out.println("Failed to load personal FAQ: " + e.getMessage());
        }

        if (!faqList.isEmpty()) {
            while (true) {
                System.out.print("\nType the number of a question you'd like the advisor to answer, or type 'back': ");
                String input = reader.readLine().trim().toLowerCase();
        
                if (input.equals("back")) break;
        
                try {
                    int choice = Integer.parseInt(input);
                    if (choice >= 1 && choice <= faqList.size()) {
                        String selectedQuestion = faqList.get(choice - 1);
                        System.out.println("\nYou asked: " + selectedQuestion);
        
                        // Send to assistant
                        JSONArray vectorStoreArray = new JSONArray();
                        vectorStoreArray.put(vectorStoreId);
        
                        JSONObject fileSearchJson = new JSONObject();
                        fileSearchJson.put("vector_store_ids", vectorStoreArray);
        
                        JSONObject toolResourcesJson = new JSONObject();
                        toolResourcesJson.put("file_search", fileSearchJson);
        
                        // First: create the thread
                        threadId = assistant.createThread(
                            List.of(new JSONObject().put("role", "user").put("content", selectedQuestion)),
                            null,
                            null
                        );
        
                        if (threadId == null) {
                            System.out.println("Failed to create thread for this FAQ.");
                            continue;
                        }
        
                        // Then: create the run
                        runId = assistant.createRun(
                            threadId,
                            assistantId,
                            null, null, null, null,
                            null, null, null,
                            null, null, null,
                            null, null,
                            null, null, null,
                            null, // responseFormat
                            toolResourcesJson
                        );
        
                        if (runId == null) {
                            System.out.println("Failed to create run.");
                            continue;
                        }
        
                        boolean completed = assistant.waitForRunCompletion(threadId, runId, 60, 1000);
        
                        if (!completed) {
                            System.out.println("The assistant encountered an issue. Please try again.");
                            continue;
                        }
        
                        List<String> replies = assistant.listMessages(threadId, runId);
                        if (replies != null && !replies.isEmpty()) {
                            System.out.println("\nAdvisor: " + replies.get(0));
                        } else {
                            System.out.println("Advisor had no response.");
                        }
        
                        assistant.deleteResource("threads", threadId);
                    } else {
                        System.out.println("Invalid number.");
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Please enter a number or 'back'.");
                }
            }
        }
        
    }
}

                else if (selection.equals("skip")) {
                    break; // skip FAQ
                } 
                else {
                    System.out.println("Invalid input. Type '1', '2', or 'skip'.");
                }
            }
            
    
            // === START NORMAL CHAT LOOP ===
            System.out.println("\n=== ACU AI Academic Advisor Chat ===");
            System.out.println("\n=== Ask Away! ===");
            System.out.println("\nType 'exit' to end the conversation.");
    
            String userInput;
            while (true) {
                System.out.print("\nYou: ");
                userInput = reader.readLine().trim();
                saveQuestion(userInput); // ← this logs and updates personal FAQ if needed

                
    
                if (userInput.equalsIgnoreCase("exit")) {
                    break;
                }
    
                if (userInput.isEmpty()) {
                    continue;
                }
    
                if (threadId == null) {
                    List<JSONObject> messages = List.of(
                            new JSONObject()
                                    .put("role", "user")
                                    .put("content", userInput)
                    );
                    threadId = assistant.createThread(messages, null, null);
                    if (threadId == null) {
                        System.out.println("Failed to create thread. Please try again.");
                        continue;
                    }
                } else {
                    String messageId = assistant.addMessageToThread(threadId, userInput);
                    if (messageId == null) {
                        System.out.println("Failed to send message. Please try again.");
                        continue;
                    }
                }
    
                runId = assistant.createRun(
                    threadId,
                    assistantId,
                    null, // model
                    null, // reasoningEffort
                    null, // instructions
                    null, // additionalInstructions
                    null, // additionalMessages
                    null, // tools
                    null, // metadata
                    null, // temperature
                    null, // topP
                    null, // stream
                    null, // maxPromptTokens
                    null, // maxCompletionTokens
                    null, // truncationStrategy
                    null, // toolChoice
                    null, // parallelToolCalls
                    null, // responseFormat
                    null  // toolResources (✅ this is the one that was missing)
                );
                
    
                if (runId == null) {
                    System.out.println("Failed to create run. Please try again.");
                    continue;
                }
    
                boolean completed = assistant.waitForRunCompletion(threadId, runId, 60, 1000);
    
                if (!completed) {
                    System.out.println("The assistant encountered an issue. Please try again.");
                    continue;
                }
    
                List<String> retrievedMessages = assistant.listMessages(threadId, runId);
                if (retrievedMessages != null && !retrievedMessages.isEmpty()) {
                    System.out.println("\nAdvisor: " + retrievedMessages.get(0));
                } else {
                    System.out.println("No response received. Please try again.");
                }
            }
    
            if (threadId != null) {
                assistant.deleteResource("threads", threadId);
            }
    
        } catch (IOException e) {
            System.out.println("Error reading input: " + e.getMessage());
        }
    }    
}
