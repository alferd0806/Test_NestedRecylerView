package com.gaohui.nestedrecyclerview.kotlin

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewParent
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.gaohui.nestedrecyclerview.UIUtils
import com.gaohui.nestedrecyclerview.kotlin.adapter.MultiTypeAdapter
import com.gaohui.nestedrecyclerview.kotlin.helper.FlingHelper
import com.google.android.material.appbar.AppBarLayout
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs

class ParentRecyclerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RecyclerView(context, attrs, defStyleAttr) {

    private var mMaxDistance: Int = 0

    private val mFlingHelper = FlingHelper(context)

    /**
     * 记录上次Event事件的y坐标
     */
    private var lastY: Float = 0f
    private var totalDy = 0
    var testDy = 0

    /**
     * 用于判断RecyclerView是否在fling
     */
    private var isStartFling = false

    /**
     * 记录当前滑动的y轴加速度
     */
    private var velocityY: Int = 0
    private var canScrollVertically: AtomicBoolean

    init {
        mMaxDistance =
            mFlingHelper.getVelocityByDistance((UIUtils.getScreenHeight() * 4).toDouble())
        canScrollVertically = AtomicBoolean(true)
        addOnScrollListener(object : OnScrollListener() {
            override fun onScrollStateChanged(
                recyclerView: RecyclerView,
                newState: Int
            ) {
                //如果父RecyclerView fling过程中已经到底部，需要让子RecyclerView滑动剩余的fling
                if (newState == SCROLL_STATE_IDLE) {
                    dispatchChildFling()
                }
            }

            override fun onScrolled(
                recyclerView: RecyclerView,
                dx: Int,
                dy: Int
            ) {
                //检查当前的滑动方向上是否还可以滑动
                canScroll = when {
                    dy > 0 -> {   //向上滑
                        canScrollVertically(1)
                    }
                    dy < 0 -> {  //向下滑
                        canScrollVertically(-1)
                    }
                    else -> {
                        false
                    }
                }
                testDy += dy
                if (isStartFling) {
                    totalDy = 0
                    isStartFling = false
                }
                //在RecyclerView fling情况下，记录当前RecyclerView在y轴的偏移
                totalDy += dy
            }
        })
    }

    private var canScroll = false
    var isCollapse: Boolean? = null

    private fun findAppBarLayout() {
        if (isNestedScrollingEnabled) {
            var p: ViewParent? = parent
            while (p != null) {
                if (p is CoordinatorLayout) {
                    val childCount = p.childCount
                    if (childCount > 0) {
                        for (i in 0 until childCount) {
                            val child = p.getChildAt(i)
                            if (child is AppBarLayout) {
                                child.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { _, verticalRange ->
                                    isCollapse =
                                        abs(verticalRange) >= child.totalScrollRange
                                })
                                break
                            }
                        }
                    }
                    break
                }
                p = p.parent
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        //初始化AppBarLayout监听
        findAppBarLayout()
    }

    private fun dispatchChildFling() {
        if (isScrollEnd() && velocityY != 0) {
            val splineFlingDistance = mFlingHelper.getSplineFlingDistance(velocityY)
            if (splineFlingDistance > totalDy) {
                childFling(mFlingHelper.getVelocityByDistance(splineFlingDistance - totalDy.toDouble()))
            }
        }
        totalDy = 0
        velocityY = 0
    }

    private fun childFling(velY: Int) {
        findNestedScrollingChildRecyclerView()?.fling(0, velY)
    }

    fun initLayoutManager() {
        val linearLayoutManager =
            object : LinearLayoutManager(context) {
                override fun scrollVerticallyBy(dy: Int, recycler: Recycler?, state: State?): Int {
                    return try {
                        super.scrollVerticallyBy(dy, recycler, state)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        0
                    }
                }

                override fun onLayoutChildren(recycler: Recycler?, state: State?) {
                    try {
                        super.onLayoutChildren(recycler, state)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                override fun canScrollVertically(): Boolean {
                    val childRecyclerView = findNestedScrollingChildRecyclerView()
                    return if (childRecyclerView?.isScrollTop() == false) {
                        canScroll
                    } else {
                        //当childRecyclerView已经滑到顶部，且继续向上滑
                        if (deltaYInTouchEvent < 0) {
                            true
                        } else {
                            canScrollVertically.get()
                        }
                    }
                }

                override fun addDisappearingView(child: View?) {
                    try {
                        super.addDisappearingView(child)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                override fun supportsPredictiveItemAnimations(): Boolean {
                    return false
                }
            }
        linearLayoutManager.orientation = LinearLayoutManager.VERTICAL
        layoutManager = linearLayoutManager
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (ev != null && ev.action == MotionEvent.ACTION_DOWN) {
            //ACTION_DOWN的时候重置加速度
            velocityY = 0
            stopScroll()
        }
        if ((ev == null || ev.action == MotionEvent.ACTION_MOVE).not()) {
            //在非ACTION_MOVE的情况下，将lastY置为0
            lastY = 0f
        }
        return try {
            super.dispatchTouchEvent(ev)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private var deltaYInTouchEvent = 0

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (lastY == 0f) {
            lastY = e.y
        }
        if (isScrollEnd()) {
            //如果父RecyclerView已经滑动到底部，需要让子RecyclerView滑动剩余的距离
            val childRecyclerView = findNestedScrollingChildRecyclerView()
            childRecyclerView?.let {
                deltaYInTouchEvent = (lastY - e.y).toInt()
                canScrollVertically.set(false)
                it.scrollBy(0, deltaYInTouchEvent)
            }
        }
        if (e.action == MotionEvent.ACTION_UP) {
            canScrollVertically.set(true)
        }
        lastY = e.y
        return try {
            super.onTouchEvent(e)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override fun fling(velX: Int, velY: Int): Boolean {
        val fling = super.fling(velX, velY)
        if (!fling || velY <= 0) {
            velocityY = 0
        } else {
            isStartFling = true
            velocityY = velY
        }
        return fling
    }

    private fun isScrollEnd(): Boolean {
        //RecyclerView.canScrollVertically(1)的值表示是否能向上滚动，false表示已经滚动到底部
        return !canScrollVertically(1)
    }

    private fun findNestedScrollingChildRecyclerView(): ChildRecyclerView? {
        (adapter as? MultiTypeAdapter)?.apply {
            return getCurrentChildRecyclerView()
        }
        return null
    }

    override fun scrollToPosition(position: Int) {
        //处理一键置顶会出现卡顿的问题
        findNestedScrollingChildRecyclerView()?.scrollToPosition(position)
        postDelayed({
            super.scrollToPosition(position)
        }, 50)
    }

    //----------------------------------------------------------------------------------------------
    // NestedScroll. fix：当ChildRecyclerView下滑时(手指未放开)，ChildRecyclerView滑动到顶部（非fling），此时ParentRecyclerView不会继续下滑。
    //----------------------------------------------------------------------------------------------

    override fun onStartNestedScroll(child: View?, target: View?, nestedScrollAxes: Int): Boolean {
        return (target != null) && target is ChildRecyclerView
    }

    override fun onNestedPreScroll(target: View?, dx: Int, dy: Int, consumed: IntArray) {
        val childRecyclerView = findNestedScrollingChildRecyclerView()
        //1.当前Parent RecyclerView没有滑动底，且dy> 0 是下滑
        val isParentCanScroll = dy > 0 && isScrollEnd().not()
        //2.当前Child RecyclerView滑到顶部了，且dy < 0,即上滑
        val isChildCanNotScroll = !(dy >= 0
                || childRecyclerView == null
                || childRecyclerView.isScrollTop().not())
        //以上两种情况都需要让Parent RecyclerView去scroll，和下面onNestedPreFling机制类似
        if (isParentCanScroll || isChildCanNotScroll) {
            scrollBy(0, dy)
            consumed[1] = dy
        }
    }

    override fun onNestedPreFling(target: View?, velocityX: Float, velocityY: Float): Boolean {
        val childRecyclerView = findNestedScrollingChildRecyclerView()
        val isParentCanFling = velocityY > 0f && isScrollEnd().not()
        val isChildCanNotFling = !(velocityY >= 0
                || childRecyclerView == null
                || childRecyclerView.isScrollTop().not())
        if (isParentCanFling.not() && isChildCanNotFling.not()) {
            return false
        }
        fling(0, velocityY.toInt())
        return true
    }

    fun isChildRecyclerViewCanScrollUp(): Boolean {
        return findNestedScrollingChildRecyclerView()?.isScrollTop()?.not() ?: false
    }

    //----------------------------------------------------------------------------------------------
    // NestedScroll. fix：当ChildRecyclerView下滑时(手指未放开)，ChildRecyclerView滑动到顶部（非fling），此时ParentRecyclerView不会继续下滑。
    //----------------------------------------------------------------------------------------------
}