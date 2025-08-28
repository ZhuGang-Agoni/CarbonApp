package com.zg.carbonapp.ViewModel

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.SurfaceView
import android.view.View
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.zg.carbonapp.MMKV.RecordManager
import com.zg.carbonapp.R
import com.zg.carbonapp.model.GameRecord
import com.zg.carbonapp.model.Garbage
import com.zg.carbonapp.model.GarbageType
import com.zg.carbonapp.model.Player
import kotlinx.coroutines.*
import java.util.*
import kotlin.coroutines.resume

class GarbageSortingGameView(context: Context, attrs: AttributeSet?) : SurfaceView(context, attrs), Runnable {

    // 游戏核心控制
    private var gameThread: Thread? = null
    @Volatile private var isGameRunning = false
    private val canvasLock = Any()
    private var gameSpeed = 6f  // 基础速度
    private var score = 0
    private var isGameOver = false

    // 玩家相关
    private lateinit var player: Player
    private var playerX = 0f
    private val playerY get() = height * 0.75f
    private var playerWidth = 200f
    private var playerHeight = 250f
    private var playerBitmap: Bitmap? = null

    // 垃圾相关
    private val garbages = LinkedList<GarbageItem>()
    private var lastGarbageSpawn = 0L
    private val minSpawnInterval = 800L
    private var maxSpawnInterval = 2000L
    private val maxGarbageSize = 150 // 最大尺寸限制

    // 垃圾数据列表
    private val garbageDataList = listOf(
        Garbage(R.drawable.apple_core, GarbageType.KITCHEN),
        Garbage(R.drawable.battery, GarbageType.HAZARDOUS),
        Garbage(R.drawable.newspaper, GarbageType.RECYCLABLE),
        Garbage(R.drawable.fish_bone, GarbageType.KITCHEN),
        Garbage(R.drawable.clothes, GarbageType.OTHER),
        Garbage(R.drawable.cigarettes, GarbageType.OTHER),
        Garbage(R.drawable.__1, GarbageType.KITCHEN),
        Garbage(R.drawable._4___water_bottle__flat___oliviu_stoian_s_conflict, GarbageType.RECYCLABLE)
    )

    // 垃圾位图缓存
    private val garbageBitmapCache = mutableMapOf<Int, Bitmap>()
    private val isGarbageLoading = mutableMapOf<Int, Boolean>()

    // 垃圾项数据类
    private data class GarbageItem(
        val garbage: Garbage,
        var x: Float,
        var y: Float,
        val bitmap: Bitmap
    )

    // 背景和地面
    private var backgroundBitmap: Bitmap? = null
    private val groundPaint = Paint().apply {
        color = Color.parseColor("#6B8E23")
    }
    private val groundY get() = height * 0.92f

    // 分数绘制
    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 70f
        typeface = Typeface.DEFAULT_BOLD
        isAntiAlias = true
        setShadowLayer(6f, 3f, 3f, Color.BLACK)
    }

    // 游戏事件监听器
    private var gameEventListener: GameEventListener? = null

    // 协程作用域
    private val coroutineScope = MainScope()

    // 垃圾类型中文名称映射
    private val garbageTypeNames = mapOf(
        GarbageType.KITCHEN to "厨余垃圾",
        GarbageType.RECYCLABLE to "可回收物",
        GarbageType.HAZARDOUS to "有害垃圾",
        GarbageType.OTHER to "其他垃圾"
    )

    // 初始化标记
    private var isInitialized = false
    private var isGarbagePreloaded = false

    constructor(context: Context) : this(context, null)

    // 初始化游戏
    fun initGame(selectedPlayer: Player) {
        this.player = selectedPlayer
        coroutineScope.launch {
            // 加载玩家图片
            playerBitmap = loadImageWithGlide(player.imageRes, playerWidth.toInt(), playerHeight.toInt())
            // 预加载所有垃圾图片
            preloadGarbageBitmaps()
            // 初始化玩家位置
            playerX = (width - (playerBitmap?.width ?: playerWidth.toInt())) / 2f
            // 标记初始化完成
            isInitialized = true
            isGarbagePreloaded = true
            startGame()
            Log.d("GameView", "初始化完成：玩家图片+垃圾图片加载成功")
        }
    }

    // 创建渐变背景
    private fun createGradientBackground(width: Int, height: Int): Bitmap {
        if (width <= 0 || height <= 0) return createDefaultBitmap(1, 1)

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 天空渐变背景
        val gradient = LinearGradient(
            0f, 0f, 0f, height.toFloat(),
            Color.parseColor("#87CEEB"),
            Color.parseColor("#E0FFFF"),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), Paint().apply { shader = gradient })

        // 绘制云朵
        val cloudPaint = Paint().apply { color = Color.WHITE; isAntiAlias = true }
        for (i in 0..3) {
            val x = (i * width / 4).toFloat()
            val y = (100 + (i * 50) % 150).toFloat()
            drawCloud(canvas, x, y, cloudPaint)
        }
        return bitmap
    }

    // 绘制云朵
    private fun drawCloud(canvas: Canvas, x: Float, y: Float, paint: Paint) {
        canvas.drawCircle(x, y, 30f, paint)
        canvas.drawCircle(x + 20, y - 10, 25f, paint)
        canvas.drawCircle(x + 40, y, 30f, paint)
        canvas.drawCircle(x + 20, y + 10, 25f, paint)
    }

    // Glide加载图片 - 通用方法
    private suspend fun loadImageWithGlide(resId: Int, targetWidth: Int, targetHeight: Int): Bitmap {
        return suspendCancellableCoroutine { continuation ->
            val target = object : SimpleTarget<Bitmap>(targetWidth, targetHeight) {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    if (!continuation.isCancelled) {
                        Log.d("GameView", "Glide加载成功：resId=$resId，尺寸=${resource.width}x${resource.height}")
                        continuation.resume(resource)
                    }
                }

                override fun onLoadFailed(errorDrawable: Drawable?) {
                    if (!continuation.isCancelled) {
                        Log.e("GameView", "Glide加载失败：resId=$resId，使用默认图片")
                        val defaultBitmap = createDefaultBitmap(targetWidth, targetHeight)
                        continuation.resume(defaultBitmap)
                    }
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    // 加载被清除时取消协程
                    if (!continuation.isCancelled) {
                        continuation.cancel()
                    }
                }
            }

            // 保存target引用，避免被回收
            continuation.invokeOnCancellation {
                Glide.with(context).clear(target)
            }

            // 执行加载
            Glide.with(context)
                .asBitmap()
                .load(resId)
                .override(targetWidth, targetHeight)
                .centerCrop()
                .into(target)
        }
    }

    // Glide加载垃圾图片 - 保持原始宽高比
    private suspend fun loadGarbageImageWithGlide(resId: Int, maxSize: Int): Bitmap {
        return suspendCancellableCoroutine { continuation ->
            val target = object : SimpleTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    if (!continuation.isCancelled) {
                        // 计算保持宽高比的缩放尺寸
                        val originalWidth = resource.width
                        val originalHeight = resource.height

                        val scale: Float = if (originalWidth > originalHeight) {
                            maxSize.toFloat() / originalWidth
                        } else {
                            maxSize.toFloat() / originalHeight
                        }

                        val scaledWidth = (originalWidth * scale).toInt()
                        val scaledHeight = (originalHeight * scale).toInt()

                        val scaledBitmap = Bitmap.createScaledBitmap(
                            resource,
                            scaledWidth,
                            scaledHeight,
                            true
                        )

                        Log.d("GameView", "垃圾图片加载成功：resId=$resId，原始尺寸=${originalWidth}x${originalHeight}，缩放后=${scaledWidth}x${scaledHeight}")
                        continuation.resume(scaledBitmap)
                    }
                }

                override fun onLoadFailed(errorDrawable: Drawable?) {
                    if (!continuation.isCancelled) {
                        Log.e("GameView", "垃圾图片加载失败：resId=$resId，使用默认图片")
                        val defaultBitmap = createDefaultBitmap(maxSize, maxSize)
                        continuation.resume(defaultBitmap)
                    }
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    if (!continuation.isCancelled) {
                        continuation.cancel()
                    }
                }
            }

            continuation.invokeOnCancellation {
                Glide.with(context).clear(target)
            }

            // 执行加载 - 不指定固定尺寸，让Glide加载原始尺寸
            Glide.with(context)
                .asBitmap()
                .load(resId)
                .into(target)
        }
    }

    // 创建默认图片
    private fun createDefaultBitmap(width: Int, height: Int): Bitmap {
        val safeW = if (width <= 0) maxGarbageSize else width
        val safeH = if (height <= 0) maxGarbageSize else height

        val bitmap = Bitmap.createBitmap(safeW, safeH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 黄色背景
        canvas.drawRect(0f, 0f, safeW.toFloat(), safeH.toFloat(), Paint().apply { color = Color.parseColor("#FFFFCC") })

        // 提示文字
        val tipPaint = Paint().apply {
            color = Color.RED
            textSize = 24f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
            typeface = Typeface.DEFAULT_BOLD
        }
        canvas.drawText("加载失败", safeW/2f, safeH/2f - 15f, tipPaint)
        canvas.drawText("resId错误", safeW/2f, safeH/2f + 20f, tipPaint)

        return bitmap
    }

    // 预加载所有垃圾图片
    private suspend fun preloadGarbageBitmaps() {
        for (garbage in garbageDataList) {
            val resId = garbage.imageRes
            if (garbageBitmapCache.containsKey(resId) || isGarbageLoading[resId] == true) {
                continue
            }
            isGarbageLoading[resId] = true
            val bitmap = loadGarbageImageWithGlide(resId, maxGarbageSize)
            garbageBitmapCache[resId] = bitmap
            isGarbageLoading[resId] = false
        }
    }

    // 设置游戏事件监听器
    fun setOnGameEventListener(listener: GameEventListener) {
        this.gameEventListener = listener
    }

    // 游戏主循环
    override fun run() {
        while (isGameRunning) {
            if (!holder.surface.isValid) continue
            if (!isInitialized || playerBitmap == null) {
                controlFps()
                continue
            }

            updateGame()
            renderGame()
            controlFps()
        }
    }

    // 更新游戏逻辑
    private fun updateGame() {
        if (isGameOver) return
        if (!isGarbagePreloaded) return

        updateGarbagePosition()
        trySpawnNewGarbage()
        checkCollision()
    }

    // 更新垃圾位置
    private fun updateGarbagePosition() {
        val iterator = garbages.iterator()
        while (iterator.hasNext()) {
            val garbage = iterator.next()
            garbage.y += gameSpeed

            if (garbage.y > groundY + 20f) {
                iterator.remove()
                score = maxOf(0, score - 10)
                gameEventListener?.onGarbageMissed()
            }
        }
    }

    // 生成新垃圾
    private fun trySpawnNewGarbage() {
        val now = System.currentTimeMillis()
        val randomInterval = (Math.random() * (maxSpawnInterval - minSpawnInterval) + minSpawnInterval).toLong()
        if (now - lastGarbageSpawn < randomInterval) {
            return
        }
        lastGarbageSpawn = now

        val randomGarbage = garbageDataList.random()
        val originalBitmap = garbageBitmapCache[randomGarbage.imageRes]
            ?: run {
                Log.w("GameView", "垃圾图片未缓存：resId=${randomGarbage.imageRes}，跳过生成")
                return
            }

        // 随机缩放尺寸 (0.8-1.2倍)
        val scale = 0.8f + (Math.random() * 0.4f).toFloat()
        val scaledWidth = (originalBitmap.width * scale).toInt()
        val scaledHeight = (originalBitmap.height * scale).toInt()
        val scaledBitmap = Bitmap.createScaledBitmap(
            originalBitmap,
            scaledWidth,
            scaledHeight,
            true
        )

        // 计算随机X位置
        val maxX = width - scaledBitmap.width
        val spawnX = if (maxX <= 0) 0f else (Math.random() * maxX).toFloat()

        synchronized(canvasLock) {
            garbages.add(GarbageItem(
                garbage = randomGarbage,
                x = spawnX,
                y = -scaledBitmap.height.toFloat(),
                bitmap = scaledBitmap
            ))
        }
    }

    // 检测碰撞
    private fun checkCollision() {
        val playerRect = RectF(
            playerX,
            playerY,
            playerX + (playerBitmap?.width ?: 0).toFloat(),
            playerY + (playerBitmap?.height ?: 0).toFloat()
        )

        val iterator = garbages.iterator()
        while (iterator.hasNext()) {
            val garbage = iterator.next()
            val garbageRect = RectF(
                garbage.x,
                garbage.y,
                garbage.x + garbage.bitmap.width,
                garbage.y + garbage.bitmap.height
            )

            if (RectF.intersects(playerRect, garbageRect)) {
                if (garbage.garbage.type == player.type) {
                    score += 20
                    if (score % 200 == 0) {
                        gameSpeed += 0.5f
                        maxSpawnInterval = maxOf(500L, maxSpawnInterval - 100)
                    }
                } else {
                    score = maxOf(0, score - 30)
                    gameEventListener?.onWrongGarbageCollected(
                        garbageTypeNames[garbage.garbage.type] ?: "未知垃圾",
                        garbageTypeNames[player.type] ?: "未知垃圾桶"
                    )
                }
                iterator.remove()
            }
        }
    }

    // 绘制游戏画面
    private fun renderGame() {
        synchronized(canvasLock) {
            val canvas = holder.lockCanvas() ?: return

            try {
                // 绘制背景
                if (backgroundBitmap != null) {
                    canvas.drawBitmap(backgroundBitmap!!, 0f, 0f, null)
                } else {
                    canvas.drawColor(Color.parseColor("#87CEEB"))
                }

                // 绘制地面
                canvas.drawRect(0f, groundY, width.toFloat(), height.toFloat(), groundPaint)

                // 绘制玩家
                playerBitmap?.let {
                    canvas.drawBitmap(it, playerX, playerY, null)
                }

                // 绘制垃圾
                garbages.forEach { garbage ->
                    canvas.drawBitmap(garbage.bitmap, garbage.x, garbage.y, null)
                }

                // 绘制分数
                textPaint.textAlign = Paint.Align.LEFT
                textPaint.textSize = 70f
                canvas.drawText("分数: $score", 30f, 80f, textPaint)

                // 绘制当前角色类型
                textPaint.textSize = 50f
                val playerTypeName = garbageTypeNames[player.type] ?: "未知垃圾桶"
                canvas.drawText("当前: $playerTypeName", 30f, 150f, textPaint)
                textPaint.textSize = 70f

                // 游戏结束画面
                if (isGameOver) {
                    val overlayPaint = Paint().apply {
                        color = Color.parseColor("#CC000000")
                        isAntiAlias = true
                    }
                    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), overlayPaint)

                    textPaint.textAlign = Paint.Align.CENTER
                    textPaint.textSize = 90f
                    canvas.drawText("游戏结束", width/2f, height/2f - 50f, textPaint)

                    textPaint.textSize = 70f
                    canvas.drawText("最终得分: $score", width/2f, height/2f + 50f, textPaint)

                    textPaint.textSize = 50f
                    canvas.drawText("点击屏幕重新开始", width/2f, height/2f + 130f, textPaint)
                }
            } finally {
                holder.unlockCanvasAndPost(canvas)
            }
        }
    }

    // 控制帧率
    private fun controlFps() {
        try {
            Thread.sleep(16)  // 约60 FPS
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    // 触摸事件处理
    private var touchStartX = 0f

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (isGameOver && event.action == MotionEvent.ACTION_DOWN) {
            resetGame()
            return true
        }

        if (isGameOver) return true

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                touchStartX = event.x
            }
            MotionEvent.ACTION_MOVE -> {
                val deltaX = event.x - touchStartX
                playerX += deltaX * 0.5f
                touchStartX = event.x

                // 限制玩家在屏幕内
                val maxX = width - (playerBitmap?.width ?: playerWidth.toInt())
                playerX = playerX.coerceIn(0f, maxX.toFloat())
            }
        }
        return true
    }

    // 复活
    fun resurrect() {
        isGameOver = false
    }

    // 游戏结束
    fun gameOver() {
        isGameOver = true

        // 保存游戏记录
        val record = GameRecord(
            score = score,
            playerType = garbageTypeNames[player.type] ?: "未知",
            date = Date(),
            playerAvatar = player.imageRes
        )

        coroutineScope.launch(Dispatchers.IO) {
            RecordManager.addRecord(record)
        }

        gameEventListener?.onGameOver(score)
    }

    // 重置游戏
    fun resetGame() {
        synchronized(canvasLock) {
            isGameOver = false
            score = 0
            gameSpeed = 6f
            maxSpawnInterval = 2000L
            garbages.clear()
            playerX = (width - (playerBitmap?.width ?: playerWidth.toInt())) / 2f
        }
    }

    // 启动游戏
    fun startGame() {
        if (!isGameRunning) {
            isGameRunning = true
            gameThread = Thread(this)
            gameThread?.start()
        }
    }

    // 停止游戏
    fun stopGame() {
        isGameRunning = false
        try {
            gameThread?.join()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    // 生命周期管理
    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (visibility == View.GONE) {
            stopGame()
        } else if (visibility == View.VISIBLE && isInitialized) {
            startGame()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0) {
            backgroundBitmap = createGradientBackground(w, h)
            if (isInitialized) {
                playerX = (w - (playerBitmap?.width ?: playerWidth.toInt())) / 2f
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopGame()
        coroutineScope.cancel()

        // 清理位图缓存
        garbageBitmapCache.values.forEach { bitmap ->
            if (!bitmap.isRecycled) {
                bitmap.recycle()
            }
        }
        garbageBitmapCache.clear()

        backgroundBitmap?.let {
            if (!it.isRecycled) {
                it.recycle()
            }
        }

        playerBitmap?.let {
            if (!it.isRecycled) {
                it.recycle()
            }
        }
    }

    // 游戏事件监听器接口
    interface GameEventListener {
        fun onGarbageMissed()
        fun onWrongGarbageCollected(garbageType: String, playerType: String)
        fun onGameOver(score: Int)
    }
}