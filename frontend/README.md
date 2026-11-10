# Medi-Slot

The React frontend and the supplied Spring Boot backend now live together in this project.

## Start locally

1. Start PostgreSQL and create a database named `hospital_db`.
2. From the separate `../backend/` folder, run `./mvnw spring-boot:run`.
3. From this folder, run `npm install` once and then `npm run dev`.

Vite forwards `/api` calls to `http://localhost:8081`. The backend connects directly to PostgreSQL; set `DB_USERNAME`, `DB_PASSWORD`, and optionally `DB_URL` when your local PostgreSQL values differ from the defaults.

The backend is set up to compile in this workspace’s JDK 26 via the generated `backend/src/delombok/java` source tree. If the original Java files change, regenerate it with the Lombok JAR specified in `backend/pom.xml` before building.

## Seeded backend credentials

- Employee: `EMP001` / `password123`
- Hospital admin: `admin.noida@medislot.com` / `password123`

The login screen chooses the admin endpoint when the identifier begins with `admin`.

## Connected today

- JWT employee and hospital-admin login, with the token kept in session storage.
- Hospital and doctor catalogues populate from the API when a logged-in employee backend session is available; the designed demo content remains available if the API is offline.
- The shared API client includes methods for appointments, slots, profile updates, dashboard figures, admin doctor status, and admin appointment status, ready for the remaining UI bindings.
