package com.namnp.workmanager

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import coil.compose.rememberImagePainter
import com.namnp.workmanager.ui.theme.WorkManagerTheme
import java.time.Duration

class MainActivity : ComponentActivity() {
    @SuppressLint("UnrememberedMutableState")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

//        val downloadRequest = PeriodicWorkRequestBuilder<DownloadImageWorker>(
//            Duration.ofHours(5)
//        )
        val downloadRequest = OneTimeWorkRequestBuilder<DownloadImageWorker>()
            .setConstraints(
                Constraints.Builder()
//                    .setRequiresStorageNotLow(true)
//                    .setRequiresBatteryNotLow(true)
//                    .setRequiresCharging(true)
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
//            .setInitialDelay()
//            .setScheduleRequestedAt()
            .build()

        val colorFilterRequest = OneTimeWorkRequestBuilder<ImageColorFilterWorker>()
            .build()

        val workerManager = WorkManager.getInstance(applicationContext)

        setContent {
            WorkManagerTheme {
                val workInfos = workerManager
                    .getWorkInfosForUniqueWorkLiveData(WorkerImageKeys.DOWNLOAD_IMAGE_WORKER_ID)
                    .observeAsState() // in Compose, use Coroutine instead of Live Data
                    .value

                val downloadInfo = remember(key1 = workInfos) {
                    workInfos?.find {
                        it.id == downloadRequest.id
                    }
                }

                val filterImageInfo = remember(key1 = workInfos) {
                    workInfos?.find {
                        it.id == colorFilterRequest.id
                    }
                }

                val imageUri by derivedStateOf { // derivedStateOf: cache the result of the block
                    val downloadUri = downloadInfo?.outputData?.getString(WorkerImageKeys.IMAGE_URI)
                        ?.toUri()
                    val filterUri = filterImageInfo?.outputData?.getString(WorkerImageKeys.FILTERED_URI)
                        ?.toUri()
                    filterUri ?: downloadUri
                }

                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    imageUri?.let { uri ->
                        Image(
                            painter = rememberImagePainter(
                                data = uri
                            ),
                            contentDescription = null,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    Button(
                        onClick = {
                            workerManager
                                // 1
                                .beginUniqueWork(
                                    WorkerImageKeys.DOWNLOAD_IMAGE_WORKER_ID,
                                    ExistingWorkPolicy.KEEP, // if existing worker instance with the same ID -> skip the second
                                    // ExistingWorkPolicy.REPLACE: to replace the existing one
                                    downloadRequest
                                )
//                                .cancelAllWork()
//                                .cancelUniqueWork("ID")
                                // 2
                                .then(colorFilterRequest)
                                .enqueue() // DONE
                        },
                        enabled = downloadInfo?.state != WorkInfo.State.RUNNING
                    ) {
                        Text(text = "Start download image")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    when(downloadInfo?.state) {
                        WorkInfo.State.RUNNING -> Text("Downloading...")
                        WorkInfo.State.SUCCEEDED -> Text("Download succeeded")
                        WorkInfo.State.FAILED -> Text("Download failed")
                        WorkInfo.State.CANCELLED -> Text("Download cancelled")
                        WorkInfo.State.ENQUEUED -> Text("Download enqueued")
                        WorkInfo.State.BLOCKED -> Text("Download blocked")
                        else -> {}
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    when(filterImageInfo?.state) {
                        WorkInfo.State.RUNNING -> Text("Applying filter...")
                        WorkInfo.State.SUCCEEDED -> Text("Filter succeeded")
                        WorkInfo.State.FAILED -> Text("Filter failed")
                        WorkInfo.State.CANCELLED -> Text("Filter cancelled")
                        WorkInfo.State.ENQUEUED -> Text("Filter enqueued")
                        WorkInfo.State.BLOCKED -> Text("Filter blocked")
                        else -> {}
                    }
                }
            }
        }
    }
}