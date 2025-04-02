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
 
 import org.json.JSONObject;
 
 public class Chatbot {
     public static String redColor = "\033[1;31m";
     public static String yellowColor = "\033[1;33m";
     public static String greenColor = "\033[1;32m";
     public static String cyanColor = "\033[1;36m";
     public static String blueColor = "\033[1;34m";
     public static String purpleColor = "\033[35m"; ;
     public static String resetColor = "\033[0m";
 
     private static OpenAiAssistantEngine assistantSelfCare;
     private static final String APIKEY = "n0"; // need an key for this to work
     private static final File USER_INFO = new File("user_info.txt"); // maybe try to make it where the user can actuly login
     private static final File ACU_DATABASE = new File("acu_database.txt"); //update this to be better
     private static final int RUN_TIMEOUT_SECONDS = 60;
 
     public static void main(String[] args) {
         BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
 
         System.out.println(purpleColor + "  ----  █████"+resetColor+"╗"+purpleColor+"   ██████"+resetColor+"╗"+purpleColor+"  ██"+resetColor+"╗"+purpleColor+"   ██"+resetColor+"╗  ----" );
         System.out.println(resetColor+" ---   "+purpleColor+"██"+resetColor+"╔══"+purpleColor+"██"+resetColor+"╗"+purpleColor+"  ██"+resetColor+"╔═══╝"+purpleColor+"  ██"+resetColor+"║"+purpleColor+"   ██"+resetColor+"║" +purpleColor+"  ---" );
         System.out.println(purpleColor+" ----  ███████"+resetColor+"║"+purpleColor+"  ██"+resetColor+"║"+purpleColor+"      ██"+resetColor+"║"+purpleColor+"   ██"+resetColor+"║  ----" );
         System.out.println(resetColor+" ---   "+purpleColor+"██"+resetColor+"╔══"+purpleColor+"██"+resetColor+"║"+purpleColor+"  ██"+resetColor+"╚═══╗"+purpleColor+"  ██"+resetColor+"║"+purpleColor+"   ██"+resetColor+"║" +purpleColor+ "  ----" );
         System.out.println(purpleColor+" ----  ██"+resetColor+"║"+purpleColor+"  ██"+resetColor+"║"+purpleColor+"  ██████"+resetColor+"║"+purpleColor+"   ██████"+resetColor+"║  ---" + resetColor);
         System.out.println("═════════════════════════════════════");
         //printRainbowText("══════════════════════════════════════════════"); // I was just missing around, I don't think this should be in the code :)
         try {
             System.out.println("\nWelcome to the ACU AI Academic Advisor!");
             System.out.println("press " +yellowColor+ "(1) Log in" +resetColor+ " or " +greenColor+ "(2) Create" +resetColor+ " a Profile?");
             System.out.print("Enter " +yellowColor+ "1" +resetColor+ " or " +greenColor+ "2" +resetColor+ ": ");
             String choice = reader.readLine().trim();
     
             if (null == choice) {
                 System.out.println("Invalid choice. Please restart and select either 1 or 2.");
                 return;
             } else switch (choice) {
                 case "1" -> Login();
                 case "2" -> {
                     createProfile();
                     System.out.println("Please log in with your new profile.");
                     Login();
                 }
                 default -> {
                     System.out.println("Invalid choice. Please restart and select either 1 or 2.");
                     return;
                 }
             }
         } catch (IOException e) {
             System.out.println("An error occurred: " + e.getMessage()+ "\n, you gone and missed up!!");
             return;
         }
     
         assistantSelfCare = new OpenAiAssistantEngine(APIKEY);
         System.out.println("-------------------------");
         System.out.println("Setting up AI Academic Advisor...");
     
         String assistantId = setupAssistant();
         if (assistantId == null) {
             System.out.println("Failed to set up assistant. Exiting.");
             return;
         }
     
         startInteractiveChat(assistantId);
     }
     
 
     private static String setupAssistant() {
         // Create assistant
         String assistantId = assistantSelfCare.createAssistant(
                 "gpt-4o-mini",
                 "Personal AI Academic Advisor",
                 null,
                 "You are a real-time chat AI Academic Advisor for Abilene Christian University.\n Address the student by their first and last name based on the user info provided.\n Provide information about the student's academic journey, courses, and other academic-related topics.\n Please adress the student by their first and last name from the user info provided.",
                 null,
                 List.of("file_search"),
                 null,
                 0.2,
                 0.1,
                 null
         );
 
         if (assistantId == null) {
             System.out.println("Failed to create assistant");
             return null;
         }
 
         // Upload files to OpenAI
         String fileId = assistantSelfCare.uploadFile(USER_INFO, "assistants");
         String fileId1 = assistantSelfCare.uploadFile(ACU_DATABASE, "assistants");
 
         if (fileId == null || fileId1 == null) {
             System.out.println("Failed to upload one or more files");
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
             System.out.println("Failed to create vector store");
             return null;
         }
 
         // Update assistant with vector store
         Map<String, Object> toolResources = new HashMap<>();
         Map<String, List<String>> fileSearch = new HashMap<>();
         fileSearch.put("vector_store_ids", List.of(vectorStoreId));
         toolResources.put("file_search", fileSearch);
 
         boolean updateSuccess = assistantSelfCare.updateAssistant(
                 assistantId,
                 toolResources
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
 
         System.out.println("\n=== You can now chat with AI ===");
         System.out.println("Type " +redColor+ "exit" +resetColor+ " to end the conversation");
         System.out.println("What would you like help with?");
 
         try {
             String userInput;
             while (true) {
                 System.out.print("\nQuestion: ");
                 userInput = reader.readLine().trim();
                 System.out.println("");
 
                 if (userInput.equalsIgnoreCase("exit")) {
                     System.out.println("Exiting...");
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
                         System.out.println("Failed to create thread. Please try again.");
                         continue;
                     }
                 } else {
                     // Add message to existing thread
                     String messageId = assistantSelfCare.addMessageToThread(threadId, userInput);
                     if (messageId == null) {
                         System.out.println("Failed to send message. Please try again.");
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
                     System.out.println("Failed to create run. Please try again.");
                     continue;
                 }
 
                 boolean completed = assistantSelfCare.waitForRunCompletion(threadId, runId, RUN_TIMEOUT_SECONDS);
 
                 if (!completed) {
                     System.out.println("The assistant encountered an issue. Please try again.");
                     continue;
                 }
 
                 // Get the assistant's response
                 List<String> retrievedMessages = assistantSelfCare.listMessages(threadId, runId);
                 if (retrievedMessages != null && !retrievedMessages.isEmpty()) {
                     System.out.println(retrievedMessages.get(0));
                 } else {
                     System.out.println("No response received. Please try again.");
                 }
             }
 
             System.out.println("\nSession Statistics:");
             assistantSelfCare.getCategories().forEach(category -> {
                 System.out.println(" - " + category + ": "
                         + assistantSelfCare.getResponsesByCategory(category).size()
                         + " responses");
             });
 
             printRainbowText("\nThank you for using the ACU AI Academic Advisor. Goodbye!");
 
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
                 System.out.println("User info file not found. Unable to log in.");
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
             System.out.print("Enter your username: ");
             String username = reader.readLine().trim();
             System.out.print("Enter your password: ");
             String password = reader.readLine().trim();
     
             // Validate credentials
             if (credentials.containsKey(username) && credentials.get(username).equals(password)) {
                 System.out.println(greenColor+"Login successful."+resetColor+"\n Welcome, " + username + "!");
             } else {
                 System.out.println("Invalid username or password. Exiting.");
                 System.exit(1);
             }
         } catch (IOException e) {
             System.out.println("An error occurred during login: " + e.getMessage());
         }
     }
 
     private static void createProfile() {
         BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
         try {
             System.out.print("Enter a username: ");
             String username = reader.readLine().trim();
             System.out.print("Enter a password: ");
             String password = reader.readLine().trim();
     
             // Save to user info file
             try (java.io.FileWriter writer = new java.io.FileWriter(USER_INFO, true)) {
                 writer.write(username+ ", " +password+ "\n");
                 System.out.println(greenColor+"Profile created successfully." +resetColor);
             }
         } catch (IOException e) {
             System.out.println("An error occurred while creating the profile: " + e.getMessage());
         }
     }
 
     public static void printRainbowText(String text) {
         String[] colors = {redColor, yellowColor, greenColor, cyanColor, blueColor, purpleColor};
         for (int i = 0; i < text.length(); i++) {
             System.out.print(colors[i % colors.length] + text.charAt(i));
         }
         System.out.println(resetColor); // Reset to default color
     }
 
     
 }
 