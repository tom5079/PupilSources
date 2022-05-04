package xyz.quaver.pupil.sources.manatoki.composable

import android.widget.EditText
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.rememberImagePainter
import kotlinx.coroutines.launch
import org.kodein.di.compose.rememberInstance
import xyz.quaver.pupil.sources.manatoki.networking.ManatokiCaptcha
import xyz.quaver.pupil.sources.manatoki.networking.ManatokiHttpClient
import java.nio.ByteBuffer

@Composable
fun CaptchaDialog() {
    val client: ManatokiHttpClient by rememberInstance()
    val captchaRequested by ManatokiCaptcha.captchaRequestFlow.collectAsState()

    val coroutineScope = rememberCoroutineScope()

    var captcha: ByteBuffer? by remember { mutableStateOf(null) }
    var key: String by remember { mutableStateOf("") }

    fun loadCaptcha() {
        coroutineScope.launch {
            captcha = client.reloadCaptcha()
        }
    }

    fun checkCaptcha() {
        coroutineScope.launch {
            val result = client.checkCaptcha(key)
            key = ""

            if (result == true) {
                ManatokiCaptcha.resume()
            } else {
                loadCaptcha()
            }
        }
    }

    LaunchedEffect(captchaRequested) {
        if (captchaRequested) loadCaptcha()
    }

    if (captchaRequested) {
        Dialog(onDismissRequest = { }) {
            Card {
                Column(Modifier.padding(8.dp).fillMaxWidth()) {
                    Text("캡챠 인증!", style = MaterialTheme.typography.h6)

                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                    ) {
                        if (captcha != null) {
                            Image(
                                painter = rememberImagePainter(captcha),
                                contentDescription = "captcha",
                                modifier = Modifier
                                    .size(160.dp, 60.dp)
                                    .align(Alignment.Center)
                            )
                        } else {
                            CircularProgressIndicator()
                        }
                    }

                    TextField(
                        key,
                        onValueChange = { key = it }
                    )

                    TextButton(
                        modifier = Modifier.align(Alignment.End),
                        onClick = { checkCaptcha() }
                    ) {
                        Text("SUBMIT")
                    }
                }
            }
        }
    }
}