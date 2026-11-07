package dev.inmo.navigation.sample

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier.safeDrawingPadding().padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(stringResource(R.string.choose_sample), style = MaterialTheme.typography.headlineSmall)
                        Button(onClick = {
                            startActivity(Intent(this@MainActivity, ComposeSampleActivity::class.java))
                        }) {
                            Text(stringResource(R.string.compose_sample))
                        }
                        Button(onClick = {
                            startActivity(Intent(this@MainActivity, MVVMSampleActivity::class.java))
                        }) {
                            Text(stringResource(R.string.view_fragment_sample))
                        }
                    }
                }
            }
        }
    }
}
