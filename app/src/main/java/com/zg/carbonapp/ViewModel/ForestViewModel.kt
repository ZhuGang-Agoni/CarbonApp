//package com.zg.carbonapp.ViewModel
//
//import androidx.lifecycle.LiveData
//import androidx.lifecycle.MutableLiveData
//import androidx.lifecycle.ViewModel
//import com.zg.carbonapp.Dao.TreeModel
//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.cancel
//import kotlinx.coroutines.delay
//import kotlinx.coroutines.launch
//
//// ForestViewModel.kt - 森林场景数据管理
//class ForestViewModel : ViewModel() {
//
//    private val _trees = MutableLiveData<List<TreeModel>>(emptyList())
//    val trees: LiveData<List<TreeModel>> = _trees
//
//    private val growthUpdater = CoroutineScope(Dispatchers.Default)
//
//    init {
//        // 模拟初始树木
//        val initialTrees = listOf(
//            TreeModel("1", System.currentTimeMillis() - 86400000, 0.9f, TreeModel.TreeType.PINE, 200.0),
//            TreeModel("2", System.currentTimeMillis() - 172800000, 1.0f, TreeModel.TreeType.OAK, 200.0),
//            TreeModel("3", System.currentTimeMillis() - 259200000, 1.0f, TreeModel.TreeType.MAPLE, 200.0)
//        )
//        _trees.value = initialTrees
//
//        // 启动生长更新器
//        startGrowthUpdates()
//    }
//
//    fun addTree(tree: Unit) {
//        val current = _trees.value ?: emptyList()
//        _trees.value = current + tree
//    }
//
//    fun getTotalCarbonReduction(): Double {
//        return trees.value?.sumOf { it.carbonReduction } ?: 0.0
//    }
//
//    private fun startGrowthUpdates() {
//        growthUpdater.launch {
//            while (true) {
//                delay(60000) // 每分钟更新一次生长状态
//
//                val updatedTrees = _trees.value?.map { tree ->
//                    if (tree.growthStage < 1.0f) {
//                        // 每分钟增长0.1%
//                        val newGrowth = (tree.growthStage + 0.001f).coerceAtMost(1.0f)
//                        tree.copy(growthStage = newGrowth)
//                    } else {
//                        tree
//                    }
//                }
//
//                updatedTrees?.let {
//                    _trees.postValue(it)
//                }
//            }
//        }
//    }
//
//    override fun onCleared() {
//        super.onCleared()
//        growthUpdater.cancel()
//    }
//}