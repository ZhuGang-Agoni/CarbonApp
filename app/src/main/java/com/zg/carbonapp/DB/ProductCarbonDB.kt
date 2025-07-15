package com.zg.carbonapp.DB


import com.zg.carbonapp.Dao.Product

object ProductCarbonDB {
    // 1. 标准产品库（每个品牌1个代表产品）
    private val products = mapOf(
        // 饮料类
        "wahaha_pure" to Product(
            barcode = "default",
            name = "纯净水",
            brand = "娃哈哈",
            category = "beverage",
            carbonFootprint = 0.12,
            unit = "瓶"
        ),
        "nongfu_spring" to Product(
            barcode = "default",
            name = "天然水",
            brand = "农夫山泉",
            category = "beverage",
            carbonFootprint = 0.15,
            unit = "瓶"
        ),
        "coca_cola" to Product(
            barcode = "default",
            name = "可乐",
            brand = "可口可乐",
            category = "beverage",
            carbonFootprint = 0.32,
            unit = "罐"
        ),
        "pepsi" to Product(
            barcode = "default",
            name = "百事可乐",
            brand = "百事",
            category = "beverage",
            carbonFootprint = 0.31,
            unit = "罐"
        ),

        // 乳制品
        "yili_milk" to Product(
            barcode = "default",
            name = "纯牛奶",
            brand = "伊利",
            category = "dairy",
            carbonFootprint = 1.2,
            unit = "盒"
        ),
        "mengniu_milk" to Product(
            barcode = "default",
            name = "纯牛奶",
            brand = "蒙牛",
            category = "dairy",
            carbonFootprint = 1.15,
            unit = "盒"
        ),

        // 食品类
        "masterkong_noodle" to Product(
            barcode = "default",
            name = "方便面",
            brand = "康师傅",
            category = "food",
            carbonFootprint = 2.8,
            unit = "包"
        ),
        "haitian_soy" to Product(
            barcode = "default",
            name = "生抽酱油",
            brand = "海天",
            category = "condiment",
            carbonFootprint = 0.5,
            unit = "瓶"
        )
    )

    // 2. 品牌前缀映射表（核心！厂商识别码前7-8位）
    // 注意：实际前缀需用真实条码验证，以下为示例（可根据实际调整）
    private val brandPrefixMap = mapOf(
        // 娃哈哈（前缀示例：6902083）
        "6902083" to "wahaha_pure",
        // 农夫山泉（前缀示例：69211685）
        "69211685" to "nongfu_spring", "69211685" to "nongfu_spring",
        // 可口可乐（前缀示例：6920673）
        "6920673" to "coca_cola",
        // 百事（前缀示例：6901497）
        "6901497" to "pepsi",
        // 伊利（前缀示例：6907992、6902890）
        "6907992" to "yili_milk", "6902890" to "yili_milk",
        // 蒙牛（前缀示例：6932578）
        "6932578" to "mengniu_milk",
        // 康师傅（前缀示例：6920907）
        "6920907" to "masterkong_noodle",
        // 海天（前缀示例：6902902）
        "6902902" to "haitian_soy"
    )

    // 3. 核心查询方法（先精确匹配，再前缀匹配）
    fun getProductByBarcode(barcode: String): Product? {
        // 步骤1：精确匹配（优先匹配特殊产品）
        val exactMatch = products.values.find { it.barcode == barcode }
        if (exactMatch != null) return exactMatch

        // 步骤2：前缀匹配（覆盖同品牌所有产品）
        val cleanBarcode = barcode.replace("\\D".toRegex(), "") // 过滤非数字字符
        if (cleanBarcode.length >= 7) {
            // 尝试7位和8位前缀（提高匹配率）
            val prefix7 = cleanBarcode.substring(0, 7)
            val prefix8 = if (cleanBarcode.length >= 8) cleanBarcode.substring(0, 8) else ""

            // 检查前缀是否在映射表中
            val productId = brandPrefixMap[prefix7] ?: brandPrefixMap[prefix8]
            if (productId != null) {
                return products[productId]
            }
        }

        // 无匹配结果
        return null
    }

    // 4. 获取低碳替代品（复用原有逻辑）
    fun getLowCarbonAlternatives(category: String): List<Product> {
        return products.values
            .filter { it.category == category }
            .sortedBy { it.carbonFootprint }
            .take(3) // 取前3个低碳产品
    }
}