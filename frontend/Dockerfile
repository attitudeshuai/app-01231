# syntax=docker/dockerfile:1.4

# 构建阶段 (node:20-alpine 支持 linux/amd64, linux/arm64)
FROM node:20-alpine AS builder

WORKDIR /app

# 设置 npm 镜像源
RUN npm config set registry https://registry.npmmirror.com

# 先复制依赖文件（利用 Docker 缓存层）
COPY package.json package-lock.json* ./

# 使用 BuildKit 缓存 npm 缓存目录，大幅加速依赖安装
RUN --mount=type=cache,target=/root/.npm \
    if [ -f package-lock.json ]; then npm ci --silent; else npm install --silent; fi

# 复制源码（这一步变化最频繁，放最后）
COPY . .

# 使用 BuildKit 缓存 Vite 缓存，加速构建
RUN --mount=type=cache,target=/app/node_modules/.vite \
    npm run build

# 运行阶段 (nginx:alpine 支持 linux/amd64, linux/arm64)
FROM nginx:alpine

# 复制构建产物
COPY --from=builder /app/dist /usr/share/nginx/html

# 复制 nginx 配置
COPY nginx.conf /etc/nginx/conf.d/default.conf

# 暴露端口
EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
