# PurpleSoxs Chatbot Project

# AI Academic Advisor Chatbot

This is a Java-based console chatbot that uses OpenAI’s GPT API to act as an academic advisor for Abilene Christian University students. It dynamically pulls information from a student profile (`user_info.txt`) and an optional local SQLite database.

## 🚀 Features

- Real-time conversation with a GPT-powered academic advisor  
- Uses student data from `user_info.txt`  
- Supports academic queries like:  
  - "What classes am I currently in?"  
  - "What’s my GPA?"  
  - "How many credits do I need to graduate?"  
- Designed with a modular engine (`OpenAiAssistantEngine`) for easy integration

## 📁 Project Structure

```
.
├── Chatbot.java                 # Main class to run the chatbot
├── OpenAiAssistantEngine.java  # Engine for handling OpenAI API interactions
├── user_info.txt               # Student profile file
├── db/
│   └── acu_database.db         # (Optional) SQLite database with academic data
├── pom.xml                     # Maven configuration file
```

## ✅ Requirements

- Java 19 or higher  
- Maven  
- A valid OpenAI API key  

## 🔧 Setup & Run

1. **Clone or download this repository**

2. **Add your OpenAI API key**  
   Open `Chatbot.java` and set the `APIKEY` constant:
   ```java
   private static final String APIKEY = "your-api-key-here";
   ```

3. **Run with Maven**
   ```bash
   mvn compile exec:java
   ```

4. **Start chatting**
   You'll be prompted in the terminal:
   ```
   Welcome to the ACU Academic Advisor Chatbot!
   Type your question (or type 'exit' to quit):
   ```

## 💡 Example Questions

- What classes am I currently in?  
- Who is my advisor?  
- How many credits do I have left?  
- When will I graduate?  

## 🧠 How It Works

- The chatbot initializes an assistant using your API key  
- It loads student information from `user_info.txt` and optionally from `acu_database.db`  
- Each user question is sent to OpenAI with that context  
- Responses are shown in the terminal  

## 📜 License

MIT License

## 👤 Authors

- Michael Aghassi 
- Albert Tucker Jr.
