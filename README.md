# OpenAI Text Processor

A Spring Boot application that uses OpenAI for fixing spelling and translating files.

## Features

- File upload with drag-and-drop support
- Spell checking and grammar correction using OpenAI
- File translation to multiple languages
- Processing history
- Download processed files

## Requirements

- Java 17+
- Maven
- OpenAI API Key

## Setup

1. Clone the repository
2. Set your OpenAI API key in `src/main/resources/application.properties`:
   ```
   openai.api.key=YOUR_OPENAI_API_KEY
   ```
3. Build the project:
   ```
   mvn clean package
   ```
4. Run the application:
   ```
   java -jar target/openai-text-processor-0.0.1-SNAPSHOT.jar
   ```
5. Access the application at `http://localhost:8080`

## Usage

1. Upload a text file using the drag-and-drop area or file browser
2. Select an operation (Fix Spelling & Grammar or Translate)
3. If translating, select a target language
4. Click "Process File"
5. View and download the processed result
6. Review processing history at the bottom of the page

## Tech Stack

- Spring Boot 3.2.3
- OpenAI API (GPT-4)
- Thymeleaf
- Tailwind CSS
- JavaScript