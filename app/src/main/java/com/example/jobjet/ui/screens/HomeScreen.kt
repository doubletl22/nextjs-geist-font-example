package com.example.jobjet.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.jobjet.data.model.Job
import com.example.jobjet.data.model.JobType
import com.example.jobjet.data.model.UserRole
import com.example.jobjet.data.repository.FirebaseRepository
import com.example.jobjet.navigation.Screen
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.roundToInt

@Composable
private fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    Layout(
        content = content,
        modifier = modifier
    ) { measurables, constraints ->
        val rows = mutableListOf<List<androidx.compose.ui.layout.Placeable>>()
        var currentRow = mutableListOf<androidx.compose.ui.layout.Placeable>()
        var currentRowWidth = 0

        measurables.forEach { measurable ->
            val placeable = measurable.measure(constraints.copy(minWidth = 0))
            
            if (currentRowWidth + placeable.width > constraints.maxWidth) {
                rows.add(currentRow)
                currentRow = mutableListOf()
                currentRowWidth = 0
            }
            
            currentRow.add(placeable)
            currentRowWidth += placeable.width
        }
        
        if (currentRow.isNotEmpty()) {
            rows.add(currentRow)
        }

        val height = rows.sumOf { row ->
            row.maxOf { it.height }
        } + (rows.size - 1) * verticalArrangement.spacing.roundToPx()

        layout(constraints.maxWidth, height) {
            var y = 0
            rows.forEach { row ->
                var x = 0
                row.forEach { placeable ->
                    placeable.place(x, y)
                    x += placeable.width
                }
                y += row.maxOf { it.height } + verticalArrangement.spacing.roundToPx()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    val repository = remember { FirebaseRepository.getInstance() }
    var jobs by remember { mutableStateOf<List<Job>>(emptyList()) }
    var filteredJobs by remember { mutableStateOf<List<Job>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    
    // Search and filter states
    var searchQuery by remember { mutableStateOf("") }
    var selectedJobType by remember { mutableStateOf<JobType?>(null) }
    var showFilterDialog by remember { mutableStateOf(false) }

    // Fetch jobs
    LaunchedEffect(Unit) {
        repository.getJobs().collectLatest { jobList ->
            jobs = jobList
            // Apply initial filtering
            filteredJobs = filterJobs(jobList, searchQuery, selectedJobType)
            isLoading = false
        }
    }

    // Filter function
    fun filterJobs(
        jobList: List<Job>,
        query: String,
        type: JobType?
    ): List<Job> {
        return jobList.filter { job ->
            val matchesQuery = query.isEmpty() || 
                job.title.contains(query, ignoreCase = true) ||
                job.company.contains(query, ignoreCase = true) ||
                job.location.contains(query, ignoreCase = true)
            
            val matchesType = type == null || job.type == type
            
            matchesQuery && matchesType
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("JobJet") },
                    actions = {
                        IconButton(onClick = { showFilterDialog = true }) {
                            Icon(Icons.Default.FilterList, contentDescription = "Filter")
                        }
                        IconButton(onClick = { navController.navigate(Screen.Chat.route) }) {
                            Icon(Icons.Default.Chat, contentDescription = "Chat")
                        }
                        IconButton(onClick = { navController.navigate(Screen.Profile.route) }) {
                            Icon(Icons.Default.Person, contentDescription = "Profile")
                        }
                    }
                )
                
                // Search bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { 
                        searchQuery = it
                        filteredJobs = filterJobs(jobs, it, selectedJobType)
                    },
                    placeholder = { Text("Search jobs...") },
                    leadingIcon = { 
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    singleLine = true
                )
            }
        },
        floatingActionButton = {
            val currentUser = remember { repository.auth.currentUser }
            var userRole by remember { mutableStateOf<UserRole?>(null) }

            // Fetch user role
            LaunchedEffect(currentUser) {
                currentUser?.let { user ->
                    repository.getUserProfile(user.uid)
                        .onSuccess { userRole = it.role }
                }
            }

            // Show FAB only for employers
            if (userRole == UserRole.EMPLOYER) {
                FloatingActionButton(
                    onClick = { navController.navigate(Screen.JobPost.route) }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Post Job")
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                filteredJobs.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (searchQuery.isNotBlank() || selectedJobType != null)
                                "No jobs found matching your criteria"
                            else
                                "No jobs available at the moment",
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center
                        )
                        if (searchQuery.isNotBlank() || selectedJobType != null) {
                            TextButton(
                                onClick = {
                                    searchQuery = ""
                                    selectedJobType = null
                                    filteredJobs = jobs
                                }
                            ) {
                                Text("Clear filters")
                            }
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        items(filteredJobs) { job ->
                            JobCard(
                                job = job,
                                onClick = { navController.navigate(Screen.JobDetail.createRoute(job.id)) }
                            )
                        }
                    }
                }
            }

            // Filter Dialog
            if (showFilterDialog) {
                AlertDialog(
                    onDismissRequest = { showFilterDialog = false },
                    title = { Text("Filter Jobs") },
                    text = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Job Type", style = MaterialTheme.typography.titleMedium)
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = selectedJobType == null,
                                    onClick = { 
                                        selectedJobType = null
                                        filteredJobs = filterJobs(jobs, searchQuery, null)
                                        showFilterDialog = false
                                    },
                                    label = { Text("All") }
                                )
                                JobType.values().forEach { type ->
                                    FilterChip(
                                        selected = selectedJobType == type,
                                        onClick = { 
                                            selectedJobType = type
                                            filteredJobs = filterJobs(jobs, searchQuery, type)
                                            showFilterDialog = false
                                        },
                                        label = { 
                                            Text(
                                                type.name.replace('_', ' ')
                                                    .split(' ')
                                                    .joinToString(" ") { it.lowercase().capitalize() }
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showFilterDialog = false }) {
                            Text("Close")
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JobCard(
    job: Job,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = job.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = job.company,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = "Location",
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = job.location,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.AttachMoney,
                    contentDescription = "Salary",
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = job.salary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Chip(
                onClick = { },
                label = { Text(job.type.name) },
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
