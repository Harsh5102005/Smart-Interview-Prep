# Smart Interview Prep

Smart Interview Prep is a full-stack AI interview practice application. It helps users prepare for interviews by generating role-based questions, accepting typed or voice answers, giving AI-powered feedback, scoring answers, and saving answer history.

## Features

- User registration and login
- Start interview sessions by role and difficulty
- Difficulty levels: easy, medium, hard, and advanced
- AI-generated interview questions
- Typed answer input
- Voice answer input using the browser Web Speech API
- AI-based feedback and score out of 10
- User-wise answer history
- Clear answer history option
- React frontend connected to a Spring Boot REST API
- MySQL database persistence

## Tech Stack

### Backend

- Java 21
- Spring Boot
- Spring Web MVC
- Spring Data JPA
- MySQL
- Maven
- Lombok

### Frontend

- React
- JavaScript
- CSS
- Web Speech API

### AI

- Groq Chat Completions API
- Default model: `llama-3.1-8b-instant`

## Project Structure

```text
.
|-- src/main/java/com/interview/ai
|   |-- controller
|   |-- dto
|   |-- entities
|   |-- repository
|   `-- service
|-- src/main/resources
|   `-- application.properties
|-- src/test
|-- ai-interview-frontend
|   |-- public
|   |-- src
|   |-- package.json
|   `-- package-lock.json
|-- .env.example
|-- pom.xml
|-- mvnw
|-- mvnw.cmd
`-- README.md
```

## Requirements

- Java 21
- Node.js and npm
- MySQL
- Groq API key
- Maven, or the included Maven wrapper

## Environment Variables

Do not hardcode real secrets in the project. Use environment variables instead.

Backend variables:

```text
GROQ_API_KEY=your_groq_api_key
GROQ_API_URL=https://api.groq.com/openai/v1/chat/completions
GROQ_API_MODEL=llama-3.1-8b-instant
SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/ai_interview
SPRING_DATASOURCE_USERNAME=root
SPRING_DATASOURCE_PASSWORD=your_mysql_password
```

Frontend variable:

```text
REACT_APP_API_BASE_URL=http://localhost:8080
```

Sample files are included:

- `.env.example`
- `ai-interview-frontend/.env.example`

## Database Setup

Create the MySQL database:

```sql
CREATE DATABASE ai_interview;
```

The backend uses:

```properties
spring.jpa.hibernate.ddl-auto=update
```

This allows Spring Boot to create and update tables automatically during development.

## Run the Backend

Open PowerShell in the project root:

```powershell
cd E:\spring\ai
```

Set environment variables:

```powershell
$env:GROQ_API_KEY="your_groq_api_key"
$env:SPRING_DATASOURCE_URL="jdbc:mysql://localhost:3306/ai_interview"
$env:SPRING_DATASOURCE_USERNAME="root"
$env:SPRING_DATASOURCE_PASSWORD="your_mysql_password"
```

Start the backend:

```powershell
.\mvnw.cmd spring-boot:run
```

Backend URL:

```text
http://localhost:8080
```

## Run the Frontend

Open another PowerShell window:

```powershell
cd E:\spring\ai\ai-interview-frontend
```

Install dependencies:

```powershell
npm install
```

Start React:

```powershell
npm start
```

Frontend URL:

```text
http://localhost:3000
```

## API Endpoints

### Auth

```text
POST /api/auth/signup
POST /api/auth/login
```

### Interview

```text
POST /api/interview/start
```

### Question

```text
POST /api/question
```

### Answer

```text
POST /api/answer
```

### History

```text
GET    /api/history/{userId}
DELETE /api/history/{userId}
```

## Example API Requests

### Signup

```json
{
  "name": "Harsh",
  "email": "harsh@example.com",
  "password": "password123"
}
```

### Start Interview

```json
{
  "userId": "1",
  "role": "Java developer",
  "difficulty": "medium"
}
```

### Generate Question

```json
{
  "interviewId": "1"
}
```

### Submit Answer

```json
{
  "questionId": 1,
  "answerText": "JPA is used to map Java objects to database tables and manage persistence."
}
```

## Run Tests

Backend tests:

```powershell
cd E:\spring\ai
.\mvnw.cmd test
```

Frontend tests:

```powershell
cd E:\spring\ai\ai-interview-frontend
npm test
```

## GitHub Push Commands

If this project is already connected to your GitHub repository, use:

```powershell
cd E:\spring\ai
git status
git add README.md
git commit -m "Update README"
git push
```

Repository:

```text
https://github.com/Harsh5102005/Smart-Interview-Prep
```

## Security Notes

- Do not commit real API keys, database passwords, `.env` files, logs, `node_modules`, or build output.
- The old Groq key that was previously hardcoded should be rotated or deleted from Groq.
- User passwords are currently stored as plain text. Password hashing should be added before using this project in production.
- The backend currently allows all CORS origins for development convenience.

## Future Improvements

- Add Spring Security
- Hash passwords with BCrypt
- Add JWT authentication
- Add admin dashboard
- Add interview analytics
- Add deployment configuration
- Add global exception handling
- Add better validation messages
- Add Docker support

## Author

Harsh
