package com.example.flowalbum

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.widget.Button
import androidx.core.content.ContextCompat

/**
 * 主题助手类
 * 用于统一管理和应用主题颜色
 */
class ThemeHelper(private val context: Context) {
    
    /**
     * 主题颜色数据类
     */
    data class ThemeColors(
        val primary: Int,
        val primaryDark: Int,
        val primaryLight: Int,
        val primaryTransparent: Int
    )
    
    /**
     * 获取主题颜色
     */
    fun getThemeColors(themeIndex: Int): ThemeColors {
        return when (themeIndex) {
            0 -> ThemeColors(
                ContextCompat.getColor(context, R.color.theme_cyan),
                ContextCompat.getColor(context, R.color.theme_cyan_dark),
                ContextCompat.getColor(context, R.color.theme_cyan_light),
                ContextCompat.getColor(context, R.color.theme_cyan_transparent)
            )
            1 -> ThemeColors(
                ContextCompat.getColor(context, R.color.theme_red),
                ContextCompat.getColor(context, R.color.theme_red_dark),
                ContextCompat.getColor(context, R.color.theme_red_light),
                ContextCompat.getColor(context, R.color.theme_red_transparent)
            )
            2 -> ThemeColors(
                ContextCompat.getColor(context, R.color.theme_blue),
                ContextCompat.getColor(context, R.color.theme_blue_dark),
                ContextCompat.getColor(context, R.color.theme_blue_light),
                ContextCompat.getColor(context, R.color.theme_blue_transparent)
            )
            3 -> ThemeColors(
                ContextCompat.getColor(context, R.color.theme_orange),
                ContextCompat.getColor(context, R.color.theme_orange_dark),
                ContextCompat.getColor(context, R.color.theme_orange_light),
                ContextCompat.getColor(context, R.color.theme_orange_transparent)
            )
            4 -> ThemeColors(
                ContextCompat.getColor(context, R.color.theme_purple),
                ContextCompat.getColor(context, R.color.theme_purple_dark),
                ContextCompat.getColor(context, R.color.theme_purple_light),
                ContextCompat.getColor(context, R.color.theme_purple_transparent)
            )
            5 -> ThemeColors(
                ContextCompat.getColor(context, R.color.theme_pink),
                ContextCompat.getColor(context, R.color.theme_pink_dark),
                ContextCompat.getColor(context, R.color.theme_pink_light),
                ContextCompat.getColor(context, R.color.theme_pink_transparent)
            )
            6 -> ThemeColors(
                ContextCompat.getColor(context, R.color.theme_gold),
                ContextCompat.getColor(context, R.color.theme_gold_dark),
                ContextCompat.getColor(context, R.color.theme_gold_light),
                ContextCompat.getColor(context, R.color.theme_gold_transparent)
            )
            7 -> ThemeColors(
                ContextCompat.getColor(context, R.color.theme_mint),
                ContextCompat.getColor(context, R.color.theme_mint_dark),
                ContextCompat.getColor(context, R.color.theme_mint_light),
                ContextCompat.getColor(context, R.color.theme_mint_transparent)
            )
            8 -> ThemeColors(
                ContextCompat.getColor(context, R.color.theme_indigo),
                ContextCompat.getColor(context, R.color.theme_indigo_dark),
                ContextCompat.getColor(context, R.color.theme_indigo_light),
                ContextCompat.getColor(context, R.color.theme_indigo_transparent)
            )
            else -> ThemeColors(
                ContextCompat.getColor(context, R.color.theme_cyan),
                ContextCompat.getColor(context, R.color.theme_cyan_dark),
                ContextCompat.getColor(context, R.color.theme_cyan_light),
                ContextCompat.getColor(context, R.color.theme_cyan_transparent)
            )
        }
    }
    
    /**
     * 创建按钮背景Drawable
     */
    fun createButtonDrawable(colors: ThemeColors): StateListDrawable {
        val drawable = StateListDrawable()
        val density = context.resources.displayMetrics.density
        
        // 焦点状态
        val focusedDrawable = GradientDrawable()
        focusedDrawable.setColor(colors.primary)
        focusedDrawable.cornerRadius = 8f * density
        focusedDrawable.setStroke((2 * density).toInt(), colors.primary)
        drawable.addState(intArrayOf(android.R.attr.state_focused), focusedDrawable)
        
        // 按下状态
        val pressedDrawable = GradientDrawable()
        pressedDrawable.setColor(colors.primaryDark)
        pressedDrawable.cornerRadius = 8f * density
        drawable.addState(intArrayOf(android.R.attr.state_pressed), pressedDrawable)
        
        // 选中状态
        val selectedDrawable = GradientDrawable()
        selectedDrawable.setColor(colors.primary)
        selectedDrawable.cornerRadius = 8f * density
        drawable.addState(intArrayOf(android.R.attr.state_selected), selectedDrawable)
        
        // 正常状态
        val normalDrawable = GradientDrawable()
        normalDrawable.setColor(ContextCompat.getColor(context, R.color.button_normal))
        normalDrawable.cornerRadius = 8f * density
        drawable.addState(intArrayOf(), normalDrawable)
        
        return drawable
    }
    
    /**
     * 创建Tab按钮背景Drawable
     * Tab按钮使用透明背景，只在焦点状态显示边框
     */
    fun createTabButtonDrawable(colors: ThemeColors): StateListDrawable {
        val drawable = StateListDrawable()
        val density = context.resources.displayMetrics.density
        
        // 焦点状态 - 显示边框
        val focusedDrawable = GradientDrawable()
        focusedDrawable.setColor(android.graphics.Color.TRANSPARENT)
        focusedDrawable.cornerRadius = 4f * density
        focusedDrawable.setStroke((2 * density).toInt(), colors.primary)
        drawable.addState(intArrayOf(android.R.attr.state_focused), focusedDrawable)
        
        // 选中状态 - 透明背景，无边框
        val selectedDrawable = GradientDrawable()
        selectedDrawable.setColor(android.graphics.Color.TRANSPARENT)
        selectedDrawable.cornerRadius = 4f * density
        drawable.addState(intArrayOf(android.R.attr.state_selected), selectedDrawable)
        
        // 正常状态 - 透明背景
        val normalDrawable = GradientDrawable()
        normalDrawable.setColor(android.graphics.Color.TRANSPARENT)
        normalDrawable.cornerRadius = 4f * density
        drawable.addState(intArrayOf(), normalDrawable)
        
        return drawable
    }
    
    /**
     * 创建文本颜色状态列表
     */
    fun createButtonTextColorStateList(colors: ThemeColors): android.content.res.ColorStateList {
        return android.content.res.ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_focused),
                intArrayOf(android.R.attr.state_pressed),
                intArrayOf()
            ),
            intArrayOf(
                ContextCompat.getColor(context, R.color.button_text_focused),
                ContextCompat.getColor(context, R.color.button_text_focused),
                ContextCompat.getColor(context, R.color.button_text)
            )
        )
    }
    
    /**
     * 创建Tab文本颜色状态列表
     * 只有选中状态使用主题色，聚焦状态保持默认颜色
     */
    fun createTabTextColorStateList(colors: ThemeColors): android.content.res.ColorStateList {
        return android.content.res.ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_selected),
                intArrayOf()
            ),
            intArrayOf(
                colors.primary,  // 选中状态：主题色
                ContextCompat.getColor(context, R.color.dialog_text)  // 其他状态（包括聚焦）：默认灰色
            )
        )
    }
    
    /**
     * 应用主题到按钮
     */
    fun applyThemeToButton(button: Button, colors: ThemeColors) {
        button.background = createButtonDrawable(colors)
        button.setTextColor(createButtonTextColorStateList(colors))
    }
    
    /**
     * 应用主题到Tab按钮
     */
    fun applyThemeToTabButton(button: Button, colors: ThemeColors) {
        button.background = createTabButtonDrawable(colors)
        button.setTextColor(createTabTextColorStateList(colors))
    }
    
    /**
     * 创建RadioButton背景Drawable
     */
    fun createRadioButtonDrawable(colors: ThemeColors): android.graphics.drawable.StateListDrawable {
        val drawable = android.graphics.drawable.StateListDrawable()
        val density = context.resources.displayMetrics.density
        
        // 选中+焦点状态
        val selectedFocusedDrawable = android.graphics.drawable.GradientDrawable()
        selectedFocusedDrawable.setColor(colors.primary)
        selectedFocusedDrawable.cornerRadius = 4f * density
        selectedFocusedDrawable.setStroke((3 * density).toInt(), colors.primaryLight)
        drawable.addState(
            intArrayOf(android.R.attr.state_checked, android.R.attr.state_focused),
            selectedFocusedDrawable
        )
        
        // 选中状态
        val selectedDrawable = android.graphics.drawable.GradientDrawable()
        selectedDrawable.setColor(colors.primary)
        selectedDrawable.cornerRadius = 4f * density
        selectedDrawable.setStroke((2 * density).toInt(), colors.primary)
        drawable.addState(intArrayOf(android.R.attr.state_checked), selectedDrawable)
        
        // 焦点状态
        val focusedDrawable = android.graphics.drawable.GradientDrawable()
        focusedDrawable.setColor(ContextCompat.getColor(context, R.color.button_normal))
        focusedDrawable.cornerRadius = 4f * density
        focusedDrawable.setStroke((3 * density).toInt(), colors.primary)
        drawable.addState(intArrayOf(android.R.attr.state_focused), focusedDrawable)
        
        // 正常状态
        val normalDrawable = android.graphics.drawable.GradientDrawable()
        normalDrawable.setColor(ContextCompat.getColor(context, R.color.button_normal))
        normalDrawable.cornerRadius = 4f * density
        drawable.addState(intArrayOf(), normalDrawable)
        
        return drawable
    }
    
    /**
     * 创建RadioButton文本颜色状态列表
     */
    fun createRadioButtonTextColorStateList(colors: ThemeColors): android.content.res.ColorStateList {
        return android.content.res.ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_checked),
                intArrayOf(android.R.attr.state_focused),
                intArrayOf()
            ),
            intArrayOf(
                ContextCompat.getColor(context, R.color.control_text), // 选中时白色文字
                colors.primary, // 焦点时主题色文字
                ContextCompat.getColor(context, R.color.dialog_text) // 正常时灰色文字
            )
        )
    }
    
    /**
     * 应用主题到RadioButton
     */
    fun applyThemeToRadioButton(radioButton: android.widget.RadioButton, colors: ThemeColors) {
        radioButton.background = createRadioButtonDrawable(colors)
        radioButton.setTextColor(createRadioButtonTextColorStateList(colors))
    }
}