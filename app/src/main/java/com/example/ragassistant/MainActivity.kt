
package com.example.ragassistant

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.ragassistant.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var selectedPdfUri: Uri? = null
    private var isLoading = false

    // Open the phone's PDF picker
    private val pdfPicker =
        registerForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri ->

            if (uri != null) {
                selectedPdfUri = uri

                binding.tvSelectedFile.text =
                    getFileName(uri)

                binding.tvUploadStatus.text =
                    "PDF selected. Ready to upload."

                binding.tvUploadStatus.setTextColor(
                    getColor(R.color.success)
                )
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupButtons()
        showInitialContent()
    }

    private fun setupButtons() {

        binding.btnSelectPdf.setOnClickListener {
            if (!isLoading) {
                pdfPicker.launch(arrayOf("application/pdf"))
            }
        }

        binding.btnUploadPdf.setOnClickListener {
            if (!isLoading) {
                uploadPdf()
            }
        }

        binding.btnSearch.setOnClickListener {
            if (!isLoading) {
                searchDocument()
            }
        }
    }

    private fun showInitialContent() {
        binding.tvSelectedFile.text = "No document selected"

        binding.tvUploadStatus.text = ""

        binding.tvRetrievedContent.text =
            "Search results will appear here."

        binding.tvSource.text = ""

        binding.progressBar.visibility = View.GONE
    }

    // Upload the selected PDF to FastAPI
    private fun uploadPdf() {

        val uri = selectedPdfUri

        if (uri == null) {
            Toast.makeText(
                this,
                "Please select a PDF first.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        lifecycleScope.launch {

            setLoading(true)

            binding.tvUploadStatus.text =
                "Uploading and indexing PDF..."

            try {
                val pdfFile = withContext(Dispatchers.IO) {
                    copyUriToCache(uri)
                }

                try {
                    val requestBody = pdfFile.asRequestBody(
                        "application/pdf".toMediaType()
                    )

                    val filePart =
                        MultipartBody.Part.createFormData(
                            "file",
                            pdfFile.name,
                            requestBody
                        )

                    val response =
                        RetrofitClient.apiService.uploadPdf(filePart)

                    if (response.isSuccessful) {

                        val result = response.body()

                        if (result != null) {
                            binding.tvUploadStatus.text =
                                "${result.message}\n" +
                                        "Chunks indexed: ${result.chunks}"

                            binding.tvUploadStatus.setTextColor(
                                getColor(R.color.success)
                            )
                        } else {
                            showError("The server returned an empty response.")
                        }

                    } else {
                        showError(
                            "Upload failed: HTTP ${response.code()}"
                        )
                    }

                } finally {
                    pdfFile.delete()
                }

            } catch (e: Exception) {
                showError(
                    "Upload failed: ${e.localizedMessage ?: "Check your backend connection."}"
                )
            } finally {
                setLoading(false)
            }
        }
    }

    // Ask a question about the uploaded document
    private fun searchDocument() {

        val question =
            binding.etQuestion.text?.toString()?.trim().orEmpty()

        if (question.isBlank()) {
            binding.etQuestion.error =
                "Please enter a question"
            binding.etQuestion.requestFocus()
            return
        }

        lifecycleScope.launch {

            setLoading(true)

            binding.tvRetrievedContent.text =
                "Searching relevant document content..."

            binding.tvSource.text = ""

            try {
                val response =
                    RetrofitClient.apiService.askQuestion(
                        AskRequest(question = question)
                    )

                if (response.isSuccessful) {

                    val result = response.body()

                    if (result != null) {
                        binding.tvRetrievedContent.text =
                            result.answer

                        binding.tvSource.text =
                            result.source?.takeIf { it.isNotBlank() }
                                ?.let { "Source: $it" }
                                ?: "Source information unavailable"

                    } else {
                        showSearchError(
                            "The server returned an empty response."
                        )
                    }

                } else {
                    showSearchError(
                        "Search failed: HTTP ${response.code()}"
                    )
                }

            } catch (e: Exception) {
                showSearchError(
                    "Could not connect to the backend. " +
                            (e.localizedMessage ?: "Check your network.")
                )
            } finally {
                setLoading(false)
            }
        }
    }

    // Copy the selected content URI into a temporary PDF file
    private suspend fun copyUriToCache(uri: Uri): File =
        withContext(Dispatchers.IO) {

            val fileName = getFileName(uri)
                .replace(Regex("[^a-zA-Z0-9._-]"), "_")
                .let {
                    if (it.endsWith(".pdf", ignoreCase = true)) {
                        it
                    } else {
                        "$it.pdf"
                    }
                }

            val tempFile = File.createTempFile(
                "rag_upload_",
                ".pdf",
                cacheDir
            )

            try {
                contentResolver.openInputStream(uri).use { input ->
                    if (input == null) {
                        throw IOException("Cannot open the selected PDF.")
                    }

                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                // Keep the temporary filename safe.
                // The server receives this filename during upload.
                tempFile

            } catch (e: Exception) {
                tempFile.delete()
                throw e
            }
        }

    // Get the selected PDF's display name
    private fun getFileName(uri: Uri): String {

        var name = "selected_document.pdf"

        contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->

            if (cursor.moveToFirst()) {
                val index =
                    cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)

                if (index >= 0) {
                    name = cursor.getString(index) ?: name
                }
            }
        }

        return name
    }

    private fun setLoading(loading: Boolean) {

        isLoading = loading

        binding.progressBar.visibility =
            if (loading) View.VISIBLE else View.GONE

        binding.btnSelectPdf.isEnabled = !loading
        binding.btnUploadPdf.isEnabled = !loading
        binding.btnSearch.isEnabled = !loading
    }

    private fun showError(message: String) {

        binding.tvUploadStatus.text = message

        binding.tvUploadStatus.setTextColor(
            getColor(android.R.color.holo_red_dark)
        )

        Toast.makeText(
            this,
            message,
            Toast.LENGTH_LONG
        ).show()
    }

    private fun showSearchError(message: String) {

        binding.tvRetrievedContent.text = message
        binding.tvSource.text = ""

        Toast.makeText(
            this,
            message,
            Toast.LENGTH_LONG
        ).show()
    }
}