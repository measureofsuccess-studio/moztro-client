package com.moztro.app.network

import android.os.Environment
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class FtpServerManager(
    val port: Int = 2121,
    private val rootDir: File = Environment.getExternalStorageDirectory()
) {
    companion object {
        private const val TAG = "FtpServerManager"
    }

    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    var isRunning = false
        private set

    fun start(onStarted: ((Int) -> Unit)? = null, onError: ((Exception) -> Unit)? = null) {
        if (isRunning) {
            onStarted?.invoke(port)
            return
        }

        serverJob = scope.launch {
            try {
                val ss = ServerSocket(port)
                serverSocket = ss
                isRunning = true
                Log.d(TAG, "FTP Server started on port $port, root: ${rootDir.absolutePath}")
                onStarted?.invoke(port)

                while (isActive && !ss.isClosed) {
                    try {
                        val clientSocket = ss.accept()
                        launch {
                            handleClient(clientSocket)
                        }
                    } catch (e: Exception) {
                        if (!isActive || ss.isClosed) break
                        Log.w(TAG, "Error accepting client: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start FTP Server: ${e.message}", e)
                isRunning = false
                onError?.invoke(e)
            } finally {
                stop()
            }
        }
    }

    fun stop() {
        isRunning = false
        try {
            serverSocket?.close()
        } catch (_: Exception) {}
        serverSocket = null
        serverJob?.cancel()
        serverJob = null
        Log.d(TAG, "FTP Server stopped")
    }

    private fun handleClient(clientSocket: Socket) {
        var currentDir = rootDir
        var passiveServerSocket: ServerSocket? = null
        var renameFrom: File? = null
        val dateFormat = SimpleDateFormat("MMM dd HH:mm", Locale.US).apply {
            timeZone = TimeZone.getDefault()
        }
        val mlsdFormat = SimpleDateFormat("yyyyMMddHHmmss", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        try {
            val reader = BufferedReader(InputStreamReader(clientSocket.getInputStream(), Charsets.UTF_8))
            val writer = PrintWriter(OutputStreamWriter(clientSocket.getOutputStream(), Charsets.UTF_8), true)

            writer.print("220 Moztro VOD FTP Service ready.\r\n")
            writer.flush()

            while (!clientSocket.isClosed) {
                val line = reader.readLine() ?: break
                val trimmed = line.trim()
                if (trimmed.isEmpty()) continue

                val parts = trimmed.split(" ", limit = 2)
                val cmd = parts[0].uppercase(Locale.ROOT)
                val arg = if (parts.size > 1) parts[1] else ""

                when (cmd) {
                    "USER" -> {
                        writer.print("331 Anonymous access allowed, send password.\r\n")
                        writer.flush()
                    }
                    "PASS" -> {
                        writer.print("230 User logged in, proceed.\r\n")
                        writer.flush()
                    }
                    "SYST" -> {
                        writer.print("215 UNIX Type: L8\r\n")
                        writer.flush()
                    }
                    "FEAT" -> {
                        writer.print("211-Features:\r\n UTF8\r\n SIZE\r\n MDTM\r\n PASV\r\n EPSV\r\n MLSD\r\n211 End\r\n")
                        writer.flush()
                    }
                    "OPTS" -> {
                        if (arg.uppercase(Locale.ROOT).startsWith("UTF8")) {
                            writer.print("200 UTF8 set to on\r\n")
                        } else {
                            writer.print("200 Command OK\r\n")
                        }
                        writer.flush()
                    }
                    "TYPE" -> {
                        writer.print("200 Type set to $arg\r\n")
                        writer.flush()
                    }
                    "NOOP" -> {
                        writer.print("200 OK\r\n")
                        writer.flush()
                    }
                    "PWD", "XPWD" -> {
                        val relPath = getRelativePath(rootDir, currentDir)
                        writer.print("257 \"$relPath\" is current directory.\r\n")
                        writer.flush()
                    }
                    "CWD" -> {
                        val target = resolveFile(currentDir, arg)
                        if (target.exists() && target.isDirectory && isUnderRoot(target)) {
                            currentDir = target
                            writer.print("250 Directory successfully changed to \"${getRelativePath(rootDir, currentDir)}\"\r\n")
                        } else {
                            writer.print("550 Failed to change directory.\r\n")
                        }
                        writer.flush()
                    }
                    "CDUP" -> {
                        val parent = currentDir.parentFile
                        if (parent != null && isUnderRoot(parent)) {
                            currentDir = parent
                            writer.print("250 Directory successfully changed.\r\n")
                        } else {
                            currentDir = rootDir
                            writer.print("250 Directory at root.\r\n")
                        }
                        writer.flush()
                    }
                    "PASV" -> {
                        try {
                            passiveServerSocket?.close()
                            val dataSocket = ServerSocket(0)
                            passiveServerSocket = dataSocket
                            val localAddr = clientSocket.localAddress
                            val ipBytes = localAddr.address
                            val portInt = dataSocket.localPort
                            val p1 = (portInt shr 8) and 0xFF
                            val p2 = portInt and 0xFF
                            val ipStr = "${ipBytes[0].toInt() and 0xFF},${ipBytes[1].toInt() and 0xFF},${ipBytes[2].toInt() and 0xFF},${ipBytes[3].toInt() and 0xFF}"
                            writer.print("227 Entering Passive Mode ($ipStr,$p1,$p2)\r\n")
                        } catch (e: Exception) {
                            writer.print("425 Can't open passive connection: ${e.message}\r\n")
                        }
                        writer.flush()
                    }
                    "EPSV" -> {
                        try {
                            passiveServerSocket?.close()
                            val dataSocket = ServerSocket(0)
                            passiveServerSocket = dataSocket
                            writer.print("229 Entering Extended Passive Mode (|||${dataSocket.localPort}|)\r\n")
                        } catch (e: Exception) {
                            writer.print("425 Can't open passive connection: ${e.message}\r\n")
                        }
                        writer.flush()
                    }
                    "LIST", "NLST" -> {
                        val dataServer = passiveServerSocket
                        passiveServerSocket = null
                        if (dataServer == null) {
                            writer.print("425 Use PASV or EPSV first.\r\n")
                            writer.flush()
                        } else {
                            writer.print("150 Here comes the directory listing.\r\n")
                            writer.flush()

                            try {
                                val dataSocket = dataServer.accept()
                                dataServer.close()
                                val dataOut = PrintWriter(OutputStreamWriter(dataSocket.getOutputStream(), Charsets.UTF_8), true)

                                val targetDir = if (arg.isNotEmpty() && !arg.startsWith("-")) {
                                    val candidate = resolveFile(currentDir, arg)
                                    if (candidate.isDirectory) candidate else currentDir
                                } else {
                                    currentDir
                                }

                                val files = targetDir.listFiles() ?: emptyArray()
                                for (f in files) {
                                    val isDir = f.isDirectory
                                    val typeChar = if (isDir) 'd' else '-'
                                    val perms = "${typeChar}rwxrwxrwx"
                                    val size = if (isDir) 0L else f.length()
                                    val dateStr = dateFormat.format(Date(f.lastModified()))
                                    val name = f.name
                                    dataOut.print("$perms 1 owner group $size $dateStr $name\r\n")
                                }
                                dataOut.flush()
                                dataSocket.close()
                                writer.print("226 Directory send OK.\r\n")
                            } catch (e: Exception) {
                                Log.w(TAG, "LIST error: ${e.message}")
                                writer.print("550 Failed to send directory listing: ${e.message}\r\n")
                            }
                            writer.flush()
                        }
                    }
                    "MLSD" -> {
                        val dataServer = passiveServerSocket
                        passiveServerSocket = null
                        if (dataServer == null) {
                            writer.print("425 Use PASV or EPSV first.\r\n")
                            writer.flush()
                        } else {
                            writer.print("150 Opening data connection for MLSD.\r\n")
                            writer.flush()

                            try {
                                val dataSocket = dataServer.accept()
                                dataServer.close()
                                val dataOut = PrintWriter(OutputStreamWriter(dataSocket.getOutputStream(), Charsets.UTF_8), true)

                                val files = currentDir.listFiles() ?: emptyArray()
                                for (f in files) {
                                    val type = if (f.isDirectory) "dir" else "file"
                                    val size = if (f.isDirectory) 0L else f.length()
                                    val modify = mlsdFormat.format(Date(f.lastModified()))
                                    dataOut.print("type=$type;size=$size;modify=$modify; ${f.name}\r\n")
                                }
                                dataOut.flush()
                                dataSocket.close()
                                writer.print("226 MLSD complete.\r\n")
                            } catch (e: Exception) {
                                writer.print("550 MLSD failed.\r\n")
                            }
                            writer.flush()
                        }
                    }
                    "RETR" -> {
                        val dataServer = passiveServerSocket
                        passiveServerSocket = null
                        if (dataServer == null) {
                            writer.print("425 Use PASV or EPSV first.\r\n")
                            writer.flush()
                        } else {
                            val file = resolveFile(currentDir, arg)
                            if (!file.exists() || !file.isFile || !isUnderRoot(file)) {
                                dataServer.close()
                                writer.print("550 File not found or is a directory.\r\n")
                                writer.flush()
                            } else {
                                writer.print("150 Opening BINARY mode data connection for ${file.name} (${file.length()} bytes).\r\n")
                                writer.flush()

                                try {
                                    val dataSocket = dataServer.accept()
                                    dataServer.close()
                                    val fileIn = BufferedInputStream(FileInputStream(file))
                                    val dataOut = BufferedOutputStream(dataSocket.getOutputStream())

                                    val buffer = ByteArray(65536)
                                    var bytesRead: Int
                                    while (fileIn.read(buffer).also { bytesRead = it } != -1) {
                                        dataOut.write(buffer, 0, bytesRead)
                                    }
                                    dataOut.flush()
                                    fileIn.close()
                                    dataSocket.close()
                                    writer.print("226 Transfer complete.\r\n")
                                } catch (e: Exception) {
                                    Log.w(TAG, "RETR error: ${e.message}")
                                    writer.print("426 Connection closed; transfer aborted.\r\n")
                                }
                                writer.flush()
                            }
                        }
                    }
                    "STOR" -> {
                        val dataServer = passiveServerSocket
                        passiveServerSocket = null
                        if (dataServer == null) {
                            writer.print("425 Use PASV or EPSV first.\r\n")
                            writer.flush()
                        } else {
                            val file = resolveFile(currentDir, arg)
                            if (!isUnderRoot(file)) {
                                dataServer.close()
                                writer.print("550 Permission denied.\r\n")
                                writer.flush()
                            } else {
                                writer.print("150 Ok to send data.\r\n")
                                writer.flush()

                                try {
                                    val dataSocket = dataServer.accept()
                                    dataServer.close()
                                    val dataIn = BufferedInputStream(dataSocket.getInputStream())
                                    val fileOut = BufferedOutputStream(FileOutputStream(file))

                                    val buffer = ByteArray(65536)
                                    var bytesRead: Int
                                    while (dataIn.read(buffer).also { bytesRead = it } != -1) {
                                        fileOut.write(buffer, 0, bytesRead)
                                    }
                                    fileOut.flush()
                                    fileOut.close()
                                    dataSocket.close()
                                    writer.print("226 Transfer complete.\r\n")
                                } catch (e: Exception) {
                                    Log.w(TAG, "STOR error: ${e.message}")
                                    writer.print("426 Connection closed; transfer aborted.\r\n")
                                }
                                writer.flush()
                            }
                        }
                    }
                    "DELE" -> {
                        val file = resolveFile(currentDir, arg)
                        if (file.exists() && file.isFile && isUnderRoot(file) && file.delete()) {
                            writer.print("250 File deleted successfully.\r\n")
                        } else {
                            writer.print("550 Delete failed.\r\n")
                        }
                        writer.flush()
                    }
                    "MKD", "XMKD" -> {
                        val dir = resolveFile(currentDir, arg)
                        if (isUnderRoot(dir) && (dir.exists() || dir.mkdirs())) {
                            writer.print("257 \"${getRelativePath(rootDir, dir)}\" created.\r\n")
                        } else {
                            writer.print("550 Failed to create directory.\r\n")
                        }
                        writer.flush()
                    }
                    "RMD", "XRMD" -> {
                        val dir = resolveFile(currentDir, arg)
                        if (dir.exists() && dir.isDirectory && isUnderRoot(dir) && dir.deleteRecursively()) {
                            writer.print("250 Directory removed successfully.\r\n")
                        } else {
                            writer.print("550 Failed to remove directory.\r\n")
                        }
                        writer.flush()
                    }
                    "RNFR" -> {
                        val file = resolveFile(currentDir, arg)
                        if (file.exists() && isUnderRoot(file)) {
                            renameFrom = file
                            writer.print("350 Ready for RNTO.\r\n")
                        } else {
                            writer.print("550 File not found.\r\n")
                        }
                        writer.flush()
                    }
                    "RNTO" -> {
                        val target = resolveFile(currentDir, arg)
                        val source = renameFrom
                        renameFrom = null
                        if (source != null && isUnderRoot(target) && source.renameTo(target)) {
                            writer.print("250 Rename successful.\r\n")
                        } else {
                            writer.print("550 Rename failed.\r\n")
                        }
                        writer.flush()
                    }
                    "SIZE" -> {
                        val file = resolveFile(currentDir, arg)
                        if (file.exists() && file.isFile && isUnderRoot(file)) {
                            writer.print("213 ${file.length()}\r\n")
                        } else {
                            writer.print("550 File not found.\r\n")
                        }
                        writer.flush()
                    }
                    "MDTM" -> {
                        val file = resolveFile(currentDir, arg)
                        if (file.exists() && isUnderRoot(file)) {
                            writer.print("213 ${mlsdFormat.format(Date(file.lastModified()))}\r\n")
                        } else {
                            writer.print("550 File not found.\r\n")
                        }
                        writer.flush()
                    }
                    "QUIT" -> {
                        writer.print("221 Goodbye.\r\n")
                        writer.flush()
                        break
                    }
                    else -> {
                        writer.print("502 Command not implemented.\r\n")
                        writer.flush()
                    }
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Client disconnected: ${e.message}")
        } finally {
            try {
                passiveServerSocket?.close()
                clientSocket.close()
            } catch (_: Exception) {}
        }
    }

    private fun resolveFile(currentDir: File, path: String): File {
        return if (path.startsWith("/")) {
            File(rootDir, path.removePrefix("/"))
        } else {
            File(currentDir, path).canonicalFile
        }
    }

    private fun isUnderRoot(file: File): Boolean {
        val rootPath = rootDir.canonicalPath
        val filePath = file.canonicalPath
        return filePath == rootPath || filePath.startsWith(rootPath + File.separator)
    }

    private fun getRelativePath(root: File, file: File): String {
        val rootPath = root.canonicalPath
        val filePath = file.canonicalPath
        return if (filePath == rootPath) {
            "/"
        } else if (filePath.startsWith(rootPath)) {
            val rel = filePath.substring(rootPath.length).replace(File.separatorChar, '/')
            if (rel.startsWith("/")) rel else "/$rel"
        } else {
            "/"
        }
    }
}
