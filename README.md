# StatementIQ — India's Smartest Money Co-Pilot

## Privacy-First AI-Powered Financial Intelligence PWA

StatementIQ transforms raw bank and credit card PDF statements into a complete, intelligent picture of your financial life. Zero bank login. Zero SMS access. Just a PDF upload.

### 🏗️ Tech Stack

| Layer | Technology |
|-------|-----------|
| **Backend** | Java 21 + Spring Boot 4.0 + Gradle |
| **Frontend** | React 18 + TypeScript + Vite |
| **Database** | MongoDB 7 (Atlas / Docker) |
| **AI** | Claude API (Anthropic) |
| **Auth** | Firebase Authentication |
| **Payments** | Razorpay |
| **Styling** | TailwindCSS + shadcn/ui |
| **Charts** | Recharts |
| **Deploy** | Docker / Railway + Vercel |

### 🚀 Quick Start

#### Development (Docker)

```bash
# Clone and setup
cp .env.example .env
# Fill in your API keys in .env

# Start all services
docker-compose up -d

# Backend: http://localhost:8080
# Frontend: http://localhost:5173 (dev) or http://localhost (Docker)
# MongoDB: localhost:27017
```

#### Development (Manual)

```bash
# Backend
cd backend
./gradlew bootRun

# Frontend
cd frontend
npm install
npm run dev
```

### 🔒 Privacy First

- PDFs are processed **entirely in-memory** — never written to disk
- No bank login or SMS access required
- Transaction data encrypted in transit (TLS) and at rest (MongoDB Atlas)
- No PII sent to AI services
- DPDP Act compliant — full data export and account deletion

### 📱 Four Pillars

- **ANALYZE** — CC + Bank statement parsing. Full 360° money picture.
- **ALERT** — Hidden charge detection. Bill due radar. Overspend warnings.
- **RECOMMEND** — Smart Swipe daily card suggestions. Reward optimization.
- **GOALS** — AI-powered savings planner with daily check-ins and streaks.
