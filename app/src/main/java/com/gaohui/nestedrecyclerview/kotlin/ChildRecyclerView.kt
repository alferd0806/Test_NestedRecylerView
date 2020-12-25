package com.gaohui.nestedrecyclerview.kotlin

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.gaohui.nestedrecyclerview.UIUtils
import com.gaohui.nestedrecyclerview.kotlin.helper.FlingHelper
import kotlin.math.abs

open class ChildRecyclerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RecyclerView(context, attrs, defStyleAttr) {

    private val mFlingHelper = FlingHelper(context)
    private var mMaxDistance = 0

    private var mVelocityY = 0

    var isStartFling: Boolean = false
    var totalDy: Int = 0

    var mParentRecyclerView: ParentRecyclerView? = null

    init {
        mMaxDistance =
            mFlingHelper.getVelocityByDistance((UIUtils.getScreenHeight() * 4).toDouble())
        overScrollMode = OVER_SCROLL_NEVER
        initScrollListener()
    }

    private fun initScrollListener() {
        addOnScrollListener(object : OnScrollListener() {
            override fun onScrolled(
                recyclerView: RecyclerView,
                dx: Int,
                dy: Int
            ) {
                if (isStartFling) {
                    totalDy = 0
                    isStartFling = false
                }
                totalDy += dy
            }

            override fun onScrollStateChanged(
                recyclerView: RecyclerView,
                newState: Int
            ) {
                if (newState == SCROLL_STATE_IDLE) {
                    dispatchParentFling()
                }
            }
        })
    }

    private fun dispatchParentFling() {
        val mParentRecyclerView = findParentRecyclerView()
        mParentRecyclerView?.run {
            if (isScrollTop() && mVelocityY != 0) {
                //当前ChildRecyclerView已经滑动到顶部，且竖直方向加速度不为0,如果有多余的需要交由父RecyclerView继续fling
                val flingDistance = mFlingHelper.getSplineFlingDistance(mVelocityY)
                if (flingDistance > (abs(this@ChildRecyclerView.totalDy))) {
                    fling(
                        0,
                        -mFlingHelper.getVelocityByDistance(flingDistance + this@ChildRecyclerView.totalDy)
                    )
                }
                //fix 在run方法里面，注意 this@ChildRecyclerView的使用，否则使用的是ParentRecyclerView的变量
                this@ChildRecyclerView.totalDy = 0
                mVelocityY = 0
            }
        }
    }

    override fun fling(velocityX: Int, velocityY: Int): Boolean {
        if (isAttachedToWindow.not()) return false
        val fling = super.fling(velocityX, velocityY)
        if (!fling || velocityY >= 0) {
            //fling为false表示加速度达不到fling的要求，将mVelocityY重置
            mVelocityY = 0
        } else {
            //正在进行fling
            isStartFling = true
            mVelocityY = velocityY
        }
        ignore = false
        return fling
    }

    fun isScrollTop(): Boolean {
        //RecyclerView.canScrollVertically(-1)的值表示是否能向下滚动，false表示已经滚动到顶部
        return !canScrollVertically(-1)
    }

    private fun findParentRecyclerView(): ParentRecyclerView? {
        if (null != mParentRecyclerView) {
            return mParentRecyclerView
        }
        var parentView = parent
        while ((parentView is ParentRecyclerView).not()) {
            parentView = parentView.parent
        }
        return parentView as? ParentRecyclerView
    }

    override fun startNestedScroll(axes: Int, type: Int): Boolean {
        if (type == ViewCompat.TYPE_NON_TOUCH) {
            return false
        }
        return super.startNestedScroll(axes, type)
    }

    private var handleChildTouchEvent: Boolean? = null
    private var lastY = 0f
    private var ignore = false

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
//        Log.i("zxm", "test=${findParentRecyclerView()?.testDy}")
        when (ev?.action) {
            MotionEvent.ACTION_DOWN -> {
                mVelocityY = 0
                lastY = ev.y
            }
            MotionEvent.ACTION_MOVE -> {
                if (ev.y - lastY > 0 && findParentRecyclerView()?.isCollapse == true && findParentRecyclerView()?.testDy == 0) {
                    ignore = true
                    super.dispatchTouchEvent(ev)
                    return true
                } else {
                    ignore = false
                }
            }
            MotionEvent.ACTION_UP -> {
                ignore = false
                lastY = 0f
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onInterceptTouchEvent(e: MotionEvent?): Boolean {
        handleChildTouchEvent = findParentRecyclerView()?.isCollapse == false || ignore
        if (handleChildTouchEvent == true) {
            return false
        }
        return super.onInterceptTouchEvent(e)
    }

    override fun onTouchEvent(e: MotionEvent?): Boolean {
        if (handleChildTouchEvent == true) {
            return false
        }
        return super.onTouchEvent(e)
    }
}