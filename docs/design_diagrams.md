# 四川大学「找卡宝」UML 设计图册与架构视图

本文档汇集系统核心 UML 视图，包含用例图、类图、数据库实体关系图（ER 图）、卡片状态机模型，使用标准 Mermaid 语法绘制。

---

## 1. 系统用例图 (Use Case Diagram)

描述本校师生（失主与拾获者）与系统核心交互场景。

```mermaid
flowchart TD
    User["川大师生 (用户)"]
    Sys["系统核心 (System Core)"]

    subgraph Auth["身份认证域"]
        UC01["统一学号/校园网注册登录"]
        UC02["完善与维护实名档案 (身份证/手机)"]
    end

    subgraph Publish["信息发布与采集域"]
        UC03["发布拾获卡片 (拾卡)"]
        UC04["发布遗失登记 (寻卡)"]
        UC05["自动化 GPS 经纬度获取"]
        UC06["拍摄现场证件照片凭证"]
    end

    subgraph Query["发现与检索域"]
        UC07["浏览公共失物招领流"]
        UC08["按校区/卡片类型多维筛选"]
        UC09["精准检索 (姓名/完整证件号)"]
        UC10["第三方地图路线导航"]
        UC11["原图弹窗查看"]
    end

    subgraph Match["智能引擎与通知域"]
        UC12["全等证件号智能碰撞匹配"]
        UC13["系统级高优先级匹配通知推送"]
        UC14["公共流敏感数据动态脱敏"]
    end

    User --> UC01
    User --> UC02
    User --> UC03
    User --> UC04
    User --> UC07
    User --> UC08
    User --> UC09
    User --> UC10
    User --> UC11

    UC03 -.->|包含| UC05
    UC03 -.->|包含| UC06
    UC03 -.->|触发| UC12

    Sys --> UC12
    Sys --> UC13
    Sys --> UC14
    UC12 -->|命中目标失主| UC13
    UC07 -.->|受限于| UC14
```

---

## 2. 核心业务类图 (Class Diagram)

展示表现层、领域层与数据层核心类及其关联关系。

```mermaid
classDiagram
    class UserProfile {
        +String studentId
        +String name
        +String password
        +String idCard
        +String phone
        +String campus
        +Boolean isVerified
        +Long createdAt
    }

    class LostFoundPost {
        +String id
        +String type
        +String status
        +String title
        +String campus
        +String location
        +String date
        +String note
        +Boolean isDemo
        +String personName
        +String documentNumber
        +Double latitude
        +Double longitude
        +String imageUri
        +Long createdAt
    }

    class PostRepository {
        -PostDatabaseHelper dbHelper
        +getAllPosts() List~LostFoundPost~
        +addPost(LostFoundPost post) Boolean
        +searchPosts(String query) List~LostFoundPost~
        +updatePostStatus(String id, String newStatus) Boolean
        +deletePost(String id) Boolean
    }

    class PostDatabaseHelper {
        -SQLiteDatabase readableDb
        -SQLiteDatabase writableDb
        +insertPost(LostFoundPost post) Long
        +queryAllPosts() List~LostFoundPost~
        +searchPosts(String query) List~LostFoundPost~
        +updateStatus(String id, String status) Int
    }

    class ValidationHelper {
        +validateStudentId(String studentId) String?
        +validateIdCard(String idCard, Boolean isOptional) String?
        +validateName(String name) String?
        +validatePhone(String phone, Boolean isOptional) String?
        +maskPersonName(String name) String
        +maskDocumentNumber(String docType, String docNumber) String
    }

    class MatchNotificationHelper {
        +CHANNEL_ID String
        +ensureChannel(Context context) void
        +isPostMatchingUser(LostFoundPost post, UserProfile user) Boolean
        +sendMatchNotification(Context context, LostFoundPost post) void
    }

    class LocationHelper {
        +getCurrentLocation(Context context, Callback) void
        +getNearestCampus(Double lat, Double lng) String
    }

    class MainViewModel {
        +StateFlow~List~LostFoundPost~~ posts
        +StateFlow~UserProfile?~ currentUser
        +StateFlow~String~ selectedCampus
        +onPublishPost(...) void
        +onSearch(String query) void
        +onSwitchCampus(String campus) void
    }

    MainViewModel --> PostRepository : 依赖持久化网关
    MainViewModel --> MatchNotificationHelper : 触发通知匹配
    PostRepository --> PostDatabaseHelper : 底层数据库操作
    PostRepository --> LostFoundPost : 管理实体集合
    MatchNotificationHelper --> LostFoundPost : 检查卡片信息
    MatchNotificationHelper --> UserProfile : 检查失主档案
    MainViewModel ..> LocationHelper : 获取地理定位
    MainViewModel ..> ValidationHelper : 校验用户输入
```

---

## 3. 数据库实体关系 ER 图 (Entity-Relationship Diagram)

本地 SQLite 核心持久化表结构设计：

```mermaid
erDiagram
    USERS {
        VARCHAR(20) student_id PK "学号 (主键)"
        VARCHAR(50) name "真实姓名"
        VARCHAR(100) password "校园网密码/应用密码"
        VARCHAR(18) id_card "18位居民身份证号码"
        VARCHAR(15) phone "联系电话"
        VARCHAR(20) campus "常用校区"
        BOOLEAN is_verified "是否通过校园认证"
        BIGINT created_at "注册时间戳"
    }

    POSTS {
        VARCHAR(36) id PK "条目 UUID"
        VARCHAR(10) type "类型 (拾卡 / 寻卡)"
        VARCHAR(20) status "状态 (还在原地 / 代为保管 / 已认领)"
        VARCHAR(50) title "证件类别 (校园卡/身份证等)"
        VARCHAR(20) campus "所在校区"
        VARCHAR(100) location "具体物理位置"
        VARCHAR(30) date "发布日期字符串"
        TEXT note "补充备注 (私有字段)"
        BOOLEAN is_demo "是否为演示数据"
        VARCHAR(50) person_name "失主姓名"
        VARCHAR(50) document_number "原始证件号"
        DOUBLE latitude "经度坐标"
        DOUBLE longitude "纬度坐标"
        TEXT image_uri "现场图片 Content URI"
        BIGINT created_at "创建时间戳"
    }

    USERS ||--o{ POSTS : "发布或被匹配关联"
```

---

## 4. 卡片流转生命周期状态图 (State Diagram)

展示失物从遗失、拾获、公示、匹配、认领到归档的完整状态演进过程：

```mermaid
stateDiagram-v2
    [*] --> 丢失发生 : 失主发现证件遗失
    [*] --> 拾获发现 : 拾获者在现场发现卡片

    state 拾获发现 {
        [*] --> 现场核实
        现场核实 --> 放置原地 : 物品安全且易被失主察觉
        现场核实 --> 代为保管 : 处于户外或易丢失区域
    }

    放置原地 --> 公示中_还在原地 : 提交发布并附经纬度/现场照片
    代为保管 --> 公示中_代为保管 : 提交发布并附经纬度/现场照片
    丢失发生 --> 公示中_正在寻找 : 失主发布寻卡登记

    state 智能处理阶段 {
        公示中_还在原地 --> 智能匹配检查
        公示中_代为保管 --> 智能匹配检查
        智能匹配检查 --> 触发实时通知推送 : 证件号与已认证用户完全一致
        智能匹配检查 --> 等待失主检索 : 暂无注册匹配用户
    }

    触发实时通知推送 --> 线下核验认领
    等待失主检索 --> 线下核验认领 : 失主通过姓名/卡号检索定位

    线下核验认领 --> 已被失主认领 : 出示有效证件完成核验
    已被失主认领 --> 已归档 : 交易完成闭环
    已归档 --> [*]
```
