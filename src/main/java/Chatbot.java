/*
 * Albert Tucker 
 * CS375
 * ChatBot for classes
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
  
     private static OpenAiAssistantEngine assistantSelfCare;
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
     private static final int RUN_TIMEOUT_SECONDS = 90;
     private static String usersName;
  
  
     public static void main(String[] args) {
         loadQuestionHistory();
         
         //usersName = parseUserInfo();
         assistantSelfCare = new OpenAiAssistantEngine(APIKEY);
         
  
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
  
         assistantSelfCare = new OpenAiAssistantEngine(APIKEY);
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
         assistant = assistantSelfCare;
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
      
         if (assistantId == null) {
             TextEngine.printWithDelay("Failed to create assistant", true);
             return null;
         }
 
      // Upload files to OpenAI
         String fileId = assistantSelfCare.uploadFile(USER_INFO_FILE, "assistants");
         String fileId1 = assistantSelfCare.uploadFile(ACU_DATABASE_FILE, "assistants");
      
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
   
         //boolean updateSuccess = assistantSelfCare.updateAssistant(assistantId,toolResources);
         boolean updateSuccess = assistant.modifyAssistant(assistantId, null, null, null, null, null, null, null, null, toolResources, null, null);
         
         if (!updateSuccess) {
             TextEngine.printWithDelay("Failed to update assistant with vector store", true);
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
                     threadId = assistantSelfCare.createThread(messages, null, null);
                     if (threadId == null) {
                         TextEngine.printWithDelay("Failed to create thread. Please try again.", true);
                         sessionTimer.reset();
                         continue;
                     }
                 } else {
                     // Add message to existing thread
                     String messageId = assistantSelfCare.addMessageToThread(threadId, userInput);
                     if (messageId == null) {
                         TextEngine.printWithDelay("Failed to send message. Please try again.", true);
                         sessionTimer.reset();
                         continue;
                     }
                 }
   
                 // Create and monitor run
                 /*String runId = assistantSelfCare.createRun(
                     threadId,
                     assistantId,
                     null, null, null, null, null, null, null, null, null, null,
                     null, null, null, null, null, null
                 );*/
   
                 if (runId == null) {
                     TextEngine.printWithDelay("Failed to create run. Please try again.", true);
                     sessionTimer.reset();
                     continue;
                 }
   
                 AtomicBoolean isRunning = new AtomicBoolean(true);
                 Thread loadingThread = startLoadingAnimation(isRunning);
   
                 boolean completed = assistantSelfCare.waitForRunCompletion(threadId, runId, 60,1000);
   
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
         
         TextEngine.printWithDelay("Here are some FAQ's based on different departments in the school:", false);
         TextEngine.printWithDelay("Section 1. Student Sucess FAQ", false);
         TextEngine.printWithDelay("Section 2. Your Personal FAQ", false);
         TextEngine.printWithDelay("Section 3. Academic FAQ", false);
         TextEngine.printWithDelay("Section 4. ACU IT FAQ", false);
         TextEngine.printWithDelay("Please type the department number you would like to view (e.g., '1'), or type 'skip' to go directly to the chatbot.", false);
          
         TextEngine.printWithDelay("Section: ", true);
         String selection = reader.readLine().trim().toLowerCase();
         if (selection.equals("1") || selection.contains("academic")) {
              TextEngine.printWithDelay("\n--- FAQs for Academic Advisor ---", false);
              TextEngine.printWithDelay("1. How do I check my current/upcoming registrations?", false);
              TextEngine.printWithDelay("2. When can I access my upcoming courses in Canvas?", false);
              TextEngine.printWithDelay("3. When do the next courses start?", false);
              TextEngine.printWithDelay("4. What if I need to change or drop an upcoming course?", false);
              TextEngine.printWithDelay("5. When will final grades be posted?", false);
              TextEngine.printWithDelay("6. What happens if I fail my course(s)?", false);
              TextEngine.printWithDelay("7. What happens if I’m not financially clear for the next term?", false);
              TextEngine.printWithDelay("8. How can I reach my Financial Intake Specialist (FIS) about financial concerns?", false);
              TextEngine.printWithDelay("9. How do I reach out to technical support?", false);
              TextEngine.printWithDelay("\nPlease enter the question number (1 through 9), type 'chat' to begin chatting, or 'back' to return to departments.", false);
             
              while (true) {
                   TextEngine.printWithDelay("FAQ #: ", true);
                   String faqInput = reader.readLine().trim().toLowerCase();
             
                   switch (faqInput) {
                        case "1":
                        TextEngine.printWithDelay("To check on upcoming/current registrations, please navigate to Degree Works and scroll to the bottom. It will show you your current and registered classes for the next term. To access Degree Works, please go to my.acu.edu, and in the search bar, please type in \"Degree Works\".", false);
                        break;
                        case "2":
                             TextEngine.printWithDelay("You can only access your upcoming courses in Canvas when the professor decides to publish them. Most of the time, you will have access a day or two before the start day of the semester.", false);
                             break;
                        case "3":
                             TextEngine.printWithDelay("The next courses start depending on when you registered for them. If you signed up for summer classes they will start during the summer term. If you signed up for classes during the fall or spring term they will start when you get there. You can check the specific day by going to my.acu.edu and searching for \"First day of classes\".", false);
                             break;
                        case "4":
                             TextEngine.printWithDelay("If you need to drop or change a course, you will need to reach out to your academic advisor and ask them to either drop or change a course. If you drop a course within the first week of the start of the semester (Monday-Friday before 5:00 PM) then you will get a full refund for that course and your transcript will be unaffected. If you decide to change a class to another class during the first week, your transcript will also be unaffected.", false);
                             break;
                        case "5":
                             TextEngine.printWithDelay("Final grades will be posted within 1-2 weeks of the semester concluding. If you go to my.acu.edu and in the search bar type banner, you will have access to see your unofficial transcript and see your grades. Once there in the search bar, type in \"transcript\" and you will see. The transcript will only show a letter grade, so if you wish to see your numerical grade then please navigate to Canvas. Once there, please select courses and on the top right select \"view all courses\". Here you can see all the previous courses you took and what numerical value you achieved in them.", false);
                             break;
                        case "6":
                             TextEngine.printWithDelay("If you fail your course(s) they will count as an F on your transcript and carry a weight of 0 on the 4.0 scale. Regardless of it being a major class, university requirement, or an elective, you would not get credit for the class or classes and would have to retake them. ACU's policy is that you have 3 attempts to retake a class. Every time that you retake it whatever grade you make on the end will replace the current one. Also, ACU will keep the highest score automatically. So if you get a B in a class, decide to retake it for an A and get a C instead you will keep the B.", false);
                             break;
                        case "7":
                             TextEngine.printWithDelay("If you are not financially clear for the next term, then you need to call Student Services at 325-674-2300. They will tell you the next steps and ultimately, if you aren't able to pay, you will be removed from the school.", false);
                             break;
                        case "8":
                             TextEngine.printWithDelay("Along with an academic advisor, each student is assigned a financial advisor as well. To reach them, please call Wildcat Central at 325-674-6770 and ask them who it is.", false);
                             break;
                        case "9":
                             TextEngine.printWithDelay("To reach ACU technical support, please call 325-674-5555. They will assist you with whatever technical problems you have.", false);
                             break;
                        case "chat":
                             TextEngine.printWithDelay("\nEntering chatbot mode...", false);
                             break;
                        case "back":
                             return;
                        default:
                             TextEngine.printWithDelay("Please enter a valid number (1–9), 'chat', or 'back'.", true);
                             continue;
                         }
             
                         if (faqInput.equals("chat")) break;
                         
                         TextEngine.printWithDelay("\nYou can type another FAQ number, 'chat' to begin chatting, or 'exit' to quit the chatbot.", true);
                         break;
                     }
             
                     //break; // exit FAQ loop to continue to chatbot
                     
                 }
                 else if (selection.equals("2")) {
                     TextEngine.printWithDelay("\n--- Your Personal FAQ ---", false);
                     List<String> faqList = new ArrayList<>();
                 
                     if (!PERSONAL_FAQ_FILE.exists()) {
                         TextEngine.printWithDelay("You have no personal FAQs yet.", false);
                     } else {
                         try (BufferedReader faqReader = new BufferedReader(new FileReader(PERSONAL_FAQ_FILE))) {
                             String line;
                             int i = 1;
                             while ((line = faqReader.readLine()) != null) {
                                 faqList.add(line.substring(2)); // remove "- " prefix
                                 TextEngine.printWithDelay(i + ". " + line.substring(2), false);
                                 i++;
                             }
                         } catch (IOException e) {
                         TextEngine.printWithDelay("Failed to load personal FAQ: " + e.getMessage(), false);
                         }
                 
                         if (!faqList.isEmpty()) {
                             while (true) {
                                 TextEngine.printWithDelay("\nType the number of a question you'd like the advisor to answer, or type 'back': ", true);
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
                                             TextEngine.printWithDelay("Failed to create run.", false);
                                             continue;
                                         }
                 
                                         boolean completed = assistant.waitForRunCompletion(threadId, runId, 60, 1000);
                 
                                         if (!completed) {
                                             TextEngine.printWithDelay("The assistant encountered an issue. Please try again.", false);
                                             continue;
                                         }
                 
                                         List<String> replies = assistant.listMessages(threadId, runId);
                                         if (replies != null && !replies.isEmpty()) {
                                             TextEngine.printWithDelay("\nAdvisor: " + replies.get(0), false);
                                         } else {
                                             TextEngine.printWithDelay("Advisor had no response.", false);
                                         }
                 
                                     } else {
                                         TextEngine.printWithDelay("Invalid number.", false);
                                     }
                                 } catch (NumberFormatException e) {
                                     TextEngine.printWithDelay("Please enter a number or 'back'.", false);
                                 }
                             }
                         }
                     }
                 }
                 else if (selection.equals("3")) {
                      TextEngine.printWithDelay("\n--- FAQs for Academic Advisor (On-Campus) ---", false);
                      TextEngine.printWithDelay("1. How do I calculate my GPA?", false);
                      TextEngine.printWithDelay("2. What will my GPA be next semester if I make these certain grades?", false);
                      TextEngine.printWithDelay("3. What are the prerequisites for this class?", false);
                      TextEngine.printWithDelay("4. What classes should I take next semester?", false);
                      TextEngine.printWithDelay("5. What are all the requirements for my major?", false);
                      TextEngine.printWithDelay("6. What Summer classes are good options for me to take at another school and transfer in?", false);
                      TextEngine.printWithDelay("\nPlease enter the question number (1 through 6), type 'chat' to begin chatting, or 'back' to return to departments.", false);
                  
                      while (true) {
                          TextEngine.printWithDelay("FAQ #: ", true);
                          String academicFaq = reader.readLine().trim().toLowerCase();
                  
                          switch (academicFaq) {
                              case "1":
                                  TextEngine.printWithDelay("You can calculate your GPA by adding up all the grade points you've earned and dividing by the total number of credit hours. Check with Degree Works for your current GPA data.", false);
                                  break;
                              case "2":
                                  TextEngine.printWithDelay("To estimate your GPA for next semester, assume letter grades for each course and apply ACU's grade point scale to see how it impacts your cumulative average.", false);
                                  break;
                              case "3":
                                  TextEngine.printWithDelay("Prerequisites vary by course. You can look up a specific course in the ACU course catalog to see what prerequisites are listed.", false);
                                  break;
                              case "4":
                                  TextEngine.printWithDelay("Course selection depends on your degree audit and what requirements are still pending. Your advisor can help pick classes that align with your graduation timeline.", false);
                                  break;
                              case "5":
                                  TextEngine.printWithDelay("All major requirements are listed in your Degree Works audit. It will show you completed, in-progress, and remaining requirements.", false);
                                  break;
                              case "6":
                                  TextEngine.printWithDelay("General Education and elective classes are good options for summer transfer. Always confirm with your advisor before registering at another school.", false);
                                  break;
                              case "chat":
                                  TextEngine.printWithDelay("\nEntering chatbot mode...", false);
                                  break;
                              case "back":
                                  return;
                              default:
                                  TextEngine.printWithDelay("Please enter a valid number (1 through 6), 'chat', or 'back'.", true);
                                  continue;
                          }
                  
                          if (academicFaq.equals("chat")) break;
                          TextEngine.printWithDelay("\nYou can type another FAQ number, 'chat' to begin chatting, or 'back' to return.", true);
                      }
                  }
                  else if (selection.equals("4")) {
                     TextEngine.printWithDelay("\n--- FAQs for ACU IT ---", false);
                     TextEngine.printWithDelay("1. How do I reset my password?", false);
                     TextEngine.printWithDelay("2. How do I install lockdown browser?", false);
                     TextEngine.printWithDelay("3. How do I set up my Wi-Fi?", false);
                     TextEngine.printWithDelay("4. How do I print?", false);
                     TextEngine.printWithDelay("5. How to register a non computer/laptop devide to the network?", false);
                     TextEngine.printWithDelay("\nPlease enter the question number (1–5), type 'chat' to begin chatting, or 'back' to return to departments.", false);
 
                     while (true) {
                         TextEngine.printWithDelay("FAQ #: ", true);
                         String itFaq = reader.readLine().trim().toLowerCase();
 
                         switch (itFaq) {
                             case "1":
                                 TextEngine.printWithDelay("To reset your password, go to acu.edu/password. Then log in with your ACU credentials to which it will prompt you to change your password. If you are having trouble, please call ACU IT at 325-674-5555.", false);
                                 break;
                             case "2":
                                 TextEngine.printWithDelay("To download LockDown Browser to your computer, go to: https://download.respondus.com/lockdown/download.php?id=167846866. This is an ACU specific link and you must use this link to download LockDown Browser for an ACU class.", false);
                                 break;
                             case "3":
                                 TextEngine.printWithDelay("To join any ACU Wi-Fi network, ACUSecure or ACUGuest, go to your settings and select the network. It will then prompt you to enter your username and password. Your username is your ACU email (without @acu.edu, i.e abc21c) and your password is the same password you use to log into my.acu.edu. If you are having trouble, please call ACU IT at 325-674-5555.", false);
                                 break;
                             case "4":
                                 TextEngine.printWithDelay("If you want to print from a personal device to any of the printers in the residence halls or labs, go to acu.edu/print. You can log in with your ACU username (without the @acu.edu, i.e abc21c) and password. From here you will be able to upload documents directly. To change the printer you print to you can select it from the drop-down box in the lower right hand corner. You will need to search the printer in the list by typing in the name of the printer, which can be found on a label on each printer.", false);
                                 break;
                             case "5":
                                 TextEngine.printWithDelay("To start please go here: https://clearpass.acu.edu/guest/auth_login.php?target=%2Fguest%2Fmac_create.php. This is the ACU specific link to register a device. Once there, please select \"Register a Device\" and fill out the form. The MAC address is the physical address of the device and can be found in the settings of the device. If you are having trouble, please call ACU IT at 325-674-5555.", false);
                                 break;
                             case "chat":
                                 TextEngine.printWithDelay("\nEntering chatbot mode...", false);
                                 break;
                             case "back":
                                 return;
                             default:
                                 TextEngine.printWithDelay("Please enter a valid number (1–5), 'chat', or 'back'.", true);
                                 continue;
                         }
 
                         if (itFaq.equals("chat")) break;
                         TextEngine.printWithDelay("\nYou can type another FAQ number, 'chat' to begin chatting, or 'back' to return.", true);
                     }
                 }
                 //else if (selection.equals("skip")) {
                     //break; // skip FAQ
                 //} 
                 else {
                     TextEngine.printWithDelay("Invalid input. Type '1', '2', or 'skip'.", true);
                 }
             
     }
 }
