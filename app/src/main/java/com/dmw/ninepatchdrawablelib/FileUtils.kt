package com.dmw.ninepatchdrawablelib;

import android.content.Context
import android.content.res.AssetManager
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


/**
 * 拷贝文件
 */
object FileUtils {


    fun copyBubbleFrameFromAssets(context: Context): Boolean {
        return try {
            val assetManager = context.assets
            val externalDir = context.getExternalFilesDir(null)
                ?: return false // Handle case where external storage is unavailable

            val sourceFolder = "bubbleframe"
            val targetPath = "${externalDir.absolutePath}/bubbleframe"

            // Create target directory if it doesn't exist
            val targetDir = File(targetPath)
            if (!targetDir.exists()) {
                targetDir.mkdirs()
            }

            // Get list of files in bubbleframe folder
            assetManager.list(sourceFolder)?.forEach { fileName ->
                copyAssetFile(assetManager, "$sourceFolder/$fileName", "$targetPath/$fileName")
            }
            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }

    private fun copyAssetFile(assetManager: AssetManager, assetPath: String, targetPath: String) {
        assetManager.open(assetPath).use { inputStream ->
            FileOutputStream(targetPath).use { outputStream ->
                val buffer = ByteArray(1024)
                var read: Int
                while (inputStream.read(buffer).also { read = it } != -1) {
                    outputStream.write(buffer, 0, read)
                }
            }
        }
    }

    fun listFiles(context: Context): List<String> {
        val externalDir = context.getExternalFilesDir(null)
            ?: return emptyList() // Return empty list if external storage is unavailable

        val targetPath = "${externalDir.absolutePath}/bubbleframe"
        val targetDir = File(targetPath)

        return if (targetDir.exists() && targetDir.isDirectory) {
            targetDir.listFiles()?.map { it.name }?.sorted() ?: emptyList()
        } else {
            emptyList()
        }
    }
}