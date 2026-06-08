# syntax=docker/dockerfile:1.4

# 构建阶段 (maven:3.9-eclipse-temurin-17 支持 linux/amd64, linux/arm64)
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app

# 配置 Maven 阿里云镜像加速
RUN mkdir -p /root/.m2 && \
    echo '<?xml version="1.0" encoding="UTF-8"?><settings><mirrors><mirror><id>aliyun</id><mirrorOf>central</mirrorOf><url>https://maven.aliyun.com/repository/central</url></mirror></mirrors></settings>' > /root/.m2/settings.xml

# 先复制 pom.xml 下载依赖（利用 Docker 缓存层）
COPY pom.xml .

# 使用 BuildKit 缓存 Maven 本地仓库，大幅加速依赖下载
RUN --mount=type=cache,target=/root/.m2/repository \
    mvn dependency:go-offline -B -q

# 复制源码并构建
COPY src ./src

# 使用 BuildKit 缓存加速构建（跳过测试编译和运行）
RUN --mount=type=cache,target=/root/.m2/repository \
    mvn package -Dmaven.test.skip=true -B -q

# 运行阶段 (eclipse-temurin:17-jre-jammy 支持 linux/amd64, linux/arm64)
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# 设置时区
ENV TZ=Asia/Shanghai

# 创建上传目录
RUN mkdir -p /app/uploads

# 复制构建产物
COPY --from=builder /app/target/*.jar app.jar

# 设置环境变量
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseContainerSupport"

# 暴露端口
EXPOSE 9999

# 启动命令
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
