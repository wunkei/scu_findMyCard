# 川大寻卡 (SCU FindMyCard)

[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-purple.svg)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-blue.svg)](https://developer.android.com/jetpack/compose)
[![MinSDK](https://img.shields.io/badge/MinSDK-24-orange.svg)](https://developer.android.com)
[![TargetSDK](https://img.shields.io/badge/TargetSDK-37-brightgreen.svg)](https://developer.android.com)

**川大寻卡 (SCU FindMyCard)** 是一款专为四川大学师生打造的校园卡及重要身份凭证（居民身份证、港澳通行证、护照等）寻回与互助认领 Android 应用。

本项目针对高校校园证件遗失痛点，集成了**学号与校园网密码实名认证**、**敏感信息多维度脱敏保护**、**防撞库精确搜索**、**GPS自动经纬度定位**、**现场相机拍照存证**以及**失主精准匹配后一键地图导航与照片查看**等核心特性，致力于提供既安全合规又高效省心的校园互助体验。

---

## 🌟 核心功能特性

### 1. 严格的实名账号体系与隐私安全
- **学号为主键实名制**：以川大 13 位学号（验证入学年份 2000 年至当前年份）为主键，结合姓名与校园网统一认证密码进行注册登录。
- **一人一卡一档**：姓名、身份证等高敏感字段只允许绑定一次，杜绝冒名登记；后续修改需联系管理员核验。
- **便捷找回密码**：核验学号与校园网密码后，即可直接重设应用登录密码。

### 2. 公开信息广场与强隐私脱敏
- **姓名脱敏**：
  - 两位姓名仅显示末位（如 `李四` $\to$ `*四`）；
  - 三位及以上姓名仅保留首末字符，中间全部打码（如 `张三丰` $\to$ `张*丰`、`诸葛孔明` $\to$ `诸**明`）。
- **证件号码脱敏**：
  - 居民身份证显示前 6 位与后 2 位（如 `510107**********34`）；
  - 校园卡显示前 4 位与后 3 位（如 `2023******001`）；
  - 其他证件显示前 2 位与后 2 位（如 `EE****78`）。
- **防撞库精确搜索**：大厅搜索栏**只允许匹配完整姓名或完整证件号码**（完全匹配），严禁通过地点、标题或卡号模糊尾号撞库搜寻他人信息。

### 3. 拾获登记：自动经纬度获取与相机拍照存证
- **GPS 自动经纬度定位**：打开发布界面时，自动通过系统定位服务与地理逆编码识别当前所在校区（江安/望江/华西）及具体地点，并记录高精度经纬度坐标。
- **发布时自动调用相机拍照**：在发布拾获信息时自动调起系统相机（基于 Android 标准 `FileProvider` 安全路径），拍摄物品当前放置现场照片并完成绑定。

### 4. 失主精准匹配专属权限（核验解锁）
- **完全匹配推送与标黄识别**：卡片证件号码与失主实名档案**完全一致**时，系统自动识别为“精准匹配”。
- **匹配后专享解锁内容**：
  - 📍 **显示精确经纬度坐标**：北纬、东经度数精确展示；
  - 🗺️ **一键跳转地图导航**：直接调起原生地图（高德/腾讯/百度/Google 地图，支持网页版降级）进行路线导航；
  - 📷 **查看现场放置照片**：展示物品当前存放状态，支持点击放大查看大图；
  - ✅ **一键标记取回**：失主确认取回后，卡片立即从待认领广场移出，进入归档管理。

### 5. 校区筛选与已归还归档
- 信息大厅支持同级筛选标签：`全部校区`、`江安`、`望江`、`华西`、`已归还`。
- 失主取回卡片后自动移入 `已归还` 归档视图，既方便历史溯源，又保持大厅待认领信息的整洁。

---

## 🛠️ 技术架构与选型

| 技术层级 | 选型 / 方案 | 作用与优势 |
| :--- | :--- | :--- |
| **编程语言** | Kotlin (100%) | 现代化空安全语法、协程原生支持 |
| **UI 框架** | Jetpack Compose + Material 3 | 声明式响应式 UI，无 XML 布局负担 |
| **界面风格** | Kyant Backdrop + 自定义圆角卡片 | 视觉层次清晰，交互流畅动感 |
| **架构模式** | MVVM + StateFlow + 协程 | 数据状态驱动、单向数据流、生命周期安全 |
| **数据持久化** | SQLite (SQLiteOpenHelper) | 原生轻量嵌入式数据库，平滑支持表升级与迁移 |
| **位置与定位** | LocationManager + Geocoder | 混合定位与逆地理编码解析 |
| **图像与相机** | ActivityResultContracts.TakePicture + FileProvider | 严格遵循 Android 存储沙盒与相机安全共享规范 |
| **地图导航** | Android Intent (geo:URI / 地图外部降级) | 无缝联动系统原生地图应用 |
| **数据校验** | ValidationHelper | ISO 7064:1983.MOD 11-2 身份证算法、学号年份规范正则校验 |

---

## 📁 核心项目结构

```text
MyApplication/
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── AndroidManifest.xml              # 权限声明、主入口与 FileProvider 配置
│   │       ├── res/
│   │       │   ├── xml/file_paths.xml           # 相机拍照安全共享文件路径映射
│   │       │   └── values/                      # 主题与颜色资源
│   │       └── java/com/example/myapplication/
│   │           ├── MainActivity.kt              # 主入口 Activity，初始化 ViewModel
│   │           ├── MainScreens.kt               # 核心 Compose 界面（大厅、发布、个人中心等）
│   │           ├── MainViewModel.kt             # UI 状态维护与业务逻辑调度
│   │           ├── PostRepository.kt            # 数据仓库层
│   │           ├── PostDatabaseHelper.kt        # SQLite 本地数据库操作与版本迁移
│   │           ├── LostFoundPost.kt             # 证件卡片数据模型
│   │           ├── UserProfile.kt               # 用户实名档案数据模型
│   │           ├── ValidationHelper.kt          # 学号/身份证校验与姓名/号码脱敏算法
│   │           ├── LocationHelper.kt            # GPS 定位、校区匹配与地理编码
│   │           └── MatchNotificationHelper.kt   # 失主档案精确匹配与通知判定
│   └── build.gradle.kts                         # 模块构建与依赖配置
├── gradle/                                      # Gradle Wrapper 配置
└── README.md                                    # 项目说明文档
```

---

## 🚀 编译与运行环境

- **IDE**：Android Studio Ladybug (2024.2.1) 或更高版本
- **JDK**：OpenJDK 17 / 21
- **Android Gradle Plugin (AGP)**：8.7+
- **Min SDK**：24 (Android 7.0)
- **Target SDK**：37 (Android 15+)

### 命令行构建 APK

```bash
# 编译 Debug APK
./gradlew assembleDebug

# 生成产物路径：app/build/outputs/apk/debug/app-debug.apk
```

---

## 🔒 隐私与合规说明

1. **零第三方追踪**：本项目不接入任何涉及用户追踪或商业推广的第三方 SDK。
2. **公开界面强脱敏**：任何未匹配的普通访客仅能浏览脱敏后的字符碎片，无法反推失主身份或证件全号。
3. **现场信息定向授权**：现场经纬度导航与照片存证仅对**完全匹配的失主本人**开放查看。

---

## 📄 开源仓库与协议

- **GitHub 仓库**：[https://github.com/wunkei/scu_findMyCard](https://github.com/wunkei/scu_findMyCard)
- **代码贡献与建议**：欢迎提交 Issue 或 Pull Request 共同改进！
