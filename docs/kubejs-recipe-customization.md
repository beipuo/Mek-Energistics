# Mek-Energistics KubeJS 配方定制

Mek-Energistics 的机器合成配方大多是普通 datapack `minecraft:crafting_shapeless` 配方：

```json
{
  "type": "minecraft:crafting_shapeless",
  "ingredients": [
    { "item": "mekanism:enrichment_chamber" },
    { "item": "mekenergistics:me_factory_installer" }
  ],
  "result": {
    "count": 1,
    "id": "mekenergistics:me_enrichment_chamber"
  }
}
```

因此不需要给 Mek-Energistics 增加 KubeJS 硬前置，也不需要 Java 侧 KubeJS 插件就能让整合包作者批量改配方。推荐方式是提供一个 KubeJS `server_scripts` helper：先按 recipe id 移除默认配方，再用数组批量重建。

KubeJS 文档依据：

- 脚本放在 `kubejs/server_scripts`，可随 `/reload` 重载：https://kubejs.com/wiki/folder-structure/server-scripts
- 配方修改写在 `ServerEvents.recipes(event => { ... })`：https://kubejs.com/wiki/tutorials/recipes
- `event.shapeless(output, inputs)` 添加无序合成。
- `event.remove({ id: 'namespace:path' })` 按 recipe id 删除原配方。
- 大量相似配方推荐用 helper function 和数组循环。
- `//requires: modid` 可让整份脚本只在指定模组存在时加载：https://kubejs.com/wiki/tips/headers

整合包如果用 CurseMaven 管理依赖，可以添加：

```gradle
implementation "curse.maven:kubejs-238086:8083208"
```

## 推荐脚本

把下面内容放入整合包的 `kubejs/server_scripts/mekenergistics_recipes.js`。
模板里保留了 `//requires: mekenergistics`，但对 `evolvedmekanism`、`emextras` 等可选模组使用分组级 `requires` 判断；这样同一份脚本可以同时覆盖基础 Mek、MekMM、MekE、EMEKE 等多组配方。

```js
//requires: mekenergistics

const DEFAULT_INSTALLER = 'mekenergistics:me_factory_installer'

const isLoaded = modid => Platform.mods[modid] !== undefined

function replaceMeMachineRecipe(event, recipePath, source, result, options) {
  const opts = options || {}
  const requires = opts.requires || []

  if (!requires.every(isLoaded)) {
    return
  }

  const id = `mekenergistics:${recipePath}`
  const installer = opts.installer || DEFAULT_INSTALLER
  const inputs = opts.inputs || [source, installer]

  event.remove({ id: id })
  event.shapeless(result, inputs).id(id)
}

function replaceMeMachines(event, entries, options) {
  entries.forEach(entry => {
    replaceMeMachineRecipe(event, entry[0], entry[1], entry[2], options)
  })
}

ServerEvents.recipes(event => {
  replaceMeMachines(event, [
    ['me_enrichment_chamber', 'mekanism:enrichment_chamber', 'mekenergistics:me_enrichment_chamber'],
    ['me_crusher', 'mekanism:crusher', 'mekenergistics:me_crusher'],
    ['me_energized_smelter', 'mekanism:energized_smelter', 'mekenergistics:me_energized_smelter'],
    ['me_metallurgic_infuser', 'mekanism:metallurgic_infuser', 'mekenergistics:me_metallurgic_infuser'],
    ['me_osmium_compressor', 'mekanism:osmium_compressor', 'mekenergistics:me_osmium_compressor'],
    ['me_combiner', 'mekanism:combiner', 'mekenergistics:me_combiner'],
    ['me_precision_sawmill', 'mekanism:precision_sawmill', 'mekenergistics:me_precision_sawmill']
  ])

  replaceMeMachines(event, [
    ['me_basic_enriching_factory', 'mekanism:basic_enriching_factory', 'mekenergistics:me_basic_enriching_factory'],
    ['me_advanced_enriching_factory', 'mekanism:advanced_enriching_factory', 'mekenergistics:me_advanced_enriching_factory'],
    ['me_elite_enriching_factory', 'mekanism:elite_enriching_factory', 'mekenergistics:me_elite_enriching_factory'],
    ['me_ultimate_enriching_factory', 'mekanism:ultimate_enriching_factory', 'mekenergistics:me_ultimate_enriching_factory']
  ])

  replaceMeMachines(event, [
    ['me_alloyer', 'evolvedmekanism:alloyer', 'mekenergistics:me_alloyer'],
    ['me_basic_alloying_factory', 'evolvedmekanism:basic_alloying_factory', 'mekenergistics:me_basic_alloying_factory'],
    ['me_advanced_alloying_factory', 'evolvedmekanism:advanced_alloying_factory', 'mekenergistics:me_advanced_alloying_factory'],
    ['me_elite_alloying_factory', 'evolvedmekanism:elite_alloying_factory', 'mekenergistics:me_elite_alloying_factory'],
    ['me_ultimate_alloying_factory', 'evolvedmekanism:ultimate_alloying_factory', 'mekenergistics:me_ultimate_alloying_factory']
  ], { requires: ['evolvedmekanism'] })

  replaceMeMachines(event, [
    ['me_absolute_alloying_factory', 'emextras:absolute_alloying_factory', 'mekenergistics:me_absolute_alloying_factory'],
    ['me_supreme_alloying_factory', 'emextras:supreme_alloying_factory', 'mekenergistics:me_supreme_alloying_factory'],
    ['me_cosmic_alloying_factory', 'emextras:cosmic_alloying_factory', 'mekenergistics:me_cosmic_alloying_factory'],
    ['me_infinite_alloying_factory', 'emextras:infinite_alloying_factory', 'mekenergistics:me_infinite_alloying_factory']
  ], { requires: ['evolvedmekanism', 'emextras'] })
})
```

## 改配方成本

整合包作者常见需求只需要改数组或 `installer`：

```js
ServerEvents.recipes(event => {
  replaceMeMachineRecipe(
    event,
    'me_enrichment_chamber',
    'mekanism:enrichment_chamber',
    'mekenergistics:me_enrichment_chamber',
    { inputs: ['mekanism:enrichment_chamber', 'mekenergistics:me_factory_installer', 'minecraft:nether_star'] }
  )
})
```

如果整合包想统一把 ME 安装器替换成更贵的物品：

```js
ServerEvents.recipes(event => {
  replaceMeMachines(event, [
    ['me_enrichment_chamber', 'mekanism:enrichment_chamber', 'mekenergistics:me_enrichment_chamber'],
    ['me_crusher', 'mekanism:crusher', 'mekenergistics:me_crusher']
  ], { installer: 'minecraft:netherite_upgrade_smithing_template' })
})
```

## 什么时候才需要 Java 侧 KubeJS 插件

当前不建议加。理由：

- Mek-Energistics 的配方已经是 datapack 原生 crafting recipe，KubeJS 默认支持。
- 这些配方的重复点在 recipe id、输入机器、输出 ME 机器三元组，用 JS helper 就能消除。
- Java 侧 KubeJS 插件会引入额外 API 兼容负担，还要保证没有 KubeJS 时不触发类加载。

只有当我们以后要让脚本作者调用类似 `MekEnergisticsEvents.registerMachine(...)`，动态生成方块、物品、BlockEntity、菜单、语言、模型、JEI 和安装器逻辑时，才值得做 Java 插件。单纯改合成配方时，KubeJS server script 是更稳的接口。

## 维护规则

新增 ME 机器时，给整合包作者暴露的最小信息是：

- 默认 recipe id：`mekenergistics:<recipe_path>`。
- 原机器 item id。
- ME 机器 item id。
- 是否需要可选模组，例如 `mekmm`、`mekanism_extras`、`evolvedmekanism`、`emextras`。

如果配方文件带 `neoforge:conditions`，KubeJS 模板里也要用 `requires` 做同等保护，避免未安装可选模组时脚本引用不存在的物品。
