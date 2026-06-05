FROM python:3.10-slim

WORKDIR /app

# 安装依赖
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# 复制代码
COPY app/ ./app/

# 环境变量
ENV AI_PROVIDER=mock
ENV RABBITMQ_HOST=rabbitmq
ENV RABBITMQ_PORT=5672
ENV RABBITMQ_USER=guest
ENV RABBITMQ_PASS=guest
ENV HOST=0.0.0.0
ENV PORT=8000

EXPOSE 8000

# 启动
CMD ["python", "-m", "app.main"]
