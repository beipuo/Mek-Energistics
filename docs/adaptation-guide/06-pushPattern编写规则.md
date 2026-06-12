# 6. `pushPattern` 编写规则

`pushPattern` 是 AE 下单把原料推入机器的核心。新增机器时最容易出资源复制或吞资源的问题，必须按下面顺序写：

1. 检查 AE 节点 active。
2. 检查 `patternDetails` 属于 `getAvailablePatterns()`。
3. 检查 `inputHolder` 数量和机器输入数一致。
4. 用 `MeFactoryPatternInput.single(counter)` 解析输入。
5. 严格区分 item、chemical、fluid。
6. 所有输入都 `Action.SIMULATE` 成功后再 `Action.EXECUTE`。
7. 任何一个输入不能完整插入时直接返回 `false`。
8. execute 后 `setChanged()`。

已有工具：

- `MeFactoryPatternInput.single(...)`: 把 AE `KeyCounter` 转成单一 item/chemical/fluid 输入。一个 counter 混入多个 key 会返回 `null`。
- `MeFactoryInventoryInsert.canInsertAcrossSlots(...)`: 跨 factory 输入槽模拟插入。
- `MeFactoryInventoryInsert.insertAcrossSlots(...)`: 先 simulate，再 execute。
- `MeChemicalInputHelper.insertPair(...)`: 双 chemical tank 按左右顺序插入，并在不合法时回滚。

双输入机器要注意输入顺序。Chemical Infuser 这类左右 tank 可互换的机器，应尝试 `(first, second)` 和 `(second, first)`。Combiner 这类主槽/副槽语义不同的机器不能随便交换。
