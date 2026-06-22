import React, { useCallback, useEffect, useMemo, useState } from "react";
import "./index.css";

const API_BASE_URL = process.env.REACT_APP_API_BASE_URL || "http://localhost:8080";

function App() {
  const [authMode, setAuthMode] = useState("login");
  const [currentUser, setCurrentUser] = useState(() => {
    const savedUser = localStorage.getItem("currentUser");
    return savedUser ? JSON.parse(savedUser) : null;
  });
  const [authForm, setAuthForm] = useState({
    name: "",
    email: "",
    password: "",
  });
  const [authError, setAuthError] = useState("");
  const [isAuthenticating, setIsAuthenticating] = useState(false);
  const [interviewId, setInterviewId] = useState("");
  const [role, setRole] = useState("Java developer");
  const [difficulty, setDifficulty] = useState("medium");
  const [question, setQuestion] = useState("");
  const [questionId, setQuestionId] = useState("");
  const [answer, setAnswer] = useState("");
  const [feedback, setFeedback] = useState("");
  const [score, setScore] = useState("");
  const [error, setError] = useState("");
  const [isStartingInterview, setIsStartingInterview] = useState(false);
  const [isLoadingQuestion, setIsLoadingQuestion] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isListening, setIsListening] = useState(false);
  const [history, setHistory] = useState([]);
  const [isLoadingHistory, setIsLoadingHistory] = useState(false);
  const [isClearingHistory, setIsClearingHistory] = useState(false);

  const wordCount = useMemo(() => {
    return answer.trim() ? answer.trim().split(/\s+/).length : 0;
  }, [answer]);

  const scorePercent = score === "" ? 0 : Math.max(0, Math.min(100, score * 10));

  const loadHistory = useCallback(async (userId = currentUser?.id) => {
    if (!userId) {
      return;
    }

    setIsLoadingHistory(true);

    try {
      const res = await fetch(`${API_BASE_URL}/api/history/${userId}`);

      if (!res.ok) {
        const message = await res.text();
        throw new Error(message || "Could not load answer history.");
      }

      const data = await res.json();
      setHistory(Array.isArray(data) ? data : []);
    } catch (err) {
      setError(err.message);
    } finally {
      setIsLoadingHistory(false);
    }
  }, [currentUser?.id]);

  const clearHistory = async () => {
    if (!currentUser?.id || history.length === 0) {
      return;
    }

    setIsClearingHistory(true);
    setError("");

    try {
      const res = await fetch(`${API_BASE_URL}/api/history/${currentUser.id}`, {
        method: "DELETE",
      });

      if (!res.ok) {
        const message = await res.text();
        throw new Error(message || "Could not clear answer history.");
      }

      setHistory([]);
    } catch (err) {
      setError(err.message);
    } finally {
      setIsClearingHistory(false);
    }
  };

  useEffect(() => {
    if (currentUser?.id) {
      loadHistory(currentUser.id);
    }
  }, [currentUser?.id, loadHistory]);


  const updateAuthForm = (field, value) => {
    setAuthForm((current) => ({
      ...current,
      [field]: value,
    }));
  };

  const submitAuth = async (event) => {
    event.preventDefault();
    setIsAuthenticating(true);
    setAuthError("");

    try {
      const endpoint = authMode === "login" ? "login" : "signup";
      const payload =
        authMode === "login"
          ? { email: authForm.email, password: authForm.password }
          : authForm;

      const res = await fetch(`${API_BASE_URL}/api/auth/${endpoint}`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(payload),
      });

      if (!res.ok) {
        const message = await res.text();
        throw new Error(message || "Authentication failed.");
      }

      const user = await res.json();
      setCurrentUser(user);
      localStorage.setItem("currentUser", JSON.stringify(user));
      setAuthForm({
        name: "",
        email: "",
        password: "",
      });
    } catch (err) {
      setAuthError(err.message);
    } finally {
      setIsAuthenticating(false);
    }
  };

  const logout = () => {
    setCurrentUser(null);
    localStorage.removeItem("currentUser");
    clearInterviewSession();
    setHistory([]);
    setError("");
  };

  const clearInterviewSession = () => {
    setInterviewId("");
    setQuestion("");
    setQuestionId("");
    setAnswer("");
    setFeedback("");
    setScore("");
  };

  const speak = (text) => {
    if (!text || !window.speechSynthesis) {
      return;
    }

    window.speechSynthesis.cancel();
    const speech = new SpeechSynthesisUtterance(text);
    speech.lang = "en-US";
    window.speechSynthesis.speak(speech);
  };

  const startInterview = async () => {
    setIsStartingInterview(true);
    setError("");

    try {
      const res = await fetch(`${API_BASE_URL}/api/interview/start`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ role, difficulty, userId: currentUser.id }),
      });

      if (!res.ok) {
        const message = await res.text();
        throw new Error(message || "Could not start the interview.");
      }

      const data = await res.json();
      setInterviewId(data.id || "");
      setQuestion("");
      setQuestionId("");
      setAnswer("");
      setFeedback("");
      setScore("");
    } catch (err) {
      setError(err.message);
    } finally {
      setIsStartingInterview(false);
    }
  };

  const getQuestion = async () => {
    if (!interviewId) {
      setError("Choose a role and difficulty before loading questions.");
      return;
    }

    setIsLoadingQuestion(true);
    setError("");

    try {
      const res = await fetch(`${API_BASE_URL}/api/question`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ interviewId }),
      });

      if (!res.ok) {
        const message = await res.text();
        throw new Error(message || "Could not load a question.");
      }

      const data = await res.json();
      setQuestion(data.questionText || "");
      setQuestionId(data.id || "");
      setAnswer("");
      setFeedback("");
      setScore("");

      speak(data.questionText);
    } catch (err) {
      setError(err.message);
    } finally {
      setIsLoadingQuestion(false);
    }
  };

  const startListening = () => {
    const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
    if (!SpeechRecognition) {
      setError("Speech recognition is not supported in this browser. Type your answer instead.");
      return;
    }

    setError("");
    setIsListening(true);

    const recognition = new SpeechRecognition();
    recognition.lang = "en-US";
    recognition.interimResults = false;
    recognition.maxAlternatives = 1;

    recognition.onresult = (event) => {
      const transcript = event.results[0][0].transcript;
      setAnswer((current) => `${current}${current ? " " : ""}${transcript}`.trim());
    };

    recognition.onerror = () => {
      setError("Could not capture your voice. Try again or type the answer.");
    };

    recognition.onend = () => {
      setIsListening(false);
    };

    recognition.start();
  };

  const submitAnswer = async () => {
    if (!questionId) {
      setError("Start the interview before submitting an answer.");
      return;
    }

    setIsSubmitting(true);
    setError("");
    setFeedback("");
    setScore("");

    try {
      const res = await fetch(`${API_BASE_URL}/api/answer`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          questionId,
          answerText: answer,
        }),
      });

      if (!res.ok) {
        const message = await res.text();
        throw new Error(message || "Could not submit the answer.");
      }

      const data = await res.json();
      setFeedback(data.feedback || "");
      setScore(Number.isFinite(data.score) ? data.score : 0);
      loadHistory();

      if (data.feedback) {
        speak(data.feedback);
      }
    } catch (err) {
      setError(err.message);
    } finally {
      setIsSubmitting(false);
    }
  };

  if (!currentUser) {
    return (
      <main className="auth-shell">
        <section className="auth-panel">
          <div>
            <p className="eyebrow">AI Interview Console</p>
            <h1>{authMode === "login" ? "Login to continue." : "Create your account."}</h1>
          </div>

          <div className="auth-tabs" aria-label="Authentication mode">
            <button
              className={authMode === "login" ? "auth-tab active" : "auth-tab"}
              onClick={() => {
                setAuthMode("login");
                setAuthError("");
              }}
              type="button"
            >
              Login
            </button>
            <button
              className={authMode === "signup" ? "auth-tab active" : "auth-tab"}
              onClick={() => {
                setAuthMode("signup");
                setAuthError("");
              }}
              type="button"
            >
              Register
            </button>
          </div>

          <form className="auth-form" onSubmit={submitAuth}>
            {authMode === "signup" && (
              <label className="field">
                <span>Name</span>
                <input
                  value={authForm.name}
                  onChange={(event) => updateAuthForm("name", event.target.value)}
                  placeholder="Your name"
                />
              </label>
            )}

            <label className="field">
              <span>Email</span>
              <input
                type="email"
                value={authForm.email}
                onChange={(event) => updateAuthForm("email", event.target.value)}
                placeholder="you@example.com"
              />
            </label>

            <label className="field">
              <span>Password</span>
              <input
                type="password"
                value={authForm.password}
                onChange={(event) => updateAuthForm("password", event.target.value)}
                placeholder="Password"
              />
            </label>

            {authError && <div className="error-banner">{authError}</div>}

            <button className="primary-button auth-submit" disabled={isAuthenticating}>
              {isAuthenticating ? "Please wait..." : authMode === "login" ? "Login" : "Register"}
            </button>
          </form>
        </section>
      </main>
    );
  }

  return (
    <main className="app-shell">
      <section className="workspace">
        <header className="topbar">
          <div>
            <p className="eyebrow">AI Interview Console</p>
            <h1>Practice answers with instant scoring.</h1>
          </div>
          <div className="topbar-actions">
            <div className="user-pill">Hi, {currentUser.name}</div>
            <div className="status-pill">
              <span className={interviewId ? "status-dot active" : "status-dot"} />
              {interviewId ? `${difficulty} interview` : "Setup required"}
            </div>
            <button className="ghost-button" onClick={logout}>
              Logout
            </button>
          </div>
        </header>

        <section className="setup-panel">
          <div className="section-heading">
            <span>Interview Setup</span>
            {interviewId && <span className="meta-text">Session #{interviewId}</span>}
          </div>
          <div className="setup-grid">
            <label className="field">
              <span>Role</span>
              <input
                value={role}
                onChange={(event) => {
                  setRole(event.target.value);
                  clearInterviewSession();
                }}
                placeholder="Java developer"
              />
            </label>
            <label className="field">
              <span>Difficulty</span>
              <select
                value={difficulty}
                onChange={(event) => {
                  setDifficulty(event.target.value);
                  clearInterviewSession();
                }}
              >
                <option value="easy">Easy</option>
                <option value="medium">Medium</option>
                <option value="hard">Hard</option>
                <option value="advanced">Advanced</option>
              </select>
            </label>
            <button className="primary-button setup-button" onClick={startInterview} disabled={isStartingInterview}>
              {isStartingInterview ? "Starting..." : interviewId ? "Restart Setup" : "Create Interview"}
            </button>
          </div>
        </section>

        <section className="question-panel">
          <div className="section-heading">
            <span>Question</span>
            <button className="ghost-button" onClick={() => speak(question)} disabled={!question}>
              Replay
            </button>
          </div>
          <p className={question ? "question-text" : "empty-text"}>
            {question || "Create an interview, then load your first question."}
          </p>
          <button className="primary-button" onClick={getQuestion} disabled={!interviewId || isLoadingQuestion}>
            {isLoadingQuestion ? "Loading..." : question ? "Next Question" : "Load Question"}
          </button>
        </section>

        <section className="answer-panel">
          <div className="section-heading">
            <span>Your Answer</span>
            <span className="meta-text">{wordCount} words</span>
          </div>
          <textarea
            className="answer-input"
            value={answer}
            onChange={(event) => setAnswer(event.target.value)}
            placeholder="Type your answer here, or use voice input and refine it before submitting."
            disabled={!questionId || isSubmitting}
          />
          <div className="action-row">
            <button className="secondary-button" onClick={startListening} disabled={!questionId || isListening}>
              {isListening ? "Listening..." : "Voice Input"}
            </button>
            <button className="primary-button" onClick={submitAnswer} disabled={!questionId || isSubmitting}>
              {isSubmitting ? "Checking..." : "Submit Answer"}
            </button>
          </div>
        </section>

        {error && <div className="error-banner">{error}</div>}

        <section className="history-panel">
          <div className="section-heading">
            <span>Answer History</span>
            <div className="history-actions">
              <button className="ghost-button" onClick={() => loadHistory()} disabled={isLoadingHistory}>
                {isLoadingHistory ? "Refreshing..." : "Refresh"}
              </button>
              {history.length > 0 && (
                <button className="danger-button" onClick={clearHistory} disabled={isClearingHistory}>
                  {isClearingHistory ? "Clearing..." : "Clear History"}
                </button>
              )}
            </div>
          </div>

          {history.length === 0 ? (
            <p className="empty-text">Your answered questions will appear here after you submit them.</p>
          ) : (
            <div className="history-list">
              {history.map((item) => (
                <article className="history-card" key={item.id}>
                  <div className="history-card-header">
                    <div>
                      <span className="history-role">{item.role || "Interview"}</span>
                      <span className="history-difficulty">{item.difficulty || "medium"}</span>
                    </div>
                    <strong>{item.score}/10</strong>
                  </div>
                  <p className="history-question">{item.question}</p>
                  <div className="history-block">
                    <span>Your answer</span>
                    <p>{item.answer || "No answer text saved."}</p>
                  </div>
                  <div className="history-block feedback">
                    <span>Feedback</span>
                    <p>{item.feedback || "No feedback saved."}</p>
                  </div>
                </article>
              ))}
            </div>
          )}
        </section>
      </section>

      <aside className="score-panel">
        <div className="score-header">
          <span>Feedback</span>
          <strong>{score === "" ? "--" : `${score}/10`}</strong>
        </div>
        <div className="score-ring" style={{ "--score": `${scorePercent}%` }}>
          <span>{score === "" ? "--" : score}</span>
        </div>
        <div className="score-track">
          <span style={{ width: `${scorePercent}%` }} />
        </div>
        <p className={feedback ? "feedback-text" : "empty-text"}>
          {feedback || "Submit an answer to see score, feedback, and improvement guidance."}
        </p>
      </aside>
    </main>
  );
}

export default App;
