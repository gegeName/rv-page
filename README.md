# rv-page

`rv-page` 是页面级 RecyclerView 装配 DSL，用一个入口组合标题、单条数据、普通列表、横向/纵向嵌套列表、分页列表和自定义 Adapter。

## 引入

本仓库内 app 直接依赖本地 module：

```kotlin
dependencies {
    implementation(project(":rv-page"))
}
```

发布到 JitPack 后可按实际 tag 引入：

```kotlin
dependencies {
    implementation("com.github.<USER>.<REPO>:rv-page:<TAG>")
}
```

## 完整 Section 示例

```kotlin
private lateinit var titleSection: SingleSectionHandle<String>
private lateinit var feedSection: ListSectionHandle<FeedItem>
private lateinit var blockSection: MultiListSectionHandle<HomeBlock>
private lateinit var cardSection: CarouselSectionHandle<Card>
private lateinit var pagingSection: PagingSectionHandle<FeedItem>

val page = RvPage.with(this)
    .recyclerView(binding.recyclerView)
    .layoutManager(LinearLayoutManager(this))
    .sections {
        titleSection = single<String, ItemTitleBinding>(
            ItemTitleBinding::inflate,
            tag = "title"
        ) {
            data = "RvPage"
            onBind { b, title -> b.titleText.text = title }
        }

        feedSection = list<FeedItem, ItemFeedBinding>(
            ItemFeedBinding::inflate,
            tag = "feed"
        ) {
            keyOf { it.id }
            data = feedItems
            onBind { b, item, _ ->
                b.titleText.text = item.title
                b.summaryText.text = item.summary
            }
            onItemClick(throttleMs = 500, keyOf = { it.id }) { _, item, _ ->
                // handle click
            }
        }

        blockSection = multiList<HomeBlock>(tag = "blocks") {
            keyOf { it.id }
            data = blocks
            addType<ItemBannerBinding>(
                isMine = { it is HomeBlock.Banner },
                inflate = ItemBannerBinding::inflate
            ) { b, item, _ ->
                val banner = item as HomeBlock.Banner
                b.titleText.text = banner.title
            }
            addType<ItemTextBinding>(
                isMine = { it is HomeBlock.Text },
                inflate = ItemTextBinding::inflate
            ) { b, item, _ ->
                val text = item as HomeBlock.Text
                b.bodyText.text = text.body
            }
        }

        cardSection = carousel<Card, ItemCardBinding>(
            ItemCardBinding::inflate,
            tag = "cards"
        ) {
            keyOf { it.id }
            data = cards
            heightDp = 132
            itemSpacingDp = 12
            edgeLeftDp = 16
            edgeRightDp = 16
            snap = CarouselSnap.LINEAR
            onBind { b, item, _ -> b.titleText.text = item.title }
        }

        custom(
            tag = "legacy",
            adapter = legacyAdapter,
            spanFull = true
        )

        pagingSection = pagingList<FeedItem, ItemFeedBinding>(
            ItemFeedBinding::inflate,
            tag = "paging_feed"
        ) {
            flow = viewModel.feedFlow
            keyOf { it.id }
            onBind { b, item, _ ->
                b.titleText.text = item.title
                b.summaryText.text = item.summary
            }
            onLoadError { error -> showToast(error.message.orEmpty()) }
        }
    }
    .start()

titleSection.submit("Updated title")
feedSection.submit(newFeedItems)
blockSection.submit(newBlocks)
cardSection.submit(newCards)
page.hideSection("legacy")
page.showSection("legacy")
page.refresh()
```

分页多布局使用 `pagingMultiList`，适合分页流内有多种 item：

```kotlin
pagingMultiList<HomeBlock>(tag = "paging_blocks") {
    flow = viewModel.blockFlow
    keyOf { it.id }
    addType<ItemBannerBinding>(
        isMine = { it is HomeBlock.Banner },
        inflate = ItemBannerBinding::inflate
    ) { b, item, _ ->
        b.titleText.text = (item as HomeBlock.Banner).title
    }
    addType<ItemTextBinding>(
        isMine = { it is HomeBlock.Text },
        inflate = ItemTextBinding::inflate
    ) { b, item, _ ->
        b.bodyText.text = (item as HomeBlock.Text).body
    }
}
```

## 使用规则

- 每个 section 建议显式传 `tag`，方便后续查找、显隐或调试。
- `list`、`multiList`、`carousel` 必须提供 `diff` 或 `keyOf { ... }`，推荐优先使用稳定业务 id。
- `single` 的 `data = null` 会隐藏该 section；`list`/`carousel` 提交空列表时不会展示内容。
- 同一个页面最多声明一个分页 section：`pagingList` 和 `pagingMultiList` 合计最多一个。
- ViewBinding 使用函数引用传入，例如 `ItemBinding::inflate`，库内部不通过反射查找 binding。
- `spanFull` 默认 `true`，在 `GridLayoutManager` 下会占满整行；需要宫格混排时再改为 `false` 或使用 `spanSize { ... }`。

## Carousel

默认 carousel 内部是横向 `LinearLayoutManager`：

```kotlin
carousel<Card, ItemCardBinding>(ItemCardBinding::inflate, tag = "cards") {
    keyOf { it.id }
    data = cards
    heightDp = 132
    itemSpacingDp = 12
    edgeLeftDp = 16
    edgeRightDp = 16
    snap = CarouselSnap.LINEAR
    onBind { b, item, _ -> b.titleText.text = item.title }
}
```

如果需要自定义内部 LayoutManager：

```kotlin
carousel<Item, ItemBinding>(ItemBinding::inflate, tag = "nested_list") {
    keyOf { it.id }
    heightDp = 300
    layoutManager { context ->
        LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
    }
    onBind { b, item, _ -> /* bind */ }
}
```

同方向嵌套滚动时必须给 carousel 设置固定高度，例如 `heightDp = 300`。如果内部 RecyclerView 是 `wrap_content`，内容会撑开自身，不会产生内部滚动区域。

需要在业务 XML 中复用同方向嵌套滚动处理时，可以直接使用：

```xml
<com.chat.rv_page.NestedCarouselRecyclerView
    android:layout_width="match_parent"
    android:layout_height="300dp" />
```

## 混淆规则

库已在 `rv-page/consumer-rules.pro` 内提供最小 consumer rules。当前只保留公开自定义 View `NestedCarouselRecyclerView` 的类名和 XML 构造函数，避免 XML inflation 或字符串类名引用失效：

```proguard
-keep,allowshrinking,allowoptimization class com.chat.rv_page.NestedCarouselRecyclerView {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}
```

不要整包 keep `com.chat.rv_page.**`。本库 DSL 不依赖反射创建 binding 或 adapter，过宽的 keep 规则会降低混淆和裁剪效果。只有当业务方自己通过反射、字符串类名或 XML 引用额外公开类时，才为对应类补充精确规则。

## 分页提示

分页 section 委托 `pagingHelper` 处理：

```kotlin
pagingList<FeedItem, ItemFeedBinding>(ItemFeedBinding::inflate, tag = "feed") {
    flow = viewModel.feedFlow
    keyOf { it.id }
    onBind { b, item, _ -> b.titleText.text = item.title }
}
```

分页页内如需静态 header/footer，直接在 `pagingList` 前后声明普通 section 即可。

## 许可证

本项目基于 [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0) 开源。Apache 2.0 自带"按现状提供、不作担保、不承担责任"的条款，并额外包含专利授权与责任限制条款。

## 免责声明 / Disclaimer

本项目（以下简称"本软件"）是一个通用的媒体选择工具，仅供学习、研究和合法用途使用。

1. 本软件按"现状"提供，作者不对其适用性、可靠性、安全性作任何明示或暗示的担保。
2. 使用者应自行遵守所在国家/地区的法律法规。对于使用者利用本软件从事的任何违法、侵权或其他不当行为，作者不承担由此产生的任何责任。
3. 本软件不针对任何违法用途设计，作者不认可、不支持将其用于任何违反法律法规的用途。
4. 在适用法律允许的最大范围内，作者不对因使用或无法使用本软件而导致的任何直接或间接损失承担责任。
5. 使用本软件即表示使用者已知悉并接受以上条款。