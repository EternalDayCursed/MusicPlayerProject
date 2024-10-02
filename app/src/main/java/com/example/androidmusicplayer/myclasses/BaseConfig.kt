package com.example.androidmusicplayer.myclasses

import android.content.Context
import android.text.format.DateFormat
import androidx.core.content.ContextCompat
import com.example.androidmusicplayer.R
import com.example.androidmusicplayer.extension.getInternalStoragePath
import com.example.androidmusicplayer.extension.getSDCardPath
import com.example.androidmusicplayer.extension.getSharedPrefs
import com.example.androidmusicplayer.helper.ACCENT_COLOR
import com.example.androidmusicplayer.helper.APP_ICON_COLOR
import com.example.androidmusicplayer.helper.APP_ID
import com.example.androidmusicplayer.helper.APP_RUN_COUNT
import com.example.androidmusicplayer.helper.BACKGROUND_COLOR
import com.example.androidmusicplayer.helper.COLOR_PICKER_RECENT_COLORS
import com.example.androidmusicplayer.helper.CONFLICT_SKIP
import com.example.androidmusicplayer.helper.CUSTOM_ACCENT_COLOR
import com.example.androidmusicplayer.helper.CUSTOM_APP_ICON_COLOR
import com.example.androidmusicplayer.helper.CUSTOM_BACKGROUND_COLOR
import com.example.androidmusicplayer.helper.CUSTOM_PRIMARY_COLOR
import com.example.androidmusicplayer.helper.CUSTOM_TEXT_COLOR
import com.example.androidmusicplayer.helper.DATE_FORMAT
import com.example.androidmusicplayer.helper.DATE_FORMAT_EIGHT
import com.example.androidmusicplayer.helper.DATE_FORMAT_FIVE
import com.example.androidmusicplayer.helper.DATE_FORMAT_FOUR
import com.example.androidmusicplayer.helper.DATE_FORMAT_ONE
import com.example.androidmusicplayer.helper.DATE_FORMAT_SEVEN
import com.example.androidmusicplayer.helper.DATE_FORMAT_SIX
import com.example.androidmusicplayer.helper.DATE_FORMAT_THREE
import com.example.androidmusicplayer.helper.DATE_FORMAT_TWO
import com.example.androidmusicplayer.helper.FAVORITES
import com.example.androidmusicplayer.helper.FONT_SIZE
import com.example.androidmusicplayer.helper.INITIAL_WIDGET_HEIGHT
import com.example.androidmusicplayer.helper.INTERNAL_STORAGE_PATH
import com.example.androidmusicplayer.helper.IS_USING_AUTO_THEME
import com.example.androidmusicplayer.helper.IS_USING_MODIFIED_APP_ICON
import com.example.androidmusicplayer.helper.IS_USING_SHARED_THEME
import com.example.androidmusicplayer.helper.IS_USING_SYSTEM_THEME
import com.example.androidmusicplayer.helper.KEEP_LAST_MODIFIED
import com.example.androidmusicplayer.helper.LAST_CONFLICT_APPLY_TO_ALL
import com.example.androidmusicplayer.helper.LAST_CONFLICT_RESOLUTION
import com.example.androidmusicplayer.helper.LAST_EXPORTED_SETTINGS_FOLDER
import com.example.androidmusicplayer.helper.LAST_EXPORT_PATH
import com.example.androidmusicplayer.helper.LAST_ICON_COLOR
import com.example.androidmusicplayer.helper.LAST_USED_VIEW_PAGER_PAGE
import com.example.androidmusicplayer.helper.OTG_ANDROID_DATA_TREE_URI
import com.example.androidmusicplayer.helper.OTG_ANDROID_OBB_TREE_URI
import com.example.androidmusicplayer.helper.OTG_PARTITION
import com.example.androidmusicplayer.helper.OTG_REAL_PATH
import com.example.androidmusicplayer.helper.OTG_TREE_URI
import com.example.androidmusicplayer.helper.PRIMARY_ANDROID_DATA_TREE_URI
import com.example.androidmusicplayer.helper.PRIMARY_ANDROID_OBB_TREE_URI
import com.example.androidmusicplayer.helper.PRIMARY_COLOR
import com.example.androidmusicplayer.helper.SD_ANDROID_DATA_TREE_URI
import com.example.androidmusicplayer.helper.SD_ANDROID_OBB_TREE_URI
import com.example.androidmusicplayer.helper.SD_CARD_PATH
import com.example.androidmusicplayer.helper.SD_TREE_URI
import com.example.androidmusicplayer.helper.SHOULD_USE_SHARED_THEME
import com.example.androidmusicplayer.helper.SORT_FOLDER_PREFIX
import com.example.androidmusicplayer.helper.SORT_ORDER
import com.example.androidmusicplayer.helper.TEXT_COLOR
import com.example.androidmusicplayer.helper.USE_24_HOUR_FORMAT
import com.example.androidmusicplayer.helper.USE_ENGLISH
import com.example.androidmusicplayer.helper.VIEW_TYPE
import com.example.androidmusicplayer.helper.VIEW_TYPE_LIST
import com.example.androidmusicplayer.helper.WAS_APP_ICON_CUSTOMIZATION_WARNING_SHOWN
import com.example.androidmusicplayer.helper.WAS_APP_ON_SD_SHOWN
import com.example.androidmusicplayer.helper.WAS_CUSTOM_THEME_SWITCH_DESCRIPTION_SHOWN
import com.example.androidmusicplayer.helper.WAS_ORANGE_ICON_CHECKED
import com.example.androidmusicplayer.helper.WAS_SHARED_THEME_EVER_ACTIVATED
import com.example.androidmusicplayer.helper.WAS_SHARED_THEME_FORCED
import com.example.androidmusicplayer.helper.WAS_USE_ENGLISH_TOGGLED
import com.example.androidmusicplayer.helper.WIDGET_BG_COLOR
import com.example.androidmusicplayer.helper.WIDGET_ID_TO_MEASURE
import com.example.androidmusicplayer.helper.WIDGET_TEXT_COLOR
import com.example.androidmusicplayer.helper.isSPlus
import java.text.SimpleDateFormat
import java.util.LinkedList


open class BaseConfig(val context: Context) {
    protected val prefs = context.getSharedPrefs()

    companion object {
        fun newInstance(context: Context) = BaseConfig(context)
    }

    var appRunCount: Int
        get() = prefs.getInt(APP_RUN_COUNT, 0)
        set(appRunCount) = prefs.edit().putInt(APP_RUN_COUNT, appRunCount).apply()

    var primaryAndroidDataTreeUri: String
        get() = prefs.getString(PRIMARY_ANDROID_DATA_TREE_URI, "")!!
        set(uri) = prefs.edit().putString(PRIMARY_ANDROID_DATA_TREE_URI, uri).apply()

    var sdAndroidDataTreeUri: String
        get() = prefs.getString(SD_ANDROID_DATA_TREE_URI, "")!!
        set(uri) = prefs.edit().putString(SD_ANDROID_DATA_TREE_URI, uri).apply()

    var otgAndroidDataTreeUri: String
        get() = prefs.getString(OTG_ANDROID_DATA_TREE_URI, "")!!
        set(uri) = prefs.edit().putString(OTG_ANDROID_DATA_TREE_URI, uri).apply()

    var primaryAndroidObbTreeUri: String
        get() = prefs.getString(PRIMARY_ANDROID_OBB_TREE_URI, "")!!
        set(uri) = prefs.edit().putString(PRIMARY_ANDROID_OBB_TREE_URI, uri).apply()

    var sdAndroidObbTreeUri: String
        get() = prefs.getString(SD_ANDROID_OBB_TREE_URI, "")!!
        set(uri) = prefs.edit().putString(SD_ANDROID_OBB_TREE_URI, uri).apply()

    var otgAndroidObbTreeUri: String
        get() = prefs.getString(OTG_ANDROID_OBB_TREE_URI, "")!!
        set(uri) = prefs.edit().putString(OTG_ANDROID_OBB_TREE_URI, uri).apply()

    var sdTreeUri: String
        get() = prefs.getString(SD_TREE_URI, "")!!
        set(uri) = prefs.edit().putString(SD_TREE_URI, uri).apply()

    var OTGTreeUri: String
        get() = prefs.getString(OTG_TREE_URI, "")!!
        set(OTGTreeUri) = prefs.edit().putString(OTG_TREE_URI, OTGTreeUri).apply()

    var OTGPartition: String
        get() = prefs.getString(OTG_PARTITION, "")!!
        set(OTGPartition) = prefs.edit().putString(OTG_PARTITION, OTGPartition).apply()

    var OTGPath: String
        get() = prefs.getString(OTG_REAL_PATH, "")!!
        set(OTGPath) = prefs.edit().putString(OTG_REAL_PATH, OTGPath).apply()

    var sdCardPath: String
        get() = prefs.getString(SD_CARD_PATH, getDefaultSDCardPath())!!
        set(sdCardPath) = prefs.edit().putString(SD_CARD_PATH, sdCardPath).apply()

    private fun getDefaultSDCardPath() = if (prefs.contains(SD_CARD_PATH)) "" else context.getSDCardPath()

    var internalStoragePath: String
        get() = prefs.getString(INTERNAL_STORAGE_PATH, getDefaultInternalPath())!!
        set(internalStoragePath) = prefs.edit().putString(INTERNAL_STORAGE_PATH, internalStoragePath).apply()

    private fun getDefaultInternalPath() = if (prefs.contains(INTERNAL_STORAGE_PATH)) "" else getInternalStoragePath()

    var textColor: Int
        get() = prefs.getInt(TEXT_COLOR, ContextCompat.getColor(context, R.color.default_text_color))
        set(textColor) = prefs.edit().putInt(TEXT_COLOR, textColor).apply()

    var backgroundColor: Int
        get() = prefs.getInt(BACKGROUND_COLOR, ContextCompat.getColor(context, R.color.default_background_color))
        set(backgroundColor) = prefs.edit().putInt(BACKGROUND_COLOR, backgroundColor).apply()

    var primaryColor: Int
        get() = prefs.getInt(PRIMARY_COLOR, ContextCompat.getColor(context, R.color.default_primary_color))
        set(primaryColor) = prefs.edit().putInt(PRIMARY_COLOR, primaryColor).apply()

    var accentColor: Int
        get() = prefs.getInt(ACCENT_COLOR, ContextCompat.getColor(context, R.color.default_accent_color))
        set(accentColor) = prefs.edit().putInt(ACCENT_COLOR, accentColor).apply()

    var appIconColor: Int
        get() = prefs.getInt(APP_ICON_COLOR, ContextCompat.getColor(context, R.color.default_app_icon_color))
        set(appIconColor) {
            isUsingModifiedAppIcon = appIconColor != ContextCompat.getColor(context, R.color.color_primary)
            prefs.edit().putInt(APP_ICON_COLOR, appIconColor).apply()
        }

    var lastIconColor: Int
        get() = prefs.getInt(LAST_ICON_COLOR, ContextCompat.getColor(context, R.color.color_primary))
        set(lastIconColor) = prefs.edit().putInt(LAST_ICON_COLOR, lastIconColor).apply()

    var customTextColor: Int
        get() = prefs.getInt(CUSTOM_TEXT_COLOR, textColor)
        set(customTextColor) = prefs.edit().putInt(CUSTOM_TEXT_COLOR, customTextColor).apply()

    var customBackgroundColor: Int
        get() = prefs.getInt(CUSTOM_BACKGROUND_COLOR, backgroundColor)
        set(customBackgroundColor) = prefs.edit().putInt(CUSTOM_BACKGROUND_COLOR, customBackgroundColor).apply()

    var customPrimaryColor: Int
        get() = prefs.getInt(CUSTOM_PRIMARY_COLOR, primaryColor)
        set(customPrimaryColor) = prefs.edit().putInt(CUSTOM_PRIMARY_COLOR, customPrimaryColor).apply()

    var customAccentColor: Int
        get() = prefs.getInt(CUSTOM_ACCENT_COLOR, accentColor)
        set(customAccentColor) = prefs.edit().putInt(CUSTOM_ACCENT_COLOR, customAccentColor).apply()

    var customAppIconColor: Int
        get() = prefs.getInt(CUSTOM_APP_ICON_COLOR, appIconColor)
        set(customAppIconColor) = prefs.edit().putInt(CUSTOM_APP_ICON_COLOR, customAppIconColor).apply()

    var widgetBgColor: Int
        get() = prefs.getInt(WIDGET_BG_COLOR, ContextCompat.getColor(context, R.color.default_widget_bg_color))
        set(widgetBgColor) = prefs.edit().putInt(WIDGET_BG_COLOR, widgetBgColor).apply()

    var widgetTextColor: Int
        get() = prefs.getInt(WIDGET_TEXT_COLOR, ContextCompat.getColor(context, R.color.default_widget_text_color))
        set(widgetTextColor) = prefs.edit().putInt(WIDGET_TEXT_COLOR, widgetTextColor).apply()

    var keepLastModified: Boolean
        get() = prefs.getBoolean(KEEP_LAST_MODIFIED, true)
        set(keepLastModified) = prefs.edit().putBoolean(KEEP_LAST_MODIFIED, keepLastModified).apply()

    var useEnglish: Boolean
        get() = prefs.getBoolean(USE_ENGLISH, false)
        set(useEnglish) {
            wasUseEnglishToggled = true
            prefs.edit().putBoolean(USE_ENGLISH, useEnglish).commit()
        }

    var wasUseEnglishToggled: Boolean
        get() = prefs.getBoolean(WAS_USE_ENGLISH_TOGGLED, false)
        set(wasUseEnglishToggled) = prefs.edit().putBoolean(WAS_USE_ENGLISH_TOGGLED, wasUseEnglishToggled).apply()

    var wasSharedThemeEverActivated: Boolean
        get() = prefs.getBoolean(WAS_SHARED_THEME_EVER_ACTIVATED, false)
        set(wasSharedThemeEverActivated) = prefs.edit().putBoolean(WAS_SHARED_THEME_EVER_ACTIVATED, wasSharedThemeEverActivated).apply()

    var isUsingSharedTheme: Boolean
        get() = prefs.getBoolean(IS_USING_SHARED_THEME, false)
        set(isUsingSharedTheme) = prefs.edit().putBoolean(IS_USING_SHARED_THEME, isUsingSharedTheme).apply()

    // used by Simple Thank You, stop using shared Shared Theme if it has been changed in it
    var shouldUseSharedTheme: Boolean
        get() = prefs.getBoolean(SHOULD_USE_SHARED_THEME, false)
        set(shouldUseSharedTheme) = prefs.edit().putBoolean(SHOULD_USE_SHARED_THEME, shouldUseSharedTheme).apply()

    var isUsingAutoTheme: Boolean
        get() = prefs.getBoolean(IS_USING_AUTO_THEME, false)
        set(isUsingAutoTheme) = prefs.edit().putBoolean(IS_USING_AUTO_THEME, isUsingAutoTheme).apply()

    var isUsingSystemTheme: Boolean
        get() = prefs.getBoolean(IS_USING_SYSTEM_THEME, isSPlus())
        set(isUsingSystemTheme) = prefs.edit().putBoolean(IS_USING_SYSTEM_THEME, isUsingSystemTheme).apply()

    var wasCustomThemeSwitchDescriptionShown: Boolean
        get() = prefs.getBoolean(WAS_CUSTOM_THEME_SWITCH_DESCRIPTION_SHOWN, false)
        set(wasCustomThemeSwitchDescriptionShown) = prefs.edit().putBoolean(WAS_CUSTOM_THEME_SWITCH_DESCRIPTION_SHOWN, wasCustomThemeSwitchDescriptionShown)
            .apply()

    var wasSharedThemeForced: Boolean
        get() = prefs.getBoolean(WAS_SHARED_THEME_FORCED, false)
        set(wasSharedThemeForced) = prefs.edit().putBoolean(WAS_SHARED_THEME_FORCED, wasSharedThemeForced).apply()

    var lastConflictApplyToAll: Boolean
        get() = prefs.getBoolean(LAST_CONFLICT_APPLY_TO_ALL, true)
        set(lastConflictApplyToAll) = prefs.edit().putBoolean(LAST_CONFLICT_APPLY_TO_ALL, lastConflictApplyToAll).apply()

    var lastConflictResolution: Int
        get() = prefs.getInt(LAST_CONFLICT_RESOLUTION, CONFLICT_SKIP)
        set(lastConflictResolution) = prefs.edit().putInt(LAST_CONFLICT_RESOLUTION, lastConflictResolution).apply()

    var sorting: Int
        get() = prefs.getInt(SORT_ORDER, context.resources.getInteger(R.integer.default_sorting))
        set(sorting) = prefs.edit().putInt(SORT_ORDER, sorting).apply()

    fun saveCustomSorting(path: String, value: Int) {
        if (path.isEmpty()) {
            sorting = value
        } else {
            prefs.edit().putInt(SORT_FOLDER_PREFIX + path.toLowerCase(), value).apply()
        }
    }

    fun getFolderSorting(path: String) = prefs.getInt(SORT_FOLDER_PREFIX + path.toLowerCase(), sorting)

    fun removeCustomSorting(path: String) {
        prefs.edit().remove(SORT_FOLDER_PREFIX + path.toLowerCase()).apply()
    }

    fun hasCustomSorting(path: String) = prefs.contains(SORT_FOLDER_PREFIX + path.toLowerCase())

    var lastUsedViewPagerPage: Int
        get() = prefs.getInt(LAST_USED_VIEW_PAGER_PAGE, context.resources.getInteger(R.integer.default_viewpager_page))
        set(lastUsedViewPagerPage) = prefs.edit().putInt(LAST_USED_VIEW_PAGER_PAGE, lastUsedViewPagerPage).apply()

    var use24HourFormat: Boolean
        get() = prefs.getBoolean(USE_24_HOUR_FORMAT, DateFormat.is24HourFormat(context))
        set(use24HourFormat) = prefs.edit().putBoolean(USE_24_HOUR_FORMAT, use24HourFormat).apply()

    var isUsingModifiedAppIcon: Boolean
        get() = prefs.getBoolean(IS_USING_MODIFIED_APP_ICON, false)
        set(isUsingModifiedAppIcon) = prefs.edit().putBoolean(IS_USING_MODIFIED_APP_ICON, isUsingModifiedAppIcon).apply()

    var appId: String
        get() = prefs.getString(APP_ID, "")!!
        set(appId) = prefs.edit().putString(APP_ID, appId).apply()

    var initialWidgetHeight: Int
        get() = prefs.getInt(INITIAL_WIDGET_HEIGHT, 0)
        set(initialWidgetHeight) = prefs.edit().putInt(INITIAL_WIDGET_HEIGHT, initialWidgetHeight).apply()

    var widgetIdToMeasure: Int
        get() = prefs.getInt(WIDGET_ID_TO_MEASURE, 0)
        set(widgetIdToMeasure) = prefs.edit().putInt(WIDGET_ID_TO_MEASURE, widgetIdToMeasure).apply()

    var wasOrangeIconChecked: Boolean
        get() = prefs.getBoolean(WAS_ORANGE_ICON_CHECKED, false)
        set(wasOrangeIconChecked) = prefs.edit().putBoolean(WAS_ORANGE_ICON_CHECKED, wasOrangeIconChecked).apply()

    var wasAppOnSDShown: Boolean
        get() = prefs.getBoolean(WAS_APP_ON_SD_SHOWN, false)
        set(wasAppOnSDShown) = prefs.edit().putBoolean(WAS_APP_ON_SD_SHOWN, wasAppOnSDShown).apply()

    var wasAppIconCustomizationWarningShown: Boolean
        get() = prefs.getBoolean(WAS_APP_ICON_CUSTOMIZATION_WARNING_SHOWN, false)
        set(wasAppIconCustomizationWarningShown) = prefs.edit().putBoolean(WAS_APP_ICON_CUSTOMIZATION_WARNING_SHOWN, wasAppIconCustomizationWarningShown)
            .apply()

    var dateFormat: String
        get() = prefs.getString(DATE_FORMAT, getDefaultDateFormat())!!
        set(dateFormat) = prefs.edit().putString(DATE_FORMAT, dateFormat).apply()

    private fun getDefaultDateFormat(): String {
        val format = DateFormat.getDateFormat(context)
        val pattern = (format as SimpleDateFormat).toLocalizedPattern()
        return when (pattern.lowercase().replace(" ", "")) {
            "d.M.y" -> DATE_FORMAT_ONE
            "dd/mm/y" -> DATE_FORMAT_TWO
            "mm/dd/y" -> DATE_FORMAT_THREE
            "y-mm-dd" -> DATE_FORMAT_FOUR
            "dmmmmy" -> DATE_FORMAT_FIVE
            "mmmmdy" -> DATE_FORMAT_SIX
            "mm-dd-y" -> DATE_FORMAT_SEVEN
            "dd-mm-y" -> DATE_FORMAT_EIGHT
            else -> DATE_FORMAT_ONE
        }
    }

    var lastExportedSettingsFolder: String
        get() = prefs.getString(LAST_EXPORTED_SETTINGS_FOLDER, "")!!
        set(lastExportedSettingsFolder) = prefs.edit().putString(LAST_EXPORTED_SETTINGS_FOLDER, lastExportedSettingsFolder).apply()

    var fontSize: Int
        get() = prefs.getInt(FONT_SIZE, context.resources.getInteger(R.integer.default_font_size))
        set(size) = prefs.edit().putInt(FONT_SIZE, size).apply()

    var favorites: MutableSet<String>
        get() = prefs.getStringSet(FAVORITES, HashSet())!!
        set(favorites) = prefs.edit().remove(FAVORITES).putStringSet(FAVORITES, favorites).apply()

    // color picker last used colors
    var colorPickerRecentColors: LinkedList<Int>
        get(): LinkedList<Int> {
            val defaultList = arrayListOf(
                ContextCompat.getColor(context, R.color.md_red_700),
                ContextCompat.getColor(context, R.color.md_blue_700),
                ContextCompat.getColor(context, R.color.md_green_700),
                ContextCompat.getColor(context, R.color.md_yellow_700),
                ContextCompat.getColor(context, R.color.md_orange_700)
            )
            return LinkedList(prefs.getString(COLOR_PICKER_RECENT_COLORS, null)?.lines()?.map { it.toInt() } ?: defaultList)
        }
        set(recentColors) = prefs.edit().putString(COLOR_PICKER_RECENT_COLORS, recentColors.joinToString(separator = "\n")).apply()

    var lastExportPath: String
        get() = prefs.getString(LAST_EXPORT_PATH, "")!!
        set(lastExportPath) = prefs.edit().putString(LAST_EXPORT_PATH, lastExportPath).apply()

    var viewType: Int
        get() = prefs.getInt(VIEW_TYPE, VIEW_TYPE_LIST)
        set(viewType) = prefs.edit().putInt(VIEW_TYPE, viewType).apply()

}
