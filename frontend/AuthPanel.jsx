import { useState } from "react";

const API_ORIGIN = (
  import.meta.env.VITE_BASE_API_URL || import.meta.env.VITE_API_URL || ""
).replace(/\/$/, "");

export default function AuthPanel({ onAuthenticated }) {
  const [mode, setMode] = useState("login");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  let submitLabel = "Create account";
  if (busy) submitLabel = "Please wait…";
  else if (mode === "login") submitLabel = "Sign in";

  async function handleSubmit(event) {
    event.preventDefault();
    setBusy(true);
    setError("");

    try {
      const response = await fetch(`${API_ORIGIN}/api/auth/${mode}`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email: email.trim(), password }),
      });
      const result = await response.json().catch(() => ({}));
      if (!response.ok) {
        throw new Error(result.detail || "Unable to authenticate. Check your details and try again.");
      }
      await onAuthenticated(result);
      setPassword("");
    } catch (requestError) {
      setError(requestError.message || "The authentication service is unavailable.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="auth-panel">
      <fieldset className="auth-tabs">
        <legend className="visually-hidden">Account access</legend>
        <button
          type="button"
          className={mode === "login" ? "auth-tab active" : "auth-tab"}
          aria-pressed={mode === "login"}
          onClick={() => { setMode("login"); setError(""); }}
        >
          Sign in
        </button>
        <button
          type="button"
          className={mode === "register" ? "auth-tab active" : "auth-tab"}
          aria-pressed={mode === "register"}
          onClick={() => { setMode("register"); setError(""); }}
        >
          Create account
        </button>
      </fieldset>

      <form className="auth-form" onSubmit={handleSubmit}>
        <label>
          <span>Email</span>
          <input
            type="email"
            autoComplete="email"
            maxLength={254}
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            required
          />
        </label>
        <label>
          <span>Password</span>
          <input
            type="password"
            autoComplete={mode === "login" ? "current-password" : "new-password"}
            minLength={12}
            maxLength={72}
            pattern={mode === "register" ? "(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[^A-Za-z0-9]).{12,72}" : undefined}
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            required
          />
        </label>
        {mode === "register" && (
          <p className="password-hint">Use 12 or more characters with upper/lowercase letters, a number, and a symbol.</p>
        )}
        {error && <p className="auth-error" role="alert">{error}</p>}
        <button className="primary-button auth-submit" type="submit" disabled={busy}>
          {submitLabel}
        </button>
      </form>
    </div>
  );
}