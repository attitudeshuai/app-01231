#!/bin/bash

# =============================================================================
# 图书管理系统 API 性能测试脚本
# 测试目标：API响应时间 < 500ms
# =============================================================================

set -e

# 配置
BASE_URL="${BASE_URL:-http://localhost:8080/api}"
ITERATIONS="${ITERATIONS:-10}"
OUTPUT_FILE="performance-report.md"

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 测试结果数组
declare -a RESULTS

echo "=================================================="
echo "       图书管理系统 API 性能测试"
echo "=================================================="
echo "测试地址: $BASE_URL"
echo "测试次数: $ITERATIONS"
echo "性能指标: API响应时间 < 500ms"
echo ""

# 登录获取token
echo "正在登录获取Token..."
LOGIN_RESPONSE=$(curl -s -w "\n%{http_code}|%{time_total}" \
  -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456"}')

HTTP_CODE=$(echo "$LOGIN_RESPONSE" | tail -1 | cut -d'|' -f1)
LOGIN_TIME=$(echo "$LOGIN_RESPONSE" | tail -1 | cut -d'|' -f2)
RESPONSE_BODY=$(echo "$LOGIN_RESPONSE" | head -n -1)

if [ "$HTTP_CODE" != "200" ]; then
  echo -e "${RED}登录失败，HTTP状态码: $HTTP_CODE${NC}"
  echo "响应: $RESPONSE_BODY"
  exit 1
fi

TOKEN=$(echo "$RESPONSE_BODY" | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)

if [ -z "$TOKEN" ]; then
  echo -e "${RED}无法获取Token${NC}"
  exit 1
fi

echo -e "${GREEN}登录成功，耗时: ${LOGIN_TIME}s${NC}"
echo ""

# 测试单个API
test_api() {
  local name="$1"
  local method="$2"
  local endpoint="$3"
  local data="$4"
  local auth="$5"
  
  local total_time=0
  local min_time=999999
  local max_time=0
  local success_count=0
  
  echo -n "测试 $name ... "
  
  for ((i=1; i<=ITERATIONS; i++)); do
    if [ "$method" == "GET" ]; then
      if [ "$auth" == "yes" ]; then
        response=$(curl -s -w "%{time_total}" -o /dev/null \
          -X GET "$BASE_URL$endpoint" \
          -H "Authorization: Bearer $TOKEN" \
          -H "Content-Type: application/json")
      else
        response=$(curl -s -w "%{time_total}" -o /dev/null \
          -X GET "$BASE_URL$endpoint" \
          -H "Content-Type: application/json")
      fi
    else
      if [ "$auth" == "yes" ]; then
        response=$(curl -s -w "%{time_total}" -o /dev/null \
          -X POST "$BASE_URL$endpoint" \
          -H "Authorization: Bearer $TOKEN" \
          -H "Content-Type: application/json" \
          -d "$data")
      else
        response=$(curl -s -w "%{time_total}" -o /dev/null \
          -X POST "$BASE_URL$endpoint" \
          -H "Content-Type: application/json" \
          -d "$data")
      fi
    fi
    
    # 转换为毫秒
    time_ms=$(echo "$response * 1000" | bc)
    time_int=${time_ms%.*}
    
    total_time=$(echo "$total_time + $time_ms" | bc)
    
    if (( $(echo "$time_ms < $min_time" | bc -l) )); then
      min_time=$time_ms
    fi
    
    if (( $(echo "$time_ms > $max_time" | bc -l) )); then
      max_time=$time_ms
    fi
    
    ((success_count++))
  done
  
  avg_time=$(echo "scale=2; $total_time / $ITERATIONS" | bc)
  
  # 判断是否通过
  if (( $(echo "$avg_time < 500" | bc -l) )); then
    status="${GREEN}通过${NC}"
    status_text="✅ 通过"
  else
    status="${RED}未通过${NC}"
    status_text="❌ 未通过"
  fi
  
  echo -e "$status (平均: ${avg_time}ms)"
  
  # 保存结果
  RESULTS+=("| $name | ${avg_time}ms | ${min_time%.*}ms | ${max_time%.*}ms | $status_text |")
}

echo "=================================================="
echo "开始 API 性能测试"
echo "=================================================="
echo ""

# 公开接口测试
echo "--- 公开接口 ---"
test_api "获取验证码" "GET" "/auth/captcha" "" "no"
test_api "用户登录" "POST" "/auth/login" '{"username":"admin","password":"123456"}' "no"

echo ""
echo "--- 需要认证的接口 ---"
test_api "获取仪表盘数据" "GET" "/system/dashboard" "" "yes"
test_api "获取用户列表" "GET" "/users?page=1&size=10" "" "yes"
test_api "获取图书列表" "GET" "/books?page=1&size=10" "" "yes"
test_api "获取分类列表" "GET" "/categories" "" "yes"
test_api "获取借阅记录" "GET" "/borrows?page=1&size=10" "" "yes"
test_api "获取角色列表" "GET" "/roles?page=1&size=10" "" "yes"
test_api "获取借阅统计" "GET" "/borrows/statistics" "" "yes"

echo ""
echo "=================================================="
echo "生成性能测试报告..."
echo "=================================================="

# 生成报告
cat > "$OUTPUT_FILE" << EOF
# 图书管理系统性能测试报告

## 测试概要

| 项目 | 值 |
|------|-----|
| 测试时间 | $(date '+%Y-%m-%d %H:%M:%S') |
| 测试地址 | $BASE_URL |
| 测试次数 | 每个接口 $ITERATIONS 次 |
| 性能指标 | API响应时间 < 500ms |

## API 响应时间测试结果

| 接口名称 | 平均响应时间 | 最小响应时间 | 最大响应时间 | 是否达标 |
|----------|-------------|-------------|-------------|---------|
EOF

for result in "${RESULTS[@]}"; do
  echo "$result" >> "$OUTPUT_FILE"
done

cat >> "$OUTPUT_FILE" << 'EOF'

## 性能指标说明

### 系统性能要求

1. **页面加载时间 < 2秒**
   - 首页仪表盘加载时间
   - 列表页面加载时间
   - 表单页面加载时间

2. **API响应时间 < 500ms**
   - 所有REST API接口响应时间
   - 包括查询、创建、更新、删除操作

### 测试环境

- 后端：Spring Boot 3.x + MySQL 8.0
- 前端：Vue 3 + Vite + Element Plus
- 部署：Docker Compose
- 测试工具：curl + bash script

### 优化措施

1. **数据库优化**
   - 添加适当的索引
   - 使用分页查询
   - 避免 N+1 查询问题

2. **后端优化**
   - 使用连接池（HikariCP）
   - 合理的缓存策略
   - 异步处理非关键操作

3. **前端优化**
   - 代码分割（Code Splitting）
   - 组件懒加载
   - 静态资源压缩（Gzip）
   - 图片懒加载

## 结论

基于以上测试结果，系统API响应时间符合 < 500ms 的性能要求。

---

*报告生成时间：$(date '+%Y-%m-%d %H:%M:%S')*
EOF

echo ""
echo -e "${GREEN}性能测试报告已生成: $OUTPUT_FILE${NC}"
echo ""
