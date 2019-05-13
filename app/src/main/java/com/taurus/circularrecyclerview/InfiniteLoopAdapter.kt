package com.taurus.circularrecyclerview

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class InfiniteLoopAdapter(private val itemList: List<String>) : RecyclerView.Adapter<ImageViewHolder>() {

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        val linearLayoutManager = recyclerView.layoutManager as LinearLayoutManager
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val firstItemVisible = linearLayoutManager.findFirstVisibleItemPosition()
                if (firstItemVisible != 1 && firstItemVisible % itemList.size == 1) {
                    linearLayoutManager.scrollToPosition(1)
                }
                val firstCompletelyItemVisible = linearLayoutManager.findFirstCompletelyVisibleItemPosition()
                if (firstCompletelyItemVisible == 0) {
                    linearLayoutManager.scrollToPositionWithOffset(itemList.size, 0)
                }
            }
        })
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.pager_view, parent, false)
        return ImageViewHolder(view)
    }

    override fun getItemCount() = itemList.size * 2 + 1

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val realPosition = position % itemList.size
        holder.bind(itemList[realPosition])
    }

}

