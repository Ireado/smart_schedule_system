# 基于AI的智能课程表安排系统

这是一个基于文档摘要实现的 Spring Boot 项目原型，包含：

- 教师、班级、教室、课程、课表数据模型
- 课程排布接口与三方互斥约束校验
- AI 排课客户端预留接口
- 静态前端页面，可直接初始化示例数据并查看课表

## 技术栈

- Java 21
- Spring Boot 3
- Spring Web + Spring Data JPA
- H2（默认开发运行）
- MySQL（生产/课程要求场景）
- HTML / CSS / JavaScript

## 运行方式

### 1. 默认方式（H2）

```bash
mvn spring-boot:run -s .mvn-settings.xml
```

启动后访问：

- `http://localhost:8080/`
- H2 控制台：`http://localhost:8080/h2-console`

### 2. MySQL 方式

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=mysql -s .mvn-settings.xml
```

或设置环境变量：

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`

示例：

```bash
$env:DB_URL="jdbc:mysql://localhost:3306/ai_schedule_system?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=UTF-8"
$env:DB_USERNAME="root"
$env:DB_PASSWORD="root"
mvn spring-boot:run -Dspring-boot.run.profiles=mysql -s .mvn-settings.xml
```

## 主要接口

- `POST /api/demo/reset` 初始化示例数据
- `POST /api/schedules/generate` 生成课表
- `GET /api/snapshot` 查看系统概览
- `GET /api/timetables/teachers/{teacherId}` 查询教师课表
- `GET /api/timetables/classes/{classId}` 查询班级课表

## AI 接口说明

当前保留了 `AiSchedulingClient` 扩展点：

- 当 `AI_ENABLED=true` 且配置 `AI_ENDPOINT` 时，可在 `HttpAiSchedulingClient` 中接入真实 LLM API
- 当前默认回退到本地排课算法，保证项目可以独立运行

## 当前实现说明

文档中提到“AI 作为核心调度算法引擎”，但没有给出完整接口协议，因此当前版本采用：

- 先保留 AI 调用入口
- 再用本地约束排课算法保证教师、班级、教室不冲突

如果你要继续扩展，下一步建议补充：

- 登录鉴权（教师/学生角色）
- 课程、教师、班级、教室的增删改查页面
- 更复杂的约束条件（连堂课、教师偏好、教室类型、禁排时段）
- 真实大模型排课提示词与结果解析
