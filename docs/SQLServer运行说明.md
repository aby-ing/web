# SQL Server 运行说明

## 1. 确认服务
本机检测到 SQL Server Express 服务：

```text
SQL Server (SQLEXPRESS)
```

如果服务没启动，在 Windows 服务中启动 `SQL Server (SQLEXPRESS)`。

## 2. 创建数据库
用 SQL Server Management Studio 或 Azure Data Studio 连接本机实例：

```text
localhost\SQLEXPRESS
```

然后执行：

```sql
CREATE DATABASE restaurant_order;
```

也可以执行 `database/init.sql`，它会创建数据库、建表并插入初始数据。

## 3. 配置账号密码
项目默认使用 SQL Server 登录：

```text
用户名：sa
密码：123456
```

如果你的 `sa` 密码不是 `123456`，启动前设置：

```powershell
$env:DB_USERNAME='sa'
$env:DB_PASSWORD='你的密码'
```

如果 SQL Server 没启用 `sa` 登录，需要在 SSMS 中启用 SQL Server 身份验证，或新建一个数据库用户并把环境变量改成该用户。

## 4. 启动项目
```powershell
cd E:\web\campus-restaurant-order-system
.\run.ps1
```

访问：

```text
http://localhost:8080/
```

## 5. 当前配置
配置文件：

```text
backend/src/main/resources/application.properties
```

连接串：

```text
jdbc:sqlserver://localhost:50374;databaseName=restaurant_order;encrypt=true;trustServerCertificate=true
```

如果你的 SQL Server 端口变了，可以用下面命令查询：

```powershell
sqlcmd -S localhost\SQLEXPRESS -U sa -P 123456 -Q "EXEC xp_readerrorlog 0, 1, N'Server is listening on'"
```

如果你的 SQL Server 不是这个端口，可以启动前覆盖：

```powershell
$env:DB_URL='jdbc:sqlserver://localhost:1433;databaseName=restaurant_order;encrypt=true;trustServerCertificate=true'
```
