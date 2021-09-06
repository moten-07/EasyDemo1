package com.moten.easydemo

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import com.google.android.material.snackbar.Snackbar
import com.moten.easydemo.databinding.ActivitySettingBinding
/**
 * 视频设置页
 */
class SettingActivity : BaseActivity() {
    private lateinit var binding: ActivitySettingBinding
    companion object{
        var channelList: MutableList<String> = ArrayList(listOf("test01", "test02", "test03"))
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val mAdapter =
            ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, channelList)
        binding.apply {
            edChannelName.setAdapter(mAdapter)
            go.setOnClickListener {
                if (!channelList.contains(edChannelName.text.toString())) {
                    channelList.add(edChannelName.text.toString())
                    mAdapter.notifyDataSetChanged()
                    Log.d("TAG", "onCreate: can add")
                }
                if ((edChannelName.text.toString() == "") || edChannelName.text.isEmpty()
                ) {
                    Snackbar.make(this.root, "频道名不能为空", Snackbar.LENGTH_SHORT).show()
                    return@setOnClickListener

                }
                val intent = Intent(this@SettingActivity, VideoActivity::class.java)
                intent.putExtra("channelName", binding.edChannelName.text.toString())
                startActivity(intent)
            }
        }


    }
}