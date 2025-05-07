/*
 * 
 * Chatbot.java
 * 
 * Author: Albert Tucker, Michael Aghassi
 * Course: CS375 - Software Engineering II
 * 
 * Description:
 * This Java program implements an AI-powered academic advising chatbot designed specifically 
 * for students at Abilene Christian University (ACU). It utilizes OpenAI's Assistant API 
 * to provide students with real-time responses to academic questions by referencing user-specific 
 * data stored locally (user_info.txt and acu_database.txt).
 *
 * Key Features:
 * - Login and user profile creation with local credential storage.
 * - Integration with OpenAI’s Assistant API for natural language interaction.
 * - File vector store setup for persistent and context-aware question answering.
 * - Chat session management with inactivity timeouts.
 * - Chat history tracking and automatic addition to a personal FAQ list after 3 repeated queries.
 * - Department-specific FAQs for Academic Advising, IT Support, and more.
 * - Text-based UI enhancements via TextEngine (colors, delayed output, interactive settings).
 *
 * Dependencies:
 * - OpenAiAssistantEngine.java: Handles API interaction and assistant operations.
 * - TextEngine.java: Provides formatted and timed console output features.
 * - chatTimer.java: Manages session timeout behavior.
 * - org.json: For JSON request/response handling.
 *
 * Usage:
 * Compile and run the application. Choose to log in or create a new profile. Once authenticated,
 * the user can chat with the assistant, access FAQs, or modify settings.
 */

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
 import java.util.concurrent.atomic.AtomicBoolean;
 
 import org.json.JSONArray;
 import org.json.JSONObject;
  
 public class Chatbot {
  //all the colors needed for this code
     public static String threadId = null;
     public static String runId = null;
     public static String redColor = "\033[1;31m";
     public static String yellowColor = "\033[1;33m";
     public static String greenColor = "\033[1;32m";
     public static String cyanColor = "\033[1;36m";
     public static String blueColor = "\033[1;34m";
     public static String purpleColor = "\033[35m";
     public static String resetColor = "\033[0m";
  
     //private static OpenAiAssistantEngine assistantSelfCare;
     private static OpenAiAssistantEngine assistant;
     private static final String APIKEY = System.getenv("OPENAI_API_KEY");
     private static final File USER_INFO_FILE = new File("user_info.txt");
     private static final File ACU_DATABASE_FILE = new File("acu_database.txt");
     private static final File CHAT_HISTORY_FILE = new File("chat_history.txt");
     private static final File PERSONAL_FAQ_FILE = new File("personal_faq.txt");
     private static String vectorStoreId;
     private static String assistantId;
     public static final Map<String, Integer> questionCount = new HashMap<>();
     public static BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
     //private static final int RUN_TIMEOUT_SECONDS = 90;
     private static String usersName;
  
  
     public static void main(String[] args) {


         loadQuestionHistory();
         
         //usersName = parseUserInfo();
         assistant = new OpenAiAssistantEngine(APIKEY);
         
  
         TextEngine.clearScreen();
         printStartupBanner();
         TextEngine.printWithDelay("\nWelcome to the ACU AI Academic Advisor!", true);
  
         try {
             TextEngine.printWithDelay("press "+yellowColor+ "(1)" +resetColor+ " Log in or " +yellowColor+ "(2)" +resetColor+ " Create a Profile?", true);
             TextEngine.printWithDelay("Enter " +yellowColor+ "1" +resetColor+ " or " +yellowColor+ "2" +resetColor+ ": ", false);
             String choice = reader.readLine().trim();
  
             if (null == choice) {
                 TextEngine.printWithDelay("Invalid choice. Please restart and select either 1 or 2.", true);
                 return;
             } else switch (choice) {
                 case "1" -> Login();
                 case "2" -> createProfile();
                     
                 default -> {
                     TextEngine.printWithDelay("Invalid choice. Please enter either 1 or 2.", false);
                     
                     choice = reader.readLine().trim();
                     if (null == choice) {
                        TextEngine.printWithDelay("Invalid choice. Please restart and select either 1 or 2.", true);
                        return;
                     } else switch (choice) {
                         case "1" -> Login();
                         case "2" -> createProfile();
                         default -> {
                             TextEngine.printWithDelay("Exting program for safty...", true);
                             return;
                         }
                     }
                 }
             }
         } catch (IOException e) {
             TextEngine.printWithDelay("An error occurred: " + e.getMessage() + ", you gone and messed up!!", true);
             return;
         }
  
         assistant = new OpenAiAssistantEngine(APIKEY);
         System.out.println("-------------------------");
         TextEngine.printWithDelay("Setting up AI Academic Advisor...", true);
  
        
         
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
        if (question == null || question.trim().isEmpty()) {
            return;
        }
    
        question = question.trim();
    
        if (question.equalsIgnoreCase("exit") || question.equalsIgnoreCase("reset") ||
            question.equalsIgnoreCase("back") || question.equalsIgnoreCase("help")) {
            return;
        }
    
        // Ensure chat history file exists or is created
        try {
            if (!CHAT_HISTORY_FILE.exists()) {
                CHAT_HISTORY_FILE.createNewFile();
            }
    
            try (FileWriter writer = new FileWriter(CHAT_HISTORY_FILE, true)) {
                writer.write(question + "\n");
                writer.flush(); // Ensure it's written to disk immediately
            }
    
            System.out.println("Saved question to history: " + question);
    
        } catch (IOException e) {
            System.out.println("Failed to save question: " + e.getMessage());
        }
    
        // Count question for personal FAQ tracking
        questionCount.put(question, questionCount.getOrDefault(question, 0) + 1);
    
        if (questionCount.get(question) == 3) {
            try {
                if (!PERSONAL_FAQ_FILE.exists()) {
                    PERSONAL_FAQ_FILE.createNewFile();
                }
    
                try (FileWriter writer = new FileWriter(PERSONAL_FAQ_FILE, true)) {
                    writer.write("- " + question + "\n");
                    writer.flush();
                }
    
                System.out.println("Added to personal FAQ: " + question);
    
            } catch (IOException e) {
                System.out.println("Failed to update personal FAQ: " + e.getMessage());
            }
        }
    }
    
    
  
      private static void printStartupBanner() {
         System.out.println(purpleColor + "  ----  █████" + resetColor + "╗" + purpleColor + "   ██████" + resetColor + "╗" + purpleColor + "  ██" + resetColor + "╗" + purpleColor + "   ██" + resetColor + "╗  " +purpleColor+ "----");
         System.out.println(resetColor + " ---   " + purpleColor + "██" + resetColor + "╔══" + purpleColor + "██" + resetColor + "╗" + purpleColor + "  ██" + resetColor + "╔═══╝" + purpleColor + "  ██" + resetColor + "║" + purpleColor + "   ██" + resetColor + "║  ---");
         System.out.println(purpleColor + " ----  ███████" + resetColor + "║" + purpleColor + "  ██" + resetColor + "║" + purpleColor + "      ██" + resetColor + "║" + purpleColor + "   ██" + resetColor + "║  " +purpleColor+"----");
         System.out.println(resetColor + " ---   " + purpleColor + "██" + resetColor + "╔══" + purpleColor + "██" + resetColor + "║" + purpleColor + "  ██" + resetColor + "╚═══╗" + purpleColor + "  ██" + resetColor + "║" + purpleColor + "   ██" + resetColor + "║  ----");
         System.out.println(purpleColor + " ----  ██" + resetColor + "║" + purpleColor + "  ██" + resetColor + "║" + purpleColor + "  ██████" + resetColor + "║" + purpleColor + "   ██████" + resetColor + "║  " +purpleColor+"---" + resetColor);
         System.out.println("═════════════════════════════════════");
       }
  
       private static String parseUserInfo() {
         try (BufferedReader reader = new BufferedReader(new java.io.FileReader(USER_INFO_FILE))) {
             String line = reader.readLine();
             if (line != null && line.startsWith("Name:")) {
                 return line.substring("Name:".length()).trim();
             } else {
                 TextEngine.printWithDelay("Invalid format in user info file. Expected 'Name: <name>' on first line.", true);
                 return null;
             }
         } catch (IOException e) {
             TextEngine.printWithDelay("Error reading user info file: " + e.getMessage(), true);
             return null;
         }
       }
   
     private static String setupAssistant() {
         // Create assistant
         String assistantName;
         //assistant = assistant;
         if (usersName == null) {
             assistantName = "AI Academic Advisor";
         } else {
             String[] nameParts = usersName.split(" ");
             if (nameParts.length > 1) {
                 assistantName = "AI Academic Advisor for " + nameParts[0] + " " + nameParts[1];
             } else {
                 assistantName = "AI Academic Advisor for " + usersName;
             }
         }
          
         assistantId = assistant.createAssistant(
            "gpt-3.5-turbo",
            assistantName,
            null,
            "You are a real-time AI Academic Advisor for Abilene Christian University. You are only allowed to use the data provided in user_info.txt when answering any question. "
          + "For questions like 'What classes am I currently in?', retrieve the value exactly following the line that starts with 'Enrolled Courses:' from user_info.txt. "
          + "NEVER generate or assume course names. Do not pull information from anywhere except the uploaded user_info.txt. "
          + "If you are unsure or the value does not exist in user_info.txt, respond with: 'I could not find that information in your file.'",
            null,
            List.of("file_search"),
            null,
            0.5,
            0.5,
            null
        );
        
         
      
         if (assistantId == null) {
             TextEngine.printWithDelay("Failed to create assistant", true);
             return null;
         }
 
      // Upload files to OpenAI
         String fileId = assistant.uploadFile(USER_INFO_FILE, "assistants");
         String fileId1 = assistant.uploadFile(ACU_DATABASE_FILE, "assistants");
      
         if (fileId == null || fileId1 == null) {
             TextEngine.printWithDelay("Failed to upload one or more files", true);
             return null;
         }
   
         // Create metadata for files
         Map<String, String> fileMetadata = new HashMap<>();
         fileMetadata.put(fileId, "This fileID is associated with the user info");
         fileMetadata.put(fileId1, "This fileID is associated with the ACU database");
   
         // Create vector store
        vectorStoreId = assistant.createVectorStore(
             "User Files",
             Arrays.asList(fileId, fileId1),
             null,
             null,
             fileMetadata
         );
   
         if (vectorStoreId == null) {
             TextEngine.printWithDelay("Failed to create vector store", true);
             return null;
         }
   
         // Update assistant with vector store
// Update assistant with vector store
Map<String, Object> toolResources = new HashMap<>();
toolResources.put("file_search", Map.of(
    "vector_store_ids", List.of(vectorStoreId)
));

boolean updateSuccess = assistant.modifyAssistant(
    assistantId, null, null, null, null, null, null, null, null,
    toolResources, null, null
);

if (!updateSuccess) {
    TextEngine.printWithDelay("Failed to update assistant with vector store.", true);
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
         final int INACTIVITY_TIMEOUT_SECONDS = 90;
  
         TextEngine.clearScreen();
         printStartupBanner();
         System.out.println("\n=== You can now chat with AI ===");
         TextEngine.printWithDelay("Type " +redColor+ "exit" +resetColor+ " to end the conversation", true);
         TextEngine.printWithDelay("Type " +yellowColor+ "setting" +resetColor+ " to change the text settings", true);
         TextEngine.printWithDelay("Type " +yellowColor+ "help" +resetColor+ " if you are lost and would like to see some FAQs", true);
         TextEngine.printWithDelay("What would you like help with?", true);
   
         try {
             String userInput;
             chatTimer sessionTimer = new chatTimer(INACTIVITY_TIMEOUT_SECONDS, () -> {
                 System.out.println("\n" + redColor + "Session timed out due to inactivity. Logging out..." + resetColor);
                 System.exit(0);
             });
             while (true) {
                 System.out.print("\nQuestion: ");
                 sessionTimer.reset(); // Start/reset before input
                 userInput = reader.readLine().trim();

                 if (userInput == null || userInput.trim().isEmpty()) {
                    sessionTimer.reset();
                    continue;
                }
                userInput = userInput.trim();
                saveQuestion(userInput); // Save the question to the history
                 
                 System.out.println("");
                  
                 if (userInput.equalsIgnoreCase("settings"))
                 {
                     TextEngine.openSettings(reader);
                     sessionTimer.reset();
                     continue;
                 }
                 if (userInput.equalsIgnoreCase("help"))
                 {
                     Chatbot.FAQs();
                     sessionTimer.reset();
                     continue;
                 }
              
                 if (userInput.equalsIgnoreCase("exit")) {
                     TextEngine.printWithDelay("Exiting...", true);
                     TextEngine.printRainbowText("\nThank you for using the ACU AI Academic Advisor. Goodbye!");
                     break;
                 }
   
                 if (userInput.isEmpty()) {
                     sessionTimer.reset();
                     continue;
                 }
   
                 // Create a thread if it doesn't exist yet
                 if (threadId == null) {
                     List<JSONObject> messages = List.of(
                         new JSONObject()
                         .put("role", "user") 
                         .put("content", userInput)
                     );
                     threadId = assistant.createThread(messages, null, null);
                     if (threadId == null) {
                         TextEngine.printWithDelay("Failed to create thread. Please try again.", true);
                         sessionTimer.reset();
                         continue;
                     }
                 } else {
                     // Add message to existing thread
                     String messageId = assistant.addMessageToThread(threadId, userInput);
                     if (messageId == null) {
                         TextEngine.printWithDelay("Failed to send message. Please try again.", true);
                         sessionTimer.reset();
                         continue;
                     }
                 }
   
                 JSONArray vectorStoreArray = new JSONArray();
                 vectorStoreArray.put(vectorStoreId);

                 JSONObject fileSearchJson = new JSONObject();
                 fileSearchJson.put("vector_store_ids", vectorStoreArray);

                 JSONObject toolResourcesJson = new JSONObject();
                 toolResourcesJson.put("file_search", fileSearchJson);

                 // Create and monitor run
                 runId = assistant.createRun(
                     threadId,
                     assistantId,
                     null, 
                     null, 
                     null, 
                     null, 
                     null, 
                     null, 
                     null, 
                     null, 
                     null, 
                     null,
                     null, 
                     null, 
                     null, 
                     null, 
                     null, 
                     null, 
                     toolResourcesJson
                 );
                 
   
                 if (runId == null) {
                     TextEngine.printWithDelay("Failed to create run. Please try again.", true);
                     sessionTimer.reset();
                     continue;
                 }
   
                 AtomicBoolean isRunning = new AtomicBoolean(true);
                 Thread loadingThread = startLoadingAnimation(isRunning);
   
                 boolean completed = assistant.waitForRunCompletion(threadId, runId, 60,1000);
   
                 isRunning.set(false);
                 try {
                     loadingThread.join();
                 } catch (InterruptedException e) {
                     Thread.currentThread().interrupt();
                 } 
                 if (!completed) {
                     TextEngine.printWithDelay("The assistant encountered an issue. Please try again.", true);
                     sessionTimer.reset();
                     continue;
                 }
   
                 // Get the assistant's response
                 List<String> retrievedMessages = assistant.listMessages(threadId, runId);
                 if (retrievedMessages != null && !retrievedMessages.isEmpty()) {
                     TextEngine.printWithDelay(retrievedMessages.get(0), true);
                 } else {
                     TextEngine.printWithDelay("No response received. Please try again.", true);
                 }
             }
   
             System.out.println("\nSession Statistics:");
             assistant.getCategories().forEach(category -> {
                 System.out.println(" - " + category + ": "+ assistant.getResponsesByCategory(category).size() + " responses");
             });
   
              
   
             // Clean up resources
             if (threadId != null) {
                assistant.deleteResource("threads", threadId);
             }
   
         } catch (IOException e) {
             System.out.println("Error reading input: " + e.getMessage());
         }
     }   
   
     private static void Login() {
         Map<String, String> credentials = new HashMap<>();
      
         try {
             if (!USER_INFO_FILE.exists()) {
                 TextEngine.printWithDelay("User info file not found. Unable to log in.", true);
                 return;
             }
       
             // Read user info into a map (assuming format: username,password)
             try (BufferedReader fileReader = new BufferedReader(new InputStreamReader(new java.io.FileInputStream(USER_INFO_FILE)))) {
                 String line;
                 while ((line = fileReader.readLine()) != null) {
                     String[] parts = line.split(",");
                     if (parts.length == 2) {
                         credentials.put(parts[0].trim(), parts[1].trim());
                     }
                 }
             }
      
             // Get user input for login
             TextEngine.printWithDelay("Enter you name: ", false);
             String username = reader.readLine().trim();
             TextEngine.printWithDelay("Enter your password: ", false);
             String password = reader.readLine().trim();
       
             // Validate credentials
             if (credentials.containsKey(username) && credentials.get(username).equals(password)) {
                 TextEngine.printWithDelay(greenColor+"Login successful."+resetColor+"\n Welcome, " + username + "!", true);
             } else {
                 TextEngine.printWithDelay("Invalid username or password. Exiting.", true);
                 System.exit(1);
             }
         } catch (IOException e) {
             TextEngine.printWithDelay("An error occurred during login: " + e.getMessage(), false);
         }
     }
   
     private static void createProfile() {
         try {
             TextEngine.printWithDelay("Enter a username: ", false);
             String username = reader.readLine().trim();
             TextEngine.printWithDelay("Enter a password: ", false);
             String password = reader.readLine().trim();
      
             // Save to user info file
             try (java.io.FileWriter writer = new java.io.FileWriter(USER_INFO_FILE, true)) {
                 writer.write(username + "," + password + "\n");
                 System.out.println(greenColor+"Profile created successfully." +resetColor);
             }
         } catch (IOException e) {
             TextEngine.printWithDelay("An error occurred while creating the profile: " + e.getMessage(), false);
         }
     }
  
     private static Thread startLoadingAnimation(AtomicBoolean isRunning) {
         Thread loadingThread = new Thread(() -> { String[] frames = {".  ", ".. ", "...", " ..", "  .", "   "};
             int index = 0;
             try {
                 while (isRunning.get()) {
                     System.out.print("\rThinking" + frames[index]);
                     System.out.flush();
                     index = (index + 1) % frames.length;
                     Thread.sleep(300);
                 }
                 System.out.print("\r                                \r");
                 System.out.flush();
             } catch (InterruptedException e) {
                 Thread.currentThread().interrupt();
             }
         });
         loadingThread.setDaemon(true);
         loadingThread.start();
         return loadingThread;
     }
  
    private static void FAQs() throws IOException{
         
        TextEngine.printWithDelay("Here are some FAQ's based on different departments in the school:", true);
        TextEngine.printWithDelay("Section 1. Student Sucess FAQ", true);
        TextEngine.printWithDelay("Section 2. Your Personal FAQ", true);
        TextEngine.printWithDelay("Section 3. Academic FAQ", true);
        TextEngine.printWithDelay("Section 4. ACU IT FAQ", true);
        TextEngine.printWithDelay("Please type the department number you would like to view (e.g., '1'), or type 'skip' to go directly to the chatbot.", false);
         
        TextEngine.printWithDelay("Section: ", false);
        String selection = reader.readLine().trim().toLowerCase();
        if (selection.equals("1") || selection.contains("academic")) {
            TextEngine.printWithDelay("\n--- FAQs for Academic Advisor ---", true);
            TextEngine.printWithDelay("1. How do I check my current/upcoming registrations?", true);
            TextEngine.printWithDelay("2. When can I access my upcoming courses in Canvas?", true);
            TextEngine.printWithDelay("3. When do the next courses start?", true);
            TextEngine.printWithDelay("4. What if I need to change or drop an upcoming course?", true);
            TextEngine.printWithDelay("5. When will final grades be posted?", true);
            TextEngine.printWithDelay("6. What happens if I fail my course(s)?", true);
            TextEngine.printWithDelay("7. What happens if I'm not financially clear for the next term?", true);
            TextEngine.printWithDelay("8. How can I reach my Financial Intake Specialist (FIS) about financial concerns?", true);
            TextEngine.printWithDelay("9. How do I reach out to technical support?", true);
            TextEngine.printWithDelay("\nPlease enter the question number (1 through 9), type 'chat' to begin chatting, or 'back' to return to departments.", true);
             
            while (true) {
                TextEngine.printWithDelay("FAQ #: ", false);
                String faqInput = reader.readLine().trim().toLowerCase();
             
                switch (faqInput) {
                    case "1" -> TextEngine.printWithDelay("To check on upcoming/current registrations, please navigate to Degree Works and scroll to the bottom. It will show you your current and registered classes for the next term. To access Degree Works, please go to my.acu.edu, and in the search bar, please type in \"Degree Works\".", true);
                    case "2" -> TextEngine.printWithDelay("You can only access your upcoming courses in Canvas when the professor decides to publish them. Most of the time, you will have access a day or two before the start day of the semester.", true);
                    case "3" -> TextEngine.printWithDelay("The next courses start depending on when you registered for them. If you signed up for summer classes they will start during the summer term. If you signed up for classes during the fall or spring term they will start when you get there. You can check the specific day by going to my.acu.edu and searching for \"First day of classes\".", true);
                    case "4" -> TextEngine.printWithDelay("If you need to drop or change a course, you will need to reach out to your academic advisor and ask them to either drop or change a course. If you drop a course within the first week of the start of the semester (Monday-Friday before 5:00 PM) then you will get a full refund for that course and your transcript will be unaffected. If you decide to change a class to another class during the first week, your transcript will also be unaffected.", true);
                    case "5" -> TextEngine.printWithDelay("Final grades will be posted within 1-2 weeks of the semester concluding. If you go to my.acu.edu and in the search bar type banner, you will have access to see your unofficial transcript and see your grades. Once there in the search bar, type in \"transcript\" and you will see. The transcript will only show a letter grade, so if you wish to see your numerical grade then please navigate to Canvas. Once there, please select courses and on the top right select \"view all courses\". Here you can see all the previous courses you took and what numerical value you achieved in them.", true);
                    case "6" -> TextEngine.printWithDelay("If you fail your course(s) they will count as an F on your transcript and carry a weight of 0 on the 4.0 scale. Regardless of it being a major class, university requirement, or an elective, you would not get credit for the class or classes and would have to retake them. ACU's policy is that you have 3 attempts to retake a class. Every time that you retake it whatever grade you make on the end will replace the current one. Also, ACU will keep the highest score automatically. So if you get a B in a class, decide to retake it for an A and get a C instead you will keep the B.", true);
                    case "7" -> TextEngine.printWithDelay("If you are not financially clear for the next term, then you need to call Student Services at 325-674-2300. They will tell you the next steps and ultimately, if you aren't able to pay, you will be removed from the school.", true);
                    case "8" -> TextEngine.printWithDelay("Along with an academic advisor, each student is assigned a financial advisor as well. To reach them, please call Wildcat Central at 325-674-6770 and ask them who it is.", true);
                    case "9" -> TextEngine.printWithDelay("To reach ACU technical support, please call 325-674-5555. They will assist you with whatever technical problems you have.", true);
                    case "chat" -> TextEngine.printWithDelay("\nEntering chatbot mode...", true);
                    case "back" -> {
                        return;
                    }
                    default -> {
                        TextEngine.printWithDelay("Please enter a valid number (1-9), 'chat', or 'back'.", false);
                        continue;
                    }
                }
             
                if (faqInput.equals("chat")) break;
                         
                TextEngine.printWithDelay("\nYou can type another FAQ number, 'chat' to begin chatting, or 'exit' to quit the chatbot.", false);
                    break;
            }
            //break; // exit FAQ loop to continue to chatbot
        }

        else if (selection.equals("2")) {
            TextEngine.printWithDelay("\n--- Your Personal FAQ ---", true);
            List<String> faqList = new ArrayList<>();
                
            if (!PERSONAL_FAQ_FILE.exists()) { TextEngine.printWithDelay("You have no personal FAQs yet.", true);} 
            else {
                try (BufferedReader faqReader = new BufferedReader(new FileReader(PERSONAL_FAQ_FILE))) {
                    String line;
                    int i = 1;
                    while ((line = faqReader.readLine()) != null) {
                        faqList.add(line.substring(2)); // remove "- " prefix
                        TextEngine.printWithDelay(i + ". " + line.substring(2), true);
                        i++;
                    }
                } catch (IOException e) {
                    TextEngine.printWithDelay("Failed to load personal FAQ: " + e.getMessage(), true);
                }
            
                if (!faqList.isEmpty()) {
                    while (true) {
                        TextEngine.printWithDelay("\nType the number of a question you'd like the advisor to answer, or type 'back': ", false);
                        String input = reader.readLine().trim().toLowerCase();
            
                        if (input.equals("back")) break;
                
                        try {
                            int choice = Integer.parseInt(input);
                            if (choice >= 1 && choice <= faqList.size()) {
                                String selectedQuestion = faqList.get(choice - 1);
                                TextEngine.printWithDelay("\nYou asked: " + selectedQuestion, false);
                
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
                                    toolResourcesJson 
                                );
                                        
                
                                if (runId == null) {
                                    TextEngine.printWithDelay("Failed to create run.", true);
                                    continue;
                                }
                                boolean completed = assistant.waitForRunCompletion(threadId, runId, 60, 1000);
                
                                if (!completed) {
                                    TextEngine.printWithDelay("The assistant encountered an issue. Please try again.", true);
                                    continue;
                                }
                
                                List<String> replies = assistant.listMessages(threadId, runId);
                                if (replies != null && !replies.isEmpty()) {
                                    TextEngine.printWithDelay("\nAdvisor: " + replies.get(0), true);
                                } else {
                                    TextEngine.printWithDelay("Advisor had no response.", true);
                                }
                
                                } else {
                                    TextEngine.printWithDelay("Invalid number.", true);
                                }
                            } 
                        catch (NumberFormatException e) {
                            TextEngine.printWithDelay("Please enter a number or 'back'.", true);
                        }
                    }
                }
            }
        }
            
        else if (selection.equals("3")) {
            TextEngine.printWithDelay("\n--- FAQs for Academic Advisor (On-Campus) ---", true);
            TextEngine.printWithDelay("1. How do I calculate my GPA?", true);
            TextEngine.printWithDelay("2. What will my GPA be next semester if I make these certain grades?", true);
            TextEngine.printWithDelay("3. What are the prerequisites for this class?", true);
            TextEngine.printWithDelay("4. What classes should I take next semester?", true);
            TextEngine.printWithDelay("5. What are all the requirements for my major?", true);
            TextEngine.printWithDelay("6. What Summer classes are good options for me to take at another school and transfer in?", true);
            TextEngine.printWithDelay("\nPlease enter the question number (1 through 6), type 'chat' to begin chatting, or 'back' to return to departments.", true);
            
            while (true) {
                TextEngine.printWithDelay("FAQ #: ", false);
                String academicFaq = reader.readLine().trim().toLowerCase();
            
                switch (academicFaq) {
                    case "1" -> TextEngine.printWithDelay("You can calculate your GPA by adding up all the grade points you've earned and dividing by the total number of credit hours. Check with Degree Works for your current GPA data.", true);
                    case "2" -> TextEngine.printWithDelay("To estimate your GPA for next semester, assume letter grades for each course and apply ACU's grade point scale to see how it impacts your cumulative average.", true);
                    case "3" -> TextEngine.printWithDelay("Prerequisites vary by course. You can look up a specific course in the ACU course catalog to see what prerequisites are listed.", true);
                    case "4" -> TextEngine.printWithDelay("Course selection depends on your degree audit and what requirements are still pending. Your advisor can help pick classes that align with your graduation timeline.", true);
                    case "5" -> TextEngine.printWithDelay("All major requirements are listed in your Degree Works audit. It will show you completed, in-progress, and remaining requirements.", true);
                    case "6" -> TextEngine.printWithDelay("General Education and elective classes are good options for summer transfer. Always confirm with your advisor before registering at another school.", true);
                    case "chat" -> TextEngine.printWithDelay("\nEntering chatbot mode...", true);
                    case "back" -> {
                        return;
                    }
                    default -> {
                        TextEngine.printWithDelay("Please enter a valid number (1 through 6), 'chat', or 'back'.", false);
                        continue;
                    }
                }
            
                if (academicFaq.equals("chat")) break;
                    TextEngine.printWithDelay("\nYou can type another FAQ number, 'chat' to begin chatting, or 'back' to return.", false);
            }
        }

        else if (selection.equals("4")) {
            TextEngine.printWithDelay("\n--- FAQs for ACU IT ---", true);
            TextEngine.printWithDelay("1. How do I reset my password?", true);
            TextEngine.printWithDelay("2. How do I install lockdown browser?", true);
            TextEngine.printWithDelay("3. How do I set up my Wi-Fi?", true);
            TextEngine.printWithDelay("4. How do I print?", true);
            TextEngine.printWithDelay("5. How to register a non computer/laptop devide to the network?", true);
            TextEngine.printWithDelay("\nPlease enter the question number (1-5), type 'chat' to begin chatting, or 'back' to return to departments.", true);

            while (true) {
                TextEngine.printWithDelay("FAQ #: ", false);
                String itFaq = reader.readLine().trim().toLowerCase();
                switch (itFaq) {
                    case "1" -> TextEngine.printWithDelay("To reset your password, go to acu.edu/password. Then log in with your ACU credentials to which it will prompt you to change your password. If you are having trouble, please call ACU IT at 325-674-5555.", true);
                    case "2" -> TextEngine.printWithDelay("To download LockDown Browser to your computer, go to: https://download.respondus.com/lockdown/download.php?id=167846866. This is an ACU specific link and you must use this link to download LockDown Browser for an ACU class.", true);
                    case "3" -> TextEngine.printWithDelay("To join any ACU Wi-Fi network, ACUSecure or ACUGuest, go to your settings and select the network. It will then prompt you to enter your username and password. Your username is your ACU email (without @acu.edu, i.e abc21c) and your password is the same password you use to log into my.acu.edu. If you are having trouble, please call ACU IT at 325-674-5555.", true);
                    case "4" -> TextEngine.printWithDelay("If you want to print from a personal device to any of the printers in the residence halls or labs, go to acu.edu/print. You can log in with your ACU username (without the @acu.edu, i.e abc21c) and password. From here you will be able to upload documents directly. To change the printer you print to you can select it from the drop-down box in the lower right hand corner. You will need to search the printer in the list by typing in the name of the printer, which can be found on a label on each printer.", true);
                    case "5" -> TextEngine.printWithDelay("To start please go here: https://clearpass.acu.edu/guest/auth_login.php?target=%2Fguest%2Fmac_create.php. This is the ACU specific link to register a device. Once there, please select \"Register a Device\" and fill out the form. The MAC address is the physical address of the device and can be found in the settings of the device. If you are having trouble, please call ACU IT at 325-674-5555.", true);
                    case "chat" -> TextEngine.printWithDelay("\nEntering chatbot mode...", true);
                    case "back" -> {
                        return;
                    }
                    default -> {
                        TextEngine.printWithDelay("Please enter a valid number (1-5), 'chat', or 'back'.", false);
                        continue;
                    }
                }

                if (itFaq.equals("chat")) break;
                TextEngine.printWithDelay("\nYou can type another FAQ number, 'chat' to begin chatting, or 'back' to return.", true);
            }
        }
        
        else if (selection.equals("skip")) return;
        
        else {
            TextEngine.printWithDelay("Invalid input. Type '1', '2', or 'skip'.", false);
        }
    }

}
