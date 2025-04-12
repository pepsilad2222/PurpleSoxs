/*
 * Albert Tucker 
 * CS375
 * ChatBot for classes
 */

 import java.io.BufferedReader;
 import java.io.File;
 import java.io.IOException;
 import java.io.InputStreamReader;
 import java.util.Arrays;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 import java.util.concurrent.atomic.AtomicBoolean;
 
 import org.json.JSONObject;
 
 public class Chatbot {
     public static String redColor = "\033[1;31m";
     public static String yellowColor = "\033[1;33m";
     public static String greenColor = "\033[1;32m";
     public static String cyanColor = "\033[1;36m";
     public static String blueColor = "\033[1;34m";
     public static String purpleColor = "\033[35m";
     public static String resetColor = "\033[0m";
 
     private static OpenAiAssistantEngine assistantSelfCare;
     private static final String APIKEY = "API_KEY";
     private static final File USER_INFO = new File("user_info.txt");
     private static final File ACU_DATABASE = new File("acu_database.txt");
     private static final int RUN_TIMEOUT_SECONDS = 60;
     private static String usersName;
 
 
     public static void main(String[] args) {
         
         usersName = parseUserInfo();
         assistantSelfCare = new OpenAiAssistantEngine(APIKEY);
         BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
 
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
                 case "2" -> {
                     createProfile();
                     TextEngine.printWithDelay("Please log in with your new profile.", true);
                     Login();
                 }
                 default -> {
                     TextEngine.printWithDelay("Invalid choice. Please restart and select either 1 or 2.", true);
                     return;
                 }
             }
         } catch (IOException e) {
             TextEngine.printWithDelay("An error occurred: " + e.getMessage() + ", you gone and messed up!!", true);
             return;
         }
 
         assistantSelfCare = new OpenAiAssistantEngine(APIKEY);
         System.out.println("-------------------------");
         TextEngine.printWithDelay("Setting up AI Academic Advisor...", true);
 
         String assistantId = setupAssistant();
         if (assistantId == null) {
             TextEngine.printWithDelay("Failed to set up assistant. Exiting.", true);
             return;
         }
 
         startInteractiveChat(assistantId);
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
         try (BufferedReader reader = new BufferedReader(new java.io.FileReader(USER_INFO))) {
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
         
         String assistantId = assistantSelfCare.createAssistant(
             "gpt-4o-mini",
             assistantName,
             null,
             "You are a real-time chat AI Academic Advisor for Abilene Christian University. Please refer to the user_info file for user related information. ",
             null,
             List.of("file_search"),
             null,
             0.2,
             0.1,
             null
         );
 
         // Upload files to OpenAI
         String fileId = assistantSelfCare.uploadFile(USER_INFO, "assistants");
         String fileId1 = assistantSelfCare.uploadFile(ACU_DATABASE, "assistants");
 
  
         if (assistantId == null) {
             TextEngine.printWithDelay("Failed to create assistant", true);
             return null;
         }
  
          
         if (fileId == null || fileId1 == null) {
             TextEngine.printWithDelay("Failed to upload one or more files", true);
             return null;
         }
  
         // Create metadata for files
         Map<String, String> fileMetadata = new HashMap<>();
         fileMetadata.put(fileId, "This fileID is associated with the user info");
         fileMetadata.put(fileId1, "This fileID is associated with the ACU database");
  
         // Create vector store
         String vectorStoreId = assistantSelfCare.createVectorStore(
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
         Map<String, Object> toolResources = new HashMap<>();
         Map<String, List<String>> fileSearch = new HashMap<>();
         fileSearch.put("vector_store_ids", List.of(vectorStoreId));
         toolResources.put("file_search", fileSearch);
  
         boolean updateSuccess = assistantSelfCare.updateAssistant(assistantId,toolResources);
  
         if (!updateSuccess) {
             TextEngine.printWithDelay("Failed to update assistant with vector store", true);
             return null;
         }
  
         System.out.println("Assistant setup successfully with ID: " + assistantId);
         return assistantId;
     }
  
     private static void startInteractiveChat(String assistantId) {
         BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
         String threadId = null;
         final int INACTIVITY_TIMEOUT_SECONDS = 40;
 
         TextEngine.clearScreen();
         //printStartupBanner();
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
                 sessionTimer.reset();
                 System.out.println("");
                 
                 if (userInput.equalsIgnoreCase("settings"))
                 {
                     TextEngine.openSettings(reader);
                     continue;
                 }
                 if (userInput.equalsIgnoreCase("help"))
                 {
                     Chatbot.FAQs();
                     continue;
                 }
             
                 if (userInput.equalsIgnoreCase("exit")) {
                     TextEngine.printWithDelay("Exiting...", true);
                     TextEngine.printRainbowText("\nThank you for using the ACU AI Academic Advisor. Goodbye!");
                     break;
                 }
  
                 if (userInput.isEmpty()) {
                     continue;
                 }
  
                 // Create a thread if it doesn't exist yet
                 if (threadId == null) {
                     List<JSONObject> messages = List.of(
                         new JSONObject()
                         .put("role", "user")
                         .put("content", userInput)
                     );
                     threadId = assistantSelfCare.createThread(messages, null, null);
                     if (threadId == null) {
                         TextEngine.printWithDelay("Failed to create thread. Please try again.", true);
                         continue;
                     }
                 } else {
                     // Add message to existing thread
                     String messageId = assistantSelfCare.addMessageToThread(threadId, userInput);
                     if (messageId == null) {
                         TextEngine.printWithDelay("Failed to send message. Please try again.", true);
                         continue;
                     }
                 }
  
                 // Create and monitor run
                 String runId = assistantSelfCare.createRun(
                     threadId,
                     assistantId,
                     null, null, null, null, null, null, null, null, null, null,
                     null, null, null, null, null, null
                 );
  
                 if (runId == null) {
                     TextEngine.printWithDelay("Failed to create run. Please try again.", true);
                     continue;
                 }
  
                 AtomicBoolean isRunning = new AtomicBoolean(true);
                 Thread loadingThread = startLoadingAnimation(isRunning);
  
                 boolean completed = assistantSelfCare.waitForRunCompletion(threadId, runId, RUN_TIMEOUT_SECONDS);
  
                 isRunning.set(false);
                 try {
                     loadingThread.join();
                 } catch (InterruptedException e) {
                     Thread.currentThread().interrupt();
                 } 
                 if (!completed) {
                     TextEngine.printWithDelay("The assistant encountered an issue. Please try again.", true);
                     continue;
                 }
  
                 // Get the assistant's response
                 List<String> retrievedMessages = assistantSelfCare.listMessages(threadId, runId);
                 if (retrievedMessages != null && !retrievedMessages.isEmpty()) {
                     TextEngine.printWithDelay(retrievedMessages.get(0), true);
                 } else {
                     TextEngine.printWithDelay("No response received. Please try again.", true);
                 }
             }
  
             System.out.println("\nSession Statistics:");
             assistantSelfCare.getCategories().forEach(category -> {
                 System.out.println(" - " + category + ": "+ assistantSelfCare.getResponsesByCategory(category).size() + " responses");
             });
  
             
  
             // Clean up resources
             if (threadId != null) {
                 assistantSelfCare.deleteResource("threads", threadId);
             }
  
         } catch (IOException e) {
             System.out.println("Error reading input: " + e.getMessage());
         }
     }   
  
     private static void Login() {
         BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
         Map<String, String> credentials = new HashMap<>();
      
         try {
             if (!USER_INFO.exists()) {
                 TextEngine.printWithDelay("User info file not found. Unable to log in.", true);
                 return;
             }
      
             // Read user info into a map (assuming format: username,password)
             try (BufferedReader fileReader = new BufferedReader(new InputStreamReader(new java.io.FileInputStream(USER_INFO)))) {
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
         BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
         try {
             TextEngine.printWithDelay("Enter a username: ", false);
             String username = reader.readLine().trim();
             TextEngine.printWithDelay("Enter a password: ", false);
             String password = reader.readLine().trim();
      
             // Save to user info file
             try (java.io.FileWriter writer = new java.io.FileWriter(USER_INFO, true)) {
                 writer.write(username+ ", " +password+ "\n");
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
 
     private static void FAQs(){
         
        /* TextEngine.printWithDelay("here are some questions that you could ask.");
         System.out.println("======================================");
 
         TextEngine.printWithDelay("What classes should I take next semester based on my major?");
         TextEngine.printWithDelay("----------------");
         TextEngine.printWithDelay("How can I drop or withdraw from a class?");
         TextEngine.printWithDelay("----------------");
         TextEngine.printWithDelay("What happens if I fail a course? Can I retake it?");
         TextEngine.printWithDelay("----------------");
         TextEngine.printWithDelay("How do I declare or change my major or minor?");
         TextEngine.printWithDelay("----------------");
         TextEngine.printWithDelay("Where can I find my degree plan or academic progress report?");
         TextEngine.printWithDelay("----------------");
         TextEngine.printWithDelay("How do I schedule a meeting with my academic advisor?");
         TextEngine.printWithDelay("----------------");
         TextEngine.printWithDelay("What academic support services are available at ACU?");
         TextEngine.printWithDelay("----------------");
         TextEngine.printWithDelay("When are the deadlines for registration, add/drop, or graduation?");
         TextEngine.printWithDelay("----------------");
         TextEngine.printWithDelay("Who do I contact for help with financial aid or billing questions?");
 
         System.out.println("======================================");*/
 
     }
 }