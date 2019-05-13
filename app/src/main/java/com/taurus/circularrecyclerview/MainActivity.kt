package com.taurus.circularrecyclerview

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val list = listOf("Item 1", "Item 2", "Item 3", "Item 4",  "Item 5",  "Item 6")
        recyclerView.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = InfiniteLoopAdapter(list)
            PagerSnapHelper().attachToRecyclerView(this)
            pageIndicator.attachTo(this)
        }

    }
}
