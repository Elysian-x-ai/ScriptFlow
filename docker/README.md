# ScriptFlow Docker 快速部署

## 前置条件

- WSL 2 或 Linux 环境
- Docker 和 Docker Compose 已安装

## 快速启动

### 1. 进入 docker 目录

```bash
cd docker
```

### 2. 启动所有服务

```bash
docker-compose up -d
```

### 3. 查看服务状态

```bash
docker-compose ps
```

### 4. 查看服务日志

```bash
# 查看所有服务日志
docker-compose logs -f

# 查看特定服务日志
docker-compose logs -f mysql
docker-compose logs -f rabbitmq
```

### 5. 停止服务

```bash
docker-compose stop
```

### 6. 彻底删除（包括数据）

```bash
docker-compose down -v
```

## 服务访问地址

| 服务 | 访问地址 | 用户名 | 密码 |
|------|---------|--------|------|
| MySQL | `localhost:3306` | `root` | `12345` |
| Redis | `localhost:6379` | 无 | 无 |
| RabbitMQ UI | `http://localhost:15672` | `guest` | `guest` |
| MinIO Console | `http://localhost:9001` | `minioadmin` | `minioadmin` |
| AI Service | `http://localhost:8000` | - | - |

## 初始化步骤

MySQL 会自动执行 `scriptflow-dal/src/main/resources/sql/schema.sql` 进行数据库初始化。

### 手动初始化 MinIO Bucket（可选）

如果需要，可以在 MinIO Console 中创建 `scriptflow` bucket：

1. 打开 http://localhost:9001
2. 使用 `minioadmin`/`minioadmin` 登录
3. 创建名为 `scriptflow` 的 bucket

## 与项目其他组件配合

启动 Docker 服务后，你可以：

1. 启动 Java 后端（在 Windows 或 WSL 中）
2. 启动 Python AI 服务
3. 启动前端

所有服务都会通过 `localhost` 访问 Docker 中的中间件服务。

## 故障排查

### MySQL 连接失败

```bash
# 查看 MySQL 日志
docker-compose logs mysql

# 进入 MySQL 容器
docker exec -it scriptflow-mysql mysql -uroot -p12345
```

### RabbitMQ 连接失败

```bash
# 检查 RabbitMQ 状态
docker exec -it scriptflow-rabbitmq rabbitmqctl status
```

### MinIO 健康检查

```bash
curl http://localhost:9000/minio/health/live
```

## 端口说明

| 端口 | 用途 |
|------|------|
| 3306 | MySQL |
| 6379 | Redis |
| 5672 | RabbitMQ AMQP |
| 15672 | RabbitMQ Management UI |
| 9000 | MinIO API |
| 9001 | MinIO Console |
| 8000 | AI Service API |

