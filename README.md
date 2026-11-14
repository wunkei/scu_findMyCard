# SCU FindMyCards

[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-purple.svg)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-blue.svg)](https://developer.android.com/jetpack/compose)
[![Architecture](https://img.shields.io/badge/Architecture-Clean%20%2F%20MVVM-blueviolet.svg)](docs/architecture.md)
[![Tests](https://img.shields.io/badge/Unit%20Tests-30%20Passed-brightgreen.svg)](app/src/test)
[![MinSDK](https://img.shields.io/badge/MinSDK-24-orange.svg)](https://developer.android.com)
[![TargetSDK](https://img.shields.io/badge/TargetSDK-37-brightgreen.svg)](https://developer.android.com)

**SCU FindMyCard** 是一款面向四川大学师生的高品质、安全合规、智能互助校园卡及重要身份凭证（居民身份证、校园卡、学生证、通行证等）失物招领 Android 应用。

本项目依据**现代软件工程（Software Engineering）优秀项目标准**设计并重构，严格践行**分层解耦架构 (Clean Architecture / MVVM)**、**单一职责原则 (SRP)**、**防篡改单元测试全覆盖**、**基于 GB 11643-1999 与 ISO 7064 的严密算法校验**以及**行业最高标准的字段级隐私脱敏规范**。

---

## 📚 软件工程项目文档矩阵

| 规范文档 | 内容说明 | 快速导航 |
| :--- | :--- | :--- |
| 📋 **需求规格说明书 (SRS)** | 建设背景、业务场景、用户画像、FR/NFR 功能矩阵与验收指标 | [docs/requirements.md](docs/requirements.md) |
| 🏛️ **系统架构与设计说明书** | Modern Android 分层模型、数据流向、解耦设计原则与时序图 | [docs/architecture.md](docs/architecture.md) |
| 📐 **UML 规范设计图册** | 系统用例图、核心业务类图、数据库 ER 图、卡片状态机模型 (Mermaid) | [docs/design_diagrams.md](docs/design_diagrams.md) |

---

## 🌟 核心业务与技术特性

### 1. 严格的实名账号体系与校园网认证
- **学号为主键实名制**：以川大 13 位学号（验证入学年份 2000 年至当前年份）为主键，结合姓名与校园网统一认证密码进行注册登录。
- **极简极速注册**：仅需填写「真实姓名」、「学号」与「校园网密码」三项即可完成开户；后续可自愿完善身份证用于精准匹配。
- **便捷找回密码**：核验学号与校园网密码后，直接重设应用登录密码。

### 2. 行业高标准的敏感信息多维度脱敏保护
- **姓名脱敏**：
  - 2位姓名只显示末位字符（如 `李四` $\to$ `*四`）；
  - 3位姓名显示首末位字符，中间打码（如 `张三丰` $\to$ `张*丰`）；
  - 4位及以上姓名显示首末位，中间全部屏蔽（如 `诸葛孔明` $\to$ `诸**明`、`买买提·吐尔逊` $\to$ `买*****逊`）。
- **证件号码分级脱敏**：
  - **居民身份证**：仅显示前 6 位与后 2 位（如 `110101**********75`）；
  - **校园卡/学生证**：仅显示前 4 位与后 3 位（如 `2023******001`）；
  - **其他证件**：仅显示前 2 位与后 2 位（如 `EB*****67`）。
- **防撞库防爬虫精确搜索**：大厅搜索栏**仅允许匹配完整姓名或完整证件号码**，禁止模糊泛查与私人备注公开暴晒。

### 3. 拾获登记：自动化定位与相机现场存证
- **GPS 自动经纬度获取**：打开登记界面时，`LocationHelper` 自动通过定位传感器与几何算法识别江安/望江/华西校区并固化精确经纬度。
- **现场实拍照片留证**：基于 Android 原生 `FileProvider` 安全沙箱机制，调起系统相机拍摄证件当前放置现场照片并持久化关联。

### 4. 智能碰撞引擎与系统级推送通知
- **自动碰撞算法**：发布新的「拾卡」记录时，后台自动比对证件号与已认证用户档案。
- **系统级高优先级提醒**：命中目标失主后立即唤醒 Android 系统通知渠道（`IMPORTANCE_HIGH`）发送横幅通知，点击直达卡片并解锁精确经纬度导航与高清原图。

---

## 🏗️ 架构分层工程目录树

经过模块化重构，代码彻底消除“上帝类”，严格划分为三层包结构：

```text
com.example.myapplication/
├── MainActivity.kt                      # 应用容器入口 (由450行瘦身至110行，仅保留胶囊容器与根路由)
├── MainViewModel.kt                     # 统一 UI 状态机与业务流程调度中心
│
├── data/                                # 【数据层 Data Layer】
│   ├── model/                           # 核心数据模型
│   │   ├── LostFoundPost.kt             # 失物/拾获卡片不可变实体 (含经纬度与照片 URI)
│   │   └── UserProfile.kt               # 实名认证用户档案实体
│   ├── local/                           # 本地持久化
│   │   └── PostDatabaseHelper.kt        # SQLiteOpenHelper 原生高性能数据库助手
│   └── repository/                      # 仓储抽象
│       └── PostRepository.kt            # 单一数据源持久化仓储网关
│
├── domain/                              # 【领域层 Domain Layer (纯业务逻辑，100%单测覆盖)】
│   ├── validation/                      # 校验与算法规范
│   │   └── ValidationHelper.kt          # 18位身份证 MOD 11-2 校验、学号/手机校验、动态规则脱敏
│   ├── matching/                        # 智能计算
│   │   └── MatchNotificationHelper.kt   # 证件号全等碰撞引擎与系统通知渠道
│   └── auth/                            # 认证领域
│       └── EduroamAuthHelper.kt         # 校园网统一身份认证协议封装
│
├── util/                                # 【工具与硬件交互层 Util Layer】
│   └── LocationHelper.kt                # GPS / Network 融合定位与校区几何归类计算
│
└── ui/                                  # 【表现层 Presentation Layer】
    ├── components/                      # 原子级复用 UI 组件库
    │   ├── UIConstants.kt               # 全局调色板、语义色、卡片配置常量
    │   ├── UIUtils.kt                   # 地图 Intent 启动器、标题生成、示例数据注入
    │   └── CommonComponents.kt          # PageShell, LostFoundCard, SearchBox, FormTextField 等14个通用组件
    ├── screens/                         # 四大核心独立业务页面
    │   ├── DiscoverScreen.kt            # 信息大厅与多维检索流
    │   ├── PublishScreen.kt             # 智能拾获/寻卡登记与相机拍照录入
    │   ├── ProfileScreen.kt             # 个人认证档案与我的发布管理
    │   └── RegisterScreen.kt            # 极简实名注册认证流
    ├── dialogs/                         # 独立交互弹窗
    │   ├── AuthDialog.kt                # 校园网快速登录/密码重设弹窗
    │   └── ProfileEditDialog.kt         # 个人资料与身份证自愿完善弹窗
    └── navigation/                      # 导航交互
        └── FloatingGlassNav.kt          # 浮动毛玻璃拟态导航栏胶囊组件
```

---

## 🧪 单元测试套件 (Unit Tests)

本项目配备针对 Domain 层核心业务逻辑的高覆盖自动化单元测试，涵盖所有关键算法边界与异常分支：

| 测试类 | 覆盖模块 | 测试用例数 | 状态 |
| :--- | :--- | :--- | :--- |
| `ValidationHelperTest` | 18位身份证算法 (ISO 7064 MOD 11-2)、学号规则、手机号规则、姓名及证件分级脱敏 | 17 | ✅ 100% 通过 |
| `MatchNotificationHelperTest` | 拾卡自动碰撞逻辑、大小写空白不敏感性、寻卡排除、证件号完全匹配 | 6 | ✅ 100% 通过 |
| `LostFoundPostTest` | 卡片实体构造、经纬度/照片URI属性绑定、不可变模型 copy | 3 | ✅ 100% 通过 |
| `UserProfileTest` | 用户实名模型构造、字段默认值、资料更新不可变性 | 4 | ✅ 100% 通过 |
| **总计** | **核心领域层与数据模型** | **30** | **✅ 30 Passed** |

### 运行单元测试命令

```bash
# 执行本地 JVM 单元测试
./gradlew testDebugUnitTest
```

---

## 🚀 构建与运行指引

- **开发工具**：Android Studio Ladybug (2024.2.1) 或更高版本
- **编译工具链**：JDK 17 / 21，Android Gradle Plugin (AGP) 8.7+
- **系统要求**：Android 7.0 (API 24) 及以上

```bash
# 1. 克隆代码仓库
git clone https://github.com/wunkei/scu_findMyCard.git
cd scu_findMyCard

# 2. 编译调试版 APK
./gradlew assembleDebug

# 产物输出位置：app/build/outputs/apk/debug/app-debug.apk
```

---

## 🔒 隐私与合规承诺

1. **零第三方隐私风险**：本项目完全自研，不接入任何商业跟踪或数据挖掘 SDK。
2. **多重安全隔离**：现场实拍照片仅保存在 App 私有沙盒，通过系统标准 `FileProvider` 临时赋权拉起系统相机；
3. **数据最小化原则**：未经过实名精准匹配的用户，绝无任何途径获取失主原始证件号与现场导航坐标。
