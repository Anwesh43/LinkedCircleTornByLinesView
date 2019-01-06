package com.anwesh.uiprojects.circletornbylinesview

/**
 * Created by anweshmishra on 06/01/19.
 */

import android.view.View
import android.view.MotionEvent
import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Color

val nodes : Int = 5
val lines : Int = 4
val scGap : Float = 0.05f
val scDiv : Double = 0.51
val strokeFactor : Int = 90
val sizeFactor : Float = 2.9f
val rSizeFactor : Float = 3.5f
val foreColor : Int = Color.parseColor("#f44336")
val backColor : Int = Color.parseColor("#BDBDBD")
val delay : Long = 25

fun Int.inverse() : Float = 1f / this
fun Float.maxScale(i : Int, n : Int) : Float = Math.max(0f, this - i * n.inverse())
fun Float.divideScale(i : Int, n : Int) : Float = Math.min(n.inverse(), maxScale(i, n)) * n
fun Float.scaleFactor() : Float = Math.floor(this / scDiv).toFloat()
fun Float.mirrorValue(a : Int, b : Int) : Float = (1 - scaleFactor()) * a.inverse() + scaleFactor() * b.inverse()
fun Float.updateScale(dir : Float, a : Int, b : Int) : Float = mirrorValue(a, b) * dir * scGap

fun Canvas.drawLineIndicator(x : Float, size : Float, sc1 : Float, sc2 : Float, paint : Paint) {
    save()
    translate(x, 0f)
    rotate(90f * sc2)
    drawLine(-size/2, 0f, -size/2 + 2 * size * sc1, 0f, paint)
    restore()
}

fun Canvas.drawLineIndicatorOnEitherSide(x : Float, size : Float, sc1 : Float, sc2 : Float, paint: Paint) {
    for (j in 0..1) {
        val k : Float = 1f - 2 * j
        drawLineIndicator(x * k, size, sc1, sc2, paint)
    }
}

fun Canvas.drawCTLNode(i : Int, scale : Float, paint : Paint) {
    val w : Float = width.toFloat()
    val h : Float = height.toFloat()
    val gap : Float = h / (nodes + 1)
    val size : Float = gap / sizeFactor
    val rSize : Float = gap / rSizeFactor
    val sc1 : Float = scale.divideScale(0, 2)
    val sc2 : Float = scale.divideScale(1, 2)
    paint.style = Paint.Style.STROKE
    paint.strokeCap = Paint.Cap.ROUND
    paint.strokeWidth = Math.min(w, h) / strokeFactor
    paint.color = foreColor
    val yGap : Float = (2 * rSize) / (lines)
    save()
    translate(w/2, gap * (i + 1))
    drawLineIndicatorOnEitherSide(w/2 - size * 1.3f, size, sc1, sc2, paint)
    rotate(90f * sc2)
    drawCircle(0f, 0f, rSize, paint)
    translate(-size, -size)
    for (j in 0..(lines - 1)) {
        val sc : Float = sc1.divideScale(j, lines)
        save()
        translate(0f, yGap * (j + 1))
        drawLine(0f, 0f, 2 * size * sc, 0f, paint)
        restore()
    }
    restore()
}

class CircleTornByLinesView(ctx : Context) : View(ctx) {

    private val paint : Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val renderer : Renderer = Renderer(this)

    override fun onDraw(canvas : Canvas) {
        renderer.render(canvas, paint)
    }

    override fun onTouchEvent(event : MotionEvent) : Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                renderer.handleTap()
            }
        }
        return true
    }

    data class State(var scale : Float = 0f, var dir : Float = 0f, var prevScale : Float = 0f) {

        fun update(cb : (Float) -> Unit) {
            scale += scale.updateScale(dir, lines, 1)
            if (Math.abs(scale - prevScale) > 1) {
                scale = prevScale + dir
                dir = 0f
                prevScale = scale
                cb(prevScale)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            if (dir == 0f) {
                dir = 1f - 2 * prevScale
                cb()
            }
        }
    }

    data class Animator(var view : View, var animated : Boolean = false) {

        fun animate(cb : () -> Unit) {
            if (animated) {
                cb()
                try {
                    Thread.sleep(delay)
                    view.invalidate()
                } catch(ex : Exception) {

                }
            }
        }

        fun start() {
            if (!animated) {
                animated = true
                view.postInvalidate()
            }
        }

        fun stop() {
            if (animated) {
                animated = false
            }
        }
    }

    data class CTLNode(var i : Int, val state : State = State()) {

        private var next : CTLNode? = null
        private var prev : CTLNode? = null

        init {
            addNeighbor()
        }

        fun addNeighbor() {
            if (i < nodes - 1) {
                next = CTLNode(i + 1)
                next?.prev = this
            }
        }

        fun draw(canvas : Canvas, paint : Paint) {
            canvas.drawCTLNode(i, state.scale, paint)
            next?.draw(canvas, paint)
        }

        fun update(cb : (Int, Float) -> Unit) {
            state.update {
                cb(i, it)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            state.startUpdating(cb)
        }

        fun getNext(dir : Int, cb : () -> Unit) : CTLNode {
            var curr : CTLNode? = prev
            if (dir == 1) {
                curr = next
            }
            if (curr != null) {
                return curr
            }
            cb()
            return this
        }
    }

    data class CircleTornByLines(var i : Int) {

        private var root : CTLNode = CTLNode(0)
        private var curr : CTLNode = root
        private var dir : Int = 1

        fun draw(canvas : Canvas, paint : Paint) {
            root.draw(canvas, paint)
        }

        fun update(cb : (Int, Float) -> Unit) {
            curr.update {i, scl ->
                curr = curr.getNext(dir) {
                    dir *= -1
                }
                cb(i, scl)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            curr.startUpdating(cb)
        }
    }

    data class Renderer(var view : CircleTornByLinesView) {

        private val animator : Animator = Animator(view)
        private val ctl : CircleTornByLines = CircleTornByLines(0)

        fun render(canvas : Canvas, paint : Paint) {
            canvas.drawColor(backColor)
            ctl.draw(canvas, paint)
            animator.animate {
                ctl.update {i, scl ->
                    animator.stop()
                }
            }
        }

        fun handleTap() {
            ctl.startUpdating {
                animator.start()
            }
        }
    }

    companion object {
        fun create(activity : Activity) : CircleTornByLinesView {
            val view : CircleTornByLinesView = CircleTornByLinesView(activity)
            activity.setContentView(view)
            return view
        }
    }
}