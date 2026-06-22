# AI Interview Practice Platform

AI Interview Practice Platform is a full-stack web application for practicing interview answers. Users can create an account, start an interview for a selected role and difficulty, receive AI-generated questions, submit typed or voice answers, get instant AI feedback with a score, and review answer history.

## Features

- User signup and login
- Role-based interview sessions
- Difficulty levels: easy, medium, hard, and advanced
- AI-generated interview questions
- Typed and voice answer input
- AI feedback and scoring out of 10
- Answer history per user
- Clear answer history option

## Tech Stack

Backend:

- Java 21
- Spring Boot
- Spring Web MVC
- Spring Data JPA
- MySQL
- Maven
- Lombok

Frontend:

- React
- JavaScript
- CSS
- Web Speech API

AI:

- Groq Chat Completions API
- Default model: `llama-3.1-8b-instant`

## Project Structure

```text
.
├── src/main/java/com/interview/ai
│   ├── controller
│   ├── dto
│   ├── entities
│   ├── repository
│   └── service
├── src/main/resources
│   └── application.properties
├── src/test
├── ai-interview-frontend
│   ├── public
│   ├── src
│   └── package.json
└── pom.xml
```

## Requirements

- Java 21
- Maven or the included Maven wrapper
- Node.js and npm
- MySQL
- Groq API key

## Environment Variables

Create your own environment values before running the app. A sample file is available at `.env.example`.

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

## Database Setup

Create a MySQL database:

```sql
CREATE DATABASE ai_interview;
```

The backend uses `spring.jpa.hibernate.ddl-auto=update`, so Spring Boot creates and updates tables automatically during development.

## Run Backend

From the project root:

```bash
./mvnw spring-boot:run
```

On Windows PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

The backend runs at:

```text
http://localhost:8080
```

## Run Frontend

From the frontend folder:

```bash
cd ai-interview-frontend
npm install
npm start
```

The frontend runs at:

```text
http://localhost:3000
```

## API Endpoints

Auth:

- `POST /api/auth/signup`
- `POST /api/auth/login`

Interview:

- `POST /api/interview/start`

Question:

- `POST /api/question`

Answer:

- `POST /api/answer`

History:

- `GET /api/history/{userId}`
- `DELETE /api/history/{userId}`

## Tests

Run backend tests:

```bash
./mvnw test
```

On Windows PowerShell:

```powershell
.\mvnw.cmd test
```

Run frontend tests:

```bash
cd ai-interview-frontend
npm test
```

## Security Notes

- Do not commit real API keys, database passwords, `.env` files, logs, `node_modules`, or build output.
- User passwords are currently stored as plain text. Add password hashing before using this project in production.
- The backend currently allows all CORS origins for development convenience.

## Future Improvements

- Add Spring Security and password hashing
- Add JWT-based authentication
- Add production deployment configuration
- Add better global exception handling
- Add interview analytics and progress tracking
