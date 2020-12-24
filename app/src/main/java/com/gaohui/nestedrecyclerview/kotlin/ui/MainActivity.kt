package com.gaohui.nestedrecyclerview.kotlin.ui

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.gaohui.nestedrecyclerview.BaseMenuActivity
import com.gaohui.nestedrecyclerview.R
import com.gaohui.nestedrecyclerview.kotlin.adapter.MultiTypeAdapter
import com.gaohui.nestedrecyclerview.kotlin.bean.CategoryBean
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class MainActivity : BaseMenuActivity() {

    private val mDataList = ArrayList<Any>()

    private val strArray = arrayOf("推荐", "视频", "直播", "图片", "精华", "热门")

    var lastBackPressedTime = 0L

    private val multiTypeAdapter =
        MultiTypeAdapter(mDataList)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        kttRecyclerView.initLayoutManager()
        kttRecyclerView.adapter = multiTypeAdapter

        refresh()
    }

    private fun refresh() {
        mDataList.clear()
        for (i in 0..1) {
            mDataList.add("parent item text $i")
        }
        val categoryBean = CategoryBean()
        categoryBean.tabTitleList.clear()
        categoryBean.tabTitleList.addAll(strArray.asList())
        mDataList.add(categoryBean)
        multiTypeAdapter.notifyDataSetChanged()
    }

    override fun onBackPressed() {
        if (System.currentTimeMillis() - lastBackPressedTime < 2000) {
            super.onBackPressed()
        } else {
            kttRecyclerView.scrollToPosition(0)
            Toast.makeText(this, "再按一次退出程序", Toast.LENGTH_SHORT).show()
            lastBackPressedTime = System.currentTimeMillis()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        multiTypeAdapter.destroy()
    }
}
