package com.taurus.circularrecyclerview.indicator

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

internal class ScrollListener(private val indicator: PageIndicator) : RecyclerView.OnScrollListener() {
    private var midPos = 0
    private var scrollX = 0

    override fun onScrolled(
        recyclerView: RecyclerView,
        dx: Int,
        dy: Int
    ) {
        super.onScrolled(recyclerView, dx, dy)
        val linearLayoutManager = recyclerView.layoutManager as LinearLayoutManager
        val realCount = (recyclerView.adapter?.itemCount?.minus(1))?.div(2) ?: 0
        scrollX += dx
        recyclerView.getChildAt(0)?.width?.let {
            val midPos = Math.floor(((scrollX + it / 2f) / it).toDouble()).toInt()
            if (this.midPos != midPos) {
                when {
                    this.midPos < midPos -> indicator.swipeNext()
                    else -> indicator.swipePrevious()
                }
            }
            this.midPos = midPos

            val firstItemVisible = linearLayoutManager.findFirstVisibleItemPosition()
            if (firstItemVisible != 1 && firstItemVisible % realCount == 1) {
                // From last item to first item
                indicator.swipeFirstItem()
            }
            val firstCompletelyItemVisible = linearLayoutManager.findFirstCompletelyVisibleItemPosition()
            if (firstCompletelyItemVisible == 0) {
                // From first item to last item
                indicator.swipeLastItem()
            }
        }
    }
}

