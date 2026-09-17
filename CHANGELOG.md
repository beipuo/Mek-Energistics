# Changelog

## 3.0.7

### English

#### Changes

- Added an optional built-in resource pack with alternative textures for the ME factory installer and ME upgrade cards.
- Updated the Evolved Mekanism Extras compile and runtime dependency to `1.2.2` (CurseMaven file `8678947`).

#### Fixes

- Fixed factory tier upgrades losing patterns. Patterns, slot order, and ME settings now transfer through the machine upgrade data.
- Fixed energy handling after installing or removing ME upgrades: cached recipes now switch energy sources immediately, and successful AE energy consumption wakes paused recipes without requiring the machine to be broken and replaced.
- Fixed Metallurgic Infusers and Infusing Factories rejecting item + item → item processing patterns by exposing the infusion-material input.

#### Maintenance

- Added regression coverage for pattern transfer, upgrade energy transitions, and infuser inputs, including idle, blocked-output, and simulated energy checks.
- Added ignore rules for local diagnostics, tool caches, test instances, and preserved workspace artifacts.

### 中文

#### 变更

- 新增可选内置资源包，为 ME 工厂安装器和 ME 升级卡提供替代材质。
- 将 Evolved Mekanism Extras 的编译与运行依赖更新为 `1.2.2`（CurseMaven 文件 `8678947`）。

#### 修复

- 修复工厂升阶时样板丢失的问题，通过机器升级数据保留样板、槽位顺序和 ME 设置。
- 修复安装或卸载 ME 升级后的供能处理：缓存配方现在会立即切换能源来源，成功消耗 AE 能量后会唤醒暂停的配方，无需拆除并重新放置机器。
- 修复冶金灌注机及灌注工厂拒绝“物品＋物品→物品”加工样板的问题，补齐灌注材料输入。

#### 维护

- 增加样板迁移、升级供能切换和灌注输入的回归测试，涵盖空闲、输出堵塞和模拟能量查询。
- 补充本地诊断文件、工具缓存、测试实例和工作区保留文件的忽略规则。

## 3.0.6

### English

#### Change

- Adapted Mekanism Magic original machines to ME pattern-provider upgrades through `IMekanismMagicAutomation`, and registered `IN_WORLD_GRID_NODE_HOST` so NeoForge AE2 can discover the hosts.
- Added the first-party ME pattern-automation SPI (`IMePatternAutomationHost` / `MePatternAutomation`) so third-party Mekanism machines can take pattern-provider upgrades on their original blocks, with English and Chinese author documentation.

### 中文

#### 变更

- 通过 `IMekanismMagicAutomation` 为 Mekanism Magic 原机接入 ME 样板供应，并注册 `IN_WORLD_GRID_NODE_HOST`，使 NeoForge AE2 能发现主机。
- 提供 ME 样板自动化公共接口（`IMePatternAutomationHost` / `MePatternAutomation`），第三方 Mekanism 机器可在原方块上接入样板供应器升级，并附中英文作者文档。

## 3.0.5

### English

#### Change

- Updated the Mekanism Extras compile and runtime dependency to CurseMaven file `8677677` (Mekanism Extras `1.4.1`).
- Added ME variants for the Absolute, Supreme, Cosmic, and Infinite Pressing factories.

#### Fix

- Adapted Mekanism Extras and Mekanism: MoreMachine compatibility to the `1.4.1` renamed factory classes and updated Mekanism: Advanced Factory signatures and generics.
- Routed Pressing factory primary, secondary, and tertiary inputs through the AE2 pattern provider and wrapped `TripleItemToItemRecipe` energy handling.
- Added runtime-gated Mixin accessors and GameTest coverage for the new factory registrations and three-lane input layout.

### 中文

#### 变更

- 将 Mekanism Extras 的编译与运行依赖更新为 CurseMaven 文件 `8677677`（Mekanism Extras `1.4.1`）。
- 新增 Absolute、Supreme、Cosmic 与 Infinite 四种压制工厂的 ME 变体。

#### 修复

- 适配 Mekanism Extras 与 Mekanism: MoreMachine `1.4.1` 的工厂类重命名，以及 Mekanism: Advanced Factory 构造器签名和泛型变化。
- 将压制工厂的主输入、secondary 输入和 tertiary 输入接入 AE2 样板供应器，并补充 `TripleItemToItemRecipe` 能量处理包装。
- 增加按运行时类存在性加载的 Mixin 访问器，并为新工厂注册和三路输入布局增加 GameTest 覆盖。

## 3.0.4

### English

#### Change

- Updated the Mekanism: MoreMachine compile and runtime dependency to CurseMaven file `8659501` (MekMM `1.4.0`) and raised the optional compatibility minimum to `1.4.0`.

#### Fix

- Adapted MekMM compatibility to its `1.4.0` API changes, including the renamed recipe-viewer type, two-parameter advanced factory generics, and BigStack inventory slots on the Large Antiprotonic Nucleosynthesizer.
- Added version-gated compatibility for Mekanism Extras and Evolved Mekanism Extras so their `1.4.0` MekMM integrations do not initialize the unsupported Pressing factory path.

### 中文

#### 变更

- 将 Mekanism: MoreMachine 的编译与运行依赖更新为 CurseMaven 文件 `8659501`（MekMM `1.4.0`），并将可选兼容最低版本提高至 `1.4.0`。

#### 修复

- 适配 MekMM `1.4.0` 的 API 变化，包括配方查看器类型重命名、高级工厂双泛型参数，以及大型反质子核合成机的 BigStack 物品槽位。
- 为 Mekanism Extras 与 Evolved Mekanism Extras 增加按版本限定的兼容处理，避免其 MekMM `1.4.0` 集成初始化不受支持的 Pressing 工厂路径。

## 3.0.3

### English

#### Fix

- Fixed dedicated-server interface configuration synchronization by registering the server-to-client payload in the common network channel and dispatching it through the payload context, avoiding duplicate client-side registration.
- Fixed native AE2 crafting CPU smart multiplication being limited to the machine's immediate empty input capacity. Native CPU submissions now batch the complete extractable task balance into the persistent smart queue, while counted third-party provider APIs retain their capacity negotiation.
- Fixed Metallurgic Infuser reaction and conversion mode routing so item, chemical, and output paths remain consistent after changing the output mode, including the corresponding factory layouts.
- Fixed cached ME pattern I/O layouts not being invalidated when factory or infuser output modes change.

### 中文

#### 修复

- 修复专用服务端的接口配置同步问题：现在会在公共网络通道注册服务端到客户端的数据包，并通过 payload 上下文派发，避免客户端重复注册。
- 修复原版 AE2 合成 CPU 的智能倍增被机器当前即时空余输入容量限制的问题。原版 CPU 现在会将当前可提取的完整任务批量提交到持久化智能队列，同时保留第三方计数供应器的容量协商逻辑。
- 修复冶金灌注机反应模式与转换模式的投料和输出路由，使物品、化学品及产物路径在切换输出模式后保持一致，并同步修正对应工厂布局。
- 修复工厂或灌注机切换输出模式后，缓存的 ME 样板输入输出布局未及时失效的问题。

## 3.0.2

### English

#### Fix

- Fixed a server crash in supported chemical recipe machines where energy wrapping could cast a Solar Neutron Activator to a Rotary Condensentrator. Energy containers are now resolved from the machine's registered containers.

### 中文

#### 修复

- 修复受支持化学配方机器在包装能量时，将太阳能中子活化器错误强转为回转式冷凝机而导致服务端崩溃的问题。现在会从机器已注册的能源容器中解析实际能源容器。

## 3.0.1

### English

#### Fix

- Fixed ME Alloyer, ME Chemixer, ME Solidification Chamber, and ME Thermalizer opening incompatible generic screens instead of their matching Evolved Mekanism GUIs. Native ME variants now also report their built-in AE connection correctly in Jade without requiring an installable ME upgrade.
- Fixed the fluid-output-to-AE button on normal and large Rotary Condensentrators changing only on the client, resetting after the GUI was reopened, and leaving produced fluid in the machine. The selected output mode is now applied and persisted by the server for native ME machines and supported machines with an ME upgrade, while switching the Rotary Condensentrator conversion direction refreshes its cached chemical/fluid I/O ports.

### 中文

#### 修复

- 修复 ME 合金炉、ME 化学混合机、ME 固化室与 ME 热能机错误打开不兼容的通用界面，而非对应 Evolved Mekanism 原生 GUI 的问题。原生 ME 变体现在无需安装式 ME 升级，也能在 Jade 中正确显示其内置 AE 连接状态。
- 修复普通及大型回转式冷凝机的“流体输出到 AE”按钮仅在客户端变化、重新打开 GUI 后恢复关闭，并导致产出流体滞留在机器内的问题。所选输出模式现在会由服务端应用并持久保存，同时覆盖原生 ME 机器与安装 ME 升级的受支持机器；切换回转式冷凝机的转换方向时，也会刷新缓存的化学品/流体输入输出端口。

## 3.0.0

### English

#### Change

- Added the ME Pattern Provider Upgrade, allowing supported existing Mekanism and addon machines and factories to join an AE2 network and expose pattern-provider controls without being converted into separate ME blocks. Installed upgrades, patterns, output routing, terminal names, and visibility settings persist across reloads.
- Added the ME Passive Crafting Upgrade with configurable interval and batch multiplier controls. It periodically extracts complete pattern inputs from connected AE storage, submits them atomically to the machine, restores rejected inputs, and is discoverable through JEI.
- Expanded installable-upgrade coverage across Mekanism, Mekanism: MoreMachine, Mekanism Extras, Evolved Mekanism, and Mekanism Empowered machines and factories. Added dedicated upgrade-card artwork, switched the project license to MIT, and updated the Data Energistics compatibility runtime to 2.4.2 through CurseMaven.

#### Fix

- Fixed ME upgrade state, patterns, factory recipes, AE nodes, and adjacent capabilities not being restored or refreshed reliably after world and recipe reloads.
- Fixed pattern-window slot paging, phantom windows on unsupported addon machines, and Jade AE status appearing when no ME upgrade was active.
- Fixed ME-connected machines exposing network energy only to recipe checks instead of refilling every local energy container before work, which could break GUIs, parallel limits, and machine-specific logic.
- Fixed compatibility crashes involving Mekanism Extras factory input layouts, MekBee machines, and unsupported addon machines scanned by AE2 pattern terminals.

### 中文

#### 变更

- 新增 ME 样板供应器升级，使受支持的现有 Mekanism 及其附属机器与工厂无需转换成独立 ME 方块，即可接入 AE2 网络并使用样板供应器控制。已安装升级、样板、输出路由、终端名称与可见性设置均可在重载后保留。
- 新增 ME 被动合成升级，提供可配置的执行间隔与批次数量。它会定期从已连接的 AE 存储提取完整样板输入，原子地提交给机器，在拒绝时返还材料，并可通过 JEI 查询。
- 将可安装升级覆盖扩展至 Mekanism、Mekanism: MoreMachine、Mekanism Extras、Evolved Mekanism 与 Mekanism Empowered 的机器和工厂；新增专用升级卡贴图，将项目许可证切换为 MIT，并通过 CurseMaven 将 Data Energistics 兼容测试版本更新至 2.4.2。

#### 修复

- 修复世界或配方重载后，ME 升级状态、样板、工厂配方、AE 节点及相邻能力未能可靠恢复或刷新的问题。
- 修复样板窗口翻页槽位错误、不受支持的附属机器显示虚假窗口，以及未启用 ME 升级时 Jade 仍显示 AE 状态的问题。
- 修复接入 ME 的机器仅在配方检查中读取网络能量，却未在工作前补充全部本地能源容器，进而影响 GUI、并行限制与机器专用逻辑的问题。
- 修复 Mekanism Extras 工厂输入布局、MekBee 机器，以及 AE2 样板终端扫描不受支持附属机器时的兼容性崩溃。

## 3.0.0-beta4

### English

#### Fix

- Fixed a server crash when a Mekanism Extras factory with an active ME upgrade resolved its pattern input layout. Base factories no longer directly load a Mixin accessor, while alloying factories retain second-input routing through their dedicated Mixin bridge.

### 中文

#### 修复

- 修复 Mekanism Extras 工厂启用 ME 升级后，解析样板输入布局时引发服务端崩溃的问题。基础工厂不再直接加载 Mixin 访问器，合金工厂则通过专用 Mixin 桥接保留第二输入槽路由。

## 3.0.0-beta3

### English

#### Fix

- Fixed unsupported third-party Mekanism factory screens inheriting the ME upgrade hooks and displaying a phantom pattern window. External machines now expose the pattern window only when they match a supported upgrade profile and have an active ME upgrade.
- Fixed a client crash when paging through a pattern window whose backing container did not provide the corresponding virtual slots. Missing page slots are now cleared without being passed to Mekanism's non-null virtual-slot API.

### 中文

#### 修复

- 修复不受支持的第三方 Mekanism 工厂界面因继承 ME 升级钩子而错误显示样板窗口的问题。外部机器现在仅在匹配受支持的升级配置且 ME 升级已启用时显示样板窗口。
- 修复样板窗口缺少对应容器虚拟槽时，翻页导致客户端崩溃的问题。缺失的页面槽现在会被安全清空，不再传给 Mekanism 要求非空的虚拟槽接口。

## 3.0.0-beta2

### English

#### Fix

- Fixed a server crash when AE2 WCWT scanned an unsupported Mekanism addon machine that inherited the ME upgrade interface without a matching machine profile. Pattern-terminal grouping now falls back to the source block's icon and name instead of dereferencing the absent profile.
- Fixed connected ME machines only exposing AE network energy to recipe checks instead of their real local FE state. Every standalone ME machine and machine with an active ME upgrade now draws the exact missing energy from AE before its server work tick and stores it in all local energy containers, so GUIs, parallel-operation limits, and machine-specific logic see the supplied FE.

### 中文

#### 修复

- 修复 AE2 WCWT 扫描未受支持的 Mekanism 附属机器时，机器虽继承 ME 升级接口但没有匹配配置而引发的服务端崩溃。样板终端分组现在会回退到原方块的图标和名称，不再解引用空配置。
- 修复已连接的 ME 机器仅在配方检查中读取 AE 网络能量、本地 FE 状态却未实际更新的问题。现在所有独立 ME 机器及已启用 ME 升级的原机器都会在服务端工作 tick 前，从 AE 网络抽取本地全部能源容器的精确缺口并写入 FE，使 GUI、并行数限制和机器专用逻辑都能读取到实际供能。

## 3.0.0beta1

### English

#### Change

- Added the new ME upgrade card artwork, switched the mod license to MIT, and archived outdated docs and temporary project files for the 3.0.0beta1 cleanup build.

### 中文

#### 变更

- 接入新的 ME 升级卡贴图，将模组协议改为 MIT，并归档过时文档与临时项目文件，作为 3.0.0beta1 清理构建。

## 2.0.6-beta3

### English

#### Fix

- Fixed smart pattern multiplication still dispatching only one craft when Thunderbolt Core was installed. Enabled ME machines and factories now advertise Thunderbolt's unbounded batch-accounting mode, allowing ordinary AE2, NeoECO, AdvancedAE, and AE2 Lightning Tech time-wheel crafting CPUs to use Thunderbolt's official batch-provider path while remaining limited by each CPU's copy budget, available materials and energy, and the machine's physical input capacity. Disabling smart multiplication retains normal single-craft accounting.

### 中文

#### 修复

- 修复安装 Thunderbolt Core 后，智能样板倍增仍只派发单份配方的问题。开启倍增的 ME 机器与工厂现在会声明 Thunderbolt 的无界批量记账模式，使普通 AE2、NeoECO、AdvancedAE 与 AE2 Lightning Tech 时间轮合成 CPU 均可通过 Thunderbolt 官方批量供应器路径派发多份配方，同时仍受各 CPU 的份数预算、可用材料与能量，以及机器物理输入容量限制；关闭智能倍增时继续使用普通单份记账。

## 2.0.6-beta2

### English

#### Change

- Added counted batch-dispatch compatibility for AE2 Lightning Tech and Thunderbolt Core. With smart pattern multiplication enabled, Thunderbolt crafting CPUs can now send ME machines and factories as many complete crafts as their current physical input capacity accepts in one atomic transfer. Disabling smart multiplication keeps the optimized ordinary single-craft path, while direct batch routing prevents duplicate multiplication and returns every unaccepted craft to the CPU.
- Added AE2 Lightning Tech 2.0.4 and Thunderbolt Core 1.0.2 to the compatibility test runtime and compiled directly against Thunderbolt Core's published batch-provider API.

### 中文

#### 变更

- 新增对 AE2 Lightning Tech 与 Thunderbolt Core 计数批量派发的兼容。开启智能样板倍增后，Thunderbolt 合成 CPU 可根据 ME 机器或工厂当前的物理输入容量，在一次原子传输中派发多份完整配方；关闭智能倍增时仍使用优化后的普通单份派发路径。直接批量路由可避免与 Mek-E 自身倍增逻辑重复计算，并将所有未接收的配方份数交还 CPU。
- 将 AE2 Lightning Tech 2.0.4 与 Thunderbolt Core 1.0.2 加入兼容性测试运行环境，并直接使用 Thunderbolt Core 已发布的批量供应器 API 进行编译。

## 2.0.6-beta

### English

#### Change

- Added counted batch-dispatch compatibility for Neo ECO AE Extension 21.2.0. Its crafting CPU can now send ME machines and factories as many complete crafts as their physical inputs, CPU inventory, available AE power, and remaining task count permit, while preserving atomic rollback and preventing duplicate multiplication even when a batch is reduced to one craft.
- Raised the minimum supported dependency versions, without upper bounds, to Applied Energistics 2 19.2.17, Mekanism 10.7.19, and Applied Mekanistics 1.6.3. Updated the NeoForge development baseline to 21.1.228 and the OmniSequence: Transfinite test runtime to the current 1.3.9 build.

#### Fix

- Fixed existing ME Large Chemical Infusers, ME Large Rotary Condensentrators, and ME Large Antiprotonic Nucleosynthesizers converted with the ME Factory Installer not exposing their physical back-face energy capability until broken and placed again. Their fixed Mekanism: MoreMachine energy port is now registered directly, including for already placed converted machines.

### 中文

#### 变更

- 新增对 Neo ECO AE Extension 21.2.0 的计数批量派发兼容。其合成 CPU 现在可根据 ME 机器或工厂的物理输入容量、CPU 库存、可用 AE 能量与任务剩余数量派发多份完整配方；失败时保持原子回滚，并且即使批量缩减为单份也不会与 Mek-E 自身倍增逻辑重复计算。
- 在不设置上限的前提下，将最低支持版本提高至 Applied Energistics 2 19.2.17、Mekanism 10.7.19 与 Applied Mekanistics 1.6.3；同时将 NeoForge 开发基线更新至 21.1.228，并将 OmniSequence: Transfinite 测试运行环境更新到当前 1.3.9 构建。

#### 修复

- 修复使用 ME 工厂安装器转换后，已经放置的 ME 大型化学灌注机、ME 大型旋转式冷凝机与 ME 大型反质子核合成机无法暴露物理背面能源能力，必须拆除重放才能接入能源的问题。现在会直接注册 Mekanism: MoreMachine 的固定能源端口，已放置并完成转换的机器同样生效。

## 2.0.5

### English

#### Fix

- Fixed highly parallel ME machines and factories reporting insufficient energy after an autocrafting order even when the AE network contained enough FE. Network-backed recipe checks now see the full available local and network energy instead of being capped by the machine's local energy buffer.

### 中文

#### 修复

- 修复高并行 ME 机器与工厂在自动合成下单后，即使 AE 网络中有足够 FE 仍显示能量不足的问题。网络供能配方检查现在会读取本地与网络中的完整可用能量，不再受机器本地能量缓存上限限制。

## 2.0.3

### English

#### Change

- Expanded AE2 memory card support to copy patterns together with item, chemical, and fluid output-to-AE settings, the pattern assembly name, and Pattern Access Terminal visibility.
- Preserved stored block-entity components and machine configuration when dismantling ME machines and factories.
- Made the optional OmniSequence and Data Energistics API contracts self-contained for reproducible builds, and updated the GitHub Actions upload/download steps to v5.
- Set the Minecraft 1.21.1 NeoForge development and build baseline to 21.1.220.

#### Fix

- Refreshed AE nodes, published patterns, and neighboring capabilities immediately after an installer converts a machine, so the converted ME machine can accept autocrafting jobs without first being broken and placed again.

### 中文

#### 变更

- 扩展 AE2 内存卡支持：除样板外，现在还会复制物品、化学品和流体的输出至 AE 设置、样板装配名称，以及是否在样板访问终端中显示。
- 拆除 ME 机器与工厂时保留其方块实体组件和机器配置。
- 将 OmniSequence 与 Data Energistics 的可选 API 契约改为项目内自包含，使构建可复现，并将 GitHub Actions 的上传与下载步骤更新至 v5。
- 将 Minecraft 1.21.1 的 NeoForge 开发与构建基线设为 21.1.220。

#### 修复

- 安装器转换机器后会立即刷新 AE 节点、已发布样板和相邻能力，使转换后的 ME 机器无需先拆除重放即可正常接收自动合成订单。

## 2.0.2

### English

#### Change

- Added counted batch-provider integration for Data Energistics, allowing its crafting CPU to negotiate the number of complete crafts that an ME machine or factory can currently accept and transfer the batch atomically.
- Added Omni Batch Provider API v1 integration for OmniSequence: Transfinite 1.3.9, including ordinary ME machines and factory tiers, while preventing duplicate multiplication when an Omni-managed crafting CPU controls the batch.
- Replaced the fixed 1,048,576-copy smart-pattern ceiling with capacity-aware `long` sizing, so large machines can use their actual available input capacity while remaining bounded by the crafting job, input amounts, energy, and atomic routing checks.
- Updated the NeoForge development and build baseline from 21.1.220 to 21.1.238 for Minecraft 1.21.1.

#### Fix

- Preserved pattern input-slot identities and substituted keys during Omni batch admission and used the authoritative delivered totals at commit time, avoiding incorrect assumptions when AE2 substitutions change keys or ratios.
- Kept returned-container, reusable-input, malformed, incomplete, or capacity-changed batches on safe rejection or normal one-craft dispatch paths without partially consuming inputs.

### 中文

#### 变更

- 新增 Data Energistics 计数批处理供应器适配，使其合成 CPU 能按 ME 机器或工厂当前可接收的完整配方数量进行协商，并原子地移交整批材料。
- 新增 OmniSequence: Transfinite 1.3.9 的 Omni Batch Provider API v1 适配，覆盖普通 ME 机器与各等级 ME 工厂；当批处理由 Omni 管理的合成 CPU 执行时，会避免与本模组的倍增逻辑重复计算。
- 移除智能样板固定 1,048,576 份的上限，改为基于机器真实输入容量的 `long` 范围计算，同时仍受合成任务余量、单份输入数量、AE 能量与原子路由检查约束。
- 将 Minecraft 1.21.1 的 NeoForge 开发与构建基线从 21.1.220 更新至 21.1.238。

#### 修复

- Omni 批处理准入现在会保留样板输入槽位及替代材料键，并在提交时使用 API 交付的权威材料总量，避免 AE2 替代材料改变键或比例时产生错误推算。
- 对带返还容器或可复用输入的配方，以及畸形、不完整或容量已变化的批次，保持安全拒绝或回退到普通单份派发，不会部分吞入材料。

## 2.0.1

### English

#### Fix

- Fixed a game crash when ME autocrafting sent recipes such as nutritional liquid to the Mekanism Extras ME Infinite Dissolving Factory by routing conversion items to the factory's real chemical input slot instead of its nullable inherited extra slot.
- Made shared item, chemical, and fluid input adapters reject unavailable optional-machine ports safely, preventing equivalent null-slot crashes when addon machine layouts are missing or change.

### 中文

#### 修复

- 修复 ME 自动合成向 Mekanism Extras 的 ME 悖论无限溶解工厂下单营养液等配方时发生的游戏崩溃；转换物品现在会送入工厂真实的化学品输入槽，而非继承得到的可空附加槽。
- 共用物品、化学品与流体输入适配器现在会安全拒绝不可用的可选机器端口，避免附属模组机器布局缺失或变化时发生同类空槽崩溃。

## 2.0.0

### English

#### Change

- Moved the item, chemical, and fluid output-to-AE controls into the shared pattern window and added a persisted per-machine toggle for showing patterns in the Pattern Access Terminal.
- Expanded Mekanism: MoreMachine large-machine networking so AE2 cables can connect anywhere on an exposed multiblock surface, with correctly owned nodes for AE2 security.
- Matched ME factory energy usage and storage to the base machine and factory tier it mirrors, including Evolved Mekanism, Mekanism: MoreMachine, Mekanism Extras, and Evolved Mekanism Extras machines and factories.
- Reduced per-tick overhead by caching stable pattern I/O layouts, optional-mod lookups, factory tier reflection, pattern-slot views, and machine block lookups.
- Reorganized machine definitions, energy profiles, I/O profiles, factory GUI geometry, and optional compatibility boundaries, with stronger transaction, output-drain, save-compatibility, and architecture tests.

#### Fix

- Fixed `patternPages` and `preferLocalFe` configuration values not being synchronized from the server, which could make clients address the wrong machine inventory slots.
- Fixed optional large-machine and Evolved Mekanism Extras mixins loading when their target classes were absent, and made missing optional block entities or ExtendedAE rename hooks degrade safely instead of crashing startup or world loading.
- Fixed ME factories using a flat 50 J/t cost and 2,000,000 J buffer instead of the upstream machine's configured energy values and tier process count.
- Fixed Jade AE status tooltips using unrelated AE2 translation keys and added consistent localized text for the missing-channel state.

### 中文

#### 变更

- 将物品、化学品和流体的 AE 输出控制移入共用样板窗口，并新增按机器持久化的“是否在样板访问终端中显示”开关。
- 扩展 Mekanism: MoreMachine 大型机器的网络连接，使 AE2 线缆可连接多方块结构任意外露表面，并为所有节点设置正确的 AE2 安全所有者。
- 使 ME 工厂的能耗与储能匹配其对应基础机器和工厂等级，覆盖 Evolved Mekanism、Mekanism: MoreMachine、Mekanism Extras 与 Evolved Mekanism Extras 的机器和工厂。
- 缓存稳定的样板 I/O 布局、可选模组检测、工厂等级反射结果、样板槽视图与机器方块查询，降低每 tick 的重复开销。
- 重构机器定义、能量配置、I/O 配置、工厂 GUI 布局及可选兼容边界，并加强事务安全、输出提取、存档兼容与架构测试。

#### 修复

- 修复 `patternPages` 与 `preferLocalFe` 配置未从服务端同步，导致客户端可能访问错误机器物品栏槽位的问题。
- 修复大型机器与 Evolved Mekanism Extras 的 Mixin 在目标类不存在时仍会加载的问题；当可选方块实体或 ExtendedAE 重命名钩子缺失时，现在会安全降级而非导致启动或世界加载崩溃。
- 修复 ME 工厂统一使用 50 J/t 与 2,000,000 J 储能，而未采用上游机器配置能耗及对应等级并行数的问题。
- 修复 Jade 的 AE 状态提示错误使用无关 AE2 翻译键的问题，并补充统一的“缺少频道”本地化文本。

## 2.0.0-beta

### Change

- Rebuilt ME machine integration around a shared AE support and I/O adapter layer, replacing duplicated lifecycle, pattern, input, output, and smart-batch implementations across Mekanism and supported addons.
- Changed pattern delivery to route actual AE item, chemical, and fluid keys transactionally into declared machine ports without selecting a path from the Mekanism recipe type. Failed deliveries now roll back instead of leaving partial inputs.
- Migrated native Mekanism, Mekanism: MoreMachine, Mekanism Extras, Evolved Mekanism, and Evolved Mekanism Extras machines and factories to the shared input routing and actual-output collection model.
- Centralized optional machine compatibility, registration, client setup, generated resources, factory tier graphs, and installer routes in the compatibility catalog.
- Added ME versions of the Mekanism: MoreMachine Large Rotary Condensentrator, Large Solar Neutron Activator, Large Electrolytic Separator, Large Chemical Infuser, and Large Antiprotonic Nucleosynthesizer.
- Matched the five large machines' upstream models, baked transforms, collision and selection bounds, renderers, menus, recipes, loot, and JEI integration without adding Mekanism side-configuration pages.
- Added independent item, chemical, and fluid output-to-AE controls to the shared pattern window, including support for actual fluid and chemical byproducts.
- Updated the machine adaptation guide for the catalog-driven support and I/O adapter architecture.

### Fix

- Fixed ME nodes failing to reconnect after chunk unloads, world reloads, or reuse of the same block entity instance by recreating destroyed managed nodes from retained state.
- Fixed large Mekanism: MoreMachine nodes so cables can connect through the center of any horizontal face without proxying one node into itself, and deferred large-node setup until the block entity is fully constructed.
- Fixed encoded patterns not being republished reliably after loading, while preserving existing pattern slots, priorities, terminal names, output modes, and pending smart-pattern work.
- Fixed pattern-terminal node access and restored optional Data Energistics interaction after network and world reloads.
- Fixed smart-pattern capacity checks for upgraded input slots, guarded extra-slot limit probing, and kept failed multi-input transactions atomic.
- Fixed ME Metallurgic Infuser conversion patterns so convertible items can enter the extra slot, and corrected pattern-slot quick-move behavior.
- Fixed large-machine item models, lighting and occlusion behavior, dedicated menu resolution, and the Large Antiprotonic Nucleosynthesizer renderer path.

## 1.0.7

### Fix

- Fixed a client crash when showing details for ME factory item tooltips if Mekanism reports no known item containers for the factory stack.

## 1.0.5

### Change

- Added EvolvedMekanism machine compatibility for ME Solidification Chamber, ME Thermalizer, and ME Chemical Mixer.
- Added smart pattern multiplication and faster pattern processing for ME machines and factories.
- Added safer shared AE output, pattern insertion, and network-energy helpers across Mekanism, Mekanism Extras, Mekanism: MoreMachine, EvolvedMekanism, and EvolvedMekanismExtras machines.
- Added a CurseForge publish workflow for GitHub release based publishing.
- Improved ME pattern-machine menus, quick move behavior, and configurable tile screens.

### Fix

- Fixed recipe AE support initialization order for regular recipe machines, chemical machines, Mekanism: MoreMachine machines, and compat factories.
- Fixed malformed encoded pattern handling so invalid third-party patterns are skipped safely instead of crashing the machine integration path.
- Fixed AE-backed energy container construction order and recipe energy usage wrappers.
- Fixed optional EvolvedMekanism, EvolvedMekanismExtras, Mekanism Extras, and Mekanism: MoreMachine factory support delegates.
- Fixed client setup registration on the mod event bus.

## 1.0.0

### Change

- Added smart pattern multiplication for ME pattern slots, so large autocrafting requests can place repeated patterns more conveniently when the machine has room.
- Improved ME machine items to look and read more like their Mekanism counterparts, including colored machine names and clearer item tooltips.
- Improved ME machine registration and item behavior consistency across Mekanism, Mekanism: MoreMachine, Mekanism Extras, and Evolved Mekanism Extras integrations.
- Reorganized the machine adaptation guide for pack makers and future ME machine support.

### Fix

- Fixed ME machine wrench dismantling so machine data, upgrades, side configuration, energy, inventory contents, and installed AE patterns are preserved correctly.
- Fixed ME Factory Installer upgrades across Mekanism Extras and Evolved Mekanism Extras factory chains.
- Fixed ME machine item tooltips and stored-data display by backing item behavior with the expected capabilities.
- Fixed ME Isotopic Centrifuge and ME Centrifuging Factory item icons being too large in inventories.
- Fixed ME centrifuging factory item lighting so their inventory icons match the original factory style more closely.
- Fixed ExtendedAE renaming support so quartz cutting knives can open the renaming screen on ME machines when ExtendedAE is installed.
- Fixed renamed ME machine drops so they restore the machine name through the normal item name instead of saving a separate pattern-terminal name.

## 0.0.14-beta

- Fixed Mekanism: MoreMachine CNC Stamper pattern insertion so non-consumed mold items can stay preloaded in the extra slot while AE patterns only provide consumed input items.
- Applied the same non-consumed mold handling to MekMM and Mekanism Extras stamping factories.
- Improved shared AE pattern insertion helpers for single-item inputs with required extra slots.
- Removed unsupported ME variants for Mekanism utility/configuration machines that do not need AE pattern support.
- Verified the update with Gradle build.

## 0.0.13-beta

- Adapted Mekanism Extras and EvolvedMekanismExtras factory installers so terminal VME ME factories can enter the matching extra factory chains.
- Kept cross-chain installer upgrades gated to the first extra tier, avoiding skips from lower EvolvedMekanism factory tiers.
- Verified the update with Gradle compile.

## 0.0.12-beta

- Added EvolvedMekanism and EvolvedMekanismExtras factory compatibility, including recipes, models, loot tables, lang entries, and EME Extra factory GUI support.
- Added the EvolvedMekanism Iglee Library dependency and updated runtime dependencies for the new compatibility chain.
- Fixed ME Factory Installer behavior so reusing it on existing ME machines does not downgrade them to ME Basic Factories.
- Matched Mekanism's installer interaction behavior by removing the client-side right-click use animation from ME factory installers.
- Matched EvolvedMekanism factory upgrade support for ME Evolved factories while keeping stack upgrades limited to factories whose source mod supports them.
- Added the machine adaptation guide docs.
- Verified the update with Gradle build.

## 0.0.11-beta

- Fixed transparent/missing adjacent block faces next to ME machines, including the ME Metallurgic Infuser case reported with AE2 Additions wireless transceivers.
- Restored Mekanism's original custom block shapes for ME machines and factories so adjacent blocks cull faces correctly.
- Applied the same shape fix across Mekanism, Mekanism Extras factories, and Mekanism: MoreMachine shape-sensitive machines.
- Verified the update with Gradle build.

## 0.0.10-beta

- Fixed ME machine dismantling so wrench removal keeps Mekanism machine data and drops installed AE patterns instead of deleting them.
- Fixed ME Factory Installer upgrades so installed upgrades, machine data, side configuration, energy, and AE patterns are preserved during conversion.
- Fixed Isotopic Centrifuge upgrade paths, including ME Isotopic Centrifuge to ME Basic Centrifuging Factory and Mekanism: MoreMachine centrifuging factories.
- Updated ME centrifuging factory models to use this mod's redesigned local factory indicator bars.
- Fixed machine light occlusion so ME machines, factories, and tall machines match Mekanism's original lighting behavior.
- Verified the update with Gradle build.

## 0.0.9-beta

- Fixed AE-network energy usage for Mekanism machines so recipe execution can consume AE power without displaying network power as stored FE.
- Added AE-aware recipe energy handling for Mekanism: MoreMachine base machines, regular factories, and advanced factories.
- Added AE-aware recipe energy handling for Mekanism Extras regular factories, MoreMachine-derived factories, and advanced factories.
- Kept original Mekanism/compat-mod recipe logic intact while wrapping only the cached recipe energy view.
- Verified the update with `compileJava`.

## 0.0.8-beta

- Added ME Planting Station and ME Replicator base machines for Mekanism: MoreMachine, so the ME Factory Installer maps the original base machines to their matching ME base versions instead of a basic factory.
- Improved ME Factory Installer target resolution and blocked it from remapping machines that are already ME machines.
- Added bounding-block handling for the ME Factory Installer when converting or interacting with tall machines.
- Fixed MekMM planting/replicating base machine AE support initialization during block entity construction.
- Refactored machine and factory registration so tile registration and AE grid-node capability registration share centralized descriptors.
- Matched the ME Planting Station item display transform with the ME planting factory item models.
