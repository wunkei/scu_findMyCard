# 四川大学「找卡宝 (FindMyCard)」系统详细设计说明书 (Detailed Design Document)

## 1. 文档概述与设计目标

### 1.1 编写目的
本文档基于软件工程规范，详细阐述四川大学「找卡宝」智能卡片失物招领系统的功能模块划分、UI 架构与设计规范、数据模型、业务流程以及安全控制机制。本文档旨在作为工程开发、功能验证与后期维护的技术蓝本。

### 1.2 核心设计原则
1. **安全性与防冒领 (Security & Anti-Impersonation)**：
   - 核心实名凭据（姓名、学号、居民身份证号）一旦绑定即永久锁定不可自行修改；
   - 未绑定关键信息支持按需补充，但补充过程必须经过严格二次弹窗确认以确保用户知情与数据不可篡改性；
   - 敏感信息动态按类别脱敏，公开信息流不泄露失主完整隐私。
2. **高聚合与低耦合 (High Cohesion & Low Coupling)**：
   - 全面采用 Google 官方推荐的现代 Android 架构（MVI/MVVM + Repository 模式）；
   - UI 表现层与底层存储彻底分离，核心校验算法与匹配算法抽离为纯 Kotlin 领域层（Domain Layer）。
3. **闭环可追溯体验 (Closed-Loop Lifecycle UX)**：
   - 拾卡发布、智能匹配、详情查验、地图导航、实拍大图验卡、归还确认全链路无缝闭环；
   - 支持卡片从“还在原地/代为保管”流转至“已归还失主”并归档。

---

## 2. 系统功能架构与模块划分

```mermaid
graph TD
    subgraph UI_Presentation["表现层 (Presentation Layer)"]
        Nav["FloatingGlassNav 浮动毛玻璃导航"]
        Discover["DiscoverScreen 信息大厅"]
        Publish["PublishScreen 智能发布"]
        Profile["ProfileScreen 个人与统计中心"]
        Register["RegisterScreen 实名注册页"]
        AuthDlg["AuthDialog 登录与重置密码弹窗"]
        EditDlg["ProfileEditDialog 资料管理与补录弹窗"]
    end

    subgraph ViewModel_State["状态流转层 (State Management)"]
        VM["MainViewModel (StateFlow & CoroutineScope)"]
    end

    subgraph Domain_Business["业务领域层 (Domain Layer)"]
        Val["ValidationHelper 算法与动态脱敏"]
        Match["MatchNotificationHelper 智能碰撞匹配"]
        EduAuth["EduroamAuthHelper 统一身份验证"]
    end

    subgraph Data_Repository["数据层 (Data Layer)"]
        Repo["PostRepository 仓储中枢"]
        DB["PostDatabaseHelper SQLite 数据库"]
        Pref["SharedPreferences 用户会话"]
    end

    subgraph Platform_Service["平台交互层 (Platform Services)"]
        Loc["LocationHelper GPS/网络定位"]
        Cam["FileProvider 现场照片沙箱"]
        Map["openMapWithCoordinates 外部导航联动"]
    end

    Discover --> VM
    Publish --> VM
    Profile --> VM
    Register --> VM
    AuthDlg --> VM
    EditDlg --> VM

    VM --> Repo
    VM --> Match
    VM --> Val

    Repo --> DB
    Repo --> Pref

    Publish --> Loc
    Publish --> Cam
    Discover --> Map
    Profile --> Map
```

---

## 3. 详细模块设计与业务交互机制

### 3.1 个人资料中心与两阶段实名安全补录设计 (Profile & Supplementary Binding)

#### 3.1.1 需求与业务逻辑
- **实名凭据不可篡改原则**：
  - 姓名（`name`）、学号（`studentId`）在注册时通过校园认证绑定，永久锁定（显示 🔒 锁图标）；
  - 居民身份证号（`idCard`）在注册时若已填写，永久锁定；若未填写，在个人资料中标记为 `待填写 ✏️`；
  - 手机号（`phone`）作为即时联络途径，支持用户随时按需修改更新；常驻校区（`campus`）支持随时切换。
- **两阶段弹窗确认机制 (Two-Step Confirmation)**：
  - **第 1 阶段**：用户点击实名项右侧的 `待填写 ✏️`，弹出录入弹窗，输入 18 位身份证号码。执行国家标准 MOD 11-2 校验与跨账号防重校验；
  - **第 2 阶段**：校验通过后，自动弹出醒目的二次警示确认弹窗，明确向用户提示该号码将永久绑定至其学号档案且无法自行更改。用户点击“确认永久绑定”后方持久化落库并永久锁定。若因特殊情况变更须联系管理员提交线下证明材料。

#### 3.1.2 补录状态机与流程

```mermaid
sequenceDiagram
    autonumber
    actor User as 用户
    participant Dialog as ProfileEditDialog
    participant FormDlg as 补充绑定弹窗 (Step 1)
    participant ConfirmDlg as 确认永久绑定弹窗 (Step 2)
    participant Val as ValidationHelper
    participant VM as MainViewModel
    participant DB as SQLite 数据库

    User->>Dialog: 打开个人资料编辑弹窗
    Note over Dialog: 识别 idCard 为空，渲染「待填写 ✏️」按钮
    User->>Dialog: 点击「待填写 ✏️」
    Dialog->>FormDlg: 调起录入弹窗 (showBindIdCardDialog=true)
    User->>FormDlg: 输入 18 位居民身份证号并点击「下一步」
    FormDlg->>Val: validateIdCard(inputIdCard)
    alt 格式或校验码非法 / 跨账号已绑定
        Val-->>FormDlg: 返回错误信息
        FormDlg-->>User: 标红展示错误，阻断提交
    else 算法校验通过且未被他人绑定
        FormDlg->>ConfirmDlg: 唤起二次确认弹窗 (showConfirmBindDialog=true)
        ConfirmDlg-->>User: 展示高亮完整号码与永久锁定警示文案
        alt 用户点击「返回修改」
            ConfirmDlg->>FormDlg: 返回上一步重新编辑
        else 用户点击「确认永久绑定」
            ConfirmDlg->>VM: saveUser(updatedProfile)
            VM->>DB: 更新 user 记录 (CONFLICT_REPLACE)
            DB-->>VM: 更新成功
            VM-->>Dialog: 状态刷新，idCard 变为只读脱敏展示并附带 🔒
        end
    end
```

---

### 3.2 信息广场与“我的”界面详情卡片交互设计 (Unified Post Detail Card)

#### 3.2.1 统一卡片视觉与信息呈现
在「发现页/信息大厅」（[DiscoverScreen.kt](file:///D:/Code/MyApplication/app/src/main/java/com/example/myapplication/ui/screens/DiscoverScreen.kt)）与「我的」页面（[ProfileScreen.kt](file:///D:/Code/MyApplication/app/src/main/java/com/example/myapplication/ui/screens/ProfileScreen.kt)）中，卡片信息遵循统一的设计标准：
1. **外层卡片头部胶囊标签化**：
   - 顶部由两个等高的圆角胶囊构成：`[证件类别标签 (如: 校园卡 / 居民身份证)]` 与 `[卡片状态标签 (如: 还在原地 / 由我代为保管 / 已归还失主)]`；
   - 移除旧版重复的副标题与重复状态文本，页面保持极致整洁。
2. **多入口二次点击打开完整详情卡片**：
   - **我的发布**、**未认领卡片**、**已认领卡片** 以及 **智能关联** 列表中的任意卡片，均可点击展开详细信息卡片；
   - 详细卡片包含以下完整信息：
     - 头部：证件名称 + 状态胶囊；
     - 失主姓名与证件号（根据登录身份智能脱敏/实名展示）；
     - 拾获地点与校区、发布时间；
     - 📍 **物品精确经纬度与一键地图导航按钮**：点击 `🗺️ 跳转地图导航查看位置` 通过标准 Geo Intent 呼起外部地图应用；
     - 📷 **现场实拍照片缩略图与大图放大**：展示物品现场实拍，点击缩略图弹出全屏大图预览；
     - 🔄 **一键流转按钮**：提供 `失主已领回 · 标记已归还` / `确认这是我的卡 · 标记已取回`，点击后立即完成数据库状态迁移并归档。

---

### 3.3 发布流程优化与状态逻辑 (Publish Logic & Location/Camera Coupling)

#### 3.3.1 字段联动与智能排布
1. **表单排布**：
   - 第 1 项：证件类型（校园卡 / 居民身份证 / 学生证 / 银行卡等）；
   - 第 2 项：证件号码（实时根据规则校验合法性）；
   - 第 3 项：**当前状态（默认“还在原地” / “由我代为保管”）**；
   - 第 4 项：失主姓名；
   - 第 5 项：拾获校区（优先取已登录用户的常驻校区，支持 GPS 自动反算覆盖）；
   - 第 6 项：拾获时间。
2. **状态与现场信息联动控制**：
   - 若选择 **“还在原地”**：显示“具体地点”输入框与现场拍照按钮。若未拍摄现场照片，发布时自动唤起相机进行拍摄；
   - 若选择 **“由我代为保管”**：卡片由发布者随身携带，系统自动隐藏“具体地点”与拍照输入框，但仍上传 GPS 经纬度，便于失主了解大致拾获区域。

---

### 3.4 交互操作按钮居中与设计规范 (Design System Alignment)

为保证整个系统风格的一致性与严谨的视觉层级，所有全局主操作按钮统一采用 [PrimaryActionButton](file:///D:/Code/MyApplication/app/src/main/java/com/example/myapplication/ui/components/CommonComponents.kt) 组件：
- 内部采用 `Box(contentAlignment = Alignment.Center)` 配合 `Text(textAlign = TextAlign.Center)` 强制文本绝对居中；
- 适用场景：
  - **发布页面**：底部的「发布」按钮；
  - **实名认证弹窗**：登录界面的「登录」按钮、找回密码界面的「验证校园网密码并重设」按钮；
  - **注册页面**：底部的「完成验证并注册」按钮。

---

## 4. 数据库设计与持久化规范

### 4.1 用户表结构 (`users`)

| 字段名称 | 物理类型 | 约束 | 业务说明 | 变更权限 |
| :--- | :--- | :--- | :--- | :--- |
| `student_id` | TEXT | PRIMARY KEY | 四川大学13位学号 | **不可变** (账号主键) |
| `name` | TEXT | NOT NULL | 真实姓名 | **不可变** (实名核心凭据) |
| `password` | TEXT | NOT NULL | 独立登录凭证密码 | 可重置 (须校园网校验) |
| `id_card` | TEXT | UNIQUE | 18位居民身份证号码 | **首次填写后永久锁定** |
| `phone` | TEXT | DEFAULT '' | 11位中国大陆手机号码 | **可随时自主修改** |
| `campus` | TEXT | DEFAULT '江安校区' | 常驻校区 | **可随时自主修改** |
| `is_verified`| INTEGER | DEFAULT 1 | 认证标记位 (1已认证) | 系统维护 |
| `created_at` | INTEGER | NOT NULL | 注册时间戳 | 系统生成 |

### 4.2 拾获/寻卡帖子表结构 (`posts`)

| 字段名称 | 物理类型 | 约束 | 业务说明 |
| :--- | :--- | :--- | :--- |
| `id` | TEXT | PRIMARY KEY | UUID 唯一标识 |
| `type` | TEXT | NOT NULL | 帖子类型 (`FOUND` 拾卡 / `LOST` 寻卡) |
| `status` | TEXT | NOT NULL | 状态 (`还在原地` / `由我代为保管` / `已归还失主`) |
| `title` | TEXT | NOT NULL | 标题 (`${docType} · ${location}`) |
| `campus` | TEXT | NOT NULL | 校区名称 (`江安校区` / `望江校区` / `华西校区`) |
| `location` | TEXT | DEFAULT '' | 拾获具体位置描述 |
| `date` | TEXT | NOT NULL | 拾获/丢失时间 |
| `note` | TEXT | DEFAULT '' | 补充备注 (仅详情页可见，公共流脱敏屏蔽) |
| `person_name` | TEXT | NOT NULL | 失主真实姓名 (用于匹配碰撞) |
| `document_number`| TEXT | NOT NULL | 完整证件号码 (用于匹配碰撞) |
| `latitude` | REAL | NULLABLE | 拾获地点高精度纬度 |
| `longitude` | REAL | NULLABLE | 拾获地点高精度经度 |
| `image_uri` | TEXT | DEFAULT '' | 现场拍摄实拍照片沙箱文件 URI |
| `created_at` | INTEGER | NOT NULL | 发布时间戳 |

---

## 5. 安全体系与质量保障

### 5.1 数据保护与脱敏矩阵
- **公共流 (Discover Screen)**：
  - 姓名执行动态规则脱敏（如 `*四`、`张*丰`、`诸**明`）；
  - 证件号执行动态规则脱敏（身份证保留前6后2，校园卡保留前4后3）；
  - 私人备注（`note`）从公共信息列表中物理剔除。
- **详情弹窗 (Post Detail Dialog)**：
  - 未匹配游客仅能查看脱敏号码与地点；
  - 仅当系统识别当前登录用户证件号与卡片完全匹配时，方可查看高清实拍大图、精准坐标与导航按钮。

### 5.2 软件工程质量标准
1. **自动化单元测试覆盖**：
   - 覆盖 18 位身份证 ISO 7064:1983.MOD 11-2 算法全部边界用例；
   - 覆盖学号年份区间、手机号格式有效性、动态脱敏算法有效性；
   - 覆盖智能匹配引擎的证件碰撞命中与未命中场景（当前测试套件 30 项测试用例持续 100% 通过）。
2. **静态代码质量**：
   - 零编译错误与零致命警告；
   - 采用纯 Kotlin 领域层与轻量级 Compose UI 分层架构，无冗余未用代码。
