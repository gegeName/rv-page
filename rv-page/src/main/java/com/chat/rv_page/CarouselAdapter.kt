package com.chat.rv_page

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewParent
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.chat.pagingutil.SpacingItemDecoration

/**
 * carousel section 的外层承载 Adapter:对外 0/1 个 item,该 item 是一个横向 RecyclerView,
 * 内部 adapter 由 [inner] 提供。dp 参数延迟到 [onCreateViewHolder] 换算为 px。
 */
internal class CarouselAdapter<T : Any>(
    private val inner: BaseListAdapter<T, *>,
    private val heightDp: Int,
    private val paddingStartDp: Int,
    private val paddingTopDp: Int,
    private val paddingEndDp: Int,
    private val paddingBottomDp: Int,
    private val itemSpacingDp: Int,
    private val edgeLeftDp: Int,
    private val edgeTopDp: Int,
    private val edgeRightDp: Int,
    private val edgeBottomDp: Int,
    private val layoutManagerFactory: ((Context) -> RecyclerView.LayoutManager)?,
    private val snap: CarouselSnap,
    private val sharedPool: RecyclerView.RecycledViewPool?
) : RecyclerView.Adapter<CarouselAdapter.RowHolder>() {

    class RowHolder(val rv: RecyclerView) : RecyclerView.ViewHolder(rv)

    private var hasData: Boolean = inner.currentList.isNotEmpty()

    init {
        inner.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() = sync()
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) = sync()
            override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) = sync()
        })
    }

    private fun sync() {
        val nowHas = inner.currentList.isNotEmpty()
        if (hasData == nowHas) return
        hasData = nowHas
        if (nowHas) notifyItemInserted(0) else notifyItemRemoved(0)
    }

    fun submit(list: List<T>?) {
        inner.submit(list)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RowHolder {
        val ctx = parent.context
        val density = ctx.resources.displayMetrics.density
        fun dp(v: Int): Int = if (v <= 0) 0 else (v * density + 0.5f).toInt()

        val rv = NestedCarouselRecyclerView(ctx).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                if (heightDp > 0) dp(heightDp) else ViewGroup.LayoutParams.WRAP_CONTENT
            )
            layoutManager = layoutManagerFactory?.invoke(ctx)
                ?: LinearLayoutManager(ctx, LinearLayoutManager.HORIZONTAL, false)
            isNestedScrollingEnabled = false
            setPadding(dp(paddingStartDp), dp(paddingTopDp), dp(paddingEndDp), dp(paddingBottomDp))
            clipToPadding = false
            sharedPool?.let { setRecycledViewPool(it) }
            val spacingPx = dp(itemSpacingDp)
            val edgeLeftPx = dp(edgeLeftDp)
            val edgeTopPx = dp(edgeTopDp)
            val edgeRightPx = dp(edgeRightDp)
            val edgeBottomPx = dp(edgeBottomDp)
            if (spacingPx > 0 || edgeLeftPx > 0 || edgeTopPx > 0 || edgeRightPx > 0 || edgeBottomPx > 0) {
                SpacingItemDecoration.builder()
                    .itemSpacing(spacingPx)
                    .edgeLeft(edgeLeftPx)
                    .edgeTop(edgeTopPx)
                    .edgeRight(edgeRightPx)
                    .edgeBottom(edgeBottomPx)
                    .attachRecyclerView(this)
            }
            when (snap) {
                CarouselSnap.NONE -> Unit
                CarouselSnap.LINEAR -> LinearSnapHelper().attachToRecyclerView(this)
                CarouselSnap.PAGER -> PagerSnapHelper().attachToRecyclerView(this)
            }
            adapter = inner
        }
        return RowHolder(rv)
    }

    override fun onBindViewHolder(holder: RowHolder, position: Int) {
        if (holder.rv.adapter !== inner) holder.rv.adapter = inner
    }

    override fun getItemCount(): Int = if (hasData) 1 else 0
}

class NestedCarouselRecyclerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RecyclerView(context, attrs, defStyleAttr) {

    init {
        isNestedScrollingEnabled = false
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                parent?.requestDisallowInterceptTouchEventRecursive(true)
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEventRecursive(false)
            }
        }
        return super.dispatchTouchEvent(event)
    }

    private fun ViewParent.requestDisallowInterceptTouchEventRecursive(disallowIntercept: Boolean) {
        requestDisallowInterceptTouchEvent(disallowIntercept)
        parent?.requestDisallowInterceptTouchEventRecursive(disallowIntercept)
    }
}

/** carousel 横向 snap 模式。 */
enum class CarouselSnap {
    /** 不吸附,自由滑动。 */
    NONE,

    /** 最近 item 居中吸附。 */
    LINEAR,

    /** 整页翻。 */
    PAGER
}
