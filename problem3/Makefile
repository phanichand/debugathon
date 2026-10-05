COMPOSE = docker compose -f docker/docker-compose.yml
.PHONY: up down smoke traffic test validate
up:
	$(COMPOSE) up -d --build --wait --wait-timeout 180
down:
	$(COMPOSE) down
smoke:
	python3 scripts/smoke_test.py
traffic:
	python3 scripts/generate_traffic.py
test:
	python3 -m pytest -q
validate:
	$(COMPOSE) config --quiet
