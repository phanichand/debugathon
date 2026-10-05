FROM python:3.12-slim

WORKDIR /app

COPY services/auth-service/requirements.txt /app/requirements.txt
RUN pip install --no-cache-dir -r /app/requirements.txt

COPY services/auth-service/app /app/app
COPY services/auth-service/tests /app/tests

ENV PYTHONUNBUFFERED=1

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
