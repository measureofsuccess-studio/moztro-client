package com.moztro.app.data

enum class AppLanguage(val code: String, val displayName: String) {
    ENGLISH("en", "English"),
    INDONESIAN("id", "Indonesia");

    companion object {
        fun fromCode(code: String): AppLanguage {
            return entries.find { it.code.equals(code, ignoreCase = true) } ?: ENGLISH
        }
    }
}

class AppStrings(
    // Common / Navigation
    val appName: String,
    val back: String,

    // Main Menu
    val menuWakeOnLan: String,
    val menuShutdown: String,
    val menuInput: String,
    val menuKeyboard: String,
    val menuSettings: String,
    val menuFileSend: String,
    val menuVod: String,
    val menuOverdrive: String,
    val menuAbout: String,
    val menuUpdate: String,

    // Power Menu
    val powerShutdown: String,
    val powerRestart: String,
    val powerSleep: String,

    // Settings Screen
    val settingsSectionLanguage: String,
    val settingsLanguageTitle: String,
    val settingsLanguageDesc: String,
    val settingsSectionPointer: String,
    val settingsPointerSpeed: String,
    val settingsPointerSpeedDesc: String,
    val settingsScrollSpeed: String,
    val settingsScrollSpeedDesc: String,
    val settingsLinesSuffix: String,
    val settingsSectionDisplay: String,
    val settingsFullscreen: String,
    val settingsFullscreenDesc: String,
    val settingsSectionStorage: String,
    val settingsDeleteHistory: String,
    val settingsDeleteHistoryDesc: String,
    val settingsDefaultStorage: String,
    val settingsDefaultStorageDesc: String,
    val settingsChangeButton: String,
    val settingsSectionVod: String,
    val settingsVodDefault: String,
    val settingsVodDefaultDesc: String,
    val settingsSectionOverdrive: String,
    val settingsTouchMode: String,
    val settingsTouchModeDesc: String,
    val touchModeDirect: String,
    val touchModeTrackpad: String,
    val settingsStreamQuality: String,
    val settingsStreamQualityDesc: String,
    val qualityHigh: String,
    val qualityFast: String,
    val settingsStreamAudio: String,
    val settingsStreamAudioDesc: String,

    // File Send Screen
    val fileSendSelectAll: String,
    val fileSendSelectedCount: String,
    val fileSendClear: String,
    val fileSendSendClipboard: String,
    val fileSendSelectFile: String,
    val fileSendPasteClipboard: String,
    val fileSendSentToPc: String,
    val fileSendCopiedToPhone: String,
    val fileSendFailed: String,
    val fileSendCancelled: String,
    val fileSendPending: String,
    val fileSendComplete: String,

    // VOD Screen
    val vodStatus: String,
    val vodConnectedTo: String,
    val vodNotConnected: String,
    val vodDesc: String,
    val vodFtpAddress: String,
    val vodTurnOff: String,
    val vodTurnOn: String,

    // Overdrive Screen
    val overdriveStarting: String,
    val overdriveNotConnected: String,
    val overdriveDescConnected: String,
    val overdriveDescDisconnected: String,
    val overdriveConnectButton: String,
    val overdriveEnter: String,

    // Touchpad Screen
    val touchpadTouchSurface: String,
    val touchpadDragging: String,
    val touchpadTapToType: String,

    // About Screen
    val aboutSubtitle: String,
    val aboutTitle: String,
    val aboutDescription: String,
    val aboutMainFeatures: String,
    val featureOverdriveTitle: String,
    val featureOverdriveDesc: String,
    val featureFastFileTitle: String,
    val featureFastFileDesc: String,
    val featureVodTitle: String,
    val featureVodDesc: String,
    val featureRemoteTitle: String,
    val featureRemoteDesc: String,
    val aboutDeveloperTitle: String,
    val aboutCreatedBy: String,
    val aboutBrandProject: String,
    val aboutConnectTitle: String,
    val aboutEmailCard: String,
    val aboutEmailCopied: String,
    val aboutInstagramCard: String,
    val aboutCopyright: String,
    val aboutFooter: String,

    // Update Screen
    val updateNewVersionAvailable: String,
    val updateCurrentVersion: String,
    val updateNewVersion: String,
    val updateWhatsNew: String,
    val updateNoReleaseNotes: String,
    val updateReadyToInstall: String,
    val updateDownloadBtn: String,
    val updateDownloading: String,
    val updateVerifying: String,

    // Notifications / Toasts
    val toastSendingFiles: String,
    val toastNotConnected: String,
    val toastSendingProgress: String,
    val toastCopiedClipboard: String
) {
    companion object {
        val English = AppStrings(
            appName = "Moztro",
            back = "Back",

            // Main Menu
            menuWakeOnLan = "Wake On Lan",
            menuShutdown = "Shutdown",
            menuInput = "Input",
            menuKeyboard = "Keyboard",
            menuSettings = "Settings",
            menuFileSend = "File Send",
            menuVod = "VOD",
            menuOverdrive = "Overdrive",
            menuAbout = "About",
            menuUpdate = "Update",

            // Power Menu
            powerShutdown = "Shutdown",
            powerRestart = "Restart",
            powerSleep = "Sleep",

            // Settings Screen
            settingsSectionLanguage = "LANGUAGE",
            settingsLanguageTitle = "App Language",
            settingsLanguageDesc = "Select the interface language for the application",
            settingsSectionPointer = "POINTER & TOUCHPAD",
            settingsPointerSpeed = "Pointer Speed",
            settingsPointerSpeedDesc = "Cursor movement speed in Input menu",
            settingsScrollSpeed = "Scroll Speed",
            settingsScrollSpeedDesc = "Vertical scroll lines per gesture",
            settingsLinesSuffix = "lines",
            settingsSectionDisplay = "DISPLAY & SYSTEM",
            settingsFullscreen = "Fullscreen Mode",
            settingsFullscreenDesc = "Hide status bar & navigation bar",
            settingsSectionStorage = "FILE TRANSFER STORAGE",
            settingsDeleteHistory = "Delete original files when clearing history",
            settingsDeleteHistoryDesc = "Deleting history permanently removes the original files from storage.",
            settingsDefaultStorage = "Default Storage Directory",
            settingsDefaultStorageDesc = "Auto-categorizes into Document, Image, Music, Video",
            settingsChangeButton = "Change",
            settingsSectionVod = "VIEW ON DEVICE (VOD)",
            settingsVodDefault = "View On Device (VOD) is set to 'On' by default.",
            settingsVodDefaultDesc = "Automatically starts VOD FTP storage sharing when connected to PC.",
            settingsSectionOverdrive = "OVERDRIVE (SCREEN MIRRORING)",
            settingsTouchMode = "Touch Control Mode",
            settingsTouchModeDesc = "Direct Touch maps screen taps directly to absolute PC cursor position. Trackpad mode uses relative swipe gesture control.",
            touchModeDirect = "Direct Touch",
            touchModeTrackpad = "Trackpad Mode",
            settingsStreamQuality = "Streaming Quality",
            settingsStreamQualityDesc = "Configure mirror resolution and frame rate for optimal performance.",
            qualityHigh = "High (1080p 60fps)",
            qualityFast = "Fast (540p 60fps)",
            settingsStreamAudio = "Stream PC System Audio",
            settingsStreamAudioDesc = "Play live audio from your PC through your phone speaker while mirroring.",

            // File Send Screen
            fileSendSelectAll = "Select All",
            fileSendSelectedCount = "%d selected",
            fileSendClear = "Clear (%d)",
            fileSendSendClipboard = "Send\nClipboard",
            fileSendSelectFile = "Select\nFile",
            fileSendPasteClipboard = "Paste\nClipboard",
            fileSendSentToPc = "Sent to PC",
            fileSendCopiedToPhone = "Copied to Phone",
            fileSendFailed = "Failed",
            fileSendCancelled = "Cancelled",
            fileSendPending = "Pending",
            fileSendComplete = "Complete",

            // VOD Screen
            vodStatus = "STATUS",
            vodConnectedTo = "Connected to %s",
            vodNotConnected = "Not Connected",
            vodDesc = "Access and manage local device storage directly via PC.",
            vodFtpAddress = "FTP ADDRESS",
            vodTurnOff = "Turn Off",
            vodTurnOn = "Turn On",

            // Overdrive Screen
            overdriveStarting = "Starting Overdrive Stream...",
            overdriveNotConnected = "Not Connected to PC",
            overdriveDescConnected = "Mirroring PC screen & system audio in real-time",
            overdriveDescDisconnected = "Connect to PC from the Home screen to start screen mirroring.",
            overdriveConnectButton = "CONNECT TO PC",
            overdriveEnter = "Enter",

            // Touchpad Screen
            touchpadTouchSurface = "TOUCH SURFACE",
            touchpadDragging = "DRAGGING...",
            touchpadTapToType = "TAP TO TYPE",

            // About Screen
            aboutSubtitle = "Universal PC-Android Seamless Bridge",
            aboutTitle = "ABOUT THE APP",
            aboutDescription = "Moztro is a high-speed local wireless data exchange and control ecosystem connecting your Android device and Windows PC instantly without requiring an internet or cloud connection.",
            aboutMainFeatures = "MAIN FEATURES",
            featureOverdriveTitle = "Overdrive",
            featureOverdriveDesc = "Real-time PC screen mirroring with high-precision touch & multi-gesture trackpad control.",
            featureFastFileTitle = "Fast File Transfer",
            featureFastFileDesc = "Unlimited size file & folder transfer via local high-speed WebSocket & HTTP server.",
            featureVodTitle = "View On Device (VOD)",
            featureVodDesc = "Access and manage entire Android storage directly from PC File Explorer via FTP.",
            featureRemoteTitle = "Remote Control & Power Suite",
            featureRemoteDesc = "Control media, keyboard, touchpad, clipboard sync, to PC remote shutdown/restart.",
            aboutDeveloperTitle = "DEVELOPER & CREATOR",
            aboutCreatedBy = "Created & Developed by:",
            aboutBrandProject = "Brand / Project:",
            aboutConnectTitle = "CONNECT & CONTACT",
            aboutEmailCard = "EMAIL (TAP TO COPY / EMAIL)",
            aboutEmailCopied = "Email copied to clipboard",
            aboutInstagramCard = "INSTAGRAM",
            aboutCopyright = "© 2026 Akbar Dwi Mulya (Measure of Success). All rights reserved.",
            aboutFooter = "Built for seamless local productivity and maximum device interoperability.",

            // Update Screen
            updateNewVersionAvailable = "NEW VERSION AVAILABLE",
            updateCurrentVersion = "CURRENT VERSION",
            updateNewVersion = "NEW VERSION",
            updateWhatsNew = "WHAT'S NEW IN THIS RELEASE",
            updateNoReleaseNotes = "No release notes available.",
            updateReadyToInstall = "Ready To Install",
            updateDownloadBtn = "Download %s",
            updateDownloading = "Downloading...",
            updateVerifying = "Verifying package...",

            // Notifications / Toasts
            toastSendingFiles = "Sending %d file(s)...",
            toastNotConnected = "Not connected to PC",
            toastSendingProgress = "Sending (%d/%d): %s",
            toastCopiedClipboard = "Copied to clipboard"
        )

        val Indonesian = AppStrings(
            appName = "Moztro",
            back = "Kembali",

            // Main Menu
            menuWakeOnLan = "Wake On Lan",
            menuShutdown = "Matikan PC",
            menuInput = "Input",
            menuKeyboard = "Keyboard",
            menuSettings = "Pengaturan",
            menuFileSend = "Kirim Berkas",
            menuVod = "VOD",
            menuOverdrive = "Overdrive",
            menuAbout = "Tentang",
            menuUpdate = "Pembaruan",

            // Power Menu
            powerShutdown = "Matikan",
            powerRestart = "Mulai Ulang",
            powerSleep = "Tidur",

            // Settings Screen
            settingsSectionLanguage = "PILIHAN BAHASA",
            settingsLanguageTitle = "Bahasa Aplikasi",
            settingsLanguageDesc = "Pilih bahasa antarmuka untuk aplikasi",
            settingsSectionPointer = "PENUNJUK & TOUCHPAD",
            settingsPointerSpeed = "Kecepatan Penunjuk",
            settingsPointerSpeedDesc = "Kecepatan pergerakan kursor di menu Input",
            settingsScrollSpeed = "Kecepatan Gulir",
            settingsScrollSpeedDesc = "Jumlah baris gulir vertikal per gestur",
            settingsLinesSuffix = "baris",
            settingsSectionDisplay = "TAMPILAN & SISTEM",
            settingsFullscreen = "Mode Layar Penuh",
            settingsFullscreenDesc = "Sembunyikan bilah status & bilah navigasi",
            settingsSectionStorage = "PENYIMPANAN TRANSFER BERKAS",
            settingsDeleteHistory = "Hapus berkas asli saat membersihkan riwayat",
            settingsDeleteHistoryDesc = "Menghapus riwayat akan menghapus berkas asli secara permanen dari penyimpanan.",
            settingsDefaultStorage = "Direktori Penyimpanan Default",
            settingsDefaultStorageDesc = "Mengelompokkan otomatis ke Dokumen, Gambar, Musik, Video",
            settingsChangeButton = "Ubah",
            settingsSectionVod = "VIEW ON DEVICE (VOD)",
            settingsVodDefault = "View On Device (VOD) diatur 'Aktif' secara default.",
            settingsVodDefaultDesc = "Memulai berbagi penyimpanan FTP VOD secara otomatis saat terhubung ke PC.",
            settingsSectionOverdrive = "OVERDRIVE (PENCERMINAN LAYAR)",
            settingsTouchMode = "Mode Kontrol Sentuh",
            settingsTouchModeDesc = "Sentuhan Langsung memetakan ketukan layar langsung ke posisi absolut kursor PC. Mode Trackpad menggunakan gestur usap relatif.",
            touchModeDirect = "Sentuhan Langsung",
            touchModeTrackpad = "Mode Trackpad",
            settingsStreamQuality = "Kualitas Streaming",
            settingsStreamQualityDesc = "Konfigurasi resolusi pencerminan dan frame rate untuk performa optimal.",
            qualityHigh = "Tinggi (1080p 60fps)",
            qualityFast = "Cepat (540p 60fps)",
            settingsStreamAudio = "Streaming Audio Sistem PC",
            settingsStreamAudioDesc = "Putar audio langsung dari PC melalui speaker ponsel saat mencerminkan layar.",

            // File Send Screen
            fileSendSelectAll = "Pilih Semua",
            fileSendSelectedCount = "%d dipilih",
            fileSendClear = "Bersihkan (%d)",
            fileSendSendClipboard = "Kirim\nPapan Klip",
            fileSendSelectFile = "Pilih\nBerkas",
            fileSendPasteClipboard = "Tempel\nPapan Klip",
            fileSendSentToPc = "Terkirim ke PC",
            fileSendCopiedToPhone = "Disalin ke Ponsel",
            fileSendFailed = "Gagal",
            fileSendCancelled = "Dibatalkan",
            fileSendPending = "Tertunda",
            fileSendComplete = "Selesai",

            // VOD Screen
            vodStatus = "STATUS",
            vodConnectedTo = "Terhubung ke %s",
            vodNotConnected = "Tidak Terhubung",
            vodDesc = "Akses dan kelola penyimpanan perangkat lokal langsung melalui PC.",
            vodFtpAddress = "ALAMAT FTP",
            vodTurnOff = "Matikan",
            vodTurnOn = "Aktifkan",

            // Overdrive Screen
            overdriveStarting = "Memulai Streaming Overdrive...",
            overdriveNotConnected = "Tidak Terhubung ke PC",
            overdriveDescConnected = "Mencerminkan layar PC & audio sistem secara real-time",
            overdriveDescDisconnected = "Hubungkan ke PC dari layar Utama untuk memulai pencerminan layar.",
            overdriveConnectButton = "HUBUNGKAN KE PC",
            overdriveEnter = "Enter",

            // Touchpad Screen
            touchpadTouchSurface = "AREA SENTUH",
            touchpadDragging = "MENYERET...",
            touchpadTapToType = "KETUK UNTUK MENGETIK",

            // About Screen
            aboutSubtitle = "Jembatan Mulus Universal PC-Android",
            aboutTitle = "TENTANG APLIKASI",
            aboutDescription = "Moztro adalah ekosistem kontrol dan pertukaran data lokal nirkabel berkecepatan tinggi yang menghubungkan perangkat Android dan PC Windows Anda secara instan tanpa memerlukan koneksi internet/cloud.",
            aboutMainFeatures = "FITUR UTAMA",
            featureOverdriveTitle = "Overdrive",
            featureOverdriveDesc = "Real-time PC screen mirroring dengan kontrol touch & multi-gesture trackpad presisi tinggi.",
            featureFastFileTitle = "Transfer Berkas Cepat",
            featureFastFileDesc = "Pengiriman file & folder tanpa batas ukuran via local high-speed WebSocket & HTTP server.",
            featureVodTitle = "View On Device (VOD)",
            featureVodDesc = "Akses dan kelola seluruh penyimpanan Android langsung dari PC File Explorer via FTP.",
            featureRemoteTitle = "Suite Kontrol Jarak Jauh & Daya",
            featureRemoteDesc = "Kontrol media, keyboard, touchpad, clipboard sync, hingga shutdown/restart PC jarak jauh.",
            aboutDeveloperTitle = "PENGEMBANG & PENCIPTA",
            aboutCreatedBy = "Dibuat & Dikembangkan oleh:",
            aboutBrandProject = "Brand / Proyek:",
            aboutConnectTitle = "HUBUNGI & KONTAK",
            aboutEmailCard = "EMAIL (KETUK UNTUK SALIN / KIRIM)",
            aboutEmailCopied = "Email disalin ke papan klip",
            aboutInstagramCard = "INSTAGRAM",
            aboutCopyright = "© 2026 Akbar Dwi Mulya (Measure of Success). Seluruh hak cipta dilindungi.",
            aboutFooter = "Dibuat untuk produktivitas lokal yang mulus dan interoperabilitas perangkat maksimal.",

            // Update Screen
            updateNewVersionAvailable = "VERSI BARU TERSEDIA",
            updateCurrentVersion = "VERSI SAAT INI",
            updateNewVersion = "VERSI BARU",
            updateWhatsNew = "YANG BARU DI RILIS INI",
            updateNoReleaseNotes = "Tidak ada catatan rilis.",
            updateReadyToInstall = "Siap Diinstal",
            updateDownloadBtn = "Unduh %s",
            updateDownloading = "Mengunduh...",
            updateVerifying = "Memverifikasi paket...",

            // Notifications / Toasts
            toastSendingFiles = "Mengirim %d berkas...",
            toastNotConnected = "Tidak terhubung ke PC",
            toastSendingProgress = "Mengirim (%d/%d): %s",
            toastCopiedClipboard = "Disalin ke papan klip"
        )

        fun forLanguage(language: AppLanguage): AppStrings {
            return when (language) {
                AppLanguage.ENGLISH -> English
                AppLanguage.INDONESIAN -> Indonesian
            }
        }
    }
}
