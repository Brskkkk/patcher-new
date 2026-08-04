package com.idk.gamecloner

import android.content.Context
import org.jf.baksmali.Baksmali
import org.jf.dexlib2.DexFileFactory
import org.jf.smali.Smali
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

class ApkPatcher(private val context: Context) {

    fun patch(inputApk: File, outputApk: File) {
        val workDir = File(context.cacheDir, "apk_work_${System.currentTimeMillis()}")
        
        try {
            workDir.mkdirs()
            log("Extracting APK...")
            extractApk(inputApk, workDir)
            
            log("Decompiling DEX...")
            val smaliDirs = decompileDex(workDir)
            
            log("Injecting hooks...")
            injectHooks(smaliDirs)
            
            log("Recompiling DEX...")
            recompileDex(workDir, smaliDirs)
            
            log("Repackaging...")
            repackageApk(workDir, outputApk)
            
            log("Signing...")
            signApk(outputApk)
            
        } finally {
            workDir.deleteRecursively()
        }
    }

    private fun extractApk(apk: File, workDir: File) {
        ZipFile(apk).use { zip ->
            for (entry in zip.entries()) {
                val file = File(workDir, entry.name)
                if (entry.isDirectory) {
                    file.mkdirs()
                } else {
                    file.parentFile?.mkdirs()
                    file.outputStream().use { out ->
                        zip.getInputStream(entry).copyTo(out)
                    }
                }
            }
        }
    }

    private fun decompileDex(workDir: File): List<File> {
        val dexFiles = workDir.walk()
            .filter { it.name.matches(Regex("classes.*\\.dex")) }
            .toList()

        val smaliDirs = mutableListOf<File>()

        for (dexFile in dexFiles) {
            val smaliDir = File(workDir, "smali_${dexFile.nameWithoutExtension}")
            smaliDir.mkdirs()
            smaliDirs.add(smaliDir)

            try {
                val dex = DexFileFactory.loadDexFile(dexFile, null)
                val options = org.jf.baksmali.BaksmaliOptions()
                options.deodex = false
                options.apiLevel = 34
                Baksmali.disassembleDexFile(dex, smaliDir, null, options)
            } catch (e: Exception) {
                log("Error decompiling: ${e.message}")
            }
        }

        return smaliDirs
    }

    private fun injectHooks(smaliDirs: List<File>) {
        for (smaliDir in smaliDirs) {
            smaliDir.walk()
                .filter { it.isFile && it.name.endsWith(".smali") }
                .forEach { smaliFile ->
                    patchSmaliFile(smaliFile)
                }
        }
    }

    private fun patchSmaliFile(smaliFile: File) {
        var content = smaliFile.readText()

        // Inject DisplayMetrics hook
        content = content.replace(
            Regex("""new-instance\s+([vp]\d+),\s*Landroid/util/DisplayMetrics;"""),
            """invoke-static {}, Lcom/idk/gamecloner/MetricsHook;->createSpoofedMetrics()Landroid/util/DisplayMetrics;
    move-result-object ${'$'}1"""
        )

        smaliFile.writeText(content)
    }

    private fun recompileDex(workDir: File, smaliDirs: List<File>) {
        for (smaliDir in smaliDirs) {
            val dexName = smaliDir.name.replace("smali_", "")
            val outputDex = File(workDir, "$dexName.dex")

            try {
                val options = org.jf.smali.SmaliOptions()
                Smali.assembleSmaliFiles(setOf(smaliDir), outputDex, null, options)
            } catch (e: Exception) {
                log("Error recompiling: ${e.message}")
            }
        }
    }

    private fun repackageApk(workDir: File, outputApk: File) {
        ZipOutputStream(outputApk.outputStream()).use { zip ->
            workDir.walk().forEach { file ->
                if (file.isFile) {
                    val relativePath = file.relativeTo(workDir).path.replace(File.separator, "/")
                    if (!relativePath.contains("smali")) {
                        val entry = ZipEntry(relativePath)
                        zip.putNextEntry(entry)
                        file.inputStream().use { it.copyTo(zip) }
                        zip.closeEntry()
                    }
                }
            }
        }
    }

    private fun signApk(apk: File) {
        // APK will be unsigned for testing
        // In production, implement jarsigner integration
    }

    private fun log(message: String) {
        println("🔧 $message")
    }
}
