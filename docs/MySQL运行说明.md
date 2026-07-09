# MySQL 运行说明

## 1. 确认服务
请先确认本机 MySQL 服务已经启动。常见服务名包括：

```text
MySQL
MySQL80
```

如果服务未启动，可以在 Windows“服务”里启动 MySQL，或使用你本机安装方式对应的启动命令。

## 2. 创建数据库
项目默认连接本机 MySQL：

```text
localhost:3306
```

如果只需要创建数据库，可以执行：

```sql
CREATE DATABASE IF NOT EXISTS restaurant_order
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;
```

如果需要手动建表和导入初始数据，执行：

```powershell
mysql -u root -p < database/init.sql
```

`database/init.sql` 是 MySQL 初始化脚本，会创建 `restaurant_order` 数据库、建表并插入初始数据。

## 3. 配置账号密码
项目默认使用 MySQL 账号：

```text
用户名：root
密码：123456
```

如果你的 MySQL 密码不同，启动前设置环境变量：

```powershell
$env:DB_USERNAME='root'
$env:DB_PASSWORD='你的MySQL密码'
```

如果你的 MySQL 端口、主机或数据库名不同，也可以覆盖连接串：

```powershell
$env:DB_URL='jdbc:mysql://localhost:3306/restaurant_order?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true'
```

## 4. 启动项目
在项目根目录执行：

```powershell
cd E:\web\campus-restaurant-order-system
.\run.ps1
```

或直接双击：

```text
start-system.bat
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

默认连接串：

```text
jdbc:mysql://localhost:3306/restaurant_order?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
```

常用连接参数：

- 数据库：`restaurant_order`
- 主机：`localhost`
- 端口：`3306`
- 默认用户：`root`
- 默认密码：`123456`
