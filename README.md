# LifeLink Blood Donor Network

React + Vite frontend, Spring Boot 4.1 / Java 17 API, and PostgreSQL on Render. The Vite app is in `frontend`; the API is in `backend/blood-donor-api`. Local MySQL remains supported for development.

## Run locally

1. Create a MySQL database by running `database/blood_donor.sql`.
2. In PowerShell, set a local JWT secret and optionally bootstrap the admin account, then start the API from the repository root:

	```powershell
	$env:JWT_SECRET = [Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
	$env:ADMIN_EMAIL = "admin@example.com"
	$env:ADMIN_PASSWORD = "Set-A-Strong-Admin-Password-123!"
	mvn.cmd -f backend/blood-donor-api/pom.xml spring-boot:run
	```

	Passwords may be any non-empty value up to 1024 characters. `.env` is used by Docker Compose, not by `mvn`. Local database defaults use `localhost:3306`, database `blood_donor`, and MySQL user/password `root`/`root`. Override with `DB_URL`, or `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, and `DB_PASSWORD`.
3. In another terminal, start the frontend:

	```bash
	cd frontend
	npm install
	npm run dev
	```

	Vite proxies `/api` to `http://localhost:8080`. For a separate API origin, set `VITE_BASE_API_URL` in `frontend/.env.local`.

## Deploy the API to Render

1. Connect the repository to Render and sync the Blueprint from `render.yaml`. It creates the API plus a Free Postgres database and injects its private connection string automatically; no database environment values need manual entry.
2. Render generates the JWT signing secret and initial admin password. The admin email is `24eg107f49@anurag.edu.in`; retrieve the generated password from the Render service's environment settings. Ordinary signup always receives the USER role.
3. Set Vercel's `VITE_BASE_API_URL` to the API's `onrender.com` URL. CORS is configured for the stateless bearer-token API and does not use cookies.
4. Render Free Postgres is limited to 1 GB and expires 30 days after creation; after the 14-day grace period Render deletes it. Export/upgrade before expiry if the data must be kept. `DDL_AUTO` defaults to `update`.

## Run backend with Docker Compose

Install Docker Desktop with the WSL 2 backend, then run from the repository root:

```bash
docker compose up --build
```

The API is available at `http://localhost:8080`; Compose publishes MySQL on host port `3307` to avoid colliding with a local MySQL service. Stop the services with `docker compose down`. The named database volume persists; `docker compose down -v` deletes its data. Compose requires `JWT_SECRET`; set it in the root `.env` file together with optional `ADMIN_EMAIL` and `ADMIN_PASSWORD` values.

## Deploy the frontend to Vercel

1. Import the repository and set the Vercel project **Root Directory** to `frontend`.
2. Use `npm run build` as the build command and `dist` as the output directory (Vite is detected automatically).
3. Add `VITE_BASE_API_URL` with the Render service origin, for example `https://lifelink-api.onrender.com`, without `/api` at the end. Redeploy after adding it.
4. Add the resulting Vercel origin to Render's `CORS_ALLOWED_ORIGINS` and redeploy the API.

## API

- `GET /api/health`: liveness check.
- `GET /api/donors?bloodGroup=O%2B&city=...`: list profiles that opted into public listing; phone numbers and donation dates are never returned.
- `POST /api/donors`: requires a bearer token; register a donor. Public directory listing is opt-in via `publiclyListed`.
- `GET /api/donors/{id}`: retrieve an opted-in profile.
- `POST /api/auth/register` and `POST /api/auth/login`: create a user account and issue a short-lived bearer token.
- `GET /api/auth/me`: return the signed-in account and role.
- `GET /api/donors/mine`: list the signed-in user's donor profiles.
- `GET /api/donors/admin`: admin-only list of all donor profiles.
- `PUT` and `DELETE /api/donors/{id}`: require the profile owner or an admin bearer token.
- Admin accounts are bootstrapped from `ADMIN_EMAIL` and `ADMIN_PASSWORD` on startup; ordinary registration cannot create an admin role.

## Verify

```bash
cd frontend
npm run build
cd ../backend/blood-donor-api
mvn test
```

The backend suite covers authentication/authorization, health checks, validation, and donor repository behavior.

## Data and safety

This is a donor-directory demo, not a clinical or medically validated service. Public listing exposes a consenting donor's name, age, blood group, and city. Phone numbers are accepted for recordkeeping but excluded from API responses. Passwords are BCrypt-hashed, user signup cannot grant admin rights, JWTs expire after 15 minutes, and donor mutations are restricted to the profile owner or admin. There is no MFA, refresh-token flow, or distributed rate limiting; complete security, consent, privacy, and legal review before storing real donor data or treating this as a production medical service.
