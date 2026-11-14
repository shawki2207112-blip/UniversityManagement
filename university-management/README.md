# University Management System (CSE 3230 – Lab 4)

Spring Boot 3 + Spring Security (JWT) + JPA, with three roles: **Admin**, **Teacher**, **Student**.

## Run with Docker (app + PostgreSQL)

```bash
docker compose up --build
```

Open http://localhost:8080

## Run without Docker (uses in-memory H2)

Needs JDK 17+ and Maven:

```bash
mvn spring-boot:run
```

## Demo accounts (created on first start)

| Username | Password   | Role    |
|----------|------------|---------|
| admin    | admin123   | ADMIN   |
| teacher  | teacher123 | TEACHER |
| student  | student123 | STUDENT |

## How authentication works (JWT)

1. `POST /api/auth/login` with username + password → the server checks the BCrypt hash and returns a signed JWT.
2. The client sends `Authorization: Bearer <token>` on every request.
3. `JwtAuthFilter` validates the token and loads the user; the role rules then decide access.
4. `POST /api/auth/logout` revokes the token (kept in an in-memory list, so it resets on restart).

Tokens expire after 1 hour (`app.jwt.expiration-ms`). No server-side session is used.

## Access rules (authorization)

| Endpoint           | ADMIN | TEACHER | STUDENT |
|--------------------|:-----:|:-------:|:-------:|
| `/api/admins/**`   |  yes  |   no    |   no    |
| `/api/teachers/**` |  yes  |   yes   |   no    |
| `/api/students/**` |  yes  |   no    |   yes   |

No/invalid token → `401`; valid token but wrong role → `403`.

## REST API

Replace `{type}` with `admins`, `teachers` or `students`.

| Method | URL                  | Description                | Success |
|--------|----------------------|----------------------------|---------|
| POST   | `/api/auth/login`    | Get a JWT                  | 200     |
| POST   | `/api/auth/logout`   | Revoke the current JWT     | 204     |
| GET    | `/api/me`            | Current user               | 200     |
| POST   | `/api/{type}`        | Create                     | 201     |
| GET    | `/api/{type}`        | List all                   | 200     |
| GET    | `/api/{type}/{id}`   | Get one                    | 200     |
| PUT    | `/api/{type}/{id}`   | Update name/email/password | 200     |
| DELETE | `/api/{type}/{id}`   | Delete                     | 204     |

Other errors: `400` (missing username/password), `404` (not found), `409` (username already taken).

### Try it with curl

```bash
# 1. log in and save the token
TOKEN=$(curl -s -X POST -H "Content-Type: application/json" \
     -d '{"username":"admin","password":"admin123"}' \
     http://localhost:8080/api/auth/login | sed 's/.*"token":"\([^"]*\)".*/\1/')

# 2. use it
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/teachers
curl -H "Authorization: Bearer $TOKEN" -X POST -H "Content-Type: application/json" \
     -d '{"username":"t2","password":"pass123","name":"Dr. Rahman"}' \
     http://localhost:8080/api/teachers

# 3. log out (token stops working)
curl -H "Authorization: Bearer $TOKEN" -X POST http://localhost:8080/api/auth/logout
```

## Code layout

```
config/AuthenticationConfig.java   -> WHO you are (user lookup, BCrypt, AuthenticationManager)
config/AuthorizationConfig.java    -> WHAT you may do (role rules per URL, JWT filter in the chain)
security/JwtService.java           -> create / validate / revoke tokens
security/JwtAuthFilter.java        -> reads the Bearer token on each request
controller/AuthController.java     -> /api/auth/login, /api/auth/logout
controller/UserController.java     -> CRUD for admins / teachers / students
model/, repository/                -> entity + Spring Data repository
static/login.html, index.html      -> minimal frontend (token kept in localStorage)
static/style.css                   -> a few lines of CSS
Dockerfile, compose.yaml           -> containerization
```

Notes: the role of a new user always comes from the URL (e.g. `/api/teachers` creates a TEACHER), never from
the request body. CSRF protection is off because the API is stateless and uses a header token instead of cookies.
Change `APP_JWT_SECRET` (in `compose.yaml`) before any real deployment.
