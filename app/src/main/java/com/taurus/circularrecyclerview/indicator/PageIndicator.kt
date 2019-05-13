package com.taurus.circularrecyclerview.indicator

import android.animation.ValueAnimator

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Parcelable
import android.util.AttributeSet
import android.view.View
import android.view.animation.AnimationUtils
import android.view.animation.DecelerateInterpolator
import android.view.animation.Interpolator
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.taurus.circularrecyclerview.R

class PageIndicator @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr), DotManager.TargetScrollListener {

    private lateinit var dotSizes: IntArray
    private lateinit var dotAnimators: Array<ValueAnimator>

    private val defaultPaint = Paint().apply { isAntiAlias = true }
    private val selectedPaint = Paint().apply { isAntiAlias = true }

    private val dotSize: Int
    private val dotSizeMap: Map<Byte, Int>
    private val dotBound: Int
    private val dotSpacing: Int
    private val animDuration: Long
    private val animInterpolator: Interpolator

    private var dotManager: DotManager? = null
    private var scrollAmount: Int = 0
    private var scrollAnimator: ValueAnimator? = null
    private var initialPadding: Int = 0

    private lateinit var scrollListener: RecyclerView.OnScrollListener

    var count: Int = 0
        set(value) {
            dotManager = DotManager(
                value,
                dotSize,
                dotSpacing,
                dotBound,
                dotSizeMap,
                this
            )

            dotSizes = IntArray(value)
            dotManager?.let { it.dots.forEachIndexed { index, dot -> dotSizes[index] = it.dotSizeFor(dot) } }
            dotAnimators = Array(value) { ValueAnimator() }

            initialPadding = when (value) {
                in 0..4 -> (dotBound + (4 - value) * (dotSize + dotSpacing) + dotSpacing) / 2
                else -> 2 * (dotSize + dotSpacing)
            }
            invalidate()
        }

    init {
        val ta = getContext().obtainStyledAttributes(attrs, R.styleable.PageIndicator)
        dotSizeMap = mapOf(
            BYTE_6 to ta.getDimensionPixelSize(R.styleable.PageIndicator_piSize1, 6.dp),
            BYTE_5 to ta.getDimensionPixelSize(R.styleable.PageIndicator_piSize2, 5f.dp),
            BYTE_4 to ta.getDimensionPixelSize(R.styleable.PageIndicator_piSize3, 4.5f.dp),
            BYTE_3 to ta.getDimensionPixelSize(R.styleable.PageIndicator_piSize4, 3f.dp),
            BYTE_2 to ta.getDimensionPixelSize(R.styleable.PageIndicator_piSize5, 2.5f.dp),
            BYTE_1 to ta.getDimensionPixelSize(R.styleable.PageIndicator_piSize6, .5f.dp)
        )
        dotSize = dotSizeMap.values.max() ?: 0
        dotSpacing = ta.getDimensionPixelSize(R.styleable.PageIndicator_piDotSpacing, 3.dp)
        dotBound = ta.getDimensionPixelSize(R.styleable.PageIndicator_piDotBound, 40.dp)

        animDuration = ta.getInteger(
            R.styleable.PageIndicator_piAnimDuration, DEFAULT_ANIM_DURATION
        ).toLong()
        defaultPaint.color = ta.getColor(
            R.styleable.PageIndicator_piDefaultColor,
            ContextCompat.getColor(getContext(), R.color.pi_default_color)
        )
        selectedPaint.color = ta.getColor(
            R.styleable.PageIndicator_piSelectedColor,
            ContextCompat.getColor(getContext(), R.color.pi_selected_color)
        )
        animInterpolator = AnimationUtils.loadInterpolator(
            context, ta.getResourceId(
                R.styleable.PageIndicator_piAnimInterpolator,
                R.anim.pi_default_interpolator
            )
        )
        ta.recycle()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        // FIXME: add support for `match_parent`
        setMeasuredDimension(4 * (dotSize + dotSpacing) + dotBound, dotSize)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        var paddingStart = initialPadding
        val (start, end) = getDrawingRange()

        paddingStart += (dotSize + dotSpacing) * start
        (start until end).forEach {
            canvas?.drawCircle(
                paddingStart + dotSize / 2f - scrollAmount,
                dotSize / 2f,
                dotSizes[it] / 2f,
                when (dotManager?.dots?.get(it)) {
                    BYTE_6 -> selectedPaint
                    else -> defaultPaint
                }
            )
            paddingStart += dotSize + dotSpacing
        }
    }

    override fun onSaveInstanceState(): Parcelable? {
        val superState = super.onSaveInstanceState()
        if (superState == null) {
            return superState
        }

        val savedState = SavedState(superState)
        savedState.count = this.count
        savedState.selectedIndex = this.dotManager?.selectedIndex ?: 0
        return savedState
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state !is SavedState) {
            super.onRestoreInstanceState(state)
            return
        }

        super.onRestoreInstanceState(state.superState)

        this.count = state.count
        for (i in 0 until state.selectedIndex) {
            swipeNext()
        }
    }

    override fun scrollToTarget(target: Int) {
        scrollAnimator?.cancel()
        scrollAnimator = ValueAnimator.ofInt(scrollAmount, target).apply {
            duration = animDuration
            interpolator = DEFAULT_INTERPOLATOR
            addUpdateListener { animation ->
                scrollAmount = animation.animatedValue as Int
                invalidate()
            }
            start()
        }
    }

    infix fun attachTo(recyclerView: RecyclerView) {
        if (::scrollListener.isInitialized) {
            recyclerView.removeOnScrollListener(scrollListener)
        }
        count = (recyclerView.adapter?.itemCount?.minus(1))?.div(2) ?: 0
        scrollListener = ScrollListener(this)
        recyclerView.addOnScrollListener(scrollListener)
        scrollToTarget(0)
    }

    fun swipePrevious() {
        dotManager?.goToPrevious()
        animateDots()
    }

    fun swipeNext() {
        dotManager?.goToNext()
        animateDots()
    }

    fun swipeFirstItem() {
        dotManager?.goToFirstItem()
        animateDots()
    }

    fun swipeLastItem() {
        // TODO
        animateDots()
    }

    private fun animateDots() {
        dotManager?.let {
            val (start, end) = getDrawingRange()
            (start until end).forEach { index ->
                dotAnimators[index].cancel()
                dotAnimators[index] = ValueAnimator.ofInt(dotSizes[index], it.dotSizeFor(it.dots[index]))
                    .apply {
                        duration = animDuration
                        interpolator = DEFAULT_INTERPOLATOR
                        addUpdateListener { animation ->
                            dotSizes[index] = animation.animatedValue as Int
                            invalidate()
                        }
                    }
                dotAnimators[index].start()
            }
        }
    }

    private fun getDrawingRange(): Pair<Int, Int> {
        val start = Math.max(0, (dotManager?.selectedIndex ?: 0) - MOST_VISIBLE_COUNT)
        val end = Math.min(
            dotManager?.dots?.size ?: 0,
            (dotManager?.selectedIndex ?: 0) + MOST_VISIBLE_COUNT
        )
        return Pair(start, end)
    }

    companion object {
        private const val BYTE_6 = 6.toByte()
        private const val BYTE_5 = 5.toByte()
        private const val BYTE_4 = 4.toByte()
        private const val BYTE_3 = 3.toByte()
        private const val BYTE_2 = 2.toByte()
        private const val BYTE_1 = 1.toByte()

        private const val MOST_VISIBLE_COUNT = 10
        private const val DEFAULT_ANIM_DURATION = 200

        private val DEFAULT_INTERPOLATOR = DecelerateInterpolator()
    }
}
