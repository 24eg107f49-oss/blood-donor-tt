# LifeLink Blood Donor Network

React + Vite frontend, Spring Boot 4.1 / Java 17 API, and MySQL-compatible database. The Vite app is in `frontend`; the API is in `backend/blood-donor-api`.

## Run locally

1. Create a MySQL database by running `database/blood_donor.sql`.
2. Start the API from `backend/blood-donor-api`:

	```bash
	mvn spring-boot:run
	```

	Local defaults use `localhost:3306`, database `blood_donor`, and MySQL user/password `root`/`root`. Override with `DB_URL`, or `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, and `DB_PASSWORD`.
3. In another terminal, start the frontend:

	```bash
	cd frontend
	npm install
	npm run dev
	```

	Vite proxies `/api` to `http://localhost:8080`. Copy `frontend/.env.example` to `.env.local` only when using a separate API origin.

## Deploy the API to Render

1. Connect the repository to Render and create a Blueprint from `render.yaml`. The service builds from `backend/blood-donor-api/Dockerfile` and checks `/api/health`.
2. Provide a reachable MySQL-compatible database. Set `DB_URL` to its JDBC URL (`jdbc:mysql://...`), plus `DB_USERNAME` and `DB_PASSWORD`. Do not use a `mysql://` URL. The current backend does not use PostgreSQL.
3. Set `CORS_ALLOWED_ORIGINS` to the exact deployed Vercel origin, with no path (for example, `https://your-app.vercel.app`). Set `ADMIN_API_KEY` to a strong secret. Never put this key in the frontend.
4. Keep secrets in the Render environment, not in source control. `DDL_AUTO` defaults to `update`; use a deliberate schema migration process before changing it to `validate`.

## Run backend with Docker Compose

Install Docker Desktop with the WSL 2 backend, then run from the repository root:

```bash
docker compose up --build
```

The API is available at `http://localhost:8080`; Compose publishes MySQL on host port `3307` to avoid colliding with a local MySQL service. Stop the services with `docker compose down`. The named database volume persists; `docker compose down -v` deletes its data.

## Deploy the frontend to Vercel

1. Import the repository and set the Vercel project **Root Directory** to `frontend`.
2. Use `npm run build` as the build command and `dist` as the output directory (Vite is detected automatically).
3. Add `VITE_API_URL` with the Render service origin, for example `https://lifelink-api.onrender.com`, without `/api` at the end. Redeploy after adding it.
4. Add the resulting Vercel origin to Render's `CORS_ALLOWED_ORIGINS` and redeploy the API.

## API

- `GET /api/health`: liveness check.
- `GET /api/donors?bloodGroup=O%2B&city=...`: list profiles that opted into public listing; phone numbers and donation dates are never returned.
- `POST /api/donors`: register a donor. Public directory listing is opt-in via `publiclyListed`.
- `GET /api/donors/{id}`: retrieve an opted-in profile.
- `PUT` and `DELETE /api/donors/{id}`: require the `X-Admin-Key` header matching `ADMIN_API_KEY`.

## Verify

```bash
cd frontend
npm run build
cd ../backend/blood-donor-api
mvn test
```

The backend currently has no automated test sources; `mvn test` verifies compilation and the Maven test lifecycle.

## Data and safety

This is a donor-directory demo, not a clinical or medically validated service. Public listing exposes a consenting donor's name, age, blood group, and city. Phone numbers are accepted for recordkeeping but excluded from API responses. The admin key only protects update/delete endpoints; there is no user login, registration abuse protection, or clinical eligibility assessment. Complete security, consent, privacy, and legal review before storing real donor data or treating this as a production medical service.
