import { useEffect, useMemo, useState } from "react";
import "./app.css";

const API_ORIGIN = (import.meta.env.VITE_API_URL || "").replace(/\/$/, "");
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

  async function loadDonors() {
    setLoading(true);

    try {
      const response = await fetch(API_URL);

      if (!response.ok) {
        throw new Error("Unable to load donors");
      }

      const data = await response.json();
      setDonors(data);
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
    void loadDonors();
  }, []);

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

      setForm(EMPTY_FORM);
      setMessage(
        form.publiclyListed
          ? "Registration received. Your profile is now listed."
          : "Registration received. Your profile will remain private."
      );

      await loadDonors();
    } catch {
      setMessage(
        "Registration failed. Check that the backend and database are running."
      );
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

        <a className="nav-button" href="#register">
          Become a Donor
        </a>
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
            <span className="eyebrow">JOIN OUR COMMUNITY</span>
            <h2>Become a Blood Donor</h2>
            <p>
              Complete the form to add your information to the
              donor directory.
            </p>
          </div>

          <form className="donor-form" onSubmit={handleSubmit}>
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
          </form>
        </section>

        <section className="section directory-section" id="directory">
          <div className="section-heading">
            <span className="eyebrow">FIND A DONOR</span>
            <h2>Donor Directory</h2>
            <p>
              Filter registered donors by blood group and city.
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
