# Local Setup

## Prerequisites

- Node.js 20+
- Java 21+
- Maven 3.9+
- Python 3.11+
- Docker Desktop

## Start infrastructure

```bash
docker compose -f infra/docker-compose.yml up -d
```

## Backend

```bash
cd backend
./mvnw spring-boot:run
```

On Windows PowerShell:

```powershell
mvnw.cmd spring-boot:run
```

## Allocation engine

```bash
cd allocation-engine
python -m venv .venv
# activate the environment
pip install -r requirements.txt
uvicorn app.main:app --reload --port 8001
```

## ML service

```bash
cd ml-service
python -m venv .venv
pip install -r requirements.txt
uvicorn app.main:app --reload --port 8002
```

## RAG service

```bash
cd rag-service
python -m venv .venv
pip install -r requirements.txt
uvicorn app.main:app --reload --port 8003
```

## Frontend

```bash
cd frontend
npm install
npm run dev
```
