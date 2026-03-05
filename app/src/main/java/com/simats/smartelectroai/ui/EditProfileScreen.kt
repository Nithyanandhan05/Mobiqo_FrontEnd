package com.simats.smartelectroai.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simats.smartelectroai.R

// --- Design Colors ---
private val EditBlue = Color(0xFF1976D2)
private val EditHeader = Color(0xFF1E1E1E)
private val EditLabel = Color(0xFF757575) // Gray for labels like "FULL NAME"
private val EditBorder = Color(0xFFE0E0E0)
private val EditFieldText = Color(0xFF1E1E1E)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Edit Profile",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = EditHeader
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = EditHeader
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            // Save Button fixed at bottom
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .background(Color.White)
            ) {
                Button(
                    onClick = { onBack() }, // Navigates back on save
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EditBlue)
                ) {
                    Text(
                        "SAVE CHANGES",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        },
        containerColor = Color.White
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp), // slightly wider padding for form
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // --- Profile Photo Section ---
            Box(contentAlignment = Alignment.BottomEnd) {
                // Main Photo
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE0C3A5)), // Placeholder skin tone
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.profile),
                        contentDescription = "Profile",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                // Camera Icon Badge
                Box(
                    modifier = Modifier
                        .offset(x = 4.dp, y = 4.dp)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(EditBlue)
                        .border(2.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Change",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                "Change Photo",
                color = EditBlue,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.clickable { }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // --- Form Fields ---

            // Full Name
            EditProfileField(
                label = "FULL NAME",
                value = "Alex Johnson"
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Email Address (With Verification)
            EditProfileField(
                label = "EMAIL ADDRESS",
                value = "alex.johnson@example.com",
                trailingContent = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = "Verified",
                            tint = EditBlue,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("VERIFIED", color = EditBlue, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Change", color = EditBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Mobile Number
            EditProfileField(
                label = "MOBILE NUMBER",
                value = "+1 (555) 012-3456",
                trailingContent = {
                    Text("Change", color = EditBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Row: Date of Birth & Gender
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    EditProfileField(label = "DATE OF BIRTH", value = "05/15/1992")
                }

                Box(modifier = Modifier.weight(1f)) {
                    EditProfileField(
                        label = "GENDER",
                        value = "Male",
                        trailingContent = {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = EditLabel)
                        }
                    )
                }
            }

            // Spacer to ensure content isn't hidden behind bottom bar
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

// --- Helper Composable for Fields ---
@Composable
fun EditProfileField(
    label: String,
    value: String,
    trailingContent: @Composable (() -> Unit)? = null
) {
    Column {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = EditLabel,
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Custom Input Box Look
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .border(1.dp, EditBorder, RoundedCornerShape(12.dp))
                .background(Color.White, RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = value,
                    fontSize = 15.sp,
                    color = EditFieldText,
                    fontWeight = FontWeight.Normal
                )

                if (trailingContent != null) {
                    trailingContent()
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewEditProfile() {
    EditProfileScreen(onBack = {})
}