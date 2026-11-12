# -*- coding: utf-8 -*-
import os
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
import matplotlib.patches as patches

# 设置中文字体
plt.rcParams['font.sans-serif'] = ['Microsoft YaHei', 'SimHei', 'DejaVu Sans']
plt.rcParams['axes.unicode_minus'] = False

out_dir = r'D:\Code\MyApplication\docs\images'
os.makedirs(out_dir, exist_ok=True)

def draw_er_diagram():
    fig, ax = plt.subplots(figsize=(10, 6), dpi=200)
    ax.set_xlim(0, 10)
    ax.set_ylim(0, 6)
    ax.axis('off')

    # 实体 1: 用户表 (users)
    rect1 = patches.FancyBboxPatch((0.8, 2.2), 3.2, 3.2, boxstyle="round,pad=0.1", fc='#EBF3FA', ec='#1B4F72', lw=2)
    ax.add_patch(rect1)
    ax.text(2.4, 5.0, 'USER (用户档案表)', ha='center', va='center', fontsize=12, fontweight='bold', color='#1B4F72')
    ax.plot([0.9, 3.9], [4.7, 4.7], color='#1B4F72', lw=1)
    user_attrs = [
        "PK  student_id (学号 13位)",
        "    name (真实姓名)",
        "    password (登录密码)",
        "UK  id_card (身份证 永久锁定)",
        "    phone (手机号 可随时改)",
        "    campus (常驻校区)",
        "    is_verified (认证标记)"
    ]
    for idx, attr in enumerate(user_attrs):
        ax.text(1.0, 4.4 - idx * 0.32, attr, fontsize=9.5, color='#2C3E50')

    # 实体 2: 帖子表 (posts)
    rect2 = patches.FancyBboxPatch((6.0, 1.2), 3.4, 4.2, boxstyle="round,pad=0.1", fc='#FEF9E7', ec='#B7950B', lw=2)
    ax.add_patch(rect2)
    ax.text(7.7, 5.0, 'POST (失物招领帖子表)', ha='center', va='center', fontsize=12, fontweight='bold', color='#B7950B')
    ax.plot([6.1, 9.3], [4.7, 4.7], color='#B7950B', lw=1)
    post_attrs = [
        "PK  id (UUID 唯一标识)",
        "    type (FOUND拾卡/LOST寻卡)",
        "    status (还在原地/代为保管/已归还)",
        "    title (卡片标题)",
        "    campus / location (校区/地点)",
        "    date (拾获时间)",
        "    person_name (失主姓名)",
        "    document_number (证件号)",
        "    latitude / longitude (GPS坐标)",
        "    image_uri (现场照片URI)"
    ]
    for idx, attr in enumerate(post_attrs):
        ax.text(6.2, 4.4 - idx * 0.32, attr, fontsize=9.5, color='#2C3E50')

    # 关联菱形 1: 发布 (1:N)
    rel1 = patches.Polygon([[4.5, 4.2], [5.0, 4.6], [5.5, 4.2], [5.0, 3.8]], closed=True, fc='#E8F8F5', ec='#117A65', lw=1.5)
    ax.add_patch(rel1)
    ax.text(5.0, 4.2, '发布', ha='center', va='center', fontsize=10, fontweight='bold', color='#117A65')
    ax.annotate('', xy=(4.5, 4.2), xytext=(4.0, 4.2), arrowprops=dict(arrowstyle="-", color='#1B4F72', lw=1.5))
    ax.annotate('', xy=(6.0, 4.2), xytext=(5.5, 4.2), arrowprops=dict(arrowstyle="->", color='#B7950B', lw=1.5))
    ax.text(4.1, 4.4, '1', fontsize=10, fontweight='bold', color='#1B4F72')
    ax.text(5.8, 4.4, 'N', fontsize=10, fontweight='bold', color='#B7950B')

    # 关联菱形 2: 智能碰撞匹配 (N:M)
    rel2 = patches.Polygon([[4.5, 2.5], [5.0, 2.9], [5.5, 2.5], [5.0, 2.1]], closed=True, fc='#FDEDEC', ec='#922B21', lw=1.5)
    ax.add_patch(rel2)
    ax.text(5.0, 2.5, '匹配', ha='center', va='center', fontsize=10, fontweight='bold', color='#922B21')
    ax.annotate('', xy=(4.5, 2.5), xytext=(4.0, 2.5), arrowprops=dict(arrowstyle="-", color='#1B4F72', lw=1.5))
    ax.annotate('', xy=(6.0, 2.5), xytext=(5.5, 2.5), arrowprops=dict(arrowstyle="->", color='#B7950B', lw=1.5))
    ax.text(4.1, 2.7, '1', fontsize=10, fontweight='bold', color='#1B4F72')
    ax.text(5.8, 2.7, 'M', fontsize=10, fontweight='bold', color='#922B21')

    plt.tight_layout()
    plt.savefig(os.path.join(out_dir, 'fig_er_diagram.png'), bbox_inches='tight')
    plt.close()

def draw_architecture_diagram():
    fig, ax = plt.subplots(figsize=(10, 6.5), dpi=200)
    ax.set_xlim(0, 10)
    ax.set_ylim(0, 7)
    ax.axis('off')

    # 层级定义
    layers = [
        {"title": "表现层 (Presentation Layer - Jetpack Compose & M3)", "y": 5.4, "h": 1.2, "color": "#D4E6F1", "border": "#2471A3",
         "items": "DiscoverScreen (信息大厅)  |  PublishScreen (智能发布)  |  ProfileScreen (个人中心)  |  AuthDialog & ProfileEditDialog"},
        {"title": "状态管理层 (ViewModel Layer - Kotlin StateFlow & Coroutines)", "y": 4.0, "h": 1.0, "color": "#D5F5E3", "border": "#1E8449",
         "items": "MainViewModel (单一UI状态中枢、生命周期感知、状态流发射、协程分发)"},
        {"title": "领域业务层 (Domain Layer - Pure Kotlin Business Logic)", "y": 2.5, "h": 1.1, "color": "#FCF3CF", "border": "#B7950B",
         "items": "ValidationHelper (GB11643 MOD 11-2算法、学号年份检测、动态脱敏)  |  MatchNotificationHelper (全等碰撞引擎、高优先级横幅通知)"},
        {"title": "数据持久与平台服务层 (Data Layer & Platform Services)", "y": 0.8, "h": 1.3, "color": "#FADBD8", "border": "#B03A2E",
         "items": "PostRepository (仓储网关)  |  PostDatabaseHelper (SQLite引擎)  |  LocationHelper (GPS融合)  |  FileProvider (照片沙箱)"}
    ]

    for l in layers:
        rect = patches.FancyBboxPatch((0.5, l["y"]), 9.0, l["h"], boxstyle="round,pad=0.08", fc=l["color"], ec=l["border"], lw=1.8)
        ax.add_patch(rect)
        ax.text(5.0, l["y"] + l["h"] - 0.28, l["title"], ha='center', va='center', fontsize=11, fontweight='bold', color=l["border"])
        ax.text(5.0, l["y"] + 0.35, l["items"], ha='center', va='center', fontsize=9.2, color='#2C3E50')

    # 连接箭头
    for y_arrow in [5.4, 4.0, 2.5]:
        ax.annotate('', xy=(5.0, y_arrow), xytext=(5.0, y_arrow + 0.2), arrowprops=dict(arrowstyle="->", color='#34495E', lw=2))

    plt.tight_layout()
    plt.savefig(os.path.join(out_dir, 'fig_architecture.png'), bbox_inches='tight')
    plt.close()

def draw_class_diagram():
    fig, ax = plt.subplots(figsize=(11, 7), dpi=200)
    ax.set_xlim(0, 11)
    ax.set_ylim(0, 7)
    ax.axis('off')

    # MainViewModel
    r_vm = patches.Rectangle((3.8, 4.8), 3.4, 1.8, fc='#E8F8F5', ec='#117A65', lw=1.8)
    ax.add_patch(r_vm)
    ax.text(5.5, 6.35, 'MainViewModel', ha='center', fontsize=11, fontweight='bold', color='#117A65')
    ax.plot([3.8, 7.2], [6.1, 6.1], color='#117A65', lw=1)
    ax.text(3.9, 5.8, '+ posts: StateFlow<List<Post>>\n+ currentUser: StateFlow<User?>', fontsize=8.5)
    ax.plot([3.8, 7.2], [5.5, 5.5], color='#117A65', lw=1)
    ax.text(3.9, 5.15, '+ publishPost(post)\n+ saveUser(profile)\n+ updatePostStatus(id, st)', fontsize=8.5)

    # UserProfile
    r_u = patches.Rectangle((0.5, 4.8), 2.8, 1.8, fc='#EBF5FB', ec='#2874A6', lw=1.8)
    ax.add_patch(r_u)
    ax.text(1.9, 6.35, 'UserProfile', ha='center', fontsize=11, fontweight='bold', color='#2874A6')
    ax.plot([0.5, 3.3], [6.1, 6.1], color='#2874A6', lw=1)
    ax.text(0.6, 5.5, '+ studentId: String [PK]\n+ name: String\n+ idCard: String [UK]\n+ phone: String\n+ campus: String', fontsize=8.2)

    # LostFoundPost
    r_p = patches.Rectangle((7.7, 4.6), 2.8, 2.0, fc='#FEF9E7', ec='#B7950B', lw=1.8)
    ax.add_patch(r_p)
    ax.text(9.1, 6.35, 'LostFoundPost', ha='center', fontsize=11, fontweight='bold', color='#B7950B')
    ax.plot([7.7, 10.5], [6.1, 6.1], color='#B7950B', lw=1)
    ax.text(7.8, 5.2, '+ id: String [PK]\n+ type: String\n+ status: String\n+ documentNumber: String\n+ latitude / longitude', fontsize=8.2)

    # PostRepository
    r_repo = patches.Rectangle((3.8, 2.2), 3.4, 1.8, fc='#FDEDEC', ec='#922B21', lw=1.8)
    ax.add_patch(r_repo)
    ax.text(5.5, 3.75, 'PostRepository', ha='center', fontsize=11, fontweight='bold', color='#922B21')
    ax.plot([3.8, 7.2], [3.5, 3.5], color='#922B21', lw=1)
    ax.text(3.9, 3.25, '- dbHelper: PostDatabaseHelper', fontsize=8.5)
    ax.plot([3.8, 7.2], [3.0, 3.0], color='#922B21', lw=1)
    ax.text(3.9, 2.5, '+ getAllPosts(): Flow\n+ publishPost(post)\n+ saveUser(profile)', fontsize=8.5)

    # ValidationHelper
    r_val = patches.Rectangle((0.5, 1.5), 2.8, 2.2, fc='#F4ECF7', ec='#6C3483', lw=1.8)
    ax.add_patch(r_val)
    ax.text(1.9, 3.45, 'ValidationHelper', ha='center', fontsize=10.5, fontweight='bold', color='#6C3483')
    ax.plot([0.5, 3.3], [3.2, 3.2], color='#6C3483', lw=1)
    ax.text(0.6, 2.1, '+ validateStudentId()\n+ validateIdCard() [MOD11-2]\n+ validatePhone()\n+ maskPersonName()\n+ maskDocumentNumber()', fontsize=8.0)

    # MatchNotificationHelper
    r_match = patches.Rectangle((7.7, 1.5), 2.8, 2.0, fc='#EAECEE', ec='#2C3E50', lw=1.8)
    ax.add_patch(r_match)
    ax.text(9.1, 3.25, 'MatchNotificationHelper', ha='center', fontsize=10, fontweight='bold', color='#2C3E50')
    ax.plot([7.7, 10.5], [3.0, 3.0], color='#2C3E50', lw=1)
    ax.text(7.8, 2.0, '+ isPostMatchingUser()\n+ sendMatchNotification()', fontsize=8.2)

    # 关联连线
    ax.annotate('', xy=(3.8, 5.7), xytext=(3.3, 5.7), arrowprops=dict(arrowstyle="->", lw=1.5, color='#2874A6'))
    ax.annotate('', xy=(7.2, 5.7), xytext=(7.7, 5.7), arrowprops=dict(arrowstyle="->", lw=1.5, color='#B7950B'))
    ax.annotate('', xy=(5.5, 4.0), xytext=(5.5, 4.8), arrowprops=dict(arrowstyle="->", lw=1.5, color='#922B21'))
    ax.annotate('', xy=(3.8, 2.8), xytext=(3.3, 2.8), arrowprops=dict(arrowstyle="->", lw=1.5, color='#6C3483'))
    ax.annotate('', xy=(7.2, 2.8), xytext=(7.7, 2.8), arrowprops=dict(arrowstyle="->", lw=1.5, color='#2C3E50'))

    plt.tight_layout()
    plt.savefig(os.path.join(out_dir, 'fig_class_diagram.png'), bbox_inches='tight')
    plt.close()

def draw_activity_flow():
    # 核心业务流程图：两阶段实名安全补录与发布智能碰撞
    fig, ax = plt.subplots(figsize=(10, 6), dpi=200)
    ax.set_xlim(0, 10)
    ax.set_ylim(0, 6)
    ax.axis('off')

    # 节点定义
    steps = [
        {"x": 1.2, "y": 4.8, "w": 1.8, "h": 0.8, "t": "进入个人资料\n点击「待填写」", "c": "#D4E6F1", "b": "#2471A3"},
        {"x": 4.0, "y": 4.8, "w": 2.0, "h": 0.8, "t": "输入18位身份证号\n调用 MOD 11-2 校验", "c": "#D5F5E3", "b": "#1E8449"},
        {"x": 7.2, "y": 4.8, "w": 2.2, "h": 0.8, "t": "阶段1: 格式与唯一性检测\n判断是否已被绑定", "c": "#FCF3CF", "b": "#B7950B"},
        {"x": 7.2, "y": 2.8, "w": 2.2, "h": 0.9, "t": "阶段2: 弹出二次警示弹窗\n提示「一旦绑定永久锁定」", "c": "#FADBD8", "b": "#B03A2E"},
        {"x": 4.0, "y": 2.8, "w": 2.0, "h": 0.9, "t": "用户点击「确认永久绑定」\n写入数据库持久化", "c": "#D5F5E3", "b": "#1E8449"},
        {"x": 1.2, "y": 2.8, "w": 1.8, "h": 0.9, "t": "状态更新为已认证(锁定)\n输入框永久隐藏锁定", "c": "#E8F8F5", "b": "#117A65"}
    ]

    for s in steps:
        rect = patches.FancyBboxPatch((s["x"], s["y"]), s["w"], s["h"], boxstyle="round,pad=0.08", fc=s["c"], ec=s["b"], lw=1.6)
        ax.add_patch(rect)
        ax.text(s["x"] + s["w"]/2, s["y"] + s["h"]/2, s["t"], ha='center', va='center', fontsize=9.2, fontweight='bold', color=s["b"])

    # 连线
    ax.annotate('', xy=(4.0, 5.2), xytext=(3.0, 5.2), arrowprops=dict(arrowstyle="->", lw=2, color='#34495E'))
    ax.annotate('', xy=(7.2, 5.2), xytext=(6.0, 5.2), arrowprops=dict(arrowstyle="->", lw=2, color='#34495E'))
    ax.annotate('', xy=(8.3, 3.7), xytext=(8.3, 4.8), arrowprops=dict(arrowstyle="->", lw=2, color='#34495E'))
    ax.annotate('', xy=(6.0, 3.25), xytext=(7.2, 3.25), arrowprops=dict(arrowstyle="->", lw=2, color='#34495E'))
    ax.annotate('', xy=(3.0, 3.25), xytext=(4.0, 3.25), arrowprops=dict(arrowstyle="->", lw=2, color='#34495E'))

    ax.text(5.0, 1.4, '图 4.2.1: 个人实名凭据两阶段安全补录与永久防篡改流程图', ha='center', fontsize=11, fontweight='bold', color='#1B4F72')

    plt.tight_layout()
    plt.savefig(os.path.join(out_dir, 'fig_activity_flow.png'), bbox_inches='tight')
    plt.close()

def draw_gui_mockups():
    # 绘制5个核心页面的精美线框示意图
    fig, axes = plt.subplots(1, 3, figsize=(12, 6.5), dpi=200)

    # 1. 信息广场
    ax1 = axes[0]
    ax1.set_xlim(0, 100)
    ax1.set_ylim(0, 180)
    ax1.axis('off')
    r1 = patches.FancyBboxPatch((5, 5), 90, 170, boxstyle="round,pad=1.5", fc='#F8FAFC', ec='#CBD5E1', lw=2)
    ax1.add_patch(r1)
    ax1.text(50, 166, '信息广场 (Discover)', ha='center', fontsize=11, fontweight='bold', color='#1E293B')
    # 搜索框
    ax1.add_patch(patches.Rectangle((12, 146), 76, 12, fc='#FFFFFF', ec='#E2E8F0', lw=1))
    ax1.text(20, 152, '输入失主姓名或证件号搜索', fontsize=7.5, color='#94A3B8')
    # 校区Tab
    chips = ["全部校区", "江安", "望江", "华西", "已归还"]
    for i, c in enumerate(chips):
        ax1.add_patch(patches.Rectangle((12 + i*15.5, 133), 14, 8, fc='#184C78' if i==0 else '#E2E8F0', ec='none'))
        ax1.text(19 + i*15.5, 137, c, ha='center', va='center', fontsize=6.5, color='#FFFFFF' if i==0 else '#475569')
    # 卡片1
    ax1.add_patch(patches.Rectangle((12, 85), 76, 42, fc='#FFFFFF', ec='#E2E8F0', lw=1))
    ax1.add_patch(patches.Rectangle((16, 114), 22, 7, fc='#E0F2FE', ec='none'))
    ax1.text(27, 117.5, '校园卡', ha='center', va='center', fontsize=6.5, color='#0369A1', fontweight='bold')
    ax1.add_patch(patches.Rectangle((41, 114), 24, 7, fc='#DCFCE7', ec='none'))
    ax1.text(53, 117.5, '还在原地', ha='center', va='center', fontsize=6.5, color='#15803D', fontweight='bold')
    ax1.text(16, 105, '失主：*明 · 证件号：2023******123', fontsize=7.5, color='#1E293B')
    ax1.text(16, 96, '地点：江安校区 · 一教A座大厅', fontsize=7, color='#64748B')
    ax1.text(76, 88, '查看详情 >', fontsize=7, color='#184C78', fontweight='bold')
    # 卡片2
    ax1.add_patch(patches.Rectangle((12, 36), 76, 42, fc='#FFFFFF', ec='#E2E8F0', lw=1))
    ax1.add_patch(patches.Rectangle((16, 65), 26, 7, fc='#E0F2FE', ec='none'))
    ax1.text(29, 68.5, '居民身份证', ha='center', va='center', fontsize=6.5, color='#0369A1', fontweight='bold')
    ax1.add_patch(patches.Rectangle((45, 65), 28, 7, fc='#FEF3C7', ec='none'))
    ax1.text(59, 68.5, '由我代为保管', ha='center', va='center', fontsize=6.5, color='#B45309', fontweight='bold')
    ax1.text(16, 56, '失主：李*华 · 证件号：5101**********12', fontsize=7.5, color='#1E293B')
    ax1.text(16, 47, '地点：望江校区 · 基础教学楼', fontsize=7, color='#64748B')
    ax1.text(76, 39, '查看详情 >', fontsize=7, color='#184C78', fontweight='bold')
    # 底部导航
    ax1.add_patch(patches.Rectangle((20, 12), 60, 14, fc='#FFFFFF', ec='#CBD5E1', lw=1.5))
    ax1.text(50, 19, '信息广场   |   智能发布   |   个人中心', ha='center', fontsize=7, color='#184C78', fontweight='bold')

    # 2. 智能发布页
    ax2 = axes[1]
    ax2.set_xlim(0, 100)
    ax2.set_ylim(0, 180)
    ax2.axis('off')
    r2 = patches.FancyBboxPatch((5, 5), 90, 170, boxstyle="round,pad=1.5", fc='#F8FAFC', ec='#CBD5E1', lw=2)
    ax2.add_patch(r2)
    ax2.text(50, 166, '登记拾获信息 (Publish)', ha='center', fontsize=11, fontweight='bold', color='#1E293B')
    fields = [
        ("1. 证件类型", "学生证 / 校园卡 [下拉]"),
        ("2. 证件号码", "输入13位学号"),
        ("3. 当前状态", "还在原地 [默认]"),
        ("4. 失主姓名", "输入失主真实姓名"),
        ("5. 拾获校区", "江安校区 [GPS自动推荐]"),
        ("6. 具体地点", "如：青春广场长椅上"),
    ]
    for i, (lbl, val) in enumerate(fields):
        ax2.text(14, 150 - i*18, lbl, fontsize=7.5, color='#334155', fontweight='bold')
        ax2.add_patch(patches.Rectangle((12, 137 - i*18), 76, 10, fc='#FFFFFF', ec='#E2E8F0', lw=1))
        ax2.text(16, 143 - i*18, val, fontsize=7, color='#64748B')
    # 相机按钮
    ax2.add_patch(patches.Rectangle((12, 42), 76, 13, fc='#F1F5F9', ec='#94A3B8', lw=1, ls='--'))
    ax2.text(50, 48.5, '[拍摄] 点击拍摄物品放置位置现场照片', ha='center', fontsize=6.8, color='#475569')
    # 居中主发布按钮
    ax2.add_patch(patches.Rectangle((12, 22), 76, 13, fc='#184C78', ec='none'))
    ax2.text(50, 28.5, '发  布', ha='center', va='center', fontsize=9.5, color='#FFFFFF', fontweight='bold')

    # 3. 个人中心与两阶段确认
    ax3 = axes[2]
    ax3.set_xlim(0, 100)
    ax3.set_ylim(0, 180)
    ax3.axis('off')
    r3 = patches.FancyBboxPatch((5, 5), 90, 170, boxstyle="round,pad=1.5", fc='#F8FAFC', ec='#CBD5E1', lw=2)
    ax3.add_patch(r3)
    ax3.text(50, 166, '个人中心 (Profile & Dialog)', ha='center', fontsize=11, fontweight='bold', color='#1E293B')
    # 顶部用户卡片
    ax3.add_patch(patches.Rectangle((12, 126), 76, 32, fc='#FFFFFF', ec='#E2E8F0', lw=1))
    ax3.text(26, 145, '张三 同学', fontsize=9, fontweight='bold', color='#1E293B')
    ax3.add_patch(patches.Rectangle((56, 142), 18, 6, fc='#DCFCE7', ec='none'))
    ax3.text(65, 145, '已认证', ha='center', va='center', fontsize=6, color='#15803D', fontweight='bold')
    ax3.text(16, 133, '江安校区 · 学号 2023999999999', fontsize=7, color='#64748B')
    # 统计项
    ax3.add_patch(patches.Rectangle((12, 94), 76, 26, fc='#FFFFFF', ec='#E2E8F0', lw=1))
    ax3.text(24, 109, '3\n我的发布', ha='center', fontsize=7.5, color='#184C78', fontweight='bold')
    ax3.text(50, 109, '2\n未认领', ha='center', fontsize=7.5, color='#B45309', fontweight='bold')
    ax3.text(76, 109, '1\n已认领', ha='center', fontsize=7.5, color='#7C3AED', fontweight='bold')
    # 弹窗模拟
    ax3.add_patch(patches.Rectangle((8, 18), 84, 68, fc='#FFFFFF', ec='#184C78', lw=1.5))
    ax3.text(50, 77, '[警示] 确认绑定身份证号', ha='center', fontsize=8.5, fontweight='bold', color='#B91C1C')
    ax3.add_patch(patches.Rectangle((14, 58), 72, 12, fc='#F8FAFC', ec='#E2E8F0', lw=1))
    ax3.text(50, 64, '510101199901011234', ha='center', fontsize=8, color='#184C78', fontweight='bold')
    ax3.text(50, 48, '关键提醒：一旦绑定将永久锁定\n无法自行修改，确定绑定吗？', ha='center', fontsize=6.5, color='#475569')
    ax3.add_patch(patches.Rectangle((14, 25), 32, 11, fc='#E2E8F0', ec='none'))
    ax3.text(30, 30.5, '返回修改', ha='center', va='center', fontsize=7, color='#475569')
    ax3.add_patch(patches.Rectangle((54, 25), 32, 11, fc='#184C78', ec='none'))
    ax3.text(70, 30.5, '确认永久绑定', ha='center', va='center', fontsize=7, color='#FFFFFF', fontweight='bold')

    plt.tight_layout()
    plt.savefig(os.path.join(out_dir, 'fig_gui_mockups.png'), bbox_inches='tight')
    plt.close()

if __name__ == '__main__':
    draw_er_diagram()
    draw_architecture_diagram()
    draw_class_diagram()
    draw_activity_flow()
    draw_gui_mockups()
    print("All design diagrams generated successfully in:", out_dir)
