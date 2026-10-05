# HelpDeskAI 🤖

**Enterprise-grade AI-powered customer support platform** — a self-hostable alternative to Zendesk/Freshdesk with built-in AI triage, live chat, knowledge base, and real-time analytics.

![CI](https://github.com/your-org/helpdeskAi/actions/workflows/ci.yml/badge.svg)

---

## ✨ Features

| Feature | Status |
|---|---|
| 🎫 Ticket Management (CRUD, SLA, assignment) | ✅ |
| 🤖 AI Triage (auto-classify, priority, sentiment) | ✅ |
| 💬 Live Chat (WebSocket STOMP + SockJS) | ✅ |
| 📚 Knowledge Base (articles, categories, search) | ✅ |
| 📊 Analytics Dashboard (Recharts, real-time KPIs) | ✅ |
| 🔔 Notifications (in-app + email via SMTP) | ✅ |
| 🏢 Multi-Tenancy (org-scoped data isolation) | ✅ |
| 🎨 Custom Branding (per-org colors, domain, logo) | ✅ |
| 🔑 API Keys + Webhooks | ✅ |
| 🛡️ Audit Log | ✅ |
| 🌐 Public Support Portal | ✅ |

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────┐
│  React SPA (Vite + TypeScript)  →  Vercel               │
└────────────────────┬────────────────────────────────────┘
                     │ HTTPS / WebSocket
    ┌────────────────▼──────────────────────────────────┐
    │              Render (API Services)                 │
    │                                                    │
    │  auth :8081 │ org  :8082 │ ticket :8083           │
    │  chat :8084 │ kb   :8085 │ analytics :8086        │
    │  notifications :8087  │  ai-service :8000 (FastAPI)│
    └─────────────────────────────┬─────────────────────┘
                                  │
                        ┌─────────▼──────────┐
                        │  PostgreSQL (Render)│
                        └────────────────────┘
```

---

## 🚀 Quick Start (Local Development)

### Prerequisites
- Docker & Docker Compose
- Node.js 20+
- Java 21+ (for backend development)

### 1. Clone the repo
```bash
git clone https://github.com/your-org/helpdeskAi.git
cd helpdeskAi
```

### 2. Configure environment
```bash
cp .env.example .env
# Edit .env and add your GEMINI_API_KEY
```

### 3. Start all services
```bash
docker compose up -d
```

This starts:
- PostgreSQL on `:5432`
- Redis on `:6379`
- All 7 Spring Boot microservices
- FastAPI AI service on `:8000`

### 4. Start the frontend
```bash
cd frontend
npm install --legacy-peer-deps
npm run dev
```

Frontend runs at **http://localhost:3000**

---

## 📁 Project Structure

```
helpdeskAi/
├── .github/
│   └── workflows/ci.yml          # GitHub Actions CI/CD
├── backend/
│   ├── common/                   # Shared: JWT, entities, DTOs
│   ├── auth-service/             # Authentication (port 8081)
│   ├── org-service/              # Organization management (port 8082)
│   ├── ticket-service/           # Ticket CRUD + SLA (port 8083)
│   ├── chat-service/             # WebSocket live chat (port 8084)
│   ├── kb-service/               # Knowledge base (port 8085)
│   ├── analytics-service/        # Analytics & reporting (port 8086)
│   └── notification-service/     # Email notifications (port 8087)
├── ai-service/                   # FastAPI AI service (port 8000)
├── frontend/                     # Vite + React + TypeScript
│   ├── src/
│   │   ├── components/           # Shared UI components
│   │   ├── pages/                # Route-level pages
│   │   ├── store/                # Zustand state
│   │   ├── lib/                  # API client
│   │   └── styles/               # Design tokens + global CSS
│   └── Dockerfile
├── scripts/
│   └── init-db.sql               # Creates all service databases
├── docker-compose.yml            # Local development orchestration
├── render.yaml                   # Render.com deployment spec
└── README.md
```

---

## 🔧 Environment Variables

Create a `.env` file in the root:

```env
# AI Service
GEMINI_API_KEY=your_gemini_api_key_here

# Email (SMTP)
MAIL_USERNAME=noreply@yourdomain.com
MAIL_PASSWORD=your_smtp_password

# Frontend
BASE_URL=http://localhost:3000

# JWT (must be ≥ 256 bits)
JWT_SECRET=helpdeskAiSecretKeyThatIsLongEnoughForHmacSha256AlgorithmRequires32Bytes!!
```

---

## 🚢 Deployment

### Render (Backend)
1. Fork this repo
2. Connect Render to your GitHub account
3. Select **"New Blueprint Instance"** and point to `render.yaml`
4. Set required secrets in Render dashboard (`JWT_SECRET`, `GEMINI_API_KEY`, `MAIL_*`)

### Vercel (Frontend)
```bash
cd frontend
npx vercel --prod
```
Set `VITE_API_BASE_URL` to your Render service URLs in Vercel environment variables.

---

## 🤖 AI Endpoints

The FastAPI AI service exposes:

| Endpoint | Purpose |
|---|---|
| `POST /api/v1/ai/classify` | Auto-classify ticket category |
| `POST /api/v1/ai/predict-priority` | Predict ticket priority |
| `POST /api/v1/ai/analyze-sentiment` | Customer sentiment analysis |
| `POST /api/v1/ai/suggest-reply` | AI draft reply suggestions |
| `POST /api/v1/ai/summarize` | Summarize long ticket threads |
| `POST /api/v1/ai/detect-duplicates` | Find similar/duplicate tickets |
| `POST /api/v1/ai/kb-recommendations` | Recommend KB articles |

---

## 🧪 Tech Stack

| Layer | Technology |
|---|---|
| Frontend | React 18, Vite, TypeScript, CSS Modules |
| State | Zustand + TanStack React Query |
| Backend | Spring Boot 3, Spring Security, Spring Data JPA |
| AI Service | FastAPI, Python 3.11, Google Gemini API |
| Database | PostgreSQL 16 |
| Real-time | WebSocket (STOMP over SockJS) |
| Email | JavaMail (SMTP) |
| Container | Docker, Docker Compose |
| CI/CD | GitHub Actions |
| Frontend Host | Vercel |
| Backend Host | Render.com |

---

## 📜 License

MIT License © 2025 HelpDeskAI
