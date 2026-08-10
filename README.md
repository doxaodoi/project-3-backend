# Reclaim Backend — Campus Lost & Found API

Spring Boot REST API for the Reclaim campus lost & found system.

**CPEN 208 Project 3 — University of Ghana**

## Tech Stack
- **Java 21** + **Spring Boot 3.3.6**
- **PostgreSQL** with **Flyway** migrations
- **Spring Security** + JWT (access/refresh tokens)
- **Anthropic Claude API** for AI Smart-Describe and Match Explainer

## Running Locally

### Prerequisites
- Java 21+
- Maven 3.9+
- PostgreSQL (database named `reclaim`)

### Environment Variables
| Variable | Required | Description |
|---|---|---|
| `DATABASE_URL` | Yes | JDBC URL, e.g. `jdbc:postgresql://localhost:5432/reclaim` |
| `DB_USERNAME` | Yes | Database username |
| `DB_PASSWORD` | Yes | Database password |
| `JWT_SECRET` | Yes | Secret key for JWT signing (min 64 chars) |
| `ANTHROPIC_API_KEY` | No | Anthropic API key for AI features |
| `CORS_ORIGINS` | No | Allowed origins (default: `http://localhost:3000`) |

### Start
```bash
mvn spring-boot:run
```

The API runs at `http://localhost:8080`.

## API Endpoints

### Auth
- `POST /api/auth/register` — Create account
- `POST /api/auth/login` — Sign in (returns JWT)
- `GET /api/auth/me` — Current user profile

### Items
- `GET /api/items` — Browse/search (paginated, filterable)
- `GET /api/items/{id}` — Item detail
- `POST /api/items` — Report lost/found item
- `GET /api/items/mine` — My reports

### AI
- `POST /api/ai/describe` — Smart-Describe (photo to structured fields)
- `GET /api/ai/status` — Check if AI is available

### Matches
- `GET /api/items/{id}/matches` — Suggested matches for an item
- `GET /api/matches/{id}/explanation` — AI Match Explainer

### Claims, Messages, Notifications
- Full CRUD — see controller source for details

### Admin
- `GET /api/admin/stats` — Dashboard statistics
- `GET /api/admin/items` — All items (paginated)
- `GET /api/admin/users` — All users

## Seed Data
Flyway migrations seed 5 users, 12 items with Unsplash photos, matches, and conversations.
All seed users share password: `Password123`.
