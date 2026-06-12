# Mek-Energistics: Mekanism 机器适配 Guide

这份 guide 用来沉淀 Mek-Energistics 适配 Mekanism 机器的实际流程。项目的核心不是给原机器外挂一个独立适配器，而是注册一套 `mekenergistics:me_*` 机器：它们继承 Mekanism 原机器或工厂 BlockEntity，保留 Mek 的配方、GUI、侧面配置、升级与缓存逻辑，同时接入 AE2 样板供应、AE 网络能量和产物回网。

后续适配 Mekanism More Machine、Mekanism Extras 或其它机器模组时，也应沿用同一套拆分方式：先把机器元数据放进统一枚举，再选择/新增 BlockEntity 模板，最后补注册、菜单、客户端、JEI、安装器和资源。

## 目录

| 章节 | 主题 |
| --- | --- |
| [01 - 先判断机器类型](01-判断机器类型.md) | 新增机器前的分型判断 |
| [02 - 机器事实来源: MeMekanismMachine](02-机器事实来源.md) | enum 注册、资源命名、升级链 |
| [03 - 选择 BlockEntity 模板](03-选择BlockEntity模板.md) | 模板分派与选择 |
| [04 - 普通机器接入 AE 的必做项](04-普通机器接入AE.md) | MeRecipeMachineAeSupport 接入要点 |
| [05 - 工厂机器接入 AE 的必做项](05-工厂机器接入AE.md) | MeFactoryAeMachine + MeFactoryAeSupport |
| [06 - pushPattern 编写规则](06-pushPattern编写规则.md) | 输入解析、simulate/execute 顺序 |
| [07 - 输出回网规则](07-输出回网规则.md) | AeOutputMode、powered insert、ticker 唤醒 |
| [08 - Mixin accessor](08-MixinAccessor.md) | 暴露 Mek 私有字段 |
| [09 - BlockType、Side Config 和默认物品配置](09-BlockType和SideConfig.md) | ModBlockTypes、sideConfigFor |
| [10 - 菜单和客户端 GUI](10-菜单和客户端GUI.md) | ModMenuTypes、ClientSetup、样板按钮 |
| [11 - JEI](11-JEI.md) | catalyst 注册、隐藏策略 |
| [12 - 安装器和升级链](12-安装器和升级链.md) | MeInstallerTargetResolver |
| [13 - 资源文件清单](13-资源文件清单.md) | blockstate、model、recipe、lang |
| [14 - 可选模组适配边界](14-可选模组适配边界.md) | 软依赖启动流程、典型拆分、排查表 |
| [15 - 新增 Mek 机器的推荐步骤](15-新增Mek机器步骤.md) | 12 步流程 |
| [16 - 新增工厂类型的推荐步骤](16-新增工厂类型步骤.md) | factory type 扩展、Extras tier 链、中文翻译对照 |
| [17 - 验证清单](17-验证清单.md) | 游戏内验证要点 |
| [18 - 常见坑](18-常见坑.md) | 易踩问题汇总 |
| [19 - 后续抽象方向](19-后续抽象方向.md) | 差异收敛到小接口 |
| [20 - 新增其它模组 compat 的模板](20-新增其它模组compat模板.md) | 目录结构、门面方法、接入位置 |
| [21 - 可选模组探测规则](21-可选模组探测规则.md) | ModList + class resource 探测 |
| [22 - compat 分派检查单](22-compat分派检查单.md) | 新增 compat 后的逐项检查 |
| [23 - 适配记录模板](23-适配记录模板.md) | PR/提交说明模板 |
| [24 - 按机器类型的落地 playbook](24-按机器类型的落地playbook.md) | 各类型机器的检查点 |
| [25 - 文件改动路线图](25-文件改动路线图.md) | 改文件的推荐顺序 |
| [26 - 编译和游戏内验证命令](26-编译和游戏内验证命令.md) | gradlew 命令和验证流程 |
| [27 - 当前实现索引](27-当前实现索引.md) | 已有机器族与实现对照表 |
| [28 - 外部工厂共用支撑层](28-外部工厂共用支撑层.md) | MeExternalFactorySupport |
| [29 - 当前 guide 维护规则](29-当前guide维护规则.md) | 文档回填规则 |
| [30 - 症状排查表](30-症状排查表.md) | 按现象排查 |
| [31 - 回归检查](31-回归检查.md) | 已踩过的回归问题 |
| [32 - 代码评审清单](32-代码评审清单.md) | PR 提交前扫描 |
