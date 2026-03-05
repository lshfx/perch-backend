# Perch Backend 接口简易文档

本文档基于当前项目代码实际暴露的控制器整理，适用于前端联调。

## 1. 通用说明

### 1.1 Base URL

根据当前服务部署地址拼接，以下仅列出接口相对路径，例如：

```text
http://localhost:8080/api/auth/login
```

### 1.2 统一响应结构

除 SSE 流式接口外，项目接口统一返回：

```json
{
  "code": 1,
  "msg": "成功或错误信息",
  "data": {}
}
```

字段说明：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `code` | `number` | `1` 表示成功；`0`、`400`、`401`、`403`、`500` 等表示失败 |
| `msg` | `string` | 提示信息 |
| `data` | `object/null` | 业务数据 |

注意：

- 部分业务异常会返回 HTTP 200，但 `code` 不为 `1`。
- 前端应优先根据 `code` 判断业务成功与否，不要只依赖 HTTP 状态码。

### 1.3 通用请求头

| 请求头 | 是否必填 | 说明 |
| --- | --- | --- |
| `Authorization` | 需要登录的接口必填 | 格式：`Bearer <token>` |
| `Content-Type: application/json` | JSON 接口建议携带 | 用于 `register`、`login`、`refresh` |
| `Content-Type: multipart/form-data` | 文件上传接口必填 | 用于知识库上传 |
| `Accept: text/event-stream` | SSE 接口建议携带 | 用于 AI 流式对话 |

### 1.4 鉴权规则

| 类型 | 说明 |
| --- | --- |
| 无需登录 | 可直接访问 |
| 登录必需 | 需要 `Authorization: Bearer <token>` |
| 管理员权限 | 需要登录且用户角色为 `ADMIN` |

### 1.5 Token 规则

当前 JWT 配置如下：

| 项 | 值 |
| --- | --- |
| Header 名 | `Authorization` |
| 前缀 | `Bearer` |

## 2. 接口清单

### 2.1 认证模块

| 方法 | 路径 | 作用 | 鉴权 |
| --- | --- | --- | --- |
| `POST` | `/api/auth/send-code` | 发送邮箱验证码 | 无需登录 |
| `POST` | `/api/auth/register` | 用户注册并直接返回登录态 | 无需登录 |
| `POST` | `/api/auth/login` | 用户登录 | 无需登录 |
| `POST` | `/api/auth/logout` | 当前 token 登出 | 登录必需 |
| `POST` | `/api/auth/refresh` | 刷新 access token | 无需登录 |
| `GET` | `/api/auth/me` | 获取当前登录用户认证信息 | 登录必需 |
| `GET` | `/api/auth/validate/{tokenId}` | 校验 tokenId 是否有效 | 无需登录 |

#### 2.1.1 发送邮箱验证码

- 方法：`POST`
- 路径：`/api/auth/send-code`
- 作用：向邮箱发送注册验证码

请求参数：

| 参数名 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| `email` | Query | `string` | 是 | 邮箱地址 |

请求头：

| 请求头 | 必填 | 说明 |
| --- | --- | --- |
| 无 | 否 | 不需要登录 |

成功响应示例：

```json
{
  "code": 1,
  "msg": "验证码已发送",
  "data": null
}
```

#### 2.1.2 注册

- 方法：`POST`
- 路径：`/api/auth/register`
- 作用：注册用户，成功后直接返回登录结果
- `Content-Type`：`application/json`

请求体字段：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `email` | `string` | 邮箱注册时必填 | 邮箱 |
| `password` | `string` | 邮箱注册时必填 | 密码，长度 6-20 |
| `emailVerifyCode` | `string` | 邮箱注册时必填 | 邮箱验证码 |
| `wechatCode` | `string` | 微信注册时必填 | 微信登录临时凭证 |
| `nickname` | `string` | 否 | 昵称 |
| `avatarUrl` | `string` | 否 | 头像地址 |
| `deviceInfo` | `string` | 否 | 设备信息 |

说明：

- 支持两种注册方式：邮箱注册、微信注册。
- 至少需要提供 `email` 或 `wechatCode` 其中一种。

请求示例：

```json
{
  "email": "test@example.com",
  "password": "123456",
  "emailVerifyCode": "123456",
  "nickname": "测试用户",
  "avatarUrl": "https://example.com/avatar.png",
  "deviceInfo": "web"
}
```

成功响应 `data` 字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `token` | `string` | 访问 token |
| `refreshToken` | `string` | 刷新 token |
| `userId` | `number` | 用户 ID |
| `nickname` | `string` | 用户昵称 |
| `avatarUrl` | `string` | 用户头像 |
| `role` | `string` | 用户角色，常见为 `USER` / `ADMIN` |
| `email` | `string` | 用户邮箱，微信用户可能为空 |

#### 2.1.3 登录

- 方法：`POST`
- 路径：`/api/auth/login`
- 作用：用户登录
- `Content-Type`：`application/json`

请求体字段：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `email` | `string` | 邮箱登录时必填 | 邮箱 |
| `password` | `string` | 邮箱登录时必填 | 密码 |
| `wechatCode` | `string` | 微信登录时必填 | 微信登录 code |

说明：

- 邮箱登录：传 `email + password`
- 微信登录：传 `wechatCode`

请求示例：

```json
{
  "email": "test@example.com",
  "password": "123456"
}
```

成功响应 `data` 结构同注册接口。

#### 2.1.4 登出

- 方法：`POST`
- 路径：`/api/auth/logout`
- 作用：使当前 token 失效

请求头：

| 请求头 | 必填 | 说明 |
| --- | --- | --- |
| `Authorization` | 是 | 格式：`Bearer <token>` |

请求体：无

成功响应示例：

```json
{
  "code": 1,
  "msg": "登出成功",
  "data": null
}
```

#### 2.1.5 刷新 Token

- 方法：`POST`
- 路径：`/api/auth/refresh`
- 作用：使用 `tokenId + refreshToken` 换取新的 token
- `Content-Type`：`application/json`

请求体字段：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `tokenId` | `string` | 是 | 旧 token 对应的 tokenId |
| `refreshToken` | `string` | 是 | 刷新 token |

请求示例：

```json
{
  "tokenId": "abc123",
  "refreshToken": "xxxxx.yyyyy.zzzzz"
}
```

成功响应 `data` 字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `token` | `string` | 新 access token |
| `refreshToken` | `string` | 新 refresh token |
| `tokenId` | `string` | 新 tokenId |
| `expiration` | `number` | token 有效期，单位毫秒 |
| `tokenPrefix` | `string` | 当前为 `Bearer` |

#### 2.1.6 获取当前登录用户信息

- 方法：`GET`
- 路径：`/api/auth/me`
- 作用：获取当前登录用户认证信息

请求头：

| 请求头 | 必填 | 说明 |
| --- | --- | --- |
| `Authorization` | 是 | 格式：`Bearer <token>` |

成功响应 `data` 字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `username` | `string` | 当前认证用户名 |
| `authorities` | `array` | 当前权限列表 |

响应示例：

```json
{
  "code": 1,
  "msg": null,
  "data": {
    "username": "test@example.com",
    "authorities": [
      {
        "authority": "ROLE_USER"
      }
    ]
  }
}
```

#### 2.1.7 校验 Token

- 方法：`GET`
- 路径：`/api/auth/validate/{tokenId}`
- 作用：校验某个 tokenId 当前是否有效

路径参数：

| 参数名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `tokenId` | `string` | 是 | token 标识 |

成功响应 `data` 字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `valid` | `boolean` | 是否有效 |
| `tokenId` | `string` | token 标识 |

### 2.2 AI 对话模块

| 方法 | 路径 | 作用 | 鉴权 |
| --- | --- | --- | --- |
| `GET` | `/api/ai/stream` | AI 流式聊天接口，返回 SSE | 登录必需 |

#### 2.2.1 AI 流式聊天

- 方法：`GET`
- 路径：`/api/ai/stream`
- 作用：向 AI 发送消息并接收流式响应
- 响应类型：`text/event-stream`

请求参数：

| 参数名 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| `message` | Query | `string` | 是 | 用户输入消息 |
| `sessionId` | Query | `number` | 否 | 会话 ID，不传则后端自动创建 |

请求头：

| 请求头 | 必填 | 说明 |
| --- | --- | --- |
| `Authorization` | 是 | 格式：`Bearer <token>` |
| `Accept` | 否 | 建议传 `text/event-stream` |

响应说明：

| 项 | 说明 |
| --- | --- |
| 响应头 `X-Session-Id` | 当前会话 ID，首次对话时可用于前端保存会话 |
| 响应体 | 流式文本，不是普通 JSON |

示例请求：

```text
GET /api/ai/stream?message=你好&sessionId=1
Authorization: Bearer <token>
Accept: text/event-stream
```

### 2.3 知识库模块

| 方法 | 路径 | 作用 | 鉴权 |
| --- | --- | --- | --- |
| `POST` | `/api/knowledge/ingest/single` | 上传单本知识文件并提交入库任务 | 管理员权限 |

#### 2.3.1 上传单本知识文件

- 方法：`POST`
- 路径：`/api/knowledge/ingest/single`
- 作用：上传文件并触发知识库入库
- `Content-Type`：`multipart/form-data`

请求头：

| 请求头 | 必填 | 说明 |
| --- | --- | --- |
| `Authorization` | 是 | 格式：`Bearer <token>`，且需 `ADMIN` 权限 |

表单参数：

| 参数名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `file` | `file` | 是 | 上传文件 |
| `bookName` | `string` | 否 | 书名 |
| `category` | `string` | 否 | 分类 |
| `chunkSize` | `number` | 否 | 分块大小 |
| `minChunkSizeChars` | `number` | 否 | 最小分块字符数 |
| `minChunkLengthToEmbed` | `number` | 否 | 最小嵌入长度 |
| `maxNumChunks` | `number` | 否 | 最大分块数量 |
| `keepSeparator` | `boolean` | 否 | 是否保留分隔符 |

成功响应 `data` 字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `fileName` | `string` | 原始文件名 |
| `bookName` | `string` | 书名 |
| `category` | `string` | 分类 |
| `taskId` | `string` | 入库任务 ID |

### 2.4 管理员模块

| 方法 | 路径 | 作用 | 鉴权 |
| --- | --- | --- | --- |
| `POST` | `/api/admin/kick-user/{userId}` | 强制指定用户下线 | 管理员权限 |
| `POST` | `/api/admin/revoke-token/{tokenId}` | 撤销指定 token | 管理员权限 |
| `GET` | `/api/admin/user-tokens/{userId}` | 查询用户在线 token 列表 | 管理员权限 |
| `GET` | `/api/admin/online-stats` | 获取在线统计 | 管理员权限 |
| `GET` | `/api/admin/validate-token/{tokenId}` | 校验 token 是否有效 | 管理员权限 |

#### 2.4.1 强制用户下线

- 方法：`POST`
- 路径：`/api/admin/kick-user/{userId}`
- 作用：撤销该用户全部 token

路径参数：

| 参数名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `userId` | `number` | 是 | 用户 ID |

请求头：

| 请求头 | 必填 | 说明 |
| --- | --- | --- |
| `Authorization` | 是 | 格式：`Bearer <token>`，且需 `ADMIN` 权限 |

成功响应 `data` 字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `userId` | `number` | 用户 ID |
| `timestamp` | `number` | 操作时间戳 |

#### 2.4.2 撤销指定 Token

- 方法：`POST`
- 路径：`/api/admin/revoke-token/{tokenId}`
- 作用：撤销指定 token

路径参数：

| 参数名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `tokenId` | `string` | 是 | token 标识 |

成功响应 `data` 字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `tokenId` | `string` | token 标识 |
| `timestamp` | `number` | 操作时间戳 |

#### 2.4.3 查询用户在线 Token 列表

- 方法：`GET`
- 路径：`/api/admin/user-tokens/{userId}`
- 作用：查询某个用户当前所有在线 token

路径参数：

| 参数名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `userId` | `number` | 是 | 用户 ID |

成功响应 `data` 字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `tokens` | `array` | token 列表 |
| `count` | `number` | token 数量 |
| `userId` | `number` | 用户 ID |

`tokens` 列表单项常见字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `tokenId` | `string` | token 标识 |
| `userId` | `number` | 用户 ID |
| `username` | `string` | 用户标识 |
| `deviceInfo` | `string` | 设备信息 |
| `loginTime` | `string` | 登录时间 |
| `lastAccessTime` | `string` | 最近访问时间 |
| `refreshToken` | `string` | 刷新 token |

#### 2.4.4 获取在线统计

- 方法：`GET`
- 路径：`/api/admin/online-stats`
- 作用：获取当前在线用户统计

成功响应 `data` 字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `onlineCount` | `number` | 当前在线 token 数量 |
| `timestamp` | `string` | 统计时间 |

#### 2.4.5 管理员校验 Token

- 方法：`GET`
- 路径：`/api/admin/validate-token/{tokenId}`
- 作用：校验某个 token 是否有效

路径参数：

| 参数名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `tokenId` | `string` | 是 | token 标识 |

成功响应 `data` 字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `valid` | `boolean` | 是否有效 |
| `tokenId` | `string` | token 标识 |

## 3. 前端接入建议

### 3.1 登录态保存

登录/注册成功后建议前端保存以下字段：

| 字段 | 说明 |
| --- | --- |
| `token` | 每次登录态请求放到 `Authorization` 请求头 |
| `refreshToken` | token 失效时用于刷新 |
| `userId` | 前端用户标识 |
| `role` | 用于判断是否显示管理端功能 |

推荐请求头格式：

```text
Authorization: Bearer <token>
```

### 3.2 刷新 token 建议

- 接口返回 `401` 或业务 `code=401` 时，可尝试调用 `/api/auth/refresh`
- 刷新成功后更新本地 `token`、`refreshToken`、`tokenId`
- 刷新失败则跳转登录页

### 3.3 SSE 对接建议

- `/api/ai/stream` 为流式接口，不走普通 JSON 解析
- 前端需按 SSE 或流式响应方式消费
- 首次未传 `sessionId` 时，要从响应头 `X-Session-Id` 中取回会话 ID 并缓存

## 4. 备注

- 当前安全配置中放行了 `/api/ping`，但项目代码中未找到对应控制器实现，因此未写入本文档。
- 文档内容以当前代码实现为准；若后端后续新增控制器或修改 DTO，需要同步更新。
