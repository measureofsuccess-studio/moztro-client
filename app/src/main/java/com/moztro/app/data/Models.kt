package com.moztro.app.data

data class DiscoveredServer(
    val hostname: String,
    val ip: String,
    val wsPort: Int = 8765,
    val platform: String = "Windows",
    val mac: String? = null,
    val lastSeen: Long = System.currentTimeMillis()
)

enum class ConnectionState {
    DISCONNECTED,
    DISCOVERING,
    CONNECTING,
    PAIRING_REQUESTED,
    CONNECTED,
    FAILED,
    REJECTED
}

enum class ScreenView {
    MAIN,
    POWER_MENU,
    KEYBOARD,
    TOUCHPAD,
    SETTINGS,
    FILE_SEND,
    VOD,
    OVERDRIVE,
    ABOUT
}

enum class MouseSensitivity(val label: String, val scale: Float) {
    SLOW("1.0x", 1.0f),
    MEDIUM("1.5x", 1.5f),
    FAST("2.0x", 2.2f)
}

data class PingRecord(
    val seq: Int,
    val rttMs: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val isSuccess: Boolean = true
)

enum class TransferStatus {
    UPLOADING,
    COMPLETE,
    CANCELLED,
    FAILED
}

enum class TransferItemType {
    FILE,
    CLIPBOARD
}

enum class ClipboardDirection {
    TO_PC,    // HP -> PC (Arrow UP ↑)
    FROM_PC   // PC -> HP (Arrow DOWN ↓)
}

data class FileTransferItem(
    val id: String,
    val name: String,
    val sizeBytes: Long = 0L,
    val bytesUploaded: Long = 0L,
    val progress: Float = 0f,
    val speedFormatted: String = "0 KB/s",
    val status: TransferStatus = TransferStatus.UPLOADING,
    val itemType: TransferItemType = TransferItemType.FILE,
    val textContent: String? = null,
    val clipboardDirection: ClipboardDirection? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val localFilePath: String? = null   // path to the saved file on device (for deletion)
)

data class PcDisplaySource(
    val id: String,
    val name: String,
    val width: Int = 1920,
    val height: Int = 1080,
    val isPrimary: Boolean = false
)

enum class OverdriveTouchMode(val label: String) {
    DIRECT_TOUCH("Direct Touch"),
    TRACKPAD("Trackpad Mode")
}

enum class OverdriveQuality(val label: String, val code: String) {
    BALANCED("Balanced (720p)", "BALANCED"),
    HIGH("High (1080p)", "HIGH"),
    FAST("Fast (540p)", "FAST")
}

data class PcCursorPosition(
    val x: Float,
    val y: Float,
    val screenW: Float,
    val screenH: Float
)

