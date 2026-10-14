import { useEffect, useMemo, useState } from "react";
import "./app.css";
import AuthPanel from "./AuthPanel.jsx";

const API_ORIGIN = (
  import.meta.env.VITE_BASE_API_URL || import.meta.env.VITE_API_URL || ""
).replace(/\/$/, "");
const API_URL = `${API_ORIGIN}/api/donors`;

const BLOOD_GROUPS = [
  "A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-",
];

const EMPTY_FORM = {
  fullName: "",
  age: "",
  bloodGroup: "O+",
  phone: "",
  city: "",
  lastDonationDate: "",
  available: true,
  publiclyListed: false,
};

function readSession() {
  try {
    return JSON.parse(sessionStorage.getItem("lifelink-session") || "null");
  } catch {
    return null;
  }
}

function formatLocalDate(date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

export default function App() {
  const [donors, setDonors] = useState([]);
  const [form, setForm] = useState(EMPTY_FORM);
  const [searchGroup, setSearchGroup] = useState("");
  const [searchCity, setSearchCity] = useState("");
  const [message, setMessage] = useState("");
  const [loading, setLoading] = useState(false);
  const [session, setSession] = useState(readSession);
  const [ownedDonorIds, setOwnedDonorIds] = useState([]);

  async function loadDonors(activeSession = session) {
    setLoading(true);

    try {
      const headers = activeSession?.accessToken
        ? { Authorization: `Bearer ${activeSession.accessToken}` }
        : {};
      const isAdmin = activeSession?.account?.role === "ADMIN";
      const endpoint = isAdmin ? `${API_ORIGIN}/api/donors/admin` : API_URL;
      const response = await fetch(endpoint, { headers });

      if (!response.ok) {
        throw new Error("Unable to load donors");
      }

      let data = await response.json();
      let ownedIds = [];
      if (activeSession?.accessToken && !isAdmin) {
        const ownResponse = await fetch(`${API_ORIGIN}/api/donors/mine`, { headers });
        if (!ownResponse.ok) throw new Error("Unable to load your donor profiles");
        const ownDonors = await ownResponse.json();
        ownedIds = ownDonors.map((donor) => donor.id);
        data = [...new Map([...data, ...ownDonors].map((donor) => [donor.id, donor])).values()];
      }
      setDonors(data);
      setOwnedDonorIds(ownedIds);
      setMessage("");
    } catch {
      setMessage(
        "Backend is not connected. Start Spring Boot and check MySQL."
      );
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    if (!session?.accessToken) {
      void loadDonors(null);
      return;
    }

    let active = true;
    fetch(`${API_ORIGIN}/api/auth/me`, {
      headers: { Authorization: `Bearer ${session.accessToken}` },
    }).then(async (response) => {
      if (!response.ok) throw new Error("Session expired");
      const account = await response.json();
      if (!active) return;
      const nextSession = { ...session, account };
      setSession(nextSession);
      sessionStorage.setItem("lifelink-session", JSON.stringify(nextSession));
      await loadDonors(nextSession);
    }).catch(() => {
      if (!active) return;
      sessionStorage.removeItem("lifelink-session");
      setSession(null);
      void loadDonors(null);
      setMessage("Your session expired. Sign in again to manage your profile.");
    });

    return () => { active = false; };
  }, []);

  async function handleAuthenticated(nextSession) {
    setSession(nextSession);
    sessionStorage.setItem("lifelink-session", JSON.stringify(nextSession));
    await loadDonors(nextSession);
    setMessage(`Signed in as ${nextSession.account.role.toLowerCase()}.`);
  }

  async function signOut() {
    sessionStorage.removeItem("lifelink-session");
    setSession(null);
    setOwnedDonorIds([]);
    await loadDonors(null);
    setMessage("You have signed out.");
  }

  const filteredDonors = useMemo(() => {
    return donors.filter((donor) => {
      const matchesGroup =
        !searchGroup || donor.bloodGroup === searchGroup;

      const matchesCity =
        !searchCity ||
        (donor.city || "")
          .toLowerCase()
          .includes(searchCity.toLowerCase());

      return matchesGroup && matchesCity;
    });
  }, [donors, searchGroup, searchCity]);

  function handleChange(event) {
    const { name, value, type, checked } = event.target;

    setForm((previous) => ({
      ...previous,
      [name]: type === "checkbox" ? checked : value,
    }));
  }

  async function handleSubmit(event) {
    event.preventDefault();

    const age = Number(form.age);

    if (!Number.isInteger(age) || age < 18 || age > 100) {
      setMessage("Enter an age between 18 and 100.");
      return;
    }

    if (!/^(?=(?:\D*\d){7})[+0-9() .-]{7,20}$/.test(form.phone.trim())) {
      setMessage("Enter a valid phone number using 7 to 20 digits or phone symbols.");
      return;
    }

    if (form.lastDonationDate && form.lastDonationDate > formatLocalDate(new Date())) {
      setMessage("Last donation date cannot be in the future.");
      return;
    }

    try {
      const response = await fetch(API_URL, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${session?.accessToken || ""}`,
        },
        body: JSON.stringify({
          ...form,
          fullName: form.fullName.trim(),
          phone: form.phone.trim(),
          city: form.city.trim(),
          age,
          lastDonationDate: form.lastDonationDate || null,
        }),
      });

      if (!response.ok) {
        throw new Error("Registration failed");
      }

      setForm({ ...EMPTY_FORM });
      await loadDonors(session);
      setMessage(form.publiclyListed
        ? "Registration received. Your profile is now listed."
        : "Registration received. Your profile will remain private.");
    } catch {
      setMessage(
        "Registration failed. Check that the backend and database are running."
      );
    }
  }

  async function deleteDonor(id) {
    if (!session?.accessToken || !window.confirm("Delete this donor profile?")) return;
    try {
      const response = await fetch(`${API_URL}/${id}`, {
        method: "DELETE",
        headers: { Authorization: `Bearer ${session.accessToken}` },
      });
      if (!response.ok) throw new Error("Could not delete donor profile.");
      await loadDonors(session);
      setMessage("Donor profile deleted.");
    } catch (error) {
      setMessage(error.message || "Could not delete donor profile.");
    }
  }

  const availableCount = donors.filter(
    (donor) => donor.available
  ).length;

  const bloodGroupCount = new Set(
    donors.map((donor) => donor.bloodGroup)
  ).size;

  let donorDirectory;
  if (loading) {
    donorDirectory = <p className="empty-state">Loading donors...</p>;
  } else if (filteredDonors.length === 0) {
    donorDirectory = (
      <div className="empty-state">
        <span className="empty-heart">♥</span>
        <h3>No donors found</h3>
        <p>Register a donor or try a different search.</p>
      </div>
    );
  } else {
    donorDirectory = (
      <div className="donor-grid">
        {filteredDonors.map((donor) => (
          <article className="donor-card" key={donor.id}>
            <div className="group-badge">{donor.bloodGroup}</div>
            <div className="donor-details">
              <h3>{donor.fullName}</h3>
              <p>{donor.city} · Age {donor.age}</p>
              <span className={donor.available ? "available" : "unavailable"}>
                {donor.available ? "Marked available" : "Not currently available"}
              </span>
            </div>
            {(session?.account?.role === "ADMIN" || ownedDonorIds.includes(donor.id)) && (
              <button className="delete-button" type="button" onClick={() => deleteDonor(donor.id)}>
                Delete
              </button>
            )}
          </article>
        ))}
      </div>
    );
  }

  return (
    <div className="app">
      <header className="navbar">
        <a className="brand" href="#home">
          <span className="brand-icon">♥</span>
          <span>LifeLink</span>
        </a>

        <nav>
          <a href="#home">Home</a>
          <a href="#register">Register Donor</a>
          <a href="#directory">Find Donors</a>
        </nav>

        {session ? (
          <div className="account-actions">
            <span className="account-label">{session.account.email}<small>{session.account.role}</small></span>
            <button className="secondary-button" type="button" onClick={signOut}>Sign out</button>
          </div>
        ) : (
          <a className="nav-button" href="#account">Sign in</a>
        )}
      </header>

      <main id="home">
        <section className="hero">
          <div className="hero-content">
            <span className="eyebrow">
              EVERY DROP MAKES A DIFFERENCE
            </span>

            <h1>
              Your Blood Can
              <br />
              <span>Save a Life.</span>
            </h1>

            <p>
              Connect blood donors with people who need help.
              Register as a donor or search our directory to
              find potential donors in your city.
            </p>

            <div className="hero-actions">
              <a className="primary-button" href="#register">
                Register as Donor
              </a>

              <a className="outline-button" href="#directory">
                Find Blood Donors
              </a>
            </div>

            <div className="hero-note">
              <span>♥</span>
              <span>One donation can make a meaningful difference.</span>
            </div>
          </div>

          <div className="hero-art">
            <div className="art-circle">
              <div className="blood-drop">♥</div>
              <div className="art-caption">
                <strong>Be someone's hero</strong>
                <span>Donate blood. Share hope.</span>
              </div>
            </div>

            <div className="floating-card">
              <span className="mini-heart">♥</span>
              <div>
                <strong>Give the gift of life</strong>
                <p>Every donor matters.</p>
              </div>
            </div>
          </div>
        </section>

        <section className="stats">
          <article className="stat-card">
            <span className="stat-icon">♥</span>
            <div>
              <p>Listed Donors</p>
              <h2>{donors.length}</h2>
            </div>
          </article>

          <article className="stat-card">
            <span className="stat-icon">✓</span>
            <div>
              <p>Marked Available</p>
              <h2>{availableCount}</h2>
            </div>
          </article>

          <article className="stat-card">
            <span className="stat-icon">+</span>
            <div>
              <p>Blood Groups</p>
              <h2>{bloodGroupCount} / 8</h2>
            </div>
          </article>
        </section>

        {!session && (
          <section className="section auth-section" id="account">
            <div className="section-heading">
              <span className="eyebrow">YOUR LIFELINK ACCOUNT</span>
              <h2>Sign in or create an account</h2>
              <p>Signed-in donors can manage their own profiles.</p>
            </div>
            <AuthPanel onAuthenticated={handleAuthenticated} />
          </section>
        )}

        {message && (
          <output className="message" aria-live="polite">
            {message}
            <button
              className="message-close"
              onClick={() => setMessage("")}
              aria-label="Dismiss message"
            >
              ×
            </button>
          </output>
        )}

        <section className="section" id="register">
          <div className="section-heading">
            <span className="eyebrow">{session?.account?.role === "ADMIN" ? "ADMINISTRATION" : "JOIN OUR COMMUNITY"}</span>
            <h2>{session?.account?.role === "ADMIN" ? "Register a donor" : "Become a Blood Donor"}</h2>
            <p>
              Complete the form to add your information to the
              donor directory.
            </p>
          </div>

          {session ? <form className="donor-form" onSubmit={handleSubmit}>
            <label>
              <span>Full Name *</span>
              <input
                name="fullName"
                value={form.fullName}
                onChange={handleChange}
                placeholder="Enter your full name"
                maxLength={100}
                required
              />
            </label>

            <label>
              <span>Age *</span>
              <input
                name="age"
                type="number"
                min="18"
                max="100"
                value={form.age}
                onChange={handleChange}
                placeholder="Enter your age"
                required
              />
            </label>

            <label>
              <span>Blood Group *</span>
              <select
                name="bloodGroup"
                value={form.bloodGroup}
                onChange={handleChange}
                required
              >
                {BLOOD_GROUPS.map((group) => (
                  <option key={group} value={group}>
                    {group}
                  </option>
                ))}
              </select>
            </label>

            <label>
              <span>Mobile Number *</span>
              <input
                name="phone"
                type="tel"
                value={form.phone}
                onChange={handleChange}
                placeholder="Include country code if needed"
                maxLength={20}
                required
              />
            </label>

            <label>
              <span>City *</span>
              <input
                name="city"
                value={form.city}
                onChange={handleChange}
                placeholder="Enter your city"
                maxLength={100}
                required
              />
            </label>

            <label>
              <span>Last Donation Date</span>
              <input
                name="lastDonationDate"
                type="date"
                max={formatLocalDate(new Date())}
                value={form.lastDonationDate}
                onChange={handleChange}
              />
            </label>

            <label className="availability-option">
              <input
                name="available"
                type="checkbox"
                checked={form.available}
                onChange={handleChange}
              />
              <span>Mark as potentially available for contact.</span>
            </label>
            <label className="availability-option">
              <input
                name="publiclyListed"
                type="checkbox"
                checked={form.publiclyListed}
                onChange={handleChange}
              />
              <span>I consent to publicly listing my name, age, blood group, and city.</span>
            </label>

            <button className="primary-button submit-button" type="submit">
              Register Donor →
            </button>
          </form> : (
            <div className="auth-required">
              <p>Sign in or create an account before registering a donor.</p>
              <a className="primary-button" href="#account">Sign in or create account</a>
            </div>
          )}
        </section>

        <section className="section directory-section" id="directory">
          <div className="section-heading">
            <span className="eyebrow">FIND A DONOR</span>
            <h2>{session?.account?.role === "ADMIN" ? "Donor management" : "Donor Directory"}</h2>
            <p>
              {session?.account?.role === "ADMIN"
                ? "Review and manage all donor profiles."
                : "Filter publicly listed donors; your private profiles remain visible to you."}
            </p>
          </div>

          <div className="search-bar">
            <label>
              <span>Blood Group</span>
              <select
                value={searchGroup}
                onChange={(event) =>
                  setSearchGroup(event.target.value)
                }
              >
                <option value="">All Blood Groups</option>
                {BLOOD_GROUPS.map((group) => (
                  <option key={group} value={group}>
                    {group}
                  </option>
                ))}
              </select>
            </label>

            <label>
              <span>City</span>
              <input
                value={searchCity}
                onChange={(event) =>
                  setSearchCity(event.target.value)
                }
                placeholder="Search by city"
              />
            </label>

            <button
              className="outline-button refresh-button"
              type="button"
              onClick={loadDonors}
            >
              Refresh
            </button>
          </div>

          {donorDirectory}
        </section>

        <section className="callout">
          <span className="callout-heart">♥</span>
          <div>
            <h2>Be the reason someone smiles today.</h2>
            <p>Register as a donor and help build a caring community.</p>
          </div>
          <a href="#register" className="light-button">
            Join Us →
          </a>
        </section>
      </main>

      <footer className="footer">
        <a className="brand" href="#home">
          <span className="brand-icon">♥</span>
          <span>LifeLink</span>
        </a>

        <p>
          A student full-stack project for blood donor management.
        </p>

        <small>
          Donor registration does not establish medical eligibility.
          Donation decisions must be made by qualified healthcare staff.
        </small>
      </footer>
    </div>
  );
}
