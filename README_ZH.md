# Mek Energistics

[English README](README.md)

Mek Energistics 为 Mekanism 机器添加可接入 Applied Energistics 2 自动合成网络的 ME 版本机器。  
这些机器保留 Mekanism 原版机器的处理逻辑、GUI 风格、侧面配置和升级体系，同时增加 AE2 样板供应、网络供电和产物回网能力。

## 主要内容

- 添加 Mekanism 原版机器对应的 ME 版本机器。
- 添加 ME 工厂和 ME 工厂安装器，可将支持的 Mekanism 机器升级为对应 ME 版本。
- 支持不同等级的工厂机器，原版 Mekanism 工厂安装器也可继续升级 ME 机器的工厂等级。
- 机器内置样板总成，可放入编码样板并作为 AE2 合成供应器显示在网络中。
- 支持 AE2 下单后将原料推入机器，并在加工完成后将产物返回 ME 网络。
- 支持连接 AE 网络时使用 AE 能源，同时保留 FE 能源兼容。
- 支持 AE 默认频道传递，机器可参与 AE 网络连接。
- 保留 Mekanism 原版 GUI、侧面配置、自动弹出配置、升级按钮和配置工具交互。
- 在 Mekanism 配置界面中为物品、化学品、流体输出添加 AE 输出开关。
- 支持 JEI 配方查看，并隐藏重复的 ME 工厂机器显示，配方页面仅保留基础 ME 机器作为代表。
- 支持 Jade 显示机器 AE 在线状态。
- 提供 ME 样板自动化公共接口，第三方 Mekanism 机器无需再做 ME 方块变体即可接入样板供应器升级。见 [ME 样板自动化接口](docs/me-pattern-automation.zh.md)。

## 配置选项

Mek Energistics 使用 NeoForge 原生模组配置页面：在 Mods 列表中选择 Mek Energistics，然后点击 Config。客户端配置位于 `config/`，服务端配置位于 `<存档>/serverconfig/`。

| 选项（键名） | 文件 | 默认值 | 范围 | 说明 |
| --- | --- | --- | --- | --- |
| 样板页数（`patternPages`） | 服务端 | 2 | 1–16 | 每台机器内置样板总成的页数，每页 36 个编码样板槽。**修改后需重新进入存档。** 调小后高位槽位会隐藏，但其中的样板仍保存在机器里。 |
| 优先使用网络能量（`preferNetworkEnergy`） | 服务端 | 开启 | – | 机器连接 AE 网络时，优先消耗 AE 网络（及 Applied Flux）能量，再消耗机器本地 FE 缓冲。关闭后优先使用本地缓冲。 |
| 优先使用 Applied Flux FE（`preferAppliedFluxNetworkFe`） | 服务端 | 关闭 | – | 安装 Applied Flux 时，优先消耗 Applied Flux 存储的 FE，再消耗 AE 网络能量。 |
| 默认启用智能样板倍增（`smartPatternMultiplicationDefault`） | 服务端 | 关闭 | – | 新放置机器的默认值，机器自身已保存的设置始终优先。 |
| 隐藏 JEI 机器变体（`hideJeiMachineVariants`） | 客户端 | 开启 | – | 在 JEI 中隐藏重复的 ME 工厂变体（以及 ME 输出接口升级卡）。配方页只保留基础 ME 机器作为催化机器，隐藏的变体仍可合成和使用。 |

服务端选项按存档保存并同步到客户端，多人游戏中只有服务器管理员可以修改。

## 必需依赖

| 模组 | 最低版本 | 说明 |
| --- | --- | --- |
| Minecraft | 1.21.1 | |
| NeoForge | 21.1.238 | |
| Applied Energistics 2 | 19.2.17 | AE 网络、频道、能源和样板供应能力 |
| Mekanism | 10.7.19 | 原版机器、配方、GUI 和工厂体系 |
| Applied Mekanistics | 1.6.3 | 化学品 AE Key 支持 |

## 兼容模组

| 模组 | 兼容内容 |
| --- | --- |
| JEI | 显示 ME 机器可执行的 Mekanism 配方，隐藏重复工厂变体 |
| Jade | 显示机器是否连接 AE 网络 |
| Mekanism Extras | 适配更高等级工厂与相关 ME 机器 |
| Mekanism More Machine（1.4.0+） | 适配扩展机器、大型机器与对应工厂版本 |
| Evolved Mekanism / Evolved Mekanism Extras | 合金炉、化学混合器、固化室、热化器及更高等级工厂 |
| Mekanism Magic | 通过样板自动化接口为其机器提供 ME 升级卡支持 |
| ExtendedAE | 使用石英切割刀在样板访问终端中重命名机器 |
| DataEnergistics、OmniSequence、NeoECOAE、Thunderbolt Core | 计数/批量合成 CPU 可一次推送同一样板的多份原料 |
| Applied Flux | 可选支持网络 FE 存储 |
| Mekanism Generators、AE2 JEI Integration、Advanced AE、Omni Cells、Mekanism Unleashed | 可共同加载 |

若已安装某个可选模组但其版本不受支持，对应联动会自动关闭，并在日志中提示（`Mek Energistics integration ... disabled`）。

## 使用说明

1. 放置 ME 机器并连接到 AE 网络。
2. 打开机器 GUI，通过样板按钮打开机器内置样板总成。
3. 放入 AE2 编码样板。
4. 在 AE 终端下单。
5. 机器会接收原料、执行对应 Mekanism 配方，并根据配置返回产物。

## ME 升级卡

普通 Mekanism 机器也可以不替换方块，直接在 Mekanism 升级槽中安装升级卡接入 ME 网络：

- **ME 样板供应升级**：添加机器内置样板总成。
- **ME 被动合成升级**：需先安装样板供应升级；按间隔直接从 ME 存储提交已安装的样板（间隔 1–72000 tick，单次倍率 1–64）。
- **ME 输出接口升级**：从 ME 存储为机器保持指定物品的库存。不能与上述两种升级同时安装，且接口中仍有库存时无法拆除。

## 常见问题

| 现象 | 排查方法 |
| --- | --- |
| 安装器没有反应 | 查看动作栏提示：可能有其他玩家正打开该机器界面、多方块所需空间被占用，或你没有建造权限。 |
| 附属模组机器不接受 ME 升级卡 | 在日志中搜索 `Mek Energistics integration ... disabled`，说明当前附属模组版本不受支持。 |
| 智能样板倍增被自动关闭 | 某个合成 CPU 推送了预先批量化的原料，日志会写明机器和样板。可在样板窗口中重新开启。 |
| 日志出现 `Skipping invalid encoded pattern` | 该样板已无法解码（例如配方被数据包移除），请重新编码。 |

## 构建

```powershell
.\gradlew.bat build --no-problems-report                  # 编译、单元测试、打包
.\gradlew.bat runGameTestServer                           # 游戏内 GameTest
.\gradlew.bat runClient -Pmekenergistics.debugRuns=true   # 以 DEBUG 日志启动客户端
```
