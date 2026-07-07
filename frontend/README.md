# 前端说明

本项目采用 Spring Boot 静态资源托管方式，前端源码位于：

```text
backend/src/main/resources/static/
  index.html
  styles.css
  app.js
  assets/
```

启动后访问 `http://localhost:8080/` 即可使用完整前端页面。前端通过 `fetch` 调用 `/api/**` REST 接口，不是纯静态展示页。
