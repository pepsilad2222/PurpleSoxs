
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
    private static String assistantId;

    public static final Map<String, Integer> questionCount = new HashMap<>();


    public static void main(String[] args) {
// Load question history
loadQuestionHistory();

assistant = new OpenAiAssistantEngine(APIKEY);
System.out.println("-------------------------");
System.out.println("Setting up AI Academic Advisor...");

// Try to load assistantId from file
if (new File("assistant_id.txt").exists()) {
    try (BufferedReader reader = new BufferedReader(new FileReader("assistant_id.txt"))) {
        assistantId = reader.readLine();
        System.out.println("Loaded assistant from file: " + assistantId);
    } catch (IOException e) {
        System.out.println("Failed to load assistant ID: " + e.getMessage());
    }
}

// Try to load vectorStoreId from file
if (new File("vector_store_id.txt").exists()) {
    try (BufferedReader reader = new BufferedReader(new FileReader("vector_store_id.txt"))) {
        vectorStoreId = reader.readLine();
        System.out.println("Loaded vector store from file: " + vectorStoreId);
    } catch (IOException e) {
        System.out.println("Failed to load vector store ID: " + e.getMessage());
    }
}

// Only create a new assistant/vector store if needed
if (assistantId == null || vectorStoreId == null) {
    assistantId = setupAssistant();
    if (assistantId == null) return;
}

startInteractiveChat(assistantId);
}

    public static void loadQuestionHistory() {
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

    public static void saveQuestion(String question) {
        // Ignore non-question commands
        if (question.equalsIgnoreCase("exit") || question.equalsIgnoreCase("reset") || question.equalsIgnoreCase("back") || question.trim().isEmpty()) {
            return;
        }
    
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
            "You are a real-time AI Academic Advisor for Abilene Christian University. Always use the student’s data from user_info.txt when responding. Use 'First Name' and 'Last Name' to address the student personally. Use 'Student ID' if identity confirmation is needed. Use 'Major' and 'Minor' to answer questions about their degree. Use 'GPA' to comment on academic performance. Use 'Academic Standing' to clarify eligibility. Use 'Credits Completed' and 'Credits Remaining' to explain progress. Use 'Current Semester' and 'Enrolled Courses' to list classes or give scheduling help. Use 'Advisor' to direct them to their planning contact. Use 'Email' if communication is needed. Use 'Graduation Date' to respond to planning or timeline questions. Only use the exact data in user_info.txt. Avoid generic answers or assumptions. Always reply in clear, helpful plain text with no links, citations, or formatting artifacts."
            + "If the user asks about a specific course, use the ACU database in acu_database.txt to provide accurate information. If the user asks about a specific professor, use the ACU database in acu_database.txt to provide accurate information. If the user asks about a specific class, use the ACU database in acu_database.txt to provide accurate information. If the user asks about a specific department, use the ACU database in acu_database.txt to provide accurate information. If the question asked is, What major classes do I need to take to graduate?, Refer to all_majorclasses.txt for the output.",
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

        try (FileWriter fw = new FileWriter("assistant_id.txt")) {
            fw.write(assistantId);
        } catch (IOException e) {
            System.out.println("Failed to save assistant ID: " + e.getMessage());
        }
        
        try (FileWriter fw = new FileWriter("vector_store_id.txt")) {
            fw.write(vectorStoreId);
        } catch (IOException e) {
            System.out.println("Failed to save vector store ID: " + e.getMessage());
        }
        
        return assistantId;
        
    }

    private static void startInteractiveChat(String assistantId) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        String threadId = null;
        String runId = null;
    
        System.out.println("\n=== ACU AI Academic Advisor Chat ===");
        System.out.println("Here are some FAQ's based on different departments in the school:");
        System.out.println("Section 1. Student Sucess FAQ");
        System.out.println("Section 2. Your Personal FAQ");
        System.out.println("Section 3. Academic FAQ");
        System.out.println("Section 4. ACU IT FAQ");
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
                    System.out.println("\nPlease enter the question number (1 through 9), type 'chat' to begin chatting, or 'back' to return to departments.");
            
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
                        System.out.println("\nYou can type another FAQ number, 'chat' to begin chatting, or 'exit' to quit the chatbot.");
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
                
                                        // Create toolResources JSON object
                                        JSONArray vectorStoreArray = new JSONArray();
                                        vectorStoreArray.put(vectorStoreId);
                
                                        JSONObject fileSearchJson = new JSONObject();
                                        fileSearchJson.put("vector_store_ids", vectorStoreArray);
                
                                        JSONObject toolResourcesJson = new JSONObject();
                                        toolResourcesJson.put("file_search", fileSearchJson);
                
                                        // Prepare initial message
                                        List<JSONObject> messages = List.of(
                                            new JSONObject().put("role", "user").put("content", selectedQuestion)
                                        );
                
                                        // Create thread with no tool resources (API doesn’t support it here)
                                        threadId = assistant.createThread(messages, null, null);
                
                                        if (threadId == null) {
                                            System.out.println("Failed to create thread for this FAQ.");
                                            continue;
                                        }
                
                                        // Create run with toolResourcesJson
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
                                            toolResourcesJson // ✅ 19th argument!
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
                
else if (selection.equals("3")) {
    System.out.println("\n--- FAQs for Academic Advisor (On-Campus) ---");
    System.out.println("1. How do I calculate my GPA?");
    System.out.println("2. What will my GPA be next semester if I make these certain grades?");
    System.out.println("3. What are the prerequisites for this class?");
    System.out.println("4. What classes should I take next semester?");
    System.out.println("5. What are all the requirements for my major?");
    System.out.println("6. What Summer classes are good options for me to take at another school and transfer in?");
    System.out.println("\nPlease enter the question number (1 through 6), type 'chat' to begin chatting, or 'back' to return to departments.");

    while (true) {
        System.out.print("FAQ #: ");
        String academicFaq = reader.readLine().trim().toLowerCase();

        switch (academicFaq) {
            case "1":
                System.out.println("You can calculate your GPA by adding up all the grade points you've earned and dividing by the total number of credit hours. Check with Degree Works for your current GPA data.");
                break;
            case "2":
                System.out.println("To estimate your GPA for next semester, assume letter grades for each course and apply ACU's grade point scale to see how it impacts your cumulative average.");
                break;
            case "3":
                System.out.println("Prerequisites vary by course. You can look up a specific course in the ACU course catalog to see what prerequisites are listed.");
                break;
            case "4":
                System.out.println("Course selection depends on your degree audit and what requirements are still pending. Your advisor can help pick classes that align with your graduation timeline.");
                break;
            case "5":
                System.out.println("All major requirements are listed in your Degree Works audit. It will show you completed, in-progress, and remaining requirements.");
                break;
            case "6":
                System.out.println("General Education and elective classes are good options for summer transfer. Always confirm with your advisor before registering at another school.");
                break;
            case "chat":
                System.out.println("\nEntering chatbot mode...");
                break;
            case "back":
                return;
            default:
                System.out.println("Please enter a valid number (1 through 6), 'chat', or 'back'.");
                continue;
        }

        if (academicFaq.equals("chat")) break;
        System.out.println("\nYou can type another FAQ number, 'chat' to begin chatting, or 'back' to return.");
    }
}
                else if (selection.equals("4")) {
                    System.out.println("\n--- FAQs for ACU IT ---");
                    System.out.println("1. How do I reset my password?");
                    System.out.println("2. How do I install lockdown browser?");
                    System.out.println("3. How do I set up my Wi-Fi?");
                    System.out.println("4. How do I print?");
                    System.out.println("5. How to register a non computer/laptop devide to the network?");
                    System.out.println("\nPlease enter the question number (1–5), type 'chat' to begin chatting, or 'back' to return to departments.");

                    while (true) {
                        System.out.print("FAQ #: ");
                        String itFaq = reader.readLine().trim().toLowerCase();

                        switch (itFaq) {
                            case "1":
                                System.out.println("To reset your password, go to acu.edu/password. Then log in with your ACU credentials to which it will prompt you to change your password. If you are having trouble, please call ACU IT at 325-674-5555.");
                                break;
                            case "2":
                                System.out.println("To download LockDown Browser to your computer, go to: https://download.respondus.com/lockdown/download.php?id=167846866. This is an ACU specific link and you must use this link to download LockDown Browser for an ACU class.");
                                break;
                            case "3":
                                System.out.println("To join any ACU Wi-Fi network, ACUSecure or ACUGuest, go to your settings and select the network. It will then prompt you to enter your username and password. Your username is your ACU email (without @acu.edu, i.e abc21c) and your password is the same password you use to log into my.acu.edu. If you are having trouble, please call ACU IT at 325-674-5555.");
                                break;
                            case "4":
                                System.out.println("If you want to print from a personal device to any of the printers in the residence halls or labs, go to acu.edu/print. You can log in with your ACU username (without the @acu.edu, i.e abc21c) and password. From here you will be able to upload documents directly. To change the printer you print to you can select it from the drop-down box in the lower right hand corner. You will need to search the printer in the list by typing in the name of the printer, which can be found on a label on each printer.");
                                break;
                            case "5":
                                System.out.println("To start please go here: https://clearpass.acu.edu/guest/auth_login.php?target=%2Fguest%2Fmac_create.php. This is the ACU specific link to register a device. Once there, please select \"Register a Device\" and fill out the form. The MAC address is the physical address of the device and can be found in the settings of the device. If you are having trouble, please call ACU IT at 325-674-5555.");
                                break;
                            case "chat":
                                System.out.println("\nEntering chatbot mode...");
                                break;
                            case "back":
                                return;
                            default:
                                System.out.println("Please enter a valid number (1–5), 'chat', or 'back'.");
                                continue;
                        }

                        if (itFaq.equals("chat")) break;
                        System.out.println("\nYou can type another FAQ number, 'chat' to begin chatting, or 'back' to return.");
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
            System.out.println("Type 'reset' to start a new conversation or 'exit' to end it.");
    
            String userInput;
            while (true) {
                System.out.print("\nYou: ");
                userInput = reader.readLine().trim();
                boolean isMajorClassQuery = userInput.toLowerCase().contains("what major classes do i need to graduate");

                saveQuestion(userInput);
    
                if (userInput.equalsIgnoreCase("exit")) {
                    break;
                }
    
                if (userInput.equalsIgnoreCase("reset")) {
                    threadId = null;
                    System.out.println("🧹 Conversation context cleared. Start fresh!");
                    continue;
                }
    
                if (userInput.isEmpty()) {
                    continue;
                }
    
                if (threadId == null) {
                    List<JSONObject> messages = List.of(
                            new JSONObject().put("role", "user").put("content", userInput)
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
    
                JSONArray vectorStoreArray = new JSONArray();
                vectorStoreArray.put(vectorStoreId);
                
                JSONObject fileSearchJson = new JSONObject();
                fileSearchJson.put("vector_store_ids", vectorStoreArray);
                
                JSONObject toolResourcesJson = new JSONObject();
                toolResourcesJson.put("file_search", fileSearchJson);
                
                runId = assistant.createRun(
                    threadId,
                    assistantId,
                    null, null, null, null,
                    null, null, null,
                    null, null, null,
                    null, null, null,
                    null, null, null,
                    toolResourcesJson
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
                    String rawReply = retrievedMessages.get(0);
                    boolean formatted = false;

                
                    // Remove non-ASCII characters
                    String cleanReply = rawReply.replaceAll("[^\\x20-\\x7E\\n\\r]", "").trim();
                    if (!formatted && isMajorClassQuery) {
                        try (BufferedReader readerAll = new BufferedReader(new FileReader("user_info.txt"))) {
                            System.out.println("\nAdvisor: Here are all the major classes required to graduate:");
                            String line;
                            while ((line = readerAll.readLine()) != null) {
                                String[] parts = line.split(",");
                                if (parts.length == 2) {
                                    String course = parts[0].trim().toUpperCase();
                                    String credits = parts[1].trim();
                                    System.out.println(" - " + course + " (" + credits + " credits)");
                                }
                            }
                            formatted = true;
                        } catch (IOException e) {
                            System.out.println("Error reading major class file: " + e.getMessage());
                        }
                    
                    }
                        
                
                    // Remove trailing citation artifacts
                    cleanReply = cleanReply.replaceAll("(\\d+:\\d+source)", ""); 
                    cleanReply = cleanReply.replaceAll("\\s+", " ").trim();
                
                    // General pattern for name lists
                    String[] introPhrases = {
                        "The professors in the",
                        "The faculty members in the",
                        "The instructors in the",
                        "The advisors in the",
                        "The professors are",
                        "The faculty are"
                    };
                
                    for (String phrase : introPhrases) {
                        int idx = cleanReply.indexOf(phrase);
                        if (idx != -1) {
                            int startIdx = cleanReply.indexOf("are", idx);
                            if (startIdx != -1) {
                                String beforeList = cleanReply.substring(0, startIdx + 3).trim();
                                String namesPart = cleanReply.substring(startIdx + 3).trim();
                
                                namesPart = namesPart.replaceAll("\\.\\s*$", "").replaceAll("\\s*\\d+:\\d+source\\s*$", "");
                
                                String[] names = namesPart.split(",\\s*|\\s+and\\s+");
                
                                System.out.println("\nAdvisor: " + beforeList);
                                for (String name : names) {
                                    name = name.replaceAll("\\d+:\\d+source", "").trim();
                                    if (!name.isEmpty()) {
                                        System.out.println(" - " + name);
                                    }
                                }
                                formatted = true;
                                break;
                            }
                        }
                    }
                    
                    if (!formatted && cleanReply.toLowerCase().contains("you are currently enrolled in the following classes:")) {
                        int idx = cleanReply.toLowerCase().indexOf("you are currently enrolled in the following classes:");
                        String intro = cleanReply.substring(0, idx + "you are currently enrolled in the following classes:".length()).trim();
                        String listPart = cleanReply.substring(idx + "you are currently enrolled in the following classes:".length()).trim();
                    
                        // Split by numbered lines like "1. ...", "2. ..."
                        String[] classEntries = listPart.split("\\d+\\.\\s+");
                    
                        System.out.println("\nAdvisor: " + intro);
                        for (String entry : classEntries) {
                            String trimmed = entry.trim();
                            if (!trimmed.isEmpty()) {
                                System.out.println(" - " + trimmed);
                            }
                        }
                        formatted = true;
                    }

                
                    if (!formatted) {
                        System.out.println("\nAdvisor:");
                        String[] lines = cleanReply.split("\\.\\s+");
                        for (String line : lines) {
                            String trimmed = line.trim();
                            if (!trimmed.isEmpty()) {
                                System.out.println(" - " + trimmed + ".");
                            }
                        }
                    }
                } else {
                    System.out.println("No response received. Please try again.");
                
                
                
            }

                }
            
        } catch (IOException e) {
            System.out.println("Error reading input: " + e.getMessage());
        }
    }

    
}
