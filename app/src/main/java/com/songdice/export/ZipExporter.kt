package com.songdice.export

import com.example.songdice.data.model.SongArrangement
import com.songdice.audio.MidiBuilder
import com.songdice.audio.WavSynthesizer
import com.songdice.data.model.SongBlueprint
import core.midi.model.toMidiSong
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Pure Kotlin ZipExporter for bundling Song Dice generated assets
 * (song_idea.mid, preview.wav, LeadSheet.txt, AIPrompt.txt) into a compressed .zip archive.
 */
class ZipExporter(
    private val midiBuilder: MidiBuilder = MidiBuilder(),
    private val wavSynthesizer: WavSynthesizer = WavSynthesizer(),
    private val leadSheetFormatter: LeadSheetFormatter = LeadSheetFormatter(),
    private val aiPromptFormatter: AIPromptFormatter = AIPromptFormatter()
) {

    /**
     * Compiles all 4 core assets from the blueprint and packages them into a compressed .zip byte array.
     */
    fun createZipBundle(blueprint: SongBlueprint): ByteArray {
        val arrangement = blueprint.arrangement ?: SongArrangement(
            title = blueprint.title,
            genre = blueprint.genre.displayName,
            key = blueprint.keySignature,
            bpm = blueprint.bpm,
            progression = blueprint.progression,
            tracks = emptyList()
        )

        // 1. Generate song_idea.mid
        val midiSong = arrangement.toMidiSong()
        val midiBytes = midiBuilder.buildMidiFile(midiSong)

        // 2. Generate preview.wav
        val wavBytes = wavSynthesizer.renderToWav(arrangement)

        // 3. Generate LeadSheet.txt
        val leadSheetText = leadSheetFormatter.formatLeadSheet(arrangement, blueprint.diceStates)
        val leadSheetBytes = leadSheetText.toByteArray(Charsets.UTF_8)

        // 4. Generate AIPrompt.txt
        val aiPromptText = aiPromptFormatter.formatAIPrompt(arrangement, blueprint.diceStates)
        val aiPromptBytes = aiPromptText.toByteArray(Charsets.UTF_8)

        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            writeZipEntry(zos, "song_idea.mid", midiBytes)
            writeZipEntry(zos, "preview.wav", wavBytes)
            writeZipEntry(zos, "LeadSheet.txt", leadSheetBytes)
            writeZipEntry(zos, "AIPrompt.txt", aiPromptBytes)
        }

        return baos.toByteArray()
    }

    /**
     * Generates a sanitized, standardized bundle filename format: Song_Dice_[Title]_[Key]_[BPM]BPM.zip
     */
    fun getBundleFileName(blueprint: SongBlueprint): String {
        val sanitizedTitle = sanitizePart(blueprint.title)
        val sanitizedKey = sanitizePart(blueprint.keySignature)
        val bpm = blueprint.bpm

        return "Song_Dice_${sanitizedTitle}_${sanitizedKey}_${bpm}BPM.zip"
    }

    private fun writeZipEntry(zos: ZipOutputStream, entryName: String, data: ByteArray) {
        val entry = ZipEntry(entryName)
        zos.putNextEntry(entry)
        zos.write(data)
        zos.closeEntry()
    }

    private fun sanitizePart(input: String): String {
        if (input.isBlank()) return "Untitled"
        return input
            .replace("#", "sharp")
            .replace(Regex("[\\\\/:*?\"<>|]"), "_")
            .replace(Regex("\\s+"), "_")
            .replace(Regex("_+"), "_")
            .trim('_')
    }
}
