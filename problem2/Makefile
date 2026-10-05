COMPOSE=docker compose -f docker/docker-compose.yml

.PHONY: start stop reset smoke traffic logs test

start:
	$(COMPOSE) up -d --build

stop:
	$(COMPOSE) down

reset:
	$(COMPOSE) down -v

smoke:
	./scripts/smoke-test.sh

traffic:
	./scripts/generate-traffic.sh

logs:
	$(COMPOSE) logs -f gateway auth-a auth-b auth-c

test:
	docker build -f services/auth-service/Dockerfile -t debugathon-problem2-auth .
	docker run --rm debugathon-problem2-auth pytest -q
