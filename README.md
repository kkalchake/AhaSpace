# Enlightenment | Learning Platform

A full-stack learning management platform with a Spring Boot backend and React frontend.

## Project Structure

- **Backend** (`/`): Java Spring Boot 4.0.3 application with JWT authentication
- **Frontend** (`/AhaSpaceUI`): React 19 + Vite SPA

## Backend

### Tech Stack
- **Framework**: Spring Boot 4.0.3
- **Java**: 21
- **Database**: PostgreSQL 16 (via Docker Compose)
- **Security**: Spring Security with JWT
- **Template Engine**: Thymeleaf
- **Build Tool**: Maven
- **Testing**: JUnit 5, JaCoCo (coverage)

### Features
- User registration with email validation
- JWT-based authentication
- Secure password storage (BCrypt)
- RESTful API endpoints

### Getting Started

#### Prerequisites
- Java 21
- Maven 3.9+
- Docker (with Docker Compose)

#### Clone & Build
```bash
git clone https://github.com/kkalchake/AhaSpace.git
cd enlightenment
mvn clean install
```

#### Start the database
```bash
docker compose up -d
```
This starts a PostgreSQL 16 container (`ahaspace-postgres`, database `enlightenment`) on `localhost:5432`.

#### Configure the dev profile
```bash
cp src/main/resources/application-dev.yaml.example src/main/resources/application-dev.yaml
```
Then edit `src/main/resources/application-dev.yaml` and set `gemini.api.key` to your own key. The database values already match the Docker Compose service, so leave them as-is.

#### Run Application
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

The backend will start on `http://localhost:8080`.

> **IntelliJ users:** if you run via the IDE's Run button instead of Maven, set `SPRING_PROFILES_ACTIVE=dev` (or the `dev` active profile) in your run configuration once — this is per-user IDE state and is not committed.

#### API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/register` | Register new user |
| POST | `/api/auth/login` | Authenticate user |
| GET | `/api/auth/me` | Get current user info |

#### Testing
```bash
mvn test
```

Generate coverage report:
```bash
mvn jacoco:report
```

## Frontend

See [AhaSpaceUI/README.md](./AhaSpaceUI/README.md) for frontend setup.

## Contributing

Pull requests are welcome. For major changes, please open an issue first
to discuss what you would like to change.

Please make sure to update tests as appropriate.

## License

[MIT](https://choosealicense.com/licenses/mit/)