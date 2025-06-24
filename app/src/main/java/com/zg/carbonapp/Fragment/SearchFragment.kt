package com.zg.carbonapp.Fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.zg.carbonapp.Adapter.AddSceneAdapter
import com.zg.carbonapp.Dao.Scene
import com.zg.carbonapp.MMKV.SceneMmkv
import com.zg.carbonapp.R
import com.zg.carbonapp.Tool.MyToast
import com.zg.carbonapp.databinding.FragmentSearchBinding
import com.zg.carbonapp.Tool.UpdateRecyclerHelper.sceneList
import com.zg.carbonapp.Tool.UpdateRecyclerHelper.notify

/**
 * A simple [Fragment] subclass.
 * Use the [SearchFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class SearchFragment : Fragment() {
      private lateinit var binding:FragmentSearchBinding


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding=FragmentSearchBinding.inflate(layoutInflater)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
//        sceneList.add(
//            Scene(
//                "森林",
//                "綠色植被，空氣清新",
//                "R.drawable.img",
//                "森林是碳吸收的重要場所...",
//                "碳排放：極低",
//                "空氣質量：優"
//        ))
        //从mmkv获取之前已经存储好的一个列表
        val sceneListFromMmkv = SceneMmkv.getScene()
        sceneList = if (sceneListFromMmkv != null) {
            sceneListFromMmkv
        } else {
            mutableListOf() // 返回空的可变列表
        }
        notify.value=true

        val btn=binding.addScene
        val recyclerView=binding.recyclerView
        val adapter=AddSceneAdapter(sceneList, requireActivity() as AppCompatActivity)
        recyclerView.adapter= adapter
        recyclerView.layoutManager=LinearLayoutManager(context)
        //进行一个实时的更新
        notify.observeForever {
            adapter?.notifyDataSetChanged()//刷新适配器的内容
        }
        btn.setOnClickListener{
               showAddSceneDialog()
        }


    }

    @SuppressLint("SuspiciousIndentation")
    private fun showAddSceneDialog() {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_add_scene, null)

        // 获取各个输入字段
        val etName = view.findViewById<EditText>(R.id.et_name)
        val etDesc = view.findViewById<EditText>(R.id.et_desc)
        val etImage = view.findViewById<EditText>(R.id.et_image)
        val etDetail = view.findViewById<EditText>(R.id.et_detail)


        // 构建对话框 直接以context为脚本

            AlertDialog.Builder(requireActivity())
                .setTitle("请输入你要添加的场景信息")
                .setView(view)
                .setPositiveButton("确定") { dialog, _ ->
                    // 获取所有输入内容
                    val name = etName.text.toString().trim()
                    val desc = etDesc.text.toString().trim()
                    val imageUrl = etImage.text.toString().trim()
                    val detail = etDetail.text.toString().trim()
                    // 验证必填字段
                    if (name.isEmpty()) {
                        context?.let {
                            MyToast.sendToast("场景名称不能为空",
                                it.applicationContext)
                        }
                        return@setPositiveButton
                    }

                    // 创建场景对象（根据你的数据模型调整）
                    val newScene = Scene(
                        name = name,
                        description = desc,
                        imageUrl = imageUrl,
                        detail = detail,
                    )

                    sceneList.add(newScene)
                    SceneMmkv.setScene(sceneList)//这里一样也要存储起来
                    notify.value=true
                    // 关闭对话框
                    dialog.dismiss()
                }
                .setNegativeButton("取消") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()

    }


}