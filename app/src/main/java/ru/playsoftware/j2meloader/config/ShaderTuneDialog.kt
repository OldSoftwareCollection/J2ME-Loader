/*
 *  Copyright 2020 Yury Kharchenko
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package ru.playsoftware.j2meloader.config

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import ru.playsoftware.j2meloader.R
import ru.playsoftware.j2meloader.databinding.DialogShaderTuneBinding
import ru.playsoftware.j2meloader.databinding.DialogShaderTuneItemBinding
import java.text.DecimalFormat

class ShaderTuneDialog : DialogFragment() {
    
    private var shader: ShaderInfo? = null
    private val seekBars = arrayOfNulls<SeekBar>(4)
    private var callback: Callback? = null
    private var values: FloatArray? = null
    private var nullableParentBinding: DialogShaderTuneBinding? = null
    private val parentBinding get() = nullableParentBinding!!
    
    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is Callback) {
            callback = context
        }
        val bundle = requireArguments()
        shader = bundle.getParcelable(SHADER_KEY)
        if (shader == null) {
            Toast.makeText(context, R.string.error, Toast.LENGTH_SHORT).show()
            dismiss()
            return
        }
        values = shader?.values
    }
    
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val inflater = LayoutInflater.from(context)
        nullableParentBinding = DialogShaderTuneBinding.inflate(inflater)
        val settings = shader?.settings
        val format = DecimalFormat("#.######")
        for (i in 0..3) {
            val setting = settings?.get(i) ?: continue
            DialogShaderTuneItemBinding.inflate(
                inflater, parentBinding.container, false
            ).run {
                seekBars[i] = shaderSettingValue
                val value = if (values != null) values?.get(i) else setting.def
                shaderSettingName.text = getString(R.string.shader_setting,
                    setting.name, format.format(value?.toDouble()))
                if (setting.step <= 0.0f) {
                    setting.step = (setting.max - setting.min) / 100.0f
                }
                shaderSettingValue.max = ((setting.max - setting.min) / setting.step).toInt()
                val progress = ((value?.minus(setting.min))?.div(setting.step))?.toInt()
                progress?.let { shaderSettingValue.progress = it }
                shaderSettingValue.setOnSeekBarChangeListener(
                    object : OnSeekBarChangeListener {
                        override fun onProgressChanged(seekBar: SeekBar, progress: Int,
                                                       fromUser: Boolean) {
                            val settingValue = format.format((setting.min + progress * setting.step).toDouble())
                            shaderSettingName.text = getString(R.string.shader_setting,
                                setting.name, settingValue)
                        }
            
                        override fun onStartTrackingTouch(seekBar: SeekBar) {}
                        override fun onStopTrackingTouch(seekBar: SeekBar) {}
                    }
                )
                parentBinding.container.addView(
                    this@run.root
                )
            }
        }
        with(parentBinding) {
            negativeButton.setOnClickListener { dismiss() }
            positiveButton.setOnClickListener { onClickOk() }
            neutralButton.setOnClickListener { onClickReset() }
        }
        return AlertDialog.Builder(requireActivity()).apply {
            setTitle(R.string.shader_tuning)
            setView(parentBinding.root)
        }.create()
    }
    
    private fun onClickReset() {
        for (i in 0..3) {
            val seekBar = seekBars[i] ?: continue
            val s = shader?.settings?.get(i)
            val progress = ((s?.def?.minus(s.min))?.div(s.step))?.toInt()
            progress?.let { seekBar.progress = it }
        }
    }
    
    private fun onClickOk() {
        val values = FloatArray(4)
        var i = 0
        val sbValuesLength = seekBars.size
        while (i < sbValuesLength) {
            val bar = seekBars[i]
            if (bar == null) {
                i++
                continue
            }
            val setting = shader?.settings?.get(i)
            setting?.let { values[i] = bar.progress * it.step + it.min }
            i++
        }
        callback?.onTuneComplete(values)
        dismiss()
    }
    
    internal interface Callback {
        fun onTuneComplete(values: FloatArray?)
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        nullableParentBinding = null
    }
    
    override fun onDestroy() {
        shader = null
        callback = null
        values = null
        super.onDestroy()
    }
    
    companion object {
        private const val SHADER_KEY = "shader"
        
        @JvmStatic
		fun newInstance(shader: ShaderInfo?): ShaderTuneDialog {
            val args = Bundle().apply {
                putParcelable(SHADER_KEY, shader)
            }
            val fragment = ShaderTuneDialog().apply {
                arguments = args
                isCancelable = false
            }
            return fragment
        }
    }
}