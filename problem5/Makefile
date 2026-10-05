COMPOSE = docker compose -f docker/docker-compose.yml
.PHONY: up down reset smoke traffic logs test
up:
	$(COMPOSE) up -d --build
down:
	$(COMPOSE) down
reset:
	$(COMPOSE) down -v
	$(COMPOSE) up -d --build
smoke:
	./scripts/smoke-test.sh
traffic:
	./scripts/generate-traffic.sh
logs:
	$(COMPOSE) logs -f pricing-api
test:
	cd services/pricing-api && mvn -B test
