package com.github.livingwithhippos.nullseek.ui.splash

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.livingwithhippos.nullseek.R
import com.github.livingwithhippos.nullseek.ui.theme.NullSeekTheme

@Composable
fun SplashScreen(
    modifier: Modifier = Modifier
) {
    // https://developer.android.com/develop/ui/views/launch/splash-screen#suspend-drawing
    // todo: make the default splash screen longer and remove this
    //  the things to check are: token available, network available, token working
    //  and the we decide if we need to go to no network, setup, or user screen
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = painterResource(R.drawable.logo),
            contentDescription = "Logo",
            modifier = Modifier.width(140.dp)
        )
    }
}

@Preview
@Composable
fun SplashScreenPreview() {
    NullSeekTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            SplashScreen(
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}