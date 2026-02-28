package com.sabelaperez.lockedin.ui.passwords

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sabelaperez.lockedin.R
import com.sabelaperez.lockedin.ui.theme.LockedInTheme

@Composable
fun MyPasswordsScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.my_passwords_title),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = { /* TODO */ },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC8E6C9)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.add_password),
                    color = Color.Black,
                    fontSize = 12.sp
                )
            }

            Button(
                onClick = { /* TODO */ },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFCDD2)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Box(
                  modifier = Modifier.size(20.dp).background(color = Color.Black, shape = RoundedCornerShape(2.dp)),
                  contentAlignment = Alignment.Center
                ) {
                    Box(modifier = Modifier.width(12.dp).height(2.dp).background(Color.White))
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.del_passwords),
                    color = Color.Black,
                    fontSize = 12.sp
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = "",
                onValueChange = {},
                modifier = Modifier.weight(1f),
                placeholder = { Text(stringResource(R.string.search_placeholder)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(4.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.List, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = stringResource(R.string.filter))
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = true, onCheckedChange = {})
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .border(1.dp, Color.Black)
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = stringResource(R.string.logo), fontSize = 10.sp)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = stringResource(R.string.site_name),
                modifier = Modifier.weight(1f),
                fontWeight = FontWeight.Medium
            )
            Text(
                text = stringResource(R.string.owner),
                modifier = Modifier.padding(horizontal = 8.dp),
                fontWeight = FontWeight.Medium
            )
            Box(
                modifier = Modifier.size(24.dp).background(color = Color.Gray)
            )
        }

        val passwords = listOf("google.com", "google.com", "google.com", "google.com", "google.com")
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(passwords) { site ->
                PasswordItem(site = site)
            }
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("•", fontSize = 24.sp)
                    Text("•", fontSize = 24.sp)
                    Text("•", fontSize = 24.sp)
                }
            }
        }
    }
}

@Composable
fun PasswordItem(site: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = false, onCheckedChange = {})
        Spacer(modifier = Modifier.width(8.dp))
        Row(
            modifier = Modifier
                .weight(1f)
                .border(1.dp, Color.Black)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "G",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Text(
                text = site,
                modifier = Modifier.weight(1f),
                fontSize = 18.sp
            )
        }
        Text(
            text = "You",
            modifier = Modifier.padding(horizontal = 16.dp),
            fontSize = 14.sp
        )
        IconButton(
            onClick = { /* TODO */ },
            modifier = Modifier
                .size(40.dp)
                .border(1.dp, Color.Black, RoundedCornerShape(20.dp))
        ) {
            Box(
                modifier = Modifier.size(20.dp).background(color = Color.Gray)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MyPasswordsScreenPreview() {
    LockedInTheme {
        MyPasswordsScreen()
    }
}