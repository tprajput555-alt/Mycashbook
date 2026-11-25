package com.example.mycashbook

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Entry(val id: Long, val type: String, val amount: Double, val note: String, val date: String)

class CashDb(context: android.content.Context) :
    SQLiteOpenHelper(context, "cashbook.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE entries (id INTEGER PRIMARY KEY AUTOINCREMENT, type TEXT, amount REAL, note TEXT, date TEXT)")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}
    fun add(type: String, amount: Double, note: String) {
        val values = ContentValues().apply {
            put("type", type); put("amount", amount); put("note", note)
            put("date", SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date()))
        }
        writableDatabase.insert("entries", null, values)
    }
    fun all(): List<Entry> {
        val out = mutableListOf<Entry>()
        readableDatabase.rawQuery("SELECT id,type,amount,note,date FROM entries ORDER BY id DESC", null).use { c ->
            while (c.moveToNext()) out.add(Entry(c.getLong(0), c.getString(1), c.getDouble(2), c.getString(3), c.getString(4)))
        }
        return out
    }
    fun delete(id: Long) { writableDatabase.delete("entries", "id=?", arrayOf(id.toString())) }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { CashbookScreen() } }
    }
}

@Composable
fun CashbookScreen() {
    val context = LocalContext.current
    val db = remember { CashDb(context) }
    var entries by remember { mutableStateOf(db.all()) }
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf("Cash In") }
    var showForm by remember { mutableStateOf(false) }

    val cashIn = entries.filter { it.type == "Cash In" }.sumOf { it.amount }
    val cashOut = entries.filter { it.type == "Cash Out" }.sumOf { it.amount }
    val onlineIn = entries.filter { it.type == "Online In" }.sumOf { it.amount }
    val onlineOut = entries.filter { it.type == "Online Out" }.sumOf { it.amount }
    val total = cashIn - cashOut + onlineIn - onlineOut
    fun money(v: Double) = "₹ " + String.format(Locale("en", "IN"), "%,.2f", v)

    Scaffold(topBar = {
        TopAppBarCompat()
    }, floatingActionButton = {
        FloatingActionButton(onClick = { showForm = true }) { Text("+") }
    }) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Text("Overview", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Net Balance", style = MaterialTheme.typography.titleMedium)
                        Text(money(total), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SummaryCard("Cash In", cashIn, Modifier.weight(1f))
                    SummaryCard("Cash Out", cashOut, Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SummaryCard("Online In", onlineIn, Modifier.weight(1f))
                    SummaryCard("Online Out", onlineOut, Modifier.weight(1f))
                }
            }
            item { Text("Transactions", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
            if (entries.isEmpty()) item { Text("No transactions yet. Tap + to add your first entry.") }
            items(entries, key = { it.id }) { e ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text(e.type, fontWeight = FontWeight.SemiBold)
                            if (e.note.isNotBlank()) Text(e.note, style = MaterialTheme.typography.bodySmall)
                            Text(e.date, style = MaterialTheme.typography.labelSmall)
                        }
                        Column {
                            Text(money(e.amount), fontWeight = FontWeight.Bold)
                            TextButton(onClick = { db.delete(e.id); entries = db.all() }) { Text("Delete") }
                        }
                    }
                }
            }
        }
    }

    if (showForm) {
        AlertDialog(
            onDismissRequest = { showForm = false },
            title = { Text("New Transaction") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf("Cash In", "Cash Out", "Online In", "Online Out").forEach { option ->
                        Row {
                            RadioButton(selected == option, onClick = { selected = option })
                            Text(option, Modifier.padding(top = 12.dp))
                        }
                    }
                    OutlinedTextField(amount, { amount = it }, label = { Text("Amount (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                    OutlinedTextField(note, { note = it }, label = { Text("Note / Description") }, singleLine = true)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val value = amount.toDoubleOrNull()
                    if (value != null && value > 0) {
                        db.add(selected, value, note)
                        entries = db.all()
                        amount = ""; note = ""; showForm = false
                    }
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showForm = false }) { Text("Cancel") } }
        )
    }
}

@Composable
fun SummaryCard(label: String, value: Double, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Text("₹ " + String.format(Locale("en", "IN"), "%,.2f", value),
                fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopAppBarCompat() {
    TopAppBar(title = { Text("My Cashbook") })
}
