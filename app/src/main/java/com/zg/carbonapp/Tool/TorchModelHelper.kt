package com.zg.carbonapp.Tool



import android.content.Context
import android.util.Log
import org.pytorch.IValue
import org.pytorch.LiteModuleLoader
import org.pytorch.Module
import org.pytorch.Tensor
import java.io.File
import java.io.FileOutputStream
import java.io.BufferedReader
import java.io.InputStreamReader

class TorchModelHelper(private val context: Context) {
    private var module: Module? = null
    private var scalerMean: FloatArray = floatArrayOf()
    private var scalerScale: FloatArray = floatArrayOf()
    private val TAG = "TorchModelHelper"
    private val REQUIRED_TOTAL_PARAMS = 14  // 7+7=14
    private val REQUIRED_FEATURES = 7

    // 文件名
    private val MODEL_FILE = "energy_model.ptl"
    private val PARAMS_FILE = "scale_params.txt"

    fun init(): Boolean {
        destroy()
        return try {
            // 复制文件
            copyAsset(PARAMS_FILE)
            copyAsset(MODEL_FILE)

            // 加载模型
            val modelFile = File(context.filesDir, MODEL_FILE)
            module = LiteModuleLoader.load(modelFile.absolutePath)
            Log.d(TAG, "模型加载成功")

            // 读取参数文件（关键修复）
            val paramsFile = File(context.filesDir, PARAMS_FILE)
            val params = BufferedReader(InputStreamReader(paramsFile.inputStream())).use { reader ->
                val line = reader.readLine()?.trim() ?: throw Exception("参数文件为空")
                line.split(",").map { it.toFloat() }.toFloatArray()
            }

            // 验证总长度
            if (params.size != REQUIRED_TOTAL_PARAMS) {
                throw Exception("参数总数错误：实际${params.size}，要求$REQUIRED_TOTAL_PARAMS")
            }

            // 拆分均值和标准差
            scalerMean = params.copyOfRange(0, REQUIRED_FEATURES)
            scalerScale = params.copyOfRange(REQUIRED_FEATURES, REQUIRED_TOTAL_PARAMS)

            // 验证特征数
            if (scalerMean.size != REQUIRED_FEATURES || scalerScale.size != REQUIRED_FEATURES) {
                throw Exception("特征数错误：均值${scalerMean.size}，标准差${scalerScale.size}")
            }

            Log.d(TAG, "参数加载成功")
            true
        } catch (e: Exception) {
            Log.e(TAG, "初始化失败：${e.message}")
            destroy()
            false
        }
    }

    private fun copyAsset(filename: String) {
        val destFile = File(context.filesDir, filename)
        if (destFile.exists()) destFile.delete()

        context.assets.open(filename).use { input ->
            FileOutputStream(destFile).use { output ->
                input.copyTo(output)
            }
        }
        Log.d(TAG, "复制文件：$filename")
    }

    fun predict(
        batteryTemp: Float,
        batteryLevel: Int,
        isCharging: Boolean,
        lightLevel: Float,
        tempChangeRate: Float,
        isNight: Boolean,
        pressure: Float
    ): Pair<Float, Float>? {
        if (module == null || scalerMean.size != 7) return null

        return try {
            val input = floatArrayOf(
                batteryTemp, batteryLevel.toFloat(), if (isCharging) 1f else 0f,
                lightLevel, tempChangeRate, if (isNight) 1f else 0f, pressure
            )

            val normalized = FloatArray(7)
            for (i in 0 until 7) {
                normalized[i] = (input[i] - scalerMean[i]) / scalerScale[i]
            }

            val tensor = Tensor.fromBlob(normalized, longArrayOf(1, 7))
            val output = module!!.forward(IValue.from(tensor)).toTensor().dataAsFloatArray
            Pair(output[0].coerceIn(10f, 40f), output[1].coerceIn(20f, 80f))
        } catch (e: Exception) {
            Log.e(TAG, "预测失败：${e.message}")
            null
        }
    }

    fun destroy() {
        module?.destroy()
        module = null
        scalerMean = floatArrayOf()
        scalerScale = floatArrayOf()
    }
}