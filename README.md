# 校园餐厅点餐与评价系统

## 项目简介
本项目是基于 Spring Boot 的餐厅点餐 Web 系统，选题对应实训要求中的“餐厅点餐与评价系统”。系统支持用户注册登录、菜品浏览搜索、点餐车、订单提交与查询、用户评价、管理员后台管理，并内置模拟 AI 点餐助手和评价分析模块。

## 技术栈
- 后端：Spring Boot 3.5.6、Spring Web、Spring Data JPA、Validation
- 数据库：Microsoft SQL Server / SQL Server Express
- 前端：HTML + CSS + JavaScript，静态资源由 Spring Boot 提供
- 接口风格：RESTful API，统一返回 `ApiResponse`
- AI 功能：本地规则 + 简单 NLP 模拟，可扩展为大模型 API

## 目录结构
```text
campus-restaurant-order-system/
  backend/      Spring Boot 后端和前端静态页面
  database/     初始化 SQL 脚本
  docs/         需求分析、使用手册、AI 说明、汇报大纲
  frontend/     前端说明
  README.md
```

## 启动步骤
最简单方式：双击项目根目录中的：

```text
启动点餐系统.bat
```

它会启动后端服务，并自动打开登录页。

如果要停止服务，可以管理员登录后点击右上角“停止服务”，也可以双击：

```text
停止点餐系统.bat
```

先在 SQL Server 中创建数据库。可用 SSMS 或 Azure Data Studio 执行：

```sql
CREATE DATABASE restaurant_order;
```

如果需要手动建表和导入初始数据，执行：

```text
database/init.sql
```

项目默认连接本机 SQL Server Express 的 TCP 端口：

```text
jdbc:sqlserver://localhost:50374;databaseName=restaurant_order;encrypt=true;trustServerCertificate=true
```

默认账号密码：

```text
DB_USERNAME=sa
DB_PASSWORD=123456
```

如果你的 SQL Server 密码不同，启动前先设置环境变量：

```powershell
$env:DB_USERNAME='sa'
$env:DB_PASSWORD='你的SQLServer密码'
```

```powershell
cd E:\web\campus-restaurant-order-system\backend
$env:JAVA_HOME='D:\IntelliJ IDEA 2025.2.1\jbr'
& 'D:\IntelliJ IDEA 2025.2.1\plugins\maven\lib\maven3\bin\mvn.cmd' spring-boot:run
```

也可以使用已构建的 JAR：
```powershell
cd E:\web\campus-restaurant-order-system\backend
& 'D:\IntelliJ IDEA 2025.2.1\jbr\bin\java.exe' -jar target\restaurant-order-system-1.0.0.jar
```

访问地址：`http://localhost:8080/`

数据库配置文件：`backend/src/main/resources/application.properties`

常用连接参数：
- 数据库名：`restaurant_order`
- 实例名：`SQLEXPRESS`
- 当前 TCP 端口：`50374`
- 默认用户：`sa`
- 默认密码：`123456`

## 测试账号
- 普通用户：`user` / `123456`
- 管理员：`admin` / `123456`

## 主要功能
- 用户模块：注册、登录、退出、角色识别
- 菜品模块：分类浏览、菜品搜索、菜品详情展示
- 点餐模块：点餐车、堂食/打包、订单提交、库存扣减、销量累计
- 订单模块：用户订单查询、管理员订单查询、订单状态更新
- 评价模块：用户评分和文字评价、情感标签生成、评价列表
- 后台模块：分类管理、菜品管理、AI 菜品描述生成、评价分析

## AI 功能说明
系统实现了 3 个与业务相关的模拟 AI 功能：
- AI 点餐助手：输入口味、预算、人数、忌口，输出推荐菜品组合和推荐理由。
- 智能客服：回答营业时间、打包自提、退单和推荐相关问题。
- AI 评价分析：统计好评/中性/差评、平均分和热点问题，并输出经营建议。

模拟逻辑位于 `backend/src/main/java/com/example/ordersystem/service/AiService.java`。后续可将该服务替换为 OpenAI、通义千问、DeepSeek、Dify、Coze 等 API 调用。

## 验证结果
已在本机完成构建和接口验证：
- `mvn -DskipTests package`：构建成功
- `GET /api/categories`：返回分类数据
- `POST /api/auth/login`：普通用户登录成功
- `POST /api/orders`：下单成功并生成订单号
- `POST /api/ai/recommend`：返回 AI 推荐组合
- `GET /api/admin/orders`：管理员订单查询成功
- `GET /api/admin/ai/reviews-analysis`：评价分析成功

## 小组成员分工
- 成员 A：需求分析、数据库设计、后端接口
- 成员 B：前端页面、交互联调、用户手册
- 成员 C：AI 模块、测试验证、汇报材料

提交前请将成员 A/B/C 替换为真实姓名。
