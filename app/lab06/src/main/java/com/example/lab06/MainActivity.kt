package com.example.lab06

import ParliamentMembersData
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.lab06.ui.theme.Andriod_LabsTheme
import coil.request.ImageRequest

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Andriod_LabsTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ParliamentMembersList(members = ParliamentMembersData.members, modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Composable
fun ParliamentMembersList(members: List<ParliamentMember>, modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier.padding(16.dp)) {
        items(members.size) { index ->
            ParliamentMemberItem(member = members[index])
        }
    }
}

@Composable
fun ParliamentMemberItem(member: ParliamentMember) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Image(
            painter = // Placeholder while loading
            rememberAsyncImagePainter(
                ImageRequest.Builder       // Default image if the load fails
                    (LocalContext.current).data(data = member.pictureUrl)
                    .apply(block = fun ImageRequest.Builder.() {
                        placeholder(R.drawable.placeholder) // Placeholder while loading
                        error(R.drawable.placeholder)       // Default image if the load fails
                    }).build()
            ),
            contentDescription = null,
            modifier = Modifier
                .height(100.dp)
                .width(100.dp),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "${member.firstname} ${member.lastname}",
            style = MaterialTheme.typography.bodyLarge
        )
        Text(text = "Heteka ID: ${member.hetekaId}", style = MaterialTheme.typography.bodySmall)
        Text(text = "Party: ${member.party}", style = MaterialTheme.typography.bodySmall)
        Text(text = "Seat Number: ${member.seatNumber}", style = MaterialTheme.typography.bodySmall)
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    Andriod_LabsTheme {
        ParliamentMembersList(members = ParliamentMembersData.members)
    }
}