.PHONY: dev down logs test backend-test frontend-test clean

dev:
	docker compose up --build

down:
	docker compose down

logs:
	docker compose logs -f

test: backend-test frontend-test

backend-test:
	cd backend && mvn test

frontend-test:
	cd frontend && npm ci && npm run check

clean:
	rm -rf backend/target frontend/dist frontend/node_modules
