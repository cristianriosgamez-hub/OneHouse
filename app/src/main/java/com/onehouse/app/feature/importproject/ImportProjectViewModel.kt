package com.onehouse.app.feature.importproject

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import com.onehouse.app.importer.ImportedKnxProject
import com.onehouse.app.importer.InsideControlImporter
import com.onehouse.app.importer.InsideControlProjectRepository
import java.io.Closeable
import java.util.concurrent.Executors

class ImportProjectViewModel(context: Context) : Closeable {
    private val appContext = context.applicationContext
    private val repository = InsideControlProjectRepository(appContext)
    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val observers = mutableSetOf<(Snapshot) -> Unit>()

    data class Snapshot(
        val project: ImportedKnxProject?,
        val state: ImportState
    )

    private var snapshot = Snapshot(repository.load(), ImportState.Idle)

    fun observe(observer: (Snapshot) -> Unit): Closeable {
        observers += observer
        observer(snapshot)
        return Closeable { observers -= observer }
    }

    fun beginSelection() {
        update(snapshot.copy(state = ImportState.SelectingFile))
    }

    fun selectionFailed(error: Throwable) {
        update(snapshot.copy(state = ImportState.Error(
            error.message ?: "No se pudo abrir el selector de archivos"
        )))
    }

    fun onFileSelected(contentResolver: ContentResolver, uri: Uri?) {
        if (uri == null) {
            update(snapshot.copy(state = ImportState.Idle))
            return
        }

        val fileName = queryDisplayName(contentResolver, uri) ?: "proyecto.knx"
        update(snapshot.copy(state = ImportState.Importing(fileName)))

        executor.execute {
            tryPersistReadPermission(contentResolver, uri)
            val result = InsideControlImporter.import(contentResolver, uri)
            mainHandler.post {
                when (result) {
                    is InsideControlImporter.Result.Success -> {
                        repository.save(result.project)
                        update(
                            Snapshot(
                                project = result.project,
                                state = ImportState.Success(
                                    project = result.project,
                                    fileName = fileName,
                                    message = "Proyecto importado correctamente"
                                )
                            )
                        )
                    }
                    is InsideControlImporter.Result.Failure -> {
                        update(snapshot.copy(state = ImportState.Error(result.message)))
                    }
                }
            }
        }
    }

    fun clearProject() {
        repository.clear()
        update(Snapshot(project = null, state = ImportState.Idle))
    }

    fun consumeMessage() {
        if (snapshot.state is ImportState.Success || snapshot.state is ImportState.Error) {
            update(snapshot.copy(state = ImportState.Idle))
        }
    }

    private fun tryPersistReadPermission(contentResolver: ContentResolver, uri: Uri) {
        try {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: SecurityException) {
            // Algunos proveedores (incluido el selector de ciertos Xiaomi/HyperOS)
            // solo conceden permiso temporal. La importación sigue siendo válida.
        } catch (_: UnsupportedOperationException) {
            // El proveedor no admite permisos persistentes.
        } catch (_: IllegalArgumentException) {
            // URI no persistible; se utiliza el permiso temporal del selector.
        }
    }

    private fun queryDisplayName(contentResolver: ContentResolver, uri: Uri): String? {
        var cursor: Cursor? = null
        return try {
            cursor = contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null
            )
            val index = cursor?.getColumnIndex(OpenableColumns.DISPLAY_NAME) ?: -1
            if (index >= 0 && cursor?.moveToFirst() == true) cursor?.getString(index) else null
        } catch (_: Exception) {
            null
        } finally {
            cursor?.close()
        }
    }

    private fun update(newSnapshot: Snapshot) {
        snapshot = newSnapshot
        observers.toList().forEach { it(newSnapshot) }
    }

    override fun close() {
        executor.shutdownNow()
        observers.clear()
    }
}
