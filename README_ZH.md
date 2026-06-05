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

## 配置选项

Mek Energistics 使用 NeoForge 原生模组配置页面。可在 Mods 列表中选择 Mek Energistics，然后点击 Config 打开。

| 选项 | 默认值 | 说明 |
| --- | --- | --- |
| 样板页数 | 2 | 设置每台机器内置样板总成的页数。每页有 36 个编码样板槽。 |
| 隐藏 JEI 机器变体 | 开启 | 默认在 JEI 中隐藏重复的 ME 工厂变体。配方页面只保留基础 ME 机器作为显示用催化机器，但工厂变体仍可合成并正常使用。 |
| 优先使用 Applied Flux FE | 关闭 | 安装 Applied Flux 且机器连接 AE 网络时，优先消耗 Applied Flux 存储的 FE，再消耗 AE 网络能量。 |
| 优先使用本地 FE | 开启 | 机器连接 AE 网络时，优先消耗机器本地 FE 缓冲，再消耗网络能量。关闭后优先使用网络能量。 |

## 必需依赖

| 模组 | 说明 |
| --- | --- |
| Minecraft | 1.21.1 |
| NeoForge | 21.1.220 或更高 |
| Applied Energistics 2 | AE 网络、频道、能源和样板供应能力 |
| Mekanism | 原版机器、配方、GUI 和工厂体系 |
| Applied Mekanistics | 化学品 AE Key 支持 |

## 兼容模组

| 模组 | 兼容内容 |
| --- | --- |
| JEI | 显示 ME 机器可执行的 Mekanism 配方，隐藏重复工厂变体 |
| Jade | 显示机器是否连接 AE 网络 |
| Mekanism Extras | 适配更高等级工厂与相关 ME 机器 |
| Mekanism More Machine | 适配扩展机器与对应工厂版本 |
| Mekanism Generators | 可与 Mekanism 能源体系一起使用 |
| AE2 JEI Integration | 与 AE2/JEI 配方查看一起使用 |
| Advanced AE | 可与 AE 网络扩展一起使用 |
| Omni Cells | 可与 AE 存储扩展一起使用 |
| Applied Flux | 可选支持网络 FE 存储 |
| Mekanism Unleashed | 可共同加载 |

## 使用说明

1. 放置 ME 机器并连接到 AE 网络。
2. 打开机器 GUI，通过样板按钮打开机器内置样板总成。
3. 放入 AE2 编码样板。
4. 在 AE 终端下单。
5. 机器会接收原料、执行对应 Mekanism 配方，并根据配置返回产物。

## 构建

```powershell
.\gradlew.bat build --no-problems-report
```
