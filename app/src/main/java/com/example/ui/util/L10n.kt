package com.example.ui.util

enum class AppLanguage(val code: String, val displayNameFa: String, val displayNameEn: String) {
    PERSIAN("fa", "فارسی (Persian)", "فارسی (Persian)"),
    ENGLISH("en", "English", "English")
}

enum class AppThemeMode(val code: String, val displayNameFa: String, val displayNameEn: String) {
    SYSTEM("system", "خودکار سیستم", "System Default"),
    DARK("dark", "تاریک", "Dark Mode"),
    LIGHT("light", "روشن", "Light Mode")
}

class UiStrings(val lang: AppLanguage) {
    val isFa = lang == AppLanguage.PERSIAN

    val themeModeSetting = if (isFa) "پوسته و تم برنامه" else "App Theme Mode"
    val themeModeDesc = if (isFa) "انتخاب حالت نمایش (خودکار مطابق سیستم، تاریک یا روشن):" else "Select display mode (System Auto, Dark, or Light):"

    val libraryTab = if (isFa) "آرشیو" else "Library"
    val foldersTab = if (isFa) "پوشه‌ها" else "Folders"
    val smartAiTab = if (isFa) "هوش مصنوعی" else "Smart AI"
    val settingsTab = if (isFa) "تنظیمات" else "Settings"

    val appTitle = "myFILMlist"
    val scan = if (isFa) "اسکن" else "Scan"
    val rescan = if (isFa) "اسکن مجدد" else "Rescan"
    val smartGroupingBannerTitle = if (isFa) "ادغام و دسته‌بندی هوشمند با Gemini AI" else "Smart Grouping with Gemini AI"
    val smartGroupingBannerSub = if (isFa) "سازماندهی خودکار قسمت‌ها تحت یک مجموعه" else "Auto-group episodes into unified collections"
    val smartGroupingBtn = if (isFa) "ادغام هوشمند" else "Smart Group"
    val collectionsAndTitles = if (isFa) "مجموعه‌ها و عناوین" else "Collections & Titles"
    val noVideoFound = if (isFa) "هیچ فایل ویدیویی یافت نشد" else "No video files found"
    val scanNotice = if (isFa) "برای اسکن ویدیوهای واقعی موجود در حافظه گوشی، دکمه‌های زیر را استفاده کنید:" else "To scan video files from your device storage, use the buttons below:"
    val scanStorage = if (isFa) "اسکن حافظه دستگاه" else "Scan Device Storage"
    val selectFolder = if (isFa) "انتخاب پوشه مشخص ویدیو" else "Select Specific Folder"
    val batchSelect = if (isFa) "انتخاب چندتایی" else "Multi-select"
    val selectAll = if (isFa) "همه" else "Select All"
    val cancel = if (isFa) "لغو" else "Cancel"
    fun selectedCount(count: Int) = if (isFa) "$count فایل انتخاب شد" else "$count files selected"
    val fixWithGemini = if (isFa) "اصلاح با Gemini" else "Fix with Gemini"
    val analyzing = if (isFa) "در حال تحلیل..." else "Analyzing..."

    val storageAndFolders = if (isFa) "مدیریت حافظه و پوشه‌ها" else "Storage & Folders"
    fun scannedFilesCount(count: Int) = if (isFa) "$count فایل اسکن شده" else "$count files scanned"
    val storageBreakdown = if (isFa) "آمار حجم فایل‌ها" else "Media Storage Breakdown"
    fun totalIndexed(size: String) = if (isFa) "مجموع حجم شناسایی شده: $size" else "Total Indexed: $size"
    val anime = if (isFa) "انیمه" else "Anime"
    val movies = if (isFa) "فیلم" else "Movies"
    val series = if (isFa) "سریال" else "Series"
    val scannedFilesList = if (isFa) "لیست فایل‌های اسکن شده" else "Scanned Files List"

    val geminiAssistant = if (isFa) "دستیار هوش مصنوعی Gemini" else "Gemini AI Assistant"
    fun geminiSubtitle(metaLang: String) = if (isFa) "بازسازی متادیتا و موتور پیشنهاد دهنده ($metaLang)" else "AI Metadata Restorer & Recommendations ($metaLang)"
    val batchGroupTitle = if (isFa) "گروه‌بندی هوشمند خودکار تمامی فایل‌های حافظه" else "Smart Auto-Clustering of All Files"
    val batchGroupDesc = if (isFa) "هوش مصنوعی تمامی قسمت‌های انیمه و فیلم را در حافظه بررسی کرده، نام سایت‌های دانلود را حذف می‌کند و قسمت‌های پراکنده را پس از پیش‌نمایش و تایید شما تحت یک مجموعه یکپارچه دسته‌بندی می‌کند." else "Gemini AI scans all files, removes download site tags, and groups scattered episodes under unified collections after your preview and approval."
    val askGemini = if (isFa) "پرسش مستقیم از Gemini" else "Ask Gemini Directly"
    val sendQuery = if (isFa) "ارسال پرسش به Gemini" else "Ask Gemini"
    val aiCapabilitiesTitle = if (isFa) "✨ قابلیت‌های Gemini AI برای آرشیو شما:" else "✨ What Gemini AI can do for your library:"
    val aiCapabilitiesText = if (isFa) "• پاکسازی واترمارک و نام سایت‌های دانلود از اسم فایل‌ها\n• دریافت خلاصه داستان رسمی از AniList و TMDB\n• ترجمه و معادل‌سازی عناوین فیلم‌ها و انیمه‌ها به فارسی/انگلیسی\n• پیشنهاد عناوین مشابه بر اساس ژانر و تم داستانی" else "• Strip scene releases, download site watermarks & tags\n• Retrieve official AniList & TMDB plot summaries\n• Multi-language translations for anime & movie titles\n• Smart recommendations based on genres and themes"
    val aiResponse = if (isFa) "پاسخ هوش مصنوعی" else "AI Response"

    val appSettings = if (isFa) "تنظیمات برنامه" else "App Settings"
    val settingsSubtitle = if (isFa) "زبان رابط کاربری، کلید Gemini API و حافظه کش" else "UI Language, Gemini API Key & Library Cache"
    val uiLanguageSetting = if (isFa) "زبان برنامه (UI Language)" else "UI Language"
    val uiLanguageDesc = if (isFa) "زبان منوها و بخش‌های مختلف نرم‌افزار را انتخاب کنید:" else "Select preferred language for app interfaces and menus:"
    val apiKeyTitle = if (isFa) "کلید Google Gemini API" else "Google Gemini API Key"
    val apiKeyActive = if (isFa) "کلید فعال است" else "API Key Active"
    val apiKeyDesc = if (isFa) "برای استفاده مستقیم از هوش مصنوعی Gemini در تحلیل نام پوشه‌ها، گروه‌بندی انیمه‌ها و خلاصه‌نویسی، کلید API خود را وارد کنید و دکمه تست را فشار دهید:" else "Enter your Gemini API key to use direct AI capabilities for folder parsing, grouping, and plot summaries:"
    val testAndSave = if (isFa) "تست و ذخیره کلید" else "Test & Save Key"
    val clear = if (isFa) "پاک کردن" else "Clear"
    val metadataLangSetting = if (isFa) "زبان متادیتا و ترجمه خلاصه‌ها" else "Metadata & Plot Language"
    val metadataLangDesc = if (isFa) "زبان مورد نظر برای ترجمه خلاصه‌ها و دریافت پیشنهادها را انتخاب کنید:" else "Select preferred language for plot synopses and recommendations:"
    val clearDatabase = if (isFa) "پاکسازی دیتابیس" else "Clear Cache / DB"

    val episodes = if (isFa) "قسمت‌ها" else "Episodes"
    val playVideo = if (isFa) "پخش ویدیو" else "PLAY VIDEO"
    val videoPlayer = if (isFa) "پخش‌کننده ویدیو" else "External Player"
    val systemDefault = if (isFa) "پیش‌فرض سیستم" else "System Chooser"
    val technicalDetails = if (isFa) "مشخصات فنی فایل" else "Technical File Details"
    val filename = if (isFa) "نام فایل" else "Filename"
    val size = if (isFa) "حجم" else "Size"
    val quality = if (isFa) "کیفیت" else "Quality"
    val synopsis = if (isFa) "خلاصه داستان" else "Synopsis"
    val noSynopsis = if (isFa) "توضیحات مفصلی برای این فایل ثبت نشده است." else "No detailed description available."
    val recommendationsTitle = if (isFa) "پیشنهادهای هوشمند" else "Smart Recommendations"
    val markWatched = if (isFa) "نشانه دیده‌شده" else "Mark Watched"
    val watched = if (isFa) "دیده‌شده" else "Watched"
    val searchPlaceholder = if (isFa) "جستجوی فیلم، انیمه، سریال یا ژانر..." else "Search movies, anime, series or genres..."
    val filterAll = if (isFa) "همه" else "All"
    val filterAnime = if (isFa) "انیمه" else "Anime"
    val filterMovie = if (isFa) "فیلم" else "Movie"
    val filterSeries = if (isFa) "سریال" else "Series"
}
