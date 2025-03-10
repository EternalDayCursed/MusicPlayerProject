package com.example.androidmusicplayer.helper

import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Looper
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.annotation.StringRes
import com.example.androidmusicplayer.R
import com.example.androidmusicplayer.extension.normalizeString

const val ALL_TRACKS_PLAYLIST_ID = 1
const val EQUALIZER_PRESET_CUSTOM = -1

const val ARTIST = "artist"
const val ALBUM = "album"
const val TRACK = "track"
const val PLAYLIST = "playlist"
const val FOLDER = "folder"
const val GENRE = "genre"

const val PATH = "com.example.androidmusicplayer.action."
val artworkUri = Uri.parse("content://media/external/audio/albumart")

const val PREVIOUS = PATH + "PREVIOUS"
const val PLAYPAUSE = PATH + "PLAYPAUSE"
const val NEXT = PATH + "NEXT"
const val TRACK_STATE_CHANGED = "TRACK_STATE_CHANGED"
const val EXTRA_ID = "id"
const val EXTRA_MEDIA_STORE_ID = "media_store_id"
const val EXTRA_TITLE = "title"
const val EXTRA_ARTIST = "artist"
const val EXTRA_PATH = "path"
const val EXTRA_DURATION = "duration"
const val EXTRA_ALBUM = "album"
const val EXTRA_GENRE = "genre"
const val EXTRA_COVER_ART = "cover_art"
const val EXTRA_PLAYLIST_ID = "playlist_id"
const val EXTRA_TRACK_ID = "track_id"
const val EXTRA_FOLDER_NAME = "folder_name"
const val EXTRA_ALBUM_ID = "album_id"
const val EXTRA_ARTIST_ID = "artist_id"
const val EXTRA_GENRE_ID = "genre_id"
const val EXTRA_YEAR = "year"
const val EXTRA_DATE_ADDED = "date_added"
const val EXTRA_ORDER_IN_PLAYLIST = "order_in_playlist"
const val EXTRA_FLAGS = "flags"
const val EXTRA_NEXT_MEDIA_ID = "EXTRA_NEXT_MEDIA_ID"
const val EXTRA_SHUFFLE_INDICES = "EXTRA_SHUFFLE_INDICES"

// shared preferences
const val SHUFFLE = "shuffle"
const val PLAYBACK_SETTING = "playback_setting"
const val AUTOPLAY = "autoplay"
const val SHOW_FILENAME = "show_filename"
const val SWAP_PREV_NEXT = "swap_prev_next"
const val LAST_SLEEP_TIMER_SECONDS = "last_sleep_timer_seconds"
const val SLEEP_IN_TS = "sleep_in_ts"
const val EQUALIZER_PRESET = "EQUALIZER_PRESET"
const val EQUALIZER_BANDS = "EQUALIZER_BANDS"
const val PLAYBACK_SPEED = "PLAYBACK_SPEED"
const val PLAYBACK_SPEED_PROGRESS = "PLAYBACK_SPEED_PROGRESS"
const val WAS_ALL_TRACKS_PLAYLIST_CREATED = "was_all_tracks_playlist_created"
const val TRACKS_REMOVED_FROM_ALL_TRACKS_PLAYLIST = "tracks_removed_from_all_tracks_playlist"
const val EXCLUDED_FOLDERS = "excluded_folders"
const val SORT_PLAYLIST_PREFIX = "sort_playlist_"
const val GAPLESS_PLAYBACK = "gapless_playback"
const val BASE_URL = "https://dfd6-185-97-201-250.ngrok-free.app/"

const val SEEK_INTERVAL_MS = 10000L
const val SEEK_INTERVAL_S = 10

const val SHOW_FILENAME_NEVER = 1
const val SHOW_FILENAME_IF_UNAVAILABLE = 2
const val SHOW_FILENAME_ALWAYS = 3

const val TAB_PLAYLISTS = 1
const val TAB_FOLDERS = 2
const val TAB_ALBUMS = 8
const val TAB_TRACKS = 16
const val ACTIVITY_PLAYLIST_FOLDER = 64

const val FLAG_MANUAL_CACHE = 1
const val FLAG_IS_CURRENT = 2

// show Folders tab only on Android Q+, BUCKET_DISPLAY_NAME hasn't been available before that
val allTabsMask = if (isQPlus()) {
    TAB_PLAYLISTS or TAB_FOLDERS or TAB_TRACKS
} else {
    TAB_PLAYLISTS or TAB_TRACKS
}

val tabsList: ArrayList<Int>
    get() = if (isQPlus()) {
        arrayListOf(
            TAB_PLAYLISTS,
            TAB_FOLDERS,
            TAB_TRACKS,
        )
    } else {
        arrayListOf(
            TAB_PLAYLISTS,
            TAB_TRACKS,
        )
    }

// use custom sorting constants, there are too many app specific ones
const val PLAYER_SORT_BY_TITLE = 1
const val PLAYER_SORT_BY_TRACK_COUNT = 2
const val PLAYER_SORT_BY_ALBUM_COUNT = 4
const val PLAYER_SORT_BY_YEAR = 8
const val PLAYER_SORT_BY_DURATION = 16
const val PLAYER_SORT_BY_ARTIST_TITLE = 32
const val PLAYER_SORT_BY_TRACK_ID = 64
const val PLAYER_SORT_BY_CUSTOM = 128
const val PLAYER_SORT_BY_DATE_ADDED = 256

const val PLAYLIST_SORTING = "playlist_sorting"
const val PLAYLIST_TRACKS_SORTING = "playlist_tracks_sorting"
const val FOLDER_SORTING = "folder_sorting"
const val ARTIST_SORTING = "artist_sorting"
const val ALBUM_SORTING = "album_sorting"
const val TRACK_SORTING = "track_sorting"
const val GENRE_SORTING = "genre_sorting"

const val MIME_TYPE_M3U = "audio/x-mpegurl"
const val M3U_HEADER = "#EXTM3U"
const val M3U_ENTRY = "#EXTINF:"
const val M3U_DURATION_SEPARATOR = ","

fun getPermissionToRequest() = if (isTiramisuPlus()) PERMISSION_READ_MEDIA_AUDIO else PERMISSION_WRITE_STORAGE

const val EXTERNAL_STORAGE_PROVIDER_AUTHORITY = "com.android.externalstorage.documents"
const val EXTRA_SHOW_ADVANCED = "android.content.extra.SHOW_ADVANCED"

const val APP_ICON_IDS = "app_icon_ids"
const val APP_ID = "app_id"
const val APP_LAUNCHER_NAME = "app_launcher_name"
const val IS_CUSTOMIZING_COLORS = "is_customizing_colors"

const val SAVE_DISCARD_PROMPT_INTERVAL = 1000L
const val SD_OTG_PATTERN = "^/storage/[A-Za-z0-9]{4}-[A-Za-z0-9]{4}$"
const val SD_OTG_SHORT = "^[A-Za-z0-9]{4}-[A-Za-z0-9]{4}$"
const val MD5 = "MD5"
const val SHORT_ANIMATION_DURATION = 150L
val DARK_GREY = 0xFF333333.toInt()

const val LOWER_ALPHA = 0.25f
const val MEDIUM_ALPHA = 0.5f
const val HIGHER_ALPHA = 0.75f

const val HOUR_MINUTES = 60
const val DAY_MINUTES = 24 * HOUR_MINUTES
const val WEEK_MINUTES = DAY_MINUTES * 7
const val MONTH_MINUTES = DAY_MINUTES * 30
const val YEAR_MINUTES = DAY_MINUTES * 365

const val MINUTE_SECONDS = 60
const val HOUR_SECONDS = HOUR_MINUTES * 60
const val DAY_SECONDS = DAY_MINUTES * 60
const val WEEK_SECONDS = WEEK_MINUTES * 60
const val MONTH_SECONDS = MONTH_MINUTES * 60
const val YEAR_SECONDS = YEAR_MINUTES * 60

// shared preferences
const val PREFS_KEY = "Prefs"
const val APP_RUN_COUNT = "app_run_count"
const val SD_TREE_URI = "tree_uri_2"
const val PRIMARY_ANDROID_DATA_TREE_URI = "primary_android_data_tree_uri_2"
const val OTG_ANDROID_DATA_TREE_URI = "otg_android_data_tree__uri_2"
const val SD_ANDROID_DATA_TREE_URI = "sd_android_data_tree_uri_2"
const val PRIMARY_ANDROID_OBB_TREE_URI = "primary_android_obb_tree_uri_2"
const val OTG_ANDROID_OBB_TREE_URI = "otg_android_obb_tree_uri_2"
const val SD_ANDROID_OBB_TREE_URI = "sd_android_obb_tree_uri_2"
const val OTG_TREE_URI = "otg_tree_uri_2"
const val SD_CARD_PATH = "sd_card_path_2"
const val OTG_REAL_PATH = "otg_real_path_2"
const val INTERNAL_STORAGE_PATH = "internal_storage_path"
const val TEXT_COLOR = "text_color"
const val BACKGROUND_COLOR = "background_color"
const val PRIMARY_COLOR = "primary_color_2"
const val ACCENT_COLOR = "accent_color"
const val APP_ICON_COLOR = "app_icon_color"
const val LAST_ICON_COLOR = "last_icon_color"
const val CUSTOM_TEXT_COLOR = "custom_text_color"
const val CUSTOM_BACKGROUND_COLOR = "custom_background_color"
const val CUSTOM_PRIMARY_COLOR = "custom_primary_color"
const val CUSTOM_ACCENT_COLOR = "custom_accent_color"
const val CUSTOM_APP_ICON_COLOR = "custom_app_icon_color"
const val WIDGET_BG_COLOR = "widget_bg_color"
const val WIDGET_TEXT_COLOR = "widget_text_color"
const val KEEP_LAST_MODIFIED = "keep_last_modified"
const val USE_ENGLISH = "use_english"
const val WAS_USE_ENGLISH_TOGGLED = "was_use_english_toggled"
const val WAS_SHARED_THEME_EVER_ACTIVATED = "was_shared_theme_ever_activated"
const val IS_USING_SHARED_THEME = "is_using_shared_theme"
const val IS_USING_AUTO_THEME = "is_using_auto_theme"
const val IS_USING_SYSTEM_THEME = "is_using_system_theme"
const val SHOULD_USE_SHARED_THEME = "should_use_shared_theme"
const val WAS_SHARED_THEME_FORCED = "was_shared_theme_forced"
const val WAS_CUSTOM_THEME_SWITCH_DESCRIPTION_SHOWN = "was_custom_theme_switch_description_shown"
const val LAST_CONFLICT_RESOLUTION = "last_conflict_resolution"
const val LAST_CONFLICT_APPLY_TO_ALL = "last_conflict_apply_to_all"
const val LAST_USED_VIEW_PAGER_PAGE = "last_used_view_pager_page"
const val USE_24_HOUR_FORMAT = "use_24_hour_format"
const val OTG_PARTITION = "otg_partition_2"
const val IS_USING_MODIFIED_APP_ICON = "is_using_modified_app_icon"
const val INITIAL_WIDGET_HEIGHT = "initial_widget_height"
const val WIDGET_ID_TO_MEASURE = "widget_id_to_measure"
const val WAS_ORANGE_ICON_CHECKED = "was_orange_icon_checked"
const val WAS_APP_ON_SD_SHOWN = "was_app_on_sd_shown"
const val WAS_APP_ICON_CUSTOMIZATION_WARNING_SHOWN = "was_app_icon_customization_warning_shown"
const val DATE_FORMAT = "date_format"
const val LAST_EXPORTED_SETTINGS_FOLDER = "last_exported_settings_folder"
const val FONT_SIZE = "font_size"
const val FAVORITES = "favorites"
const val COLOR_PICKER_RECENT_COLORS = "color_picker_recent_colors"
const val SHOW_TABS = "show_tabs"
const val LAST_EXPORT_PATH = "last_export_path"
const val VIEW_TYPE = "view_type"


// global intents
const val OPEN_DOCUMENT_TREE_FOR_ANDROID_DATA_OR_OBB = 1000
const val OPEN_DOCUMENT_TREE_OTG = 1001
const val OPEN_DOCUMENT_TREE_SD = 1002
const val OPEN_DOCUMENT_TREE_FOR_SDK_30 = 1003
const val SELECT_EXPORT_SETTINGS_FILE_INTENT = 1006
const val REQUEST_CODE_SET_DEFAULT_DIALER = 1007
const val CREATE_DOCUMENT_SDK_30 = 1008
const val REQUEST_CODE_SET_DEFAULT_CALLER_ID = 1010

// sorting
const val SORT_ORDER = "sort_order"
const val SORT_FOLDER_PREFIX = "sort_folder_"       // storing folder specific values at using "Use for this folder only"
const val SORT_BY_NAME = 1
const val SORT_BY_DATE_MODIFIED = 2
const val SORT_BY_SIZE = 4
const val SORT_BY_EXTENSION = 16
const val SORT_DESCENDING = 1024
const val SORT_USE_NUMERIC_VALUE = 32768


// permissions
const val PERMISSION_READ_STORAGE = 1
const val PERMISSION_WRITE_STORAGE = 2
const val PERMISSION_CAMERA = 3
const val PERMISSION_RECORD_AUDIO = 4
const val PERMISSION_READ_CONTACTS = 5
const val PERMISSION_WRITE_CONTACTS = 6
const val PERMISSION_READ_CALENDAR = 7
const val PERMISSION_WRITE_CALENDAR = 8
const val PERMISSION_CALL_PHONE = 9
const val PERMISSION_READ_CALL_LOG = 10
const val PERMISSION_WRITE_CALL_LOG = 11
const val PERMISSION_GET_ACCOUNTS = 12
const val PERMISSION_READ_SMS = 13
const val PERMISSION_SEND_SMS = 14
const val PERMISSION_READ_PHONE_STATE = 15
const val PERMISSION_MEDIA_LOCATION = 16
const val PERMISSION_POST_NOTIFICATIONS = 17
const val PERMISSION_READ_MEDIA_IMAGES = 18
const val PERMISSION_READ_MEDIA_VIDEO = 19
const val PERMISSION_READ_MEDIA_AUDIO = 20
const val PERMISSION_ACCESS_COARSE_LOCATION = 21
const val PERMISSION_ACCESS_FINE_LOCATION = 22
const val PERMISSION_READ_MEDIA_VISUAL_USER_SELECTED = 23
const val PERMISSION_READ_SYNC_SETTINGS = 24

// conflict resolving
const val CONFLICT_SKIP = 1
const val CONFLICT_OVERWRITE = 2
const val CONFLICT_MERGE = 3
const val CONFLICT_KEEP_BOTH = 4

// font sizes
const val FONT_SIZE_SMALL = 0
const val FONT_SIZE_MEDIUM = 1
const val FONT_SIZE_LARGE = 2

val audioExtensions: Array<String> get() = arrayOf(".mp3", ".wav", ".wma", ".ogg", ".m4a", ".opus", ".flac", ".aac", ".m4b")

val extensionsSupportingEXIF: Array<String> get() = arrayOf(".jpg", ".jpeg", ".png", ".webp", ".dng")

const val DATE_FORMAT_ONE = "dd.MM.yyyy"
const val DATE_FORMAT_TWO = "dd/MM/yyyy"
const val DATE_FORMAT_THREE = "MM/dd/yyyy"
const val DATE_FORMAT_FOUR = "yyyy-MM-dd"
const val DATE_FORMAT_FIVE = "d MMMM yyyy"
const val DATE_FORMAT_SIX = "MMMM d yyyy"
const val DATE_FORMAT_SEVEN = "MM-dd-yyyy"
const val DATE_FORMAT_EIGHT = "dd-MM-yyyy"
const val DATE_F636337ORMAT_ELEVEN = "yy-MM-dd"
const val DATE_FORMAT_TWELVE = "yyMMdd"
const val DATE_FORMAT_THIRTEEN = "yy.MM.dd"
const val DATE_FORMAT_FOURTEEN = "yy/MM/dd"

const val TIME_FORMAT_12 = "hh:mm a"
const val TIME_FORMAT_24 = "HH:mm"

// possible icons at the top left corner
enum class NavigationIcon(@StringRes val accessibilityResId: Int) {
    Cross(R.string.close),
    Arrow(R.string.back),
    None(0)
}

val appIconColorStrings = arrayListOf(
    ".Red",
    ".Pink",
    ".Purple",
    ".Deep_purple",
    ".Indigo",
    ".Blue",
    ".Light_blue",
    ".Cyan",
    ".Teal",
    ".Green",
    ".Light_green",
    ".Lime",
    ".Yellow",
    ".Amber",
    ".Orange",
    ".Deep_orange",
    ".Brown",
    ".Blue_grey",
    ".Grey_black"
)

const val VIEW_TYPE_LIST = 2

fun isOnMainThread() = Looper.myLooper() == Looper.getMainLooper()

fun ensureBackgroundThread(callback: () -> Unit) {
    if (isOnMainThread()) {
        Thread {
            callback()
        }.start()
    } else {
        callback()
    }
}

@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.N)
fun isNougatPlus() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N

@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.O)
fun isOreoPlus() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.Q)
fun isQPlus() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.R)
fun isRPlus() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.S)
fun isSPlus() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.TIRAMISU)
fun isTiramisuPlus() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
fun isUpsideDownCakePlus() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE

val normalizeRegex = "\\p{InCombiningDiacriticalMarks}+".toRegex()

fun getConflictResolution(resolutions: LinkedHashMap<String, Int>, path: String): Int {
    return if (resolutions.size == 1 && resolutions.containsKey("")) {
        resolutions[""]!!
    } else if (resolutions.containsKey(path)) {
        resolutions[path]!!
    } else {
        CONFLICT_SKIP
    }
}

fun getFilePlaceholderDrawables(context: Context): HashMap<String, Drawable> {
    val fileDrawables = HashMap<String, Drawable>()
    hashMapOf<String, Int>().apply {
        put("aep", R.drawable.ic_file_aep)
        put("ai", R.drawable.ic_file_ai)
        put("avi", R.drawable.ic_file_avi)
        put("css", R.drawable.ic_file_css)
        put("csv", R.drawable.ic_file_csv)
        put("dbf", R.drawable.ic_file_dbf)
        put("doc", R.drawable.ic_file_doc)
        put("docx", R.drawable.ic_file_doc)
        put("dwg", R.drawable.ic_file_dwg)
        put("exe", R.drawable.ic_file_exe)
        put("fla", R.drawable.ic_file_fla)
        put("flv", R.drawable.ic_file_flv)
        put("htm", R.drawable.ic_file_html)
        put("html", R.drawable.ic_file_html)
        put("ics", R.drawable.ic_file_ics)
        put("indd", R.drawable.ic_file_indd)
        put("iso", R.drawable.ic_file_iso)
        put("jpg", R.drawable.ic_file_jpg)
        put("jpeg", R.drawable.ic_file_jpg)
        put("js", R.drawable.ic_file_js)
        put("json", R.drawable.ic_file_json)
        put("m4a", R.drawable.ic_file_m4a)
        put("mp3", R.drawable.ic_file_mp3)
        put("mp4", R.drawable.ic_file_mp4)
        put("ogg", R.drawable.ic_file_ogg)
        put("pdf", R.drawable.ic_file_pdf)
        put("plproj", R.drawable.ic_file_plproj)
        put("ppt", R.drawable.ic_file_ppt)
        put("pptx", R.drawable.ic_file_ppt)
        put("prproj", R.drawable.ic_file_prproj)
        put("psd", R.drawable.ic_file_psd)
        put("rtf", R.drawable.ic_file_rtf)
        put("sesx", R.drawable.ic_file_sesx)
        put("sql", R.drawable.ic_file_sql)
        put("svg", R.drawable.ic_file_svg)
        put("txt", R.drawable.ic_file_txt)
        put("vcf", R.drawable.ic_file_vcf)
        put("wav", R.drawable.ic_file_wav)
        put("wmv", R.drawable.ic_file_wmv)
        put("xls", R.drawable.ic_file_xls)
        put("xlsx", R.drawable.ic_file_xls)
        put("xml", R.drawable.ic_file_xml)
        put("zip", R.drawable.ic_file_zip)
    }.forEach { (key, value) ->
        fileDrawables[key] = context.resources.getDrawable(value)
    }
    return fileDrawables
}


// visible fields filtering
const val SHOW_FIRST_NAME_FIELD = 2
const val SHOW_SURNAME_FIELD = 8
const val SHOW_PHONE_NUMBERS_FIELD = 32
const val SHOW_EMAILS_FIELD = 64
const val SHOW_ADDRESSES_FIELD = 128
const val SHOW_EVENTS_FIELD = 256
const val SHOW_NOTES_FIELD = 512
const val SHOW_GROUPS_FIELD = 2048
const val SHOW_CONTACT_SOURCE_FIELD = 4096

const val ON_CLICK_VIEW_CONTACT = 2
const val ON_CLICK_EDIT_CONTACT = 3

// apps with special handling
const val TELEGRAM_PACKAGE = "org.telegram.messenger"
const val SIGNAL_PACKAGE = "org.thoughtcrime.securesms"
const val WHATSAPP_PACKAGE = "com.whatsapp"
const val VIBER_PACKAGE = "com.viber.voip"
const val THREEMA_PACKAGE = "ch.threema.app"

const val SOCIAL_VOICE_CALL = 0
const val SOCIAL_VIDEO_CALL = 1
const val SOCIAL_MESSAGE = 2

fun getProperText(text: String, shouldNormalize: Boolean) = if (shouldNormalize) text.normalizeString() else text
